root <- rprojroot::find_rstudio_root_file()
setwd(root)

transformations <- list(
   
   list(
      name = "Integer",
      type = "int",
      null = "0"
   ),
   
   list(
      name = "Number",
      type = "double",
      null = "0"
   ),
   
   list(
      name = "String",
      type = "String",
      null = "null"
   ),
   
   list(
      name = "Boolean",
      type = "boolean",
      null = "false"
   )
   
)

SOURCE <- "src/gwt/src/org/rstudio/core/client/JsVector.java"

contents <- readLines(SOURCE)
lapply(transformations, function(transformation) {
   
   replaced <- contents
   
   name <- transformation$name
   type <- transformation$type
   null <- transformation$null
   
   fileName <- paste("JsVector", name, ".java", sep = "")
   className <- paste("JsVector", name, sep = "")
   
   replaced <- gsub(" <T> ", " ", replaced)
   replaced <- gsub("<T>", "", replaced)
   replaced <- gsub("JsVector<T>", className, replaced)
   replaced <- gsub("JsVector", className, replaced)
   replaced <- gsub("\\bT\\b", type, replaced)
   replaced <- gsub("\\bnull\\b", null, replaced)
   
   target <- file.path(dirname(SOURCE), fileName)
   writeLines(replaced, con = target, sep = "\n")
   
})
