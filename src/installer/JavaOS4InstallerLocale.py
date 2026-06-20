#
# Java-OS4 installer locale (Python 2.5).
#
# English strings with optional catalog override, in the AmigaOS 4.1
# Installation Utility locale style.  No translated catalogs ship yet;
# OpenCatalog failing is handled gracefully and the built-in strings are used.
#
import catalog


class JavaOS4InstallerLocale:
    strings = {}
    cat = None

    MSG_WELCOME = 1
    MSG_SELECT_DEST = 2
    MSG_FINISHED = 3

    def __init__(self, language="", catalogName="JavaOS4.catalog",
                 builtinLanguage="english"):
        self.strings[self.MSG_WELCOME] = (
            "Welcome to the Java-OS4 installer.\n\n"
            "This installs a Java 8 runtime for AmigaOS 4 (JamVM 2.0 plus the "
            "OpenJDK 8 class library), including a Swing/AWT toolkit so Java "
            "GUIs run in Workbench windows.\n\n"
            "The next page lets you choose where to install it. A JAVA: assign "
            "will be added to S:User-Startup so the runtime is available after "
            "every reboot.\n\n"
            "Press \"Next\" to continue.")

        self.strings[self.MSG_SELECT_DEST] = (
            "Choose the drawer to install Java-OS4 into. It will be created if "
            "it does not exist, and JAVA: will be assigned to it.")

        self.strings[self.MSG_FINISHED] = (
            "Java-OS4 has been installed. JAVA: is assigned now (no reboot "
            "needed) and added to S:User-Startup, and the 'java' command was "
            "copied to C: so it runs from any Shell.\n\n"
            "Try it from a Shell:\n\n"
            "    java -version\n"
            "    java -cp examples/HelloJava.jar HelloJava\n"
            "    java -cp examples/SwingDemo.jar SwingDemo\n\n"
            "Swing and AWT applications need no extra options. (Java-OS4 needs "
            "clib4.library 2.1+ in LIBS:; the installer added the bundled copy "
            "if it was missing.)")

        try:
            self.cat = catalog.OpenCatalog(catalogName, language,
                                           builtinLanguage)
        except:
            self.cat = None

    def GetString(self, id):
        if self.cat != None:
            return self.cat.GetString(id, self.strings[id])
        return self.strings[id]
