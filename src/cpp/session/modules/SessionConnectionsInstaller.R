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
    Linux = "linux",
    Windows = "windows",
    Darwin = "osx"
  )
  
  if (!Sys.info()["sysname"] %in% names(os_mapping))
    stop("Operating system \"", Sys.info()["sysname"], "\" is unsupported")
  
  os_mapping[[Sys.info()[["sysname"]]]]
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

odbc_bundle_extract <- function(url, placeholder, install_path) {
  bundle_temp <- tempfile()
  on.exit(unlink(bundle_temp, recursive = TRUE), add = TRUE)
  
  bundle_file_temp <- odbc_bundle_download(url, placeholder,  bundle_temp)
  
  untar(bundle_file_temp, exdir = install_path)
  
  install_path
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

odbc_bundle_check_prereqs_windows <- function() {
  
}

odbc_bundle_check_prereqs <- function() {
  os_prereqs <- list(
    osx = odbc_bundle_check_prereqs_osx,
    windows = odbc_bundle_check_prereqs_windows,
    linux = odbc_bundle_check_prereqs_linux
  )
  
  prereqs <- os_prereqs[[odbc_bundle_os_name()]]
  message("Checking Prerequisites...")
  prereqs()
}

odbc_bundle_odbcini_path <- function() {
  config <- system2("odbcinst", "-j", stdout = TRUE)
  odbcini_entry <- config[grepl("odbcinst.ini", config)]
  gsub("^[^/\\\\]*", "", odbcini_entry)
}

odbc_bundle_register <- function() {
  
}

odbc_bundle_install <- function(url, placeholder, install_path) {
  install_path <- normalizePath(install_path, mustWork = FALSE)
  
  # Check prerequisites
  odbc_bundle_check_prereqs()
  
  # Download and extract
  odbc_bundle_extract(url, placeholder, install_path)
  
  # Register
  odbc_bundle_register()
}

odbc_bundle_install(
  url = "http://odbc-drivers-path/",
  placeholder = "oracle-(os)-x(bitness).tar.gz",
  install_path = "~/RStudio-Drivers/oracle"
)