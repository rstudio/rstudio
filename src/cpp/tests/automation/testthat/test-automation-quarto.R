
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
   
   id <- remote$documentOpen(".qmd", contents)
   editor <- remote$editorGetInstance()
   editor$gotoLine(6)
   remote$keyboardExecute("<Ctrl + Shift + Enter>")
   remote$consoleExecuteExpr({ getOption("warn") })
   output <- remote$consoleOutput()
   expect_equal(tail(output, n = 1L), "[1] 2")
   
   remote$documentClose()
   remote$keyboardExecute("<Ctrl + L>")
   
})

# https://github.com/rstudio/rstudio/issues/11745
# TODO: currently failing due to above issue
# test_that("the expected chunk widgets show for multiple chunks", {

#    contents <- .rs.heredoc('
#       ---
#       title: "Chunk widgets"
#       ---
      
#       ```{r setup, include=FALSE}
#       knitr::opts_chunk$set(echo = TRUE)
#       ```
      
#       ## Quarto
      
#       This is a Quarto document.
      
#       ```{r cars}
#       summary(cars)
#       ```
      
#       ## Including Plots
      
#       You can also embed plots, for example:
      
#       ```{r pressure, echo=FALSE}
#       plot(pressure)
#       ```
      
#       The end.
#    ')
   
#    id <- remote$documentOpen(".qmd", contents)
   
#    jsChunkOptionWidgets <- remote$jsObjectsViaSelector(".rstudio_modify_chunk")
#    jsChunkPreviewWidgets <- remote$jsObjectsViaSelector(".rstudio_preview_chunk")
#    jsChunkRunWidgets <- remote$jsObjectsViaSelector(".rstudio_run_chunk")
   
#    expect_equal(length(jsChunkOptionWidgets), 3)
#    expect_equal(length(jsChunkPreviewWidgets), 3)
#    expect_equal(length(jsChunkRunWidgets), 3)
   
#    # setup chunk's "preview" widget should be aria-hidden and display:none
#    expect_true(.rs.automation.tools.isAriaHidden(jsChunkPreviewWidgets[[1]]))
#    expect_equal(jsChunkPreviewWidgets[[1]]$style$display, "none")
   
#    # all others should not be hidden
#    checkWidgetVisible <- function(widget) {
#       expect_false(.rs.automation.tools.isAriaHidden(widget))
#       expect_false(widget$style$display == "none")
#    }
#    lapply(jsChunkPreviewWidgets[2:3], checkWidgetVisible)
#    lapply(jsChunkOptionWidgets, checkWidgetVisible)
#    lapply(jsChunkRunWidgets, checkWidgetVisible)
   
#    remote$documentClose()
#    remote$keyboardExecute("<Ctrl + L>")
# })

test_that("can cancel switching to visual editor", {
   contents <- .rs.heredoc('
      ---
      title: "Visual Mode Denied"
      ---
      
      ## Quarto
      
      This is a Quarto document.
   ')
   
   remote$consoleExecute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$consoleExecute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$documentOpen(".qmd", contents)
   
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
      expect_equal(sourceModeToggle$ariaPressed, "true")
      expect_equal(visualModeToggle$ariaPressed, "false")
   }
   remote$documentClose()
   remote$keyboardExecute("<Ctrl + L>")
})

test_that("can switch to visual editor and back to source editor", {
   contents <- .rs.heredoc('
      ---
      title: "Visual Mode"
      ---
      
      ## Quarto
      
      This is a Quarto document.
   ')
   
   remote$consoleExecute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$consoleExecute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$documentOpen(".qmd", contents)
   
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
      }
      
      .rs.waitUntil("Visual Editor appears", function() {
         tryCatch({
            visualEditor <- remote$jsObjectViaSelector(".ProseMirror")
            visualEditor$contentEditable
         }, error = function(e) FALSE)
      })
      
      expect_equal(sourceModeToggle$ariaPressed, "false")
      expect_equal(visualModeToggle$ariaPressed, "true")
      
      # back to source mode
      remote$domClickElement(".rstudio_visual_md_off")
   }
   
   remote$documentClose()
   remote$keyboardExecute("<Ctrl + L>")
})

