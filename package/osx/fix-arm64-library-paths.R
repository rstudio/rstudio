#!/usr/bin/env R --vanilla -s -f

args <- commandArgs(trailingOnly = TRUE)

if (length(args) == 0) {
   message("Usage: fix-arm64-library-paths.R [x86_64 library path] [arm64 library path]")
   quit("no")
}

fix_paths <- function(dir, prefix) {
   
   owd <- setwd(dir)
   on.exit(setwd(owd), add = TRUE)
   
   # list libraries
   dylibs <- list.files(pattern = "[.]dylib$")
   
   # don't work with symlinks
   links <- nzchar(Sys.readlink(dylibs))
   dylibs <- dylibs[!links]
   
   for (dylib in dylibs) {

      # set library id
      system(paste("install_name_tool -id", dylib, dylib))
      
      # read library paths
      fmt <- "otool -L %s | tail -n+2 | cut -d' ' -f1 | sed 's|\t||g'"
      cmd <- sprintf(fmt, dylib)
      paths <- system(cmd, intern = TRUE)
      
      # remap the homebrew libraries
      old <- grep("homebrew", paths, value = TRUE)
      if (length(old) == 0)
         next
      
      new <- paste(prefix, basename(old), sep = "/")
      .mapply(function(lhs, rhs) {
         system2("install_name_tool", c(
            "-change", shQuote(lhs), shQuote(rhs),
            dylib
         ))
      }, list(old, new), NULL)
      
   }
   
}

# x86_64 paths
fix_paths(args[[1L]], "@executable_path/../Frameworks")

# arm64 paths
fix_paths(args[[2L]], "@executable_path/../Frameworks/arm64")
