# Starting Development with Visual Studio Code

You can get started with development very quickly with Visual Studio Code amd Docker.

## Install Prerequisites

* Install Docker.
* Install VS Code and install the "Remote - Containers" extension in VS Code.
* Configure your Docker resources to give sufficient resources to the
  containers. For example, if you have sufficuent resources, you may consider allocating
  8 CPUs and 6+GB RAM.

## Configure Git Credentials

Are you going to clone over SSH or over HTTPS? If you prefer to use HTTPS,
make sure you have a Github PAT (personal access token).

> Tip: If you want to clone and use a specific branch from the start, use HTTPS. If
> you are ok with the default branch, you can use SSH or HTTPS.

### Using Git with HTTPS

First, make sure the Github credential helper for OSX is installed. See
https://docs.github.com/en/github/getting-started-with-github/caching-your-github-credentials-in-git
for more information for your operating system.

For example, on a Mac, all you typically need to do is this:

```sh
git config --global credential.helper osxkeychain
```

Next, attempt a clone locally to populate the credentials in the helper:

```sh
# Use your Github username and PAT (personal access token) as the password when prompted.
# It's fine to cancel this process after you see the clone is starting successfully.
git clone https://github.com/rstudio/rstudio-pro.git
```

### Using Git with SSH

For SSH, you must be running an SSH agent. Simply add your SSH key to the SSH agent
with `ssh-add ~/.ssh/<my-key>`.

## Open Project in VS Code

1. From the Command Palette (CMD+Shift+P on Mac), choose "Remote-Containers: Clone Repository
   in Named Container Volume".
2. Enter the repository URL. For SSH, enter `git@github.com:rstudio/rstudio-pro.git`. For HTTPS, enter
   `https://github.com/rstudio/rstudio-pro.git`. If you need to choose a non-default branch, you need
   to first select the "Github" option before entering the HTTPS URL.
3. Wait for the clone and for the development container to start up. If the docker image needs to be built, this could
   take quite a while.
4. Choose the GCC kit, optionally choose a target, and click the `Build` button
