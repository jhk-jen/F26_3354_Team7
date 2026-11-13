@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.2.0
@REM
@REM Optional ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM   MAVEN_BATCH_ECHO - set to "on" to enable the echo of the batch commands
@REM   MAVEN_BATCH_PAUSE - set to "on" to wait for a key stroke before ending
@REM   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@IF "%MAVEN_BATCH_ECHO%"=="on" echo %MAVEN_BATCH_ECHO%

@REM Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set MAVEN_BATCH_ECHO=off

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%"=="" goto OkJHome
for %%i in (java.exe) do set "JAVA_HOME_BIN=%%~$PATH:i"
if not "%JAVA_HOME_BIN%"=="" for %%i in ("%JAVA_HOME_BIN%") do set "JAVA_HOME=%%~dpi.."
if "%JAVA_HOME%"=="" (
  echo.
  echo Error: JAVA_HOME not found in your environment. >&2
  echo Please set the JAVA_HOME variable in your environment to match the >&2
  echo location of your Java installation. >&2
  echo.
  goto error
)
:OkJHome

@REM ==== END VALIDATION ====

@REM Find project base directory (where .mvn folder is)
set MAVEN_PROJECTBASEDIR=%~dp0
:findBaseDir
if exist "%MAVEN_PROJECTBASEDIR%.mvn" goto baseDirFound
set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR%..\
goto findBaseDir
:baseDirFound

@REM Read distributionUrl from maven-wrapper.properties
set "distributionUrl="
for /f "usebackq tokens=1,* delims==" %%a in ("%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties") do (
  if "%%a"=="distributionUrl" set "distributionUrl=%%b"
)

@REM Compute distribution name
for %%f in (%distributionUrl%) do set "distributionName=%%~nxf"
set "distributionBase=%distributionName:.zip=%"
set "DISTRIBUTION_DIR=%USERPROFILE%\.m2\wrapper\dists\%distributionBase%"

@REM Check if already extracted
set "MAVEN_HOME="
if not exist "%DISTRIBUTION_DIR%" mkdir "%DISTRIBUTION_DIR%"
for /d %%d in ("%DISTRIBUTION_DIR%\*") do (
  if exist "%%d\bin\mvn.cmd" set "MAVEN_HOME=%%d\"
)

if not "%MAVEN_HOME%"=="" goto runMaven

@REM Download the distribution
set "zipFile=%DISTRIBUTION_DIR%\%distributionName%"
echo Downloading Maven from %distributionUrl%
powershell -Command "(New-Object System.Net.WebClient).DownloadFile('%distributionUrl%', '%zipFile%')"
echo Extracting Maven...
powershell -Command "Expand-Archive -LiteralPath '%zipFile%' -DestinationPath '%DISTRIBUTION_DIR%' -Force"
del /f /q "%zipFile%"

for /d %%d in ("%DISTRIBUTION_DIR%\*") do (
  if exist "%%d\bin\mvn.cmd" set "MAVEN_HOME=%%d\"
)

if "%MAVEN_HOME%"=="" (
  echo ERROR: Could not find Maven installation.
  goto error
)

:runMaven
set "MAVEN_JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"

@REM Execute Maven
set "MAVEN_CMD_LINE_ARGS=%MAVEN_CONFIG% %*"
"%MAVEN_HOME%bin\mvn.cmd" %MAVEN_OPTS% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %MAVEN_CONFIG% %*

if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

if "%MAVEN_BATCH_PAUSE%"=="on" pause

if "%MAVEN_TERMINATE_CMD%"=="on" exit %ERROR_CODE%

cmd /C exit /B %ERROR_CODE%
