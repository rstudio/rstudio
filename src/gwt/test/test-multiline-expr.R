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
apple <- function(banana) {
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
