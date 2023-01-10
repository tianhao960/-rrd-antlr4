
package space.vector.rr;

import lombok.Getter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import space.vector.rr.ANTLRv4Parser.AltListContext;
import space.vector.rr.ANTLRv4Parser.AlternativeContext;
import space.vector.rr.ANTLRv4Parser.AtomContext;
import space.vector.rr.ANTLRv4Parser.BlockContext;
import space.vector.rr.ANTLRv4Parser.BlockSetContext;
import space.vector.rr.ANTLRv4Parser.BlockSuffixContext;
import space.vector.rr.ANTLRv4Parser.EbnfContext;
import space.vector.rr.ANTLRv4Parser.EbnfSuffixContext;
import space.vector.rr.ANTLRv4Parser.ElementContext;
import space.vector.rr.ANTLRv4Parser.ElementsContext;
import space.vector.rr.ANTLRv4Parser.LabeledAltContext;
import space.vector.rr.ANTLRv4Parser.LabeledElementContext;
import space.vector.rr.ANTLRv4Parser.LabeledLexerElementContext;
import space.vector.rr.ANTLRv4Parser.LexerAltContext;
import space.vector.rr.ANTLRv4Parser.LexerAltListContext;
import space.vector.rr.ANTLRv4Parser.LexerAtomContext;
import space.vector.rr.ANTLRv4Parser.LexerBlockContext;
import space.vector.rr.ANTLRv4Parser.LexerElementContext;
import space.vector.rr.ANTLRv4Parser.LexerElementsContext;
import space.vector.rr.ANTLRv4Parser.LexerRuleBlockContext;
import space.vector.rr.ANTLRv4Parser.LexerRuleContext;
import space.vector.rr.ANTLRv4Parser.NotSetContext;
import space.vector.rr.ANTLRv4Parser.ParserRuleSpecContext;
import space.vector.rr.ANTLRv4Parser.RangeContext;
import space.vector.rr.ANTLRv4Parser.RuleAltListContext;
import space.vector.rr.ANTLRv4Parser.RuleBlockContext;
import space.vector.rr.ANTLRv4Parser.RulerefContext;
import space.vector.rr.ANTLRv4Parser.SetElementContext;
import space.vector.rr.ANTLRv4Parser.TerminalContext;


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Railroad rule visitor to collect all rules from an ANTLR 4 grammar and
 * translate the parse tree into a DSL that the JavaScript library
 * uses to create the SVG for each grammar rule.
 */
public class RailRoadRuleVisitor extends ANTLRv4ParserBaseVisitor<String> {
    
    @Getter
    private final Map<String, String> rules;
    
    @Getter
    private final Map<String, Set<String>> rulesRelation;
    
    public RailRoadRuleVisitor() {
        this.rules = new LinkedHashMap<>();
        this.rulesRelation = new HashMap<>();
    }
    
    @Override
    public String visitParserRuleSpec(ParserRuleSpecContext ctx) {
        String ruleName = ctx.RULE_REF().getText();
        String diagram = "Diagram(" + this.visitRuleBlock(ctx.ruleBlock()) + ").toString()";
        this.rules.put(ruleName, diagram);
        return diagram;
    }
    
    @Override
    public String visitRuleBlock(RuleBlockContext ctx) {
        return this.visitRuleAltList(ctx.ruleAltList());
    }
    
    @Override
    public String visitRuleAltList(RuleAltListContext ctx) {
        StringBuilder builder = new StringBuilder("Choice(0, ");
        List<LabeledAltContext> alternatives = ctx.labeledAlt();
        for (int i = 0; i < alternatives.size(); i++) {
            LabeledAltContext alternative = alternatives.get(i);
            builder.append(this.visitLabeledAlt(alternative)).append(comma(alternatives, i));
        }
        return builder.append(")").toString();
    }
    
    @Override
    public String visitLabeledAlt(LabeledAltContext ctx) {
        return this.visitAlternative(ctx.alternative());
    }
    
    @Override
    public String visitLexerRule(LexerRuleContext ctx) {
        String ruleName = ctx.TOKEN_REF().getText();
        String diagram = "Diagram(" + this.visitLexerRuleBlock(ctx.lexerRuleBlock()) + ").toString()";
        this.rules.put(ruleName, diagram);
        return diagram;
    }
    
