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

odbc_bundle_name <- function(placeholder) {
  os_mapping <- list (
    Linux = "linux",
    Windows = "windows",
    Darwin = "osx"
  )
  if (!Sys.info()["sysname"] %in% names(os_mapping))
    stop("Operating system \"", Sys.info()["sysname"], "\" is unsupported")
  
  os_name <- os_mapping[[Sys.info()[["sysname"]]]]
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

odbc_bundle_install <- function(url, placeholder, install_path) {
  install_path <- normalizePath(install_path, mustWork = FALSE)
  
  odbc_bundle_extract(url, placeholder, install_path)
}

odbc_bundle_install(
  url = "http://odbc-drivers-path/",
  placeholder = "oracle-(os)-x(bitness).tar.gz",
  install_path = "~/RStudio-Drivers/oracle"
)