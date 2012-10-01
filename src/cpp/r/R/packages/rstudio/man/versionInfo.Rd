\name{versionInfo}
\alias{versionInfo}
\title{
RStudio Version Information
}
\description{
Provides information about the currently running version of RStudio, including it's specific version number and whether it is running in desktop or server mode.
}
\usage{
versionInfo()
}

\value{

Returns a list with the following elements:

\tabular{ll}{
   \code{version} \tab A package version object that can be used in comparisons. This is the same value which would be returned from \code{packageVersion("rstudio")} \cr
   \code{mode} \tab Current program mode (either "desktop" or "server")\cr
}


}

\examples{
\dontrun{
require(rstudio)
rstudioVer <- versionInfo()

# Test specific version constraint
if (rstudioVer$version >= "0.97") {
   # do some 0.97 dependent stuff
}

# Check current mode
desktopMode <- rstudioVer$mode == "desktop"
serverMode <- rstudioVer$mode == "server"
}
}

