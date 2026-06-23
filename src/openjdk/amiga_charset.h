/*
 * amiga_charset.h -- AmigaOS diskfont charset MIME name -> JDK charset name.
 *
 * SINGLE SOURCE OF TRUTH for the "Amiga-1251 NoClassDefFoundError" fix.
 *   - tools/build-openjdk-natives.sh force-includes this (via jdkdefs.h) into
 *     libjava and injects a call into java_props_md.c's ParseLocale().
 *   - tools/test-amiga-charset.c compiles this directly and tests it on the host.
 * Keep the logic here only; never duplicate the table.
 *
 * THE BUG -------------------------------------------------------------------
 * On AmigaOS, clib4's nl_langinfo(CODESET) returns the diskfont.library MIME
 * name verbatim (ObtainCharsetInfo DFCS_MIMENAME).  For a non-Latin locale that
 * is a *vendor* charset name -- e.g. "Amiga-1251" (Cyrillic, MIBenum 2104) --
 * which the JDK has no charset provider for.  OpenJDK's ParseLocale() copies it
 * straight into file.encoding / sun.jnu.encoding, and the very early
 * System.initProperties -> JNU_NewStringPlatform path then forces the lazy
 * sun.nio.cs.ext.ExtendedCharsets provider (Charset.lookup2) re-entrantly,
 * before sun.misc.VM.booted().  That class-init fails and is cached as
 * erroneous, surfacing as:
 *     java.lang.NoClassDefFoundError "...unsupported charset extension: Amiga-1251"
 *
 * THE FIX -------------------------------------------------------------------
 * Normalise the encoding name BEFORE it reaches file.encoding/sun.jnu.encoding:
 *   - Western/UTF-8 locales already report standard names ("ISO-8859-1",
 *     "ISO-8859-15", "UTF-8", "US-ASCII", ...) that the JDK resolves directly
 *     -- return them unchanged.
 *   - The "Amiga-NNNN" vendor family maps to the matching Windows code page
 *     ONLY when that code page lives in the JDK STANDARD provider (rt.jar):
 *     windows-1250/1251/1252/1253/1254/1257.  Those resolve without ever
 *     touching the extended provider, so the bootstrap hazard never fires.
 *     (Amiga-1251 is byte-exact with windows-1251 for the entire Russian
 *     alphabet -- it was deliberately built from cp1251's chars 168/184/192-255;
 *     see SDK Documentation/Localization/Charsets/Amiga-1251.)
 *   - Amiga-1255/1256/1258 would map to windows-12xx that are EXTENDED-provider
 *     only (charsets.jar) and would re-trigger the same crash, so they -- and
 *     every other unrecognised "Amiga" name -- fall back to ISO8859-1, which is
 *     always loadable (it is the JDK's own ultimate fallback).
 */
#ifndef AMIGA_CHARSET_H
#define AMIGA_CHARSET_H

#include <string.h>

static const char *
amiga_normalize_encoding(const char *enc)
{
    /* Not an AmigaOS vendor charset (NULL, ISO-8859-x, UTF-8, US-ASCII, ...):
     * leave whatever clib4/diskfont reported untouched -- the JDK resolves it. */
    if (enc == 0 || strncmp(enc, "Amiga", 5) != 0)
        return enc;

    /* Vendor "Amiga-NNNN" -> standard-provider Windows code page (rt.jar). */
    if (strcmp(enc, "Amiga-1250") == 0) return "windows-1250";  /* Central European */
    if (strcmp(enc, "Amiga-1251") == 0) return "windows-1251";  /* Cyrillic (reported) */
    if (strcmp(enc, "Amiga-1252") == 0) return "windows-1252";  /* Western */
    if (strcmp(enc, "Amiga-1253") == 0) return "windows-1253";  /* Greek */
    if (strcmp(enc, "Amiga-1254") == 0) return "windows-1254";  /* Turkish */
    if (strcmp(enc, "Amiga-1257") == 0) return "windows-1257";  /* Baltic */

    /* Amiga-1255/1256/1258 (extended-provider only) and any other "Amiga*"
     * name: fall back to the always-loadable standard ISO-8859-1 so the VM
     * boots rather than crashing in the extended charset provider. */
    return "ISO8859-1";
}

#endif /* AMIGA_CHARSET_H */
