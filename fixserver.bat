@echo off
title FIX Server for LoadRunner Testing
echo ========================================
echo    FIX Server Startup Script
echo ========================================
echo.

@echo off
title FIX Server - Java 8 Compatibility
echo.

REM Setup the version of JDK
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202
set PATH=%JAVA_HOME%\bin;%PATH%


REM Config the path variable
set QUICKFIXJ_HOME=E:\Loadrunner_basic\scripts\JavaVuser_QuickFixJ\JavaVuser_QuickFixJ
set LIB_DIR=%QUICKFIXJ_HOME%\org.quickfixj-2.2.1-bin\lib
set CONFIG_FILE=fixserver.cfg
set STORE_DIR=server_store
set LOG_DIR=server_log

REM Config JAR files
set QUICKFIXJ_CORE=%LIB_DIR%\quickfixj-core-2.2.1.jar
set QUICKFIXJ_MSG=%LIB_DIR%\quickfixj-messages-fix44-2.2.1.jar
set MINA_CORE=%LIB_DIR%\mina-core-2.1.3.jar
set SLF4J_API=%LIB_DIR%\slf4j-api-1.7.30.jar


REM Config the class path
set JARS=%QUICKFIXJ_CORE%;%QUICKFIXJ_MSG%;%MINA_CORE%;%SLF4J_API%;%SLF4J_IMPL%

echo [Configuration]
echo QuickFIXJ Home: %QUICKFIXJ_HOME%
echo Config File: %CONFIG_FILE%
echo Store Dir: %STORE_DIR%
echo Log Dir: %LOG_DIR%
echo.

REM Check if JAR file exists
if not exist "%QUICKFIXJ_CORE%" (
    echo    ERROR: quickfixj-core-2.2.1.jar not found at:
    echo    %QUICKFIXJ_CORE%
    goto :error
)

if not exist "%QUICKFIXJ_MSG%" (
    echo    ERROR: quickfixj-messages-fix44-2.2.1.jar not found at:
    echo    %QUICKFIXJ_MSG%
    goto :error
)

echo  All required JAR files found
echo.

REM Create log folders
if not exist "%STORE_DIR%" (
    mkdir "%STORE_DIR%"
    echo  Created directory: %STORE_DIR%
)

if not exist "%LOG_DIR%" (
    mkdir "%LOG_DIR%"
    echo  Created directory: %LOG_DIR%
)

REM Generate config file if it doesn't exist
if not exist "%CONFIG_FILE%" (
    echo [DEFAULT] > "%CONFIG_FILE%"
    echo ConnectionType=acceptor >> "%CONFIG_FILE%"
    echo SenderCompID=FIX_SERVER >> "%CONFIG_FILE%"
    echo TargetCompID=LOADRUNNER_CLIENT >> "%CONFIG_FILE%"
    echo FileStorePath=%STORE_DIR% >> "%CONFIG_FILE%"
    echo FileLogPath=%LOG_DIR% >> "%CONFIG_FILE%"
    echo. >> "%CONFIG_FILE%"
    echo [SESSION] >> "%CONFIG_FILE%"
    echo BeginString=FIX.4.4 >> "%CONFIG_FILE%"
    echo HeartBtInt=30 >> "%CONFIG_FILE%"
    echo SocketAcceptPort=9000 >> "%CONFIG_FILE%"
    echo StartTime=00:00:00 >> "%CONFIG_FILE%"
    echo EndTime=00:00:00 >> "%CONFIG_FILE%"
    echo Created configuration file: %CONFIG_FILE%
)

echo.
echo [Starting Server]
echo Starting FIX Server on port 9000...
echo Classpath: %JARS%
echo.

REM Compile FixServer.java
if exist "FixServer.java" (
    echo Compiling FixServer.java...
    javac -cp "%JARS%" FixServer.java
    if errorlevel 1 (
        echo  Compilation failed
        goto :error
    )
    echo FixServer Compilation successful
    echo.
)

REM Start FIX Server
if exist "FixServer.class" (
    echo Starting Java FIX Server...
    java -cp ".;%JARS%" FixServer
) else (
    echo FixServer.class not found, using QuickFIX/J example...
    java -cp "%JARS%" quickfix.examples.banzai.BanzaiServer "%CONFIG_FILE%"
)

goto :end

:error
echo.
echo Failed to start FIX Server
echo Please check:
echo 1. QuickFIX/J directory exists
echo 2. All required JAR files are present
echo 3. Java is in PATH
pause
exit /b 1

:end
echo.
echo FIX Server stopped.
pause