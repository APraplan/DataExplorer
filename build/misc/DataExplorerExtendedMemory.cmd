@REM Skript to start DataExplorer with extended memory to avoid java heap space at 65 bit Windows system
@REM Copy into DataExplorer installation directory before execution
@java -D64 -Dfile.encoding=UTF-8 -jar -Xms64m -Xmx5092m DataExplorer.jar