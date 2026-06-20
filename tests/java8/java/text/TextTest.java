package java8.java.text;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class TextTest {

    static final String CLS = "TextTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", Objects.equals(got, exp)); }

    public static void main(String[] args) {
        try {
            final Locale US = Locale.US;
            final TimeZone UTC = TimeZone.getTimeZone("UTC");

            // ---- SimpleDateFormat: format a fixed Date in UTC, then round-trip ----
            // 1234567890000 ms == 2009-02-13 23:31:30 UTC
            Date fixed = new Date(1234567890000L);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", US);
            sdf.setTimeZone(UTC);
            String dateStr = sdf.format(fixed);
            ck("sdf.format", dateStr, "2009-02-13 23:31:30");
            Date parsed = sdf.parse(dateStr);
            ck("sdf.parse roundtrip ms", parsed.getTime(), 1234567890000L);
            ck("sdf.parse equals date", parsed, fixed);

            // Pattern with day-of-week / month name (locale-sensitive, fixed by Locale.US)
            SimpleDateFormat sdf2 = new SimpleDateFormat("EEE, dd MMM yyyy", US);
            sdf2.setTimeZone(UTC);
            ck("sdf2 named fields", sdf2.format(fixed), "Fri, 13 Feb 2009");

            // ---- DecimalFormat: "#,##0.00" grouping + format + parse ----
            DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(US));
            ck("df grouping format", df.format(1234567.891), "1,234,567.89");
            ck("df small format", df.format(3.5), "3.50");
            Number n1 = df.parse("1,234,567.89");
            ck("df parse value", n1.doubleValue(), 1234567.89);

            // ---- DecimalFormat scientific: "0.###E0" ----
            DecimalFormat sci = new DecimalFormat("0.###E0", new DecimalFormatSymbols(US));
            ck("sci format 12340", sci.format(12340.0), "1.234E4");
            ck("sci format 0.001234", sci.format(0.001234), "1.234E-3");

            // ---- NumberFormat factory methods (Locale.US) ----
            NumberFormat nf = NumberFormat.getInstance(US);
            ck("nf getInstance", nf.format(1234.5), "1,234.5");

            NumberFormat cf = NumberFormat.getCurrencyInstance(US);
            ck("nf currency", cf.format(1234.5), "$1,234.50");
            ck("nf currency negative", cf.format(-7.0), "-$7.00");

            NumberFormat pf = NumberFormat.getPercentInstance(US);
            ck("nf percent", pf.format(0.25), "25%");
            ck("nf percent fraction", pf.format(0.5), "50%");

            // ---- MessageFormat ----
            String msg = MessageFormat.format("{0} = {1,number,#.##}", "pi", 3.14159);
            ck("MessageFormat", msg, "pi = 3.14");

            // Locale-explicit MessageFormat instance
            MessageFormat mf = new MessageFormat("{0} items cost {1,number,currency}", US);
            ck("MessageFormat currency", mf.format(new Object[]{3, 4.5}), "3 items cost $4.50");

            // ---- ParsePosition: parse a leading number then continue ----
            ParsePosition pp = new ParsePosition(0);
            Number n2 = nf.parse("42abc", pp);
            ck("ParsePosition value", n2.longValue(), 42L);
            ck("ParsePosition index", pp.getIndex(), 2);

            // ParsePosition failure leaves errorIndex set
            ParsePosition pp2 = new ParsePosition(0);
            Number n3 = nf.parse("xyz", pp2);
            ck("ParsePosition fail null", n3, null);
            ck("ParsePosition errorIndex", pp2.getErrorIndex(), 0);
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