    @Override
    public String visitLexerRuleBlock(LexerRuleBlockContext ctx) {
        return this.visitLexerAltList(ctx.lexerAltList());
    }
    
    @Override
    public String visitLexerAltList(LexerAltListContext ctx) {
        StringBuilder builder = new StringBuilder("Choice(0, ");
        List<LexerAltContext> alts = ctx.lexerAlt();
        for (int i = 0; i < alts.size(); i++) {
            LexerAltContext alt = alts.get(i);
            builder.append(this.visitLexerAlt(alt)).append(this.comma(alts, i));
        }
        return builder.append(")").toString();
    }
    
    @Override
    public String visitLexerAlt(LexerAltContext ctx) {
        if (ctx.lexerElements() != null) {
            return this.visitLexerElements(ctx.lexerElements());
        } else {
            return "Comment('&#949;')";
        }
    }
    
    @Override
    public String visitLexerElements(LexerElementsContext ctx) {
        StringBuilder builder = new StringBuilder("Sequence(");
        List<LexerElementContext> elements = ctx.lexerElement();
        for (int i = 0; i < elements.size(); i++) {
            LexerElementContext element = elements.get(i);
            builder.append(this.visitLexerElement(element)).append(comma(elements, i));
        }
        return builder.append(")").toString();
    }
    
    @Override
    public String visitLexerElement(LexerElementContext ctx) {
        StringBuilder builder = new StringBuilder();
        boolean hasEbnfSuffix = (ctx.ebnfSuffix() != null);
        if (ctx.labeledLexerElement() != null) {
            if (hasEbnfSuffix) {
                builder.append(this.visitEbnfSuffix(ctx.ebnfSuffix())).append("(")
                        .append(this.visitLabeledLexerElement(ctx.labeledLexerElement())).append(")");
            } else {
                builder.append(this.visitLabeledLexerElement(ctx.labeledLexerElement()));
            }
        } else if (ctx.lexerAtom() != null) {
            if (hasEbnfSuffix) {
                builder.append(this.visitEbnfSuffix(ctx.ebnfSuffix())).append("(")
                        .append(this.visitLexerAtom(ctx.lexerAtom())).append(")");
            } else {
                builder.append(this.visitLexerAtom(ctx.lexerAtom()));
            }
        } else if (ctx.lexerBlock() != null) {
            if (hasEbnfSuffix) {
                builder.append(this.visitEbnfSuffix(ctx.ebnfSuffix())).append("(")
                        .append(this.visitLexerBlock(ctx.lexerBlock())).append(")");
            } else {
                builder.append(this.visitLexerBlock(ctx.lexerBlock()));
            }
        } else {
            return "Comment('&#949;')";
        }
        
        return builder.toString();
    }
    
    @Override
    public String visitLabeledLexerElement(LabeledLexerElementContext ctx) {
        if (ctx.lexerAtom() != null) {
            return this.visitLexerAtom(ctx.lexerAtom());
        } else {
            return this.visitBlock(ctx.block());
        }
    }
    
    @Override
    public String visitLexerBlock(LexerBlockContext ctx) {
        return this.visitLexerAltList(ctx.lexerAltList());
    }
    
    @Override
    public String visitAltList(AltListContext ctx) {
        StringBuilder builder = new StringBuilder("Choice(0, ");
        List<AlternativeContext> alternatives = ctx.alternative();
        for (int i = 0; i < alternatives.size(); i++) {
            AlternativeContext alternative = alternatives.get(i);
            builder.append(this.visitAlternative(alternative)).append(comma(alternatives, i));
        }
        return builder.append(")").toString();
    }
    
    @Override
    public String visitAlternative(AlternativeContext ctx) {
        if (ctx.elements() != null) {
            return this.visitElements(ctx.elements());
        } else {
            return "Comment('&#949;')";
        }
    }
    
    @Override
    public String visitElements(ElementsContext ctx) {
        
        StringBuilder builder = new StringBuilder("Sequence(");
        List<ElementContext> elements = ctx.element();
        
        for (int i = 0; i < elements.size(); i++) {
            ElementContext element = elements.get(i);
            builder.append(this.visitElement(element)).append(comma(elements, i));
        }
        return builder.append(")").toString();
    }
    
