/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
   
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
// RegistryHelper.cpp : main project file.

#include <iostream>
using namespace std;
using namespace System;
using namespace System::Security;
using namespace Microsoft::Win32;

void PrintKeys( RegistryKey ^ rkey )
{

   // Retrieve all the subkeys for the specified key.
   array<String^>^names = rkey->GetSubKeyNames();
   int icount = 0;
   Console::WriteLine( "Subkeys of {0}", rkey->Name );
   Console::WriteLine( "-----------------------------------------------" );

   // Print the contents of the array to the console.
   System::Collections::IEnumerator^ enum0 = names->GetEnumerator();
   while ( enum0->MoveNext() )
   {
      String^ s = safe_cast<String^>(enum0->Current);
      Console::WriteLine( s );

      // The following code puts a limit on the number
      // of keys displayed.  Comment it out to print the
      // complete list.
      icount++;
      if ( icount >= 500 )
            break;
   }
}


void registerDataExplorerFileExtension(String ^ osdeBasePath)
{
/*	
	[HKEY_CLASSES_ROOT\.osd]
	@="DataExplorer.DataExplorerFileExtension"

	[HKEY_CLASSES_ROOT\DataExplorer.DataExplorerFileExtension]
	@="DataExplorer.DataExplorerFileExtension"

	[HKEY_CLASSES_ROOT\Applications\DataExplorer.exe\shell\open\command]
	@="\"@GDE_DIR@\DataExplorer.exe\" \"%1\""
	#---------------------------------------------------------------------------
*/
 	RegistryKey ^ cl_root_osd = Registry::ClassesRoot->OpenSubKey(".osd", true);
	if(!cl_root_osd) 
		cl_root_osd = Registry::ClassesRoot->CreateSubKey(".osd");
	cl_root_osd->SetValue("", "DataExplorer.DataExplorerFileExtension", RegistryValueKind::String);

	RegistryKey ^ cl_root_osde = Registry::ClassesRoot->OpenSubKey("DataExplorer.DataExplorerFileExtension", true);
	if(!cl_root_osde) 
		cl_root_osde = Registry::ClassesRoot->CreateSubKey("DataExplorer.DataExplorerFileExtension");
	cl_root_osde->SetValue("", "DataExplorer.DataExplorerFileExtension", RegistryValueKind::String);

	RegistryKey ^ cr_app = Registry::ClassesRoot->OpenSubKey("Applications", true);
	RegistryKey ^ osdeApp	= cr_app->OpenSubKey("DataExplorer.exe", true);
	if(!osdeApp) 
		osdeApp = cr_app->CreateSubKey("DataExplorer.exe");

	RegistryKey ^ shell = osdeApp->CreateSubKey("shell");
	RegistryKey ^ open  = shell->CreateSubKey("open");
	RegistryKey ^ cmd	= open->CreateSubKey("command");
	cmd->SetValue("", "\"" + osdeBasePath + "\\DataExplorer.exe\" \"%1\"", RegistryValueKind::String);
	//Console::WriteLine( "\"" + osdeBasePath + "\\DataExplorer.exe\" \"%1\"" );

/*
	[HKEY_LOCAL_MACHINE\SOFTWARE\Classes\.osd]
	@="DataExplorer.DataExplorerFileExtension"

	[HKEY_LOCAL_MACHINE\SOFTWARE\Classes\DataExplorer.DataExplorerFileExtension]
	@="DataExplorer.DataExplorerFileExtension"

	[HKEY_LOCAL_MACHINE\SOFTWARE\Classes\DataExplorer.DataExplorerFileExtension\shell\open\command]
	@="\"@GDE_DIR@\DataExplorer.exe\" \"%1\""

	#--------------------------------------------------------------------------- 
*/
	RegistryKey ^ lm_sw_cl = Registry::LocalMachine->OpenSubKey("Software")->OpenSubKey("Classes", true);
 	RegistryKey ^ lm_cl_osd = lm_sw_cl->OpenSubKey(".osd", true);
	if(!lm_cl_osd) 
		lm_cl_osd = lm_sw_cl->CreateSubKey(".osd");
	lm_cl_osd->SetValue("", "DataExplorer.DataExplorerFileExtension", RegistryValueKind::String);

	RegistryKey ^ lm_cl_osde = lm_sw_cl->OpenSubKey("DataExplorer.DataExplorerFileExtension", true);
	if(lm_cl_osde) 
		lm_sw_cl->DeleteSubKeyTree("DataExplorer.DataExplorerFileExtension");
	lm_cl_osde = lm_sw_cl->CreateSubKey("DataExplorer.DataExplorerFileExtension");
	lm_cl_osde->SetValue("", "DataExplorer.DataExplorerFileExtension", RegistryValueKind::String);
	shell = lm_cl_osde->CreateSubKey("shell");
	open  = shell->CreateSubKey("open");
	cmd	= open->CreateSubKey("command");
	cmd->SetValue("", "\"" + osdeBasePath + "\\DataExplorer.exe\" \"%1\"", RegistryValueKind::String);

/*
	[HKEY_CURRENT_USER\Software\Classes\Applications\DataExplorer.exe\shell\open\command]
	@="\"@GDE_DIR@\DataExplorer.exe\" \"%1\""

	[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.osd]
	"Progid"="DataExplorer.DataExplorerFileExtension"

	[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.osd\OpenWithList]
	"a"="DataExplorer.exe"
	"MRUList"="a"

	[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.osd\OpenWithProgids]
	"DataExplorer.DataExplorerFileExtension"=hex(0):

	[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.lov\OpenWithList]
	"a"="LogView.exe"
	"b"="DataExplorer.exe"
	"MRUList"="ab"

	[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.lov\OpenWithProgids]
	"DataExplorer.DataExplorerFileExtension"=hex(0):
*/
	RegistryKey ^ cu_sw_cl_app = Registry::CurrentUser->OpenSubKey("Software")->OpenSubKey("Classes")->OpenSubKey("Applications", true);
	RegistryKey ^ exist	= cu_sw_cl_app->OpenSubKey("DataExplorer.exe", true);
	if(exist) 
		cu_sw_cl_app->DeleteSubKeyTree("DataExplorer.exe");

	osdeApp = cu_sw_cl_app->CreateSubKey("DataExplorer.exe");
	shell = osdeApp->CreateSubKey("shell");
	open  = shell->CreateSubKey("open");
	cmd	= open->CreateSubKey("command");
	cmd->SetValue("", "\"" + osdeBasePath + "\\DataExplorer.exe\" \"%1\"");

	RegistryKey ^ cu_sw_ms_win_cv_ex_ext = Registry::CurrentUser->OpenSubKey("Software")->OpenSubKey("Microsoft")->OpenSubKey("Windows")->OpenSubKey("CurrentVersion")->OpenSubKey("Explorer")->OpenSubKey("FileExts", true);
	exist	= cu_sw_ms_win_cv_ex_ext->OpenSubKey(".osd", true);
	if(exist) 
		cu_sw_ms_win_cv_ex_ext->DeleteSubKeyTree(".osd");

	RegistryKey ^ osdxExt = cu_sw_ms_win_cv_ex_ext->CreateSubKey(".osd");
	RegistryKey ^ openWithList = osdxExt->CreateSubKey("OpenWithList");
	openWithList->SetValue("a", "DataExplorer.exe");
	openWithList->SetValue("MRUList", "a");
	RegistryKey ^ openWithProgids = osdxExt->CreateSubKey("OpenWithProgids");
	openWithProgids->SetValue("DataExplorer.DataExplorerFileExtension", gcnew array<Byte>{});
	
	Boolean isOSDE = false;
	array<String^> ^keys = gcnew array<String^>{"a", "b", "c", "d", "e", "f", "g", "h"};
	RegistryKey ^ lovExt	= cu_sw_ms_win_cv_ex_ext->OpenSubKey(".lov");
	if(lovExt) { // .lov exist , check if there is already an entry for DataExplorer
		RegistryKey ^ openWithList = lovExt->OpenSubKey("OpenWithList", true);
		if(openWithList) {
			array<String^>^names = openWithList->GetValueNames();
			System::Collections::IEnumerator^ enum0 = names->GetEnumerator();
			while ( enum0->MoveNext() )	{
				String^ s = safe_cast<String^>(enum0->Current);
				//Console::WriteLine( s );
				if (s->Length >= 1) {
					String ^ tmp = safe_cast<String^>(openWithList->GetValue(s));
					//Console::WriteLine( tmp );
					isOSDE = (tmp == "DataExplorer.exe");
					if(isOSDE)
						break;
				}
			}
			if (!isOSDE) {
				int listCount = names->Length - 1; //MRUList, Default
				openWithList->SetValue(safe_cast<String^>(keys[listCount]), "DataExplorer.exe");
				String ^ mruList = safe_cast<String^>(openWithList->GetValue("MRUList"));
				openWithList->SetValue("MRUList", mruList + keys[listCount]);
			}
		}
		else { // OpenWithList does not exist -> create initial with DataExplorer only
			openWithList = lovExt->CreateSubKey("OpenWithList");
			openWithList->SetValue("a", "DataExplorer.exe", RegistryValueKind::String);
			openWithList->SetValue("MRUList", "a", RegistryValueKind::String);
		}

		isOSDE = false;
		RegistryKey ^ openWithProgids = lovExt->OpenSubKey("OpenWithProgids", true);
		if(openWithProgids) {
			array<String^>^names = openWithProgids->GetValueNames();
			System::Collections::IEnumerator^ enum0 = names->GetEnumerator();
			while ( enum0->MoveNext() )	{
				String^ s = safe_cast<String^>(enum0->Current);
				//Console::WriteLine( s );
				if (s->Length >= 1) {
					isOSDE = (s == "DataExplorer.DataExplorerFileExtension");
					if(isOSDE)
						break;
				}
			}
			if (!isOSDE) {
				openWithProgids->SetValue("DataExplorer.DataExplorerFileExtension", gcnew array<Byte>{});
			}
		}
		else { // OpenWithList does not exist -> create initial with DataExplorer only
			openWithProgids = lovExt->CreateSubKey("OpenWithProgids");
			openWithProgids->SetValue("DataExplorer.DataExplorerFileExtension", gcnew array<Byte>{});
		}
	} // end if lovExt
}

