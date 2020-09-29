# To test here, try typing 'Cmd + Enter' and ensure that the cursor
# executes and jumps from the consecutively numbered blocks.
library(dplyr)
library(data.table)

# 1
{
   1234 %>%
      # a comment
      rnorm() %>%
      
      sum()
}

# 2
{
   1 + 2
}

# 3
apple <- function(banana)
{
   print(1 +
            2 +
            3)
}

# 4
1 + 2

# 5
1234 %>% {
   1
   2
   3
} %>% sum()

# 6
1 + 2

# 7
list(
   1,
   list(
      2
   )
)

# 8
foo <- function(a, b, c) {
   
}

# 9
foo <- function(a, b, c)
{
   a <- 1
   b <- 2
}

# 10
f <- function(
   a = 1,
   b = 2,
   c = 3,
   d = list(
      x = 'x',
      y = 'y'
   ),
   e = 5
) {
   
}

# 11
x <- f(a = 1,
       b = 2,
       c = 3#,
       # d = 4
)

# 12: data.table style
dt[ , sequence := seq_len( nrow( dt ) )

    ][ , normal := rnorm( nrow( dt ) )
       
       ][ , uniform := runif( nrow( dt ) ) ]

# 13: multi-line strings
mlstring <- "first
second
third"

# 14: roxygen comments could confuse the multiline
# expression system; e.g.
#' [name](link)
statement <- 1 + 1

# 15: certain pipe styles
mtcars %>%
   filter(
      mpg > 20
   ) %>%
   select(everything())

# 16: certain pipe styles
mtcars %>%
   filter(
      mpg > 20) %>%
   select(everything())

# 17: more pipes
mtcars %>%
   filter(
      mpg > 20) %>% select(everything())

# 18: more pipes
mtcars %>% 
   mutate(
      x1 = "ok here") %>% 
   mutate(
      x2 = "not ok") # use Ctrl + Enter in this line

# cursor should end here after executing all lines
EOF
