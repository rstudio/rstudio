\name{rstudio-package}
\alias{rstudio-package}
\alias{rstudio}
\docType{package}
\title{
Tools and Utilities for RStudio
}
\description{
Tools, utility functions, and APIs for use within RStudio.
}

\author{
RStudio

Maintainer: RStudio <info@rstudio.com>
}

\keyword{ package }

\examples{
\dontrun{

# Test whether running under RStudio 
isRStudio <- Sys.getenv("RSTUDIO") == "1"

# Use RStudio version information 
if (isRStudio) {

   # Get version info
   require(rstudio)
   rstudioVer <- versionInfo()
   
   # Test specific version constraint
   if (rstudioVer$version >= "0.97") {
      # do some 0.97 dependent stuff
   }
   
   # Check current mode
   desktopMode <- rstudioVer$mode == "desktop"
   serverMode <- rstudioVer$mode == "server"
   
   # Write diagnostics to ~/rstudio-diagnostics
   rstudio::diagnosticsReport()
   
   # Get citation information for RStudio
   utils::citation("rstudio")
}

}

}

