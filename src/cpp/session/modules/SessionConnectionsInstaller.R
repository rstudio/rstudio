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

odbc_bundle_os_name <- function() {
   os_mapping <- list (
      linux = "linux",
      windows = "windows",
      darwin = "osx"
   )
   
   if (!tolower(Sys.info()["sysname"]) %in% names(os_mapping))
      stop("Operating system \"", Sys.info()["sysname"], "\" is unsupported.")
   
   os_mapping[[tolower(Sys.info()[["sysname"]])]]
}

odbc_bundle_name <- function(placeholder) {
   os_name <- odbc_bundle_os_name()
   bitness <- if (grepl("64", Sys.info()["machine"])) "64" else "32"
   
   bundle_name <- gsub("\\(os\\)", os_name, placeholder)
   bundle_name <- gsub("\\(bitness\\)", bitness, bundle_name)
   
   bundle_name
}

odbc_bundle_download <- function(url, placeholder, bundle_temp) {
   bundle_name <- odbc_bundle_name(placeholder)
   bundle_url <- paste(url, bundle_name, sep = "")
   
   if (exists(bundle_temp)) unlink(bundle_temp, recursive = TRUE)
   dir.create(bundle_temp, recursive = TRUE)
   
   bundle_file_temp <- file.path(bundle_temp, bundle_name)
   download.file(bundle_url, bundle_file_temp)
   
   bundle_file_temp
}

odbc_bundle_extract <- function(bundle_file_temp, install_path) {
   untar(bundle_file_temp, exdir = install_path)
}

odbc_bundle_check_prereqs_unixodbc <- function() {
   identical(system2("odbcinst", stdout = FALSE), 1L)
}

odbc_bundle_check_prereqs_brew <- function() {
   identical(system2("brew", stdout = FALSE), 1L)
}

odbc_bundle_check_prereqs_osx <- function() {
   if (!odbc_bundle_check_prereqs_unixodbc()) {
      if (!odbc_bundle_check_prereqs_brew()) {
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
   
   if (!odbc_bundle_check_prereqs_unixodbc())
      stop("Failed to install unixODBC, please install from www.unixodbc.org")
}

odbc_bundle_check_prereqs_linux <- function() {
   if (!odbc_bundle_check_prereqs_unixodbc())
      stop("unixODBC is not installed, please install from www.unixodbc.org")
}

odbc_bundle_registry_add <- function(entries) {
   validate_entry <- function(entry) {
      tryCatch({
         verify <- readRegistry(entry$path, "HLM")
         identical(verify[[entry$key]], entry$value)
      }, error = function(e) {
         FALSE
      })
   }

   odbc_file_escape <- function(value) {
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
              odbc_file_escape(entry$value),
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
}

odbc_bundle_registry_delete <- function(path) {
   system2(
      "REG",
      args = list(
         "DELETE",
         shQuote(path),
         "/f"
      )
   )
   
   identical(ret, 0L)
}

odbc_bundle_check_prereqs_windows <- function() {
}

odbc_bundle_check_prereqs <- function() {
   os_prereqs <- list(
      osx = odbc_bundle_check_prereqs_osx,
      windows = odbc_bundle_check_prereqs_windows,
      linux = odbc_bundle_check_prereqs_linux
   )
   
   prereqs <- os_prereqs[[odbc_bundle_os_name()]]
   prereqs()
}

odbc_bundle_odbcinst_path <- function() {
   config <- system2("odbcinst", "-j", stdout = TRUE)
   odbcini_entry <- config[grepl("odbcinst.ini", config)]
   gsub("^[^/\\\\]*", "", odbcini_entry)
}

odbc_bundle_read_ini <- function(odbcinst_path) {
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
}

odbc_bundle_write_ini <- function(odbcinst_path, data) {
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
   
   writeLines(lines, odbcinst_path)
}

odbc_bundle_register_linux <- function(name, driver_path) {
   # Find odbcinst.ini file
   odbcinst_path <- odbc_bundle_odbcinst_path()
   
   # Read odbcinst.ini
   odbcinst <- odbc_bundle_read_ini(odbcinst_path)
   
   # Set odbcinst.ini entries
   odbcinst[[name]] <- list(
      paste("Driver", "=", driver_path)
   )
   
   # Write odbcinst.ini
   odbc_bundle_write_ini(odbcinst_path, odbcinst)
}

odbc_bundle_register_windows <- function(name, driver_path) {
   odbc_bundle_registry_add(
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
}

odbc_bundle_find_driver <- function(name, install_path) {
   os_extensions <- list(
      osx = "dylib",
      windows = "dll",
      linux = "so"
   )
   
   os_extension <- os_extensions[[odbc_bundle_os_name()]]
   driver_name <- gsub(" ", "", name)
   
   driver_pattern <- paste(
      driver_name,
      "[^/\\\\]+\\.",
      os_extension,
      sep = ""
   )
   
   all_files <- dir(install_path, recursive = TRUE, full.names = TRUE)
   driver_path <- all_files[grepl(driver_pattern, all_files, ignore.case = TRUE)]
   
   if (!identical(length(driver_path), 1L))
      stop("Failed to find odbc driver inside driver bundle.")
   
   normalizePath(driver_path)
}

odbc_bundle_register <- function(name, driver_path) {
   os_registrations <- list(
      osx = odbc_bundle_register_linux,
      windows = odbc_bundle_register_windows,
      linux = odbc_bundle_register_linux
   )
   
   os_registration <- os_registrations[[odbc_bundle_os_name()]]
   
   os_registration(name, driver_path) 
}

odbc_bundle_install <- function(name, url, placeholder, install_path) {
   install_path <- file.path(
      normalizePath(install_path, mustWork = FALSE),
      tolower(name)
   )
   
   bundle_temp <- tempfile()
   on.exit(unlink(bundle_temp, recursive = TRUE), add = TRUE)

   message("Installation path: ", install_path)
   message("Installing ", name, "...")
   
   message("Checking prerequisites...")
   odbc_bundle_check_prereqs()
   
   message("Downloading driver...")
   bundle_file_temp <- odbc_bundle_download(url, placeholder, bundle_temp)
   
   message("Extracting driver...")
   odbc_bundle_extract(bundle_file_temp, install_path)
   
   message("Inspecting driver...")
   driver_path <- odbc_bundle_find_driver(name, install_path)
   
   message("Registering driver...")
   odbc_bundle_register(name, driver_path)

   message("")
   message("Installation complete!")

   invisible(NULL)
}