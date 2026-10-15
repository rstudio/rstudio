# GWT for RStudio

RStudio builds against a fork of GWT at https://github.com/rstudio/gwt. The built SDK is
not checked in; `dependencies/common/install-gwt` (and `install-dependencies.cmd` on Windows)
downloads a tarball from the `rstudio-buildtools` S3 bucket into
`dependencies/common/gwtproject`, and `src/gwt/build.xml` uses the SDK in
`gwtproject/gwt/gwt-rstudio`.

## Branches

The fork's `main` tracks upstream. Releases are built from branches named after the RStudio
release that needs them, e.g. `rstudio/montauk-daisy`. Each release branch is created from the
previous one with an upstream release tag merged in. Not every RStudio release gets a branch;
we only create one when GWT changes.

## Checking out

GWT's build also needs https://github.com/gwtproject/tools. By default it looks for it at
`../tools`, next to the `gwt` checkout; set `GWT_TOOLS` to use a different location.

	git clone https://github.com/rstudio/gwt
	git clone https://github.com/gwtproject/tools
	cd gwt
	git remote add upstream https://github.com/gwtproject/gwt

## Creating a branch for a new release

For example, to move from Montauk Daisy to a new release on GWT 2.13.1:

	git fetch upstream --tags
	git checkout rstudio/montauk-daisy
	git checkout -b rstudio/<new-release>
	git merge 2.13.1
	git push -u origin rstudio/<new-release>

Commit GWT patches for that release to this branch.

## Build names

Each published SDK has a build name, which is used for the tag in the fork, the tarball name,
and `GWT_VERSION` in the install scripts. The first build for a release is
`<upstream version>-<release>`, e.g. `2.13.1-montauk-daisy`; later builds for the same release
add a counter: `2.13.1-montauk-daisy-1`, `2.13.1-montauk-daisy-2`, and so on. The install
scripts only download a build whose name they haven't installed before, so a name is never
reused: any change to the tarball gets the next number. The tag records which commit of the
fork went into each tarball.

Only one person should prepare a build at a time. Pushing the tag claims the name; if someone
else has already pushed it, the push is rejected.

## Building

The build may fail with JDKs newer than 17. `GWT_VERSION` here is the upstream version, which
is used to brand the SDK.

	export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
	GWT_VERSION=2.13.1
	ant clean dist-dev "-Dgwt.version=${GWT_VERSION}"

The SDK is written to `build/staging/gwt-${GWT_VERSION}`. To try it locally, copy it over
the installed SDK, where `RSTUDIO` is your rstudio checkout. This assumes you have already run
`install-gwt`, which also installs the libraries GWT is used with, such as `elemental2` and
`gin`.

	rm -rf "${RSTUDIO}/dependencies/common/gwtproject/gwt/gwt-rstudio"
	cp -R "build/staging/gwt-${GWT_VERSION}" \
		"${RSTUDIO}/dependencies/common/gwtproject/gwt/gwt-rstudio"

`gwt-rstudio/about.txt` records the git revision the SDK was built from.

## Publishing

Once your changes are pushed to the release branch, find the latest build name and tag the
branch with the next one:

	git fetch origin --tags
	git tag -l "${GWT_VERSION}-montauk-daisy*"
	GWT_BUILD=2.13.1-montauk-daisy-1
	git checkout rstudio/montauk-daisy
	git pull
	git tag -a "${GWT_BUILD}" -m "${GWT_BUILD}"
	git push origin "${GWT_BUILD}"

Build from the tag with a clean working tree and copy the SDK into your rstudio checkout, as
described above.

The tarball contains the whole `gwtproject` directory, including the other libraries, not
just GWT. Package it on macOS or Linux, after removing the `gwt-<build name>` marker files that
`install-gwt` creates. Check that the name isn't already in the bucket (`aws s3 ls` should
print nothing), then upload:

	cd "${RSTUDIO}/dependencies/common/gwtproject"
	rm -f gwt-*
	tar czf ../gwt.tar.gz *
	aws s3 ls "s3://rstudio-buildtools/gwt/gwt-${GWT_BUILD}.tar.gz"
	aws s3 cp ../gwt.tar.gz "s3://rstudio-buildtools/gwt/gwt-${GWT_BUILD}.tar.gz" \
		--acl public-read
	rm ../gwt.tar.gz

Then set `GWT_VERSION` to the build name in both `dependencies/common/install-gwt` and
`dependencies/windows/install-dependencies.cmd`, and set `VERSION` in
`dependencies/metadata/gwt.txt` to the upstream version. If any jar paths changed, also update
`src/gwt/.classpath`, which IDEs use to resolve RStudio's Java libraries.
