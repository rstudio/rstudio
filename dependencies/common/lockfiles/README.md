
# lockfiles

This directory houses a variety of [renv](https://rstudio.github.io/renv)
lockfiles, which lock the R package versions used with the different Docker
images we build + test RStudio with.

A base lockfile can be generated with:

```
renv::checkout(date = "<date>", actions = "snapshot")
```

If necessary, package versions can be tweaked (upgraded; downgraded) as
necessary depending on whether or not issues arise when building packages in
the associated Docker image.

