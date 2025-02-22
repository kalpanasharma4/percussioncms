@echo off
rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

if "%OS%" == "Windows_NT" setlocal
rem ---------------------------------------------------------------------------
rem NT Service Install/Uninstall script
rem
rem Options
rem install                Install the service using Tomcat6 as service name.
rem                        Service is installed using default settings.
rem remove                 Remove the service from the System.
rem
rem name        (optional) If the second argument is present it is considered
rem                        to be new service name                                           
rem
rem $Id: service.bat 734543 2009-01-14 23:00:49Z markt $
rem ---------------------------------------------------------------------------

REM Setting LOG_JNI to 1 will enable debug logging
SET LOG_JNI=0

REM Setting LOG_LEVEL to debug will enable debug logging
SET LOG_LEVEL=error

rem Guess CATALINA_HOME if not defined
set CURRENT_DIR=%cd%

set CATALINA_HOME=%cd%
if exist "%CATALINA_HOME%\bin\PercussionDTS.exe" goto okHome
rem CD to the upper dir
cd ..
set CATALINA_HOME=%cd%
:gotHome
if exist "%CATALINA_HOME%\bin\PercussionDTS.exe" goto okHome
echo The PercussionDTS.exe was not found...
echo The CATALINA_HOME environment variable is not defined correctly.
echo This environment variable is needed to run this program
goto end
rem Make sure prerequisite environment variables are set
:okHome

set CATALINA_BASE=%CATALINA_HOME%
 
set EXECUTABLE=%CATALINA_HOME%\bin\PercussionDTS.exe

rem Set default Service name
set SERVICE_NAME=PercussionDTS
set PR_DISPLAYNAME=Percussion DTS

if "%1" == "" goto displayUsage
if "%2" == "" goto setServiceName
set SERVICE_NAME=%2
set PR_DISPLAYNAME=Apache Tomcat %2
:setServiceName
if %1 == install goto doInstall
if %1 == remove goto doRemove
if %1 == uninstall goto doRemove
echo Unknown parameter "%1"
:displayUsage
echo.
echo Usage: service.bat install/remove [service_name]
goto end

:doRemove
rem Remove the service
"%EXECUTABLE%" //DS//%SERVICE_NAME%
echo The service '%SERVICE_NAME%' has been removed
goto end

:doInstall
cd %CATALINA_HOME%
cd ..\..\JRE
SET JRE_HOME=%cd%
SET JAVA_HOME=%cd%

rem Install the service
echo Installing the service '%SERVICE_NAME%' ...
echo Using CATALINA_HOME:    %CATALINA_HOME%
echo Using CATALINA_BASE:    %CATALINA_BASE%
echo Using JAVA_HOME:        %JRE_HOME%

rem Use the environment variables as an example
rem Each command line option is prefixed with PR_

set PR_DESCRIPTION=Percussion Delivery Tier Services - Apache Tomcat Server
set PR_INSTALL=%EXECUTABLE%
set PR_LOGPATH=%CATALINA_BASE%\logs
set PR_CLASSPATH=%CATALINA_HOME%\bin\bootstrap.jar;%CATALINA_HOME%\bin\tomcat-juli.jar
set PR_JVM=%JRE_HOME%\bin\server\jvm.dll

echo Using JVM:              %PR_JVM%
"%EXECUTABLE%" //IS//%SERVICE_NAME% --LogJniMessages=%LOG_JNI% --LogLevel=%LOG_LEVEL% --StopPath=%CATALINA_HOME%\bin --StartPath=%CATALINA_HOME%\bin --Startup=auto --JavaHome=%JRE_HOME% --StartClass org.apache.catalina.startup.Bootstrap --StartMethod=main --StartParams=start --StopParams=stop --StartMethod=main --StopMethod=main --StopClass org.apache.catalina.startup.Bootstrap 
if not errorlevel 1 goto installed
echo Failed installing '%SERVICE_NAME%' service
goto end
:installed
rem Clear the environment variables. They are not needed any more.
set PR_DISPLAYNAME=
set PR_DESCRIPTION=
set PR_INSTALL=
set PR_LOGPATH=
set PR_CLASSPATH=
set PR_JVM=
rem Set extra parameters
"%EXECUTABLE%" //US//%SERVICE_NAME% --JvmOptions "-Dcatalina.base=%CATALINA_BASE%;-Dcatalina.home=%CATALINA_HOME%;-Djava.endorsed.dirs=%CATALINA_HOME%\endorsed" --StartMode jvm --StopMode jvm
rem More extra parameters
set PR_LOGPATH=%CATALINA_BASE%\logs
set PR_STDOUTPUT=auto
set PR_STDERROR=auto
"%EXECUTABLE%" //US//%SERVICE_NAME% ++JvmOptions "-Djava.io.tmpdir=%CATALINA_BASE%\temp;-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager;-Djava.util.logging.config.file=%CATALINA_BASE%\conf\logging.properties;-Dfile.encoding=UTF-8;-Dderby.system.home=%CATALINA_HOME%\derbydata" --JvmMs 128 --JvmMx 256
echo The service '%SERVICE_NAME%' has been installed.

:end
cd %CURRENT_DIR%
