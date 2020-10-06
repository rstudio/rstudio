#
# SessionConnectionsInstaller.R
#
# Copyright (C) 2020 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("odbcBundleOsName", function() {
   osMapping <- list (
      linux = "linux",
      windows = "windows",
      darwin = "osx"
   )
   
   if (!tolower(Sys.info()["sysname"]) %in% names(osMapping))
      stop("Operating system \"", Sys.info()["sysname"], "\" is unsupported.")
   
   osMapping[[tolower(Sys.info()[["sysname"]])]]
})

.rs.addFunction("odbcOsBitness", function() {
   if (grepl("64", Sys.info()["machine"])) {
      "64"
   }
   else {
      "32"
   }
})

.rs.addFunction("odbcIsWow", function() {
   identical(tolower(Sys.info()["sysname"])[[1]], "windows") &&
      identical(.rs.odbcOsBitness(), "32") &&
      nchar(Sys.getenv("ProgramW6432")) > 0
})

.rs.addFunction("odbcBundleName", function(placeholder) {
   osName <- .rs.odbcBundleOsName()
   bitness <- .rs.odbcOsBitness()
   
   bundleName <- gsub("\\(os\\)", osName, placeholder)
   bundleName <- gsub("\\(bitness\\)", bitness, bundleName)
   
   bundleName
})

.rs.addFunction("odbcBundleDownload", function(url, placeholder, bundleTemp) {
   bundleName <- .rs.odbcBundleName(placeholder)
   bundleUrl <- file.path(url, bundleName)
   
   if (exists(bundleTemp)) unlink(bundleTemp, recursive = TRUE)
   dir.create(bundleTemp, recursive = TRUE)
   
   bundleFileTemp <- file.path(bundleTemp, bundleName)
   download.file(bundleUrl, bundleFileTemp)
   
   bundleFileTemp
})

.rs.addFunction("odbcBundleExtract", function(bundleFileTemp, installPath) {
   untar(bundleFileTemp, exdir = installPath)
})

.rs.addFunction("odbcBundleCheckPrereqsUnixodbc", function() {
   identical(
      suppressWarnings(
         system2(
            "odbcinst",
            stdout = getOption("odbc.installer.verbose", FALSE),
            stderr = getOption("odbc.installer.verbose", FALSE)
         )
      ),
      1L
   )
})

.rs.addFunction("odbcBundleCheckPrereqsBrew", function() {
   identical(
      suppressWarnings(
         system2(
            "brew",
            stdout = getOption("odbc.installer.verbose", FALSE),
            stderr = getOption("odbc.installer.verbose", FALSE)
         )
      ),
      1L
   )
})

.rs.addFunction("odbcBundleCheckPrereqsOsx", function() {
   if (!.rs.odbcBundleCheckPrereqsUnixodbc()) {
      if (!.rs.odbcBundleCheckPrereqsBrew()) {
         stop(
            "unixODBC is required but missing, you can install from http://www.unixodbc.org/. ",
            "Alternatively, install Brew and RStudio will install unixODBC automatically, ",
            "you can install Brew by running: ",
            "/usr/bin/ruby -e \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)\""
         )
      }
      
      message("Installing unixODBC...")
      system2(
         "brew",
         args = list("install", "unixodbc")
      )
   }
   
   if (!.rs.odbcBundleCheckPrereqsUnixodbc())
      stop("Failed to install unixODBC, please install from www.unixodbc.org")
})

.rs.addFunction("odbcBundleCheckPrereqsLinux", function() {
   if (!.rs.odbcBundleCheckPrereqsUnixodbc())
      stop("unixODBC is not installed, please install from www.unixodbc.org")
})

