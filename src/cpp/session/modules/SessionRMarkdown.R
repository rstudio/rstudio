#
# SessionRMarkdown.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

.rs.addFunction("scalarListFromList", function(l)
{
   # hint that every non-list element of the hierarchical list l
   # is a scalar value if it is of length 1
   l <- lapply(l, function(ele) {
      if (is.null(ele))
         NULL
      else if (is.list(ele)) 
         .rs.scalarListFromList(ele)
      else if (length(ele) == 1) {
         # mark strings with encoding unless already marked (see comment
         # below in convert_to_yaml)
         if (is.character(ele) && Encoding(ele) == "unknown")
            Encoding(ele) <- "UTF-8"
         .rs.scalar(ele)
      }
      else
         ele
   })
})

.rs.addFunction("updateRMarkdownPackage", function(archive) 
{
  pkgDir <- find.package("rmarkdown")
  .rs.forceUnloadPackage("rmarkdown")
  .Call(.rs.routines$rs_installPackage,  archive, dirname(pkgDir))
})

.rs.addFunction("getCustomRenderFunction", function(file) {
  lines <- readLines(file, warn = FALSE)

  yamlFrontMatter <- tryCatch(
    rmarkdown:::parse_yaml_front_matter(lines),
    error=function(e) {
       list()
    })

  if (is.character(yamlFrontMatter$knit))
    yamlFrontMatter$knit[[1]]
  else if (!is.null(yamlFrontMatter$runtime) && 
           identical(yamlFrontMatter$runtime, "shiny")) {
    # use run as a wrapper for render when the doc requires the Shiny runtime,
    # and outputs HTML. 
    tryCatch({
       outputFormat <- rmarkdown:::output_format_from_yaml_front_matter(lines)
       formatFunction <- eval(parse(text = outputFormat$name), 
                              envir = asNamespace("rmarkdown"))
       if (identical(tolower(tools::file_ext(
            rmarkdown:::pandoc_output_file("shiny", formatFunction()$pandoc))), 
            "html"))
          "rmarkdown::run"
       else 
          # this situation is nonsensical (runtime: shiny only makse sense for
          # HTML-based output formats)
          ""
     }, error = function(e) {
        ""
     })
  }
  else
    ""
})

# given a path to a folder on disk, return information about the R Markdown
# template in that folder.
.rs.addFunction("getTemplateDetails", function(path) {
   # check for required files
   templateYaml <- file.path(path, "template.yaml")
   skeletonPath <- file.path(path, "skeleton")
   if (!file.exists(templateYaml))
      return(NULL)
   if (!file.exists(file.path(skeletonPath, "skeleton.Rmd")))
      return(NULL)

   # load template details from YAML
   templateDetails <- yaml::yaml.load_file(templateYaml)

   # enforce create_dir if there are multiple files in /skeleton/
   if (length(list.files(skeletonPath)) > 1) 
      templateDetails$create_dir <- TRUE

   templateDetails
})


.rs.addFunction("evaluateRmdParams", function(contents) {

   # extract the params using knitr::knit_params
   knitParams <- knitr::knit_params(contents)

   if (length(knitParams) > 0)
   {
      # turn them into a named list
      params <- list()
      for (param in knitParams)
         params[[param$name]] <- param$value

      # inject into global environment
      assign("params", params, envir = globalenv())
   }
})

.rs.addJsonRpcHandler("convert_to_yaml", function(input)
{
   yaml <- yaml::as.yaml(input)

   # the yaml package produces UTF-8 output strings, but doesn't mark them
   # as such, which leads to trouble (in particular: on Windows the string
   # may be later interpreted in system default encoding, which is not UTF-8.)
   # see: https://github.com/viking/r-yaml/issues/6
   if (Encoding(yaml) == "unknown")
      Encoding(yaml) <- "UTF-8"

   list(yaml = .rs.scalar(yaml))
})

.rs.addJsonRpcHandler("convert_from_yaml", function(yaml)
{
   data <- list()
   parseError <- ""
   parseSucceeded <- FALSE
   tryCatch(
   {
      data <- .rs.scalarListFromList(yaml::yaml.load(yaml))
      parseSucceeded <- TRUE
   },
   error = function(e)
   {
      parseError <<- as.character(e)
   })
   list(data = data, 
        parse_succeeded = .rs.scalar(parseSucceeded),
        parse_error = .rs.scalar(parseError))
})


.rs.addJsonRpcHandler("rmd_output_format", function(input, encoding) {
  formats <- rmarkdown:::enumerate_output_formats(input, encoding = encoding)
  if (is.character(formats))
    .rs.scalar(formats[[1]])
  else
    NULL
})

