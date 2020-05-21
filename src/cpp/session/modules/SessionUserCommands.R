#
# SessionUserCommands.R
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

assign(".rs.userCommands", new.env(parent = emptyenv()), envir = .rs.toolsEnv())

.rs.addFunction("isValidShortcut", function(shortcut)
{
   if (!is.character(shortcut))
      return(FALSE)
   
   # TODO
   TRUE
})

.rs.addFunction("normalizeKeyboardShortcut", function(shortcut)
{
   # A shortcut may be a vector of 'strings', each to be pressed
   # in sequence to trigger the shortcut. Normalize each set and
   # then paste together.
   normalized <- lapply(shortcut, function(shortcut) {
      
      # Ensure lower case
      shortcut <- tolower(shortcut)
      
      # Normalize aliases
      aliases <- list(
         "ctrl" = "control",
         "cmd" = c("meta", "command", "win", "super")
      )
      
      for (i in seq_along(aliases))
      {
         destination <- names(aliases)[[i]]
         potentials <- aliases[[i]]
         for (item in potentials)
         {
            bounded <- paste("\\b", item, "\\b", sep = "")
            shortcut <- gsub(bounded, destination, shortcut, perl = TRUE)
         }
      }
      
      # Normalize modifier key names
      for (modifier in c("ctrl", "alt", "cmd", "shift"))
      {
         reFrom <- paste(modifier, "\\s*[-+]\\s*", sep = "")
         reTo <- paste(modifier, "-", sep = "")
         shortcut <- gsub(reFrom, reTo, shortcut, perl = TRUE)
      }
      
      shortcut
      
   })
   
   paste(normalized, collapse = " ")
   
})

.rs.addFunction("registerUserCommand", function(name, shortcuts, fn, overwrite = TRUE)
{
   if (length(name) != 1 || !is.character(name))
      stop("'name' should be a length-one character vector")
   
   if (!overwrite && exists(name, envir = .rs.userCommands)) {
      stop("'", name, "' is already bound to a command; use 'overwrite = TRUE'",
           "to overwrite with the new command definition.")
   }
   
   shortcuts <- unlist(lapply(shortcuts, .rs.normalizeKeyboardShortcut))
   .rs.userCommands[[name]] <- fn
   .Call("rs_registerUserCommand", .rs.scalar(name), shortcuts)
   
   TRUE
})

.rs.addFunction("loadUserCommands", function(keybindingPath)
{
   env <- new.env(parent = globalenv())
   env$registerUserCommand <- .rs.registerUserCommand
   
   # load user commands from pre-1.3 RStudio folder if present, then from the configured user
   # command folder
   paths <- c("~/.R/keybindings", keybindingPath)
   for (path in paths)
   {
      files <- list.files(file.path(path, "R"), full.names = TRUE)
      lapply(files, function(file)
      {
         source(file, local = env)
      })
   }
})