.rs.addFunction("odbcBundleRegistryAdd", function(entries) {
   validateEntry <- function(entry) {
      tryCatch({
         verify <- readRegistry(entry$path, "HLM", view = .rs.odbcOsBitness())
         identical(verify[[entry$key]], entry$value)
      }, error = function(e) {
         FALSE
      })
   }

   odbcFileEscape <- function(value) {
    gsub("\\\\", "\\\\\\\\", value)
   }

   if (all(sapply(entries, function(e) validateEntry(e))))
      return()

   bitness <- .rs.odbcOsBitness()

   allAdded <- TRUE
   for (entry in entries) {
     fullPath <- file.path("HKEY_LOCAL_MACHINE", entry$path, fsep = "\\")
     system2(
        "REG",
        args = list(
           "ADD",
           shQuote(fullPath),
           "/v",
           shQuote(entry$key),
           "/t",
           "REG_SZ",
           "/d",
           shQuote(entry$value),
           "/f",
           paste("/reg:", bitness, sep = "")
        ),
        stdout = getOption("odbc.installer.verbose", FALSE),
        stderr = getOption("odbc.installer.verbose", FALSE)
     )

     if (!validateEntry(entry)) {
      allAdded <- FALSE
      break
     }
   }

   if (!allAdded) {
      if (.rs.odbcIsWow()) {
         stop("Failed to install x86 driver in x64 machine, retry running as administrator.")
      }

      message("Could not add registry keys from R, retrying using registry prompt.")
      addReg <- tempfile(fileext = ".reg")

      lineEntries <- sapply(entries, function(entry) {
         fullPath <- file.path("HKEY_LOCAL_MACHINE", entry$path, fsep = "\\")
         c(
            paste("[", fullPath, "]", sep = ""),
            paste(
               "\"",
               entry$key,
               "\"=\"",
               odbcFileEscape(entry$value),
               "\"",
               sep = ""
            ),
            ""
         )
      })

      lines <- c(
         "REGEDIT4",
         "",
         lineEntries
      )

      writeLines(lines, addReg)

      message("Waiting for ", addReg, " to be registered.")
      system2(
         "explorer",
         addReg
      )

      allEntriesValid <- function() {
        all(sapply(entries, function(e) validateEntry(e)))
      }

      registryStart <- Sys.time()
      registryWait <- 30
      while (!allEntriesValid() && Sys.time() < registryStart + registryWait) {
         Sys.sleep(1)
         cat(".")
      }

      if (!allEntriesValid()) {
         stop("Failed to add all registry keys using registry file.")
      }
   }
})

.rs.addFunction("odbcBundleRegistryRemove", function(entries) {
   validateEntry <- function(entry) {
      tryCatch({
         regEntry <- readRegistry(entry$path, "HLM", view = .rs.odbcOsBitness())
         if (!is.null(entry$key))
            is.null(regEntry[[entry$key]])
         else
            FALSE
      }, error = function(e) {
         TRUE
      })
   }

   if (all(sapply(entries, function(e) validateEntry(e))))
      return()

   bitness <- .rs.odbcOsBitness()

   for (entry in entries) {
      fullPath <- file.path("HKEY_LOCAL_MACHINE", entry$path, fsep = "\\")

      if (!is.null(entry$key)) {
         regArgs <- list(
            "DELETE",
            shQuote(fullPath),
            "/v",
            shQuote(entry$key),
            "/f",
            paste("/reg:", bitness, sep = "")
         )
      }
      else {
         regArgs <- list(
            "DELETE",
            shQuote(fullPath),
            "/f",
            paste("/reg:", bitness, sep = "")
         )
      }

      system2(
         "REG",
         args = regArgs
      )
   }

   if (all(sapply(entries, function(e) validateEntry(e))))
      return()

   addReg <- tempfile(fileext = ".reg")

   lineEntries <- sapply(entries, function(entry) {
      fullPath <- file.path("HKEY_LOCAL_MACHINE", entry$path, fsep = "\\")

      if (is.null(entry$key)) {
         paste("[-", fullPath, "]", sep = "")
      }
      else {
         c(
            paste("[", fullPath, "]", sep = ""),
            paste(
               "\"",
               entry$key,
               "\"=-",
               sep = ""
            ),
            ""
         )
      }
   })

   lines <- c(
      "REGEDIT4",
      "",
      unlist(lineEntries)
   )

   writeLines(lines, addReg)
   system2(
      "explorer",
      addReg
   )
})

.rs.addFunction("odbcBundleRegistryDelete", function(path) {
   system2(
      "REG",
      args = list(
         "DELETE",
         shQuote(path),
         "/f"
      )
   )
   
   identical(ret, 0L)
})

.rs.addFunction("odbcBundleCheckPrereqsWindows", function() {
})

.rs.addFunction("odbcBundleCheckPrereqs", function() {
   osPrereqs <- list(
      osx = .rs.odbcBundleCheckPrereqsOsx,
      windows = .rs.odbcBundleCheckPrereqsWindows,
      linux = .rs.odbcBundleCheckPrereqsLinux
   )
   
   prereqs <- osPrereqs[[.rs.odbcBundleOsName()]]
   prereqs()
})

.rs.addFunction("odbcBundleOdbcinstPathWithOdbcinst", function() {
   config <- system2("odbcinst", "-j", stdout = TRUE)
   odbciniEntry <- config[grepl("odbcinst.ini", config)]
   gsub("^[^/\\\\]*", "", odbciniEntry)
})

