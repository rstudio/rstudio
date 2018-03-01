#
# SessionConnectionsInstaller.R
#
# Copyright (C) 2009-18 by RStudio, Inc.
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
   os_mapping <- list (
      linux = "linux",
      windows = "windows",
      darwin = "osx"
   )
   
   if (!tolower(Sys.info()["sysname"]) %in% names(os_mapping))
      stop("Operating system \"", Sys.info()["sysname"], "\" is unsupported.")
   
   os_mapping[[tolower(Sys.info()[["sysname"]])]]
})

.rs.addFunction("odbcBundleName", function(placeholder) {
   os_name <- .rs.odbcBundleOsName()
   bitness <- if (grepl("64", Sys.info()["machine"])) "64" else "32"
   
   bundle_name <- gsub("\\(os\\)", os_name, placeholder)
   bundle_name <- gsub("\\(bitness\\)", bitness, bundle_name)
   
   bundle_name
})

.rs.addFunction("odbcBundleDownload", function(url, placeholder, bundle_temp) {
   bundle_name <- .rs.odbcBundleName(placeholder)
   bundle_url <- paste(url, bundle_name, sep = "")
   
   if (exists(bundle_temp)) unlink(bundle_temp, recursive = TRUE)
   dir.create(bundle_temp, recursive = TRUE)
   
   bundle_file_temp <- file.path(bundle_temp, bundle_name)
   download.file(bundle_url, bundle_file_temp)
   
   bundle_file_temp
})

.rs.addFunction("odbcBundleExtract", function(bundle_file_temp, install_path) {
   untar(bundle_file_temp, exdir = install_path)
})

.rs.addFunction("odbcBundleCheckPrereqsUnixodbc", function() {
   identical(system2("odbcinst", stdout = FALSE), 1L)
})

.rs.addFunction("odbcBundleCheckPrereqsBrew", function() {
   identical(system2("brew", stdout = FALSE), 1L)
})

