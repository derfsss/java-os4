#
# $VER: Java-OS4 Install @VERSION@ (@DATE@)
#
##############################################
# Java-OS4 installer.
#
# Runs under Sys:Utilities/Installation Utility (Python 2.5).  It asks where
# to install the runtime, copies it there, and adds a permanent JAVA: assign
# to S:User-Startup.  Launch by double-clicking the drawer icon, or:
#   Sys:Utilities/Installation Utility PACKAGE=install.py
##############################################

from installer import *
from JavaOS4InstallerLocale import *
import amiga

# Locale instance.
loc = JavaOS4InstallerLocale()

# Default install drawer (created if it does not exist).
defaultDest = "SYS:Java"

# The chosen install drawer, captured + normalised at the install step and reused
# by every post-copy step (icon / JAVA: assign / protection bits / clib4) so they
# all act on the SAME path the files were copied to.  The Installation Utility
# hands the destination back with a TRAILING SLASH (e.g. "SYS:Java/"), so the old
# `home + "/java"` built "SYS:Java//java" -- AmigaDOS reads "//" as the parent
# dir, giving "SYS:java", so Protect failed with "object not found" (the copy and
# the JAVA: assign happened to tolerate the slash).  rstrip("/") fixes it.
chosenDest = [defaultDest]


##############################################
# Does a file/drawer exist?
def pathExists(path):
    try:
        amiga.stat(path)
        return True
    except:
        return False


##############################################
# Create a drawer (and any missing parents).  The Installation Utility does
# not create a non-existent alternatepath, so we make it ourselves.  Only
# missing components are created -- MakeDir on an existing drawer returns a
# warning (rc 10) that the installer would surface.
def makeDirs(path):
    if path == "":
        return
    colon = path.find(":")
    if colon < 0:
        base = ""
        rest = path
    else:
        base = path[:colon + 1]     # e.g. "Work:"
        rest = path[colon + 1:]     # e.g. "Apps/Java"
    cur = base
    for part in rest.split("/"):
        if part == "":
            continue
        cur = cur + part
        if not pathExists(cur):
            amiga.system('MakeDir "' + cur + '"')
        cur = cur + "/"


##############################################
# Give the install drawer a Workbench icon, so it appears on the desktop.
# The icon is a sibling file "<drawer>.info" in the parent drawer (the
# standard AmigaOS way to icon a drawer).  Don't clobber an existing icon.
def addDrawerIcon(dest):
    if dest == "" or dest[-1:] == ":":
        return
    icon = dest + ".info"
    if not pathExists(icon):
        amiga.system('Copy >NIL: *>NIL: "content/drawer.info" "' + icon + '"')


##############################################
# Add a permanent JAVA: assign for the chosen drawer.
#
# Assigns live for this session, then rewrites S:User-Startup so the assign
# survives reboots -- replacing any earlier JAVA: assign so re-installing to a
# new location does not leave a stale one behind.
def addJavaAssign(home):
    amiga.system('Assign >NIL: JAVA: "' + home + '"')

    startup = "S:User-Startup"
    line = 'Assign >NIL: JAVA: "' + home + '"'
    try:
        f = open(startup, "r")
        data = f.read()
        f.close()
    except:
        data = ""

    out = []
    for l in data.split("\n"):
        s = l.strip().lower()
        if s[:6] == "assign" and s.find("java:") >= 0:
            continue
        out.append(l)
    while len(out) > 0 and out[-1].strip() == "":
        out = out[:-1]
    out.append(line)

    try:
        f = open(startup, "w")
        f.write("\n".join(out) + "\n")
        f.close()
    except:
        pass


##############################################
# Set the protection bits the .lha + Installation-Utility copy do not carry,
# and put `java` on the command path.
#
#  - `java` is an AmigaDOS script: it needs the 's' (script) bit to run by
#    name (without it the user must `protect java +s` by hand);
#  - jamvm-openjdk is the ELF VM binary: it needs the 'e' (execute) bit;
#  - a C:java copy makes `java` run from any Shell, not just the install
#    drawer (the launcher CDs into JAVA: itself, so the copy works anywhere).
def protectRuntime(home):
    amiga.system('Protect >NIL: "' + home + '/java" +s')
    amiga.system('Protect >NIL: "' + home + '/jamvm-openjdk" +e')
    amiga.system('Copy >NIL: "' + home + '/java" C:java')
    amiga.system('Protect >NIL: C:java +s')


##############################################
# clib4.library is the C runtime the VM and the bundled .so stubs call into.
# A fresh AmigaOS 4 machine may not have it, so install the bundled copy to
# LIBS: when it is absent.  An existing clib4.library is left untouched -- it
# may be newer, and other applications may depend on it; the bundled copy stays
# in the install drawer for a manual update.  Java-OS4 needs clib4 2.1+.
def installClib4(home):
    src = home + "/clib4.library"
    if pathExists(src) and not pathExists("LIBS:clib4.library"):
        amiga.system('Copy >NIL: "' + src + '" "LIBS:clib4.library"')
        amiga.system('Protect >NIL: "LIBS:clib4.library" +rwed')


##############################################
# Pages

welcomePage = NewPage(WELCOME)
SetString(welcomePage, "message", loc.GetString(loc.MSG_WELCOME))

destinationPage = NewPage(DESTINATION)
SetString(destinationPage, "message", loc.GetString(loc.MSG_SELECT_DEST))
SetString(destinationPage, "destination", defaultDest)


def installEntryHandler(page):
    dest = GetString(destinationPage, "destination").rstrip("/")
    chosenDest[0] = dest        # normalised chosen path, reused by the exit handler
    # Create the drawer first -- the Installation Utility will not create a
    # missing alternatepath.
    makeDirs(dest)
    AddPackage(FILEPACKAGE,
        name = "Java-OS4",
        files = ["content/Java/"],
        alternatepath = dest)
    return True


def installExitHandler(page, direction):
    if (direction != 1):
        return True
    dest = chosenDest[0]
    addDrawerIcon(dest)
    addJavaAssign(dest)
    protectRuntime(dest)
    installClib4(dest)
    return True


installPage = NewPage(INSTALL)
SetObject(installPage, "entryhandler", installEntryHandler)
SetObject(installPage, "exithandler", installExitHandler)

finalPage = NewPage(FINISH)
SetString(finalPage, "message", loc.GetString(loc.MSG_FINISHED))


##############################################
# Run the wizard.
RunInstaller()
