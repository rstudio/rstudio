set -e

# The post-create command (see `.devcontainer/devcontainer.json`) runs
# this script to generate rstudio config files from the templates in
# this directory.

cd ${WORKSPACE_FOLDER}/.devcontainer/conf
mkdir -p ../../run/conf/
sed "s~\${WORKSPACE_FOLDER}~${WORKSPACE_FOLDER}~g" rserver.conf.template  > ../../run/conf/rserver.conf


# cmake --no-warn-unused-cli -DRSTUDIO_TARGET=Server -DCMAKE_EXPORT_COMPILE_COMMANDS:BOOL=TRUE -DCMAKE_BUILD_TYPE:STRING=Debug -DCMAKE_C_COMPILER:FILEPATH=/bin/x86_64-linux-gnu-gcc-9 -DCMAKE_CXX_COMPILER:FILEPATH=/bin/x86_64-linux-gnu-g++-9 -H/workspaces/rstudio -B/workspaces/rstudio/build-Server-DEB -G Ninja

# cmake --build /workspaces/rstudio/build-Server-DEB --config Debug --target gwt_build -j 4 --
