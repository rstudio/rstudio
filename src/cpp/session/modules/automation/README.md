
Automation
==========

This folder provides tools to enable automated testing of RStudio from RStudio
itself. We use the  [Chrome DevTools Protocol][cdp] to control remote RStudio
instances. This README gives a brief summary of each file's intended scope:

* SessionAutomation.R: The base set of tools for starting an RStudio
  automation agent, and connecting to it. It also provides tools for making
  websocket requests and receiving websocket responses.

* SessionAutomationClient.R: An auto-generated interface to the remote RStudio
  agent. The functions defined here all map directly to a function available
  from the Chrome DevTools Protocol.

* SessionAutomationTargets.R: A collection of useful query selectors /
  targets, for interacting with particular facets of the RStudio IDE.

* SessionAutomationRemote.R: The "remote control", used as the primary
  interface to interacting with the remote RStudio IDE instance. This object
  is essentially a collection of methods for performing useful automation
  actions.

The automated tests themselves live in `src/cpp/tests/automation`.


[cdp]: https://chromedevtools.github.io/devtools-protocol/
