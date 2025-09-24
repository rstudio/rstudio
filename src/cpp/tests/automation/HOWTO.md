# BRAT Test Framework - How to Author Tests

BRAT (Built-in RStudio Automated Tests) is a comprehensive testing framework for automating RStudio IDE functionality. This guide explains how to write effective BRAT tests.

## Table of Contents

1. [Test Framework Architecture](#test-framework-architecture)
2. [Basic Test Structure](#basic-test-structure)
3. [The Remote Object API](#the-remote-object-api)
4. [Common Test Patterns](#common-test-patterns)
5. [Test Markers](#test-markers)
6. [Helper Functions](#helper-functions)
7. [Best Practices](#best-practices)

## Test Framework Architecture

BRAT tests use a Chrome DevTools Protocol (CDP) based automation system that allows programmatic control of RStudio's UI. The framework consists of:

- **Test Runner**: Uses the `testthat` R testing framework
- **Remote Object**: A connection to RStudio that provides automation methods
- **Session Modules**: R code in `/src/cpp/session/modules/automation/` that implements the automation API
- **Test Files**: Located in `/src/cpp/tests/automation/testthat/test-automation-*.R`

## Basic Test Structure

Every BRAT test file follows this pattern:

```r
library(testthat)

# Create a remote connection to RStudio
self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# Define tests using .rs.test()
.rs.test("description of what you're testing", {
   # Your test code here
   expect_equal(1 + 1, 2)
})
```

### The `.rs.test()` Function

Use `.rs.test()` instead of `testthat::test_that()`. This wrapper:
- Automatically resets the RStudio session before each test
- Handles test markers for selective execution
- Provides better error reporting

## The Remote Object API

The `remote` object is your primary interface for controlling RStudio. It provides namespaced methods for different aspects of automation:

### Console Operations (`remote$console.*`)

```r
# Clear the console
remote$console.clear()

# Execute R code (waits for completion by default)
remote$console.execute("x <- 42")

# Execute R expressions with automatic quoting
remote$console.executeExpr({
   library(dplyr)
   data.frame(x = 1:5) %>%
      mutate(y = x * 2)
})

# Get console output
output <- remote$console.getOutput()        # All output
output <- remote$console.getOutput(n = 10)  # Last 10 lines
```

### Editor Operations (`remote$editor.*`)

```r
# Open a temporary file with specific contents
contents <- "# My R script\nx <- 1:10\nplot(x)"
remote$editor.executeWithContents(".R", contents, function(editor) {
   # editor object provides ACE editor methods
   editor$gotoLine(2)
   editor$insert("\ny <- x^2")
   value <- editor$getValue()
})

# Common editor methods within executeWithContents:
# - editor$getValue() - get full document content
# - editor$gotoLine(n) - move to line n
# - editor$insert(text) - insert text at cursor
# - editor$selectAll() - select all text
```

### DOM Manipulation (`remote$dom.*`)

```r
# Wait for an element to appear
nodeId <- remote$dom.waitForElement("#my-button")

# Check if element exists
exists <- remote$dom.elementExists(".my-class")

# Query for elements
nodeId <- remote$dom.querySelector("#unique-id")
nodeIds <- remote$dom.querySelectorAll(".multiple-items")

# Click an element
remote$dom.clickElement(selector = "#button-id")
# Or by nodeId
remote$dom.clickElement(nodeId = nodeId)

# Check/uncheck checkboxes
remote$dom.setChecked("#my-checkbox", checked = TRUE)

# Insert text into an input field
remote$dom.insertText("#search-box", "search term")

# Check if element is checked
isChecked <- remote$dom.isChecked(nodeId)
```

### JavaScript Execution (`remote$js.*`)

```r
# Execute JavaScript and get result
result <- remote$js.eval("1 + 1")

# Access DOM elements via JavaScript
element <- remote$js.querySelector("#rstudio_console_output")
text <- element$innerText
className <- element$className

# Query multiple elements
elements <- remote$js.querySelectorAll(".tab-title")
for (i in seq_len(elements$length)) {
   el <- elements[[i - 1L]]  # JavaScript uses 0-based indexing
   print(el$innerText)
}
```

### Keyboard Input (`remote$keyboard.*`)

```r
# Insert text with special keys
remote$keyboard.insertText("Hello", "<Tab>", "World", "<Enter>")

# Common special keys:
# <Enter>, <Escape>, <Tab>, <Backspace>
# <Ctrl + A>, <Ctrl + C>, <Ctrl + V>
# <Shift + Tab>, <Alt + Enter>
# Arrow keys: <Up>, <Down>, <Left>, <Right>
```

### Command Execution (`remote$commands.*`)

```r
# Execute RStudio commands by name
remote$commands.execute("saveSourceDoc")
remote$commands.execute("buildAll")
remote$commands.execute("restartR")

# Commands are defined in:
# src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.cmd.xml
```

### Completions (`remote$completions.*`)

```r
# Request code completions
completions <- remote$completions.request("stats::rn")
# Returns: c("rnbinom", "rnorm")

# Test function parameter completions
completions <- remote$completions.request("rnorm(")
# Returns: c("n =", "mean =", "sd =")
```

### Session Management (`remote$session.*`)

```r
# Reset the session (called automatically by .rs.test())
remote$session.reset()

# Quit the session
remote$session.quit()
```

### File Operations (`remote$files.*`)

```r
# Create a directory
path <- remote$files.createDirectory("/tmp/test-dir")

# Remove files or directories
remote$files.remove("/tmp/test-dir", recursive = TRUE)
```

### Preferences (`remote$console.executeExpr()` with `.rs.uiPrefs`)

```r
# Set UI preferences
remote$console.executeExpr({
   .rs.uiPrefs$stripTrailingWhitespace$set(TRUE)
   .rs.uiPrefs$consoleHighlightConditions$set("errors_warnings_messages")
})

# Clear preferences (reset to default)
remote$console.executeExpr({
   .rs.uiPrefs$stripTrailingWhitespace$clear()
})
```

## Common Test Patterns

### Testing Console Output

```r
.rs.test("console displays errors correctly", {
   remote$console.clear()

   remote$console.executeExpr({
      stop("This is an error")
   })

   output <- remote$console.getOutput()
   expect_contains(output, "Error: This is an error")
})
```

### Testing Editor Features

```r
.rs.test("auto-indentation works correctly", {
   contents <- "if (TRUE) {"

   remote$editor.executeWithContents(".R", contents, function(editor) {
      editor$gotoLine(1)
      editor$navigateLineEnd()
      remote$keyboard.insertText("<Enter>")

      # Check that cursor is indented
      position <- editor$getCursorPosition()
      expect_equal(position$column, 3)  # Expecting 3-space indent
   })
})
```

### Testing UI Elements

```r
.rs.test("build pane shows compilation errors", {
   # Trigger a build
   remote$commands.execute("buildAll")

   # Wait for build pane to appear
   remote$dom.waitForElement("#rstudio_build_tab")

   # Check for error markers
   errorElements <- remote$js.querySelectorAll(".build-error")
   expect_true(errorElements$length > 0)
})
```

### Testing Keyboard Shortcuts

```r
.rs.test("Ctrl+Enter executes current line", {
   remote$console.clear()
   remote$keyboard.insertText("print('hello')", "<Ctrl + Enter>")

   output <- remote$console.getOutput()
   expect_contains(output, "[1] \"hello\"")
})
```

## Test Markers

Markers allow selective test execution. They're useful for:
- Marking tests as work-in-progress (`"wip"`)
- Categorizing tests by feature area
- Running specific test subsets during development

### Adding Markers to Tests

```r
.rs.markers("wip", "editor")
.rs.test("new editor feature being developed", {
   # Test implementation
})
```

### Running Tests with Markers

```bash
# Run only tests marked with "wip" using server mode
./rserver-dev --run-automation --automation-markers="wip"

# Run tests with either "editor" or "console" markers using Electron
npm run automation -- --automation-markers="editor console"
```

## Helper Functions

### `.rs.heredoc()`

Creates multi-line strings with clean formatting:

```r
code <- .rs.heredoc('
   # This is properly formatted code
   for (i in 1:10) {
      print(i)
   }
')
```

### `.rs.waitUntil()`

Waits for a condition to become true:

```r
.rs.waitUntil("element becomes visible", function() {
   element <- remote$js.querySelector("#my-element")
   !is.null(element) && element$offsetWidth > 0
})
```

### `.rs.trimWhitespace()`

Removes leading/trailing whitespace:

```r
cleaned <- .rs.trimWhitespace("  text with spaces  ")
# Returns: "text with spaces"
```

### `expect_contains()`

Custom expectation for checking if a value contains a substring:

```r
output <- remote$console.getOutput()
expect_contains(output, "expected text")
```

## Best Practices

### 1. Always Clean Up

Use `withr::defer()` to ensure cleanup happens even if tests fail:

```r
self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# Create temporary files/directories with cleanup
tempDir <- remote$files.createDirectory()
withr::defer(remote$files.remove(tempDir))
```

### 2. Use Explicit Waits

Don't rely on `Sys.sleep()` alone. Wait for specific conditions:

```r
# Good: Wait for specific element
remote$dom.waitForElement("#results-panel")

# Good: Wait for console to be ready
.rs.waitUntil("console ready", function() {
   !grepl("rstudio-console-busy",
          remote$js.querySelector("#rstudio_console_input")$className)
})

# Avoid: Arbitrary sleep
Sys.sleep(2)  # Don't know if 2 seconds is enough
```

### 3. Test One Thing at a Time

Each test should focus on a single feature or behavior:

```r
# Good: Focused test
.rs.test("Tab key triggers completion", {
   remote$keyboard.insertText("rn", "<Tab>")
   # Check completion popup appears
})

# Avoid: Testing multiple unrelated things
.rs.test("console works", {
   # Tests completion, execution, AND error handling in one test
})
```

### 4. Use Descriptive Test Names

Test names should clearly describe what is being tested:

```r
# Good
.rs.test("auto-save triggers when switching tabs", { })

# Avoid
.rs.test("test save feature", { })
```

### 5. Reset State When Needed

While `.rs.test()` resets the session, you may need to clear specific state:

```r
.rs.test("console history navigation", {
   remote$console.clear()  # Start with clean console

   # Your test...
})
```

### 6. Handle Asynchronous Operations

Many RStudio operations are asynchronous. Always wait for them to complete:

```r
.rs.test("document saves successfully", {
   remote$editor.executeWithContents(".R", "x <- 1", function(editor) {
      remote$commands.execute("saveSourceDoc")

      # Wait for save to complete
      .rs.waitUntil("document saved", function() {
         !grepl("unsaved-indicator",
                remote$js.querySelector(".tab-title")$className)
      })
   })
})
```

### 7. Use `executeExpr` for Complex R Code

For multi-line R code or code with complex quoting, use `executeExpr`:

```r
# Good: Clean and readable
remote$console.executeExpr({
   data <- data.frame(
      x = 1:10,
      y = rnorm(10)
   )
   plot(data$x, data$y)
})

# Avoid: String concatenation
remote$console.execute(paste0(
   "data <- data.frame(",
   "x = 1:10, ",
   "y = rnorm(10)",
   ")"
))
```

## Debugging Tests

### Check What's Visible

When tests fail unexpectedly, inspect the current state:

```r
# See what's in the console
output <- remote$console.getOutput()
print(output)

# Check if expected elements exist
exists <- remote$dom.elementExists("#expected-element")
print(paste("Element exists:", exists))

# Get full page HTML (useful for debugging)
html <- remote$js.eval("document.documentElement.outerHTML")
```

### Use Interactive Mode

Run tests interactively to debug:

```r
# In R console
source("testthat.R")
setwd("testthat")
source("test-automation-console.R")
# Now you can inspect the remote object and run test code line by line
```

## Additional Resources

- Test files: `/src/cpp/tests/automation/testthat/test-automation-*.R`
- Automation modules: `/src/cpp/session/modules/automation/`
- RStudio commands: `/src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.cmd.xml`
- Running tests: See `README.md` in this directory

## Contributing New Tests

When adding new tests:

1. Name your test file `test-automation-<feature>.R`
2. Group related tests in the same file
3. Use markers for tests that are experimental or WIP
4. Ensure tests are independent and can run in any order
5. Document any special setup requirements in comments
6. Test both success and failure cases when applicable
