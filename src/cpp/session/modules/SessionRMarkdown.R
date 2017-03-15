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

.rs.addFunction("isREADME", function(file) {
  identical(tools::file_path_sans_ext(basename(file)), "README")
})

.rs.addFunction("updateRMarkdownPackage", function(archive) 
{
  pkgDir <- find.package("rmarkdown")
  .rs.forceUnloadPackage("rmarkdown")
  .Call("rs_installPackage",  archive, dirname(pkgDir))
})

.rs.addFunction("getRmdRuntime", function(file) {
   lines <- readLines(file, warn = FALSE)

   yamlFrontMatter <- tryCatch(
     rmarkdown:::parse_yaml_front_matter(lines),
     error=function(e) {
        list()
     })

   if (!is.null(yamlFrontMatter$runtime))
      yamlFrontMatter$runtime
    else
      ""
})

.rs.addFunction("getCustomRenderFunction", function(file) {
  lines <- readLines(file, warn = FALSE)

  yamlFrontMatter <- tryCatch(
    rmarkdown:::parse_yaml_front_matter(lines),
    error=function(e) {
       list()
    })

  if (is.character(yamlFrontMatter[["knit"]]))
    yamlFrontMatter[["knit"]][[1]]
  else if (!is.null(yamlFrontMatter$runtime) &&
           grepl('^shiny', yamlFrontMatter$runtime)) {
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
  else {
    # return render_site if we are in a website and this isn't a README
    if (!.rs.isREADME(file)) {
       siteGenerator <- tryCatch(rmarkdown::site_generator(file),
                                 error = function(e) NULL)
       if (!is.null(siteGenerator))
         "rmarkdown::render_site"
       else
         ""
    }
    else {
       ""
    }
  }
})

# given an input file, return the associated output file, and attempt to deduce
# whether it's up to date (e.g. for input.Rmd producing input.html, see whether
# input.html exists and has been written since input.Rmd)
.rs.addFunction("getRmdOutputInfo", function(target) {
  # compute the name of the output file
  lines <- readLines(target, warn = FALSE)
  outputFormat <- rmarkdown:::output_format_from_yaml_front_matter(lines)
  outputFormat <- rmarkdown:::create_output_format(
                                    outputFormat$name, outputFormat$options)
  outputFile <- rmarkdown:::pandoc_output_file(target, outputFormat$pandoc)
  outputPath <- file.path(dirname(target), outputFile) 

  # ensure output file exists
  current <- file.exists(outputPath) && 
             file.info(outputPath)$mtime >= file.info(target)$mtime
  
  return(list(
    output_file = .rs.scalar(outputPath),
    is_current  = .rs.scalar(current)))
})

# given a path to a folder on disk, return information about the R Markdown
# template in that folder.
.rs.addFunction("getTemplateDetails", function(path) {
   # check for required files
   templateYaml <- file.path(path, "template.yaml")
   skeletonPath <- file.path(path, "skeleton")
   if (!file.exists(templateYaml)) {
      templateYaml <- file.path(path, "template.yml")
      if (!file.exists(templateYaml))
         return(NULL)
   }

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
      
      # mark as knit_params_list (so other routines know we generated it)
      class(params) <- "knit_param_list"

      # inject into global environment
      assign("params", params, envir = globalenv())
   }
})

.rs.addJsonRpcHandler("convert_to_yaml", function(input)
{
   # the yaml package doesn't treat string values kindly if they're surrounded
   # by backticks, so we will need to replace those with UUIDs we can sub out
   # later
   exprs <- list()
   tick_sub <- function(x) 
   {
      lapply(x, function(val) 
      {
         if (is.list(val)) 
         {
            # if it's a list, recurse
            tick_sub(val)
         }
         else if (is.character(val) && length(val) == 1) 
         {
            # if it's a character value, check to see if it's a backtick
            # expression
            if (identical(substr(val, 1, 1), "`") &&
                identical(substr(val, nchar(val), nchar(val)), "`")) 
            {
               # replace the backtick expression with an identifier
               key <- .Call("rs_generateShortUuid")
               exprs[[key]] <<- val
               key
            } 
            else 
            {
               # leave other character expressions as-is
               val
            }
         } 
         else 
         {
            # leave non-character values alone
            val
         }
      })
   }
   
   # substitute ticks and convert to yaml
   yaml <- yaml::as.yaml(tick_sub(input))

   # the yaml package produces UTF-8 output strings, but doesn't mark them
   # as such, which leads to trouble (in particular: on Windows the string
   # may be later interpreted in system default encoding, which is not UTF-8.)
   # see: https://github.com/viking/r-yaml/issues/6
   if (Encoding(yaml) == "unknown")
      Encoding(yaml) <- "UTF-8"

   # put the backticked expressions back
   for (key in names(exprs)) 
      yaml <- sub(key, exprs[[key]], yaml, fixed = TRUE)

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
  if (Encoding(input) == "unknown")
    Encoding(input) <- "UTF-8"
  formats <- rmarkdown:::enumerate_output_formats(input, encoding = encoding)
  if (is.character(formats))
    .rs.scalar(formats[[1]])
  else
    NULL
})

.rs.addGlobalFunction("knit_with_parameters", 
                      function(file, encoding = getOption("encoding")) {
   
   # result to return via event
   result <- NULL
   
   # check for parameters 
   if (length(knitr::knit_params(readLines(file, 
                                           warn = FALSE, 
                                           encoding = encoding),
                                 evaluate = FALSE)) > 0) {
      
      # allocate temp file to hold parameter values
      paramsFile <- .Call("rs_paramsFileForRmd", file)
     
      # read any existing parameters contained therin
      params <- list()
      if (file.exists(paramsFile))
         params <- readRDS(paramsFile)
      
      # ask for parameters
      params <- rmarkdown::knit_params_ask(
         file, 
         params = params,
         shiny_args = list(
            launch.browser = function(url, ...) {
               .Call("rs_showShinyGadgetDialog",
                     "Knit with Parameters",
                     url,
                     600,
                     600)
            },
            quiet = TRUE),
         save_caption = "Knit",
         encoding = encoding
      )
      
      if (!is.null(params)) {
         saveRDS(params, file = paramsFile)
         result <- paramsFile
      }
      
   } else {
      # return special "none" value if there are no params
      result <- "none"
   }
   
   .rs.enqueClientEvent("rmd_params_ready", result)

   invisible(NULL)
})

.rs.addJsonRpcHandler("get_rmd_output_info", function(target) {
  return(.rs.getRmdOutputInfo(target))
})

.rs.addFunction("inputDirToIndexFile", function(input_dir) {
   index <- file.path(input_dir, "index.Rmd")
   if (file.exists(index))
      index
   else {
      index <- file.path(input_dir, "index.md")
      if (file.exists(index))
         index
      else
         NULL
   }
})

.rs.addFunction("getAllOutputFormats", function(input_dir, encoding) {
   index <- .rs.inputDirToIndexFile(input_dir)
   if (!is.null(index))
      rmarkdown:::enumerate_output_formats(input = index,
                                           envir = parent.frame(),
                                           encoding = encoding)
   else
      character()
})

.rs.addFunction("isBookdownWebsite", function(input_dir, encoding) {
   index <- .rs.inputDirToIndexFile(input_dir)
   if (!is.null(index)) {
      
      formats <- rmarkdown:::enumerate_output_formats(input = index,
                                                      envir = parent.frame(),
                                                      encoding = encoding)
      any(grepl("^bookdown", formats))
   }
   else
      FALSE
})


