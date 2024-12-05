
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

.rs.test("the warn option is preserved when running chunks", {
   
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
   
   remote$console.executeExpr({ options(warn = 0) })
   remote$console.executeExpr({ getOption("warn") })
   output <- remote$console.getOutput()
   expect_equal(tail(output, n = 1L), "[1] 0")
   
   id <- remote$editor.openWithContents(".qmd", contents)
   editor <- remote$editor.getInstance()
   editor$gotoLine(6)
   remote$keyboard.insertText("<Ctrl + Shift + Enter>")
   remote$console.executeExpr({ getOption("warn") })
   output <- remote$console.getOutput()
   expect_equal(tail(output, n = 1L), "[1] 2")
   
})

# https://github.com/rstudio/rstudio/issues/11745
.rs.test("the expected chunk widgets show for multiple chunks", {
   contents <- .rs.heredoc('
      ---
      title: "Chunk widgets"
      ---

      ```{r setup, include=FALSE}
      knitr::opts_chunk$set(echo = TRUE)
      ```

      ## Quarto

      This is a Quarto document.

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

   id <- remote$editor.openWithContents(".qmd", contents)

   jsChunkOptionWidgets <- remote$js.querySelectorAll(".rstudio_modify_chunk")
   jsChunkPreviewWidgets <- remote$js.querySelectorAll(".rstudio_preview_chunk")
   jsChunkRunWidgets <- remote$js.querySelectorAll(".rstudio_run_chunk")

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

})

.rs.test("can cancel switching to visual editor", {
   
   skip_on_ci()
   contents <- .rs.heredoc('
      ---
      title: "Visual Mode Denied"
      ---
      
      ## Quarto
      
      This is a Quarto document.
   ')
   
   remote$console.execute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$console.execute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$editor.openWithContents(".qmd", contents)
   
   sourceModeToggle <- remote$js.querySelectorAll(".rstudio_visual_md_off")[[1]]
   visualModeToggle <- remote$js.querySelectorAll(".rstudio_visual_md_on")[[1]]
   
   # do this twice to also check that the "switching to visual mode" dialog appears
   # the second time (i.e. that it doesn't set the state to prevent its display when
   # it is canceled)
   for (i in 1:2) {
      expect_equal(sourceModeToggle$ariaPressed, "true")
      expect_equal(visualModeToggle$ariaPressed, "false")
      
      remote$dom.clickElement(".rstudio_visual_md_on")
      .rs.waitUntil("The switching to visual mode first time dialog appears", function()
      {
         cancelBtn <- remote$js.querySelector("#rstudio_dlg_cancel")
         grepl("Cancel", cancelBtn$innerText)
      }, swallowErrors = TRUE)
      remote$dom.clickElement("#rstudio_dlg_cancel")
      expect_equal(sourceModeToggle$ariaPressed, "true")
      expect_equal(visualModeToggle$ariaPressed, "false")
   }
   
})

.rs.test("can switch to visual editor and back to source editor", {
   
   skip_on_ci()
   contents <- .rs.heredoc('
      ---
      title: "Visual Mode"
      ---
      
      ## Quarto
      
      This is a Quarto document.
   ')
   
   remote$console.execute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$console.execute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$editor.openWithContents(".qmd", contents)
   
   sourceModeToggle <- remote$js.querySelectorAll(".rstudio_visual_md_off")[[1]]
   visualModeToggle <- remote$js.querySelectorAll(".rstudio_visual_md_on")[[1]]
   
   # do this twice to check that the "switching to visual mode" dialog doesn't appear
   # the second time
   for (i in 1:2)
   {
      expect_equal(sourceModeToggle$ariaPressed, "true")
      expect_equal(visualModeToggle$ariaPressed, "false")
      
      remote$dom.clickElement(".rstudio_visual_md_on")
      
      if (i == 1)
      {
         .rs.waitUntil("The switching to visual mode first time dialog appears", function()
         {
            okBtn <- remote$js.querySelector("#rstudio_dlg_ok")
            grepl("Use Visual Mode", okBtn$innerText)
         }, swallowErrors = TRUE)
         remote$dom.clickElement("#rstudio_dlg_ok")
      }
      
      .rs.waitUntil("Visual Editor appears", function()
      {
         visualEditor <- remote$js.querySelector(".ProseMirror")
         visualEditor$contentEditable
      }, swallowErrors = TRUE)
      
      expect_equal(sourceModeToggle$ariaPressed, "false")
      expect_equal(visualModeToggle$ariaPressed, "true")
      
      # back to source mode
      remote$dom.clickElement(".rstudio_visual_md_off")
   }
   
})

.rs.test("visual editor welcome dialog displays again if don't show again is unchecked", {
   
   skip_on_ci()
   contents <- .rs.heredoc('
      ---
      title: "Visual Mode"
      ---
      
      ##  Quarto
      
      This is a Quarto document.
   ')
   
   remote$console.execute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$console.execute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$editor.openWithContents(".qmd", contents)
   
   sourceModeToggle <- remote$js.querySelectorAll(".rstudio_visual_md_off")[[1]]
   visualModeToggle <- remote$js.querySelectorAll(".rstudio_visual_md_on")[[1]]
   
   # do this twice to check that the "switching to visual mode" dialog appears second time
   for (i in 1:2)
   {
      expect_equal(sourceModeToggle$ariaPressed, "true")
      expect_equal(visualModeToggle$ariaPressed, "false")
      
      remote$dom.clickElement(".rstudio_visual_md_on")
      
      .rs.waitUntil("The switching to visual mode first time dialog appears", function()
      {
         okBtn <- remote$js.querySelector("#rstudio_dlg_ok")
         grepl("Use Visual Mode", okBtn$innerText)
      }, swallowErrors = TRUE)
      
      # uncheck "Don't show again"
      remote$dom.clickElement(".gwt-DialogBox-ModalDialog input[type=\"checkbox\"]")
      remote$dom.clickElement("#rstudio_dlg_ok")
      
      .rs.waitUntil("Visual Editor appears", function()
      {
         visualEditor <- remote$js.querySelector(".ProseMirror")
         visualEditor$contentEditable
      }, swallowErrors = TRUE)
      
      expect_equal(sourceModeToggle$ariaPressed, "false")
      expect_equal(visualModeToggle$ariaPressed, "true")
      
      # back to source mode
      remote$dom.clickElement(".rstudio_visual_md_off")
   }
   
})

.rs.test("displaying and closing chunk options popup doesn't modify already-sorted settings", {
   
   skip_on_ci()
   contents <- .rs.heredoc('
      ---
      title: "The Title"
      ---

      ```{r one, fig.height=4, fig.width=3, message=FALSE, warning=TRUE, paged.print=TRUE}
      print("one")
      ```

      ```{r}
      print("two")
      ```

      ```{r}
      #| label: one
      #| message: false
      #| warning: true
      #| fig-height: 4
      #| fig-width: 3
      #| paged-print: true
      print("one")
      ```

      The end.
   ')

   id <- remote$editor.openWithContents(".qmd", contents)
   editor <- remote$editor.getInstance()

   checkChunkOption <- function(firstLine, lastLine, widget) {
      original <- ""
      for (line in firstLine:lastLine)
      {
         lineContent <- editor$session$getLine(line)
         original <- paste(original, lineContent, sep = "\n")
      }

      remote$dom.clickElement(nodeId = widget)
      Sys.sleep(1)
      remote$keyboard.insertText("<Escape>")

      updated <- ""
      for (line in firstLine:lastLine)
      {
         lineContent <- editor$session$getLine(line)
         updated <- paste(updated, lineContent, sep = "\n")
      }
      expect_equal(original, updated)
   }

   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")
   checkChunkOption(8, 8, chunkOptionWidgetIds[[2]])

   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")
   checkChunkOption(4, 4, chunkOptionWidgetIds[[1]])

   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")
   checkChunkOption(12, 18, chunkOptionWidgetIds[[3]])

})

.rs.test("displaying and closing chunk options popup sorts the settings", {
   
   skip_on_ci()
   contents <- .rs.heredoc('
      ---
      title: "The Title"
      ---

      ```{r one, message=FALSE, fig.height=4, fig.width=3, warning=TRUE, paged.print=TRUE}
      print("one")
      ```

      ```{r}
      #| message: false
      #| warning: true
      #| fig-height: 4
      #| label: one
      #| fig-width: 3
      #| paged-print: true
      print("one")
      ```

      The end.
   ')

   id <- remote$editor.openWithContents(".qmd", contents)
   editor <- remote$editor.getInstance()

   checkChunkOption <- function(firstLine, lastLine, expectedAfter, widget)
   {
      remote$dom.clickElement(nodeId = widget)
      Sys.sleep(2)
      remote$keyboard.insertText("<Escape>")

      updated <- ""
      for (line in (firstLine):lastLine)
      {
         lineContent <- editor$session$getLine(line)
         updated <- paste(updated, lineContent, sep = ifelse(nzchar(updated), "\n", ""))
      }
      expect_equal(expectedAfter, updated)
   }

   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")
   checkChunkOption(
      4, 4,
      '```{r one, fig.height=4, fig.width=3, message=FALSE, warning=TRUE, paged.print=TRUE}',
      chunkOptionWidgetIds[[1]])

   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")
   checkChunkOption(
      9, 14,
      '#| label: one\n#| message: false\n#| warning: true\n#| fig-height: 4\n#| fig-width: 3\n#| paged-print: true',
      chunkOptionWidgetIds[[1]])

})


# https://github.com/rstudio/rstudio/issues/14552
.rs.test("empty header labels are permitted in document outline", {
   
   skip_on_ci()
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
   
   remote$editor.executeWithContents(".qmd", contents, function(editor) {
      
      token <- as.vector(editor$session$getTokenAt(10))
      expect_equal(token$type, "markup.heading.2")
      
      remote$commands.execute("toggleDocumentOutline")
      docOutline <- remote$js.querySelector(".rstudio_doc_outline_container")
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
.rs.test("variable-width nested chunks can be folded", {
   
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

   remote$editor.executeWithContents(".qmd", contents, function(editor) {
      
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
.rs.test("raw html blocks are preserved by visual editor", {
   
   skip_on_ci()
   contents <- .rs.heredoc('
      ---
      title: Raw html blocks
      ---
      ```{=html}
      Hello <i>World</i>, how are you?
      ```
   ')
   
   remote$console.execute(".rs.writeUserState(\"visual_mode_confirmed\", TRUE)")
   remote$console.execute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   remote$editor.executeWithContents(".qmd", contents, function(editor) {
      
      sourceModeToggle <- remote$js.querySelectorAll(".rstudio_visual_md_off")[[1]]
      visualModeToggle <- remote$js.querySelectorAll(".rstudio_visual_md_on")[[1]]
      expect_equal(sourceModeToggle$ariaPressed, "true")
      
      # verify starting state
      editor <- remote$editor.getInstance()
      expect_equal(editor$session$getLine(3), "```{=html}")
      
      # switch to visual mode
      remote$dom.clickElement(".rstudio_visual_md_on")
      .rs.waitUntil("Visual Editor appears", function()
      {
         visualEditor <- remote$js.querySelector(".ProseMirror")
         visualEditor$contentEditable
      }, swallowErrors = TRUE)
      
      # back to source mode
      remote$dom.clickElement(".rstudio_visual_md_off")
      .rs.waitUntil("Source editor appears", function()
      {
         sourceEditor <- remote$js.querySelector("#rstudio_source_text_editor")
         sourceEditor$checkVisibility()
      }, swallowErrors = TRUE)
      
      # verify ending state
      editor <- remote$editor.getInstance()
      expect_equal(editor$session$getLine(4), "```{=html}")
   })
})

# https://github.com/rstudio/rstudio/issues/15253
.rs.test("raw latex blocks are preserved by visual editor", {
   
   skip_on_ci()
   contents <- .rs.heredoc('
      ---
      title: Raw LaTeX blocks
      ---
      
      ```{=latex}
      \\LaTeX
      ```
   ')
   
   remote$console.execute(".rs.writeUserState(\"visual_mode_confirmed\", TRUE)")
   remote$console.execute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   remote$editor.executeWithContents(".qmd", contents, function(editor) {
      
      sourceModeToggle <- remote$js.querySelectorAll(".rstudio_visual_md_off")[[1]]
      visualModeToggle <- remote$js.querySelectorAll(".rstudio_visual_md_on")[[1]]
      expect_equal(sourceModeToggle$ariaPressed, "true")
      
      # verify starting state
      editor <- remote$editor.getInstance()
      expect_equal(editor$session$getLine(4), "```{=latex}")
      
      # switch to visual mode
      remote$dom.clickElement(".rstudio_visual_md_on")
      .rs.waitUntil("Visual Editor appears", function()
      {
         visualEditor <- remote$js.querySelector(".ProseMirror")
         visualEditor$contentEditable
      }, swallowErrors = TRUE)
      
      # back to source mode
      remote$dom.clickElement(".rstudio_visual_md_off")
      .rs.waitUntil("Source editor appears", function()
      {
         sourceEditor <- remote$js.querySelector("#rstudio_source_text_editor")
         sourceEditor$checkVisibility()
      }, swallowErrors = TRUE)
      
      # verify ending state
      editor <- remote$editor.getInstance()
      expect_equal(editor$session$getLine(4), "```{=latex}")
   })
})
