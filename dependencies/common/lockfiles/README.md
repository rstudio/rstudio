
# lockfiles

This directory houses a variety of [renv](https://rstudio.github.io/renv)
lockfiles, which lock the R package versions used with the different Docker
images we build + test RStudio with.

If necessary, package versions can be tweaked (upgraded; downgraded) as
necessary depending on whether or not issues arise when building packages in
the associated Docker image.

See [this commit](https://github.com/rstudio/rstudio/pull/13820/commits/358ae40104453c691344a9e191a91eef5c16dc1d) for an example of how to add a new lockfile.

## How to generate a lockfile

1. On your development machine, use [docker-compile.sh](/docker/docker-compile.sh) to build an image for the platform you'd like to generate a lockfile for.

    ```bash
    # example
    ./docker-compile.sh opensuse15 server
    ```

2. Run a Docker container based on the image that was built.
    - You can use `docker image ls` to determine the Image Name or ID for the image you just built.
    - The below command will run an interactive `bash` shell for your container, based on the image.
    - To more easily retrieve the generated `renv.lock`, you may want to leverage Docker [bind mounts](https://docs.docker.com/storage/bind-mounts/) or [volumes](https://docs.docker.com/storage/volumes/)...but you can also just copy-paste the file contents if you don't want to set that up.
    - You can add `--rm` to remove the container upon exit automatically.

    ```bash
    docker run -ti <IMAGE_NAME_OR_IMAGE_ID> /bin/bash
    ```

3. In the container, navigate to the R library location.

    ```bash
    # example
    cd /root/R/aarch64-suse-linux-gnu-library/3.5
    ```

4. Open the R console in this location.

    ```bash
    R
    ```

5. You may need to install `renv` if it isn't already available.

    ```R
    install.packages("renv")
    ```

6. Before continuing in the R console, determine the versions you want to lock and verify the package repository date where the versions are available.

    The versions to lock are largely based on what's listed under [PACKAGES](/dependencies/common/install-packages), but you may need to pin to a specific version instead of using the unversioned package name.

    For example, we may want to pin `digest@0.6.31` and `testthat@3.1.10`, but use the latest (unversioned package names) for the rest of our dependencies.

    Generally, the most recent Posit Package Manager (PPM) package listing will be sufficient. You can find that date by looking at the most recent date (scroll to the bottom of the page) at [packagemanager.posit.co/cran](https://packagemanager.posit.co/cran).
    
    If you're not using the most recent date, you'll need to verify if the PPM repository listing at the date you're choosing is aware of all the dependency versions you need. To verify this:

    1. Look up each package you want to pin to a specific version at [Posit Package Manager](https://packagemanager.posit.co/client/#/repos/cran/packages/overview)
    2. On each package listing page, scroll to the "ARCHIVED VERSIONS" section and note the date of the version you want was made available.
        - digest
            > ARCHIVED VERSIONS
            > 
            > 0.6.31
            >
            > Dec 11, 2022 7:40 AM UTC
        - testthat   
            > ARCHIVED VERSIONS
            > 
            > 3.1.10
            >
            > Jul 6, 2023 10:00 PM UTC
    3. Ensure that the PPM listing date you choose is the same date or more recent than the availability date for the pinned versions you need.

    In this example, `https://packagemanager.posit.co/cran/2023-10-19` has the versions we need. `digest@0.6.31` was available on `2022-12-11` and `testthat@3.1.10` was available on `2023-07-06`, so the PPM listing for `2023-10-19` will know about these versions.

7. Back in the R console, using `renv::install`, install the default packages listed under [PACKAGES](/dependencies/common/install-packages). This is the time to pin specific versions of these packages! Pin by appending `@<VERSION>` to the package name.
    - `digest` and `testthat` are pinned to specific versions below.
    - see the [`renv::install` docs](https://rstudio.github.io/renv/reference/install.html) for more details and configuration.

    ```R
    # example
    renv::install(c(
        "digest@0.6.31",
        "purrr",
        "rmarkdown",
        "testthat@3.1.10",
        "xml2",
        "yaml"
    ))
    ```

8. Using `renv::snapshot`, snapshot the dependencies at the desired date.
    - the `repos` argument allows us to specify the repo and date of the snapshot (use the PPM url with the date you determined in step 6 above)
    - since we started `R` in the R library location, the current working directory already contains the relevant R packages to be snapshotted.
    - recursive dependencies will be included, unless otherwise configured.
    - see the [`renv::snapshot` docs](https://rstudio.github.io/renv/reference/snapshot.html) for more details and configuration.
    https://packagemanager.posit.co/cran/

    ```R
    # example
    renv::snapshot(repos = "https://packagemanager.posit.co/cran/2023-10-19")
    ```

    - When prompted `A large number of files (<NUMBER> in total) have been discovered.Do you want to proceed?`, respond "yes" (`y`).
    - When prompted `Packages must first be installed before renv can snapshot them. What do you want to do?`, respond "1: Snapshot, just using the currently installed packages." (`1`).
    - Your lockfile should have been written to your current working directory.

    ```R
    # example
    Lockfile written to "~/R/aarch64-suse-linux-gnu-library/3.5/renv.lock"
    ```

9. Exit R using `quit()` and copy the `renv.lock` to your development machine. If you didn't mount your filesystem in some way, you can `cat renv.lock` and copy-paste the contents to you development machine.

10. Update `renv.lock` if the platform already exists under [lockfiles](/dependencies/common/lockfiles/), or add the generated `renv.lock` in new platform directory (and copy in the `_deps.R` and `.dockerignore`).

11. Remove the lockfile entry for `renv` from `renv.lock` as it isn't needed.

12. Run [docker-compile.sh](/docker/docker-compile.sh) again to confirm that building the image still works and that the dependency versions match the `renv.lock` versions.

    ```bash
    # example
    ./docker-compile.sh opensuse15 server
    ```
