
library(testthat)

remote <- .rs.automation.createRemote()

test_that("Quarto Documents are highlighted as expected", {
   
   document <- .rs.heredoc('
      ---
      title: R Markdown Document
      ---

      ```{r}
      #| echo: true
      ````

      1 + 1
   ')
   
   remote$documentExecute(".Rmd", document, {
      
      response <- remote$evaluateJavascript(r'{
         var container = document.getElementById("rstudio_source_text_editor");
         var editor = container.env.editor;
         var tokens = editor.session.getTokens(8);
         JSON.stringify(tokens[0].value);
      }')
   
      expect_equal(response$result$value, "\"1 + 1\"")
   
   })
   
})
