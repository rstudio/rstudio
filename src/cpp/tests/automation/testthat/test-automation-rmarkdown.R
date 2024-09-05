
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

test_that("the warn option is preserved when running chunks", {
   
   contents <- .rs.heredoc('
      ---
      title: Chunk Warnings
      ---
      
      ```{r warning=TRUE}
      # check current option
      getOption("warn")
      # setting a global option
      options(warn = 2)
      ```
   ')
   
   remote$consoleExecuteExpr({ options(warn = 0) })
   remote$consoleExecuteExpr({ getOption("warn") })
   output <- remote$consoleOutput()
   expect_equal(tail(output, n = 1L), "[1] 0")
   
   id <- remote$documentOpen(".Rmd", contents)
   editor <- remote$editorGetInstance()
   editor$gotoLine(6)
   remote$keyboardExecute("<Ctrl + Shift + Enter>")
   remote$consoleExecuteExpr({ getOption("warn") })
   output <- remote$consoleOutput()
   expect_equal(tail(output, n = 1L), "[1] 2")
   
   remote$documentClose()
   remote$keyboardExecute("<Ctrl + L>")
   
})

test_that("the expected chunk widgets show for multiple chunks", {

   contents <- .rs.heredoc('
      ---
      title: "Chunk widgets"
      ---
      
      ```{r setup, include=FALSE}
      knitr::opts_chunk$set(echo = TRUE)
      ```
      
      ## R Markdown
      
      This is an R Markdown document.
      
      ```{r cars}
      summary(cars)
      ```
      
      ## Including Plots
      
      You can also embed plots, for example:
      
      ```{r pressure, echo=FALSE}
      plot(pressure)
      ```
      
      The end.
   ')
   
   id <- remote$documentOpen(".Rmd", contents)

   jsChunkOptionWidgets <- remote$jsObjectsViaSelector(".rstudio_modify_chunk")
   jsChunkPreviewWidgets <- remote$jsObjectsViaSelector(".rstudio_preview_chunk")
   jsChunkRunWidgets <- remote$jsObjectsViaSelector(".rstudio_run_chunk")
   
   expect_equal(length(jsChunkOptionWidgets), 3)
   expect_equal(length(jsChunkPreviewWidgets), 3)
   expect_equal(length(jsChunkRunWidgets), 3)
   
   # setup chunk's "preview" widget should be aria-hidden and display:none
   expect_true(.rs.automation.tools.isAriaHidden(jsChunkPreviewWidgets[[1]]))
   expect_equal(jsChunkPreviewWidgets[[1]]$style$display, "none")
   
   # all others should not be hidden
   checkWidgetVisible <- function(widget) {
      expect_false(.rs.automation.tools.isAriaHidden(widget))
      expect_false(widget$style$display == "none")
   }
   lapply(jsChunkPreviewWidgets[2:3], checkWidgetVisible)
   lapply(jsChunkOptionWidgets, checkWidgetVisible)
   lapply(jsChunkRunWidgets, checkWidgetVisible)
   
   remote$documentClose()
   remote$keyboardExecute("<Ctrl + L>")
})

test_that("can cancel switching to visual editor", {
   contents <- .rs.heredoc('
      ---
      title: "Visual Mode Denied"
      ---
      
      ## R Markdown
      
      This is an R Markdown document.
   ')
   
   remote$consoleExecute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$consoleExecute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$documentOpen(".Rmd", contents)
   
   sourceModeToggle <- remote$jsObjectsViaSelector(".rstudio_visual_md_off")[[1]]
   visualModeToggle <- remote$jsObjectsViaSelector(".rstudio_visual_md_on")[[1]]
   
   # do this twice to also check that the "switching to visual mode" dialog appears
   # the second time (i.e. that it doesn't set the state to prevent its display when
   # it is canceled)
   for (i in 1:2) {
      expect_equal(sourceModeToggle$ariaPressed, "true")
      expect_equal(visualModeToggle$ariaPressed, "false")
   
      remote$domClickElement(".rstudio_visual_md_on")
      .rs.waitUntil("The switching to visual mode first time dialog appears", function() {
         tryCatch({
            cancelBtn <- remote$jsObjectViaSelector("#rstudio_dlg_cancel")
            grepl("Cancel", cancelBtn$innerText)
         }, error = function(e) FALSE)
      })
      remote$domClickElement("#rstudio_dlg_cancel")
      Sys.sleep(1)
      expect_equal(sourceModeToggle$ariaPressed, "true")
      expect_equal(visualModeToggle$ariaPressed, "false")
   }
   remote$documentClose()
   remote$keyboardExecute("<Ctrl + L>")
})

test_that("can switch to visual editor", {
   contents <- .rs.heredoc('
      ---
      title: "Visual Mode"
      ---
      
      ## R Markdown
      
      This is an R Markdown document.
   ')
   
   remote$consoleExecute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$consoleExecute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$documentOpen(".Rmd", contents)
   
   sourceModeToggle <- remote$jsObjectsViaSelector(".rstudio_visual_md_off")[[1]]
   visualModeToggle <- remote$jsObjectsViaSelector(".rstudio_visual_md_on")[[1]]
   
   # do this twice to check that the "switching to visual mode" dialog doesn't appear
   # the second time
   for (i in 1:2) {
      expect_equal(sourceModeToggle$ariaPressed, "true")
      expect_equal(visualModeToggle$ariaPressed, "false")
   
      remote$domClickElement(".rstudio_visual_md_on")
   
      if (i == 1)
      {
        .rs.waitUntil("The switching to visual mode first time dialog appears", function() {
           tryCatch({
              okBtn <- remote$jsObjectViaSelector("#rstudio_dlg_ok")
              grepl("Use Visual Mode", okBtn$innerText)
            }, error = function(e) FALSE)
         })
         remote$domClickElement("#rstudio_dlg_ok")
         Sys.sleep(1)
      }
      else
      {
        .rs.waitUntil("Visual Editor appears", function() {
           tryCatch({
              visualEditor <- remote$jsObjectViaSelector(".ProseMirror")
              visualEditor$contentEditable
            }, error = function(e) FALSE)
         })
      }

      expect_equal(sourceModeToggle$ariaPressed, "false")
      expect_equal(visualModeToggle$ariaPressed, "true")

      # back to source mode
      remote$domClickElement(".rstudio_visual_md_off")
   }
   
   remote$documentClose()
   remote$keyboardExecute("<Ctrl + L>")
})