.rs.addFunction("odbcBundleOdbcinstPathUseHome", function() {
   normalizePath("~/.odbcinst.ini", mustWork = FALSE)
})

.rs.addFunction("odbcBundleOdbcinstPath", function() {
   osOdbcinstPath <- list(
      osx = .rs.odbcBundleOdbcinstPathWithOdbcinst,
      windows = .rs.odbcBundleOdbcinstPathWithOdbcinst,
      linux = .rs.odbcBundleOdbcinstPathUseHome
   )

   osOdbcinstPath[[.rs.odbcBundleOsName()]]()
})

.rs.addFunction("odbcBundleReadIni", function(odbcinstPath) {
   lines <- readLines(odbcinstPath)
   data <- list()
   
   currentDriver <- "__header__"
   
   for (line in lines) {
      # Is header?
      if (grepl(" *\\[[^]]+\\] *", line)) {
         currentDriver <- gsub("^ *\\[|\\] *", "", line)
         data[[currentDriver]] <- ""
      }
      else if (!grepl("^ *$", line)) {
         if (identical(data[[currentDriver]], ""))
            data[[currentDriver]] <- line
         else
            data[[currentDriver]] <- c(
               data[[currentDriver]],
               line
            )
      }
   }
   
   data
})

.rs.addFunction("odbcBundleWriteIni", function(odbcinstPath, data) {
   lines <- c()
   for (name in names(data)) {
      lines <- c(
         lines, 
         if (identical(name, "__header__")) 
            ""
         else
            paste("[", name, "]", sep = "")
      )
      
      lines <- c(
         lines,
         unlist(data[[name]]),
         ""
      )
   }
   
   if (is.null(lines)) lines <- c("")

   writeLines(lines, odbcinstPath)
})

.rs.addFunction("odbcOdbcInstLibPath", function() {
   odbcinstLib <- NULL

   odbcinstBin <- Sys.which("odbcinst")
   if (nchar(odbcinstBin) == 0) {
      warning("Could not find path to odbcinst.")
   }
   else {
      odbcinstLink <- Sys.readlink(odbcinstBin)
      if (!is.na(odbcinstLink)) {
         odbcinstBinPath <- normalizePath(file.path(dirname(odbcinstBin), dirname(odbcinstLink)))
         odbcinstLibPath <- normalizePath(file.path(odbcinstBinPath, "..", "lib"))
         odbcinstLib <- dir(odbcinstLibPath, pattern = "libodbcinst.*\\.dylib", full.names = TRUE)[[1]]
      }
   }

   odbcinstLib
})

.rs.addFunction("odbcBundleRegisterLinux", function(name, driverPath, version, installPath) {
   # Find odbcinst.ini file
   odbcinstPath <- .rs.odbcBundleOdbcinstPath()
   
   # Read odbcinst.ini
   odbcinst <- .rs.odbcBundleReadIni(odbcinstPath)
   
   # Set odbcinst.ini entries
   odbcinst[[name]] <- list(
      paste("Driver", "=", driverPath),
      paste("Version", "=", version),
      paste("Installer", "=", "RStudio")
   )
   
   # Write odbcinst.ini
   .rs.odbcBundleWriteIni(odbcinstPath, odbcinst)
})

.rs.addFunction("odbcBundleDriverIniPath", function(name, driverPath) {
   dir(driverPath, pattern = paste(tolower(name), ".*\\.ini", sep = ""), recursive = TRUE, full.names = T)
})

.rs.addFunction("odbcBundleRegisterOSX", function(name, driverPath, version, installPath) {
   # Update odbcinst.ini
   .rs.odbcBundleRegisterLinux(name, driverPath, version, installPath)

   # Find driver.ini file
   driverIniFile <- .rs.odbcBundleDriverIniPath(name, installPath)
   
   if (length(driverIniFile) == 0) {
      warning("Could not find '", name, "' driver INI file under: ", installPath)
   }
   else {
      # Read driver.ini
      driverIni <- .rs.odbcBundleReadIni(driverIniFile)

      # In OSX register to use unixODBC
      odbcinstLib <- .rs.odbcOdbcInstLibPath()
      if (!is.null(odbcinstLib)) {
         driverIni[["Driver"]] <- c(
            driverIni[["Driver"]],
            paste("ODBCInstLib", "=", odbcinstLib)
         )
      }
      
      # Write odbcinst.ini
      .rs.odbcBundleWriteIni(driverIniFile, driverIni)
   }
})

