
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
   
   id <- remote$editor.openWithContents(".Rmd", contents)
   editor <- remote$editor.getInstance()
   editor$gotoLine(6)
   remote$keyboard.insertText("<Ctrl + Shift + Enter>")
   remote$console.executeExpr({ getOption("warn") })
   output <- remote$console.getOutput()
   expect_equal(tail(output, n = 1L), "[1] 2")
   
   
})

.rs.test("the expected chunk widgets show for multiple chunks", {

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
   
   id <- remote$editor.openWithContents(".Rmd", contents)
   
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
      
      ## R Markdown
      
      This is an R Markdown document.
   ')
   
   remote$console.execute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$console.execute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$editor.openWithContents(".Rmd", contents)
   
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
      
      ## R Markdown
      
      This is an R Markdown document.
   ')
   
   remote$console.execute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$console.execute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$editor.openWithContents(".Rmd", contents)
   
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
      
      ## R Markdown
      
      This is an R Markdown document.
   ')
   
   remote$console.execute(".rs.writeUserState(\"visual_mode_confirmed\", FALSE)")
   remote$console.execute(".rs.writeUserPref(\"visual_markdown_editing_is_default\", FALSE)")
   
   id <- remote$editor.openWithContents(".Rmd", contents)
   
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

.rs.test("displaying and closing chunk options popup doesn't modify settings", {
   
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
       
      The end.
   ')
   
   id <- remote$editor.openWithContents(".Rmd", contents)
   editor <- remote$editor.getInstance()
   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")
   
   checkChunkOption <- function(line, expected, widget)
   {
      original <- editor$session$getLine(line)
      expect_equal(original, expected)
      remote$dom.clickElement(nodeId = widget)
      Sys.sleep(1)
      remote$keyboard.insertText("<Escape>")
      updated <- editor$session$getLine(line)
      expect_equal(original, updated)
   }
   
   checkChunkOption(
      8,
      "```{r}",
      chunkOptionWidgetIds[[2]])

   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")

   checkChunkOption(
      4,
      "```{r one, fig.height=4, fig.width=3, message=FALSE, warning=TRUE, paged.print=TRUE}",
      chunkOptionWidgetIds[[1]])
   
})

.rs.test("displaying chunk options popup and applying without making changes doesn't modify settings", {
 
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
      
      ```{r fig.cap = "a caption"}
      ```
      
      The end.
   ')
   
   id <- remote$editor.openWithContents(".Rmd", contents)
   editor <- remote$editor.getInstance()
   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")
   
   checkChunkOption <- function(line, expectedAfter, widget)
   {
      original <- editor$session$getLine(line)
      remote$dom.clickElement(nodeId = widget)
      Sys.sleep(1)
      remote$dom.clickElement("#rstudio_chunk_opt_apply")
      updated <- editor$session$getLine(line)
      expect_equal(expectedAfter, updated)
   }

   checkChunkOption(8, "```{r}", chunkOptionWidgetIds[[2]])

   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")

   checkChunkOption(
      4,
      "```{r one, fig.height=4, fig.width=3, message=FALSE, warning=TRUE, paged.print=TRUE}",
      chunkOptionWidgetIds[[1]]
   )

   # https://github.com/rstudio/rstudio/issues/6829
   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")
   checkChunkOption(12, "```{r fig.cap=\"a caption\"}", chunkOptionWidgetIds[[3]])
   
})


.rs.test("reverting chunk option changes restores original options ", {
   
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
       
      The end.
   ')
   
   id <- remote$editor.openWithContents(".Rmd", contents)
   editor <- remote$editor.getInstance()
   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")
   
   checkChunkOption <- function(line, nodeId)
   {
      original <- editor$session$getLine(line)
      remote$dom.clickElement(nodeId = nodeId)
      Sys.sleep(1)
      
      remote$dom.clickElement("#rstudio_chunk_opt_warnings")
      remote$dom.clickElement("#rstudio_chunk_opt_messages")
      remote$dom.clickElement("#rstudio_chunk_opt_warnings")
      remote$dom.clickElement("#rstudio_chunk_opt_messages")
      remote$js.querySelector("#rstudio_chunk_opt_name")$focus()
      remote$keyboard.insertText("abcdefg hijklmnop 12345")
      remote$dom.clickElement("#rstudio_chunk_opt_tables")
      remote$dom.clickElement("#rstudio_chunk_opt_figuresize")
      remote$dom.clickElement("#rstudio_chunk_opt_revert")
      updated <- editor$session$getLine(line)
      expect_equal(original, updated)
   }
   
   checkChunkOption(8, chunkOptionWidgetIds[[2]])
   chunkOptionWidgetIds <- remote$dom.querySelectorAll(".rstudio_modify_chunk")
   checkChunkOption(4, chunkOptionWidgetIds[[1]])

})

# https://github.com/rstudio/rstudio/issues/6829
.rs.test("modifying chunk options via UI doesn't mess up other options", {
   
   skip_on_ci()
   contents <- .rs.heredoc('
      ---
      title: "Issue 6829"
      ---
      
      ```{r fig.cap = "a caption"}
      print("Hello")
      ```
   
      The end.
   ')
   
   id <- remote$editor.openWithContents(".Rmd", contents)
   editor <- remote$editor.getInstance()
   
   remote$dom.clickElement(".rstudio_modify_chunk")
   Sys.sleep(1)
   remote$dom.clickElement("#rstudio_chunk_opt_warnings")
   remote$dom.clickElement("#rstudio_chunk_opt_messages")
   remote$keyboard.insertText("<Escape>")
   expect_equal('```{r fig.cap="a caption", message=TRUE, warning=TRUE}', editor$session$getLine(4))

})

.rs.test("setup chunk starting with no options works with chunk options UI", {
   
   skip_on_ci()
   contents <- .rs.heredoc('
      ---
      title: "Chunk widgets"
      ---

      ```{r setup, include=FALSE}
      ```

      This is an R Markdown document.
   ')
   id <- remote$editor.openWithContents(".Rmd", contents)
   editor <- remote$editor.getInstance()
   
   remote$dom.clickElement(".rstudio_modify_chunk")
   Sys.sleep(1)
   remote$dom.clickElement("#rstudio_chunk_opt_warnings")
   remote$dom.clickElement("#rstudio_chunk_opt_messages")
   remote$keyboard.insertText("<Escape>")
   expect_equal('```{r setup, include=FALSE}', editor$session$getLine(4))
   expect_equal('knitr::opts_chunk$set(warning = TRUE, message = TRUE)', editor$session$getLine(5))
   remote$dom.clickElement(".rstudio_modify_chunk")
   Sys.sleep(1)
   remote$dom.clickElement("#rstudio_chunk_opt_warnings")
   remote$dom.clickElement("#rstudio_chunk_opt_messages")
   remote$keyboard.insertText("<Escape>")
   expect_equal('```{r setup, include=FALSE}', editor$session$getLine(4))
   expect_equal('knitr::opts_chunk$set(warning = FALSE, message = FALSE)', editor$session$getLine(5))
   remote$dom.clickElement(".rstudio_modify_chunk")
   Sys.sleep(1)
   remote$dom.clickElement("#rstudio_chunk_opt_warnings")
   remote$dom.clickElement("#rstudio_chunk_opt_messages")
   remote$keyboard.insertText("<Escape>")
   expect_equal('```{r setup, include=FALSE}', editor$session$getLine(4))
   expect_equal('```', editor$session$getLine(5))

})

.rs.test("setup chunk with three options displays on multiple lines", {
   
   skip_on_ci()
   contents <- .rs.heredoc('
      ---
      title: "Chunk widgets"
      ---
   
      ```{r setup, include=FALSE}
      knitr::opts_chunk$set(eval = FALSE, include = FALSE)
      ```

      This is an R Markdown document.
   ')
   id <- remote$editor.openWithContents(".Rmd", contents)
   editor <- remote$editor.getInstance()
   
   remote$dom.clickElement(".rstudio_modify_chunk")
   Sys.sleep(1)
   remote$dom.clickElement("#rstudio_chunk_opt_warnings")
   remote$dom.clickElement("#rstudio_chunk_opt_messages")
   remote$keyboard.insertText("<Escape>")
   expect_equal('```{r setup, include=FALSE}', editor$session$getLine(4))
   expect_equal('knitr::opts_chunk$set(', editor$session$getLine(5))
   expect_equal('\teval = FALSE,', editor$session$getLine(6))
   expect_equal('\tmessage = TRUE,', editor$session$getLine(7))
   expect_equal('\twarning = TRUE,', editor$session$getLine(8))
   expect_equal('\tinclude = FALSE', editor$session$getLine(9))
   expect_equal(')', editor$session$getLine(10))

})

