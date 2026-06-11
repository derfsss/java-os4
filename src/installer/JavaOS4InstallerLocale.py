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
            "Java-OS4 has been installed and the JAVA: assign added to "
            "S:User-Startup.\n\n"
            "Try it from a Shell:\n\n"
            "    java -version\n"
            "    java -cp myapp.jar Main\n\n"
            "Swing and AWT applications need no extra options.")

        try:
            self.cat = catalog.OpenCatalog(catalogName, language,
                                           builtinLanguage)
        except:
            self.cat = None

    def GetString(self, id):
        if self.cat != None:
            return self.cat.GetString(id, self.strings[id])
        return self.strings[id]
