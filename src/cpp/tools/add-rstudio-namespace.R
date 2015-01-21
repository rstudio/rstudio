#!/usr/bin/env Rscript

# Add the 'rstudio' top-level namespace to every part of our code base
# where we've used 'core' or 'session'. This script is mostly a terrible
# hack, but hey, it works!

strip <- function(x) {
   x <- gsub("'.*(?<!\\\\)'", "", x, perl = TRUE)
   x <- gsub('".*(?<!\\\\)"', "", x, perl = TRUE)
   gsub("\\s*//.*", "", x, perl = TRUE)
}

add_rstudio_namespace <- function(file, nsNames) {
   
   content <- suppressWarnings(readLines(file))
   stripped <- strip(content)
   
   for (nsName in nsNames)
   {
      startPattern <- paste("^\\s*namespace\\s+", nsName, "\\s*\\{", sep = "")
      namespaceStartPos <- grep(startPattern, stripped, perl = TRUE)
      
      for (i in namespaceStartPos)
      {
         if (grepl("namespace", stripped[[i - 1]]))
            next
         
         j <- i + 1
         braceCount <- 1
         n <- length(stripped)
         
         while (j <= n)
         {
            line <- stripped[[j]]
            openBraces <- gregexpr("{", line, fixed = TRUE)[[1]]
            closedBraces <- gregexpr("}", line, fixed = TRUE)[[1]]
            
            if (openBraces[[1]] != -1)
               braceCount <- braceCount + length(openBraces)
            
            if (closedBraces[[1]] != -1)
               braceCount <- braceCount - length(closedBraces)
            
            if (braceCount <= 0)
            {
               if (braceCount < 0)
                  warning("Unexpected brace stack")
               
               content[[i]] <- paste("namespace rstudio {", content[[i]], sep = "\n")
               
               if (grepl("^\\s*\\}\\s*//\\s*name", content[[j]], perl = TRUE))
               {
                  closeNamespace <-
                     paste(content[[j]], "} // namespace rstudio", sep = "\n")
               }
               else
               {
                  closeNamespace <-
                     paste(content[[j]], "}", sep = "\n")
               }
               
               content[[j]] <- closeNamespace
               break
            }
            j <- j + 1
         }
         
      }
      
      # Re-normalize content
      splat <- strsplit(content, "\n", fixed = TRUE)
      splat <- lapply(splat, function(x) if (!length(x)) "" else x)
      content <- unlist(splat)
      
      content <- gsub("^(\\s*)using namespace core([^;]*);\\s*$",
                      "\\1using namespace rstudio::core\\2;",
                      content,
                      perl = TRUE)
      
      content <- gsub("^(\\s*)using namespace desktop([^;]*);\\s*$",
                      "\\1using namespace rstudio::desktop\\2;",
                      content,
                      perl = TRUE)
      
      stripped <- strip(content)
   }
   
   # We have to define error codes for some items
   # within the boost namespace. When these are
   # encountered we need a fully qualified namespace.
   content <- gsub(
      "\\<(.*)::errc::errc_t\\>",
      "<rstudio::\\1::errc::errc_t>",
      content,
      perl = TRUE
   )
      
   
   cat(content, file = file, sep = "\n")
   
}

# files <- "src/cpp/core/include/core/json/JsonRpc.hpp"
files <- list.files(
   path = "src/cpp",
   pattern = "cpp$|hpp$|h$|mm$|cpp\\.in$|hpp\\.in$",
   full.names = TRUE,
   recursive = TRUE
)

r_files <- grep("^src/cpp/r/", files, value = TRUE)
files <- setdiff(files, r_files)

nsNames <- c("core", "monitor", "session", "desktop", 
             "diagnostics", "server", "session_proxy")
for (file in files)
   add_rstudio_namespace(file, nsNames)

for (file in r_files)
   add_rstudio_namespace(file, c("core", "r"))

# A couple files need special handling
add_rstudio_namespace("src/cpp/session/include/session/SessionModuleContext.hpp", "r")

prefix_core_with_rstudio <- function(file) {
   content <- suppressWarnings(readLines(file))
   content <- gsub("core::", "rstudio::core::", content, fixed = TRUE)
   cat(content, file = file, sep = "\n")
}

prefix_core_with_rstudio("src/cpp/core/Assert.cpp")

fix_up_main <- function(file) {
   content <- suppressWarnings(readLines(file))
   content <- gsub("^(\\s*)using namespace rstudio::core\\s*;",
                   "\\1using namespace rstudio;\n\\1using namespace rstudio::core;",
                   content,
                   perl = TRUE)
   cat(content, file = file, sep = "\n")
}

main_files <- files[grep("Main.cpp", basename(files))]
lapply(main_files, function(file)
   fix_up_main(file))

add_using_rstudio_namespace <- function(file) {
   
   content <- suppressWarnings(readLines(file))
   
   if (!any(grepl("#include <core", content, perl = TRUE)))
      return()
   
   if (any(grepl("using namespace rstudio;", content)))
      return()
   
   idx <- grep("using namespace", content)
   sep <- "\n"
   if (!length(idx))
   {
      idx <- grep("[@{]", strip(content), perl = TRUE)[[1]]
      sep <- "\n\n"
   }
   idx <- idx[[1]]
   indent <- gsub("^(\\s*).*", "\\1", content[[idx]])
   content[[idx]] <- paste0(indent, "using namespace rstudio;", sep, content[[idx]])
   cat(content, file = file, sep = "\n")
}

needs_rstudio_namespace <- grep("mm$", files, value = TRUE)

lapply(needs_rstudio_namespace, function(file)
   add_using_rstudio_namespace(file))

webview <- "src/cpp/desktop-mac/WebViewController.mm"
content <- readLines(webview)
content[[23]] <- "using namespace rstudio;\n"
cat(content, file = webview, sep = "\n")

prefix_r_with_rstudio <- function(file) {
   content <- suppressWarnings(readLines(file))
   content <- gsub("using namespace r::([^;]*);",
                   "using namespace rstudio::r::\\1;",
                   content,
                   perl = TRUE)
   cat(content, file = file, sep = "\n")
}

session_files <- grep("^src/cpp/session", files, value = TRUE)
lapply(session_files, function(file)
   prefix_r_with_rstudio(file))
