Name "DataExplorer"

# Check for different external defines
!ifndef VERSION 
    !error "Version not defined!"
!endif

!ifndef DISTDIR
    !error "Dist directory not defined!"
!endif

!ifndef OUTDIR
    !error "Output directory not defined!"
!endif

!ifndef OUTFILENAME
    !error "Output filename not defined!"
!endif

!ifndef EXEFILENAME
    !error "Executable filename not defined!"
!endif

# Our defines
!define URL "https://savannah.nongnu.org/projects/dataexplorer"
!define FILE_EXT1 ".osd"
!define FILE_EXT2 ".lov"
!define FILE_EXT_DESCRIPTION "DataExplorer.DataExplorerFileExtension"
# MUI defines
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\DataExplorer.ico"
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_STARTMENUPAGE_REGISTRY_ROOT HKLM
!define MUI_STARTMENUPAGE_NODISABLE
!define MUI_STARTMENUPAGE_REGISTRY_KEY ${REGKEY}
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULT_FOLDER $(^Name)
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\DataExplorer_uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE

# Included files
!include MUI2.nsh
!include Library.nsh
!include common.nsh
!include Memento.nsh
!include FileAssociation.nsh

# Memento settings
!define MEMENTO_REGISTRY_ROOT HKLM
!define MEMENTO_REGISTRY_KEY ${REGKEY}

# Variables
Var StartMenuGroup

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!define MUI_PAGE_CUSTOMFUNCTION_PRE PageStartmenuPre
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuGroup
!insertmacro MUI_PAGE_INSTFILES
!define MUI_FINISHPAGE_RUN $INSTDIR\${EXEFILENAME}
#!define MUI_FINISHPAGE_SHOWREADME $INSTDIR\README.html
#!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages (have to be inserted after definition of MUI installer pages; otherwise many language strings will be missing)
!include languages.nsh

# Installer attributes
!system "if not exist ${OUTDIR} mkdir ${OUTDIR}"
OutFile "${OUTDIR}\${OUTFILENAME}"

!ifdef 64BIT
    InstallDir $PROGRAMFILES64\$(^Name)
!else
    InstallDir $PROGRAMFILES\$(^Name)
!endif

# Get last used install dir
# The language string $(^Name) doesn't work in InstallDirRegKey because lang strings aren't initialized at the point InstallDirRegKey is called.
# see http://forums.winamp.com/showthread.php?t=288829
InstallDirRegKey HKLM "SOFTWARE\DataExplorer" "InstallDir"

CRCCheck off
RequestExecutionLevel admin
!insertmacro setVersionInfo "${VERSION}" "${APPNAME}"


#======================================================================================================================================================================================================

# Installer sections
Section $(^Name) SecMain

	# Don't allow unchecking of this component
	SectionIn RO

    # Program-Folder
    SetOutPath $INSTDIR
    SetOverwrite ifnewer
    File /r ${DISTDIR}\*

SectionEnd

#------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# Start menu
${MementoSection} "$(nlsSectionStartmenu)" SecStartmenu

    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    SetOutPath $INSTDIR     #is used to define the "start in" entry of the link
    CreateDirectory $SMPROGRAMS\$StartMenuGroup
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\$(^Name).lnk"           "$INSTDIR\${EXEFILENAME}"
#    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\ReadMe.lnk"             "$INSTDIR\README.html"
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\$(nlsUninstall).lnk"    "$INSTDIR\uninstall.exe"
    !insertmacro MUI_STARTMENU_WRITE_END

${MementoSectionEnd}

#------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# Desktop Icon
${MementoSection} "$(nlsSectionDesktop)" SecDesktop

    CreateShortCut "$DESKTOP\$(^Name).lnk" "$INSTDIR\${EXEFILENAME}" "" "$INSTDIR\${EXEFILENAME}" 0

${MementoSectionEnd}

#------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# Icon in quick launch
${MementoSection} "$(nlsSectionQuicklaunch)" SecQuicklaunch

    CreateShortCut "$QUICKLAUNCH\$(^Name).lnk" "$INSTDIR\${EXEFILENAME}" "" "$INSTDIR\${EXEFILENAME}" 0

${MementoSectionEnd}


${MementoSectionDone}

