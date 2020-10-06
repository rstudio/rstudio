#
# SessionRMarkdown.R
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

.rs.setVar("markdown.acCompletionTypes", list(
   COMPLETION_HREF = 1
))

.rs.addJsonRpcHandler("markdown_get_completions", function(type, data)
{
   if (type == .rs.markdown.acCompletionTypes$COMPLETION_HREF)
      return(.rs.markdown.getCompletionsHref(data))
})

.rs.addFunction("markdown.resolveCompletionRoot", function(path)
{
   # figure out working directory
   props <- .rs.getSourceDocumentProperties(path)
   workingDirProp <- props$properties$working_dir
   
   useProject <-
      identical(workingDirProp, "project") &&
      is.character(props$path) &&
      is.character(props$project_path)
   
   if (useProject)
   {
      # path refers to the full path; project_path refers
      # to the project-relative path. use that to infer
      # the path to the project hosting the document
      # (just in case the user is editing a document that
      # belongs to an alternate project)
      substring(props$path, 1L, nchar(props$path) - nchar(props$project_path) - 1L)
   }
   else if (identical(workingDirProp, "current"))
   {
      getwd()
   }
   else
   {
      dirname(path)
   }
})

.rs.addFunction("markdown.getCompletionsHref", function(data)
{
   # extract parameters
   token <- data$token
   path <- data$path
   
   # if we don't have a path, bail
   if (is.null(path))
      return(.rs.emptyCompletions())
   
   # figure out working directory
   workingDir <- .rs.markdown.resolveCompletionRoot(path)
   
   # determine dirname, basename (need to handle trailing slashes properly
   # so can't just use dirname / basename)
   slashes <- gregexpr("[/\\]", token)[[1]]
   idx <- tail(slashes, n = 1)
   lhs <- substring(token, 1, idx - 1)
   rhs <- substring(token, idx + 1)
   
   # check to see if user is providing absolute path, and construct
   # completion directory appropriately
   isAbsolute <- grepl("^(?:[A-Z]:|/|\\\\|~)", token, perl = TRUE)
   if (!isAbsolute)
      lhs <- file.path(workingDir, lhs)
   
   # retrieve completions
   completions <- .rs.getCompletionsFile(
      token = rhs,
      path = lhs,
      quote = FALSE,
      directoriesOnly = FALSE
   )
   
   return(completions)
   
})

.rs.addFunction("scalarListFromList", function(l, expressions = FALSE)
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
      else if (identical(expressions, TRUE) && (is.expression(ele) || is.call(ele)))
         .rs.scalarListFromList(list(expr = eval(ele)))$expr
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
  # read the contents of the file
  lines <- readLines(file, warn = FALSE)
  
  # mark the encoding if it's available
  properties <- .rs.getSourceDocumentProperties(file)
  if (identical(properties$encoding, "UTF-8"))
    Encoding(lines) <- "UTF-8"
  
  yamlFrontMatter <- tryCatch(
    rmarkdown:::parse_yaml_front_matter(lines),
    error = function(e) {
      list()
    }
  )

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
  
   # read the contents of the file
   lines <- readLines(target, warn = FALSE)
   
   # mark the encoding if it's available
   properties <- .rs.getSourceDocumentProperties(target)
   if (identical(properties$encoding, "UTF-8"))
      Encoding(lines) <- "UTF-8"
   
   # compute the name of the output file
   outputFormat <- rmarkdown:::output_format_from_yaml_front_matter(lines)
   outputFormat <- rmarkdown:::create_output_format(outputFormat$name, outputFormat$options)
   outputFile <- rmarkdown:::pandoc_output_file(target, outputFormat$pandoc)

   # determine location of output file, accounting for possibility of a website project which
   # puts the output in a different location than the source file
   outputDir <- .Call("rs_getWebsiteOutputDir")
   if (is.null(outputDir))
      outputDir <- dirname(target)
   outputPath <- file.path(outputDir, outputFile)
   
   # ensure output file exists
   fileExists <- file.exists(outputPath)
   current <- fileExists && 
      file.info(outputPath)$mtime >= file.info(target)$mtime
   
   list(
      output_file = .rs.scalar(outputPath),
      is_current  = .rs.scalar(current),
      output_file_exists = .rs.scalar(fileExists)
   )
})

.rs.addFunction("getTemplateDetails", function(templateYaml) {
   yaml::yaml.load_file(templateYaml)
})

