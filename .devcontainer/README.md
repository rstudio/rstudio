# VSCode Dev Containers

This folder contains a collection of [VSCode Dev Containers](https://code.visualstudio.com/docs/remote/containers) to be used for development.

## Requirements

- [Docker](https://www.docker.com/)
- [VSCode](https://code.visualstudio.com/)
- [VSCode Development Extension Pack](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.vscode-remote-extensionpack)
- [Access to our AWS ECR Registry](https://positpbc.atlassian.net/wiki/spaces/IDEWB/pages/287539395/AWS+Resources)
- [jq](https://stedolan.github.io/jq/)

## Usage

### Generate Dev Containers

To generate the dev containers, run the following command:

```bash
./generate-dev-containers.sh
```

This will generate a `dev-*` folder for each of the platforms we build for. Each folder will contain a `devcontainer.json` file and a `docker-compose-vscode.yml` file that are used by VSCode to build and run the dev container.

### Build Dev Containers

#### Pull Images from AWS ECR

Before building the dev containers, you will need to pull the images from our AWS ECR registry. First login to the registry with the AWS cli. The preferred method to this is to [use SSO](https://positpbc.atlassian.net/wiki/spaces/ENG/pages/36343631/AWS+Single+Sign+On). 

Assuming you have set up a profile similar to this (may change depending on your given role):
```
[profile RStudioMain-IDETeam]
sso_start_url = https://rstudio.awsapps.com/start
sso_region = us-east-2
sso_account_id = 263245908434
sso_role_name = IDETeam
```

You can login to the registry with the following command:

```bash
aws sso login --profile RStudioMain-IDETeam
```

Then login to the registry with the following command:

```bash
aws ecr get-login-password --region us-east-1 --profile RStudioMain-IDETeam | docker login --username AWS --password-stdin 263245908434.dkr.ecr.us-east-1.amazonaws.com
```

Then you can pull whichever image you need with a command like this:

```bash
docker pull 263245908434.dkr.ecr.us-east-1.amazonaws.com/jenkins/ide:pro-jammy-x86_64-desert-sunflower
```

*Note: Attempting to run a dev container without pulling the image first will result in an error.*

#### Which base image should I use?

The base build image you use will depend on what branch you are currently on and whether you are in OS (rstudio) or Pro (rstudio-pro). The following table shows which image you should use for each branch. For the flower name (e.g. desert-sunflower), you can check the `version/RELEASE` folder.

When building the dev container, a `.env` folder is created in the dev folder that contains information about your current environment (e.g. branch, OS/Pro, flower name). This is used to determine which base image to use.

#### Build Dev Containers

To open your current workspace in a dev container, open the command palette (`Ctrl+Shift+P`) and select `Remote-Containers: Open Folder in Container`. Then select which container you'd like to open. This will build the container and open the workspace in the container.

You can also use `Remote-Containers: Rebuild and Reopen in Container` to rebuild the container and reopen the workspace.

## Customizing Dev Containers

### Custom scripts

When the `generate-dev-containers.sh` script is run, it will create a `init-dev.sh` script (which is git ignored) that is run when the dev container is built. This script can be used to customize the dev container. For example, you can install additional packages or run additional scripts to set up your environment.

### Customizing the dev container

When `generate-dev-containers.sh` is run, it will also look for a `devcontainer_dev.json` file in the `.devcontainer` folder. This file can be used to customize the dev container. For example, you can add additional extensions to be installed in the dev container.

For example:

```json
{
    "extensions": [
        "ms-python.python",
        "ms-toolsai.jupyter"
    ]
}
```

This json file will be merged with the base dev container template and override any values that are present in the base template.
