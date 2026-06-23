/*
 * test-amiga-charset.c -- host unit test for the AmigaOS charset-name fix.
 *
 * Compiles the SAME header the cross-build force-includes into libjava
 * (src/openjdk/amiga_charset.h), so this test can never drift from the shipped
 * logic.  It feeds amiga_normalize_encoding() the charset MIME name AmigaOS's
 * clib4 nl_langinfo(CODESET) reports for ten locales, and asserts the JDK
 * charset name the fix produces -- the value that becomes file.encoding /
 * sun.jnu.encoding, which must be resolvable at VM bootstrap.
 *
 * Build + run on the host (no Amiga / QEMU needed):
 *     cc tools/test-amiga-charset.c -o build/test-amiga-charset && \
 *         build/test-amiga-charset
 * Exits 0 iff every case passes (nonzero otherwise, for CI).
 */
#include <stdio.h>
#include <string.h>

#include "../src/openjdk/amiga_charset.h"

static int pass = 0, fail = 0;

/* Compare amiga_normalize_encoding(in) with the expected name (NULL-safe). */
static void
expect(const char *what, const char *in, const char *want)
{
    const char *got = amiga_normalize_encoding(in);
    int ok = (got == want) ||
             (got != 0 && want != 0 && strcmp(got, want) == 0);
    if (ok) {
        printf("[PASS] %-26s %-12s -> %s\n",
               what, in ? in : "(null)", got ? got : "(null)");
        pass++;
    } else {
        printf("[FAIL] %-26s %-12s -> %s (expected %s)\n",
               what, in ? in : "(null)", got ? got : "(null)",
               want ? want : "(null)");
        fail++;
    }
}

int
main(void)
{
    printf("== Amiga charset-name normalisation: 10 languages ==\n");

    /* The ten languages.  Column 2 is what AmigaOS diskfont/clib4 reports as
     * CODESET for that locale; column 3 is the JDK charset name the fix must
     * yield.  Western/UTF-8 locales already report standard names and pass
     * through unchanged; the Cyrillic case is the one that crashed 0.5.2. */
    expect("1. Russian (Cyrillic)",   "Amiga-1251",  "windows-1251"); /* the fix */
    expect("2. Italian",              "ISO-8859-1",  "ISO-8859-1");   /* passthrough */
    expect("3. German",               "ISO-8859-1",  "ISO-8859-1");
    expect("4. French (with Euro)",   "ISO-8859-15", "ISO-8859-15");
    expect("5. Spanish",              "ISO-8859-15", "ISO-8859-15");
    expect("6. Polish",               "ISO-8859-2",  "ISO-8859-2");
    expect("7. Greek",                "ISO-8859-7",  "ISO-8859-7");
    expect("8. Turkish",              "ISO-8859-9",  "ISO-8859-9");
    expect("9. English (modern)",     "UTF-8",       "UTF-8");
    expect("10. Czech (Amiga vendor)","Amiga-1250",  "windows-1250"); /* the fix */

    printf("== crash-avoidance / robustness edge cases ==\n");
    /* Windows code pages that exist ONLY in the JDK extended provider
     * (charsets.jar) must NOT be selected -- they would re-trigger the same
     * bootstrap class-init crash -- so they fall back to standard ISO-8859-1. */
    expect("Amiga-1255 (ext-only)",   "Amiga-1255",  "ISO8859-1");
    expect("Amiga-1256 (ext-only)",   "Amiga-1256",  "ISO8859-1");
    expect("Amiga-1258 (ext-only)",   "Amiga-1258",  "ISO8859-1");
    /* Unknown / future Amiga vendor names: boot on the safe fallback. */
    expect("Amiga-9999 (unknown)",    "Amiga-9999",  "ISO8859-1");
    expect("AmigaPL (no hyphen)",     "AmigaPL",     "ISO8859-1");
    /* Standard names that are NOT 'Amiga*' are returned verbatim. */
    expect("US-ASCII passthrough",    "US-ASCII",    "US-ASCII");
    expect("ISO-8859-5 Cyrillic ISO", "ISO-8859-5",  "ISO-8859-5");
    /* Defensive: a NULL encoding must not crash and must stay NULL. */
    expect("NULL safety",             (const char *)0, (const char *)0);

    printf("SUMMARY: %d passed, %d failed\n", pass, fail);
    return fail == 0 ? 0 : 1;
}
