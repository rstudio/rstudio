
projectRoot <- here::here()
testRoot <- file.path(projectRoot, "src/cpp/tests/automation")
testthat::test_dir(file.path(testRoot, "testthat"))