void unregisterDataExplorerFileExtension() {
/*	
	[HKEY_CLASSES_ROOT\.osd]
	@="DataExplorer.DataExplorerFileExtension"

	[HKEY_CLASSES_ROOT\DataExplorer.DataExplorerFileExtension]
	@="DataExplorer.DataExplorerFileExtension"

	[HKEY_CLASSES_ROOT\Applications\DataExplorer.exe\shell\open\command]
	@="\"@GDE_DIR@\DataExplorer.exe\" \"%1\""
	#---------------------------------------------------------------------------
*/
 	RegistryKey ^ cl_root_osd = Registry::ClassesRoot->OpenSubKey(".osd", true);
	if(cl_root_osd) 
		Registry::ClassesRoot->DeleteSubKey(".osd");

	RegistryKey ^ cl_root_osde = Registry::ClassesRoot->OpenSubKey("DataExplorer.DataExplorerFileExtension", true);
	if(cl_root_osde) 
		Registry::ClassesRoot->DeleteSubKeyTree("DataExplorer.DataExplorerFileExtension");

	RegistryKey ^ cr_app = Registry::ClassesRoot->OpenSubKey("Applications", true);
	RegistryKey ^ osdeApp = cr_app->OpenSubKey("DataExplorer.exe", true);
 	if(osdeApp) 
		cr_app->DeleteSubKeyTree("DataExplorer.exe");

/*
	[HKEY_LOCAL_MACHINE\SOFTWARE\Classes\.osd]
	@="DataExplorer.DataExplorerFileExtension"

	[HKEY_LOCAL_MACHINE\SOFTWARE\Classes\DataExplorer.DataExplorerFileExtension]
	@="DataExplorer.DataExplorerFileExtension"

	[HKEY_LOCAL_MACHINE\SOFTWARE\Classes\DataExplorer.DataExplorerFileExtension\shell\open\command]
	@="\"@GDE_DIR@\DataExplorer.exe\" \"%1\""

	#--------------------------------------------------------------------------- 
*/
	RegistryKey ^ lm_sw_cl = Registry::LocalMachine->OpenSubKey("Software")->OpenSubKey("Classes", true);
 	RegistryKey ^ lm_cl_osd = lm_sw_cl->OpenSubKey(".osd", true);
	if(lm_cl_osd) 
		lm_sw_cl->DeleteSubKeyTree(".osd");

	RegistryKey ^ lm_cl_osde = lm_sw_cl->OpenSubKey("DataExplorer.DataExplorerFileExtension", true);
	if(lm_cl_osde) 
		lm_sw_cl->DeleteSubKeyTree("DataExplorer.DataExplorerFileExtension");

/*
	[HKEY_CURRENT_USER\Software\Classes\Applications\DataExplorer.exe\shell\open\command]
	@="\"@GDE_DIR@\DataExplorer.exe\" \"%1\""

	[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.osd]
	"Progid"="DataExplorer.DataExplorerFileExtension"

	[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.osd\OpenWithList]
	"a"="DataExplorer.exe"
	"MRUList"="a"

	[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.osd\OpenWithProgids]
	"DataExplorer.DataExplorerFileExtension"=hex(0):

	[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.lov\OpenWithList]
	"a"="LogView.exe"
	"b"="DataExplorer.exe"
	"MRUList"="ab"

	[HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.lov\OpenWithProgids]
	"DataExplorer.DataExplorerFileExtension"=hex(0):
*/
	RegistryKey ^ cu_sw_cl_app = Registry::CurrentUser->OpenSubKey("Software")->OpenSubKey("Classes")->OpenSubKey("Applications", true);
	RegistryKey ^ exist	= cu_sw_cl_app->OpenSubKey("DataExplorer.exe", true);
	if(exist) 
		cu_sw_cl_app->DeleteSubKeyTree("DataExplorer.exe");

	RegistryKey ^ cu_sw_ms_win_cv_ex_ext = Registry::CurrentUser->OpenSubKey("Software")->OpenSubKey("Microsoft")->OpenSubKey("Windows")->OpenSubKey("CurrentVersion")->OpenSubKey("Explorer")->OpenSubKey("FileExts", true);
	exist	= cu_sw_ms_win_cv_ex_ext->OpenSubKey(".osd", true);
	if(exist) 
		cu_sw_ms_win_cv_ex_ext->DeleteSubKeyTree(".osd");

	Boolean isOSDE = false;
	array<String^> ^keys = gcnew array<String^>{"a", "b", "c", "d", "e", "f", "g", "h"};
	RegistryKey ^ lovExt	= cu_sw_ms_win_cv_ex_ext->OpenSubKey(".lov");
	if(lovExt) { // .lov exist , check if there is an entry for DataExplorer
		RegistryKey ^ openWithList = lovExt->OpenSubKey("OpenWithList", true);
		RegistryKey ^ openWithProgids = lovExt->OpenSubKey("OpenWithProgids", true);

		if(openWithList) {
			array<String^>^names = openWithList->GetValueNames();
			System::Collections::IEnumerator^ enum0 = names->GetEnumerator();
			while ( enum0->MoveNext() )	{
				String^ s = safe_cast<String^>(enum0->Current);
				//Console::WriteLine( s );
				if (s->Length >= 1) {
					String ^ tmp = safe_cast<String^>(openWithList->GetValue(s));
					//Console::WriteLine( tmp );
					isOSDE = (tmp == "DataExplorer.exe");
					if(isOSDE)
						break;
				}
			}
			if (names->Length == 2 && isOSDE) { // OSDE is the only once
				cu_sw_ms_win_cv_ex_ext->DeleteSubKeyTree(".lov");
			}
			else {
				int listCount = isOSDE ? names->Length - 2 : names->Length - 1; //MRUList
				String ^mruList = "a";
				for(int i = 1; i < listCount; ++i) {
					mruList = mruList + keys[listCount];
				}
				openWithList->SetValue("MRUList", mruList);
				if (isOSDE) {
					String ^ osdeKey = keys[names->Length - 2];
					openWithList->DeleteValue(osdeKey); //"b", "DataExplorer.exe"
					openWithProgids->DeleteValue("DataExplorer.DataExplorerFileExtension");
				}
			}
		}
	} // end if lovExt
}

int main(array<System::String ^> ^args)
{
	int rc = 0;
	try {
		if (args->Length > 0) {
			if (args[0] && args[0]->Length > 5)
				Console::WriteLine( "registerDataExplorerFileExtension(" + args[0]+ ")" );
				registerDataExplorerFileExtension(args[0]);
		}
		else {	
			Console::WriteLine( "unregisterDataExplorerFileExtension()" );
			unregisterDataExplorerFileExtension();
		}
	}
	catch(System::Security::SecurityException ^ pEx) { 
		Console::WriteLine( "SecurityException: " + pEx->Message );
		rc = 740;
	}
	catch(System::UnauthorizedAccessException ^ pEx) { 
		Console::WriteLine( "UnauthorizedAccessException: " + pEx->Message );
		rc = 740;
	}
	catch(System::ArgumentException ^ pEx) { 
		Console::WriteLine( "ArgumentException: " + pEx->Message );
	}
	catch(System::NullReferenceException ^ pEx) { 
		Console::WriteLine( "NullReferenceException: " + pEx->Message );
	}

	return rc;
}
