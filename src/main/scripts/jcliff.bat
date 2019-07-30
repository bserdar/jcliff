@echo off

setlocal EnableDelayedExpansion

set NOPAUSE=true

if "%JBOSS_HOME%" == "" (
  echo No JBOSS_HOME provided. Aborting...
  EXIT /b 1
)

if "%JCLIFF_HOME%" == "" (
  echo No JCLIFF_HOME provided. Aborting...
  EXIT /b 1
)

if not exist "%JCLIFF_HOME%" (
  echo Provided JCLIFF_HOME does not exist: %JCLIFF_HOME%
  EXIT /b 2
)

set JBOSS_CONTROLLER=%JBOSS_CONTROLLER_HOST%:%JBOSS_CONTROLLER_PORT%

set CONTROLLER=

if not "%JBOSS_CONTROLLER%" == ":" (
  set CONTROLLER=--controller=%JBOSS_CONTROLLER%
)

set JBOSS_CLI_TIMEOUT=30000


if "%JCLIFF_RULES_DIR%" == "" (
  set JCLIFF_RULES_DIR=%JCLIFF_HOME%\rules
)

if not "%JAVA_HOME%" == "" (
 set JAVA=%JAVA_HOME%\bin\java.exe
) else (
  For /F "Tokens=*" %%I in ('where java') Do Set JAVA=%%I
)

if not exist "%JAVA%" (
  echo Invalid path to Java executable: %JAVA%
  EXIT /b 3
)


if "%JCLIFF_DEPS_DIR%" == "" (
  set JCLIFF_DEPS_DIR=%JCLIFF_HOME%
)

set CLASSPATH=
 for /R %JCLIFF_HOME% %%a in (*.jar) do (
   set CLASSPATH=%%a;!CLASSPATH!
 )

set CLASSPATH=!CLASSPATH!

call "%JAVA%" -classpath "!CLASSPATH!" com.redhat.jcliff.Main --cli="%JBOSS_HOME%\bin\jboss-cli.bat" %CONTROLLER% --timeout="%JBOSS_CLI_TIMEOUT%" --ruledir="%JCLIFF_RULES_DIR%" %* 