## Heading 1 {#myHeading}

<div>
So fly
</div>

This is <em>emphasized</em>.

```{r woozy, fig.width="4"}
function max(a, b) {
  return a > b ? a : b
}
```

```{r foobar}
function max(a, b) {
  return a > b ? a : b
}
```

Term 1

:   Definition 1

Term 2 with *inline markup*

:   Definition A

        { some code, part of Definition 2 }

    Third paragraph of definition 2.

:   Definition B

    This is the second definition.


| The limerick packs laughs anatomical
| In space that is quite economical.
|    But the good ones I've seen
|    So seldom are clean
| And the clean ones so seldom are comical

---
title: "This is the dope stuff"
author: "J.J. Allaire"
---

`Some text`{=rtf}.

```{=openxml}
<w:p>
  <w:r>
    <w:br w:type="page"/>
  </w:r>
</w:p>
```

::::: {#special .sidebar}
Here is a paragraph.

And another.
:::::

[This is *some* text]{.class key="val"}

@smith04 [p. 33] says blah.

---
title: "This is the dope stuff"
author: "J.J. Allaire"
---


[@smith{ii, A, D-Z}, with a suffix]

[-@Nobody06 dat boy; -@myhouse spat boy]

> Here is a ![alt me](/images/logo.png "title"){width="100px"} paragraph.
>
> And another.

Some text.

Blah blah [see @doe99, pp. 33-35; also @smith04, chap. 1].

Blah **blah** [@doe99, pp. 33-35, 38-39 and `passim` in the house].

Blah blah [@smith04; @doe99].

This is some inline math: $aasf$$. Here it is!

$$
s
$$

This is display math rendered inline: $$ f(x) $ \$ = x^2 $$. How did that work out?

![the](/images/logo.png "title"){width="100px"}

+-------+--------------------------+--------+
| Right | Left                     | Center |
+======:+:=========================+:======:+
| 12    | 12                       | asdf   |
+-------+--------------------------+--------+
| 123   | 123                      | 123    |
+-------+--------------------------+--------+
| 1     | 1                        | 1      |
+-------+--------------------------+--------+


: Here is the caption.

Just a plain old \raw.

Here is some \textbf{raw text} inline LaTeX.

-   First
    -   Thrilla
    -   Manilla
-   Second


(@) Item 1
(@) Item 2

Some text here

(@) Item 3
(@) Item 4


## Heading 2

### JavaScript

Some JavaScript code:

``` {.javascript data-foo=400}
function max(a, b) {
  return a > b ? a : b
}
```

### Python

Some Python code:

``` {.python}
# Define a function `plus()`
def plus(a,b):
  return a + b
  
# Create a `Summation` class
class Summation(object):
  def sum(self, a, b):
    self.contents = a + b
    return self.contents 
```

## Heading 3

- [x] Checkbox me

- Another item

- [ ] Don't checkbox me


Here we go again:

- Not a tight

- List today


## Heading 4

### Formatting

"This" is **bold** text. 

*italic* at the beginning of a line.

This is*italic* text.

This **bold text with embedded *italic* text**.

This text has ~~strikeout~~ text.

Here we try out superscript^super^ and subscript~sub~

This is the [small caps]{.smallcaps} style.

### Footnotes

Let's try out^[an inline footnote] and then some text after the footnote.

Let's try out^[an inline footnote] and then some text after the footnote.

Let's try out^[an inline footnote] and then some text after the footnote.

### Linebreaks

This is hard break.  
Next line after hard break.

Nulla facilisi. Donec nec facilisis est. Aenean porttitor volutpat mauris, a imperdiet libero molestie quis. Vivamus commodo purus id aliquet hendrerit. Aliquam ipsum ante, consectetur ut consectetur vitae, tempor sit amet justo. Aenean eget tempor quam. Integer eu metus facilisis, lacinia lectus vitae, molestie est. Sed eget auctor libero. Proin aliquam tellus sed enim consectetur, vel luctus nunc auctor.

This is soft break.
Next line after soft break.

### Lists

- Unordered
- List
- Here we go

i. Ordered

ii. List

iii. Example of

## Heading 5

This is a link to *[Google](https://www.google.com){#myLink .splat target=_blank}*

This is an image:


> This is a blockquote. See how it runs!

This is *`source code`{.example}* text.



