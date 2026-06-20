package java17.java.lang;

import java.util.List;
import java.util.stream.Collectors;

public class TextBlockTest {
    static final String CLS = "TextBlockTest";
    static int P, F;
    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    public static void main(String[] args) {
        try {
            // Basic multi-line text block: incidental leading whitespace stripped,
            // each visual line followed by a newline (including the last, since the
            // closing delimiter is on its own line).
            String html = """
                    <html>
                        <body>
                        </body>
                    </html>
                    """;
            String expHtml = "<html>\n    <body>\n    </body>\n</html>\n";
            ck("basic block", html, expHtml);

            // Equals the equivalent concatenated string.
            String concat = "<html>\n" + "    <body>\n" + "    </body>\n" + "</html>\n";
            ck("equals concat", html.equals(concat), true);
            ck("intern same", html, concat);

            // No trailing newline when closing delimiter is on the last content line.
            String noTrail = """
                    one
                    two""";
            ck("no trailing newline", noTrail, "one\ntwo");

            // Incidental indentation: the common minimal indent is stripped, the
            // extra indent on the middle line is preserved.
            String indent = """
                    a
                      b
                    c
                    """;
            ck("incidental indent", indent, "a\n  b\nc\n");

            // Trailing backslash = line continuation: no newline inserted.
            String cont = """
                    no \
                    break
                    """;
            ck("line continuation", cont, "no break\n");

            // \s escape preserves a trailing space (and prevents stripping it).
            String sp = """
                    end\s
                    """;
            ck("backslash-s space", sp, "end \n");

            // Embedded double quotes need no escaping.
            String quotes = """
                    He said "hi" to me.
                    """;
            ck("embedded quotes", quotes, "He said \"hi\" to me.\n");

            // A single-line text block (still terminated by closing on next line).
            String single = """
                    solo
                    """;
            ck("single line block", single, "solo\n");

            // .lines() count and content.
            String three = """
                    L1
                    L2
                    L3
                    """;
            ck("lines count", three.lines().count(), 3L);
            List<String> ls = three.lines().collect(Collectors.toList());
            ck("lines content", ls, List.of("L1", "L2", "L3"));

            // .formatted(...) on a text block.
            String tmpl = """
                    Name: %s
                    Age: %d
                    """;
            ck("formatted", tmpl.formatted("Ada", 36), "Name: Ada\nAge: 36\n");

            // length sanity of a known block.
            ck("length", noTrail.length(), 7); // "one\ntwo"

            // String.stripIndent on an ordinary (non-text-block) string.
            String raw = "  x\n    y";
            ck("stripIndent", raw.stripIndent(), "x\n  y");

            // String.translateEscapes turns escape sequences into chars.
            ck("translateEscapes", "a\\tb\\n".translateEscapes(), "a\tb\n");

            // Text block containing a literal backslash via \\.
            String bs = """
                    c:\\dir
                    """;
            ck("literal backslash", bs, "c:\\dir\n");

            // Text block with three quotes inside via escape.
            String triple = """
                    say \"""
                    """;
            ck("escaped triple quote", triple, "say \"\"\"\n");

            // startsWith / endsWith on a text block result.
            ck("startsWith", html.startsWith("<html>"), true);
            ck("endsWith newline", html.endsWith("</html>\n"), true);

            // Closing delimiter indentation controls strip: here closing delim aligns
            // with content so all content indentation is incidental.
            String aligned = """
                        deep
                        """;
            ck("aligned strip", aligned, "deep\n");
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