    @Override
    public String visitElement(ElementContext ctx) {
        
        boolean hasEbnfSuffix = (ctx.ebnfSuffix() != null);
        
        if (ctx.labeledElement() != null) {
            if (hasEbnfSuffix) {
                return this.visitEbnfSuffix(ctx.ebnfSuffix()) + "(" + this.visitLabeledElement(ctx.labeledElement()) + ")";
            } else {
                return this.visitLabeledElement(ctx.labeledElement());
            }
        } else if (ctx.atom() != null) {
            if (hasEbnfSuffix) {
                return this.visitEbnfSuffix(ctx.ebnfSuffix()) + "(" + this.visitAtom(ctx.atom()) + ")";
            } else {
                return this.visitAtom(ctx.atom());
            }
        } else if (ctx.ebnf() != null) {
            return this.visitEbnf(ctx.ebnf());
        } else if (ctx.QUESTION() != null) {
            return "Comment('predicate')";
        } else {
            return "Comment('&#949;')";
        }
    }
    
    @Override
    public String visitLabeledElement(LabeledElementContext ctx) {
        if (ctx.atom() != null) {
            return this.visitAtom(ctx.atom());
        } else {
            return this.visitBlock(ctx.block());
        }
    }
    
    @Override
    public String visitEbnf(EbnfContext ctx) {
        if (ctx.blockSuffix() != null) {
            return this.visitBlockSuffix(ctx.blockSuffix()) + "(" + this.visitBlock(ctx.block()) + ")";
        } else {
            return this.visitBlock(ctx.block());
        }
    }
    
    @Override
    public String visitBlockSuffix(BlockSuffixContext ctx) {
        return this.visitEbnfSuffix(ctx.ebnfSuffix());
    }
    
    @Override
    public String visitEbnfSuffix(EbnfSuffixContext ctx) {
        String text = ctx.getText();
        if (text.equals("?")) {
            return "Optional";
        } else if (text.equals("*")) {
            return "ZeroOrMore";
        } else {
            return "OneOrMore";
        }
    }
    
    @Override
    public String visitLexerAtom(LexerAtomContext ctx) {
        if (ctx.RULE_REF() != null) {
            {
                buildRelation(ctx);
            }
        }
        
        if (ctx.range() != null) {
            return this.visitRange(ctx.range());
        } else if (ctx.terminal() != null) {
            return this.visitTerminal(ctx.terminal());
        } else if (ctx.RULE_REF() != null) {
            return this.visitTerminal(ctx.RULE_REF());
        } else if (ctx.notSet() != null) {
            return this.visitNotSet(ctx.notSet());
        } else if (ctx.LEXER_CHAR_SET() != null) {
            return this.visitTerminal(ctx.LEXER_CHAR_SET());
        } else {
            return "Terminal('any char')";
        }
    }
    
    @Override
    public String visitAtom(AtomContext ctx) {
        
        if (ctx.range() != null) {
            return this.visitRange(ctx.range());
        } else if (ctx.terminal() != null) {
            return this.visitTerminal(ctx.terminal());
        } else if (ctx.ruleref() != null) {
            return this.visitRuleref(ctx.ruleref());
        } else if (ctx.notSet() != null) {
            return this.visitNotSet(ctx.notSet());
        } else {
            return "NonTerminal('any token')";
        }
    }
    
    @Override
    public String visitNotSet(NotSetContext ctx) {
        if (ctx.setElement() != null) {
            return "Sequence(Comment('not'), " + this.visitSetElement(ctx.setElement()) + ")";
        } else {
            return "Sequence(Comment('not'), " + this.visitBlockSet(ctx.blockSet()) + ")";
        }
    }
    
    @Override
    public String visitBlockSet(BlockSetContext ctx) {
        StringBuilder builder = new StringBuilder("Choice(0, ");
        List<SetElementContext> elements = ctx.setElement();
        
        for (int i = 0; i < elements.size(); i++) {
            SetElementContext element = elements.get(i);
            builder.append(this.visitSetElement(element)).append(comma(elements, i));
        }
        return builder.append(")").toString();
    }
    
