
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

   # The BRAT server runs without --www-enable-origin-check=1, so
   # AsyncServerImpl's pre-handler Origin/Referer check is inactive. Every
   # 400 below is therefore produced by the /files/ handler itself, which
   # is also the situation in a default open-source deployment.

   # Warm up: the very first httr request against a fresh rserver goes
   # through session-establishment scaffolding before the /files/ handler
   # gets a chance to run, so its response status doesn't reflect the
   # cross-site policy. Make a throwaway request first so subsequent
   # assertions actually exercise the handler.
   invisible(request(list(list(`Sec-Fetch-Site` = "same-origin"))))

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
   sameOriginResponse <- request(list(list(`Sec-Fetch-Site` = "same-origin")))
   expect_equal(httr::status_code(sameOriginResponse), 200)
   # Tie "200" to "served the file" so a regression that returns 200 with
   # the wrong contents (e.g. an empty body) doesn't go unnoticed.
   expect_equal(
      .rs.trimWhitespace(httr::content(sameOriginResponse, "text", encoding = "UTF-8")),
      "test content"
   )
   expect_equal(getStatus(list(list(`Sec-Fetch-Site` = "same-site"))), 200)
   expect_equal(getStatus(list(list(`Sec-Fetch-Site` = "none"))),      200)

   # The Sec-Fetch-Site comparison is case-sensitive (per spec, browsers
   # send the lowercase tokens). A capitalized value isn't on the
   # allow-list, so it must be treated as cross-site and rejected. Pin
   # this so a future change to case-insensitive matching is a deliberate
   # decision rather than an accident.
   expect_equal(getStatus(list(list(`Sec-Fetch-Site` = "Cross-Site"))), 400)

   # Unknown / future Sec-Fetch-Site values must default to deny rather
   # than fall through both branches and silently allow the request.
   expect_equal(getStatus(list(list(`Sec-Fetch-Site` = "garbage"))), 400)

   # Older browsers don't send Sec-Fetch-Site. The handler's fallback
   # check then enforces a same-origin Referer when one is present.
   sameOriginReferer <- sprintf("%s/", serverHost)
   crossOriginReferer <- "https://attacker.example/"

   # Sec-Fetch-Site absent + same-origin Referer -> allowed.
   expect_equal(
      getStatus(list(list(Referer = sameOriginReferer))),
      200
   )

   # Sec-Fetch-Site absent + cross-origin Referer -> rejected by the
   # handler's Referer fallback.
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
   # bare-host-equality bugs. (AsyncServerImpl's pre-handler check is
   # scheme-agnostic -- it compares host:port but ignores http vs https
   # -- so even when that layer is enabled in Workbench, this handler is
   # what catches the cross-scheme case.)
   crossSchemeReferer <- sprintf("https://localhost:8788/")
   expect_equal(
      getStatus(list(list(Referer = crossSchemeReferer))),
      400
   )

   # Edge case: same scheme + same host but different port -> rejected
   # by the handler's host:port comparison.
   crossPortReferer <- "http://localhost:9999/"
   expect_equal(
      getStatus(list(list(Referer = crossPortReferer))),
      400
   )

   # Malformed Referer values must fail closed: the URL parser rejects
   # anything outside http/https/file/ftp(s), so these all produce empty
   # protocol/host. The handler treats that as a parse failure and
   # rejects rather than collapsing both sides to empty and matching.
   expect_equal(getStatus(list(list(Referer = "javascript:alert(1)"))), 400)
   expect_equal(getStatus(list(list(Referer = "about:blank"))),         400)
   expect_equal(getStatus(list(list(Referer = "not a url"))),           400)
})
