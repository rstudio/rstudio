# To test here, try typing 'Cmd + Enter' and ensure that the cursor
# executes and jumps from the consecutively numbered blocks.

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

# cursor should end here after executing all lines
EOF
