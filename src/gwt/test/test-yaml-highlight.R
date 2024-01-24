#| 
#| # Check handling of comments.
#| comments:
#| - Some text. # This is a comment.
#| - Some text# This is not a comment!
#| - Some#text#with#embedded#hashes. # A real comment.
#| - # This is a comment. The entry here should be parsed as NULL.
#| 
#| text:
#| - This is a string. It has numbers like 1, 2, and 3.
#| - The next few lines are plain numbers, and should be highlighted like so.
#| - 42.0
#| - -35.5
#| 
#| strings:
#| - "This is a
#|    multi-line string.
#|    It has some escapes: \a.
#|    \"These are embedded quotes.\""
#| 
#| - 'This is a
#|    multi-line string.
#|    Quotes are escaped by ''doubling'' them.'
#|    
#| - >
#|    This is a multi-line string.
#|    It doesn't end until the indent decreases
#|    below the initial indent.
#| 
#|    For example, this is still part of the string:
#|    section:
#|       sub-section:
#|          value
#| 
#|    The next part is no longer part of the string.
#| 
#| lists:
#| - section: ["a", b42, 42.0, no, 64.0]
#| - section: [
#|    "a",
#|    b42,
#|    42.0,
#|    true,
#|    64.0
#| ]
#| 
#| dictionaries:
#| - { "a": 42, "b": 64 }
#| 
#| https://github.com/rstudio/rstudio/issues/13836:
#| - label: fig-plot-no-1
#| - fig-subcap:
#|    - Caption for Plot 1
#|    - Caption for Plot 2
#| - layout-ncol: 2.1
#| 
