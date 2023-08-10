#!/usr/bin/env bash

# For each of our platforms, generate a devcontainer.json file

PLATFORMS=( "Jammy-arm64" "Jammy-x86_64" "Bionic-x86_64" "Bionic-arm64" "Centos7-x86_64" "openSUSE15-x86_64" "RHEL8-x86_64" "RHEL9-arm64" "RHEL9-x86_64")

for PLATFORM in "${PLATFORMS[@]}"
do
    echo "Generating devcontainer.json for $PLATFORM"

    PLATFORM_LOWERCASE=$(echo "$PLATFORM" | tr '[:upper:]' '[:lower:]')
    PLATFORM_DIR="dev-$PLATFORM_LOWERCASE"

    mkdir -p $PLATFORM_DIR

    # infer make parallelism
    if [ "$(uname)" = "Darwin" ]; then
        # macos; Docker for Mac defaults to half of host's cores
        PARALLEL_JOBS=$(( `sysctl -n hw.ncpu` / 2 ))
    elif hash nproc 2>/dev/null; then
        # linux
        PARALLEL_JOBS=$(nproc --all)
    fi

    # output template to $PLATFORM/devcontainer.json
    sed -e "s/{PLATFORM}/$PLATFORM/g" \
        -e "s/{PLATFORM_LOWERCASE}/$PLATFORM_LOWERCASE/g" \
        -e "s/{PARALLEL_JOBS}/$PARALLEL_JOBS/g" \
         template_devcontainer.json > temp.json
    
    # if devcontainer_dev.json exists, merge it with $PLATFORM_DIR/devcontainer.json with jq and output to $PLATFORM/devcontainer.json
    if [ -f devcontainer_dev.json ]; then
        jq -s '.[0] * .[1]' temp.json devcontainer_dev.json  > $PLATFORM_DIR/devcontainer.json
        rm temp.json
    else
        mv temp.json $PLATFORM_DIR/devcontainer.json
    fi
  
    # move docker-compose-vscode.yml to $PLATFORM_DIR
    cp template_docker-compose-vscode.yml $PLATFORM_DIR/docker-compose-vscode.yml
done

# create init-dev.sh script if it doesn't exist
if [ ! -f init-dev.sh ]; then
    cat << EOF > init-dev.sh
#!/usr/bin/env bash

# This script is used for developers to add custom commands to run when initializing a dev container
# It is run by the .devcontainer/Dockerfile
EOF
    chmod +x init-dev.sh
fi
