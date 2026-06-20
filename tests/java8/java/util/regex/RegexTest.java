package java8.java.util.regex;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {
    static final String CLS = "RegexTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    public static void main(String[] args) {
        try {
            // Pattern.matches static
            ck("Pattern.matches.true", Pattern.matches("a*b", "aaab"), true);
            ck("Pattern.matches.false", Pattern.matches("a*b", "aaac"), false);

            // Matcher.matches full-region
            Pattern digits = Pattern.compile("\\d+");
            ck("matcher.matches", digits.matcher("12345").matches(), true);
            ck("matcher.matches.partial", digits.matcher("12a45").matches(), false);

            // find loop counting matches
            Matcher fm = Pattern.compile("\\d+").matcher("a12b34c567");
            int count = 0; StringBuilder joined = new StringBuilder();
            while (fm.find()) { count++; if (joined.length()>0) joined.append(","); joined.append(fm.group()); }
            ck("find.count", count, 3);
            ck("find.joined", joined.toString(), "12,34,567");

            // group(n), groupCount, start/end
            Matcher gm = Pattern.compile("(\\w+)@(\\w+)\\.(\\w+)").matcher("user@host.com");
            ck("groups.matches", gm.matches(), true);
            ck("groupCount", gm.groupCount(), 3);
            ck("group0", gm.group(), "user@host.com");
            ck("group1", gm.group(1), "user");
            ck("group2", gm.group(2), "host");
            ck("group3", gm.group(3), "com");
            ck("start1", gm.start(1), 0);
            ck("end1", gm.end(1), 4);
            ck("start2", gm.start(2), 5);

            // named groups
            Matcher nm = Pattern.compile("(?<y>\\d+)-(?<m>\\d+)").matcher("2026-06");
            ck("named.matches", nm.matches(), true);
            ck("named.y", nm.group("y"), "2026");
            ck("named.m", nm.group("m"), "06");

            // lookahead: word followed by digit, capture word only
            Matcher la = Pattern.compile("foo(?=bar)").matcher("foobar foobaz");
            ck("lookahead.find", la.find(), true);
            ck("lookahead.group", la.group(), "foo");
            ck("lookahead.nomore", la.find(), false);

            // character classes + quantifiers
            ck("charclass", Pattern.matches("[A-Fa-f0-9]{6}", "1aB3Cf"), true);
            ck("quantifier.opt", Pattern.matches("colou?r", "color"), true);
            ck("quantifier.plus", Pattern.matches("ab+", "abbbb"), true);

            // replaceAll / replaceFirst with $1
            ck("replaceAll.$1", "John Smith".replaceAll("(\\w+) (\\w+)", "$2 $1"), "Smith John");
            Matcher rf = Pattern.compile("(\\d)(\\d)").matcher("12 34");
            ck("replaceFirst.$1$2", rf.replaceFirst("$2$1"), "21 34");
            Matcher ra = Pattern.compile("(\\d)(\\d)").matcher("12 34");
            ck("replaceAll.$1$2", ra.replaceAll("$2$1"), "21 43");

            // appendReplacement / appendTail
            Matcher am = Pattern.compile("\\d+").matcher("x1y22z");
            StringBuffer sb = new StringBuffer();
            while (am.find()) { am.appendReplacement(sb, "["+am.group()+"]"); }
            am.appendTail(sb);
            ck("appendReplacement/Tail", sb.toString(), "x[1]y[22]z");

            // Pattern.split with limit
            Pattern comma = Pattern.compile(",");
            ck("split.nolimit", Arrays.toString(comma.split("a,b,c")), "[a, b, c]");
            ck("split.limit2", Arrays.toString(comma.split("a,b,c", 2)), "[a, b,c]");
            ck("split.negTrailing", Arrays.toString(comma.split("a,b,,", -1)), "[a, b, , ]");

            // Pattern.quote
            String q = Pattern.quote("a.b*c");
            ck("quote.matchesLiteral", Pattern.matches(q, "a.b*c"), true);
            ck("quote.noMeta", Pattern.matches(q, "axbxc"), false);

            // flags: CASE_INSENSITIVE
            ck("CASE_INSENSITIVE", Pattern.compile("hello", Pattern.CASE_INSENSITIVE).matcher("HeLLo").matches(), true);

            // flags: MULTILINE — ^ matches at each line start
            Matcher ml = Pattern.compile("^\\w", Pattern.MULTILINE).matcher("ab\ncd\nef");
            int mlCount = 0; StringBuilder firsts = new StringBuilder();
            while (ml.find()) { mlCount++; firsts.append(ml.group()); }
            ck("MULTILINE.count", mlCount, 3);
            ck("MULTILINE.firsts", firsts.toString(), "ace");

            // flags: DOTALL — . matches newline
            ck("DOTALL", Pattern.compile("a.b", Pattern.DOTALL).matcher("a\nb").matches(), true);
            ck("DOTALL.off", Pattern.compile("a.b").matcher("a\nb").matches(), false);

            // inline flag (?i)
            ck("inlineFlag.i", Pattern.matches("(?i)abc", "ABC"), true);

            // String regex parity
            ck("String.matches", "12345".matches("\\d+"), true);
            ck("String.replaceAll", "a1b2".replaceAll("\\d", "#"), "a#b#");
            ck("String.split", Arrays.toString("p:q:r".split(":")), "[p, q, r]");
            ck("String.split.limit", Arrays.toString("p:q:r".split(":", 2)), "[p, q:r]");
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
