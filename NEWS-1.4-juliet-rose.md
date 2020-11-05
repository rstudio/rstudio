
## RStudio 1.4 "Juliet Rose" Release Notes


### Misc

* Improve detection for crashes that occur early in session initialization (#7983)

### RStudio Server

* **BREAKING:** RStudio when served via `http` erroneously reported its own address as `https` during redirects if the header `X-Forwarded-Proto` was defined by a proxy. That could lead to a confusing proxy setup. That has been fixed, but existing proxy installations with redirect rewite settings matching for `https` may have to be adjusted.
