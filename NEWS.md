RStudio 1.4 "Evergreen Rose"

* Removed the breaking change introduced in Juliet Rose that changed the behavior of the X-Forwarded-Proto header when RSW is behind a proxy server (Pro #2657)
* Fixed an issue where RStudio Desktop Pro could fail when connecting to remote sessions via https (Pro #2651)
* Fixed a crash that can occur when opening a VS Code session behind a path-rewriting proxy (Pro #2699)
* Updated embedded nginx to 1.20.1 (Pro #2676)
* Blocked access to code-server's `/absproxy/<port>` url (Pro #3275)
* Fixed a security issue where shiny apps and vscode sessions remained active after signout (rstudio-pro#3287)
