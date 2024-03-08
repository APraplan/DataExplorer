# Global settings
SetCompressor /SOLID LZMA
AllowSkipFiles off
XPStyle on
ShowInstDetails show
ShowUninstDetails show
BGGradient off

# Global defines
!define REGKEY "SOFTWARE\$(^Name)" 
!define COMPANY ""

# --------------------------------------------------------------------------------------------------------
# Checks if the script was passed a version number
!macro isVersionSet

    # Version not specified/empty?
    !define Empty_${VERSION}
    !ifndef Empty_
        !define VERSION_NOT_EMPTY
    !endif
    
    # Cancel if version was not specified
    !ifndef VERSION | VERSION_NOT_EMPTY
        !error "Version number not defined!"
    !endif
    
!macroend

# --------------------------------------------------------------------------------------------------------
# Checks if the to be installed program is still running
# Requires the plugin FindProcDLL.dll (s. http://nsis.sourceforge.net/FindProcDLL_plug-in)
!macro checkAppRunning PROCESSNAME TEXT
    
    FindProcDLL::FindProc ${PROCESSNAME}
    StrCmp $R0 "1" 0 ok
	MessageBox MB_OK|MB_ICONEXCLAMATION ${TEXT} /SD IDOK
    Abort
    ok:
     
!macroend

# --------------------------------------------------------------------------------------------------------
# Create setup´s version info 
!macro setVersionInfo VERSION APPNAME

    VIProductVersion "${VERSION}.0"
    VIAddVersionKey ProductName "${APPNAME}"
    VIAddVersionKey ProductVersion "${VERSION}"
    VIAddVersionKey CompanyName "${COMPANY}"
    VIAddVersionKey CompanyWebsite "${URL}"
    VIAddVersionKey FileVersion ""
    VIAddVersionKey FileDescription ""
    VIAddVersionKey LegalCopyright ""

!macroend

# --------------------------------------------------------------------------------------------------------
# Create uninstaller entry for the control panel 
!macro writeUninstallEntry

    WriteUninstaller $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${VERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" Publisher "${COMPANY}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${URL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1

!macroend

