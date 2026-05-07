
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


# Tests for the /files/ HTTP endpoint security check (rstudio-pro#10980).
# The endpoint serves user-controlled files with native MIME types and must
# reject cross-site requests so attacker pages can't redirect victims into
# loading attacker HTML in the RStudio session origin.
#
# The BRAT server runs at http://localhost:8788 with --auth-none=1 (see
# automation.ensureRunningServerInstance), so we can drive raw HTTP requests
# from the test driver via httr without dealing with auth cookies.

.rs.test("/files/ rejects cross-site requests", {
   skip_if(.rs.isDesktop())

   # Drop a small test file in the home directory so the handler reaches the
   # Sec-Fetch-Site check before any not-found logic. Use a leading '.' so we
   # don't pollute the visible Files pane during test runs.
   testFile <- ".rstudio-test-files-endpoint"
   remote$console.execute(sprintf(
      "writeLines('test content', file.path('~', '%s'))",
      testFile
   ))
   withr::defer({
      remote$console.execute(sprintf(
         "unlink(file.path('~', '%s'))",
         testFile
      ))
   })

   url <- sprintf("http://localhost:8788/files/%s", testFile)
   getStatus <- function(secFetchSite = NULL) {
      response <- if (is.null(secFetchSite)) {
         httr::GET(url)
      } else {
         httr::GET(url, httr::add_headers(`Sec-Fetch-Site` = secFetchSite))
      }
      httr::status_code(response)
   }

   # Cross-site requests must be rejected.
   expect_equal(getStatus("cross-site"), 400)

   # All other Sec-Fetch-Site values are legitimate and must pass through.
   # "none" covers user-typed URLs and bookmarks; "same-origin" / "same-site"
   # cover navigations from inside RStudio (or a sibling subdomain).
   expect_equal(getStatus("same-origin"), 200)
   expect_equal(getStatus("same-site"),   200)
   expect_equal(getStatus("none"),        200)

   # Older browsers don't send Sec-Fetch-* headers at all; the existing
   # AsyncServerImpl Origin/Referer check still catches non-empty mismatched
   # Referers, so we accept the absent-header case here.
   expect_equal(getStatus(NULL), 200)
})
