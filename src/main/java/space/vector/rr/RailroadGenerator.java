

package space.vector.rr;

import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Railroad Generator to generate railroad diagram.
 *
 */
@Slf4j
public class RailroadGenerator {
    
    private static final ScriptEngineManager MANAGER = new ScriptEngineManager();
    private static final ScriptEngine ENGINE = MANAGER.getEngineByName("graal.js");
    private static final String RAILROAD_SCRIPT = inputAsString(RailroadGenerator.class.getResourceAsStream("/railroad-diagram.js"));
    private static final String RAILROAD_CSS = inputAsString(RailroadGenerator.class.getResourceAsStream("/railroad-diagram.css"));
    private static final String HTML_TEMPLATE = inputAsString(RailroadGenerator.class.getResourceAsStream("/template.html"));
    private static final Pattern TEXT_PATTERN = Pattern.compile("(<text\\s+[^>]*?>\\s*(.+?)\\s*</text>)|[\\s\\S]");
    
    static {
        try {
            ENGINE.eval(RAILROAD_SCRIPT);
        } catch (ScriptException e) {
            log.error("could not evaluate script:\n{}", RAILROAD_SCRIPT);
            System.exit(1);
        }
    }
    private Map<String, String> rules;
    private Map<String, Set<String>> rulesRelation;
    private Map<String, String> comments;
    
    public RailroadGenerator() {
        this.rules = new HashMap<>();
        this.comments = new HashMap<>();
        this.rulesRelation = new HashMap<>();
    }
    
    /**
     * parse the antlr4 grammar and get all the rules and rules relations.
     *
     * @param grammarFile grammar file
     * @throws IOException
     */
    public void parse(File grammarFile) throws IOException {
        
        InputStream input = new FileInputStream(grammarFile);
        
        ANTLRv4Lexer lexer = new ANTLRv4Lexer(new ANTLRInputStream(new BufferedInputStream(input)));
        ANTLRv4Parser parser = new ANTLRv4Parser(new CommonTokenStream(lexer));
        
        ParseTree tree = parser.grammarSpec();
        RailRoadRuleVisitor visitor = new RailRoadRuleVisitor();
        visitor.visit(tree);
        
        this.rules.putAll(visitor.getRules());
        this.comments.putAll(CommentsParser.commentsMap(inputAsString(new FileInputStream(grammarFile))));
        this.rulesRelation.putAll(visitor.getRulesRelation());
    }
    
    private String getSVG(String ruleName) {
        try {
            CharSequence dsl = rules.get(ruleName);
            if (dsl == null) {
                return "";
            }
            String svg = (String) ENGINE.eval(dsl.toString());
            svg = svg.replaceFirst("<svg ", "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ");
            svg = svg.replaceFirst("<g ", "<style type=\"text/css\">" + RAILROAD_CSS + "</style>\n<g ");
            return svg;
        } catch (ScriptException e) {
            log.error("get svg of rule {} fail", ruleName);
            throw new RuntimeException(e);
        }
    }
    
    private String getHtml(String fileName, String rootRule) {
        StringBuilder rows = new StringBuilder();
        
        for (String ruleName : iterateRules(rootRule)) {
            String svg = this.getSVG(ruleName);
            String ruleDescription = comments.get(ruleName);
            
            rows.append("<tr><td id=\"").append(fileName).append("_").append(ruleName).append("\"><h4>").append(ruleName).append("</h4></td><td>").append(svg).append("</td></tr>");
            if (ruleDescription != null) {
                rows.append("<tr class=\"border-notop\"><td></td><td>" + ruleDescription.replaceAll("\n", "<br>") + "</td></tr>");
            }
        }
        
        final String template = HTML_TEMPLATE.replace("${rows}", rows);
        return addLinks(fileName, template);
    }
    
    private Collection<String> iterateRules(String rootRule) {
        if (null == rootRule) {
            return this.rules.keySet();
        } else {
            return iterateRulesBroadcast(rootRule);
        }
    }
    
    private Collection<String> iterateRulesBroadcast(String rootRule) {
        Collection<String> rules = new ArrayList<>(this.rules.size());
        Collection<String> rulesProcessed = new HashSet<>(this.rules.size());
        rules.add(rootRule);
        rulesProcessed.add(rootRule);
        
        Queue<String> ruleQueue = new LinkedList<>();
        ruleQueue.addAll(this.rulesRelation.get(rootRule));
        rulesProcessed.addAll(this.rulesRelation.get(rootRule));
        
        while (!ruleQueue.isEmpty()) {
            String rule = ruleQueue.poll();
            rules.add(rule);
            Collection<String> childRules = this.rulesRelation.get(rule);
            if (null != childRules && !childRules.isEmpty()) {
                childRules.forEach(
                        childRule -> {
                            if (!rulesProcessed.contains(childRule)) {
                                rulesProcessed.add(childRule);
                                ruleQueue.add(childRule);
                            }
                        });
            }
        }
        return rules;
    }
    
    /**
     * Creates an html page containing all grammar rules.
     *
     * @param dir
     *          output dir
     * @param fileName
     *          output fine name
     *
     * @return`true` iff the creation of the html page was successful.
     */
    public boolean createHtml(String dir, String fileName, String rootRule) {
        String html = this.getHtml(fileName, rootRule);
        PrintWriter out = null;
        
        try {
            out = new PrintWriter(new File(dir + "/" + fileName));
            out.write(html);
            return true;
        } catch (IOException e) {
            log.error("create html fail,Exception:{}", e.getMessage());
            return false;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    
    private String addLinks(String fileName, String template) {
        StringBuilder builder = new StringBuilder();
        Matcher m = TEXT_PATTERN.matcher(template);
        while (m.find()) {
            if (m.group(1) == null) {
                builder.append(m.group());
            } else {
                String textTag = m.group(1);
                String rule = m.group(2);
                if (!this.rules.containsKey(rule)) {
                    builder.append(textTag);
                } else {
                    builder.append("<a xlink:href=\"").append("#").append(fileName).append("_").append(rule).append("\">").append(textTag).append("</a>");
                }
            }
        }
        return builder.toString();
    }
    
    private static String inputAsString(InputStream input) {
        final StringBuilder builder = new StringBuilder();
        final Scanner scan = new Scanner(input);
        
        while (scan.hasNextLine()) {
            builder.append(scan.nextLine()).append(scan.hasNextLine() ? "\n" : "");
        }
        
        return builder.toString();
    }
}