.rs.addFunction("odbcBundleCheckPrereqsOsx", function() {
   if (!.rs.odbcBundleCheckPrereqsUnixodbc()) {
      if (!.rs.odbcBundleCheckPrereqsBrew()) {
         message("Installing Brew...")
         system2(
            "/usr/bin/ruby",
            args = list(
               "-e",
               "\"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)\""
            )
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
   validate_entry <- function(entry) {
      tryCatch({
         verify <- readRegistry(entry$path, "HLM")
         identical(verify[[entry$key]], entry$value)
      }, error = function(e) {
         FALSE
      })
   }

   odbcFileEscape <- function(value) {
    gsub("\\\\", "\\\\\\\\", value)
   }

   if (all(sapply(entries, function(e) validate_entry(e))))
      return()

   all_added <- TRUE
   for (entry in entries) {
     full_path <- file.path("HKEY_LOCAL_MACHINE", entry$path, fsep = "\\")
     system2(
        "REG",
        args = list(
           "ADD",
           shQuote(full_path),
           "/v",
           shQuote(entry$key),
           "/t",
           "REG_SZ",
           "/d",
           shQuote(entry$value),
           "/f"
        )
     )

     if (!validate_entry(entry)) {
      all_added <- FALSE
      break
     }
   }

   if (!all_added) {
      message("Could not add registry keys from R, retrying using registry prompt.")
      add_reg <- tempfile(fileext = ".reg")

      line_entries <- sapply(entries, function(entry) {
         full_path <- file.path("HKEY_LOCAL_MACHINE", entry$path, fsep = "\\")
         c(
            paste("[", full_path, "]", sep = ""),
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
         line_entries
      )

      writeLines(lines, add_reg)

      message("Waiting for ", add_reg, " to be registered.")
      system2(
         "explorer",
         add_reg
      )

      all_entries_valid <- function() {
        all(sapply(entries, function(e) validate_entry(e)))
      }

      registry_start <- Sys.time()
      registry_wait <- 300
      while (!all_entries_valid() && Sys.time() < registry_start + registry_wait) {
         Sys.sleep(1)
      }

      if (!all_entries_valid()) {
         stop("Failed to add all registry keys using registry file.")
      }
   }
})

.rs.addFunction("odbcBundleRegistryRemove", function(entries) {
   validate_entry <- function(entry) {
      tryCatch({
         reg_entry <- readRegistry(entry$path, "HLM")
         if (!is.null(entry$key))
            is.null(reg_entry[[entry$key]])
         else
            FALSE
      }, error = function(e) {
         TRUE
      })
   }

   if (all(sapply(entries, function(e) validate_entry(e))))
      return()

   for (entry in entries) {
      full_path <- file.path("HKEY_LOCAL_MACHINE", entry$path, fsep = "\\")

      if (!is.null(entry$key)) {
         reg_args <- list(
            "DELETE",
            shQuote(full_path),
            "/v",
            shQuote(entry$key),
            "/f"
         )
      }
      else {
         reg_args <- list(
            "DELETE",
            shQuote(full_path),
            "/f"
         )
      }

      system2(
         "REG",
         args = reg_args
      )
   }

   if (all(sapply(entries, function(e) validate_entry(e))))
      return()

   add_reg <- tempfile(fileext = ".reg")

   line_entries <- sapply(entries, function(entry) {
      full_path <- file.path("HKEY_LOCAL_MACHINE", entry$path, fsep = "\\")

      if (is.null(entry$key)) {
         paste("[-", full_path, "]", sep = "")
      }
      else {
         c(
            paste("[", full_path, "]", sep = ""),
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
      unlist(line_entries)
   )

   writeLines(lines, add_reg)
   system2(
      "explorer",
      add_reg
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
   os_prereqs <- list(
      osx = .rs.odbcBundleCheckPrereqsOsx,
      windows = .rs.odbcBundleCheckPrereqsWindows,
      linux = .rs.odbcBundleCheckPrereqsLinux
   )
   
   prereqs <- os_prereqs[[.rs.odbcBundleOsName()]]
   prereqs()
})

.rs.addFunction("odbcBundleOdbcinstPath", function() {
   config <- system2("odbcinst", "-j", stdout = TRUE)
   odbcini_entry <- config[grepl("odbcinst.ini", config)]
   gsub("^[^/\\\\]*", "", odbcini_entry)
})

.rs.addFunction("odbcBundleReadIni", function(odbcinst_path) {
   lines <- readLines(odbcinst_path)
   data <- list()
   
   current_driver <- NULL
   
   for (line in lines) {
      # Is header?
      if (grepl(" *\\[[^]]+\\] *", line)) {
         current_driver <- gsub("^ *\\[|\\] *", "", line)
         data[[current_driver]] <- ""
      }
      else if (!grepl("^ *$", line)) {
         if (identical(data[[current_driver]], ""))
            data[[current_driver]] <- line
         else
            data[[current_driver]] <- c(
               data[[current_driver]],
               line
            )
      }
   }
   
   data
})

.rs.addFunction("odbcBundleWriteIni", function(odbcinst_path, data) {
   lines <- c()
   for (name in names(data)) {
      lines <- c(
         lines, 
         paste("[", name, "]", sep = "")
      )
      
      lines <- c(
         lines,
         unlist(data[[name]]),
         ""
      )
   }
   
   if (is.null(lines)) lines <- c("")

   writeLines(lines, odbcinst_path)
})

.rs.addFunction("odbcBundleRegisterLinux", function(name, driver_path) {
   # Find odbcinst.ini file
   odbcinst_path <- .rs.odbcBundleOdbcinstPath()
   
   # Read odbcinst.ini
   odbcinst <- .rs.odbcBundleReadIni(odbcinst_path)
   
   # Set odbcinst.ini entries
   odbcinst[[name]] <- list(
      paste("Driver", "=", driver_path)
   )
   
   # Write odbcinst.ini
   .rs.odbcBundleWriteIni(odbcinst_path, odbcinst)
})

.rs.addFunction("odbcBundleRegisterWindows", function(name, driver_path) {
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
            value = driver_path
         )
      )
   )
})

.rs.addFunction("odbcBundleFindDriver", function(name, install_path, library_pattern) { 
   os_extensions <- list(
      osx = "dylib$",
      windows = "dll$",
      linux = ".so$"
   )
   
   os_extension <- os_extensions[[.rs.odbcBundleOsName()]]
   driver_name <- gsub(" ", "", name)
   
   if (is.null(library_pattern)) {
      library_pattern <- paste(
         driver_name,
         "[^/\\\\]+\\.",
         os_extension,
         sep = ""
      )
   }
   
   all_files <- dir(install_path, recursive = TRUE, full.names = TRUE)
   driver_path <- all_files[grepl(library_pattern, all_files, ignore.case = TRUE)]

   if (!identical(length(driver_path), 1L))
      stop("Failed to find ", library, " inside driver bundle.")
   
   normalizePath(driver_path)
})

.rs.addFunction("odbcBundleRegister", function(name, driver_path) {
   os_registrations <- list(
      osx = .rs.odbcBundleRegisterLinux,
      windows = .rs.odbcBundleRegisterWindows,
      linux = .rs.odbcBundleRegisterLinux
   )
   
   os_registration <- os_registrations[[.rs.odbcBundleOsName()]]
   
   os_registration(name, driver_path) 
})

.rs.addFunction("odbcBundleValidate", function(bundle_file, md5) {
   if (!is.null(md5) && nchar(md5) > 0) {
      valid_md5s <- strsplit(as.character(md5), ",")[[1]]
      bundle_md5 <- tools::md5sum(bundle_file)
      if (!bundle_md5 %in% valid_md5s) {
         stop("Failed to validate bundle with signature ", md5, " but got ", bundle_md5, " instead.")
      }
   }
})

.rs.addFunction("odbcBundleInstall", function(name, url, placeholder, install_path, library_pattern = NULL, md5 = NULL) {
   install_path <- file.path(
      normalizePath(install_path, mustWork = FALSE),
      tolower(name)
   )
   
   bundle_temp <- tempfile()
   on.exit(unlink(bundle_temp, recursive = TRUE), add = TRUE)

   message("Installation path: ", install_path)
   message("Installing ", name)
   
   message("Checking prerequisites")
   .rs.odbcBundleCheckPrereqs()
   
   message("Downloading driver")
   bundle_file_temp <- .rs.odbcBundleDownload(url, placeholder, bundle_temp)

   message("Validating driver")
   .rs.odbcBundleValidate(bundle_file_temp, md5)
   
   message("Extracting driver")
   .rs.odbcBundleExtract(bundle_file_temp, install_path)
   
   message("Inspecting driver")
   driver_path <- .rs.odbcBundleFindDriver(name, install_path, library_pattern)
   
   message("Registering driver")
   .rs.odbcBundleRegister(name, driver_path)

   message("")
   message("Installation complete")

   invisible(NULL)
})