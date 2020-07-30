#
# Options.R
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

# get version
.rs.addGlobalFunction("RStudio.Version", function()
{
   .rs.api.versionInfo()
})

# custom browseURL implementation.
.rs.setOption("browser", function(url)
{
   .Call("rs_browseURL", url, PACKAGE = "(embedding)")
})

# default viewer option if not already set
.rs.setOptionDefault("viewer", function(url, height = NULL)
{
   if (!is.character(url) || (length(url) != 1))
      stop("url must be a single element character vector.", call. = FALSE)
   
   if (identical(height, "maximize"))
      height <- -1
   
   if (!is.null(height) && (!is.numeric(height) || (length(height) != 1)))
      stop("height must be a single element numeric vector or 'maximize'.", call. = FALSE)
   
   invisible(.Call("rs_viewer", url, height, PACKAGE = "(embedding)"))
})

# default page_viewer option if not already set
.rs.setOptionDefault("page_viewer", function(url,
                                             title = "RStudio Viewer",
                                             self_contained = FALSE)
{
   if (!is.character(url) || (length(url) != 1))
      stop("url must be a single element character vector.", call. = FALSE)
   
   if (!is.character(title) || (length(title) != 1))
      stop("title must be a single element character vector.", call. = FALSE)
   
   if (!is.logical(self_contained) || (length(self_contained) != 1))
      stop("self_contained must be a single element logical vector.", call. = FALSE)
   
   invisible(.Call("rs_showPageViewer", url, title, self_contained, PACKAGE = "(embedding)"))
})

# default shinygadgets.showdialog if not already set
.rs.setOptionDefault("shinygadgets.showdialog", function(caption,
                                                         url,
                                                         width = NULL,
                                                         height = NULL)
{
   if (!is.character(caption) || (length(caption) != 1))
      stop("caption must be a single element character vector.", call. = FALSE)
   
   if (!is.character(url) || (length(url) != 1))
      stop("url must be a single element character vector.", call. = FALSE)
   
   # default width and height
   if (is.null(width))
      width <- 600
   if (is.null(height))
      height <- 600
   
   # validate width and height
   if (!is.numeric(width) || (length(width) != 1))
      stop("width must be a single element numeric vector.", call. = FALSE)
   if (!is.numeric(height) || (length(height) != 1))
      stop("height must be a single element numeric vector.", call. = FALSE)
   
   invisible(.Call("rs_showShinyGadgetDialog", caption, url, width, height, PACKAGE = "(embedding)"))
})

# provide askpass function
.rs.setOption("askpass", function(prompt)
{
   .rs.askForPassword(prompt)
})

# provide asksecret function
.rs.setOption("asksecret", function(name,
                                    title = name,
                                    prompt = paste(name, ":", sep = ""))
{
   .rs.askForSecret(name, title, prompt)
})

# provide restart function
.rs.setOption("restart", function(afterRestartCommand = "")
{
   .rs.restartR(afterRestartCommand)
})

# custom pager implementation
.rs.setOption("pager", function(files, header, title, delete.file)
{
   .rs.pager(files, header, title, delete.file)
})

# never allow graphical menus
options(menu.graphics = FALSE)

# set max print to 1000 if not already set
if (is.null(getOption("max.print")) || (getOption("max.print") == 99999)) {
   options(max.print = 1000)
}

# set max print so that the DOM won't go haywire showing large datasets
if (getOption("max.print", 10000) > 10000) {
   options(max.print = 10000)
}

# set RStudio as the GUI
local({
   platform = .Platform
   if (platform$GUI != "RStudio") {
      platform$GUI = "RStudio"
      unlockBinding(".Platform", asNamespace("base"))
      assign(".Platform", platform, inherits = TRUE)
      lockBinding(".Platform", asNamespace("base"))
   }
})

# set default x display (see below for comment on why we need to do this)
if (is.na(Sys.getenv("DISPLAY", unset = NA)))
   Sys.setenv(DISPLAY = ":0")

# the above two display oriented command affect the behavior of edit.data.frame
# and edit.matrix as follows: these methods will use .Internal(edit, ...) rather
# than .Internal(dataentry, ...) if DISPLAY == "" or if the .Platform$GUI is
# "unknown". since we plan on running on a server without X available we need
# to manually make sure that the DISPLAY environment variable exists and that
# the .Platform$GUI is not "unknown"

# configure profvis to use custom path to store profiles
options(profvis.output_path = NULL)

# configure profvis to not delete generated profiles
options(profvis.keep_output = TRUE)

# indicate that we're not in a notebook by default
options(rstudio.notebook.executing = FALSE)

# provide a custom HTTP user agent
.rs.initHttpUserAgent()
