if exist "%LocalAppData%\node-gyp\Cache" rmdir /s /q "%LocalAppData%\node-gyp\Cache"
@echo off

if exist build rmdir /s /q build 
pushd ..\..\src\gwt
ant clean 
popd
