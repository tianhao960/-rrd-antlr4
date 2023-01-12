# rrg-antlr4
Inspired by the project [rrd-antlr4](https://github.com/bkiers/rrd-antlr4) , and add following features:

1 could specify the root rule of the grammar, and output rules BFs from root rule.

2 could specify the import grammar directory.

3 make this tool as a maven plugin.
    
# Basic usage

use this tool as a maven plugin.

```xml
<plugin>
    <groupId>space.vector</groupId>
    <artifactId>rr-maven-plugin</artifactId>
    <version>0.0.1</version>
    <executions>
        <execution>
            <goals>
                <goal>rr</goal>
            </goals>
            <configuration>
                <rootRule>execute</rootRule>
                <libDirectory>src/main/antlr4/imports/mysql/</libDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

# Credits
| project                                            | License                                                                              |
|----------------------------------------------------|--------------------------------------------------------------------------------------|
| [rrd-antlr4](https://github.com/bkiers/rrd-antlr4) | [MIT](https://github.com/bkiers/rrd-antlr4/blob/master/LICENSE)                      |
| [grammars-v4](https://github.com/antlr/grammars-v4/) | [BSD](https://github.com/antlr/grammars-v4/blob/master/antlr/antlr4/ANTLRv4Lexer.g4) |
| [railroad-diagrams](https://github.com/tabatkins/railroad-diagrams) | [MIT](https://github.com/tabatkins/railroad-diagrams/blob/gh-pages/LICENSE) |