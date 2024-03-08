# Nmake macros for building Windows 32-Bit apps

#setenv /XP /Release [/x86 | /X64]

!include <win32.mak>

JDK=c:\Programme\Java\jdk1.8.0_121

all: $(OUTDIR) $(OUTDIR)\WinHelper32.dll
x86: $(OUTDIR) $(OUTDIR)\WinHelper32.dll
X64: $(OUTDIR) $(OUTDIR)\WinHelper64.dll

#----- If OUTDIR does not exist, then create directory
$(OUTDIR) :
    if not exist "$(OUTDIR)/$(NULL)" mkdir $(OUTDIR)

# Update the object files if necessary

$(OUTDIR)\WinHelper.obj: WinHelper.cpp WinHelper.h
    $(cc) $(cflags) -EHsc $(cvarsdll) /D "NDEBUG" /D "_WINDLL" /D "_AFXDLL" /D "_UNICODE" /D "UNICODE" /I. /I"$(JDK)\include" /I"$(JDK)\include\win32" /Fo"$(OUTDIR)\\" /Fd"$(OUTDIR)\\" WinHelper.cpp

# Update the dynamic link library

$(OUTDIR)\WinHelper32.dll: $(OUTDIR)\WinHelper.obj
    $(link) $(linkdebug) $(dlllflags) -out:$(OUTDIR)\WinHelper32.dll $(OUTDIR)\WinHelper.obj ole32.lib uuid.lib setupapi.lib Advapi32.lib
    mt -manifest $(OUTDIR)\WinHelper32.dll.manifest -outputresource:$(OUTDIR)\WinHelper32.dll;2

$(OUTDIR)\WinHelper64.dll: $(OUTDIR)\WinHelper.obj
    $(link) $(linkdebug) $(dlllflags) -out:$(OUTDIR)\WinHelper64.dll $(OUTDIR)\WinHelper.obj ole32.lib uuid.lib setupapi.lib comsuppw.lib Advapi32.lib
    mt -manifest $(OUTDIR)\WinHelper64.dll.manifest -outputresource:$(OUTDIR)\WinHelper64.dll;2

#--------------------- Clean Rule --------------------------------------------------------
# Rules for cleaning out those old files
clean:
        $(CLEANUP)