# given a path to a folder on disk, return information about the R Markdown
# template in that folder.
.rs.addFunction("getTemplateYamlFile", function(path) {
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

   # will need to enforce create_dir if there are multiple files in /skeleton/
   multiFile = length(list.files(skeletonPath)) > 1 

   # return metadata; we won't parse until the client requests template files
   list(
      template_yaml = .rs.scalar(templateYaml),
      multi_file    = .rs.scalar(multiFile)
   )
})


.rs.addFunction("evaluateRmdParams", function(contents) {

   Encoding(contents) <- "UTF-8"

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
            needsPlaceholder <- (function() {
               
               # if it's a character value, check to see if it's a backtick
               # expression
               if (identical(substr(val, 1, 1), "`") &&
                   identical(substr(val, nchar(val), nchar(val)), "`"))
               {
                  return(TRUE)
               }
               
               # if it's a tagged value, placeholder
               if (grepl("^[!]", val))
                  return(TRUE)
               
               FALSE
            })()
            
            if (needsPlaceholder)
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
   Encoding(yaml) <- "UTF-8"

   data <- list()
   parseError <- ""
   parseSucceeded <- FALSE
   
   tryCatch(
   {
      handlers <- list(r = function(x) paste("!r", x))
      data <- .rs.scalarListFromList(yaml::yaml.load(yaml, handlers = handlers))
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

.rs.addFunction("isBookdownDir", function(input_dir, encoding) {
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

.rs.addFunction("bookdown.SourceFiles", function(input_dir) {
   wd <- getwd()
   on.exit(setwd(wd), add = TRUE)
   setwd(input_dir)
   bookdown:::source_files()
})


.rs.addFunction("bookdown.frontMatterValue", function(input_dir, value) {
   wd <- getwd()
   on.exit(setwd(wd), add = TRUE)
   setwd(input_dir)
   files <- bookdown:::source_files()
   if (length(files) > 0)
   {
      index <- files[[1]]
      front_matter <- rmarkdown::yaml_front_matter(index)
      if (is.character(front_matter[[value]]))
         front_matter[[value]]
      else if (is.logical(front_matter[[value]]))
         paste0("LOGICAL:",front_matter[[value]])
      else
         character()
   }
   else
   {
      character()
   }
})

.rs.addFunction("isSiteProject", function(input_dir, encoding, site) {
   
   index <- .rs.inputDirToIndexFile(input_dir)
   if (!is.null(index)) {
      any(grepl(site, readLines(index, encoding = encoding)))
   }
   else
      FALSE
})

.rs.addFunction("tinytexRoot", function()
{
   sysname <- Sys.info()[["sysname"]]
   if (sysname == "Windows")
      file.path(Sys.getenv("APPDATA"), "TinyTeX")
   else if (sysname == "Darwin")
      "~/Library/TinyTeX"
   else
      "~/.TinyTeX"
})

.rs.addFunction("tinytexBin", function()
{
   root <- tryCatch(
      tinytex:::tinytex_root(),
      error = function(e) .rs.tinytexRoot()
   )
   
   if (!file.exists(root))
      return(NULL)
   
   # NOTE: binary directory has a single arch-specific subdir;
   # rather than trying to hard-code the architecture we just
   # infer it directly.
   #
   # some users will end up with tinytex installations that
   # 'exist', but are broken for some reason (no longer have
   # a 'bin' directory). detect those cases properly
   #
   # https://github.com/rstudio/rstudio/issues/7615
   bin <- file.path(root, "bin")
   if (!file.exists(bin))
      return(NULL)

   subbin <- list.files(bin, full.names = TRUE)
   if (length(subbin) == 0)
      return(NULL)

   normalizePath(subbin[[1]], mustWork = TRUE)
})

.rs.addFunction("bookdown.renderedOutputPath", function(websiteDir, outputPath)
{
   # set encoding
   Encoding(websiteDir) <- "UTF-8"
   Encoding(outputPath) <- "UTF-8"
   
   # if we have a PDF for this file, use it
   if (tools::file_ext(outputPath) == "pdf")
      return(outputPath)
   
   # if that fails, use root index file
   # note that this gets remapped as appropriate to knitted posts; see:
   # https://github.com/rstudio/rstudio/issues/6945
   index <- file.path(websiteDir, "index.html")
   if (file.exists(index))
      return(index)
   
   # default to using output file path
   # (necessary for self-contained books, which may not have an index)
   outputPath
})