.rs.addFunction("odbcBundleRegisterWindows", function(name, driverPath, version, installPath) {
   .rs.odbcBundleRegistryAdd(
      list(
         list(
            path = file.path("SOFTWARE", "ODBC", "ODBCINST.INI", "ODBC Drivers", fsep = "\\"),
            key = name,
            value = "installed"
         ),
         list(
            path = file.path("SOFTWARE", "ODBC", "ODBCINST.INI", name, fsep = "\\"),
            key = "Driver",
            value = driverPath
         ),
         list(
            path = file.path("SOFTWARE", "ODBC", "ODBCINST.INI", name, fsep = "\\"),
            key = "Setup",
            value = driverPath
         ),
         list(
            path = file.path("SOFTWARE", "ODBC", "ODBCINST.INI", name, fsep = "\\"),
            key = "Version",
            value = version
         ),
         list(
            path = file.path("SOFTWARE", "ODBC", "ODBCINST.INI", name, fsep = "\\"),
            key = "Installer",
            value = "RStudio"
         )
      )
   )
})

.rs.addFunction("odbcBundleFindDriver", function(name, installPath, libraryPattern) { 
   osExtensions <- list(
      osx = "dylib$",
      windows = "dll$",
      linux = "so$"
   )
   
   osExtension <- osExtensions[[.rs.odbcBundleOsName()]]
   driverName <- gsub(paste(" |", trimws(.rs.connectionOdbcRStudioDriver()), sep = ""), "", name)
   
   if (is.null(libraryPattern) || nchar(libraryPattern) == 0) {
      libraryPattern <- paste(
         driverName,
         "[^/\\\\]+\\.",
         osExtension,
         sep = ""
      )
   }

   # apply dynamic patterns
   osName <- .rs.odbcBundleOsName()
   bitness <- .rs.odbcOsBitness()
   libraryPattern <- gsub("\\(os\\)", osName, libraryPattern)
   libraryPattern <- gsub("\\(bitness\\)", bitness, libraryPattern)
   
   allFiles <- dir(installPath, recursive = TRUE, full.names = TRUE)
   driverPath <- allFiles[grepl(libraryPattern, allFiles, ignore.case = TRUE)]

   if (!identical(length(driverPath), 1L))
      stop("Failed to find ", library, " inside driver bundle.")
   
   normalizePath(driverPath)
})

.rs.addFunction("odbcBundleRegister", function(name, driverPath, version, installPath) {
   osRegistrations <- list(
      osx = .rs.odbcBundleRegisterOSX,
      windows = .rs.odbcBundleRegisterWindows,
      linux = .rs.odbcBundleRegisterLinux
   )
   
   osRegistration <- osRegistrations[[.rs.odbcBundleOsName()]]
   
   osRegistration(name, driverPath, version, installPath) 
})

.rs.addFunction("odbcBundleValidate", function(bundleFile, md5) {
   if (!is.null(md5) && nchar(md5) > 0) {
      validMd5s <- strsplit(as.character(md5), "[ \n,]+")[[1]]
      bundleMd5 <- tools::md5sum(bundleFile)
      if (!bundleMd5 %in% validMd5s) {
         stop("Failed to validate bundle with signature ", md5, " but got ", bundleMd5, " instead.")
      }
   }
})

.rs.addFunction("odbcBundleInstall", function(
   name,
   url,
   placeholder,
   installPath,
   libraryPattern = NULL,
   md5 = NULL,
   version = "") {

   installPath <- file.path(
      normalizePath(installPath, mustWork = FALSE),
      tolower(name)
   )
   
   bundleTemp <- tempfile()
   on.exit(unlink(bundleTemp, recursive = TRUE), add = TRUE)

   message("Installing")
   message("  Driver: ", name)
   message("  Version: ", version)
   message("  Path: ", installPath)

   message("Checking prerequisites")
   .rs.odbcBundleCheckPrereqs()
   
   message("Downloading bundle")
   bundleFileTemp <- .rs.odbcBundleDownload(url, placeholder, bundleTemp)

   message("Validating bundle")
   .rs.odbcBundleValidate(bundleFileTemp, md5)
   
   message("Extracting bundle")
   .rs.odbcBundleExtract(bundleFileTemp, installPath)
   
   message("Inspecting driver")
   driverPath <- .rs.odbcBundleFindDriver(name, installPath, libraryPattern)
   
   message("Registering driver")
   .rs.odbcBundleRegister(name, driverPath, version, installPath)

   message("")
   message("Installation complete")

   invisible(NULL)
})

.rs.addFunction("connectionOdbcRStudioDriver", function() {
   " with RStudio Driver"
})
