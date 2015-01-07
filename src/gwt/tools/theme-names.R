## A script that helps in updating AceThemes.java.

dir <- "/Users/kevin/git/rstudio/src/gwt/src/org/rstudio/studio/client/workbench/views/source/editors/text/themes"
files <- list.files(dir, pattern = "css$")
names <- sub("\\.css$", "", files)

.simpleCap <- function(x) {
   paste(unlist(lapply(x, function(x) {
      s <- strsplit(x, " ")[[1]]
      paste(toupper(substring(s, 1, 1)), substring(s, 2),
            sep = "", collapse = " ")
   })))
}

capcase <- unlist(lapply(names, function(x) {
   splat <- strsplit(x, "_")[[1]]
   paste(.simpleCap(splat), collapse = " ")
}))

Kmisc::cat.cb(sprintf("public static final String %s = \"%s\";",
                      toupper(names),
                      capcase), sep = "\n")

Kmisc::cat.cb(sprintf("addTheme(%s, res.%s());",
              toupper(names),
              names), sep = "\n")

Kmisc::cat.cb(sprintf("@Source(\"%s\")\nStaticDataResource %s();\n",
                      paste(names, ".css", sep = ""),
                      names), sep = "\n")