#------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# Various stuff
Section -post

    SetShellVarContext current
    WriteRegStr HKLM "${REGKEY}" "InstallDir" $INSTDIR
    !insertmacro writeUninstallEntry

    # register file extensions
    ${registerExtension} "$INSTDIR\${EXEFILENAME}" "${FILE_EXT1}" "${FILE_EXT_DESCRIPTION}"
    ${registerExtension} "$INSTDIR\${EXEFILENAME}" "${FILE_EXT2}" "${FILE_EXT_DESCRIPTION}"
    
SectionEnd

#======================================================================================================================================================================================================

# Uninstaller sections
Section un.Main

    # Program-Folder
    # Files have to be deleted one by one to not accidentally delete anything else in case
    # the user e.g. specified "c:\program files" as the install dir 
    RmDir /r $INSTDIR\devices
    RmDir /r $INSTDIR\java
    Delete $INSTDIR\README.de
    Delete $INSTDIR\rxtxSerial.dll
    Delete $INSTDIR\WinHelper32.dll
    Delete $INSTDIR\WinHelper64.dll
    Delete $INSTDIR\README.en
    Delete $INSTDIR\DataExplorer.exe
    Delete $INSTDIR\DevicePropertiesEditor.exe
    Delete $INSTDIR\DataExplorer.jar
    Delete $INSTDIR\uninstall.exe
    RmDir /REBOOTOK $INSTDIR

SectionEnd

#------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

Section un.post

    SetShellVarContext current
    
    # NSIS RegistryKeys
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    DeleteRegValue HKLM "${REGKEY}" StartMenuGroup
    DeleteRegValue HKLM "${REGKEY}" "InstallDir"
    DeleteRegKey /IfEmpty HKLM "${REGKEY}"

    # Startmenu
    Delete "$SMPROGRAMS\$StartMenuGroup\$(^Name).lnk"
    Delete "$SMPROGRAMS\$StartMenuGroup\$(nlsUninstall).lnk"
#    Delete "$SMPROGRAMS\$StartMenuGroup\ReadMe.lnk"
    RmDir "$SMPROGRAMS\$StartMenuGroup"

    # Desktop    
    Delete "$DESKTOP\$(^Name).lnk"

    # Quicklaunch    
    Delete "$QUICKLAUNCH\$(^Name).lnk"

    # unregister file extensions
    ${unregisterExtension} "${FILE_EXT1}" "${FILE_EXT_DESCRIPTION}"
    ${unregisterExtension} "${FILE_EXT2}" "${FILE_EXT_DESCRIPTION}"

SectionEnd

#======================================================================================================================================================================================================

# Texts for the description that is shown on the right part of the components screen
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
	!insertmacro MUI_DESCRIPTION_TEXT ${SecMain} "$(nlsDescSectionMain)"
	!insertmacro MUI_DESCRIPTION_TEXT ${SecStartmenu} "$(nlsDescSectionStartmenu)"
	!insertmacro MUI_DESCRIPTION_TEXT ${SecDesktop} "$(nlsDescSectionDesktop)"
    !insertmacro MUI_DESCRIPTION_TEXT ${SecQuicklaunch} "$(nlsDescSectionQuicklaunch)"
!insertmacro MUI_FUNCTION_DESCRIPTION_END

#======================================================================================================================================================================================================

# Installer functions
Function .onInit
    !insertmacro checkAppRunning ${EXEFILENAME} "$(nlsAppRunning)"

    # for testing only
    # !insertmacro MUI_LANGDLL_DISPLAY
    
    InitPluginsDir
    
    # restore components selected last time
	${MementoSectionRestore}
    
FunctionEnd

# Save memento settings after successful install
Function .onInstSuccess
	${MementoSectionSave}
FunctionEnd

# Uninstaller functions
Function un.onInit
	!insertmacro checkAppRunning ${EXEFILENAME} "$(nlsAppRunning)"
    ReadRegStr $INSTDIR HKLM "${REGKEY}" "InstallDir"
    ReadRegStr $StartMenuGroup HKLM "${REGKEY}" StartMenuGroup
FunctionEnd

# Check whether the section for the startmenu is marked for installation
Function PageStartmenuPre
	!insertmacro SectionFlagIsSet ${SecStartmenu} ${SF_SELECTED} End ""
	Abort
	End:
FunctionEnd
