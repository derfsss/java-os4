package java17.java.lang;

import java.util.Objects;

public class SwitchExprTest {

    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

    static int P, F;
    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    // Arrow switch over an enum, exhaustive (no default needed for full enum coverage).
    static int workHours(Day d) {
        return switch (d) {
            case MON, TUE, WED, THU -> 8;
            case FRI -> 6;
            case SAT, SUN -> 0;
        };
    }

    // Arrow switch over String, multi-labels, yielding from default.
    static String category(String s) {
        return switch (s) {
            case "a", "e", "i", "o", "u" -> "vowel";
            case "y" -> "sometimes";
            default -> "consonant";
        };
    }

    // Arrow switch over int with multi-labels and a default.
    static int classifyInt(int n) {
        return switch (n) {
            case 1, 3, 5, 7, 9 -> 1;   // odd small
            case 0, 2, 4, 6, 8 -> 2;   // even small
            default -> 9;              // out of range
        };
    }

    // Switch expression with a block body using yield.
    static int blockYield(int n) {
        return switch (n) {
            case 1 -> 100;
            case 2 -> {
                int t = 0;
                for (int i = 1; i <= n; i++) t += i;
                yield t * 10;   // (1+2)*10 = 30
            }
            default -> {
                int sq = n * n;
                yield sq + 1;
            }
        };
    }

    // Switch as a STATEMENT with arrows (no fallthrough).
    static String dayKind(Day d) {
        String kind;
        switch (d) {
            case SAT, SUN -> kind = "weekend";
            default -> kind = "weekday";
        }
        return kind;
    }

    public static void main(String[] args) {
        try {
            // Exhaustive enum arrow switch expression.
            ck("workHours MON", workHours(Day.MON), 8);
            ck("workHours THU", workHours(Day.THU), 8);
            ck("workHours FRI", workHours(Day.FRI), 6);
            ck("workHours SAT", workHours(Day.SAT), 0);
            ck("workHours SUN", workHours(Day.SUN), 0);

            // String multi-label + default yield.
            ck("category a", category("a"), "vowel");
            ck("category o", category("o"), "vowel");
            ck("category y", category("y"), "sometimes");
            ck("category b", category("b"), "consonant");
            ck("category z", category("z"), "consonant");

            // int multi-label + default.
            ck("classifyInt 3", classifyInt(3), 1);
            ck("classifyInt 9", classifyInt(9), 1);
            ck("classifyInt 4", classifyInt(4), 2);
            ck("classifyInt 0", classifyInt(0), 2);
            ck("classifyInt 42", classifyInt(42), 9);

            // Block body with yield.
            ck("blockYield 1", blockYield(1), 100);
            ck("blockYield 2", blockYield(2), 30);
            ck("blockYield 5", blockYield(5), 26);  // 25+1

            // Statement-form arrow switch, no fallthrough.
            ck("dayKind MON", dayKind(Day.MON), "weekday");
            ck("dayKind FRI", dayKind(Day.FRI), "weekday");
            ck("dayKind SAT", dayKind(Day.SAT), "weekend");
            ck("dayKind SUN", dayKind(Day.SUN), "weekend");

            // Switch expression directly as an operand (composition).
            int combined = workHours(Day.MON) + classifyInt(7) + blockYield(2);
            ck("combined expr", combined, 8 + 1 + 30);
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println("SwitchExprTest: "+P+"/"+(P+F)+" passed");
        System.out.println("SwitchExprTest RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
