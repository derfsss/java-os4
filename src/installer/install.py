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


##############################################
# Create a drawer (and any missing parents).  The Installation Utility does
# not create a non-existent alternatepath, so we make it ourselves.  MakeDir
# on an existing drawer fails harmlessly (output discarded).
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
        amiga.system('MakeDir >NIL: *>NIL: "' + cur + '"')
        cur = cur + "/"


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
# Pages

welcomePage = NewPage(WELCOME)
SetString(welcomePage, "message", loc.GetString(loc.MSG_WELCOME))

destinationPage = NewPage(DESTINATION)
SetString(destinationPage, "message", loc.GetString(loc.MSG_SELECT_DEST))
SetString(destinationPage, "destination", defaultDest)


def installEntryHandler(page):
    dest = GetString(destinationPage, "destination")
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
    addJavaAssign(GetString(destinationPage, "destination"))
    return True


installPage = NewPage(INSTALL)
SetObject(installPage, "entryhandler", installEntryHandler)
SetObject(installPage, "exithandler", installExitHandler)

finalPage = NewPage(FINISH)
SetString(finalPage, "message", loc.GetString(loc.MSG_FINISHED))


##############################################
# Run the wizard.
RunInstaller()