    @Override
    public String visitBlock(BlockContext ctx) {
        return this.visitAltList(ctx.altList());
    }
    
    @Override
    public String visitRuleref(RulerefContext ctx) {
        buildRelation(ctx);
        return this.visitTerminal(ctx.RULE_REF());
    }
    
    @Override
    public String visitRange(RangeContext ctx) {
        return String.format("'%s .. %s'", this.escapeTerminal(ctx.STRING_LITERAL(0)), this.escapeTerminal(ctx.STRING_LITERAL(1)));
    }
    
    @Override
    public String visitTerminal(TerminalContext ctx) {
        if (ctx.TOKEN_REF() != null) {
            ParserRuleContext context = ctx.getParent();
            String text = ctx.TOKEN_REF().getText();
            while (null != context) {
                if (context instanceof ParserRuleSpecContext) {
                    String ruleName = ((ParserRuleSpecContext) context).RULE_REF().getText();
                    if (rulesRelation.containsKey(ruleName)) {
                        rulesRelation.get(ruleName).add(text);
                    } else {
                        Set<String> temp = new HashSet<>();
                        temp.add(text);
                        rulesRelation.put(ruleName, temp);
                    }
                    break;
                } else if (context instanceof LexerRuleContext) {
                    String ruleName = ((LexerRuleContext) context).TOKEN_REF().getText();
                    if (rulesRelation.containsKey(ruleName)) {
                        rulesRelation.get(ruleName).add(text);
                    } else {
                        Set<String> temp = new HashSet<>();
                        temp.add(text);
                        rulesRelation.put(ruleName, temp);
                    }
                    break;
                }
                context = context.getParent();
            }
            return this.visitTerminal(ctx.TOKEN_REF());
        } else {
            return this.visitTerminal(ctx.STRING_LITERAL());
        }
    }
    
    private String escapeTerminal(TerminalNode node) {
        String text = node.getText();
        String escaped = text.replace("\\u", "\\\\u");
        switch (node.getSymbol().getType()) {
            case ANTLRv4Lexer.STRING_LITERAL:
                return "\\'" + escaped.substring(1, escaped.length() - 1) + "\\'";
            default:
                return escaped.replace("'", "\\'");
        }
    }
    
    private String comma(Collection<?> collection, int index) {
        return index < collection.size() - 1 ? ", " : "";
    }
    
    @Override
    public String visitTerminal(TerminalNode node) {
        switch (node.getSymbol().getType()) {
            case ANTLRv4Lexer.STRING_LITERAL:
            case ANTLRv4Lexer.LEXER_CHAR_SET:
                return "Terminal('" + this.escapeTerminal(node) + "')";
            case ANTLRv4Lexer.TOKEN_REF:
                return "Terminal('" + node.getText() + "')";
            default:
                return "NonTerminal('" + node.getText() + "')";
        }
    }
    
    private void buildRelation(RulerefContext ctx) {
        ParserRuleContext context = ctx.getParent();
        while (null != context) {
            if (context instanceof ParserRuleSpecContext) {
                String ruleName = ((ParserRuleSpecContext) context).RULE_REF().getText();
                if (rulesRelation.containsKey(ruleName)) {
                    rulesRelation.get(ruleName).add(ctx.RULE_REF().getText());
                } else {
                    Set<String> temp = new HashSet<>();
                    temp.add(ctx.RULE_REF().getText());
                    rulesRelation.put(ruleName, temp);
                }
                break;
            }
            context = context.getParent();
        }
    }
    
    private void buildRelation(LexerAtomContext ctx) {
        ParserRuleContext context = ctx.getParent();
        while (null != context) {
            if (context instanceof LexerRuleContext) {
                String ruleName = ((LexerRuleContext) context).TOKEN_REF().getText();
                if (rulesRelation.containsKey(ruleName)) {
                    rulesRelation.get(ruleName).add(ctx.RULE_REF().getText());
                } else {
                    Set<String> temp = new HashSet<>();
                    temp.add(ctx.RULE_REF().getText());
                    rulesRelation.put(ruleName, temp);
                }
                break;
            }
            context = context.getParent();
        }
    }
}
