
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

.rs.test("/files/ enforces cross-site protections", {
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

   serverHost <- "http://localhost:8788"
   url <- sprintf("%s/files/%s", serverHost, testFile)
   request <- function(headers = list()) {
      args <- c(list(url), lapply(headers, function(v) {
         do.call(httr::add_headers, v)
      }))
      do.call(httr::GET, args)
   }
   getStatus <- function(headers = list()) {
      httr::status_code(request(headers))
   }

   # Primary check: Sec-Fetch-Site == "cross-site" is the explicit attacker
   # signal and must be rejected.
   expect_equal(
      getStatus(list(list(`Sec-Fetch-Site` = "cross-site"))),
      400
   )

   # Other Sec-Fetch-Site values are legitimate. "none" covers user-typed
   # URLs and bookmarks. "same-origin" covers in-RStudio navigation.
   # "same-site" is allowed deliberately so Posit Workbench front-ends
   # iframe-embedding RStudio across sibling subdomains keep functioning.
   expect_equal(getStatus(list(list(`Sec-Fetch-Site` = "same-origin"))), 200)
   expect_equal(getStatus(list(list(`Sec-Fetch-Site` = "same-site"))),   200)
   expect_equal(getStatus(list(list(`Sec-Fetch-Site` = "none"))),        200)

   # Older browsers don't send Sec-Fetch-Site. The handler's fallback
   # check then enforces a same-origin Referer when one is present.
   sameOriginReferer <- sprintf("%s/", serverHost)
   crossOriginReferer <- "https://attacker.example/"

   # Sec-Fetch-Site absent + same-origin Referer -> allowed.
   expect_equal(
      getStatus(list(list(Referer = sameOriginReferer))),
      200
   )

   # Sec-Fetch-Site absent + cross-origin Referer -> rejected. This is
   # caught by AsyncServerImpl's pre-handler check, but the assertion
   # documents the end-to-end policy.
   expect_equal(
      getStatus(list(list(Referer = crossOriginReferer))),
      400
   )

   # Sec-Fetch-Site absent + Referer absent -> allowed. This preserves
   # direct URL navigation in older browsers; the residual attack window
   # (older browser + attacker-strips-referer) is the documented trade-off.
   expect_equal(getStatus(), 200)

   # Edge case: same host:port but different scheme is NOT same-origin.
   # The handler compares scheme + host:port to canonicalize away from
   # bare-host-equality bugs. AsyncServerImpl's pre-handler check is
   # host-only, so it lets this through; the /files/ handler rejects it.
   crossSchemeReferer <- sprintf("https://localhost:8788/")
   expect_equal(
      getStatus(list(list(Referer = crossSchemeReferer))),
      400
   )

   # Edge case: same scheme + same host but different port -> rejected.
   # Caught upstream by AsyncServerImpl, but asserted here so a
   # regression that loosens host:port comparison is visible.
   crossPortReferer <- "http://localhost:9999/"
   expect_equal(
      getStatus(list(list(Referer = crossPortReferer))),
      400
   )
})
