context("lists")

test_that("lists converted to scalars", {
   input <- list(1, 2, 3)
   expect_equal(.rs.scalarListFromList(input), 
                list(.rs.scalar(1), .rs.scalar(2), .rs.scalar(3)))
})

test_that("nested lists converted to scalars", {
   input <- list(1, 2, list(3, 4))
   expect_equal(.rs.scalarListFromList(input), 
                list(.rs.scalar(1), .rs.scalar(2), 
                     list(.rs.scalar(3), .rs.scalar(4))))
})

