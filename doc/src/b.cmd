pushd ..\zed\doctool
call ant -f DocTool.ant.xml
popd
call ant -f userdoc.ant.xml
