# Nmake macros for building Windows 32-Bit apps

#setenv /XP /Release [/x86 | /X64]
!include <win32.mak>

all: $(OUTDIR) $(OUTDIR)\Register32.exe 
x86: $(OUTDIR) $(OUTDIR)\Register32.exe 
X64: $(OUTDIR) $(OUTDIR)\Register64.exe 

#----- If OUTDIR does not exist, then create directory
$(OUTDIR) :
    if not exist "$(OUTDIR)/$(NULL)" mkdir $(OUTDIR)

$(OUTDIR)\RegistryHelper.obj: RegistryHelper.cpp
    $(cc) /GL -DWIN32 -D_WIN32 -W3 -D_WINNT -D_WIN32_WINNT=0x0501 -DNTDDI_VERSION=0x05010000 -D_WIN32_IE=0x0600 -DWINVER=0x0501 /D "NDEBUG" /D "_UNICODE" /D "UNICODE" /FD /EHa /MD /Fo"$(OUTDIR)\\" /Fd"$(OUTDIR)\\" /W3 /c /Zi /clr /TP /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.dll" /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.Data.dll" /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.XML.dll" ./RegistryHelper.cpp

$(OUTDIR)\AssemblyInfo.obj: AssemblyInfo.cpp stdafx.h
    $(cc) /GL -DWIN32 -D_WIN32 -W3 -D_WINNT -D_WIN32_WINNT=0x0501 -DNTDDI_VERSION=0x05010000 -D_WIN32_IE=0x0600 -DWINVER=0x0501 /D "NDEBUG" /D "_UNICODE" /D "UNICODE" /FD /EHa /MD /Fo"$(OUTDIR)\\" /Fd"$(OUTDIR)\\" /W3 /c /Zi /clr /TP /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.dll" /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.Data.dll" /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.XML.dll" ./AssemblyInfo.cpp


	
#cl -c -DCRTAPI1=_cdecl -DCRTAPI2=_cdecl -nologo -GS -D_X86_=1  
#	-DWIN32 -D_WIN32 -W3 -D_WINNT -D_WIN32_WINNT=0x0501 -DNTDDI_VERSION=0x05010000 -D_WIN32_IE=0x0600 -DWINVER=0x0501  -D_MT -MTd /WX /Fo"XP32_DEBUG\\" /Fd"XP32_DEBUG\\" RegistryHelper.cpp
#/Od /D "WIN32" /D "_DEBUG" /D "_UNICODE" /D "UNICODE" /FD /EHa /MDd /Fo"Debug\\" /Fd"Debug\vc90.pdb" /W3 /nologo /c /Zi /clr /TP /errorReport:prompt /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.dll" /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.Data.dll" /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.XML.dll"
#/GL /D "WIN32" /D "NDEBUG" /D "_UNICODE" /D "UNICODE" /FD /EHa /MD /Fo"Release\\" /Fd"Release\vc90.pdb" /W3 /nologo /c /Zi /clr /TP /errorReport:prompt /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.dll" /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.Data.dll" /FU "c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\System.XML.dll"

$(OUTDIR)\Register32.exe: $(OUTDIR)\RegistryHelper.obj $(OUTDIR)\AssemblyInfo.obj
    $(link) /OUT:$(OUTDIR)\Register32.exe $(conflags) /VERSION:1.70 /INCREMENTAL:NO /MANIFEST /MANIFESTFILE:"$(OUTDIR)\Register32.exe.manifest" /MANIFESTUAC:"level='requireAdministrator' uiAccess='false'" $(OUTDIR)\RegistryHelper.obj $(OUTDIR)\AssemblyInfo.obj $(conlibs)
	mt -manifest $(OUTDIR)\Register32.exe.manifest -outputresource:$(OUTDIR)\Register32.exe
	
$(OUTDIR)\Register64.exe: $(OUTDIR)\RegistryHelper.obj $(OUTDIR)\AssemblyInfo.obj
    $(link) /OUT:$(OUTDIR)\Register64.exe $(conflags) /VERSION:1.70 /INCREMENTAL:NO /MANIFEST /MANIFESTFILE:"$(OUTDIR)\Register64.exe.manifest" /MANIFESTUAC:"level='requireAdministrator' uiAccess='false'" $(OUTDIR)\RegistryHelper.obj $(OUTDIR)\AssemblyInfo.obj $(conlibs)
	mt -manifest $(OUTDIR)\Register64.exe.manifest -outputresource:$(OUTDIR)\Register64.exe

#/LTCG /DYNAMICBASE /FIXED:No /NXCOMPAT
#--------------------- Clean Rule --------------------------------------------------------
# Rules for cleaning out those old files
clean:
        $(CLEANUP)