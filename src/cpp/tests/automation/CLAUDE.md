# BRAT Test Framework - How to Author Tests

BRAT (Built-in RStudio Automated Tests) uses CDP (Chrome DevTools Protocol) to automate and test RStudio's UI. Tests are written in R using `testthat`, with a `remote` object that provides methods for controlling the IDE.

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
- Filters tests by markers (see [Test Markers](#test-markers))
- Calls `session.reset()` before each test to clear popups, console, and close documents

## The Remote Object API

The `remote` object is your primary interface for controlling RStudio. It provides namespaced methods for different aspects of automation.

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
# Open a temporary file, execute callback, then auto-close and clear console
contents <- "# My R script\nx <- 1:10\nplot(x)"
remote$editor.executeWithContents(".R", contents, function(editor) {
   # editor object provides ACE editor methods
   editor$gotoLine(2)
   editor$insert("\ny <- x^2")
   value <- editor$getValue()
})
# NOTE: executeWithContents has on.exit cleanup that closes the document
# and sends Ctrl+L to clear the console. No manual cleanup needed.

# Open a temporary file without a callback (caller must close manually)
remote$editor.openWithContents(".R", contents)

# Open an existing file by path
remote$editor.openDocument("/path/to/file.R")

# Close the active document
remote$editor.closeDocument()

# Get the active ACE editor JavaScript object
editor <- remote$editor.getInstance()
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

# Click an element — supports selector, nodeId, or objectId
remote$dom.clickElement(selector = "#button-id")
remote$dom.clickElement(nodeId = nodeId)
remote$dom.clickElement(objectId = objId,
                        verticalOffset = 10L,
                        horizontalOffset = 0L,
                        button = "left")

# Check/uncheck checkboxes
remote$dom.setChecked("#my-checkbox", checked = TRUE)

# Insert text into an input field
remote$dom.insertText("#search-box", "search term")

# Check if element is checked — accepts CSS selector string or numeric nodeId
isChecked <- remote$dom.isChecked("#my-checkbox")
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

# Execute a keyboard shortcut directly (platform-aware modifier handling)
# "Cmd" maps to Meta on macOS, Ctrl elsewhere
remote$keyboard.executeShortcut("Ctrl + L")
remote$keyboard.executeShortcut("Cmd + Shift + P")
```

### Command Execution (`remote$commands.*`)

```r
# Execute RStudio commands by name
remote$commands.execute("saveSourceDoc")
remote$commands.execute("buildAll")
remote$commands.execute("restartR")

# Can also pass command objects directly
remote$commands.execute(.rs.appCommands$sourceActiveDocument)
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
# Reset the session state (clears popups, console, closes documents)
# Called automatically by .rs.test() before each test
remote$session.reset()

# Restart the R session (full restart, waits for session to be ready)
remote$session.restart()

# Quit the session
remote$session.quit()
```

### Modal Dialogs (`remote$modals.*`)

```r
# Click a modal dialog button by name (e.g. "ok", "cancel")
# Constructs selector #rstudio_dlg_{buttonName}
remote$modals.click("ok")
remote$modals.click("cancel")
```

### Project Operations (`remote$project.*`)

```r
# Create and open a new project (generates random name if NULL)
remote$project.create(projectName = "myproject", type = "default")
remote$project.create(type = "package")

# Close the current project
remote$project.close()

# Get the project toolbar label text
label <- remote$project.getLabel()
```

### Package Utilities (`remote$package.*`)

```r
# Check if an R package is installed
remote$package.isInstalled("dplyr")
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

Standard `testthat` expectation (>= 3.1.0) for checking if a value contains a substring:

```r
output <- remote$console.getOutput()
expect_contains(output, "expected text")
```

## Best Practices

### Clean Up with `withr::defer()`

```r
self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# Create temporary files/directories with cleanup
tempDir <- remote$files.createDirectory()
withr::defer(remote$files.remove(tempDir))
```

### Use Explicit Waits, Not `Sys.sleep()`

```r
# Good: Wait for specific element
remote$dom.waitForElement("#results-panel")

# Good: Wait for condition
.rs.waitUntil("console ready", function() {
   !grepl("rstudio-console-busy",
          remote$js.querySelector("#rstudio_console_input")$className)
})
```

### Use `executeExpr` Over String Concatenation

```r
# Good: Clean and readable
remote$console.executeExpr({
   data <- data.frame(x = 1:10, y = rnorm(10))
   plot(data$x, data$y)
})

# Avoid: String concatenation
remote$console.execute(paste0("data <- data.frame(", "x = 1:10, ", "y = rnorm(10)", ")"))
```

## Debugging Tests

When tests fail unexpectedly, inspect the current state:

```r
# See what's in the console
output <- remote$console.getOutput()
print(output)

# Check if expected elements exist
exists <- remote$dom.elementExists("#expected-element")
print(paste("Element exists:", exists))

# Get full page HTML
html <- remote$js.eval("document.documentElement.outerHTML")
```
