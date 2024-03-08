@REM ***** script to launch DataExplorer ** 27 Aug 2022 *****

@REM add path to self contained JRE, require to be exec from installation directory
@set PATH=%cd%\runtime\bin;%PATH%
@echo %PATH%

@echo java -Dfile.encoding=UTF-8 -jar -Xms64m -Xmx3092m ./DataExplorer.jar
@java -Dfile.encoding=UTF-8 -jar -Xms64m -Xmx3092m ./DataExplorer.jar
