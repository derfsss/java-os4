package java8.java.time;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class TimeTest {
    static final String CLS = "TimeTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    public static void main(String[] args) {
        try {
            // ---- LocalDate ----
            LocalDate d = LocalDate.of(2020, 2, 28);
            ck("LocalDate.of", d.toString(), "2020-02-28");
            ck("LocalDate.plusDays", d.plusDays(1).toString(), "2020-02-29");
            ck("LocalDate.plusMonths", d.plusMonths(1).toString(), "2020-03-28");
            ck("LocalDate.getDayOfWeek", d.getDayOfWeek(), DayOfWeek.FRIDAY);
            ck("LocalDate.isLeapYear", d.isLeapYear(), Boolean.TRUE);
            ck("LocalDate.lengthOfMonth", d.lengthOfMonth(), 29);
            ck("LocalDate.parse", LocalDate.parse("2019-07-04").toString(), "2019-07-04");

            LocalDate a = LocalDate.of(2020, 1, 1);
            LocalDate b = LocalDate.of(2020, 3, 15);
            Period until = a.until(b);
            ck("LocalDate.until Period months", until.getMonths(), 2);
            ck("LocalDate.until Period days", until.getDays(), 14);

            // ---- LocalTime ----
            LocalTime t = LocalTime.of(10, 30, 0);
            ck("LocalTime.of", t.toString(), "10:30");
            ck("LocalTime.plusHours", t.plusHours(5).toString(), "15:30");
            ck("LocalTime.isBefore", t.isBefore(LocalTime.of(11, 0)), Boolean.TRUE);

            // ---- LocalDateTime ----
            LocalDateTime dt = LocalDateTime.of(2020, 6, 15, 9, 45, 30);
            ck("LocalDateTime.of", dt.toString(), "2020-06-15T09:45:30");
            ck("LocalDateTime.plus days", dt.plusDays(2).toLocalDate().toString(), "2020-06-17");
            ck("LocalDateTime.format", dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)),
                    "2020-06-15 09:45");

            // ---- Duration ----
            Duration dur = Duration.ofHours(2).plus(Duration.ofMinutes(30));
            ck("Duration.toMinutes", dur.toMinutes(), 150L);
            ck("Duration.toSeconds (getSeconds)", dur.getSeconds(), 9000L);
            Duration between = Duration.between(LocalTime.of(8, 0), LocalTime.of(10, 15));
            ck("Duration.between minutes", between.toMinutes(), 135L);

            // ---- Period ----
            Period per = Period.between(LocalDate.of(2020, 1, 10), LocalDate.of(2021, 4, 25));
            ck("Period.between getMonths", per.getMonths(), 3);
            ck("Period.between getDays", per.getDays(), 15);
            Period norm = Period.of(1, 15, 0).normalized();
            ck("Period.normalized years", norm.getYears(), 2);
            ck("Period.normalized months", norm.getMonths(), 3);

            // ---- Instant ----
            Instant inst = Instant.ofEpochSecond(1000);
            ck("Instant.ofEpochSecond", inst.getEpochSecond(), 1000L);
            ck("Instant.plusSeconds", inst.plusSeconds(500).getEpochSecond(), 1500L);
            ck("Instant.isAfter", inst.plusSeconds(1).isAfter(inst), Boolean.TRUE);

            // ---- ZonedDateTime ----
            ZonedDateTime z = ZonedDateTime.of(LocalDateTime.of(2020, 6, 15, 12, 0), ZoneId.of("UTC"));
            ck("ZonedDateTime.of zone", z.getZone().getId(), "UTC");
            ZonedDateTime z2 = z.withZoneSameInstant(ZoneId.of("UTC"));
            ck("ZonedDateTime.withZoneSameInstant instant", z2.toInstant(), z.toInstant());

            // ---- DateTimeFormatter round-trip ----
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String s = fmt.format(LocalDate.of(2022, 12, 25));
            ck("DateTimeFormatter.format", s, "2022-12-25");
            ck("DateTimeFormatter.parse round-trip", LocalDate.parse(s, fmt), LocalDate.of(2022, 12, 25));
            ck("DateTimeFormatter.ISO_LOCAL_DATE", DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.of(2000, 1, 2)),
                    "2000-01-02");

            // ---- ChronoUnit ----
            long days = ChronoUnit.DAYS.between(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31));
            ck("ChronoUnit.DAYS.between", days, 30L);

            // ---- Month + DayOfWeek enums ----
            ck("Month.of", Month.of(6), Month.JUNE);
            ck("Month.getValue", Month.DECEMBER.getValue(), 12);
            ck("DayOfWeek.of", DayOfWeek.of(1), DayOfWeek.MONDAY);
            ck("DayOfWeek.plus", DayOfWeek.SATURDAY.plus(1), DayOfWeek.SUNDAY);
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