test_that("visual editor welcome dialog displays again if don't show again is unchecked", {
   contents <- .rs.heredoc('
      ---
      title: "Visual Mode"
      ---
      
      ##  Quarto
      
      This is a Quarto document.
   ')
   
   remote$consoleExecute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$consoleExecute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$documentOpen(".qmd", contents)
   
   sourceModeToggle <- remote$jsObjectsViaSelector(".rstudio_visual_md_off")[[1]]
   visualModeToggle <- remote$jsObjectsViaSelector(".rstudio_visual_md_on")[[1]]
   
   # do this twice to check that the "switching to visual mode" dialog appears second time
   for (i in 1:2) {
      expect_equal(sourceModeToggle$ariaPressed, "true")
      expect_equal(visualModeToggle$ariaPressed, "false")
      
      remote$domClickElement(".rstudio_visual_md_on")
      
      .rs.waitUntil("The switching to visual mode first time dialog appears", function() {
         tryCatch({
            okBtn <- remote$jsObjectViaSelector("#rstudio_dlg_ok")
            grepl("Use Visual Mode", okBtn$innerText)
         }, error = function(e) FALSE)
      })
      
      # uncheck "Don't show again"
      remote$domClickElement(".gwt-DialogBox-ModalDialog input[type=\"checkbox\"]")
      remote$domClickElement("#rstudio_dlg_ok")
      
      .rs.waitUntil("Visual Editor appears", function() {
         tryCatch({
           visualEditor <- remote$jsObjectViaSelector(".ProseMirror")
           visualEditor$contentEditable
         }, error = function(e) FALSE)
      })
      
      expect_equal(sourceModeToggle$ariaPressed, "false")
      expect_equal(visualModeToggle$ariaPressed, "true")
      
      # back to source mode
      remote$domClickElement(".rstudio_visual_md_off")
   }
   
   remote$documentClose()
   remote$keyboardExecute("<Ctrl + L>")
})

# https://github.com/rstudio/rstudio/issues/14552
test_that("empty header labels are permitted in document outline", {
   
   contents <- .rs.heredoc('
      ---
      format: revealjs
      ---
      
      # Section with title
      
      ## Slide with title
      
      Some content.
      
      ##
      
      Slide without title
      
      # Another section with title
      
      ## Another slide with title
   ')
   
   remote$documentExecute(".qmd", contents, function(editor) {
      
      token <- as.vector(editor$session$getTokenAt(10))
      expect_equal(token$type, "markup.heading.2")
      
      remote$commandExecute("toggleDocumentOutline")
      docOutline <- remote$jsObjectViaSelector(".rstudio_doc_outline_container")
      contents <- docOutline$innerText
      
      # Remove non-breaking spaces
      contents <- gsub("\u00a0", "", contents)
      
      # Split on newlines
      contents <- strsplit(contents, "\\n+", perl = TRUE)[[1]]
      expect_equal(contents, c(
         "Section with title",
         "Slide with title",
         "(Untitled)",
         "Another section with title",
         "Another slide with title"
      ))

   })

})

# https://github.com/rstudio/rstudio/issues/15191
test_that("variable-width nested chunks can be folded", {
   
   contents <- .rs.heredoc('
      ---
      title: Folding
      ---
      
      `````{verbatim}
      
      This is some text.
      
      ```{r nested}
      print(1 + 1)
      ```
      
      `````
      
      # Header

   ')

   remote$documentExecute(".qmd", contents, function(editor) {
      
      # Check the fold widget strings.
      session <- editor$session
      expect_equal(session$getFoldWidget(4), "start")
      expect_equal(session$getFoldWidget(8), "")
      expect_equal(session$getFoldWidget(10), "")
      expect_equal(session$getFoldWidget(12), "end")
      
      # Check the computed ranges for the folds.
      expected <- list(
         start = list(row = 4, column = 15),
         end = list(row = 12, column = 0)
      )
      
      range <- as.vector(session$getFoldWidgetRange(4))
      expect_equal(range, expected)
      
      range <- as.vector(session$getFoldWidgetRange(12))
      expect_equal(range, expected)
      
   })
   
})

# https://github.com/rstudio/rstudio/issues/15189
test_that("raw html blocks are preserved by visual editor", {
   
   contents <- .rs.heredoc('
      ---
      title: Raw html blocks
      ---
      ```{=html}
      Hello <i>World</i>, how are you?
      ```
   ')
   
   remote$consoleExecute(".rs.writeUserState(\"visual_mode_confirmed\", TRUE)")
   remote$consoleExecute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   remote$documentExecute(".qmd", contents, function(editor) {
      
      sourceModeToggle <- remote$jsObjectsViaSelector(".rstudio_visual_md_off")[[1]]
      visualModeToggle <- remote$jsObjectsViaSelector(".rstudio_visual_md_on")[[1]]
      expect_equal(sourceModeToggle$ariaPressed, "true")
      
      # verify starting state
      editor <- remote$editorGetInstance()
      expect_equal(editor$session$getLine(3), "```{=html}")
      
      # switch to visual mode
      remote$domClickElement(".rstudio_visual_md_on")
      .rs.waitUntil("Visual Editor appears", function() {
         tryCatch({
            visualEditor <- remote$jsObjectViaSelector(".ProseMirror")
            visualEditor$contentEditable
         }, error = function(e) FALSE)
      })
      
      # back to source mode
      remote$domClickElement(".rstudio_visual_md_off")
      .rs.waitUntil("Source editor appears", function() {
         tryCatch({
            sourceEditor <- remote$jsObjectViaSelector("#rstudio_source_text_editor")
            sourceEditor$checkVisibility()
         }, error = function(e) FALSE)
      })
      
      # verify ending state
      editor <- remote$editorGetInstance()
      expect_equal(editor$session$getLine(4), "```{=html}")
   })
})

# https://github.com/rstudio/rstudio/issues/15253
test_that("raw latex blocks are preserved by visual editor", {
   
   contents <- .rs.heredoc('
      ---
      title: Raw LaTeX blocks
      ---
      
      ```{=latex}
      \\LaTeX
      ```
   ')
   
   remote$consoleExecute(".rs.writeUserState(\"visual_mode_confirmed\", TRUE)")
   remote$consoleExecute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   remote$documentExecute(".qmd", contents, function(editor) {
      
      sourceModeToggle <- remote$jsObjectsViaSelector(".rstudio_visual_md_off")[[1]]
      visualModeToggle <- remote$jsObjectsViaSelector(".rstudio_visual_md_on")[[1]]
      expect_equal(sourceModeToggle$ariaPressed, "true")
      
      # verify starting state
      editor <- remote$editorGetInstance()
      expect_equal(editor$session$getLine(4), "```{=latex}")
      
      # switch to visual mode
      remote$domClickElement(".rstudio_visual_md_on")
      .rs.waitUntil("Visual Editor appears", function() {
         tryCatch({
            visualEditor <- remote$jsObjectViaSelector(".ProseMirror")
            visualEditor$contentEditable
         }, error = function(e) FALSE)
      })
      
      # back to source mode
      remote$domClickElement(".rstudio_visual_md_off")
      .rs.waitUntil("Source editor appears", function() {
         tryCatch({
            sourceEditor <- remote$jsObjectViaSelector("#rstudio_source_text_editor")
            sourceEditor$checkVisibility()
         }, error = function(e) FALSE)
      })
      
      # verify ending state
      editor <- remote$editorGetInstance()
      expect_equal(editor$session$getLine(4), "```{=latex}")
   })
})
