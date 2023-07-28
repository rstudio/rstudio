
context("Translations")

# https://github.com/rstudio/rstudio/issues/10308
test_that("translations work on Windows", {
   skip_if(Sys.info()[["sysname"]] != "Windows")
   withr::local_envvar(LANGUAGE = "fr")
   err <- tryCatch(noSuchVariable, error = identity)
   expect_equal(conditionMessage(err), "objet 'noSuchVariable' introuvable")
})
