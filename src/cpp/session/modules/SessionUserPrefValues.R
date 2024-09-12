#
# SessionUserPrefValues.R
#
# Copyright (C) 2024 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

#
# This file was automatically generated -- please do not modify it by hand.
#

.rs.setVar("uiPrefs", new.env(parent = emptyenv()))

# Run .Rprofile on resume
# Whether to run .Rprofile again after resuming a suspended R session.
.rs.uiPrefs$runRprofileOnResume <- list(
   get = function() { .rs.getUserPref("run_rprofile_on_resume") },
   set = function(value) { .rs.setUserPref("run_rprofile_on_resume", value) }
)

# Save workspace on quit
# Whether to save the workspace to an .Rdata file after the R session ends.
.rs.uiPrefs$saveWorkspace <- list(
   get = function() { .rs.getUserPref("save_workspace") },
   set = function(value) { .rs.setUserPref("save_workspace", value) }
)

# Load workspace on start
# Whether to load the workspace when the R session begins.
.rs.uiPrefs$loadWorkspace <- list(
   get = function() { .rs.getUserPref("load_workspace") },
   set = function(value) { .rs.setUserPref("load_workspace", value) }
)

# Initial working directory
# The initial working directory for new R sessions.
.rs.uiPrefs$initialWorkingDirectory <- list(
   get = function() { .rs.getUserPref("initial_working_directory") },
   set = function(value) { .rs.setUserPref("initial_working_directory", value) }
)

# 
# The CRAN mirror to use.
.rs.uiPrefs$cranMirror <- list(
   get = function() { .rs.getUserPref("cran_mirror") },
   set = function(value) { .rs.setUserPref("cran_mirror", value) }
)

# 
# The name of the default Bioconductor mirror.
.rs.uiPrefs$bioconductorMirrorName <- list(
   get = function() { .rs.getUserPref("bioconductor_mirror_name") },
   set = function(value) { .rs.setUserPref("bioconductor_mirror_name", value) }
)

# 
# The URL of the default Bioconductor mirror.
.rs.uiPrefs$bioconductorMirrorUrl <- list(
   get = function() { .rs.getUserPref("bioconductor_mirror_url") },
   set = function(value) { .rs.setUserPref("bioconductor_mirror_url", value) }
)

# Save R console history
# Whether to always save the R console history.
.rs.uiPrefs$alwaysSaveHistory <- list(
   get = function() { .rs.getUserPref("always_save_history") },
   set = function(value) { .rs.setUserPref("always_save_history", value) }
)

# Remove duplicates from console history
# Whether to remove duplicate entries from the R console history.
.rs.uiPrefs$removeHistoryDuplicates <- list(
   get = function() { .rs.getUserPref("remove_history_duplicates") },
   set = function(value) { .rs.setUserPref("remove_history_duplicates", value) }
)

# Show .Last.value in Environment pane
# Show the result of the last expression (.Last.value) in the Environment pane.
.rs.uiPrefs$showLastDotValue <- list(
   get = function() { .rs.getUserPref("show_last_dot_value") },
   set = function(value) { .rs.setUserPref("show_last_dot_value", value) }
)

# Line ending format
# The line ending format to use when saving files.
.rs.uiPrefs$lineEndingConversion <- list(
   get = function() { .rs.getUserPref("line_ending_conversion") },
   set = function(value) { .rs.setUserPref("line_ending_conversion", value) }
)

# Use newlines in Makefiles
# Whether to use newlines when saving Makefiles.
.rs.uiPrefs$useNewlinesInMakefiles <- list(
   get = function() { .rs.getUserPref("use_newlines_in_makefiles") },
   set = function(value) { .rs.setUserPref("use_newlines_in_makefiles", value) }
)

# 
# The terminal shell to use on Windows.
.rs.uiPrefs$windowsTerminalShell <- list(
   get = function() { .rs.getUserPref("windows_terminal_shell") },
   set = function(value) { .rs.setUserPref("windows_terminal_shell", value) }
)

# 
# The terminal shell to use on POSIX operating systems (MacOS and Linux).
.rs.uiPrefs$posixTerminalShell <- list(
   get = function() { .rs.getUserPref("posix_terminal_shell") },
   set = function(value) { .rs.setUserPref("posix_terminal_shell", value) }
)

# 
# The fully qualified path to the custom shell command to use in the Terminal
.rs.uiPrefs$customShellCommand <- list(
   get = function() { .rs.getUserPref("custom_shell_command") },
   set = function(value) { .rs.setUserPref("custom_shell_command", value) }
)

# 
# The command-line options to pass to the custom shell command.
.rs.uiPrefs$customShellOptions <- list(
   get = function() { .rs.getUserPref("custom_shell_options") },
   set = function(value) { .rs.setUserPref("custom_shell_options", value) }
)

# Show line numbers in editor
# Show line numbers in RStudio's code editor.
.rs.uiPrefs$showLineNumbers <- list(
   get = function() { .rs.getUserPref("show_line_numbers") },
   set = function(value) { .rs.setUserPref("show_line_numbers", value) }
)

# Use relative line numbers in editor
# Show relative, rather than absolute, line numbers in RStudio's code editor.
.rs.uiPrefs$relativeLineNumbers <- list(
   get = function() { .rs.getUserPref("relative_line_numbers") },
   set = function(value) { .rs.setUserPref("relative_line_numbers", value) }
)

# Highlight selected word in editor
# Highlight the selected word in RStudio's code editor.
.rs.uiPrefs$highlightSelectedWord <- list(
   get = function() { .rs.getUserPref("highlight_selected_word") },
   set = function(value) { .rs.setUserPref("highlight_selected_word", value) }
)

# Highlight selected line in editor
# Highlight the selected line in RStudio's code editor.
.rs.uiPrefs$highlightSelectedLine <- list(
   get = function() { .rs.getUserPref("highlight_selected_line") },
   set = function(value) { .rs.setUserPref("highlight_selected_line", value) }
)

# 
# Layout of panes in the RStudio workbench.
.rs.uiPrefs$panes <- list(
   get = function() { .rs.getUserPref("panes") },
   set = function(value) { .rs.setUserPref("panes", value) }
)

# Allow source columns
# Whether to enable the ability to add source columns to display.
.rs.uiPrefs$allowSourceColumns <- list(
   get = function() { .rs.getUserPref("allow_source_columns") },
   set = function(value) { .rs.setUserPref("allow_source_columns", value) }
)

# Insert spaces for Tab
# Whether to insert spaces when pressing the Tab key.
.rs.uiPrefs$useSpacesForTab <- list(
   get = function() { .rs.getUserPref("use_spaces_for_tab") },
   set = function(value) { .rs.setUserPref("use_spaces_for_tab", value) }
)

# Number of spaces for Tab
# The number of spaces to insert when pressing the Tab key.
.rs.uiPrefs$numSpacesForTab <- list(
   get = function() { .rs.getUserPref("num_spaces_for_tab") },
   set = function(value) { .rs.setUserPref("num_spaces_for_tab", value) }
)

# Auto-detect indentation in files
# Whether to automatically detect indentation settings from file contents.
.rs.uiPrefs$autoDetectIndentation <- list(
   get = function() { .rs.getUserPref("auto_detect_indentation") },
   set = function(value) { .rs.setUserPref("auto_detect_indentation", value) }
)

# Show margin in editor
# Whether to show the margin guide in the RStudio code editor.
.rs.uiPrefs$showMargin <- list(
   get = function() { .rs.getUserPref("show_margin") },
   set = function(value) { .rs.setUserPref("show_margin", value) }
)

# Use a blinking cursor
# Whether to flash the cursor off and on.
.rs.uiPrefs$blinkingCursor <- list(
   get = function() { .rs.getUserPref("blinking_cursor") },
   set = function(value) { .rs.setUserPref("blinking_cursor", value) }
)

# Margin column
# The number of columns of text after which the margin is shown.
.rs.uiPrefs$marginColumn <- list(
   get = function() { .rs.getUserPref("margin_column") },
   set = function(value) { .rs.setUserPref("margin_column", value) }
)

# Show invisible characters in editor
# Whether to show invisible characters, such as spaces and tabs, in the RStudio
.rs.uiPrefs$showInvisibles <- list(
   get = function() { .rs.getUserPref("show_invisibles") },
   set = function(value) { .rs.setUserPref("show_invisibles", value) }
)

# Indentation guides
# Style for indentation guides in the RStudio code editor.
.rs.uiPrefs$indentGuides <- list(
   get = function() { .rs.getUserPref("indent_guides") },
   set = function(value) { .rs.setUserPref("indent_guides", value) }
)

# Continue comments after adding new line
# Whether to continue comments (by inserting the comment character) after adding
.rs.uiPrefs$continueCommentsOnNewline <- list(
   get = function() { .rs.getUserPref("continue_comments_on_newline") },
   set = function(value) { .rs.setUserPref("continue_comments_on_newline", value) }
)

# Whether web links in comments are clickable
# Whether web links in comments are clickable.
.rs.uiPrefs$highlightWebLink <- list(
   get = function() { .rs.getUserPref("highlight_web_link") },
   set = function(value) { .rs.setUserPref("highlight_web_link", value) }
)

# Keybinding set for editor
# The keybindings to use in the RStudio code editor.
.rs.uiPrefs$editorKeybindings <- list(
   get = function() { .rs.getUserPref("editor_keybindings") },
   set = function(value) { .rs.setUserPref("editor_keybindings", value) }
)

# Auto-insert matching parentheses and brackets
# Whether to insert matching pairs, such as () and [], when the first is typed.
.rs.uiPrefs$insertMatching <- list(
   get = function() { .rs.getUserPref("insert_matching") },
   set = function(value) { .rs.setUserPref("insert_matching", value) }
)

# Insert spaces around = in R code
# Whether to insert spaces around the equals sign in R code.
.rs.uiPrefs$insertSpacesAroundEquals <- list(
   get = function() { .rs.getUserPref("insert_spaces_around_equals") },
   set = function(value) { .rs.setUserPref("insert_spaces_around_equals", value) }
)

# Insert parentheses after functions
# Whether to insert parentheses after function completions.
.rs.uiPrefs$insertParensAfterFunctionCompletion <- list(
   get = function() { .rs.getUserPref("insert_parens_after_function_completion") },
   set = function(value) { .rs.setUserPref("insert_parens_after_function_completion", value) }
)

# Complete multi-line statements with Tab
# Whether to attempt completion of multiple-line statements when pressing Tab.
.rs.uiPrefs$tabMultilineCompletion <- list(
   get = function() { .rs.getUserPref("tab_multiline_completion") },
   set = function(value) { .rs.setUserPref("tab_multiline_completion", value) }
)

# Use Tab to trigger autocompletion
# Whether to attempt completion of statements when pressing Tab.
.rs.uiPrefs$tabCompletion <- list(
   get = function() { .rs.getUserPref("tab_completion") },
   set = function(value) { .rs.setUserPref("tab_completion", value) }
)

# Show function help tooltips on idle
# Whether to show help tooltips for functions when the cursor has not been
.rs.uiPrefs$showHelpTooltipOnIdle <- list(
   get = function() { .rs.getUserPref("show_help_tooltip_on_idle") },
   set = function(value) { .rs.setUserPref("show_help_tooltip_on_idle", value) }
)

# Surround selections with
# Which kinds of delimiters can be used to surround the current selection.
.rs.uiPrefs$surroundSelection <- list(
   get = function() { .rs.getUserPref("surround_selection") },
   set = function(value) { .rs.setUserPref("surround_selection", value) }
)

# Enable code snippets
# Whether to enable code snippets in the RStudio code editor.
.rs.uiPrefs$enableSnippets <- list(
   get = function() { .rs.getUserPref("enable_snippets") },
   set = function(value) { .rs.setUserPref("enable_snippets", value) }
)

# Use code completion for R
# When to use auto-completion for R code in the RStudio code editor.
.rs.uiPrefs$codeCompletion <- list(
   get = function() { .rs.getUserPref("code_completion") },
   set = function(value) { .rs.setUserPref("code_completion", value) }
)

# Use code completion for other languages
# When to use auto-completion for other languages (such as JavaScript and SQL) in
.rs.uiPrefs$codeCompletionOther <- list(
   get = function() { .rs.getUserPref("code_completion_other") },
   set = function(value) { .rs.setUserPref("code_completion_other", value) }
)

# Use code completion in the R console
# Whether to always use code completion in the R console.
.rs.uiPrefs$consoleCodeCompletion <- list(
   get = function() { .rs.getUserPref("console_code_completion") },
   set = function(value) { .rs.setUserPref("console_code_completion", value) }
)

# Delay before completing code (ms)
# The number of milliseconds to wait before offering code suggestions.
.rs.uiPrefs$codeCompletionDelay <- list(
   get = function() { .rs.getUserPref("code_completion_delay") },
   set = function(value) { .rs.setUserPref("code_completion_delay", value) }
)

# Number of characters for code completion
# The number of characters in a symbol that can be entered before completions are
.rs.uiPrefs$codeCompletionCharacters <- list(
   get = function() { .rs.getUserPref("code_completion_characters") },
   set = function(value) { .rs.setUserPref("code_completion_characters", value) }
)

# Include all function arguments in completion list
# When set, RStudio will include all function arguments in the completion list,
.rs.uiPrefs$codeCompletionIncludeAlreadyUsed <- list(
   get = function() { .rs.getUserPref("code_completion_include_already_used") },
   set = function(value) { .rs.setUserPref("code_completion_include_already_used", value) }
)

# Show function signature tooltips
# Whether to show function signature tooltips during autocompletion.
.rs.uiPrefs$showFunctionSignatureTooltips <- list(
   get = function() { .rs.getUserPref("show_function_signature_tooltips") },
   set = function(value) { .rs.setUserPref("show_function_signature_tooltips", value) }
)

# Show data preview in autocompletion help popup
# Whether a data preview is shown in the autocompletion help popup for datasets
.rs.uiPrefs$showDataPreview <- list(
   get = function() { .rs.getUserPref("show_data_preview") },
   set = function(value) { .rs.setUserPref("show_data_preview", value) }
)

# Show diagnostics in R code
# Whether to show diagnostic messages (such as syntax and usage errors) for R
.rs.uiPrefs$showDiagnosticsR <- list(
   get = function() { .rs.getUserPref("show_diagnostics_r") },
   set = function(value) { .rs.setUserPref("show_diagnostics_r", value) }
)

# Show diagnostics in C++ code
# Whether to show diagnostic messages for C++ code as you type.
.rs.uiPrefs$showDiagnosticsCpp <- list(
   get = function() { .rs.getUserPref("show_diagnostics_cpp") },
   set = function(value) { .rs.setUserPref("show_diagnostics_cpp", value) }
)

# Show diagnostics in YAML code
# Whether to show diagnostic messages for YAML code as you type.
.rs.uiPrefs$showDiagnosticsYaml <- list(
   get = function() { .rs.getUserPref("show_diagnostics_yaml") },
   set = function(value) { .rs.setUserPref("show_diagnostics_yaml", value) }
)

# Show diagnostics in other languages
# Whether to show diagnostic messages for other types of code (not R, C++, or
.rs.uiPrefs$showDiagnosticsOther <- list(
   get = function() { .rs.getUserPref("show_diagnostics_other") },
   set = function(value) { .rs.setUserPref("show_diagnostics_other", value) }
)

# Show style diagnostics for R code
# Whether to show style diagnostics (suggestions for improving R code style)
.rs.uiPrefs$styleDiagnostics <- list(
   get = function() { .rs.getUserPref("style_diagnostics") },
   set = function(value) { .rs.setUserPref("style_diagnostics", value) }
)

# Check code for problems when saving
# Whether to check code for problems after saving it.
.rs.uiPrefs$diagnosticsOnSave <- list(
   get = function() { .rs.getUserPref("diagnostics_on_save") },
   set = function(value) { .rs.setUserPref("diagnostics_on_save", value) }
)

# Run R code diagnostics in the background
# Whether to run code diagnostics in the background, as you type.
.rs.uiPrefs$backgroundDiagnostics <- list(
   get = function() { .rs.getUserPref("background_diagnostics") },
   set = function(value) { .rs.setUserPref("background_diagnostics", value) }
)

# Run R code diagnostics after (ms)
# The number of milliseconds to delay before running code diagnostics in the
.rs.uiPrefs$backgroundDiagnosticsDelayMs <- list(
   get = function() { .rs.getUserPref("background_diagnostics_delay_ms") },
   set = function(value) { .rs.setUserPref("background_diagnostics_delay_ms", value) }
)

# Run diagnostics on R function calls
# Whether to run diagnostics in R function calls.
.rs.uiPrefs$diagnosticsInRFunctionCalls <- list(
   get = function() { .rs.getUserPref("diagnostics_in_r_function_calls") },
   set = function(value) { .rs.setUserPref("diagnostics_in_r_function_calls", value) }
)

# Check arguments to R function calls
# Whether to check arguments to R function calls.
.rs.uiPrefs$checkArgumentsToRFunctionCalls <- list(
   get = function() { .rs.getUserPref("check_arguments_to_r_function_calls") },
   set = function(value) { .rs.setUserPref("check_arguments_to_r_function_calls", value) }
)

# Check for unexpected assignments
# Whether to check for unexpected variable assignments inside R function calls.
.rs.uiPrefs$checkUnexpectedAssignmentInFunctionCall <- list(
   get = function() { .rs.getUserPref("check_unexpected_assignment_in_function_call") },
   set = function(value) { .rs.setUserPref("check_unexpected_assignment_in_function_call", value) }
)

# Warn when R variable used but not defined
# Whether to generate a warning if a variable is used without being defined in
.rs.uiPrefs$warnIfNoSuchVariableInScope <- list(
   get = function() { .rs.getUserPref("warn_if_no_such_variable_in_scope") },
   set = function(value) { .rs.setUserPref("warn_if_no_such_variable_in_scope", value) }
)

# Warn when R variable defined but not used
# Whether to generate a warning if a variable is defined without being used in
.rs.uiPrefs$warnVariableDefinedButNotUsed <- list(
   get = function() { .rs.getUserPref("warn_variable_defined_but_not_used") },
   set = function(value) { .rs.setUserPref("warn_variable_defined_but_not_used", value) }
)

# Detect missing R packages in the editor
# Whether to automatically discover and offer to install missing R package
.rs.uiPrefs$autoDiscoverPackageDependencies <- list(
   get = function() { .rs.getUserPref("auto_discover_package_dependencies") },
   set = function(value) { .rs.setUserPref("auto_discover_package_dependencies", value) }
)

# Ensure files end with a newline when saving
# Whether to ensure that source files end with a newline character.
.rs.uiPrefs$autoAppendNewline <- list(
   get = function() { .rs.getUserPref("auto_append_newline") },
   set = function(value) { .rs.setUserPref("auto_append_newline", value) }
)

# Strip trailing whitespace when saving
# Whether to strip trailing whitespace from each line when saving.
.rs.uiPrefs$stripTrailingWhitespace <- list(
   get = function() { .rs.getUserPref("strip_trailing_whitespace") },
   set = function(value) { .rs.setUserPref("strip_trailing_whitespace", value) }
)

# Restore cursor position when reopening files
# Whether to save the position of the cursor when a file is closed, restore it
.rs.uiPrefs$restoreSourceDocumentCursorPosition <- list(
   get = function() { .rs.getUserPref("restore_source_document_cursor_position") },
   set = function(value) { .rs.setUserPref("restore_source_document_cursor_position", value) }
)

# Re-indent code when pasting
# Whether to automatically re-indent code when it's pasted into RStudio.
.rs.uiPrefs$reindentOnPaste <- list(
   get = function() { .rs.getUserPref("reindent_on_paste") },
   set = function(value) { .rs.setUserPref("reindent_on_paste", value) }
)

# Vertically align function arguments
# Whether to vertically align arguments to R function calls during automatic
.rs.uiPrefs$verticallyAlignArgumentsIndent <- list(
   get = function() { .rs.getUserPref("vertically_align_arguments_indent") },
   set = function(value) { .rs.setUserPref("vertically_align_arguments_indent", value) }
)

# Soft-wrap source files
# Whether to soft-wrap source files, wrapping the text for display without
.rs.uiPrefs$softWrapRFiles <- list(
   get = function() { .rs.getUserPref("soft_wrap_r_files") },
   set = function(value) { .rs.setUserPref("soft_wrap_r_files", value) }
)

# Soft-wrap R Markdown files
# Whether to soft-wrap R Markdown files (and similar types such as R HTML and R
.rs.uiPrefs$softWrapRmdFiles <- list(
   get = function() { .rs.getUserPref("soft_wrap_rmd_files") },
   set = function(value) { .rs.setUserPref("soft_wrap_rmd_files", value) }
)

# Focus console after executing R code
# Whether to focus the R console after executing an R command from a script.
.rs.uiPrefs$focusConsoleAfterExec <- list(
   get = function() { .rs.getUserPref("focus_console_after_exec") },
   set = function(value) { .rs.setUserPref("focus_console_after_exec", value) }
)

# Fold style in editor
# The style of folding to use.
.rs.uiPrefs$foldStyle <- list(
   get = function() { .rs.getUserPref("fold_style") },
   set = function(value) { .rs.setUserPref("fold_style", value) }
)

# Save R scripts before sourcing
# Whether to automatically save scripts before executing them.
.rs.uiPrefs$saveBeforeSourcing <- list(
   get = function() { .rs.getUserPref("save_before_sourcing") },
   set = function(value) { .rs.setUserPref("save_before_sourcing", value) }
)

# Syntax highlighting in R console
# Whether to use syntax highlighting in the R console.
.rs.uiPrefs$syntaxColorConsole <- list(
   get = function() { .rs.getUserPref("syntax_color_console") },
   set = function(value) { .rs.setUserPref("syntax_color_console", value) }
)

# Different color for error output in R console
# Whether to display error, warning, and message output in a different color.
.rs.uiPrefs$highlightConsoleErrors <- list(
   get = function() { .rs.getUserPref("highlight_console_errors") },
   set = function(value) { .rs.setUserPref("highlight_console_errors", value) }
)

# Scroll past end of file
# Whether to allow scrolling past the end of a file.
.rs.uiPrefs$scrollPastEndOfDocument <- list(
   get = function() { .rs.getUserPref("scroll_past_end_of_document") },
   set = function(value) { .rs.setUserPref("scroll_past_end_of_document", value) }
)

# Highlight R function calls
# Whether to highlight R function calls in the code editor.
.rs.uiPrefs$highlightRFunctionCalls <- list(
   get = function() { .rs.getUserPref("highlight_r_function_calls") },
   set = function(value) { .rs.setUserPref("highlight_r_function_calls", value) }
)

# Enable preview of named and hexadecimal colors
# Whether to show preview for named and hexadecimal colors.
.rs.uiPrefs$colorPreview <- list(
   get = function() { .rs.getUserPref("color_preview") },
   set = function(value) { .rs.setUserPref("color_preview", value) }
)

# Use rainbow parentheses
# Whether to highlight parentheses in a variety of colors.
.rs.uiPrefs$rainbowParentheses <- list(
   get = function() { .rs.getUserPref("rainbow_parentheses") },
   set = function(value) { .rs.setUserPref("rainbow_parentheses", value) }
)

# Use rainbow fenced divs
# Whether to highlight fenced divs in a variety of colors.
.rs.uiPrefs$rainbowFencedDivs <- list(
   get = function() { .rs.getUserPref("rainbow_fenced_divs") },
   set = function(value) { .rs.setUserPref("rainbow_fenced_divs", value) }
)

# Maximum characters per line in R console
# The maximum number of characters to display in a single line in the R console.
.rs.uiPrefs$consoleLineLengthLimit <- list(
   get = function() { .rs.getUserPref("console_line_length_limit") },
   set = function(value) { .rs.setUserPref("console_line_length_limit", value) }
)

# Maximum lines in R console
# The maximum number of console actions to store and display in the console
.rs.uiPrefs$consoleMaxLines <- list(
   get = function() { .rs.getUserPref("console_max_lines") },
   set = function(value) { .rs.setUserPref("console_max_lines", value) }
)

# ANSI escape codes in R console
# How to treat ANSI escape codes in the console.
.rs.uiPrefs$ansiConsoleMode <- list(
   get = function() { .rs.getUserPref("ansi_console_mode") },
   set = function(value) { .rs.setUserPref("ansi_console_mode", value) }
)

# Limit visible console output
# Whether to only show a limited window of the total console output
.rs.uiPrefs$limitVisibleConsole <- list(
   get = function() { .rs.getUserPref("limit_visible_console") },
   set = function(value) { .rs.setUserPref("limit_visible_console", value) }
)

# Show toolbar on R Markdown chunks
# Whether to show a toolbar on code chunks in R Markdown documents.
.rs.uiPrefs$showInlineToolbarForRCodeChunks <- list(
   get = function() { .rs.getUserPref("show_inline_toolbar_for_r_code_chunks") },
   set = function(value) { .rs.setUserPref("show_inline_toolbar_for_r_code_chunks", value) }
)

# Highlight code chunks in R Markdown files
# Whether to highlight code chunks in R Markdown documents with a different
.rs.uiPrefs$highlightCodeChunks <- list(
   get = function() { .rs.getUserPref("highlight_code_chunks") },
   set = function(value) { .rs.setUserPref("highlight_code_chunks", value) }
)

# Save files before building
# Whether to save all open, unsaved files before building the project.
.rs.uiPrefs$saveFilesBeforeBuild <- list(
   get = function() { .rs.getUserPref("save_files_before_build") },
   set = function(value) { .rs.setUserPref("save_files_before_build", value) }
)

# Save and reload R workspace on build
# Whether RStudio should save and reload the R workspace when building the
.rs.uiPrefs$saveAndReloadWorkspaceOnBuild <- list(
   get = function() { .rs.getUserPref("save_and_reload_workspace_on_build") },
   set = function(value) { .rs.setUserPref("save_and_reload_workspace_on_build", value) }
)

# Editor font size (points)
# The default editor font size, in points.
.rs.uiPrefs$fontSizePoints <- list(
   get = function() { .rs.getUserPref("font_size_points") },
   set = function(value) { .rs.setUserPref("font_size_points", value) }
)

# Help panel font size (points)
# The help panel font size, in points.
.rs.uiPrefs$helpFontSizePoints <- list(
   get = function() { .rs.getUserPref("help_font_size_points") },
   set = function(value) { .rs.setUserPref("help_font_size_points", value) }
)

# Theme
# The name of the color theme to apply to the text editor in RStudio.
.rs.uiPrefs$editorTheme <- list(
   get = function() { .rs.getUserPref("editor_theme") },
   set = function(value) { .rs.setUserPref("editor_theme", value) }
)

# Enable editor fonts on RStudio Server
# Whether to use a custom editor font in RStudio Server.
.rs.uiPrefs$serverEditorFontEnabled <- list(
   get = function() { .rs.getUserPref("server_editor_font_enabled") },
   set = function(value) { .rs.setUserPref("server_editor_font_enabled", value) }
)

# Editor font
# The name of the fixed-width editor font to use with RStudio Server.
.rs.uiPrefs$serverEditorFont <- list(
   get = function() { .rs.getUserPref("server_editor_font") },
   set = function(value) { .rs.setUserPref("server_editor_font", value) }
)

# Default character encoding
# The default character encoding to use when saving files.
.rs.uiPrefs$defaultEncoding <- list(
   get = function() { .rs.getUserPref("default_encoding") },
   set = function(value) { .rs.setUserPref("default_encoding", value) }
)

# Show top toolbar
# Whether to show the toolbar at the top of the RStudio workbench.
.rs.uiPrefs$toolbarVisible <- list(
   get = function() { .rs.getUserPref("toolbar_visible") },
   set = function(value) { .rs.setUserPref("toolbar_visible", value) }
)

# Default new project location
# The directory path under which to place new projects by default.
.rs.uiPrefs$defaultProjectLocation <- list(
   get = function() { .rs.getUserPref("default_project_location") },
   set = function(value) { .rs.setUserPref("default_project_location", value) }
)

# Default open project location
# The default directory to use in file dialogs when opening a project.
.rs.uiPrefs$defaultOpenProjectLocation <- list(
   get = function() { .rs.getUserPref("default_open_project_location") },
   set = function(value) { .rs.setUserPref("default_open_project_location", value) }
)

# Source with echo by default
# Whether to echo R code when sourcing it.
.rs.uiPrefs$sourceWithEcho <- list(
   get = function() { .rs.getUserPref("source_with_echo") },
   set = function(value) { .rs.setUserPref("source_with_echo", value) }
)

# Default Sweave engine
# The default engine to use when processing Sweave documents.
.rs.uiPrefs$defaultSweaveEngine <- list(
   get = function() { .rs.getUserPref("default_sweave_engine") },
   set = function(value) { .rs.setUserPref("default_sweave_engine", value) }
)

# Default LaTeX program
# The default program to use when processing LaTeX documents.
.rs.uiPrefs$defaultLatexProgram <- list(
   get = function() { .rs.getUserPref("default_latex_program") },
   set = function(value) { .rs.setUserPref("default_latex_program", value) }
)

# Use Roxygen for documentation
# Whether to use Roxygen for documentation.
.rs.uiPrefs$useRoxygen <- list(
   get = function() { .rs.getUserPref("use_roxygen") },
   set = function(value) { .rs.setUserPref("use_roxygen", value) }
)

# Enable data import
# Whether to use RStudio's data import feature.
.rs.uiPrefs$useDataimport <- list(
   get = function() { .rs.getUserPref("use_dataimport") },
   set = function(value) { .rs.setUserPref("use_dataimport", value) }
)

# PDF previewer
# The program to use to preview PDF files after generation.
.rs.uiPrefs$pdfPreviewer <- list(
   get = function() { .rs.getUserPref("pdf_previewer") },
   set = function(value) { .rs.setUserPref("pdf_previewer", value) }
)

# Enable Rnw concordance
# Whether to always enable the concordance for RNW files.
.rs.uiPrefs$alwaysEnableRnwConcordance <- list(
   get = function() { .rs.getUserPref("always_enable_rnw_concordance") },
   set = function(value) { .rs.setUserPref("always_enable_rnw_concordance", value) }
)

# Insert numbered LaTeX sections
# Whether to insert numbered sections in LaTeX.
.rs.uiPrefs$insertNumberedLatexSections <- list(
   get = function() { .rs.getUserPref("insert_numbered_latex_sections") },
   set = function(value) { .rs.setUserPref("insert_numbered_latex_sections", value) }
)

# Spelling dictionary language
# The language of the spelling dictionary to use for spell checking.
.rs.uiPrefs$spellingDictionaryLanguage <- list(
   get = function() { .rs.getUserPref("spelling_dictionary_language") },
   set = function(value) { .rs.setUserPref("spelling_dictionary_language", value) }
)

# Custom spelling dictionaries
# The list of custom dictionaries to use when spell checking.
.rs.uiPrefs$spellingCustomDictionaries <- list(
   get = function() { .rs.getUserPref("spelling_custom_dictionaries") },
   set = function(value) { .rs.setUserPref("spelling_custom_dictionaries", value) }
)

# Lint document after load (ms)
# The number of milliseconds to wait before linting a document after it is
.rs.uiPrefs$documentLoadLintDelay <- list(
   get = function() { .rs.getUserPref("document_load_lint_delay") },
   set = function(value) { .rs.setUserPref("document_load_lint_delay", value) }
)

# Ignore uppercase words in spell check
# Whether to ignore words in uppercase when spell checking.
.rs.uiPrefs$ignoreUppercaseWords <- list(
   get = function() { .rs.getUserPref("ignore_uppercase_words") },
   set = function(value) { .rs.setUserPref("ignore_uppercase_words", value) }
)

# Ignore words with numbers in spell check
# Whether to ignore words with numbers in them when spell checking.
.rs.uiPrefs$ignoreWordsWithNumbers <- list(
   get = function() { .rs.getUserPref("ignore_words_with_numbers") },
   set = function(value) { .rs.setUserPref("ignore_words_with_numbers", value) }
)

# Use real-time spellchecking
# Whether to enable real-time spellchecking by default.
.rs.uiPrefs$realTimeSpellchecking <- list(
   get = function() { .rs.getUserPref("real_time_spellchecking") },
   set = function(value) { .rs.setUserPref("real_time_spellchecking", value) }
)

# Navigate to build errors
# Whether to navigate to build errors.
.rs.uiPrefs$navigateToBuildError <- list(
   get = function() { .rs.getUserPref("navigate_to_build_error") },
   set = function(value) { .rs.setUserPref("navigate_to_build_error", value) }
)

# Enable the Packages pane
# Whether to enable RStudio's Packages pane.
.rs.uiPrefs$packagesPaneEnabled <- list(
   get = function() { .rs.getUserPref("packages_pane_enabled") },
   set = function(value) { .rs.setUserPref("packages_pane_enabled", value) }
)

# C++ template
# C++ template.
.rs.uiPrefs$cppTemplate <- list(
   get = function() { .rs.getUserPref("cpp_template") },
   set = function(value) { .rs.setUserPref("cpp_template", value) }
)

# Restore last opened documents on startup
# Whether to restore the last opened source documents when RStudio starts up.
.rs.uiPrefs$restoreSourceDocuments <- list(
   get = function() { .rs.getUserPref("restore_source_documents") },
   set = function(value) { .rs.setUserPref("restore_source_documents", value) }
)

# Handle errors only when user code present
# Whether to handle errors only when user code is on the stack.
.rs.uiPrefs$handleErrorsInUserCodeOnly <- list(
   get = function() { .rs.getUserPref("handle_errors_in_user_code_only") },
   set = function(value) { .rs.setUserPref("handle_errors_in_user_code_only", value) }
)

# Auto-expand error tracebacks
# Whether to automatically expand tracebacks when an error occurs.
.rs.uiPrefs$autoExpandErrorTracebacks <- list(
   get = function() { .rs.getUserPref("auto_expand_error_tracebacks") },
   set = function(value) { .rs.setUserPref("auto_expand_error_tracebacks", value) }
)

# Check for new version at startup
# Whether to check for new versions of RStudio when RStudio starts.
.rs.uiPrefs$checkForUpdates <- list(
   get = function() { .rs.getUserPref("check_for_updates") },
   set = function(value) { .rs.setUserPref("check_for_updates", value) }
)

# Show internal functions when debugging
# Whether to show functions without source references in the Traceback pane while
.rs.uiPrefs$showInternalFunctions <- list(
   get = function() { .rs.getUserPref("show_internal_functions") },
   set = function(value) { .rs.setUserPref("show_internal_functions", value) }
)

# Run Shiny applications in
# Where to display Shiny applications when they are run.
.rs.uiPrefs$shinyViewerType <- list(
   get = function() { .rs.getUserPref("shiny_viewer_type") },
   set = function(value) { .rs.setUserPref("shiny_viewer_type", value) }
)

# Run Shiny applications in the background
# Whether to run Shiny applications as background jobs.
.rs.uiPrefs$shinyBackgroundJobs <- list(
   get = function() { .rs.getUserPref("shiny_background_jobs") },
   set = function(value) { .rs.setUserPref("shiny_background_jobs", value) }
)

# Run Plumber APIs in
# Where to display Shiny applications when they are run.
.rs.uiPrefs$plumberViewerType <- list(
   get = function() { .rs.getUserPref("plumber_viewer_type") },
   set = function(value) { .rs.setUserPref("plumber_viewer_type", value) }
)

# Document author
# The default name to use as the document author when creating new documents.
.rs.uiPrefs$documentAuthor <- list(
   get = function() { .rs.getUserPref("document_author") },
   set = function(value) { .rs.setUserPref("document_author", value) }
)

# Use current date when rendering document
# Use current date when rendering document
.rs.uiPrefs$rmdAutoDate <- list(
   get = function() { .rs.getUserPref("rmd_auto_date") },
   set = function(value) { .rs.setUserPref("rmd_auto_date", value) }
)

# Path to preferred R Markdown template
# The path to the preferred R Markdown template.
.rs.uiPrefs$rmdPreferredTemplatePath <- list(
   get = function() { .rs.getUserPref("rmd_preferred_template_path") },
   set = function(value) { .rs.setUserPref("rmd_preferred_template_path", value) }
)

# Display R Markdown documents in
# Where to display R Markdown documents when they have completed rendering.
.rs.uiPrefs$rmdViewerType <- list(
   get = function() { .rs.getUserPref("rmd_viewer_type") },
   set = function(value) { .rs.setUserPref("rmd_viewer_type", value) }
)

# Show diagnostic info when publishing
# Whether to show verbose diagnostic information when publishing content.
.rs.uiPrefs$showPublishDiagnostics <- list(
   get = function() { .rs.getUserPref("show_publish_diagnostics") },
   set = function(value) { .rs.setUserPref("show_publish_diagnostics", value) }
)

# 
# Whether to show UI for publishing content to Posit Cloud.
.rs.uiPrefs$enableCloudPublishUi <- list(
   get = function() { .rs.getUserPref("enable_cloud_publish_ui") },
   set = function(value) { .rs.setUserPref("enable_cloud_publish_ui", value) }
)

# Check SSL certificates when publishing
# Whether to check remote server SSL certificates when publishing content.
.rs.uiPrefs$publishCheckCertificates <- list(
   get = function() { .rs.getUserPref("publish_check_certificates") },
   set = function(value) { .rs.setUserPref("publish_check_certificates", value) }
)

# Use custom CA bundle when publishing
# Whether to use a custom certificate authority (CA) bundle when publishing
.rs.uiPrefs$usePublishCaBundle <- list(
   get = function() { .rs.getUserPref("use_publish_ca_bundle") },
   set = function(value) { .rs.setUserPref("use_publish_ca_bundle", value) }
)

# Path to custom CA bundle for publishing
# The path to the custom certificate authority (CA) bundle to use when publishing
.rs.uiPrefs$publishCaBundle <- list(
   get = function() { .rs.getUserPref("publish_ca_bundle") },
   set = function(value) { .rs.setUserPref("publish_ca_bundle", value) }
)

# Show chunk output inline in all documents
# Whether to show chunk output inline for ordinary R Markdown documents.
.rs.uiPrefs$rmdChunkOutputInline <- list(
   get = function() { .rs.getUserPref("rmd_chunk_output_inline") },
   set = function(value) { .rs.setUserPref("rmd_chunk_output_inline", value) }
)

# Open document outline by default
# Whether to show the document outline by default when opening R Markdown
.rs.uiPrefs$showDocOutlineRmd <- list(
   get = function() { .rs.getUserPref("show_doc_outline_rmd") },
   set = function(value) { .rs.setUserPref("show_doc_outline_rmd", value) }
)

# Document outline font size
# The font size to use for items in the document outline.
.rs.uiPrefs$documentOutlineFontSize <- list(
   get = function() { .rs.getUserPref("document_outline_font_size") },
   set = function(value) { .rs.setUserPref("document_outline_font_size", value) }
)

# Automatically run Setup chunk when needed
# Whether to automatically run an R Markdown document's Setup chunk before
.rs.uiPrefs$autoRunSetupChunk <- list(
   get = function() { .rs.getUserPref("auto_run_setup_chunk") },
   set = function(value) { .rs.setUserPref("auto_run_setup_chunk", value) }
)

# Hide console when running R Markdown chunks
# Whether to hide the R console when executing inline R Markdown chunks.
.rs.uiPrefs$hideConsoleOnChunkExecute <- list(
   get = function() { .rs.getUserPref("hide_console_on_chunk_execute") },
   set = function(value) { .rs.setUserPref("hide_console_on_chunk_execute", value) }
)

# Unit of R code execution
# The unit of R code to execute when the Execute command is invoked.
.rs.uiPrefs$executionBehavior <- list(
   get = function() { .rs.getUserPref("execution_behavior") },
   set = function(value) { .rs.setUserPref("execution_behavior", value) }
)

# Show the Terminal tab
# Whether to show the Terminal tab.
.rs.uiPrefs$showTerminalTab <- list(
   get = function() { .rs.getUserPref("show_terminal_tab") },
   set = function(value) { .rs.setUserPref("show_terminal_tab", value) }
)

# Use local echo in the Terminal
# Whether to use local echo in the Terminal.
.rs.uiPrefs$terminalLocalEcho <- list(
   get = function() { .rs.getUserPref("terminal_local_echo") },
   set = function(value) { .rs.setUserPref("terminal_local_echo", value) }
)

# Use websockets in the Terminal
# Whether to use websockets to communicate with the shell in the Terminal tab.
.rs.uiPrefs$terminalWebsockets <- list(
   get = function() { .rs.getUserPref("terminal_websockets") },
   set = function(value) { .rs.setUserPref("terminal_websockets", value) }
)

# Close Terminal pane after shell exit
# Whether to close the terminal pane after the shell exits.
.rs.uiPrefs$terminalCloseBehavior <- list(
   get = function() { .rs.getUserPref("terminal_close_behavior") },
   set = function(value) { .rs.setUserPref("terminal_close_behavior", value) }
)

# Save and restore system environment in Terminal tab
# Whether to track and save changes to system environment variables in the
.rs.uiPrefs$terminalTrackEnvironment <- list(
   get = function() { .rs.getUserPref("terminal_track_environment") },
   set = function(value) { .rs.setUserPref("terminal_track_environment", value) }
)

# Ignored environment variables
# Environment variables which should be ignored when tracking changed to
.rs.uiPrefs$terminalIgnoredEnvironmentVariables <- list(
   get = function() { .rs.getUserPref("terminal_ignored_environment_variables") },
   set = function(value) { .rs.setUserPref("terminal_ignored_environment_variables", value) }
)

# Enable Terminal hooks
# Enabled Terminal hooks? Required for Python terminal integration, which places
.rs.uiPrefs$terminalHooks <- list(
   get = function() { .rs.getUserPref("terminal_hooks") },
   set = function(value) { .rs.setUserPref("terminal_hooks", value) }
)

# Terminal bell style
# Terminal bell style
.rs.uiPrefs$terminalBellStyle <- list(
   get = function() { .rs.getUserPref("terminal_bell_style") },
   set = function(value) { .rs.setUserPref("terminal_bell_style", value) }
)

# Terminal tab rendering engine
# Terminal rendering engine: canvas is faster, dom may be needed for some
.rs.uiPrefs$terminalRenderer <- list(
   get = function() { .rs.getUserPref("terminal_renderer") },
   set = function(value) { .rs.setUserPref("terminal_renderer", value) }
)

# Make links in Terminal clickable
# Whether web links displayed in the Terminal tab are made clickable.
.rs.uiPrefs$terminalWeblinks <- list(
   get = function() { .rs.getUserPref("terminal_weblinks") },
   set = function(value) { .rs.setUserPref("terminal_weblinks", value) }
)

# Show R Markdown render command
# Whether to print the render command use to knit R Markdown documents in the R
.rs.uiPrefs$showRmdRenderCommand <- list(
   get = function() { .rs.getUserPref("show_rmd_render_command") },
   set = function(value) { .rs.setUserPref("show_rmd_render_command", value) }
)

# Enable dragging text in code editor
# Whether to enable moving text on the editing surface by clicking and dragging
.rs.uiPrefs$enableTextDrag <- list(
   get = function() { .rs.getUserPref("enable_text_drag") },
   set = function(value) { .rs.setUserPref("enable_text_drag", value) }
)

# Show hidden files in Files pane
# Whether to show hidden files in the Files pane.
.rs.uiPrefs$showHiddenFiles <- list(
   get = function() { .rs.getUserPref("show_hidden_files") },
   set = function(value) { .rs.setUserPref("show_hidden_files", value) }
)

# Files always shown in the Files Pane
# List of file names (case sensitive) that are always shown in the Files Pane,
.rs.uiPrefs$alwaysShownFiles <- list(
   get = function() { .rs.getUserPref("always_shown_files") },
   set = function(value) { .rs.setUserPref("always_shown_files", value) }
)

# Extensions always shown in the Files Pane
# List of file extensions (beginning with ., not case sensitive) that are always
.rs.uiPrefs$alwaysShownExtensions <- list(
   get = function() { .rs.getUserPref("always_shown_extensions") },
   set = function(value) { .rs.setUserPref("always_shown_extensions", value) }
)

# Sort file names naturally in Files pane
# Whether to sort file names naturally, so that e.g., file10.R comes after
.rs.uiPrefs$sortFileNamesNaturally <- list(
   get = function() { .rs.getUserPref("sort_file_names_naturally") },
   set = function(value) { .rs.setUserPref("sort_file_names_naturally", value) }
)

# Synchronize the Files pane with the current working directory
# Whether to change the directory in the Files pane automatically when the
.rs.uiPrefs$syncFilesPaneWorkingDir <- list(
   get = function() { .rs.getUserPref("sync_files_pane_working_dir") },
   set = function(value) { .rs.setUserPref("sync_files_pane_working_dir", value) }
)

# Jobs tab visibility
# The visibility of the Jobs tab.
.rs.uiPrefs$jobsTabVisibility <- list(
   get = function() { .rs.getUserPref("jobs_tab_visibility") },
   set = function(value) { .rs.setUserPref("jobs_tab_visibility", value) }
)

# 
# Whether to show the Workbench Jobs tab in RStudio Pro and RStudio Workbench.
.rs.uiPrefs$showLauncherJobsTab <- list(
   get = function() { .rs.getUserPref("show_launcher_jobs_tab") },
   set = function(value) { .rs.setUserPref("show_launcher_jobs_tab", value) }
)

# 
# How to sort jobs in the Workbench Jobs tab in RStudio Pro and RStudio
.rs.uiPrefs$launcherJobsSort <- list(
   get = function() { .rs.getUserPref("launcher_jobs_sort") },
   set = function(value) { .rs.setUserPref("launcher_jobs_sort", value) }
)

# 
# How to detect busy status in the Terminal.
.rs.uiPrefs$busyDetection <- list(
   get = function() { .rs.getUserPref("busy_detection") },
   set = function(value) { .rs.setUserPref("busy_detection", value) }
)

# 
# A list of apps that should not be considered busy in the Terminal.
.rs.uiPrefs$busyExclusionList <- list(
   get = function() { .rs.getUserPref("busy_exclusion_list") },
   set = function(value) { .rs.setUserPref("busy_exclusion_list", value) }
)

# Working directory for knitting
# The working directory to use when knitting R Markdown documents.
.rs.uiPrefs$knitWorkingDir <- list(
   get = function() { .rs.getUserPref("knit_working_dir") },
   set = function(value) { .rs.setUserPref("knit_working_dir", value) }
)

# Show in Document Outline
# Which objects to show in the document outline pane.
.rs.uiPrefs$docOutlineShow <- list(
   get = function() { .rs.getUserPref("doc_outline_show") },
   set = function(value) { .rs.setUserPref("doc_outline_show", value) }
)

# Preview LaTeX equations on idle
# When to preview LaTeX mathematical equations when cursor has not moved
.rs.uiPrefs$latexPreviewOnCursorIdle <- list(
   get = function() { .rs.getUserPref("latex_preview_on_cursor_idle") },
   set = function(value) { .rs.setUserPref("latex_preview_on_cursor_idle", value) }
)

# Wrap around when going to previous/next tab
# Whether to wrap around when going to the previous or next editor tab.
.rs.uiPrefs$wrapTabNavigation <- list(
   get = function() { .rs.getUserPref("wrap_tab_navigation") },
   set = function(value) { .rs.setUserPref("wrap_tab_navigation", value) }
)

# Global theme
# The theme to use for the main RStudio user interface.
.rs.uiPrefs$globalTheme <- list(
   get = function() { .rs.getUserPref("global_theme") },
   set = function(value) { .rs.setUserPref("global_theme", value) }
)

# Ignore whitespace in VCS diffs
# Whether to ignore whitespace when generating diffs of version controlled files.
.rs.uiPrefs$gitDiffIgnoreWhitespace <- list(
   get = function() { .rs.getUserPref("git_diff_ignore_whitespace") },
   set = function(value) { .rs.setUserPref("git_diff_ignore_whitespace", value) }
)

# Sign git commits
# Whether to sign git commits.
.rs.uiPrefs$gitSignedCommits <- list(
   get = function() { .rs.getUserPref("git_signed_commits") },
   set = function(value) { .rs.setUserPref("git_signed_commits", value) }
)

# Double click to select in the Console
# Whether double-clicking should select a word in the Console pane.
.rs.uiPrefs$consoleDoubleClickSelect <- list(
   get = function() { .rs.getUserPref("console_double_click_select") },
   set = function(value) { .rs.setUserPref("console_double_click_select", value) }
)

# Warn when automatic session suspension is paused
# Whether the 'Auto Suspension Blocked' icon should appear in the R Console
.rs.uiPrefs$consoleSuspendBlockedNotice <- list(
   get = function() { .rs.getUserPref("console_suspend_blocked_notice") },
   set = function(value) { .rs.setUserPref("console_suspend_blocked_notice", value) }
)

# Number of seconds to delay warning
# How long to wait before warning that automatic session suspension has been
.rs.uiPrefs$consoleSuspendBlockedNoticeDelay <- list(
   get = function() { .rs.getUserPref("console_suspend_blocked_notice_delay") },
   set = function(value) { .rs.setUserPref("console_suspend_blocked_notice_delay", value) }
)

# Create a Git repo in new projects
# Whether a git repo should be initialized inside new projects by default.
.rs.uiPrefs$newProjGitInit <- list(
   get = function() { .rs.getUserPref("new_proj_git_init") },
   set = function(value) { .rs.setUserPref("new_proj_git_init", value) }
)

# Create an renv environment in new projects
# Whether an renv environment should be created inside new projects by default.
.rs.uiPrefs$newProjUseRenv <- list(
   get = function() { .rs.getUserPref("new_proj_use_renv") },
   set = function(value) { .rs.setUserPref("new_proj_use_renv", value) }
)

# Root document for PDF compilation
# The root document to use when compiling PDF documents.
.rs.uiPrefs$rootDocument <- list(
   get = function() { .rs.getUserPref("root_document") },
   set = function(value) { .rs.setUserPref("root_document", value) }
)

# Show user home page in RStudio Workbench
# When to show the server home page in RStudio Workbench.
.rs.uiPrefs$showUserHomePage <- list(
   get = function() { .rs.getUserPref("show_user_home_page") },
   set = function(value) { .rs.setUserPref("show_user_home_page", value) }
)

# 
# Whether to reuse sessions when opening projects in RStudio Workbench.
.rs.uiPrefs$reuseSessionsForProjectLinks <- list(
   get = function() { .rs.getUserPref("reuse_sessions_for_project_links") },
   set = function(value) { .rs.setUserPref("reuse_sessions_for_project_links", value) }
)

# Enable version control if available
# Whether to enable RStudio's version control system interface.
.rs.uiPrefs$vcsEnabled <- list(
   get = function() { .rs.getUserPref("vcs_enabled") },
   set = function(value) { .rs.setUserPref("vcs_enabled", value) }
)

# Auto-refresh state from version control
# Automatically refresh VCS status?
.rs.uiPrefs$vcsAutorefresh <- list(
   get = function() { .rs.getUserPref("vcs_autorefresh") },
   set = function(value) { .rs.setUserPref("vcs_autorefresh", value) }
)

# Path to Git executable
# The path to the Git executable to use.
.rs.uiPrefs$gitExePath <- list(
   get = function() { .rs.getUserPref("git_exe_path") },
   set = function(value) { .rs.setUserPref("git_exe_path", value) }
)

# Path to Subversion executable
# The path to the Subversion executable to use.
.rs.uiPrefs$svnExePath <- list(
   get = function() { .rs.getUserPref("svn_exe_path") },
   set = function(value) { .rs.setUserPref("svn_exe_path", value) }
)

# 
# The path to the terminal executable to use.
.rs.uiPrefs$terminalPath <- list(
   get = function() { .rs.getUserPref("terminal_path") },
   set = function(value) { .rs.setUserPref("terminal_path", value) }
)

# 
# The path to the SSH key file to use.
.rs.uiPrefs$rsaKeyPath <- list(
   get = function() { .rs.getUserPref("rsa_key_path") },
   set = function(value) { .rs.setUserPref("rsa_key_path", value) }
)

# 
# The encryption type to use for the SSH key file.
.rs.uiPrefs$sshKeyType <- list(
   get = function() { .rs.getUserPref("ssh_key_type") },
   set = function(value) { .rs.setUserPref("ssh_key_type", value) }
)

# Use the devtools R package if available
# Whether to use the devtools R package.
.rs.uiPrefs$useDevtools <- list(
   get = function() { .rs.getUserPref("use_devtools") },
   set = function(value) { .rs.setUserPref("use_devtools", value) }
)

# Always use --preclean when installing package
# Always use --preclean when installing package.
.rs.uiPrefs$cleanBeforeInstall <- list(
   get = function() { .rs.getUserPref("clean_before_install") },
   set = function(value) { .rs.setUserPref("clean_before_install", value) }
)

# Download R packages securely
# Whether to use secure downloads when fetching R packages.
.rs.uiPrefs$useSecureDownload <- list(
   get = function() { .rs.getUserPref("use_secure_download") },
   set = function(value) { .rs.setUserPref("use_secure_download", value) }
)

# Clean up temporary files after R CMD CHECK
# Whether to clean up temporary files after running R CMD CHECK.
.rs.uiPrefs$cleanupAfterRCmdCheck <- list(
   get = function() { .rs.getUserPref("cleanup_after_r_cmd_check") },
   set = function(value) { .rs.setUserPref("cleanup_after_r_cmd_check", value) }
)

# View directory after R CMD CHECK
# Whether to view the directory after running R CMD CHECK.
.rs.uiPrefs$viewDirAfterRCmdCheck <- list(
   get = function() { .rs.getUserPref("view_dir_after_r_cmd_check") },
   set = function(value) { .rs.setUserPref("view_dir_after_r_cmd_check", value) }
)

# Hide object files in the Files pane
# Whether to hide object files in the Files pane.
.rs.uiPrefs$hideObjectFiles <- list(
   get = function() { .rs.getUserPref("hide_object_files") },
   set = function(value) { .rs.setUserPref("hide_object_files", value) }
)

# Restore last project when starting RStudio
# Whether to restore the last project when starting RStudio.
.rs.uiPrefs$restoreLastProject <- list(
   get = function() { .rs.getUserPref("restore_last_project") },
   set = function(value) { .rs.setUserPref("restore_last_project", value) }
)

# Number of seconds for safe project startup
# The number of seconds after which a project is deemed to have successfully
.rs.uiPrefs$projectSafeStartupSeconds <- list(
   get = function() { .rs.getUserPref("project_safe_startup_seconds") },
   set = function(value) { .rs.setUserPref("project_safe_startup_seconds", value) }
)

# Use tinytex to compile .tex files
# Use tinytex to compile .tex files.
.rs.uiPrefs$useTinytex <- list(
   get = function() { .rs.getUserPref("use_tinytex") },
   set = function(value) { .rs.setUserPref("use_tinytex", value) }
)

# Clean output after running Texi2Dvi
# Whether to clean output after running Texi2Dvi.
.rs.uiPrefs$cleanTexi2dviOutput <- list(
   get = function() { .rs.getUserPref("clean_texi2dvi_output") },
   set = function(value) { .rs.setUserPref("clean_texi2dvi_output", value) }
)

# Shell escape LaTeX documents
# Whether to enable shell escaping with LaTeX documents.
.rs.uiPrefs$latexShellEscape <- list(
   get = function() { .rs.getUserPref("latex_shell_escape") },
   set = function(value) { .rs.setUserPref("latex_shell_escape", value) }
)

# Restore project R version in RStudio Pro and RStudio Workbench
# Whether to restore the last version of R used by the project in RStudio Pro and
.rs.uiPrefs$restoreProjectRVersion <- list(
   get = function() { .rs.getUserPref("restore_project_r_version") },
   set = function(value) { .rs.setUserPref("restore_project_r_version", value) }
)

# Clang verbosity level (0 - 2)
# The verbosity level to use with Clang (0 - 2)
.rs.uiPrefs$clangVerbose <- list(
   get = function() { .rs.getUserPref("clang_verbose") },
   set = function(value) { .rs.setUserPref("clang_verbose", value) }
)

# Submit crash reports to RStudio
# Whether to automatically submit crash reports to RStudio.
.rs.uiPrefs$submitCrashReports <- list(
   get = function() { .rs.getUserPref("submit_crash_reports") },
   set = function(value) { .rs.setUserPref("submit_crash_reports", value) }
)

# 
# The R version to use by default.
.rs.uiPrefs$defaultRVersion <- list(
   get = function() { .rs.getUserPref("default_r_version") },
   set = function(value) { .rs.setUserPref("default_r_version", value) }
)

# Maximum number of columns in data viewer
# The maximum number of columns to show at once in the data viewer.
.rs.uiPrefs$dataViewerMaxColumns <- list(
   get = function() { .rs.getUserPref("data_viewer_max_columns") },
   set = function(value) { .rs.setUserPref("data_viewer_max_columns", value) }
)

# Maximum number of character in data viewer cells
# The maximum number of characters to show in a data viewer cell.
.rs.uiPrefs$dataViewerMaxCellSize <- list(
   get = function() { .rs.getUserPref("data_viewer_max_cell_size") },
   set = function(value) { .rs.setUserPref("data_viewer_max_cell_size", value) }
)

# Enable support for screen readers
# Support accessibility aids such as screen readers.
.rs.uiPrefs$enableScreenReader <- list(
   get = function() { .rs.getUserPref("enable_screen_reader") },
   set = function(value) { .rs.setUserPref("enable_screen_reader", value) }
)

# Seconds to wait before updating ARIA live region
# Number of milliseconds to wait after last keystroke before updating live
.rs.uiPrefs$typingStatusDelayMs <- list(
   get = function() { .rs.getUserPref("typing_status_delay_ms") },
   set = function(value) { .rs.setUserPref("typing_status_delay_ms", value) }
)

# Reduced animation/motion mode
# Reduce use of animations in the user interface.
.rs.uiPrefs$reducedMotion <- list(
   get = function() { .rs.getUserPref("reduced_motion") },
   set = function(value) { .rs.setUserPref("reduced_motion", value) }
)

# Tab key always moves focus
# Tab key moves focus out of text editing controls instead of inserting tabs.
.rs.uiPrefs$tabKeyMoveFocus <- list(
   get = function() { .rs.getUserPref("tab_key_move_focus") },
   set = function(value) { .rs.setUserPref("tab_key_move_focus", value) }
)

# Tab key moves focus directly from find text to replace text in find panel
# In source editor find panel, tab key moves focus directly from find text to
.rs.uiPrefs$findPanelLegacyTabSequence <- list(
   get = function() { .rs.getUserPref("find_panel_legacy_tab_sequence") },
   set = function(value) { .rs.setUserPref("find_panel_legacy_tab_sequence", value) }
)

# Show focus outline around focused panel
# Show which panel contains keyboard focus.
.rs.uiPrefs$showPanelFocusRectangle <- list(
   get = function() { .rs.getUserPref("show_panel_focus_rectangle") },
   set = function(value) { .rs.setUserPref("show_panel_focus_rectangle", value) }
)

# Autosave mode on idle
# How to deal with changes to documents on idle.
.rs.uiPrefs$autoSaveOnIdle <- list(
   get = function() { .rs.getUserPref("auto_save_on_idle") },
   set = function(value) { .rs.setUserPref("auto_save_on_idle", value) }
)

# Idle period for document autosave (ms)
# The idle period, in milliseconds, after which documents should be auto-saved.
.rs.uiPrefs$autoSaveIdleMs <- list(
   get = function() { .rs.getUserPref("auto_save_idle_ms") },
   set = function(value) { .rs.setUserPref("auto_save_idle_ms", value) }
)

# Save documents when editor loses input focus
# Whether to automatically save when the editor loses focus.
.rs.uiPrefs$autoSaveOnBlur <- list(
   get = function() { .rs.getUserPref("auto_save_on_blur") },
   set = function(value) { .rs.setUserPref("auto_save_on_blur", value) }
)

# Initial working directory for new terminals
# Initial directory for new terminals.
.rs.uiPrefs$terminalInitialDirectory <- list(
   get = function() { .rs.getUserPref("terminal_initial_directory") },
   set = function(value) { .rs.setUserPref("terminal_initial_directory", value) }
)

# Show full path to project in RStudio Desktop windows
# Whether to show the full path to project in desktop window title.
.rs.uiPrefs$fullProjectPathInWindowTitle <- list(
   get = function() { .rs.getUserPref("full_project_path_in_window_title") },
   set = function(value) { .rs.setUserPref("full_project_path_in_window_title", value) }
)

# Use visual editing by default for new markdown documents
# Whether to enable visual editing by default for new markdown documents
.rs.uiPrefs$visualMarkdownEditingIsDefault <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_is_default") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_is_default", value) }
)

# Default list spacing in visual markdown editing mode
# Default spacing for lists created in the visual editor
.rs.uiPrefs$visualMarkdownEditingListSpacing <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_list_spacing") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_list_spacing", value) }
)

# Wrap text in visual markdown editing mode
# Whether to automatically wrap text when writing markdown
.rs.uiPrefs$visualMarkdownEditingWrap <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_wrap") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_wrap", value) }
)

# Wrap column for visual markdown editing mode
# The column to wrap text at when writing markdown
.rs.uiPrefs$visualMarkdownEditingWrapAtColumn <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_wrap_at_column") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_wrap_at_column", value) }
)

# Place visual markdown footnotes in
# Placement of footnotes within markdown output.
.rs.uiPrefs$visualMarkdownEditingReferencesLocation <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_references_location") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_references_location", value) }
)

# Write canonical visual mode markdown in source mode
# Whether to write canonical visual mode markdown when saving from source mode.
.rs.uiPrefs$visualMarkdownEditingCanonical <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_canonical") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_canonical", value) }
)

# Max content width for visual markdown editor (px)
# Maximum content width for visual editing mode, in pixels
.rs.uiPrefs$visualMarkdownEditingMaxContentWidth <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_max_content_width") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_max_content_width", value) }
)

# Show document outline in visual markdown editing mode
# Whether to show the document outline by default when opening R Markdown
.rs.uiPrefs$visualMarkdownEditingShowDocOutline <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_show_doc_outline") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_show_doc_outline", value) }
)

# Show margin in visual mode code blocks
# Whether to show the margin guide in the visual mode code blocks.
.rs.uiPrefs$visualMarkdownEditingShowMargin <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_show_margin") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_show_margin", value) }
)

# Show line numbers in visual mode code blocks
# Whether to show line numbers in the code editors used in visual mode
.rs.uiPrefs$visualMarkdownCodeEditorLineNumbers <- list(
   get = function() { .rs.getUserPref("visual_markdown_code_editor_line_numbers") },
   set = function(value) { .rs.setUserPref("visual_markdown_code_editor_line_numbers", value) }
)

# Font size for visual editing mode
# The default visual editing mode font size, in points
.rs.uiPrefs$visualMarkdownEditingFontSizePoints <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_font_size_points") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_font_size_points", value) }
)

# Editor for code chunks in visual editing mode
# The name of the editor to use to provide code editing in visual mode
.rs.uiPrefs$visualMarkdownCodeEditor <- list(
   get = function() { .rs.getUserPref("visual_markdown_code_editor") },
   set = function(value) { .rs.setUserPref("visual_markdown_code_editor", value) }
)

# Zotero libraries
# Zotero libraries to insert citations from.
.rs.uiPrefs$zoteroLibraries <- list(
   get = function() { .rs.getUserPref("zotero_libraries") },
   set = function(value) { .rs.setUserPref("zotero_libraries", value) }
)

# 
# Preferred emoji skintone
.rs.uiPrefs$emojiSkintone <- list(
   get = function() { .rs.getUserPref("emoji_skintone") },
   set = function(value) { .rs.setUserPref("emoji_skintone", value) }
)

# Disabled aria-live announcements
# List of aria-live announcements to disable.
.rs.uiPrefs$disabledAriaLiveAnnouncements <- list(
   get = function() { .rs.getUserPref("disabled_aria_live_announcements") },
   set = function(value) { .rs.setUserPref("disabled_aria_live_announcements", value) }
)

# Maximum number of console lines to announce
# Maximum number of lines of console output announced after a command.
.rs.uiPrefs$screenreaderConsoleAnnounceLimit <- list(
   get = function() { .rs.getUserPref("screenreader_console_announce_limit") },
   set = function(value) { .rs.setUserPref("screenreader_console_announce_limit", value) }
)

# List of path components ignored by file monitor
# List of path components; file monitor will ignore paths containing one or more
.rs.uiPrefs$fileMonitorIgnoredComponents <- list(
   get = function() { .rs.getUserPref("file_monitor_ignored_components") },
   set = function(value) { .rs.setUserPref("file_monitor_ignored_components", value) }
)

# Install R package dependencies one at a time
# Whether to install R package dependencies one at a time.
.rs.uiPrefs$installPkgDepsIndividually <- list(
   get = function() { .rs.getUserPref("install_pkg_deps_individually") },
   set = function(value) { .rs.setUserPref("install_pkg_deps_individually", value) }
)

# R graphics backend
# R graphics backend.
.rs.uiPrefs$graphicsBackend <- list(
   get = function() { .rs.getUserPref("graphics_backend") },
   set = function(value) { .rs.setUserPref("graphics_backend", value) }
)

# R graphics antialiasing method
# Type of anti-aliasing to be used for generated R plots.
.rs.uiPrefs$graphicsAntialiasing <- list(
   get = function() { .rs.getUserPref("graphics_antialiasing") },
   set = function(value) { .rs.setUserPref("graphics_antialiasing", value) }
)

# Fixed-width font list for RStudio Server
# List of fixed-width fonts to check for browser support.
.rs.uiPrefs$browserFixedWidthFonts <- list(
   get = function() { .rs.getUserPref("browser_fixed_width_fonts") },
   set = function(value) { .rs.setUserPref("browser_fixed_width_fonts", value) }
)

# 
# The Python type.
.rs.uiPrefs$pythonType <- list(
   get = function() { .rs.getUserPref("python_type") },
   set = function(value) { .rs.setUserPref("python_type", value) }
)

# 
# The Python version.
.rs.uiPrefs$pythonVersion <- list(
   get = function() { .rs.getUserPref("python_version") },
   set = function(value) { .rs.setUserPref("python_version", value) }
)

# 
# The path to the default Python interpreter.
.rs.uiPrefs$pythonPath <- list(
   get = function() { .rs.getUserPref("python_path") },
   set = function(value) { .rs.setUserPref("python_path", value) }
)

# Save Retry Timeout
# The maximum amount of seconds of retry for save operations.
.rs.uiPrefs$saveRetryTimeout <- list(
   get = function() { .rs.getUserPref("save_retry_timeout") },
   set = function(value) { .rs.setUserPref("save_retry_timeout", value) }
)

# Use R's native pipe operator, |>
# Whether the Insert Pipe Operator command should use the native R pipe operator,
.rs.uiPrefs$insertNativePipeOperator <- list(
   get = function() { .rs.getUserPref("insert_native_pipe_operator") },
   set = function(value) { .rs.setUserPref("insert_native_pipe_operator", value) }
)

# Remember recently used items in Command Palette
# Whether to keep track of recently used commands in the Command Palette
.rs.uiPrefs$commandPaletteMru <- list(
   get = function() { .rs.getUserPref("command_palette_mru") },
   set = function(value) { .rs.setUserPref("command_palette_mru", value) }
)

# Show memory usage in Environment Pane
# Whether to compute and show memory usage in the Environment Pane
.rs.uiPrefs$showMemoryUsage <- list(
   get = function() { .rs.getUserPref("show_memory_usage") },
   set = function(value) { .rs.setUserPref("show_memory_usage", value) }
)

# Interval for requerying memory stats (seconds)
# How many seconds to wait between automatic requeries of memory statistics (0 to
.rs.uiPrefs$memoryQueryIntervalSeconds <- list(
   get = function() { .rs.getUserPref("memory_query_interval_seconds") },
   set = function(value) { .rs.setUserPref("memory_query_interval_seconds", value) }
)

# Enable terminal Python integration
# Enable Python terminal hooks. When enabled, the RStudio-configured version of
.rs.uiPrefs$terminalPythonIntegration <- list(
   get = function() { .rs.getUserPref("terminal_python_integration") },
   set = function(value) { .rs.setUserPref("terminal_python_integration", value) }
)

# Session protocol debug logging
# Enable session protocol debug logging showing all session requests and events
.rs.uiPrefs$sessionProtocolDebug <- list(
   get = function() { .rs.getUserPref("session_protocol_debug") },
   set = function(value) { .rs.setUserPref("session_protocol_debug", value) }
)

# Automatically activate project Python environments
# When enabled, if the active project contains a Python virtual environment, then
.rs.uiPrefs$pythonProjectEnvironmentAutomaticActivate <- list(
   get = function() { .rs.getUserPref("python_project_environment_automatic_activate") },
   set = function(value) { .rs.setUserPref("python_project_environment_automatic_activate", value) }
)

# Check values in the Environment pane for null external pointers
# When enabled, RStudio will detect R objects containing null external pointers
.rs.uiPrefs$checkNullExternalPointers <- list(
   get = function() { .rs.getUserPref("check_null_external_pointers") },
   set = function(value) { .rs.setUserPref("check_null_external_pointers", value) }
)

# User Interface Language:
# The IDE's user-interface language.
.rs.uiPrefs$uiLanguage <- list(
   get = function() { .rs.getUserPref("ui_language") },
   set = function(value) { .rs.setUserPref("ui_language", value) }
)

# Auto hide menu bar
# Hide desktop menu bar until Alt key is pressed.
.rs.uiPrefs$autohideMenubar <- list(
   get = function() { .rs.getUserPref("autohide_menubar") },
   set = function(value) { .rs.setUserPref("autohide_menubar", value) }
)

# Use native file and message dialog boxes
# Whether RStudio Desktop will use the operating system's native File and Message
.rs.uiPrefs$nativeFileDialogs <- list(
   get = function() { .rs.getUserPref("native_file_dialogs") },
   set = function(value) { .rs.setUserPref("native_file_dialogs", value) }
)

# Discard pending console input on error
# When enabled, any pending console input will be discarded when an (uncaught) R
.rs.uiPrefs$discardPendingConsoleInputOnError <- list(
   get = function() { .rs.getUserPref("discard_pending_console_input_on_error") },
   set = function(value) { .rs.setUserPref("discard_pending_console_input_on_error", value) }
)

# Editor scroll speed sensitivity
# An integer value, 1-200, to set the editor scroll multiplier. The higher the
.rs.uiPrefs$editorScrollMultiplier <- list(
   get = function() { .rs.getUserPref("editor_scroll_multiplier") },
   set = function(value) { .rs.setUserPref("editor_scroll_multiplier", value) }
)

# Text rendering
# Control how text is rendered within the IDE surface.
.rs.uiPrefs$textRendering <- list(
   get = function() { .rs.getUserPref("text_rendering") },
   set = function(value) { .rs.setUserPref("text_rendering", value) }
)

# Disable Electron accessibility support
# Disable Electron accessibility support.
.rs.uiPrefs$disableRendererAccessibility <- list(
   get = function() { .rs.getUserPref("disable_renderer_accessibility") },
   set = function(value) { .rs.setUserPref("disable_renderer_accessibility", value) }
)

# Enable GitHub Copilot
# When enabled, RStudio will use GitHub Copilot to provide code suggestions.
.rs.uiPrefs$copilotEnabled <- list(
   get = function() { .rs.getUserPref("copilot_enabled") },
   set = function(value) { .rs.setUserPref("copilot_enabled", value) }
)

# Show Copilot code suggestions:
# Control when Copilot code suggestions are displayed in the editor.
.rs.uiPrefs$copilotCompletionsTrigger <- list(
   get = function() { .rs.getUserPref("copilot_completions_trigger") },
   set = function(value) { .rs.setUserPref("copilot_completions_trigger", value) }
)

# GitHub Copilot completions delay
# The delay (in milliseconds) before GitHub Copilot completions are requested
.rs.uiPrefs$copilotCompletionsDelay <- list(
   get = function() { .rs.getUserPref("copilot_completions_delay") },
   set = function(value) { .rs.setUserPref("copilot_completions_delay", value) }
)

# Pressing Tab key will prefer inserting:
# Control the behavior of the Tab key when both Copilot code suggestions and
.rs.uiPrefs$copilotTabKeyBehavior <- list(
   get = function() { .rs.getUserPref("copilot_tab_key_behavior") },
   set = function(value) { .rs.setUserPref("copilot_tab_key_behavior", value) }
)

# Index project files with GitHub Copilot
# When enabled, RStudio will index project files with GitHub Copilot.
.rs.uiPrefs$copilotIndexingEnabled <- list(
   get = function() { .rs.getUserPref("copilot_indexing_enabled") },
   set = function(value) { .rs.setUserPref("copilot_indexing_enabled", value) }
)

# 
# User-provided name for the currently opened R project.
.rs.uiPrefs$projectName <- list(
   get = function() { .rs.getUserPref("project_name") },
   set = function(value) { .rs.setUserPref("project_name", value) }
)

# Default working directory for background jobs
# Default working directory in background job dialog.
.rs.uiPrefs$runBackgroundJobDefaultWorkingDir <- list(
   get = function() { .rs.getUserPref("run_background_job_default_working_dir") },
   set = function(value) { .rs.setUserPref("run_background_job_default_working_dir", value) }
)

# Code formatter
# The formatter to use when reformatting code.
.rs.uiPrefs$codeFormatter <- list(
   get = function() { .rs.getUserPref("code_formatter") },
   set = function(value) { .rs.setUserPref("code_formatter", value) }
)

# Use strict transformers when formatting code
# When set, strict transformers will be used when formatting code. See the
.rs.uiPrefs$codeFormatterStylerStrict <- list(
   get = function() { .rs.getUserPref("code_formatter_styler_strict") },
   set = function(value) { .rs.setUserPref("code_formatter_styler_strict", value) }
)

# 
# The external command to be used when reformatting code.
.rs.uiPrefs$codeFormatterExternalCommand <- list(
   get = function() { .rs.getUserPref("code_formatter_external_command") },
   set = function(value) { .rs.setUserPref("code_formatter_external_command", value) }
)

# Reformat documents on save
# When set, the selected formatter will be used to reformat documents on save.
.rs.uiPrefs$reformatOnSave <- list(
   get = function() { .rs.getUserPref("reformat_on_save") },
   set = function(value) { .rs.setUserPref("reformat_on_save", value) }
)

# Default project user data directory
# The folder in which RStudio should store project .Rproj.user data.
.rs.uiPrefs$projectUserDataDirectory <- list(
   get = function() { .rs.getUserPref("project_user_data_directory") },
   set = function(value) { .rs.setUserPref("project_user_data_directory", value) }
)

#
# SessionUserPrefValues.R
#
# Copyright (C) 2024 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

#
# This file was automatically generated -- please do not modify it by hand.
#

.rs.setVar("uiPrefs", new.env(parent = emptyenv()))

# Run .Rprofile on resume
# Whether to run .Rprofile again after resuming a suspended R session.
.rs.uiPrefs$runRprofileOnResume <- list(
   get = function() { .rs.getUserPref("run_rprofile_on_resume") },
   set = function(value) { .rs.setUserPref("run_rprofile_on_resume", value) }
)

# Save workspace on quit
# Whether to save the workspace to an .Rdata file after the R session ends.
.rs.uiPrefs$saveWorkspace <- list(
   get = function() { .rs.getUserPref("save_workspace") },
   set = function(value) { .rs.setUserPref("save_workspace", value) }
)

# Load workspace on start
# Whether to load the workspace when the R session begins.
.rs.uiPrefs$loadWorkspace <- list(
   get = function() { .rs.getUserPref("load_workspace") },
   set = function(value) { .rs.setUserPref("load_workspace", value) }
)

# Initial working directory
# The initial working directory for new R sessions.
.rs.uiPrefs$initialWorkingDirectory <- list(
   get = function() { .rs.getUserPref("initial_working_directory") },
   set = function(value) { .rs.setUserPref("initial_working_directory", value) }
)

# 
# The CRAN mirror to use.
.rs.uiPrefs$cranMirror <- list(
   get = function() { .rs.getUserPref("cran_mirror") },
   set = function(value) { .rs.setUserPref("cran_mirror", value) }
)

# 
# The name of the default Bioconductor mirror.
.rs.uiPrefs$bioconductorMirrorName <- list(
   get = function() { .rs.getUserPref("bioconductor_mirror_name") },
   set = function(value) { .rs.setUserPref("bioconductor_mirror_name", value) }
)

# 
# The URL of the default Bioconductor mirror.
.rs.uiPrefs$bioconductorMirrorUrl <- list(
   get = function() { .rs.getUserPref("bioconductor_mirror_url") },
   set = function(value) { .rs.setUserPref("bioconductor_mirror_url", value) }
)

# Save R console history
# Whether to always save the R console history.
.rs.uiPrefs$alwaysSaveHistory <- list(
   get = function() { .rs.getUserPref("always_save_history") },
   set = function(value) { .rs.setUserPref("always_save_history", value) }
)

# Remove duplicates from console history
# Whether to remove duplicate entries from the R console history.
.rs.uiPrefs$removeHistoryDuplicates <- list(
   get = function() { .rs.getUserPref("remove_history_duplicates") },
   set = function(value) { .rs.setUserPref("remove_history_duplicates", value) }
)

# Show .Last.value in Environment pane
# Show the result of the last expression (.Last.value) in the Environment pane.
.rs.uiPrefs$showLastDotValue <- list(
   get = function() { .rs.getUserPref("show_last_dot_value") },
   set = function(value) { .rs.setUserPref("show_last_dot_value", value) }
)

# Line ending format
# The line ending format to use when saving files.
.rs.uiPrefs$lineEndingConversion <- list(
   get = function() { .rs.getUserPref("line_ending_conversion") },
   set = function(value) { .rs.setUserPref("line_ending_conversion", value) }
)

# Use newlines in Makefiles
# Whether to use newlines when saving Makefiles.
.rs.uiPrefs$useNewlinesInMakefiles <- list(
   get = function() { .rs.getUserPref("use_newlines_in_makefiles") },
   set = function(value) { .rs.setUserPref("use_newlines_in_makefiles", value) }
)

# 
# The terminal shell to use on Windows.
.rs.uiPrefs$windowsTerminalShell <- list(
   get = function() { .rs.getUserPref("windows_terminal_shell") },
   set = function(value) { .rs.setUserPref("windows_terminal_shell", value) }
)

# 
# The terminal shell to use on POSIX operating systems (MacOS and Linux).
.rs.uiPrefs$posixTerminalShell <- list(
   get = function() { .rs.getUserPref("posix_terminal_shell") },
   set = function(value) { .rs.setUserPref("posix_terminal_shell", value) }
)

# 
# tab.
.rs.uiPrefs$customShellCommand <- list(
   get = function() { .rs.getUserPref("custom_shell_command") },
   set = function(value) { .rs.setUserPref("custom_shell_command", value) }
)

# 
# The command-line options to pass to the custom shell command.
.rs.uiPrefs$customShellOptions <- list(
   get = function() { .rs.getUserPref("custom_shell_options") },
   set = function(value) { .rs.setUserPref("custom_shell_options", value) }
)

# Show line numbers in editor
# Show line numbers in RStudio's code editor.
.rs.uiPrefs$showLineNumbers <- list(
   get = function() { .rs.getUserPref("show_line_numbers") },
   set = function(value) { .rs.setUserPref("show_line_numbers", value) }
)

# Use relative line numbers in editor
# Show relative, rather than absolute, line numbers in RStudio's code editor.
.rs.uiPrefs$relativeLineNumbers <- list(
   get = function() { .rs.getUserPref("relative_line_numbers") },
   set = function(value) { .rs.setUserPref("relative_line_numbers", value) }
)

# Highlight selected word in editor
# Highlight the selected word in RStudio's code editor.
.rs.uiPrefs$highlightSelectedWord <- list(
   get = function() { .rs.getUserPref("highlight_selected_word") },
   set = function(value) { .rs.setUserPref("highlight_selected_word", value) }
)

# Highlight selected line in editor
# Highlight the selected line in RStudio's code editor.
.rs.uiPrefs$highlightSelectedLine <- list(
   get = function() { .rs.getUserPref("highlight_selected_line") },
   set = function(value) { .rs.setUserPref("highlight_selected_line", value) }
)

# 
# Layout of panes in the RStudio workbench.
.rs.uiPrefs$panes <- list(
   get = function() { .rs.getUserPref("panes") },
   set = function(value) { .rs.setUserPref("panes", value) }
)

# Allow source columns
# Whether to enable the ability to add source columns to display.
.rs.uiPrefs$allowSourceColumns <- list(
   get = function() { .rs.getUserPref("allow_source_columns") },
   set = function(value) { .rs.setUserPref("allow_source_columns", value) }
)

# Insert spaces for Tab
# Whether to insert spaces when pressing the Tab key.
.rs.uiPrefs$useSpacesForTab <- list(
   get = function() { .rs.getUserPref("use_spaces_for_tab") },
   set = function(value) { .rs.setUserPref("use_spaces_for_tab", value) }
)

# Number of spaces for Tab
# The number of spaces to insert when pressing the Tab key.
.rs.uiPrefs$numSpacesForTab <- list(
   get = function() { .rs.getUserPref("num_spaces_for_tab") },
   set = function(value) { .rs.setUserPref("num_spaces_for_tab", value) }
)

# Auto-detect indentation in files
# Whether to automatically detect indentation settings from file contents.
.rs.uiPrefs$autoDetectIndentation <- list(
   get = function() { .rs.getUserPref("auto_detect_indentation") },
   set = function(value) { .rs.setUserPref("auto_detect_indentation", value) }
)

# Show margin in editor
# Whether to show the margin guide in the RStudio code editor.
.rs.uiPrefs$showMargin <- list(
   get = function() { .rs.getUserPref("show_margin") },
   set = function(value) { .rs.setUserPref("show_margin", value) }
)

# Use a blinking cursor
# Whether to flash the cursor off and on.
.rs.uiPrefs$blinkingCursor <- list(
   get = function() { .rs.getUserPref("blinking_cursor") },
   set = function(value) { .rs.setUserPref("blinking_cursor", value) }
)

# Margin column
# The number of columns of text after which the margin is shown.
.rs.uiPrefs$marginColumn <- list(
   get = function() { .rs.getUserPref("margin_column") },
   set = function(value) { .rs.setUserPref("margin_column", value) }
)

# Show invisible characters in editor
# code editor.
.rs.uiPrefs$showInvisibles <- list(
   get = function() { .rs.getUserPref("show_invisibles") },
   set = function(value) { .rs.setUserPref("show_invisibles", value) }
)

# Indentation guides
# Style for indentation guides in the RStudio code editor.
.rs.uiPrefs$indentGuides <- list(
   get = function() { .rs.getUserPref("indent_guides") },
   set = function(value) { .rs.setUserPref("indent_guides", value) }
)

# Continue comments after adding new line
# a new line.
.rs.uiPrefs$continueCommentsOnNewline <- list(
   get = function() { .rs.getUserPref("continue_comments_on_newline") },
   set = function(value) { .rs.setUserPref("continue_comments_on_newline", value) }
)

# Whether web links in comments are clickable
# Whether web links in comments are clickable.
.rs.uiPrefs$highlightWebLink <- list(
   get = function() { .rs.getUserPref("highlight_web_link") },
   set = function(value) { .rs.setUserPref("highlight_web_link", value) }
)

# Keybinding set for editor
# The keybindings to use in the RStudio code editor.
.rs.uiPrefs$editorKeybindings <- list(
   get = function() { .rs.getUserPref("editor_keybindings") },
   set = function(value) { .rs.setUserPref("editor_keybindings", value) }
)

# Auto-insert matching parentheses and brackets
# Whether to insert matching pairs, such as () and [], when the first is typed.
.rs.uiPrefs$insertMatching <- list(
   get = function() { .rs.getUserPref("insert_matching") },
   set = function(value) { .rs.setUserPref("insert_matching", value) }
)

# Insert spaces around = in R code
# Whether to insert spaces around the equals sign in R code.
.rs.uiPrefs$insertSpacesAroundEquals <- list(
   get = function() { .rs.getUserPref("insert_spaces_around_equals") },
   set = function(value) { .rs.setUserPref("insert_spaces_around_equals", value) }
)

# Insert parentheses after functions
# Whether to insert parentheses after function completions.
.rs.uiPrefs$insertParensAfterFunctionCompletion <- list(
   get = function() { .rs.getUserPref("insert_parens_after_function_completion") },
   set = function(value) { .rs.setUserPref("insert_parens_after_function_completion", value) }
)

# Complete multi-line statements with Tab
# Whether to attempt completion of multiple-line statements when pressing Tab.
.rs.uiPrefs$tabMultilineCompletion <- list(
   get = function() { .rs.getUserPref("tab_multiline_completion") },
   set = function(value) { .rs.setUserPref("tab_multiline_completion", value) }
)

# Use Tab to trigger autocompletion
# Whether to attempt completion of statements when pressing Tab.
.rs.uiPrefs$tabCompletion <- list(
   get = function() { .rs.getUserPref("tab_completion") },
   set = function(value) { .rs.setUserPref("tab_completion", value) }
)

# Show function help tooltips on idle
# recently moved.
.rs.uiPrefs$showHelpTooltipOnIdle <- list(
   get = function() { .rs.getUserPref("show_help_tooltip_on_idle") },
   set = function(value) { .rs.setUserPref("show_help_tooltip_on_idle", value) }
)

# Surround selections with
# Which kinds of delimiters can be used to surround the current selection.
.rs.uiPrefs$surroundSelection <- list(
   get = function() { .rs.getUserPref("surround_selection") },
   set = function(value) { .rs.setUserPref("surround_selection", value) }
)

# Enable code snippets
# Whether to enable code snippets in the RStudio code editor.
.rs.uiPrefs$enableSnippets <- list(
   get = function() { .rs.getUserPref("enable_snippets") },
   set = function(value) { .rs.setUserPref("enable_snippets", value) }
)

# Use code completion for R
# When to use auto-completion for R code in the RStudio code editor.
.rs.uiPrefs$codeCompletion <- list(
   get = function() { .rs.getUserPref("code_completion") },
   set = function(value) { .rs.setUserPref("code_completion", value) }
)

# Use code completion for other languages
# the RStudio code editor.
.rs.uiPrefs$codeCompletionOther <- list(
   get = function() { .rs.getUserPref("code_completion_other") },
   set = function(value) { .rs.setUserPref("code_completion_other", value) }
)

# Use code completion in the R console
# Whether to always use code completion in the R console.
.rs.uiPrefs$consoleCodeCompletion <- list(
   get = function() { .rs.getUserPref("console_code_completion") },
   set = function(value) { .rs.setUserPref("console_code_completion", value) }
)

# Delay before completing code (ms)
# The number of milliseconds to wait before offering code suggestions.
.rs.uiPrefs$codeCompletionDelay <- list(
   get = function() { .rs.getUserPref("code_completion_delay") },
   set = function(value) { .rs.setUserPref("code_completion_delay", value) }
)

# Number of characters for code completion
# offered.
.rs.uiPrefs$codeCompletionCharacters <- list(
   get = function() { .rs.getUserPref("code_completion_characters") },
   set = function(value) { .rs.setUserPref("code_completion_characters", value) }
)

# Include all function arguments in completion list
# even if those arguments have already appear to be used in the current function
.rs.uiPrefs$codeCompletionIncludeAlreadyUsed <- list(
   get = function() { .rs.getUserPref("code_completion_include_already_used") },
   set = function(value) { .rs.setUserPref("code_completion_include_already_used", value) }
)

# Show function signature tooltips
# Whether to show function signature tooltips during autocompletion.
.rs.uiPrefs$showFunctionSignatureTooltips <- list(
   get = function() { .rs.getUserPref("show_function_signature_tooltips") },
   set = function(value) { .rs.setUserPref("show_function_signature_tooltips", value) }
)

# Show data preview in autocompletion help popup
# and values.
.rs.uiPrefs$showDataPreview <- list(
   get = function() { .rs.getUserPref("show_data_preview") },
   set = function(value) { .rs.setUserPref("show_data_preview", value) }
)

# Show diagnostics in R code
# code as you type.
.rs.uiPrefs$showDiagnosticsR <- list(
   get = function() { .rs.getUserPref("show_diagnostics_r") },
   set = function(value) { .rs.setUserPref("show_diagnostics_r", value) }
)

# Show diagnostics in C++ code
# Whether to show diagnostic messages for C++ code as you type.
.rs.uiPrefs$showDiagnosticsCpp <- list(
   get = function() { .rs.getUserPref("show_diagnostics_cpp") },
   set = function(value) { .rs.setUserPref("show_diagnostics_cpp", value) }
)

# Show diagnostics in YAML code
# Whether to show diagnostic messages for YAML code as you type.
.rs.uiPrefs$showDiagnosticsYaml <- list(
   get = function() { .rs.getUserPref("show_diagnostics_yaml") },
   set = function(value) { .rs.setUserPref("show_diagnostics_yaml", value) }
)

# Show diagnostics in other languages
# YAML).
.rs.uiPrefs$showDiagnosticsOther <- list(
   get = function() { .rs.getUserPref("show_diagnostics_other") },
   set = function(value) { .rs.setUserPref("show_diagnostics_other", value) }
)

# Show style diagnostics for R code
# Whether to show style diagnostics (suggestions for improving R code style)
.rs.uiPrefs$styleDiagnostics <- list(
   get = function() { .rs.getUserPref("style_diagnostics") },
   set = function(value) { .rs.setUserPref("style_diagnostics", value) }
)

# Check code for problems when saving
# Whether to check code for problems after saving it.
.rs.uiPrefs$diagnosticsOnSave <- list(
   get = function() { .rs.getUserPref("diagnostics_on_save") },
   set = function(value) { .rs.setUserPref("diagnostics_on_save", value) }
)

# Run R code diagnostics in the background
# Whether to run code diagnostics in the background, as you type.
.rs.uiPrefs$backgroundDiagnostics <- list(
   get = function() { .rs.getUserPref("background_diagnostics") },
   set = function(value) { .rs.setUserPref("background_diagnostics", value) }
)

# Run R code diagnostics after (ms)
# background.
.rs.uiPrefs$backgroundDiagnosticsDelayMs <- list(
   get = function() { .rs.getUserPref("background_diagnostics_delay_ms") },
   set = function(value) { .rs.setUserPref("background_diagnostics_delay_ms", value) }
)

# Run diagnostics on R function calls
# Whether to run diagnostics in R function calls.
.rs.uiPrefs$diagnosticsInRFunctionCalls <- list(
   get = function() { .rs.getUserPref("diagnostics_in_r_function_calls") },
   set = function(value) { .rs.setUserPref("diagnostics_in_r_function_calls", value) }
)

# Check arguments to R function calls
# Whether to check arguments to R function calls.
.rs.uiPrefs$checkArgumentsToRFunctionCalls <- list(
   get = function() { .rs.getUserPref("check_arguments_to_r_function_calls") },
   set = function(value) { .rs.setUserPref("check_arguments_to_r_function_calls", value) }
)

# Check for unexpected assignments
# Whether to check for unexpected variable assignments inside R function calls.
.rs.uiPrefs$checkUnexpectedAssignmentInFunctionCall <- list(
   get = function() { .rs.getUserPref("check_unexpected_assignment_in_function_call") },
   set = function(value) { .rs.setUserPref("check_unexpected_assignment_in_function_call", value) }
)

# Warn when R variable used but not defined
# the current scope.
.rs.uiPrefs$warnIfNoSuchVariableInScope <- list(
   get = function() { .rs.getUserPref("warn_if_no_such_variable_in_scope") },
   set = function(value) { .rs.setUserPref("warn_if_no_such_variable_in_scope", value) }
)

# Warn when R variable defined but not used
# the current scope
.rs.uiPrefs$warnVariableDefinedButNotUsed <- list(
   get = function() { .rs.getUserPref("warn_variable_defined_but_not_used") },
   set = function(value) { .rs.setUserPref("warn_variable_defined_but_not_used", value) }
)

# Detect missing R packages in the editor
# dependencies.
.rs.uiPrefs$autoDiscoverPackageDependencies <- list(
   get = function() { .rs.getUserPref("auto_discover_package_dependencies") },
   set = function(value) { .rs.setUserPref("auto_discover_package_dependencies", value) }
)

# Ensure files end with a newline when saving
# Whether to ensure that source files end with a newline character.
.rs.uiPrefs$autoAppendNewline <- list(
   get = function() { .rs.getUserPref("auto_append_newline") },
   set = function(value) { .rs.setUserPref("auto_append_newline", value) }
)

# Strip trailing whitespace when saving
# Whether to strip trailing whitespace from each line when saving.
.rs.uiPrefs$stripTrailingWhitespace <- list(
   get = function() { .rs.getUserPref("strip_trailing_whitespace") },
   set = function(value) { .rs.setUserPref("strip_trailing_whitespace", value) }
)

# Restore cursor position when reopening files
# when the file is opened.
.rs.uiPrefs$restoreSourceDocumentCursorPosition <- list(
   get = function() { .rs.getUserPref("restore_source_document_cursor_position") },
   set = function(value) { .rs.setUserPref("restore_source_document_cursor_position", value) }
)

# Re-indent code when pasting
# Whether to automatically re-indent code when it's pasted into RStudio.
.rs.uiPrefs$reindentOnPaste <- list(
   get = function() { .rs.getUserPref("reindent_on_paste") },
   set = function(value) { .rs.setUserPref("reindent_on_paste", value) }
)

# Vertically align function arguments
# indentation.
.rs.uiPrefs$verticallyAlignArgumentsIndent <- list(
   get = function() { .rs.getUserPref("vertically_align_arguments_indent") },
   set = function(value) { .rs.setUserPref("vertically_align_arguments_indent", value) }
)

# Soft-wrap source files
# inserting newline characters.
.rs.uiPrefs$softWrapRFiles <- list(
   get = function() { .rs.getUserPref("soft_wrap_r_files") },
   set = function(value) { .rs.setUserPref("soft_wrap_r_files", value) }
)

# Soft-wrap R Markdown files
# Notebooks)
.rs.uiPrefs$softWrapRmdFiles <- list(
   get = function() { .rs.getUserPref("soft_wrap_rmd_files") },
   set = function(value) { .rs.setUserPref("soft_wrap_rmd_files", value) }
)

# Focus console after executing R code
# Whether to focus the R console after executing an R command from a script.
.rs.uiPrefs$focusConsoleAfterExec <- list(
   get = function() { .rs.getUserPref("focus_console_after_exec") },
   set = function(value) { .rs.setUserPref("focus_console_after_exec", value) }
)

# Fold style in editor
# The style of folding to use.
.rs.uiPrefs$foldStyle <- list(
   get = function() { .rs.getUserPref("fold_style") },
   set = function(value) { .rs.setUserPref("fold_style", value) }
)

# Save R scripts before sourcing
# Whether to automatically save scripts before executing them.
.rs.uiPrefs$saveBeforeSourcing <- list(
   get = function() { .rs.getUserPref("save_before_sourcing") },
   set = function(value) { .rs.setUserPref("save_before_sourcing", value) }
)

# Syntax highlighting in R console
# Whether to use syntax highlighting in the R console.
.rs.uiPrefs$syntaxColorConsole <- list(
   get = function() { .rs.getUserPref("syntax_color_console") },
   set = function(value) { .rs.setUserPref("syntax_color_console", value) }
)

# Different color for error output in R console
# Whether to display error, warning, and message output in a different color.
.rs.uiPrefs$highlightConsoleErrors <- list(
   get = function() { .rs.getUserPref("highlight_console_errors") },
   set = function(value) { .rs.setUserPref("highlight_console_errors", value) }
)

# Scroll past end of file
# Whether to allow scrolling past the end of a file.
.rs.uiPrefs$scrollPastEndOfDocument <- list(
   get = function() { .rs.getUserPref("scroll_past_end_of_document") },
   set = function(value) { .rs.setUserPref("scroll_past_end_of_document", value) }
)

# Highlight R function calls
# Whether to highlight R function calls in the code editor.
.rs.uiPrefs$highlightRFunctionCalls <- list(
   get = function() { .rs.getUserPref("highlight_r_function_calls") },
   set = function(value) { .rs.setUserPref("highlight_r_function_calls", value) }
)

# Enable preview of named and hexadecimal colors
# Whether to show preview for named and hexadecimal colors.
.rs.uiPrefs$colorPreview <- list(
   get = function() { .rs.getUserPref("color_preview") },
   set = function(value) { .rs.setUserPref("color_preview", value) }
)

# Use rainbow parentheses
# Whether to highlight parentheses in a variety of colors.
.rs.uiPrefs$rainbowParentheses <- list(
   get = function() { .rs.getUserPref("rainbow_parentheses") },
   set = function(value) { .rs.setUserPref("rainbow_parentheses", value) }
)

# Use rainbow fenced divs
# Whether to highlight fenced divs in a variety of colors.
.rs.uiPrefs$rainbowFencedDivs <- list(
   get = function() { .rs.getUserPref("rainbow_fenced_divs") },
   set = function(value) { .rs.setUserPref("rainbow_fenced_divs", value) }
)

# Maximum characters per line in R console
# The maximum number of characters to display in a single line in the R console.
.rs.uiPrefs$consoleLineLengthLimit <- list(
   get = function() { .rs.getUserPref("console_line_length_limit") },
   set = function(value) { .rs.setUserPref("console_line_length_limit", value) }
)

# Maximum lines in R console
# scrollback buffer.
.rs.uiPrefs$consoleMaxLines <- list(
   get = function() { .rs.getUserPref("console_max_lines") },
   set = function(value) { .rs.setUserPref("console_max_lines", value) }
)

# ANSI escape codes in R console
# How to treat ANSI escape codes in the console.
.rs.uiPrefs$ansiConsoleMode <- list(
   get = function() { .rs.getUserPref("ansi_console_mode") },
   set = function(value) { .rs.setUserPref("ansi_console_mode", value) }
)

# Limit visible console output
# Whether to only show a limited window of the total console output
.rs.uiPrefs$limitVisibleConsole <- list(
   get = function() { .rs.getUserPref("limit_visible_console") },
   set = function(value) { .rs.setUserPref("limit_visible_console", value) }
)

# Show toolbar on R Markdown chunks
# Whether to show a toolbar on code chunks in R Markdown documents.
.rs.uiPrefs$showInlineToolbarForRCodeChunks <- list(
   get = function() { .rs.getUserPref("show_inline_toolbar_for_r_code_chunks") },
   set = function(value) { .rs.setUserPref("show_inline_toolbar_for_r_code_chunks", value) }
)

# Highlight code chunks in R Markdown files
# background color.
.rs.uiPrefs$highlightCodeChunks <- list(
   get = function() { .rs.getUserPref("highlight_code_chunks") },
   set = function(value) { .rs.setUserPref("highlight_code_chunks", value) }
)

# Save files before building
# Whether to save all open, unsaved files before building the project.
.rs.uiPrefs$saveFilesBeforeBuild <- list(
   get = function() { .rs.getUserPref("save_files_before_build") },
   set = function(value) { .rs.setUserPref("save_files_before_build", value) }
)

# Save and reload R workspace on build
# project.
.rs.uiPrefs$saveAndReloadWorkspaceOnBuild <- list(
   get = function() { .rs.getUserPref("save_and_reload_workspace_on_build") },
   set = function(value) { .rs.setUserPref("save_and_reload_workspace_on_build", value) }
)

# Editor font size (points)
# The default editor font size, in points.
.rs.uiPrefs$fontSizePoints <- list(
   get = function() { .rs.getUserPref("font_size_points") },
   set = function(value) { .rs.setUserPref("font_size_points", value) }
)

# Help panel font size (points)
# The help panel font size, in points.
.rs.uiPrefs$helpFontSizePoints <- list(
   get = function() { .rs.getUserPref("help_font_size_points") },
   set = function(value) { .rs.setUserPref("help_font_size_points", value) }
)

# Theme
# The name of the color theme to apply to the text editor in RStudio.
.rs.uiPrefs$editorTheme <- list(
   get = function() { .rs.getUserPref("editor_theme") },
   set = function(value) { .rs.setUserPref("editor_theme", value) }
)

# Enable editor fonts on RStudio Server
# Whether to use a custom editor font in RStudio Server.
.rs.uiPrefs$serverEditorFontEnabled <- list(
   get = function() { .rs.getUserPref("server_editor_font_enabled") },
   set = function(value) { .rs.setUserPref("server_editor_font_enabled", value) }
)

# Editor font
# The name of the fixed-width editor font to use with RStudio Server.
.rs.uiPrefs$serverEditorFont <- list(
   get = function() { .rs.getUserPref("server_editor_font") },
   set = function(value) { .rs.setUserPref("server_editor_font", value) }
)

# Default character encoding
# The default character encoding to use when saving files.
.rs.uiPrefs$defaultEncoding <- list(
   get = function() { .rs.getUserPref("default_encoding") },
   set = function(value) { .rs.setUserPref("default_encoding", value) }
)

# Show top toolbar
# Whether to show the toolbar at the top of the RStudio workbench.
.rs.uiPrefs$toolbarVisible <- list(
   get = function() { .rs.getUserPref("toolbar_visible") },
   set = function(value) { .rs.setUserPref("toolbar_visible", value) }
)

# Default new project location
# The directory path under which to place new projects by default.
.rs.uiPrefs$defaultProjectLocation <- list(
   get = function() { .rs.getUserPref("default_project_location") },
   set = function(value) { .rs.setUserPref("default_project_location", value) }
)

# Default open project location
# The default directory to use in file dialogs when opening a project.
.rs.uiPrefs$defaultOpenProjectLocation <- list(
   get = function() { .rs.getUserPref("default_open_project_location") },
   set = function(value) { .rs.setUserPref("default_open_project_location", value) }
)

# Source with echo by default
# Whether to echo R code when sourcing it.
.rs.uiPrefs$sourceWithEcho <- list(
   get = function() { .rs.getUserPref("source_with_echo") },
   set = function(value) { .rs.setUserPref("source_with_echo", value) }
)

# Default Sweave engine
# The default engine to use when processing Sweave documents.
.rs.uiPrefs$defaultSweaveEngine <- list(
   get = function() { .rs.getUserPref("default_sweave_engine") },
   set = function(value) { .rs.setUserPref("default_sweave_engine", value) }
)

# Default LaTeX program
# The default program to use when processing LaTeX documents.
.rs.uiPrefs$defaultLatexProgram <- list(
   get = function() { .rs.getUserPref("default_latex_program") },
   set = function(value) { .rs.setUserPref("default_latex_program", value) }
)

# Use Roxygen for documentation
# Whether to use Roxygen for documentation.
.rs.uiPrefs$useRoxygen <- list(
   get = function() { .rs.getUserPref("use_roxygen") },
   set = function(value) { .rs.setUserPref("use_roxygen", value) }
)

# Enable data import
# Whether to use RStudio's data import feature.
.rs.uiPrefs$useDataimport <- list(
   get = function() { .rs.getUserPref("use_dataimport") },
   set = function(value) { .rs.setUserPref("use_dataimport", value) }
)

# PDF previewer
# The program to use to preview PDF files after generation.
.rs.uiPrefs$pdfPreviewer <- list(
   get = function() { .rs.getUserPref("pdf_previewer") },
   set = function(value) { .rs.setUserPref("pdf_previewer", value) }
)

# Enable Rnw concordance
# Whether to always enable the concordance for RNW files.
.rs.uiPrefs$alwaysEnableRnwConcordance <- list(
   get = function() { .rs.getUserPref("always_enable_rnw_concordance") },
   set = function(value) { .rs.setUserPref("always_enable_rnw_concordance", value) }
)

# Insert numbered LaTeX sections
# Whether to insert numbered sections in LaTeX.
.rs.uiPrefs$insertNumberedLatexSections <- list(
   get = function() { .rs.getUserPref("insert_numbered_latex_sections") },
   set = function(value) { .rs.setUserPref("insert_numbered_latex_sections", value) }
)

# Spelling dictionary language
# The language of the spelling dictionary to use for spell checking.
.rs.uiPrefs$spellingDictionaryLanguage <- list(
   get = function() { .rs.getUserPref("spelling_dictionary_language") },
   set = function(value) { .rs.setUserPref("spelling_dictionary_language", value) }
)

# Custom spelling dictionaries
# The list of custom dictionaries to use when spell checking.
.rs.uiPrefs$spellingCustomDictionaries <- list(
   get = function() { .rs.getUserPref("spelling_custom_dictionaries") },
   set = function(value) { .rs.setUserPref("spelling_custom_dictionaries", value) }
)

# Lint document after load (ms)
# loaded.
.rs.uiPrefs$documentLoadLintDelay <- list(
   get = function() { .rs.getUserPref("document_load_lint_delay") },
   set = function(value) { .rs.setUserPref("document_load_lint_delay", value) }
)

# Ignore uppercase words in spell check
# Whether to ignore words in uppercase when spell checking.
.rs.uiPrefs$ignoreUppercaseWords <- list(
   get = function() { .rs.getUserPref("ignore_uppercase_words") },
   set = function(value) { .rs.setUserPref("ignore_uppercase_words", value) }
)

# Ignore words with numbers in spell check
# Whether to ignore words with numbers in them when spell checking.
.rs.uiPrefs$ignoreWordsWithNumbers <- list(
   get = function() { .rs.getUserPref("ignore_words_with_numbers") },
   set = function(value) { .rs.setUserPref("ignore_words_with_numbers", value) }
)

# Use real-time spellchecking
# Whether to enable real-time spellchecking by default.
.rs.uiPrefs$realTimeSpellchecking <- list(
   get = function() { .rs.getUserPref("real_time_spellchecking") },
   set = function(value) { .rs.setUserPref("real_time_spellchecking", value) }
)

# Navigate to build errors
# Whether to navigate to build errors.
.rs.uiPrefs$navigateToBuildError <- list(
   get = function() { .rs.getUserPref("navigate_to_build_error") },
   set = function(value) { .rs.setUserPref("navigate_to_build_error", value) }
)

# Enable the Packages pane
# Whether to enable RStudio's Packages pane.
.rs.uiPrefs$packagesPaneEnabled <- list(
   get = function() { .rs.getUserPref("packages_pane_enabled") },
   set = function(value) { .rs.setUserPref("packages_pane_enabled", value) }
)

# C++ template
# C++ template.
.rs.uiPrefs$cppTemplate <- list(
   get = function() { .rs.getUserPref("cpp_template") },
   set = function(value) { .rs.setUserPref("cpp_template", value) }
)

# Restore last opened documents on startup
# Whether to restore the last opened source documents when RStudio starts up.
.rs.uiPrefs$restoreSourceDocuments <- list(
   get = function() { .rs.getUserPref("restore_source_documents") },
   set = function(value) { .rs.setUserPref("restore_source_documents", value) }
)

# Handle errors only when user code present
# Whether to handle errors only when user code is on the stack.
.rs.uiPrefs$handleErrorsInUserCodeOnly <- list(
   get = function() { .rs.getUserPref("handle_errors_in_user_code_only") },
   set = function(value) { .rs.setUserPref("handle_errors_in_user_code_only", value) }
)

# Auto-expand error tracebacks
# Whether to automatically expand tracebacks when an error occurs.
.rs.uiPrefs$autoExpandErrorTracebacks <- list(
   get = function() { .rs.getUserPref("auto_expand_error_tracebacks") },
   set = function(value) { .rs.setUserPref("auto_expand_error_tracebacks", value) }
)

# Check for new version at startup
# Whether to check for new versions of RStudio when RStudio starts.
.rs.uiPrefs$checkForUpdates <- list(
   get = function() { .rs.getUserPref("check_for_updates") },
   set = function(value) { .rs.setUserPref("check_for_updates", value) }
)

# Show internal functions when debugging
# debugging.
.rs.uiPrefs$showInternalFunctions <- list(
   get = function() { .rs.getUserPref("show_internal_functions") },
   set = function(value) { .rs.setUserPref("show_internal_functions", value) }
)

# Run Shiny applications in
# Where to display Shiny applications when they are run.
.rs.uiPrefs$shinyViewerType <- list(
   get = function() { .rs.getUserPref("shiny_viewer_type") },
   set = function(value) { .rs.setUserPref("shiny_viewer_type", value) }
)

# Run Shiny applications in the background
# Whether to run Shiny applications as background jobs.
.rs.uiPrefs$shinyBackgroundJobs <- list(
   get = function() { .rs.getUserPref("shiny_background_jobs") },
   set = function(value) { .rs.setUserPref("shiny_background_jobs", value) }
)

# Run Plumber APIs in
# Where to display Shiny applications when they are run.
.rs.uiPrefs$plumberViewerType <- list(
   get = function() { .rs.getUserPref("plumber_viewer_type") },
   set = function(value) { .rs.setUserPref("plumber_viewer_type", value) }
)

# Document author
# The default name to use as the document author when creating new documents.
.rs.uiPrefs$documentAuthor <- list(
   get = function() { .rs.getUserPref("document_author") },
   set = function(value) { .rs.setUserPref("document_author", value) }
)

# Use current date when rendering document
# Use current date when rendering document
.rs.uiPrefs$rmdAutoDate <- list(
   get = function() { .rs.getUserPref("rmd_auto_date") },
   set = function(value) { .rs.setUserPref("rmd_auto_date", value) }
)

# Path to preferred R Markdown template
# The path to the preferred R Markdown template.
.rs.uiPrefs$rmdPreferredTemplatePath <- list(
   get = function() { .rs.getUserPref("rmd_preferred_template_path") },
   set = function(value) { .rs.setUserPref("rmd_preferred_template_path", value) }
)

# Display R Markdown documents in
# Where to display R Markdown documents when they have completed rendering.
.rs.uiPrefs$rmdViewerType <- list(
   get = function() { .rs.getUserPref("rmd_viewer_type") },
   set = function(value) { .rs.setUserPref("rmd_viewer_type", value) }
)

# Show diagnostic info when publishing
# Whether to show verbose diagnostic information when publishing content.
.rs.uiPrefs$showPublishDiagnostics <- list(
   get = function() { .rs.getUserPref("show_publish_diagnostics") },
   set = function(value) { .rs.setUserPref("show_publish_diagnostics", value) }
)

# 
# Whether to show UI for publishing content to Posit Cloud.
.rs.uiPrefs$enableCloudPublishUi <- list(
   get = function() { .rs.getUserPref("enable_cloud_publish_ui") },
   set = function(value) { .rs.setUserPref("enable_cloud_publish_ui", value) }
)

# Check SSL certificates when publishing
# Whether to check remote server SSL certificates when publishing content.
.rs.uiPrefs$publishCheckCertificates <- list(
   get = function() { .rs.getUserPref("publish_check_certificates") },
   set = function(value) { .rs.setUserPref("publish_check_certificates", value) }
)

# Use custom CA bundle when publishing
# content.
.rs.uiPrefs$usePublishCaBundle <- list(
   get = function() { .rs.getUserPref("use_publish_ca_bundle") },
   set = function(value) { .rs.setUserPref("use_publish_ca_bundle", value) }
)

# Path to custom CA bundle for publishing
# content.
.rs.uiPrefs$publishCaBundle <- list(
   get = function() { .rs.getUserPref("publish_ca_bundle") },
   set = function(value) { .rs.setUserPref("publish_ca_bundle", value) }
)

# Show chunk output inline in all documents
# Whether to show chunk output inline for ordinary R Markdown documents.
.rs.uiPrefs$rmdChunkOutputInline <- list(
   get = function() { .rs.getUserPref("rmd_chunk_output_inline") },
   set = function(value) { .rs.setUserPref("rmd_chunk_output_inline", value) }
)

# Open document outline by default
# documents.
.rs.uiPrefs$showDocOutlineRmd <- list(
   get = function() { .rs.getUserPref("show_doc_outline_rmd") },
   set = function(value) { .rs.setUserPref("show_doc_outline_rmd", value) }
)

# Document outline font size
# The font size to use for items in the document outline.
.rs.uiPrefs$documentOutlineFontSize <- list(
   get = function() { .rs.getUserPref("document_outline_font_size") },
   set = function(value) { .rs.setUserPref("document_outline_font_size", value) }
)

# Automatically run Setup chunk when needed
# running other chunks.
.rs.uiPrefs$autoRunSetupChunk <- list(
   get = function() { .rs.getUserPref("auto_run_setup_chunk") },
   set = function(value) { .rs.setUserPref("auto_run_setup_chunk", value) }
)

# Hide console when running R Markdown chunks
# Whether to hide the R console when executing inline R Markdown chunks.
.rs.uiPrefs$hideConsoleOnChunkExecute <- list(
   get = function() { .rs.getUserPref("hide_console_on_chunk_execute") },
   set = function(value) { .rs.setUserPref("hide_console_on_chunk_execute", value) }
)

# Unit of R code execution
# The unit of R code to execute when the Execute command is invoked.
.rs.uiPrefs$executionBehavior <- list(
   get = function() { .rs.getUserPref("execution_behavior") },
   set = function(value) { .rs.setUserPref("execution_behavior", value) }
)

# Show the Terminal tab
# Whether to show the Terminal tab.
.rs.uiPrefs$showTerminalTab <- list(
   get = function() { .rs.getUserPref("show_terminal_tab") },
   set = function(value) { .rs.setUserPref("show_terminal_tab", value) }
)

# Use local echo in the Terminal
# Whether to use local echo in the Terminal.
.rs.uiPrefs$terminalLocalEcho <- list(
   get = function() { .rs.getUserPref("terminal_local_echo") },
   set = function(value) { .rs.setUserPref("terminal_local_echo", value) }
)

# Use websockets in the Terminal
# Whether to use websockets to communicate with the shell in the Terminal tab.
.rs.uiPrefs$terminalWebsockets <- list(
   get = function() { .rs.getUserPref("terminal_websockets") },
   set = function(value) { .rs.setUserPref("terminal_websockets", value) }
)

# Close Terminal pane after shell exit
# Whether to close the terminal pane after the shell exits.
.rs.uiPrefs$terminalCloseBehavior <- list(
   get = function() { .rs.getUserPref("terminal_close_behavior") },
   set = function(value) { .rs.setUserPref("terminal_close_behavior", value) }
)

# Save and restore system environment in Terminal tab
# Terminal.
.rs.uiPrefs$terminalTrackEnvironment <- list(
   get = function() { .rs.getUserPref("terminal_track_environment") },
   set = function(value) { .rs.setUserPref("terminal_track_environment", value) }
)

# Ignored environment variables
# environment variables within a Terminal. Environment variables in this list
.rs.uiPrefs$terminalIgnoredEnvironmentVariables <- list(
   get = function() { .rs.getUserPref("terminal_ignored_environment_variables") },
   set = function(value) { .rs.setUserPref("terminal_ignored_environment_variables", value) }
)

# Enable Terminal hooks
# the active version of Python on the PATH in new Terminal sessions.
.rs.uiPrefs$terminalHooks <- list(
   get = function() { .rs.getUserPref("terminal_hooks") },
   set = function(value) { .rs.setUserPref("terminal_hooks", value) }
)

# Terminal bell style
# Terminal bell style
.rs.uiPrefs$terminalBellStyle <- list(
   get = function() { .rs.getUserPref("terminal_bell_style") },
   set = function(value) { .rs.setUserPref("terminal_bell_style", value) }
)

# Terminal tab rendering engine
# browsers or graphics cards
.rs.uiPrefs$terminalRenderer <- list(
   get = function() { .rs.getUserPref("terminal_renderer") },
   set = function(value) { .rs.setUserPref("terminal_renderer", value) }
)

# Make links in Terminal clickable
# Whether web links displayed in the Terminal tab are made clickable.
.rs.uiPrefs$terminalWeblinks <- list(
   get = function() { .rs.getUserPref("terminal_weblinks") },
   set = function(value) { .rs.setUserPref("terminal_weblinks", value) }
)

# Show R Markdown render command
# Markdown tab.
.rs.uiPrefs$showRmdRenderCommand <- list(
   get = function() { .rs.getUserPref("show_rmd_render_command") },
   set = function(value) { .rs.setUserPref("show_rmd_render_command", value) }
)

# Enable dragging text in code editor
# it.
.rs.uiPrefs$enableTextDrag <- list(
   get = function() { .rs.getUserPref("enable_text_drag") },
   set = function(value) { .rs.setUserPref("enable_text_drag", value) }
)

# Show hidden files in Files pane
# Whether to show hidden files in the Files pane.
.rs.uiPrefs$showHiddenFiles <- list(
   get = function() { .rs.getUserPref("show_hidden_files") },
   set = function(value) { .rs.setUserPref("show_hidden_files", value) }
)

# Files always shown in the Files Pane
# regardless of whether hidden files are shown
.rs.uiPrefs$alwaysShownFiles <- list(
   get = function() { .rs.getUserPref("always_shown_files") },
   set = function(value) { .rs.setUserPref("always_shown_files", value) }
)

# Extensions always shown in the Files Pane
# shown in the Files Pane, regardless of whether hidden files are shown
.rs.uiPrefs$alwaysShownExtensions <- list(
   get = function() { .rs.getUserPref("always_shown_extensions") },
   set = function(value) { .rs.setUserPref("always_shown_extensions", value) }
)

# Sort file names naturally in Files pane
# file9.R
.rs.uiPrefs$sortFileNamesNaturally <- list(
   get = function() { .rs.getUserPref("sort_file_names_naturally") },
   set = function(value) { .rs.setUserPref("sort_file_names_naturally", value) }
)

# Synchronize the Files pane with the current working directory
# working directory in R changes.
.rs.uiPrefs$syncFilesPaneWorkingDir <- list(
   get = function() { .rs.getUserPref("sync_files_pane_working_dir") },
   set = function(value) { .rs.setUserPref("sync_files_pane_working_dir", value) }
)

# Jobs tab visibility
# The visibility of the Jobs tab.
.rs.uiPrefs$jobsTabVisibility <- list(
   get = function() { .rs.getUserPref("jobs_tab_visibility") },
   set = function(value) { .rs.setUserPref("jobs_tab_visibility", value) }
)

# 
# Whether to show the Workbench Jobs tab in RStudio Pro and RStudio Workbench.
.rs.uiPrefs$showLauncherJobsTab <- list(
   get = function() { .rs.getUserPref("show_launcher_jobs_tab") },
   set = function(value) { .rs.setUserPref("show_launcher_jobs_tab", value) }
)

# 
# Workbench.
.rs.uiPrefs$launcherJobsSort <- list(
   get = function() { .rs.getUserPref("launcher_jobs_sort") },
   set = function(value) { .rs.setUserPref("launcher_jobs_sort", value) }
)

# 
# How to detect busy status in the Terminal.
.rs.uiPrefs$busyDetection <- list(
   get = function() { .rs.getUserPref("busy_detection") },
   set = function(value) { .rs.setUserPref("busy_detection", value) }
)

# 
# A list of apps that should not be considered busy in the Terminal.
.rs.uiPrefs$busyExclusionList <- list(
   get = function() { .rs.getUserPref("busy_exclusion_list") },
   set = function(value) { .rs.setUserPref("busy_exclusion_list", value) }
)

# Working directory for knitting
# The working directory to use when knitting R Markdown documents.
.rs.uiPrefs$knitWorkingDir <- list(
   get = function() { .rs.getUserPref("knit_working_dir") },
   set = function(value) { .rs.setUserPref("knit_working_dir", value) }
)

# Show in Document Outline
# Which objects to show in the document outline pane.
.rs.uiPrefs$docOutlineShow <- list(
   get = function() { .rs.getUserPref("doc_outline_show") },
   set = function(value) { .rs.setUserPref("doc_outline_show", value) }
)

# Preview LaTeX equations on idle
# recently.
.rs.uiPrefs$latexPreviewOnCursorIdle <- list(
   get = function() { .rs.getUserPref("latex_preview_on_cursor_idle") },
   set = function(value) { .rs.setUserPref("latex_preview_on_cursor_idle", value) }
)

# Wrap around when going to previous/next tab
# Whether to wrap around when going to the previous or next editor tab.
.rs.uiPrefs$wrapTabNavigation <- list(
   get = function() { .rs.getUserPref("wrap_tab_navigation") },
   set = function(value) { .rs.setUserPref("wrap_tab_navigation", value) }
)

# Global theme
# The theme to use for the main RStudio user interface.
.rs.uiPrefs$globalTheme <- list(
   get = function() { .rs.getUserPref("global_theme") },
   set = function(value) { .rs.setUserPref("global_theme", value) }
)

# Ignore whitespace in VCS diffs
# Whether to ignore whitespace when generating diffs of version controlled files.
.rs.uiPrefs$gitDiffIgnoreWhitespace <- list(
   get = function() { .rs.getUserPref("git_diff_ignore_whitespace") },
   set = function(value) { .rs.setUserPref("git_diff_ignore_whitespace", value) }
)

# Sign git commits
# Whether to sign git commits.
.rs.uiPrefs$gitSignedCommits <- list(
   get = function() { .rs.getUserPref("git_signed_commits") },
   set = function(value) { .rs.setUserPref("git_signed_commits", value) }
)

# Double click to select in the Console
# Whether double-clicking should select a word in the Console pane.
.rs.uiPrefs$consoleDoubleClickSelect <- list(
   get = function() { .rs.getUserPref("console_double_click_select") },
   set = function(value) { .rs.setUserPref("console_double_click_select", value) }
)

# Warn when automatic session suspension is paused
# toolbar.
.rs.uiPrefs$consoleSuspendBlockedNotice <- list(
   get = function() { .rs.getUserPref("console_suspend_blocked_notice") },
   set = function(value) { .rs.setUserPref("console_suspend_blocked_notice", value) }
)

# Number of seconds to delay warning
# paused. Higher values for less frequent notices.
.rs.uiPrefs$consoleSuspendBlockedNoticeDelay <- list(
   get = function() { .rs.getUserPref("console_suspend_blocked_notice_delay") },
   set = function(value) { .rs.setUserPref("console_suspend_blocked_notice_delay", value) }
)

# Create a Git repo in new projects
# Whether a git repo should be initialized inside new projects by default.
.rs.uiPrefs$newProjGitInit <- list(
   get = function() { .rs.getUserPref("new_proj_git_init") },
   set = function(value) { .rs.setUserPref("new_proj_git_init", value) }
)

# Create an renv environment in new projects
# Whether an renv environment should be created inside new projects by default.
.rs.uiPrefs$newProjUseRenv <- list(
   get = function() { .rs.getUserPref("new_proj_use_renv") },
   set = function(value) { .rs.setUserPref("new_proj_use_renv", value) }
)

# Root document for PDF compilation
# The root document to use when compiling PDF documents.
.rs.uiPrefs$rootDocument <- list(
   get = function() { .rs.getUserPref("root_document") },
   set = function(value) { .rs.setUserPref("root_document", value) }
)

# Show user home page in RStudio Workbench
# When to show the server home page in RStudio Workbench.
.rs.uiPrefs$showUserHomePage <- list(
   get = function() { .rs.getUserPref("show_user_home_page") },
   set = function(value) { .rs.setUserPref("show_user_home_page", value) }
)

# 
# Whether to reuse sessions when opening projects in RStudio Workbench.
.rs.uiPrefs$reuseSessionsForProjectLinks <- list(
   get = function() { .rs.getUserPref("reuse_sessions_for_project_links") },
   set = function(value) { .rs.setUserPref("reuse_sessions_for_project_links", value) }
)

# Enable version control if available
# Whether to enable RStudio's version control system interface.
.rs.uiPrefs$vcsEnabled <- list(
   get = function() { .rs.getUserPref("vcs_enabled") },
   set = function(value) { .rs.setUserPref("vcs_enabled", value) }
)

# Auto-refresh state from version control
# Automatically refresh VCS status?
.rs.uiPrefs$vcsAutorefresh <- list(
   get = function() { .rs.getUserPref("vcs_autorefresh") },
   set = function(value) { .rs.setUserPref("vcs_autorefresh", value) }
)

# Path to Git executable
# The path to the Git executable to use.
.rs.uiPrefs$gitExePath <- list(
   get = function() { .rs.getUserPref("git_exe_path") },
   set = function(value) { .rs.setUserPref("git_exe_path", value) }
)

# Path to Subversion executable
# The path to the Subversion executable to use.
.rs.uiPrefs$svnExePath <- list(
   get = function() { .rs.getUserPref("svn_exe_path") },
   set = function(value) { .rs.setUserPref("svn_exe_path", value) }
)

# 
# The path to the terminal executable to use.
.rs.uiPrefs$terminalPath <- list(
   get = function() { .rs.getUserPref("terminal_path") },
   set = function(value) { .rs.setUserPref("terminal_path", value) }
)

# 
# The path to the SSH key file to use.
.rs.uiPrefs$rsaKeyPath <- list(
   get = function() { .rs.getUserPref("rsa_key_path") },
   set = function(value) { .rs.setUserPref("rsa_key_path", value) }
)

# 
# The encryption type to use for the SSH key file.
.rs.uiPrefs$sshKeyType <- list(
   get = function() { .rs.getUserPref("ssh_key_type") },
   set = function(value) { .rs.setUserPref("ssh_key_type", value) }
)

# Use the devtools R package if available
# Whether to use the devtools R package.
.rs.uiPrefs$useDevtools <- list(
   get = function() { .rs.getUserPref("use_devtools") },
   set = function(value) { .rs.setUserPref("use_devtools", value) }
)

# Always use --preclean when installing package
# Always use --preclean when installing package.
.rs.uiPrefs$cleanBeforeInstall <- list(
   get = function() { .rs.getUserPref("clean_before_install") },
   set = function(value) { .rs.setUserPref("clean_before_install", value) }
)

# Download R packages securely
# Whether to use secure downloads when fetching R packages.
.rs.uiPrefs$useSecureDownload <- list(
   get = function() { .rs.getUserPref("use_secure_download") },
   set = function(value) { .rs.setUserPref("use_secure_download", value) }
)

# Clean up temporary files after R CMD CHECK
# Whether to clean up temporary files after running R CMD CHECK.
.rs.uiPrefs$cleanupAfterRCmdCheck <- list(
   get = function() { .rs.getUserPref("cleanup_after_r_cmd_check") },
   set = function(value) { .rs.setUserPref("cleanup_after_r_cmd_check", value) }
)

# View directory after R CMD CHECK
# Whether to view the directory after running R CMD CHECK.
.rs.uiPrefs$viewDirAfterRCmdCheck <- list(
   get = function() { .rs.getUserPref("view_dir_after_r_cmd_check") },
   set = function(value) { .rs.setUserPref("view_dir_after_r_cmd_check", value) }
)

# Hide object files in the Files pane
# Whether to hide object files in the Files pane.
.rs.uiPrefs$hideObjectFiles <- list(
   get = function() { .rs.getUserPref("hide_object_files") },
   set = function(value) { .rs.setUserPref("hide_object_files", value) }
)

# Restore last project when starting RStudio
# Whether to restore the last project when starting RStudio.
.rs.uiPrefs$restoreLastProject <- list(
   get = function() { .rs.getUserPref("restore_last_project") },
   set = function(value) { .rs.setUserPref("restore_last_project", value) }
)

# Number of seconds for safe project startup
# started.
.rs.uiPrefs$projectSafeStartupSeconds <- list(
   get = function() { .rs.getUserPref("project_safe_startup_seconds") },
   set = function(value) { .rs.setUserPref("project_safe_startup_seconds", value) }
)

# Use tinytex to compile .tex files
# Use tinytex to compile .tex files.
.rs.uiPrefs$useTinytex <- list(
   get = function() { .rs.getUserPref("use_tinytex") },
   set = function(value) { .rs.setUserPref("use_tinytex", value) }
)

# Clean output after running Texi2Dvi
# Whether to clean output after running Texi2Dvi.
.rs.uiPrefs$cleanTexi2dviOutput <- list(
   get = function() { .rs.getUserPref("clean_texi2dvi_output") },
   set = function(value) { .rs.setUserPref("clean_texi2dvi_output", value) }
)

# Shell escape LaTeX documents
# Whether to enable shell escaping with LaTeX documents.
.rs.uiPrefs$latexShellEscape <- list(
   get = function() { .rs.getUserPref("latex_shell_escape") },
   set = function(value) { .rs.setUserPref("latex_shell_escape", value) }
)

# Restore project R version in RStudio Pro and RStudio Workbench
# RStudio Workbench.
.rs.uiPrefs$restoreProjectRVersion <- list(
   get = function() { .rs.getUserPref("restore_project_r_version") },
   set = function(value) { .rs.setUserPref("restore_project_r_version", value) }
)

# Clang verbosity level (0 - 2)
# The verbosity level to use with Clang (0 - 2)
.rs.uiPrefs$clangVerbose <- list(
   get = function() { .rs.getUserPref("clang_verbose") },
   set = function(value) { .rs.setUserPref("clang_verbose", value) }
)

# Submit crash reports to RStudio
# Whether to automatically submit crash reports to RStudio.
.rs.uiPrefs$submitCrashReports <- list(
   get = function() { .rs.getUserPref("submit_crash_reports") },
   set = function(value) { .rs.setUserPref("submit_crash_reports", value) }
)

# 
# The R version to use by default.
.rs.uiPrefs$defaultRVersion <- list(
   get = function() { .rs.getUserPref("default_r_version") },
   set = function(value) { .rs.setUserPref("default_r_version", value) }
)

# Maximum number of columns in data viewer
# The maximum number of columns to show at once in the data viewer.
.rs.uiPrefs$dataViewerMaxColumns <- list(
   get = function() { .rs.getUserPref("data_viewer_max_columns") },
   set = function(value) { .rs.setUserPref("data_viewer_max_columns", value) }
)

# Maximum number of character in data viewer cells
# The maximum number of characters to show in a data viewer cell.
.rs.uiPrefs$dataViewerMaxCellSize <- list(
   get = function() { .rs.getUserPref("data_viewer_max_cell_size") },
   set = function(value) { .rs.setUserPref("data_viewer_max_cell_size", value) }
)

# Enable support for screen readers
# Support accessibility aids such as screen readers.
.rs.uiPrefs$enableScreenReader <- list(
   get = function() { .rs.getUserPref("enable_screen_reader") },
   set = function(value) { .rs.setUserPref("enable_screen_reader", value) }
)

# Seconds to wait before updating ARIA live region
# region.
.rs.uiPrefs$typingStatusDelayMs <- list(
   get = function() { .rs.getUserPref("typing_status_delay_ms") },
   set = function(value) { .rs.setUserPref("typing_status_delay_ms", value) }
)

# Reduced animation/motion mode
# Reduce use of animations in the user interface.
.rs.uiPrefs$reducedMotion <- list(
   get = function() { .rs.getUserPref("reduced_motion") },
   set = function(value) { .rs.setUserPref("reduced_motion", value) }
)

# Tab key always moves focus
# Tab key moves focus out of text editing controls instead of inserting tabs.
.rs.uiPrefs$tabKeyMoveFocus <- list(
   get = function() { .rs.getUserPref("tab_key_move_focus") },
   set = function(value) { .rs.setUserPref("tab_key_move_focus", value) }
)

# Tab key moves focus directly from find text to replace text in find panel
# replace text.
.rs.uiPrefs$findPanelLegacyTabSequence <- list(
   get = function() { .rs.getUserPref("find_panel_legacy_tab_sequence") },
   set = function(value) { .rs.setUserPref("find_panel_legacy_tab_sequence", value) }
)

# Show focus outline around focused panel
# Show which panel contains keyboard focus.
.rs.uiPrefs$showPanelFocusRectangle <- list(
   get = function() { .rs.getUserPref("show_panel_focus_rectangle") },
   set = function(value) { .rs.setUserPref("show_panel_focus_rectangle", value) }
)

# Autosave mode on idle
# How to deal with changes to documents on idle.
.rs.uiPrefs$autoSaveOnIdle <- list(
   get = function() { .rs.getUserPref("auto_save_on_idle") },
   set = function(value) { .rs.setUserPref("auto_save_on_idle", value) }
)

# Idle period for document autosave (ms)
# The idle period, in milliseconds, after which documents should be auto-saved.
.rs.uiPrefs$autoSaveIdleMs <- list(
   get = function() { .rs.getUserPref("auto_save_idle_ms") },
   set = function(value) { .rs.setUserPref("auto_save_idle_ms", value) }
)

# Save documents when editor loses input focus
# Whether to automatically save when the editor loses focus.
.rs.uiPrefs$autoSaveOnBlur <- list(
   get = function() { .rs.getUserPref("auto_save_on_blur") },
   set = function(value) { .rs.setUserPref("auto_save_on_blur", value) }
)

# Initial working directory for new terminals
# Initial directory for new terminals.
.rs.uiPrefs$terminalInitialDirectory <- list(
   get = function() { .rs.getUserPref("terminal_initial_directory") },
   set = function(value) { .rs.setUserPref("terminal_initial_directory", value) }
)

# Show full path to project in RStudio Desktop windows
# Whether to show the full path to project in desktop window title.
.rs.uiPrefs$fullProjectPathInWindowTitle <- list(
   get = function() { .rs.getUserPref("full_project_path_in_window_title") },
   set = function(value) { .rs.setUserPref("full_project_path_in_window_title", value) }
)

# Use visual editing by default for new markdown documents
# Whether to enable visual editing by default for new markdown documents
.rs.uiPrefs$visualMarkdownEditingIsDefault <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_is_default") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_is_default", value) }
)

# Default list spacing in visual markdown editing mode
# Default spacing for lists created in the visual editor
.rs.uiPrefs$visualMarkdownEditingListSpacing <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_list_spacing") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_list_spacing", value) }
)

# Wrap text in visual markdown editing mode
# Whether to automatically wrap text when writing markdown
.rs.uiPrefs$visualMarkdownEditingWrap <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_wrap") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_wrap", value) }
)

# Wrap column for visual markdown editing mode
# The column to wrap text at when writing markdown
.rs.uiPrefs$visualMarkdownEditingWrapAtColumn <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_wrap_at_column") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_wrap_at_column", value) }
)

# Place visual markdown footnotes in
# Placement of footnotes within markdown output.
.rs.uiPrefs$visualMarkdownEditingReferencesLocation <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_references_location") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_references_location", value) }
)

# Write canonical visual mode markdown in source mode
# Whether to write canonical visual mode markdown when saving from source mode.
.rs.uiPrefs$visualMarkdownEditingCanonical <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_canonical") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_canonical", value) }
)

# Max content width for visual markdown editor (px)
# Maximum content width for visual editing mode, in pixels
.rs.uiPrefs$visualMarkdownEditingMaxContentWidth <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_max_content_width") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_max_content_width", value) }
)

# Show document outline in visual markdown editing mode
# documents in visual mode.
.rs.uiPrefs$visualMarkdownEditingShowDocOutline <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_show_doc_outline") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_show_doc_outline", value) }
)

# Show margin in visual mode code blocks
# Whether to show the margin guide in the visual mode code blocks.
.rs.uiPrefs$visualMarkdownEditingShowMargin <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_show_margin") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_show_margin", value) }
)

# Show line numbers in visual mode code blocks
# Whether to show line numbers in the code editors used in visual mode
.rs.uiPrefs$visualMarkdownCodeEditorLineNumbers <- list(
   get = function() { .rs.getUserPref("visual_markdown_code_editor_line_numbers") },
   set = function(value) { .rs.setUserPref("visual_markdown_code_editor_line_numbers", value) }
)

# Font size for visual editing mode
# The default visual editing mode font size, in points
.rs.uiPrefs$visualMarkdownEditingFontSizePoints <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_font_size_points") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_font_size_points", value) }
)

# Editor for code chunks in visual editing mode
# The name of the editor to use to provide code editing in visual mode
.rs.uiPrefs$visualMarkdownCodeEditor <- list(
   get = function() { .rs.getUserPref("visual_markdown_code_editor") },
   set = function(value) { .rs.setUserPref("visual_markdown_code_editor", value) }
)

# Zotero libraries
# Zotero libraries to insert citations from.
.rs.uiPrefs$zoteroLibraries <- list(
   get = function() { .rs.getUserPref("zotero_libraries") },
   set = function(value) { .rs.setUserPref("zotero_libraries", value) }
)

# 
# Preferred emoji skintone
.rs.uiPrefs$emojiSkintone <- list(
   get = function() { .rs.getUserPref("emoji_skintone") },
   set = function(value) { .rs.setUserPref("emoji_skintone", value) }
)

# Disabled aria-live announcements
# List of aria-live announcements to disable.
.rs.uiPrefs$disabledAriaLiveAnnouncements <- list(
   get = function() { .rs.getUserPref("disabled_aria_live_announcements") },
   set = function(value) { .rs.setUserPref("disabled_aria_live_announcements", value) }
)

# Maximum number of console lines to announce
# Maximum number of lines of console output announced after a command.
.rs.uiPrefs$screenreaderConsoleAnnounceLimit <- list(
   get = function() { .rs.getUserPref("screenreader_console_announce_limit") },
   set = function(value) { .rs.setUserPref("screenreader_console_announce_limit", value) }
)

# List of path components ignored by file monitor
# of these components.
.rs.uiPrefs$fileMonitorIgnoredComponents <- list(
   get = function() { .rs.getUserPref("file_monitor_ignored_components") },
   set = function(value) { .rs.setUserPref("file_monitor_ignored_components", value) }
)

# Install R package dependencies one at a time
# Whether to install R package dependencies one at a time.
.rs.uiPrefs$installPkgDepsIndividually <- list(
   get = function() { .rs.getUserPref("install_pkg_deps_individually") },
   set = function(value) { .rs.setUserPref("install_pkg_deps_individually", value) }
)

# R graphics backend
# R graphics backend.
.rs.uiPrefs$graphicsBackend <- list(
   get = function() { .rs.getUserPref("graphics_backend") },
   set = function(value) { .rs.setUserPref("graphics_backend", value) }
)

# R graphics antialiasing method
# Type of anti-aliasing to be used for generated R plots.
.rs.uiPrefs$graphicsAntialiasing <- list(
   get = function() { .rs.getUserPref("graphics_antialiasing") },
   set = function(value) { .rs.setUserPref("graphics_antialiasing", value) }
)

# Fixed-width font list for RStudio Server
# List of fixed-width fonts to check for browser support.
.rs.uiPrefs$browserFixedWidthFonts <- list(
   get = function() { .rs.getUserPref("browser_fixed_width_fonts") },
   set = function(value) { .rs.setUserPref("browser_fixed_width_fonts", value) }
)

# 
# The Python type.
.rs.uiPrefs$pythonType <- list(
   get = function() { .rs.getUserPref("python_type") },
   set = function(value) { .rs.setUserPref("python_type", value) }
)

# 
# The Python version.
.rs.uiPrefs$pythonVersion <- list(
   get = function() { .rs.getUserPref("python_version") },
   set = function(value) { .rs.setUserPref("python_version", value) }
)

# 
# The path to the default Python interpreter.
.rs.uiPrefs$pythonPath <- list(
   get = function() { .rs.getUserPref("python_path") },
   set = function(value) { .rs.setUserPref("python_path", value) }
)

# Save Retry Timeout
# The maximum amount of seconds of retry for save operations.
.rs.uiPrefs$saveRetryTimeout <- list(
   get = function() { .rs.getUserPref("save_retry_timeout") },
   set = function(value) { .rs.setUserPref("save_retry_timeout", value) }
)

# Use R's native pipe operator, |>
# |>
.rs.uiPrefs$insertNativePipeOperator <- list(
   get = function() { .rs.getUserPref("insert_native_pipe_operator") },
   set = function(value) { .rs.setUserPref("insert_native_pipe_operator", value) }
)

# Remember recently used items in Command Palette
# Whether to keep track of recently used commands in the Command Palette
.rs.uiPrefs$commandPaletteMru <- list(
   get = function() { .rs.getUserPref("command_palette_mru") },
   set = function(value) { .rs.setUserPref("command_palette_mru", value) }
)

# Show memory usage in Environment Pane
# Whether to compute and show memory usage in the Environment Pane
.rs.uiPrefs$showMemoryUsage <- list(
   get = function() { .rs.getUserPref("show_memory_usage") },
   set = function(value) { .rs.setUserPref("show_memory_usage", value) }
)

# Interval for requerying memory stats (seconds)
# disable)
.rs.uiPrefs$memoryQueryIntervalSeconds <- list(
   get = function() { .rs.getUserPref("memory_query_interval_seconds") },
   set = function(value) { .rs.setUserPref("memory_query_interval_seconds", value) }
)

# Enable terminal Python integration
# Python will be placed on the PATH.
.rs.uiPrefs$terminalPythonIntegration <- list(
   get = function() { .rs.getUserPref("terminal_python_integration") },
   set = function(value) { .rs.setUserPref("terminal_python_integration", value) }
)

# Session protocol debug logging
# Enable session protocol debug logging showing all session requests and events
.rs.uiPrefs$sessionProtocolDebug <- list(
   get = function() { .rs.getUserPref("session_protocol_debug") },
   set = function(value) { .rs.setUserPref("session_protocol_debug", value) }
)

# Automatically activate project Python environments
# RStudio will automatically activate this environment on startup.
.rs.uiPrefs$pythonProjectEnvironmentAutomaticActivate <- list(
   get = function() { .rs.getUserPref("python_project_environment_automatic_activate") },
   set = function(value) { .rs.setUserPref("python_project_environment_automatic_activate", value) }
)

# Check values in the Environment pane for null external pointers
# when building the Environment pane, and avoid introspecting their contents
.rs.uiPrefs$checkNullExternalPointers <- list(
   get = function() { .rs.getUserPref("check_null_external_pointers") },
   set = function(value) { .rs.setUserPref("check_null_external_pointers", value) }
)

# User Interface Language:
# The IDE's user-interface language.
.rs.uiPrefs$uiLanguage <- list(
   get = function() { .rs.getUserPref("ui_language") },
   set = function(value) { .rs.setUserPref("ui_language", value) }
)

# Auto hide menu bar
# Hide desktop menu bar until Alt key is pressed.
.rs.uiPrefs$autohideMenubar <- list(
   get = function() { .rs.getUserPref("autohide_menubar") },
   set = function(value) { .rs.setUserPref("autohide_menubar", value) }
)

# Use native file and message dialog boxes
# dialog boxes.
.rs.uiPrefs$nativeFileDialogs <- list(
   get = function() { .rs.getUserPref("native_file_dialogs") },
   set = function(value) { .rs.setUserPref("native_file_dialogs", value) }
)

# Discard pending console input on error
# error occurs.
.rs.uiPrefs$discardPendingConsoleInputOnError <- list(
   get = function() { .rs.getUserPref("discard_pending_console_input_on_error") },
   set = function(value) { .rs.setUserPref("discard_pending_console_input_on_error", value) }
)

# Editor scroll speed sensitivity
# value, the faster the scrolling.
.rs.uiPrefs$editorScrollMultiplier <- list(
   get = function() { .rs.getUserPref("editor_scroll_multiplier") },
   set = function(value) { .rs.setUserPref("editor_scroll_multiplier", value) }
)

# Text rendering
# Control how text is rendered within the IDE surface.
.rs.uiPrefs$textRendering <- list(
   get = function() { .rs.getUserPref("text_rendering") },
   set = function(value) { .rs.setUserPref("text_rendering", value) }
)

# Disable Electron accessibility support
# Disable Electron accessibility support.
.rs.uiPrefs$disableRendererAccessibility <- list(
   get = function() { .rs.getUserPref("disable_renderer_accessibility") },
   set = function(value) { .rs.setUserPref("disable_renderer_accessibility", value) }
)

# Enable GitHub Copilot
# When enabled, RStudio will use GitHub Copilot to provide code suggestions.
.rs.uiPrefs$copilotEnabled <- list(
   get = function() { .rs.getUserPref("copilot_enabled") },
   set = function(value) { .rs.setUserPref("copilot_enabled", value) }
)

# Show Copilot code suggestions:
# Control when Copilot code suggestions are displayed in the editor.
.rs.uiPrefs$copilotCompletionsTrigger <- list(
   get = function() { .rs.getUserPref("copilot_completions_trigger") },
   set = function(value) { .rs.setUserPref("copilot_completions_trigger", value) }
)

# GitHub Copilot completions delay
# after the cursor position has changed.
.rs.uiPrefs$copilotCompletionsDelay <- list(
   get = function() { .rs.getUserPref("copilot_completions_delay") },
   set = function(value) { .rs.setUserPref("copilot_completions_delay", value) }
)

# Pressing Tab key will prefer inserting:
# RStudio code completions are visible.
.rs.uiPrefs$copilotTabKeyBehavior <- list(
   get = function() { .rs.getUserPref("copilot_tab_key_behavior") },
   set = function(value) { .rs.setUserPref("copilot_tab_key_behavior", value) }
)

# Index project files with GitHub Copilot
# When enabled, RStudio will index project files with GitHub Copilot.
.rs.uiPrefs$copilotIndexingEnabled <- list(
   get = function() { .rs.getUserPref("copilot_indexing_enabled") },
   set = function(value) { .rs.setUserPref("copilot_indexing_enabled", value) }
)

# 
# User-provided name for the currently opened R project.
.rs.uiPrefs$projectName <- list(
   get = function() { .rs.getUserPref("project_name") },
   set = function(value) { .rs.setUserPref("project_name", value) }
)

# Default working directory for background jobs
# Default working directory in background job dialog.
.rs.uiPrefs$runBackgroundJobDefaultWorkingDir <- list(
   get = function() { .rs.getUserPref("run_background_job_default_working_dir") },
   set = function(value) { .rs.setUserPref("run_background_job_default_working_dir", value) }
)

# Code formatter
# The formatter to use when reformatting code.
.rs.uiPrefs$codeFormatter <- list(
   get = function() { .rs.getUserPref("code_formatter") },
   set = function(value) { .rs.setUserPref("code_formatter", value) }
)

# Use strict transformers when formatting code
# `styler` package documentation for more details.
.rs.uiPrefs$codeFormatterStylerStrict <- list(
   get = function() { .rs.getUserPref("code_formatter_styler_strict") },
   set = function(value) { .rs.setUserPref("code_formatter_styler_strict", value) }
)

# 
# The external command to be used when reformatting code.
.rs.uiPrefs$codeFormatterExternalCommand <- list(
   get = function() { .rs.getUserPref("code_formatter_external_command") },
   set = function(value) { .rs.setUserPref("code_formatter_external_command", value) }
)

# Reformat documents on save
# When set, the selected formatter will be used to reformat documents on save.
.rs.uiPrefs$reformatOnSave <- list(
   get = function() { .rs.getUserPref("reformat_on_save") },
   set = function(value) { .rs.setUserPref("reformat_on_save", value) }
)

# Default project user data directory
# The folder in which RStudio should store project .Rproj.user data.
.rs.uiPrefs$projectUserDataDirectory <- list(
   get = function() { .rs.getUserPref("project_user_data_directory") },
   set = function(value) { .rs.setUserPref("project_user_data_directory", value) }
)

#
# SessionUserPrefValues.R
#
# Copyright (C) 2024 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

#
# This file was automatically generated -- please do not modify it by hand.
#

.rs.setVar("uiPrefs", new.env(parent = emptyenv()))

# Run .Rprofile on resume
# Whether to run .Rprofile again after resuming a suspended R session.
.rs.uiPrefs$runRprofileOnResume <- list(
   get = function() { .rs.getUserPref("run_rprofile_on_resume") },
   set = function(value) { .rs.setUserPref("run_rprofile_on_resume", value) }
)

# Save workspace on quit
# Whether to save the workspace to an .Rdata file after the R session ends.
.rs.uiPrefs$saveWorkspace <- list(
   get = function() { .rs.getUserPref("save_workspace") },
   set = function(value) { .rs.setUserPref("save_workspace", value) }
)

# Load workspace on start
# Whether to load the workspace when the R session begins.
.rs.uiPrefs$loadWorkspace <- list(
   get = function() { .rs.getUserPref("load_workspace") },
   set = function(value) { .rs.setUserPref("load_workspace", value) }
)

# Initial working directory
# The initial working directory for new R sessions.
.rs.uiPrefs$initialWorkingDirectory <- list(
   get = function() { .rs.getUserPref("initial_working_directory") },
   set = function(value) { .rs.setUserPref("initial_working_directory", value) }
)

# 
# The CRAN mirror to use.
.rs.uiPrefs$cranMirror <- list(
   get = function() { .rs.getUserPref("cran_mirror") },
   set = function(value) { .rs.setUserPref("cran_mirror", value) }
)

# 
# The name of the default Bioconductor mirror.
.rs.uiPrefs$bioconductorMirrorName <- list(
   get = function() { .rs.getUserPref("bioconductor_mirror_name") },
   set = function(value) { .rs.setUserPref("bioconductor_mirror_name", value) }
)

# 
# The URL of the default Bioconductor mirror.
.rs.uiPrefs$bioconductorMirrorUrl <- list(
   get = function() { .rs.getUserPref("bioconductor_mirror_url") },
   set = function(value) { .rs.setUserPref("bioconductor_mirror_url", value) }
)

# Save R console history
# Whether to always save the R console history.
.rs.uiPrefs$alwaysSaveHistory <- list(
   get = function() { .rs.getUserPref("always_save_history") },
   set = function(value) { .rs.setUserPref("always_save_history", value) }
)

# Remove duplicates from console history
# Whether to remove duplicate entries from the R console history.
.rs.uiPrefs$removeHistoryDuplicates <- list(
   get = function() { .rs.getUserPref("remove_history_duplicates") },
   set = function(value) { .rs.setUserPref("remove_history_duplicates", value) }
)

# Show .Last.value in Environment pane
# Show the result of the last expression (.Last.value) in the Environment pane.
.rs.uiPrefs$showLastDotValue <- list(
   get = function() { .rs.getUserPref("show_last_dot_value") },
   set = function(value) { .rs.setUserPref("show_last_dot_value", value) }
)

# Line ending format
# The line ending format to use when saving files.
.rs.uiPrefs$lineEndingConversion <- list(
   get = function() { .rs.getUserPref("line_ending_conversion") },
   set = function(value) { .rs.setUserPref("line_ending_conversion", value) }
)

# Use newlines in Makefiles
# Whether to use newlines when saving Makefiles.
.rs.uiPrefs$useNewlinesInMakefiles <- list(
   get = function() { .rs.getUserPref("use_newlines_in_makefiles") },
   set = function(value) { .rs.setUserPref("use_newlines_in_makefiles", value) }
)

# 
# The terminal shell to use on Windows.
.rs.uiPrefs$windowsTerminalShell <- list(
   get = function() { .rs.getUserPref("windows_terminal_shell") },
   set = function(value) { .rs.setUserPref("windows_terminal_shell", value) }
)

# 
# The terminal shell to use on POSIX operating systems (MacOS and Linux).
.rs.uiPrefs$posixTerminalShell <- list(
   get = function() { .rs.getUserPref("posix_terminal_shell") },
   set = function(value) { .rs.setUserPref("posix_terminal_shell", value) }
)

# 
# The fully qualified path to the custom shell command to use in the Terminal
.rs.uiPrefs$customShellCommand <- list(
   get = function() { .rs.getUserPref("custom_shell_command") },
   set = function(value) { .rs.setUserPref("custom_shell_command", value) }
)

# 
# The command-line options to pass to the custom shell command.
.rs.uiPrefs$customShellOptions <- list(
   get = function() { .rs.getUserPref("custom_shell_options") },
   set = function(value) { .rs.setUserPref("custom_shell_options", value) }
)

# Show line numbers in editor
# Show line numbers in RStudio's code editor.
.rs.uiPrefs$showLineNumbers <- list(
   get = function() { .rs.getUserPref("show_line_numbers") },
   set = function(value) { .rs.setUserPref("show_line_numbers", value) }
)

# Use relative line numbers in editor
# Show relative, rather than absolute, line numbers in RStudio's code editor.
.rs.uiPrefs$relativeLineNumbers <- list(
   get = function() { .rs.getUserPref("relative_line_numbers") },
   set = function(value) { .rs.setUserPref("relative_line_numbers", value) }
)

# Highlight selected word in editor
# Highlight the selected word in RStudio's code editor.
.rs.uiPrefs$highlightSelectedWord <- list(
   get = function() { .rs.getUserPref("highlight_selected_word") },
   set = function(value) { .rs.setUserPref("highlight_selected_word", value) }
)

# Highlight selected line in editor
# Highlight the selected line in RStudio's code editor.
.rs.uiPrefs$highlightSelectedLine <- list(
   get = function() { .rs.getUserPref("highlight_selected_line") },
   set = function(value) { .rs.setUserPref("highlight_selected_line", value) }
)

# 
# Layout of panes in the RStudio workbench.
.rs.uiPrefs$panes <- list(
   get = function() { .rs.getUserPref("panes") },
   set = function(value) { .rs.setUserPref("panes", value) }
)

# Allow source columns
# Whether to enable the ability to add source columns to display.
.rs.uiPrefs$allowSourceColumns <- list(
   get = function() { .rs.getUserPref("allow_source_columns") },
   set = function(value) { .rs.setUserPref("allow_source_columns", value) }
)

# Insert spaces for Tab
# Whether to insert spaces when pressing the Tab key.
.rs.uiPrefs$useSpacesForTab <- list(
   get = function() { .rs.getUserPref("use_spaces_for_tab") },
   set = function(value) { .rs.setUserPref("use_spaces_for_tab", value) }
)

# Number of spaces for Tab
# The number of spaces to insert when pressing the Tab key.
.rs.uiPrefs$numSpacesForTab <- list(
   get = function() { .rs.getUserPref("num_spaces_for_tab") },
   set = function(value) { .rs.setUserPref("num_spaces_for_tab", value) }
)

# Auto-detect indentation in files
# Whether to automatically detect indentation settings from file contents.
.rs.uiPrefs$autoDetectIndentation <- list(
   get = function() { .rs.getUserPref("auto_detect_indentation") },
   set = function(value) { .rs.setUserPref("auto_detect_indentation", value) }
)

# Show margin in editor
# Whether to show the margin guide in the RStudio code editor.
.rs.uiPrefs$showMargin <- list(
   get = function() { .rs.getUserPref("show_margin") },
   set = function(value) { .rs.setUserPref("show_margin", value) }
)

# Use a blinking cursor
# Whether to flash the cursor off and on.
.rs.uiPrefs$blinkingCursor <- list(
   get = function() { .rs.getUserPref("blinking_cursor") },
   set = function(value) { .rs.setUserPref("blinking_cursor", value) }
)

# Margin column
# The number of columns of text after which the margin is shown.
.rs.uiPrefs$marginColumn <- list(
   get = function() { .rs.getUserPref("margin_column") },
   set = function(value) { .rs.setUserPref("margin_column", value) }
)

# Show invisible characters in editor
# Whether to show invisible characters, such as spaces and tabs, in the RStudio
.rs.uiPrefs$showInvisibles <- list(
   get = function() { .rs.getUserPref("show_invisibles") },
   set = function(value) { .rs.setUserPref("show_invisibles", value) }
)

# Indentation guides
# Style for indentation guides in the RStudio code editor.
.rs.uiPrefs$indentGuides <- list(
   get = function() { .rs.getUserPref("indent_guides") },
   set = function(value) { .rs.setUserPref("indent_guides", value) }
)

# Continue comments after adding new line
# Whether to continue comments (by inserting the comment character) after adding
.rs.uiPrefs$continueCommentsOnNewline <- list(
   get = function() { .rs.getUserPref("continue_comments_on_newline") },
   set = function(value) { .rs.setUserPref("continue_comments_on_newline", value) }
)

# Whether web links in comments are clickable
# Whether web links in comments are clickable.
.rs.uiPrefs$highlightWebLink <- list(
   get = function() { .rs.getUserPref("highlight_web_link") },
   set = function(value) { .rs.setUserPref("highlight_web_link", value) }
)

# Keybinding set for editor
# The keybindings to use in the RStudio code editor.
.rs.uiPrefs$editorKeybindings <- list(
   get = function() { .rs.getUserPref("editor_keybindings") },
   set = function(value) { .rs.setUserPref("editor_keybindings", value) }
)

# Auto-insert matching parentheses and brackets
# Whether to insert matching pairs, such as () and [], when the first is typed.
.rs.uiPrefs$insertMatching <- list(
   get = function() { .rs.getUserPref("insert_matching") },
   set = function(value) { .rs.setUserPref("insert_matching", value) }
)

# Insert spaces around = in R code
# Whether to insert spaces around the equals sign in R code.
.rs.uiPrefs$insertSpacesAroundEquals <- list(
   get = function() { .rs.getUserPref("insert_spaces_around_equals") },
   set = function(value) { .rs.setUserPref("insert_spaces_around_equals", value) }
)

# Insert parentheses after functions
# Whether to insert parentheses after function completions.
.rs.uiPrefs$insertParensAfterFunctionCompletion <- list(
   get = function() { .rs.getUserPref("insert_parens_after_function_completion") },
   set = function(value) { .rs.setUserPref("insert_parens_after_function_completion", value) }
)

# Complete multi-line statements with Tab
# Whether to attempt completion of multiple-line statements when pressing Tab.
.rs.uiPrefs$tabMultilineCompletion <- list(
   get = function() { .rs.getUserPref("tab_multiline_completion") },
   set = function(value) { .rs.setUserPref("tab_multiline_completion", value) }
)

# Use Tab to trigger autocompletion
# Whether to attempt completion of statements when pressing Tab.
.rs.uiPrefs$tabCompletion <- list(
   get = function() { .rs.getUserPref("tab_completion") },
   set = function(value) { .rs.setUserPref("tab_completion", value) }
)

# Show function help tooltips on idle
# Whether to show help tooltips for functions when the cursor has not been
.rs.uiPrefs$showHelpTooltipOnIdle <- list(
   get = function() { .rs.getUserPref("show_help_tooltip_on_idle") },
   set = function(value) { .rs.setUserPref("show_help_tooltip_on_idle", value) }
)

# Surround selections with
# Which kinds of delimiters can be used to surround the current selection.
.rs.uiPrefs$surroundSelection <- list(
   get = function() { .rs.getUserPref("surround_selection") },
   set = function(value) { .rs.setUserPref("surround_selection", value) }
)

# Enable code snippets
# Whether to enable code snippets in the RStudio code editor.
.rs.uiPrefs$enableSnippets <- list(
   get = function() { .rs.getUserPref("enable_snippets") },
   set = function(value) { .rs.setUserPref("enable_snippets", value) }
)

# Use code completion for R
# When to use auto-completion for R code in the RStudio code editor.
.rs.uiPrefs$codeCompletion <- list(
   get = function() { .rs.getUserPref("code_completion") },
   set = function(value) { .rs.setUserPref("code_completion", value) }
)

# Use code completion for other languages
# When to use auto-completion for other languages (such as JavaScript and SQL) in
.rs.uiPrefs$codeCompletionOther <- list(
   get = function() { .rs.getUserPref("code_completion_other") },
   set = function(value) { .rs.setUserPref("code_completion_other", value) }
)

# Use code completion in the R console
# Whether to always use code completion in the R console.
.rs.uiPrefs$consoleCodeCompletion <- list(
   get = function() { .rs.getUserPref("console_code_completion") },
   set = function(value) { .rs.setUserPref("console_code_completion", value) }
)

# Delay before completing code (ms)
# The number of milliseconds to wait before offering code suggestions.
.rs.uiPrefs$codeCompletionDelay <- list(
   get = function() { .rs.getUserPref("code_completion_delay") },
   set = function(value) { .rs.setUserPref("code_completion_delay", value) }
)

# Number of characters for code completion
# The number of characters in a symbol that can be entered before completions are
.rs.uiPrefs$codeCompletionCharacters <- list(
   get = function() { .rs.getUserPref("code_completion_characters") },
   set = function(value) { .rs.setUserPref("code_completion_characters", value) }
)

# Include all function arguments in completion list
# invocation.
.rs.uiPrefs$codeCompletionIncludeAlreadyUsed <- list(
   get = function() { .rs.getUserPref("code_completion_include_already_used") },
   set = function(value) { .rs.setUserPref("code_completion_include_already_used", value) }
)

# Show function signature tooltips
# Whether to show function signature tooltips during autocompletion.
.rs.uiPrefs$showFunctionSignatureTooltips <- list(
   get = function() { .rs.getUserPref("show_function_signature_tooltips") },
   set = function(value) { .rs.setUserPref("show_function_signature_tooltips", value) }
)

# Show data preview in autocompletion help popup
# Whether a data preview is shown in the autocompletion help popup for datasets
.rs.uiPrefs$showDataPreview <- list(
   get = function() { .rs.getUserPref("show_data_preview") },
   set = function(value) { .rs.setUserPref("show_data_preview", value) }
)

# Show diagnostics in R code
# Whether to show diagnostic messages (such as syntax and usage errors) for R
.rs.uiPrefs$showDiagnosticsR <- list(
   get = function() { .rs.getUserPref("show_diagnostics_r") },
   set = function(value) { .rs.setUserPref("show_diagnostics_r", value) }
)

# Show diagnostics in C++ code
# Whether to show diagnostic messages for C++ code as you type.
.rs.uiPrefs$showDiagnosticsCpp <- list(
   get = function() { .rs.getUserPref("show_diagnostics_cpp") },
   set = function(value) { .rs.setUserPref("show_diagnostics_cpp", value) }
)

# Show diagnostics in YAML code
# Whether to show diagnostic messages for YAML code as you type.
.rs.uiPrefs$showDiagnosticsYaml <- list(
   get = function() { .rs.getUserPref("show_diagnostics_yaml") },
   set = function(value) { .rs.setUserPref("show_diagnostics_yaml", value) }
)

# Show diagnostics in other languages
# Whether to show diagnostic messages for other types of code (not R, C++, or
.rs.uiPrefs$showDiagnosticsOther <- list(
   get = function() { .rs.getUserPref("show_diagnostics_other") },
   set = function(value) { .rs.setUserPref("show_diagnostics_other", value) }
)

# Show style diagnostics for R code
# Whether to show style diagnostics (suggestions for improving R code style)
.rs.uiPrefs$styleDiagnostics <- list(
   get = function() { .rs.getUserPref("style_diagnostics") },
   set = function(value) { .rs.setUserPref("style_diagnostics", value) }
)

# Check code for problems when saving
# Whether to check code for problems after saving it.
.rs.uiPrefs$diagnosticsOnSave <- list(
   get = function() { .rs.getUserPref("diagnostics_on_save") },
   set = function(value) { .rs.setUserPref("diagnostics_on_save", value) }
)

# Run R code diagnostics in the background
# Whether to run code diagnostics in the background, as you type.
.rs.uiPrefs$backgroundDiagnostics <- list(
   get = function() { .rs.getUserPref("background_diagnostics") },
   set = function(value) { .rs.setUserPref("background_diagnostics", value) }
)

# Run R code diagnostics after (ms)
# The number of milliseconds to delay before running code diagnostics in the
.rs.uiPrefs$backgroundDiagnosticsDelayMs <- list(
   get = function() { .rs.getUserPref("background_diagnostics_delay_ms") },
   set = function(value) { .rs.setUserPref("background_diagnostics_delay_ms", value) }
)

# Run diagnostics on R function calls
# Whether to run diagnostics in R function calls.
.rs.uiPrefs$diagnosticsInRFunctionCalls <- list(
   get = function() { .rs.getUserPref("diagnostics_in_r_function_calls") },
   set = function(value) { .rs.setUserPref("diagnostics_in_r_function_calls", value) }
)

# Check arguments to R function calls
# Whether to check arguments to R function calls.
.rs.uiPrefs$checkArgumentsToRFunctionCalls <- list(
   get = function() { .rs.getUserPref("check_arguments_to_r_function_calls") },
   set = function(value) { .rs.setUserPref("check_arguments_to_r_function_calls", value) }
)

# Check for unexpected assignments
# Whether to check for unexpected variable assignments inside R function calls.
.rs.uiPrefs$checkUnexpectedAssignmentInFunctionCall <- list(
   get = function() { .rs.getUserPref("check_unexpected_assignment_in_function_call") },
   set = function(value) { .rs.setUserPref("check_unexpected_assignment_in_function_call", value) }
)

# Warn when R variable used but not defined
# Whether to generate a warning if a variable is used without being defined in
.rs.uiPrefs$warnIfNoSuchVariableInScope <- list(
   get = function() { .rs.getUserPref("warn_if_no_such_variable_in_scope") },
   set = function(value) { .rs.setUserPref("warn_if_no_such_variable_in_scope", value) }
)

# Warn when R variable defined but not used
# Whether to generate a warning if a variable is defined without being used in
.rs.uiPrefs$warnVariableDefinedButNotUsed <- list(
   get = function() { .rs.getUserPref("warn_variable_defined_but_not_used") },
   set = function(value) { .rs.setUserPref("warn_variable_defined_but_not_used", value) }
)

# Detect missing R packages in the editor
# Whether to automatically discover and offer to install missing R package
.rs.uiPrefs$autoDiscoverPackageDependencies <- list(
   get = function() { .rs.getUserPref("auto_discover_package_dependencies") },
   set = function(value) { .rs.setUserPref("auto_discover_package_dependencies", value) }
)

# Ensure files end with a newline when saving
# Whether to ensure that source files end with a newline character.
.rs.uiPrefs$autoAppendNewline <- list(
   get = function() { .rs.getUserPref("auto_append_newline") },
   set = function(value) { .rs.setUserPref("auto_append_newline", value) }
)

# Strip trailing whitespace when saving
# Whether to strip trailing whitespace from each line when saving.
.rs.uiPrefs$stripTrailingWhitespace <- list(
   get = function() { .rs.getUserPref("strip_trailing_whitespace") },
   set = function(value) { .rs.setUserPref("strip_trailing_whitespace", value) }
)

# Restore cursor position when reopening files
# Whether to save the position of the cursor when a file is closed, restore it
.rs.uiPrefs$restoreSourceDocumentCursorPosition <- list(
   get = function() { .rs.getUserPref("restore_source_document_cursor_position") },
   set = function(value) { .rs.setUserPref("restore_source_document_cursor_position", value) }
)

# Re-indent code when pasting
# Whether to automatically re-indent code when it's pasted into RStudio.
.rs.uiPrefs$reindentOnPaste <- list(
   get = function() { .rs.getUserPref("reindent_on_paste") },
   set = function(value) { .rs.setUserPref("reindent_on_paste", value) }
)

# Vertically align function arguments
# Whether to vertically align arguments to R function calls during automatic
.rs.uiPrefs$verticallyAlignArgumentsIndent <- list(
   get = function() { .rs.getUserPref("vertically_align_arguments_indent") },
   set = function(value) { .rs.setUserPref("vertically_align_arguments_indent", value) }
)

# Soft-wrap source files
# Whether to soft-wrap source files, wrapping the text for display without
.rs.uiPrefs$softWrapRFiles <- list(
   get = function() { .rs.getUserPref("soft_wrap_r_files") },
   set = function(value) { .rs.setUserPref("soft_wrap_r_files", value) }
)

# Soft-wrap R Markdown files
# Whether to soft-wrap R Markdown files (and similar types such as R HTML and R
.rs.uiPrefs$softWrapRmdFiles <- list(
   get = function() { .rs.getUserPref("soft_wrap_rmd_files") },
   set = function(value) { .rs.setUserPref("soft_wrap_rmd_files", value) }
)

# Focus console after executing R code
# Whether to focus the R console after executing an R command from a script.
.rs.uiPrefs$focusConsoleAfterExec <- list(
   get = function() { .rs.getUserPref("focus_console_after_exec") },
   set = function(value) { .rs.setUserPref("focus_console_after_exec", value) }
)

# Fold style in editor
# The style of folding to use.
.rs.uiPrefs$foldStyle <- list(
   get = function() { .rs.getUserPref("fold_style") },
   set = function(value) { .rs.setUserPref("fold_style", value) }
)

# Save R scripts before sourcing
# Whether to automatically save scripts before executing them.
.rs.uiPrefs$saveBeforeSourcing <- list(
   get = function() { .rs.getUserPref("save_before_sourcing") },
   set = function(value) { .rs.setUserPref("save_before_sourcing", value) }
)

# Syntax highlighting in R console
# Whether to use syntax highlighting in the R console.
.rs.uiPrefs$syntaxColorConsole <- list(
   get = function() { .rs.getUserPref("syntax_color_console") },
   set = function(value) { .rs.setUserPref("syntax_color_console", value) }
)

# Different color for error output in R console
# Whether to display error, warning, and message output in a different color.
.rs.uiPrefs$highlightConsoleErrors <- list(
   get = function() { .rs.getUserPref("highlight_console_errors") },
   set = function(value) { .rs.setUserPref("highlight_console_errors", value) }
)

# Scroll past end of file
# Whether to allow scrolling past the end of a file.
.rs.uiPrefs$scrollPastEndOfDocument <- list(
   get = function() { .rs.getUserPref("scroll_past_end_of_document") },
   set = function(value) { .rs.setUserPref("scroll_past_end_of_document", value) }
)

# Highlight R function calls
# Whether to highlight R function calls in the code editor.
.rs.uiPrefs$highlightRFunctionCalls <- list(
   get = function() { .rs.getUserPref("highlight_r_function_calls") },
   set = function(value) { .rs.setUserPref("highlight_r_function_calls", value) }
)

# Enable preview of named and hexadecimal colors
# Whether to show preview for named and hexadecimal colors.
.rs.uiPrefs$colorPreview <- list(
   get = function() { .rs.getUserPref("color_preview") },
   set = function(value) { .rs.setUserPref("color_preview", value) }
)

# Use rainbow parentheses
# Whether to highlight parentheses in a variety of colors.
.rs.uiPrefs$rainbowParentheses <- list(
   get = function() { .rs.getUserPref("rainbow_parentheses") },
   set = function(value) { .rs.setUserPref("rainbow_parentheses", value) }
)

# Use rainbow fenced divs
# Whether to highlight fenced divs in a variety of colors.
.rs.uiPrefs$rainbowFencedDivs <- list(
   get = function() { .rs.getUserPref("rainbow_fenced_divs") },
   set = function(value) { .rs.setUserPref("rainbow_fenced_divs", value) }
)

# Maximum characters per line in R console
# The maximum number of characters to display in a single line in the R console.
.rs.uiPrefs$consoleLineLengthLimit <- list(
   get = function() { .rs.getUserPref("console_line_length_limit") },
   set = function(value) { .rs.setUserPref("console_line_length_limit", value) }
)

# Maximum lines in R console
# The maximum number of console actions to store and display in the console
.rs.uiPrefs$consoleMaxLines <- list(
   get = function() { .rs.getUserPref("console_max_lines") },
   set = function(value) { .rs.setUserPref("console_max_lines", value) }
)

# ANSI escape codes in R console
# How to treat ANSI escape codes in the console.
.rs.uiPrefs$ansiConsoleMode <- list(
   get = function() { .rs.getUserPref("ansi_console_mode") },
   set = function(value) { .rs.setUserPref("ansi_console_mode", value) }
)

# Limit visible console output
# Whether to only show a limited window of the total console output
.rs.uiPrefs$limitVisibleConsole <- list(
   get = function() { .rs.getUserPref("limit_visible_console") },
   set = function(value) { .rs.setUserPref("limit_visible_console", value) }
)

# Show toolbar on R Markdown chunks
# Whether to show a toolbar on code chunks in R Markdown documents.
.rs.uiPrefs$showInlineToolbarForRCodeChunks <- list(
   get = function() { .rs.getUserPref("show_inline_toolbar_for_r_code_chunks") },
   set = function(value) { .rs.setUserPref("show_inline_toolbar_for_r_code_chunks", value) }
)

# Highlight code chunks in R Markdown files
# Whether to highlight code chunks in R Markdown documents with a different
.rs.uiPrefs$highlightCodeChunks <- list(
   get = function() { .rs.getUserPref("highlight_code_chunks") },
   set = function(value) { .rs.setUserPref("highlight_code_chunks", value) }
)

# Save files before building
# Whether to save all open, unsaved files before building the project.
.rs.uiPrefs$saveFilesBeforeBuild <- list(
   get = function() { .rs.getUserPref("save_files_before_build") },
   set = function(value) { .rs.setUserPref("save_files_before_build", value) }
)

# Save and reload R workspace on build
# Whether RStudio should save and reload the R workspace when building the
.rs.uiPrefs$saveAndReloadWorkspaceOnBuild <- list(
   get = function() { .rs.getUserPref("save_and_reload_workspace_on_build") },
   set = function(value) { .rs.setUserPref("save_and_reload_workspace_on_build", value) }
)

# Editor font size (points)
# The default editor font size, in points.
.rs.uiPrefs$fontSizePoints <- list(
   get = function() { .rs.getUserPref("font_size_points") },
   set = function(value) { .rs.setUserPref("font_size_points", value) }
)

# Help panel font size (points)
# The help panel font size, in points.
.rs.uiPrefs$helpFontSizePoints <- list(
   get = function() { .rs.getUserPref("help_font_size_points") },
   set = function(value) { .rs.setUserPref("help_font_size_points", value) }
)

# Theme
# The name of the color theme to apply to the text editor in RStudio.
.rs.uiPrefs$editorTheme <- list(
   get = function() { .rs.getUserPref("editor_theme") },
   set = function(value) { .rs.setUserPref("editor_theme", value) }
)

# Enable editor fonts on RStudio Server
# Whether to use a custom editor font in RStudio Server.
.rs.uiPrefs$serverEditorFontEnabled <- list(
   get = function() { .rs.getUserPref("server_editor_font_enabled") },
   set = function(value) { .rs.setUserPref("server_editor_font_enabled", value) }
)

# Editor font
# The name of the fixed-width editor font to use with RStudio Server.
.rs.uiPrefs$serverEditorFont <- list(
   get = function() { .rs.getUserPref("server_editor_font") },
   set = function(value) { .rs.setUserPref("server_editor_font", value) }
)

# Default character encoding
# The default character encoding to use when saving files.
.rs.uiPrefs$defaultEncoding <- list(
   get = function() { .rs.getUserPref("default_encoding") },
   set = function(value) { .rs.setUserPref("default_encoding", value) }
)

# Show top toolbar
# Whether to show the toolbar at the top of the RStudio workbench.
.rs.uiPrefs$toolbarVisible <- list(
   get = function() { .rs.getUserPref("toolbar_visible") },
   set = function(value) { .rs.setUserPref("toolbar_visible", value) }
)

# Default new project location
# The directory path under which to place new projects by default.
.rs.uiPrefs$defaultProjectLocation <- list(
   get = function() { .rs.getUserPref("default_project_location") },
   set = function(value) { .rs.setUserPref("default_project_location", value) }
)

# Default open project location
# The default directory to use in file dialogs when opening a project.
.rs.uiPrefs$defaultOpenProjectLocation <- list(
   get = function() { .rs.getUserPref("default_open_project_location") },
   set = function(value) { .rs.setUserPref("default_open_project_location", value) }
)

# Source with echo by default
# Whether to echo R code when sourcing it.
.rs.uiPrefs$sourceWithEcho <- list(
   get = function() { .rs.getUserPref("source_with_echo") },
   set = function(value) { .rs.setUserPref("source_with_echo", value) }
)

# Default Sweave engine
# The default engine to use when processing Sweave documents.
.rs.uiPrefs$defaultSweaveEngine <- list(
   get = function() { .rs.getUserPref("default_sweave_engine") },
   set = function(value) { .rs.setUserPref("default_sweave_engine", value) }
)

# Default LaTeX program
# The default program to use when processing LaTeX documents.
.rs.uiPrefs$defaultLatexProgram <- list(
   get = function() { .rs.getUserPref("default_latex_program") },
   set = function(value) { .rs.setUserPref("default_latex_program", value) }
)

# Use Roxygen for documentation
# Whether to use Roxygen for documentation.
.rs.uiPrefs$useRoxygen <- list(
   get = function() { .rs.getUserPref("use_roxygen") },
   set = function(value) { .rs.setUserPref("use_roxygen", value) }
)

# Enable data import
# Whether to use RStudio's data import feature.
.rs.uiPrefs$useDataimport <- list(
   get = function() { .rs.getUserPref("use_dataimport") },
   set = function(value) { .rs.setUserPref("use_dataimport", value) }
)

# PDF previewer
# The program to use to preview PDF files after generation.
.rs.uiPrefs$pdfPreviewer <- list(
   get = function() { .rs.getUserPref("pdf_previewer") },
   set = function(value) { .rs.setUserPref("pdf_previewer", value) }
)

# Enable Rnw concordance
# Whether to always enable the concordance for RNW files.
.rs.uiPrefs$alwaysEnableRnwConcordance <- list(
   get = function() { .rs.getUserPref("always_enable_rnw_concordance") },
   set = function(value) { .rs.setUserPref("always_enable_rnw_concordance", value) }
)

# Insert numbered LaTeX sections
# Whether to insert numbered sections in LaTeX.
.rs.uiPrefs$insertNumberedLatexSections <- list(
   get = function() { .rs.getUserPref("insert_numbered_latex_sections") },
   set = function(value) { .rs.setUserPref("insert_numbered_latex_sections", value) }
)

# Spelling dictionary language
# The language of the spelling dictionary to use for spell checking.
.rs.uiPrefs$spellingDictionaryLanguage <- list(
   get = function() { .rs.getUserPref("spelling_dictionary_language") },
   set = function(value) { .rs.setUserPref("spelling_dictionary_language", value) }
)

# Custom spelling dictionaries
# The list of custom dictionaries to use when spell checking.
.rs.uiPrefs$spellingCustomDictionaries <- list(
   get = function() { .rs.getUserPref("spelling_custom_dictionaries") },
   set = function(value) { .rs.setUserPref("spelling_custom_dictionaries", value) }
)

# Lint document after load (ms)
# The number of milliseconds to wait before linting a document after it is
.rs.uiPrefs$documentLoadLintDelay <- list(
   get = function() { .rs.getUserPref("document_load_lint_delay") },
   set = function(value) { .rs.setUserPref("document_load_lint_delay", value) }
)

# Ignore uppercase words in spell check
# Whether to ignore words in uppercase when spell checking.
.rs.uiPrefs$ignoreUppercaseWords <- list(
   get = function() { .rs.getUserPref("ignore_uppercase_words") },
   set = function(value) { .rs.setUserPref("ignore_uppercase_words", value) }
)

# Ignore words with numbers in spell check
# Whether to ignore words with numbers in them when spell checking.
.rs.uiPrefs$ignoreWordsWithNumbers <- list(
   get = function() { .rs.getUserPref("ignore_words_with_numbers") },
   set = function(value) { .rs.setUserPref("ignore_words_with_numbers", value) }
)

# Use real-time spellchecking
# Whether to enable real-time spellchecking by default.
.rs.uiPrefs$realTimeSpellchecking <- list(
   get = function() { .rs.getUserPref("real_time_spellchecking") },
   set = function(value) { .rs.setUserPref("real_time_spellchecking", value) }
)

# Navigate to build errors
# Whether to navigate to build errors.
.rs.uiPrefs$navigateToBuildError <- list(
   get = function() { .rs.getUserPref("navigate_to_build_error") },
   set = function(value) { .rs.setUserPref("navigate_to_build_error", value) }
)

# Enable the Packages pane
# Whether to enable RStudio's Packages pane.
.rs.uiPrefs$packagesPaneEnabled <- list(
   get = function() { .rs.getUserPref("packages_pane_enabled") },
   set = function(value) { .rs.setUserPref("packages_pane_enabled", value) }
)

# C++ template
# C++ template.
.rs.uiPrefs$cppTemplate <- list(
   get = function() { .rs.getUserPref("cpp_template") },
   set = function(value) { .rs.setUserPref("cpp_template", value) }
)

# Restore last opened documents on startup
# Whether to restore the last opened source documents when RStudio starts up.
.rs.uiPrefs$restoreSourceDocuments <- list(
   get = function() { .rs.getUserPref("restore_source_documents") },
   set = function(value) { .rs.setUserPref("restore_source_documents", value) }
)

# Handle errors only when user code present
# Whether to handle errors only when user code is on the stack.
.rs.uiPrefs$handleErrorsInUserCodeOnly <- list(
   get = function() { .rs.getUserPref("handle_errors_in_user_code_only") },
   set = function(value) { .rs.setUserPref("handle_errors_in_user_code_only", value) }
)

# Auto-expand error tracebacks
# Whether to automatically expand tracebacks when an error occurs.
.rs.uiPrefs$autoExpandErrorTracebacks <- list(
   get = function() { .rs.getUserPref("auto_expand_error_tracebacks") },
   set = function(value) { .rs.setUserPref("auto_expand_error_tracebacks", value) }
)

# Check for new version at startup
# Whether to check for new versions of RStudio when RStudio starts.
.rs.uiPrefs$checkForUpdates <- list(
   get = function() { .rs.getUserPref("check_for_updates") },
   set = function(value) { .rs.setUserPref("check_for_updates", value) }
)

# Show internal functions when debugging
# Whether to show functions without source references in the Traceback pane while
.rs.uiPrefs$showInternalFunctions <- list(
   get = function() { .rs.getUserPref("show_internal_functions") },
   set = function(value) { .rs.setUserPref("show_internal_functions", value) }
)

# Run Shiny applications in
# Where to display Shiny applications when they are run.
.rs.uiPrefs$shinyViewerType <- list(
   get = function() { .rs.getUserPref("shiny_viewer_type") },
   set = function(value) { .rs.setUserPref("shiny_viewer_type", value) }
)

# Run Shiny applications in the background
# Whether to run Shiny applications as background jobs.
.rs.uiPrefs$shinyBackgroundJobs <- list(
   get = function() { .rs.getUserPref("shiny_background_jobs") },
   set = function(value) { .rs.setUserPref("shiny_background_jobs", value) }
)

# Run Plumber APIs in
# Where to display Shiny applications when they are run.
.rs.uiPrefs$plumberViewerType <- list(
   get = function() { .rs.getUserPref("plumber_viewer_type") },
   set = function(value) { .rs.setUserPref("plumber_viewer_type", value) }
)

# Document author
# The default name to use as the document author when creating new documents.
.rs.uiPrefs$documentAuthor <- list(
   get = function() { .rs.getUserPref("document_author") },
   set = function(value) { .rs.setUserPref("document_author", value) }
)

# Use current date when rendering document
# Use current date when rendering document
.rs.uiPrefs$rmdAutoDate <- list(
   get = function() { .rs.getUserPref("rmd_auto_date") },
   set = function(value) { .rs.setUserPref("rmd_auto_date", value) }
)

# Path to preferred R Markdown template
# The path to the preferred R Markdown template.
.rs.uiPrefs$rmdPreferredTemplatePath <- list(
   get = function() { .rs.getUserPref("rmd_preferred_template_path") },
   set = function(value) { .rs.setUserPref("rmd_preferred_template_path", value) }
)

# Display R Markdown documents in
# Where to display R Markdown documents when they have completed rendering.
.rs.uiPrefs$rmdViewerType <- list(
   get = function() { .rs.getUserPref("rmd_viewer_type") },
   set = function(value) { .rs.setUserPref("rmd_viewer_type", value) }
)

# Show diagnostic info when publishing
# Whether to show verbose diagnostic information when publishing content.
.rs.uiPrefs$showPublishDiagnostics <- list(
   get = function() { .rs.getUserPref("show_publish_diagnostics") },
   set = function(value) { .rs.setUserPref("show_publish_diagnostics", value) }
)

# 
# Whether to show UI for publishing content to Posit Cloud.
.rs.uiPrefs$enableCloudPublishUi <- list(
   get = function() { .rs.getUserPref("enable_cloud_publish_ui") },
   set = function(value) { .rs.setUserPref("enable_cloud_publish_ui", value) }
)

# Check SSL certificates when publishing
# Whether to check remote server SSL certificates when publishing content.
.rs.uiPrefs$publishCheckCertificates <- list(
   get = function() { .rs.getUserPref("publish_check_certificates") },
   set = function(value) { .rs.setUserPref("publish_check_certificates", value) }
)

# Use custom CA bundle when publishing
# Whether to use a custom certificate authority (CA) bundle when publishing
.rs.uiPrefs$usePublishCaBundle <- list(
   get = function() { .rs.getUserPref("use_publish_ca_bundle") },
   set = function(value) { .rs.setUserPref("use_publish_ca_bundle", value) }
)

# Path to custom CA bundle for publishing
# The path to the custom certificate authority (CA) bundle to use when publishing
.rs.uiPrefs$publishCaBundle <- list(
   get = function() { .rs.getUserPref("publish_ca_bundle") },
   set = function(value) { .rs.setUserPref("publish_ca_bundle", value) }
)

# Show chunk output inline in all documents
# Whether to show chunk output inline for ordinary R Markdown documents.
.rs.uiPrefs$rmdChunkOutputInline <- list(
   get = function() { .rs.getUserPref("rmd_chunk_output_inline") },
   set = function(value) { .rs.setUserPref("rmd_chunk_output_inline", value) }
)

# Open document outline by default
# Whether to show the document outline by default when opening R Markdown
.rs.uiPrefs$showDocOutlineRmd <- list(
   get = function() { .rs.getUserPref("show_doc_outline_rmd") },
   set = function(value) { .rs.setUserPref("show_doc_outline_rmd", value) }
)

# Document outline font size
# The font size to use for items in the document outline.
.rs.uiPrefs$documentOutlineFontSize <- list(
   get = function() { .rs.getUserPref("document_outline_font_size") },
   set = function(value) { .rs.setUserPref("document_outline_font_size", value) }
)

# Automatically run Setup chunk when needed
# Whether to automatically run an R Markdown document's Setup chunk before
.rs.uiPrefs$autoRunSetupChunk <- list(
   get = function() { .rs.getUserPref("auto_run_setup_chunk") },
   set = function(value) { .rs.setUserPref("auto_run_setup_chunk", value) }
)

# Hide console when running R Markdown chunks
# Whether to hide the R console when executing inline R Markdown chunks.
.rs.uiPrefs$hideConsoleOnChunkExecute <- list(
   get = function() { .rs.getUserPref("hide_console_on_chunk_execute") },
   set = function(value) { .rs.setUserPref("hide_console_on_chunk_execute", value) }
)

# Unit of R code execution
# The unit of R code to execute when the Execute command is invoked.
.rs.uiPrefs$executionBehavior <- list(
   get = function() { .rs.getUserPref("execution_behavior") },
   set = function(value) { .rs.setUserPref("execution_behavior", value) }
)

# Show the Terminal tab
# Whether to show the Terminal tab.
.rs.uiPrefs$showTerminalTab <- list(
   get = function() { .rs.getUserPref("show_terminal_tab") },
   set = function(value) { .rs.setUserPref("show_terminal_tab", value) }
)

# Use local echo in the Terminal
# Whether to use local echo in the Terminal.
.rs.uiPrefs$terminalLocalEcho <- list(
   get = function() { .rs.getUserPref("terminal_local_echo") },
   set = function(value) { .rs.setUserPref("terminal_local_echo", value) }
)

# Use websockets in the Terminal
# Whether to use websockets to communicate with the shell in the Terminal tab.
.rs.uiPrefs$terminalWebsockets <- list(
   get = function() { .rs.getUserPref("terminal_websockets") },
   set = function(value) { .rs.setUserPref("terminal_websockets", value) }
)

# Close Terminal pane after shell exit
# Whether to close the terminal pane after the shell exits.
.rs.uiPrefs$terminalCloseBehavior <- list(
   get = function() { .rs.getUserPref("terminal_close_behavior") },
   set = function(value) { .rs.setUserPref("terminal_close_behavior", value) }
)

# Save and restore system environment in Terminal tab
# Whether to track and save changes to system environment variables in the
.rs.uiPrefs$terminalTrackEnvironment <- list(
   get = function() { .rs.getUserPref("terminal_track_environment") },
   set = function(value) { .rs.setUserPref("terminal_track_environment", value) }
)

# Ignored environment variables
# will not be saved when a Terminal instance is saved and restored.
.rs.uiPrefs$terminalIgnoredEnvironmentVariables <- list(
   get = function() { .rs.getUserPref("terminal_ignored_environment_variables") },
   set = function(value) { .rs.setUserPref("terminal_ignored_environment_variables", value) }
)

# Enable Terminal hooks
# Enabled Terminal hooks? Required for Python terminal integration, which places
.rs.uiPrefs$terminalHooks <- list(
   get = function() { .rs.getUserPref("terminal_hooks") },
   set = function(value) { .rs.setUserPref("terminal_hooks", value) }
)

# Terminal bell style
# Terminal bell style
.rs.uiPrefs$terminalBellStyle <- list(
   get = function() { .rs.getUserPref("terminal_bell_style") },
   set = function(value) { .rs.setUserPref("terminal_bell_style", value) }
)

# Terminal tab rendering engine
# Terminal rendering engine: canvas is faster, dom may be needed for some
.rs.uiPrefs$terminalRenderer <- list(
   get = function() { .rs.getUserPref("terminal_renderer") },
   set = function(value) { .rs.setUserPref("terminal_renderer", value) }
)

# Make links in Terminal clickable
# Whether web links displayed in the Terminal tab are made clickable.
.rs.uiPrefs$terminalWeblinks <- list(
   get = function() { .rs.getUserPref("terminal_weblinks") },
   set = function(value) { .rs.setUserPref("terminal_weblinks", value) }
)

# Show R Markdown render command
# Whether to print the render command use to knit R Markdown documents in the R
.rs.uiPrefs$showRmdRenderCommand <- list(
   get = function() { .rs.getUserPref("show_rmd_render_command") },
   set = function(value) { .rs.setUserPref("show_rmd_render_command", value) }
)

# Enable dragging text in code editor
# Whether to enable moving text on the editing surface by clicking and dragging
.rs.uiPrefs$enableTextDrag <- list(
   get = function() { .rs.getUserPref("enable_text_drag") },
   set = function(value) { .rs.setUserPref("enable_text_drag", value) }
)

# Show hidden files in Files pane
# Whether to show hidden files in the Files pane.
.rs.uiPrefs$showHiddenFiles <- list(
   get = function() { .rs.getUserPref("show_hidden_files") },
   set = function(value) { .rs.setUserPref("show_hidden_files", value) }
)

# Files always shown in the Files Pane
# List of file names (case sensitive) that are always shown in the Files Pane,
.rs.uiPrefs$alwaysShownFiles <- list(
   get = function() { .rs.getUserPref("always_shown_files") },
   set = function(value) { .rs.setUserPref("always_shown_files", value) }
)

# Extensions always shown in the Files Pane
# List of file extensions (beginning with ., not case sensitive) that are always
.rs.uiPrefs$alwaysShownExtensions <- list(
   get = function() { .rs.getUserPref("always_shown_extensions") },
   set = function(value) { .rs.setUserPref("always_shown_extensions", value) }
)

# Sort file names naturally in Files pane
# Whether to sort file names naturally, so that e.g., file10.R comes after
.rs.uiPrefs$sortFileNamesNaturally <- list(
   get = function() { .rs.getUserPref("sort_file_names_naturally") },
   set = function(value) { .rs.setUserPref("sort_file_names_naturally", value) }
)

# Synchronize the Files pane with the current working directory
# Whether to change the directory in the Files pane automatically when the
.rs.uiPrefs$syncFilesPaneWorkingDir <- list(
   get = function() { .rs.getUserPref("sync_files_pane_working_dir") },
   set = function(value) { .rs.setUserPref("sync_files_pane_working_dir", value) }
)

# Jobs tab visibility
# The visibility of the Jobs tab.
.rs.uiPrefs$jobsTabVisibility <- list(
   get = function() { .rs.getUserPref("jobs_tab_visibility") },
   set = function(value) { .rs.setUserPref("jobs_tab_visibility", value) }
)

# 
# Whether to show the Workbench Jobs tab in RStudio Pro and RStudio Workbench.
.rs.uiPrefs$showLauncherJobsTab <- list(
   get = function() { .rs.getUserPref("show_launcher_jobs_tab") },
   set = function(value) { .rs.setUserPref("show_launcher_jobs_tab", value) }
)

# 
# How to sort jobs in the Workbench Jobs tab in RStudio Pro and RStudio
.rs.uiPrefs$launcherJobsSort <- list(
   get = function() { .rs.getUserPref("launcher_jobs_sort") },
   set = function(value) { .rs.setUserPref("launcher_jobs_sort", value) }
)

# 
# How to detect busy status in the Terminal.
.rs.uiPrefs$busyDetection <- list(
   get = function() { .rs.getUserPref("busy_detection") },
   set = function(value) { .rs.setUserPref("busy_detection", value) }
)

# 
# A list of apps that should not be considered busy in the Terminal.
.rs.uiPrefs$busyExclusionList <- list(
   get = function() { .rs.getUserPref("busy_exclusion_list") },
   set = function(value) { .rs.setUserPref("busy_exclusion_list", value) }
)

# Working directory for knitting
# The working directory to use when knitting R Markdown documents.
.rs.uiPrefs$knitWorkingDir <- list(
   get = function() { .rs.getUserPref("knit_working_dir") },
   set = function(value) { .rs.setUserPref("knit_working_dir", value) }
)

# Show in Document Outline
# Which objects to show in the document outline pane.
.rs.uiPrefs$docOutlineShow <- list(
   get = function() { .rs.getUserPref("doc_outline_show") },
   set = function(value) { .rs.setUserPref("doc_outline_show", value) }
)

# Preview LaTeX equations on idle
# When to preview LaTeX mathematical equations when cursor has not moved
.rs.uiPrefs$latexPreviewOnCursorIdle <- list(
   get = function() { .rs.getUserPref("latex_preview_on_cursor_idle") },
   set = function(value) { .rs.setUserPref("latex_preview_on_cursor_idle", value) }
)

# Wrap around when going to previous/next tab
# Whether to wrap around when going to the previous or next editor tab.
.rs.uiPrefs$wrapTabNavigation <- list(
   get = function() { .rs.getUserPref("wrap_tab_navigation") },
   set = function(value) { .rs.setUserPref("wrap_tab_navigation", value) }
)

# Global theme
# The theme to use for the main RStudio user interface.
.rs.uiPrefs$globalTheme <- list(
   get = function() { .rs.getUserPref("global_theme") },
   set = function(value) { .rs.setUserPref("global_theme", value) }
)

# Ignore whitespace in VCS diffs
# Whether to ignore whitespace when generating diffs of version controlled files.
.rs.uiPrefs$gitDiffIgnoreWhitespace <- list(
   get = function() { .rs.getUserPref("git_diff_ignore_whitespace") },
   set = function(value) { .rs.setUserPref("git_diff_ignore_whitespace", value) }
)

# Sign git commits
# Whether to sign git commits.
.rs.uiPrefs$gitSignedCommits <- list(
   get = function() { .rs.getUserPref("git_signed_commits") },
   set = function(value) { .rs.setUserPref("git_signed_commits", value) }
)

# Double click to select in the Console
# Whether double-clicking should select a word in the Console pane.
.rs.uiPrefs$consoleDoubleClickSelect <- list(
   get = function() { .rs.getUserPref("console_double_click_select") },
   set = function(value) { .rs.setUserPref("console_double_click_select", value) }
)

# Warn when automatic session suspension is paused
# Whether the 'Auto Suspension Blocked' icon should appear in the R Console
.rs.uiPrefs$consoleSuspendBlockedNotice <- list(
   get = function() { .rs.getUserPref("console_suspend_blocked_notice") },
   set = function(value) { .rs.setUserPref("console_suspend_blocked_notice", value) }
)

# Number of seconds to delay warning
# How long to wait before warning that automatic session suspension has been
.rs.uiPrefs$consoleSuspendBlockedNoticeDelay <- list(
   get = function() { .rs.getUserPref("console_suspend_blocked_notice_delay") },
   set = function(value) { .rs.setUserPref("console_suspend_blocked_notice_delay", value) }
)

# Create a Git repo in new projects
# Whether a git repo should be initialized inside new projects by default.
.rs.uiPrefs$newProjGitInit <- list(
   get = function() { .rs.getUserPref("new_proj_git_init") },
   set = function(value) { .rs.setUserPref("new_proj_git_init", value) }
)

# Create an renv environment in new projects
# Whether an renv environment should be created inside new projects by default.
.rs.uiPrefs$newProjUseRenv <- list(
   get = function() { .rs.getUserPref("new_proj_use_renv") },
   set = function(value) { .rs.setUserPref("new_proj_use_renv", value) }
)

# Root document for PDF compilation
# The root document to use when compiling PDF documents.
.rs.uiPrefs$rootDocument <- list(
   get = function() { .rs.getUserPref("root_document") },
   set = function(value) { .rs.setUserPref("root_document", value) }
)

# Show user home page in RStudio Workbench
# When to show the server home page in RStudio Workbench.
.rs.uiPrefs$showUserHomePage <- list(
   get = function() { .rs.getUserPref("show_user_home_page") },
   set = function(value) { .rs.setUserPref("show_user_home_page", value) }
)

# 
# Whether to reuse sessions when opening projects in RStudio Workbench.
.rs.uiPrefs$reuseSessionsForProjectLinks <- list(
   get = function() { .rs.getUserPref("reuse_sessions_for_project_links") },
   set = function(value) { .rs.setUserPref("reuse_sessions_for_project_links", value) }
)

# Enable version control if available
# Whether to enable RStudio's version control system interface.
.rs.uiPrefs$vcsEnabled <- list(
   get = function() { .rs.getUserPref("vcs_enabled") },
   set = function(value) { .rs.setUserPref("vcs_enabled", value) }
)

# Auto-refresh state from version control
# Automatically refresh VCS status?
.rs.uiPrefs$vcsAutorefresh <- list(
   get = function() { .rs.getUserPref("vcs_autorefresh") },
   set = function(value) { .rs.setUserPref("vcs_autorefresh", value) }
)

# Path to Git executable
# The path to the Git executable to use.
.rs.uiPrefs$gitExePath <- list(
   get = function() { .rs.getUserPref("git_exe_path") },
   set = function(value) { .rs.setUserPref("git_exe_path", value) }
)

# Path to Subversion executable
# The path to the Subversion executable to use.
.rs.uiPrefs$svnExePath <- list(
   get = function() { .rs.getUserPref("svn_exe_path") },
   set = function(value) { .rs.setUserPref("svn_exe_path", value) }
)

# 
# The path to the terminal executable to use.
.rs.uiPrefs$terminalPath <- list(
   get = function() { .rs.getUserPref("terminal_path") },
   set = function(value) { .rs.setUserPref("terminal_path", value) }
)

# 
# The path to the SSH key file to use.
.rs.uiPrefs$rsaKeyPath <- list(
   get = function() { .rs.getUserPref("rsa_key_path") },
   set = function(value) { .rs.setUserPref("rsa_key_path", value) }
)

# 
# The encryption type to use for the SSH key file.
.rs.uiPrefs$sshKeyType <- list(
   get = function() { .rs.getUserPref("ssh_key_type") },
   set = function(value) { .rs.setUserPref("ssh_key_type", value) }
)

# Use the devtools R package if available
# Whether to use the devtools R package.
.rs.uiPrefs$useDevtools <- list(
   get = function() { .rs.getUserPref("use_devtools") },
   set = function(value) { .rs.setUserPref("use_devtools", value) }
)

# Always use --preclean when installing package
# Always use --preclean when installing package.
.rs.uiPrefs$cleanBeforeInstall <- list(
   get = function() { .rs.getUserPref("clean_before_install") },
   set = function(value) { .rs.setUserPref("clean_before_install", value) }
)

# Download R packages securely
# Whether to use secure downloads when fetching R packages.
.rs.uiPrefs$useSecureDownload <- list(
   get = function() { .rs.getUserPref("use_secure_download") },
   set = function(value) { .rs.setUserPref("use_secure_download", value) }
)

# Clean up temporary files after R CMD CHECK
# Whether to clean up temporary files after running R CMD CHECK.
.rs.uiPrefs$cleanupAfterRCmdCheck <- list(
   get = function() { .rs.getUserPref("cleanup_after_r_cmd_check") },
   set = function(value) { .rs.setUserPref("cleanup_after_r_cmd_check", value) }
)

# View directory after R CMD CHECK
# Whether to view the directory after running R CMD CHECK.
.rs.uiPrefs$viewDirAfterRCmdCheck <- list(
   get = function() { .rs.getUserPref("view_dir_after_r_cmd_check") },
   set = function(value) { .rs.setUserPref("view_dir_after_r_cmd_check", value) }
)

# Hide object files in the Files pane
# Whether to hide object files in the Files pane.
.rs.uiPrefs$hideObjectFiles <- list(
   get = function() { .rs.getUserPref("hide_object_files") },
   set = function(value) { .rs.setUserPref("hide_object_files", value) }
)

# Restore last project when starting RStudio
# Whether to restore the last project when starting RStudio.
.rs.uiPrefs$restoreLastProject <- list(
   get = function() { .rs.getUserPref("restore_last_project") },
   set = function(value) { .rs.setUserPref("restore_last_project", value) }
)

# Number of seconds for safe project startup
# The number of seconds after which a project is deemed to have successfully
.rs.uiPrefs$projectSafeStartupSeconds <- list(
   get = function() { .rs.getUserPref("project_safe_startup_seconds") },
   set = function(value) { .rs.setUserPref("project_safe_startup_seconds", value) }
)

# Use tinytex to compile .tex files
# Use tinytex to compile .tex files.
.rs.uiPrefs$useTinytex <- list(
   get = function() { .rs.getUserPref("use_tinytex") },
   set = function(value) { .rs.setUserPref("use_tinytex", value) }
)

# Clean output after running Texi2Dvi
# Whether to clean output after running Texi2Dvi.
.rs.uiPrefs$cleanTexi2dviOutput <- list(
   get = function() { .rs.getUserPref("clean_texi2dvi_output") },
   set = function(value) { .rs.setUserPref("clean_texi2dvi_output", value) }
)

# Shell escape LaTeX documents
# Whether to enable shell escaping with LaTeX documents.
.rs.uiPrefs$latexShellEscape <- list(
   get = function() { .rs.getUserPref("latex_shell_escape") },
   set = function(value) { .rs.setUserPref("latex_shell_escape", value) }
)

# Restore project R version in RStudio Pro and RStudio Workbench
# Whether to restore the last version of R used by the project in RStudio Pro and
.rs.uiPrefs$restoreProjectRVersion <- list(
   get = function() { .rs.getUserPref("restore_project_r_version") },
   set = function(value) { .rs.setUserPref("restore_project_r_version", value) }
)

# Clang verbosity level (0 - 2)
# The verbosity level to use with Clang (0 - 2)
.rs.uiPrefs$clangVerbose <- list(
   get = function() { .rs.getUserPref("clang_verbose") },
   set = function(value) { .rs.setUserPref("clang_verbose", value) }
)

# Submit crash reports to RStudio
# Whether to automatically submit crash reports to RStudio.
.rs.uiPrefs$submitCrashReports <- list(
   get = function() { .rs.getUserPref("submit_crash_reports") },
   set = function(value) { .rs.setUserPref("submit_crash_reports", value) }
)

# 
# The R version to use by default.
.rs.uiPrefs$defaultRVersion <- list(
   get = function() { .rs.getUserPref("default_r_version") },
   set = function(value) { .rs.setUserPref("default_r_version", value) }
)

# Maximum number of columns in data viewer
# The maximum number of columns to show at once in the data viewer.
.rs.uiPrefs$dataViewerMaxColumns <- list(
   get = function() { .rs.getUserPref("data_viewer_max_columns") },
   set = function(value) { .rs.setUserPref("data_viewer_max_columns", value) }
)

# Maximum number of character in data viewer cells
# The maximum number of characters to show in a data viewer cell.
.rs.uiPrefs$dataViewerMaxCellSize <- list(
   get = function() { .rs.getUserPref("data_viewer_max_cell_size") },
   set = function(value) { .rs.setUserPref("data_viewer_max_cell_size", value) }
)

# Enable support for screen readers
# Support accessibility aids such as screen readers.
.rs.uiPrefs$enableScreenReader <- list(
   get = function() { .rs.getUserPref("enable_screen_reader") },
   set = function(value) { .rs.setUserPref("enable_screen_reader", value) }
)

# Seconds to wait before updating ARIA live region
# Number of milliseconds to wait after last keystroke before updating live
.rs.uiPrefs$typingStatusDelayMs <- list(
   get = function() { .rs.getUserPref("typing_status_delay_ms") },
   set = function(value) { .rs.setUserPref("typing_status_delay_ms", value) }
)

# Reduced animation/motion mode
# Reduce use of animations in the user interface.
.rs.uiPrefs$reducedMotion <- list(
   get = function() { .rs.getUserPref("reduced_motion") },
   set = function(value) { .rs.setUserPref("reduced_motion", value) }
)

# Tab key always moves focus
# Tab key moves focus out of text editing controls instead of inserting tabs.
.rs.uiPrefs$tabKeyMoveFocus <- list(
   get = function() { .rs.getUserPref("tab_key_move_focus") },
   set = function(value) { .rs.setUserPref("tab_key_move_focus", value) }
)

# Tab key moves focus directly from find text to replace text in find panel
# In source editor find panel, tab key moves focus directly from find text to
.rs.uiPrefs$findPanelLegacyTabSequence <- list(
   get = function() { .rs.getUserPref("find_panel_legacy_tab_sequence") },
   set = function(value) { .rs.setUserPref("find_panel_legacy_tab_sequence", value) }
)

# Show focus outline around focused panel
# Show which panel contains keyboard focus.
.rs.uiPrefs$showPanelFocusRectangle <- list(
   get = function() { .rs.getUserPref("show_panel_focus_rectangle") },
   set = function(value) { .rs.setUserPref("show_panel_focus_rectangle", value) }
)

# Autosave mode on idle
# How to deal with changes to documents on idle.
.rs.uiPrefs$autoSaveOnIdle <- list(
   get = function() { .rs.getUserPref("auto_save_on_idle") },
   set = function(value) { .rs.setUserPref("auto_save_on_idle", value) }
)

# Idle period for document autosave (ms)
# The idle period, in milliseconds, after which documents should be auto-saved.
.rs.uiPrefs$autoSaveIdleMs <- list(
   get = function() { .rs.getUserPref("auto_save_idle_ms") },
   set = function(value) { .rs.setUserPref("auto_save_idle_ms", value) }
)

# Save documents when editor loses input focus
# Whether to automatically save when the editor loses focus.
.rs.uiPrefs$autoSaveOnBlur <- list(
   get = function() { .rs.getUserPref("auto_save_on_blur") },
   set = function(value) { .rs.setUserPref("auto_save_on_blur", value) }
)

# Initial working directory for new terminals
# Initial directory for new terminals.
.rs.uiPrefs$terminalInitialDirectory <- list(
   get = function() { .rs.getUserPref("terminal_initial_directory") },
   set = function(value) { .rs.setUserPref("terminal_initial_directory", value) }
)

# Show full path to project in RStudio Desktop windows
# Whether to show the full path to project in desktop window title.
.rs.uiPrefs$fullProjectPathInWindowTitle <- list(
   get = function() { .rs.getUserPref("full_project_path_in_window_title") },
   set = function(value) { .rs.setUserPref("full_project_path_in_window_title", value) }
)

# Use visual editing by default for new markdown documents
# Whether to enable visual editing by default for new markdown documents
.rs.uiPrefs$visualMarkdownEditingIsDefault <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_is_default") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_is_default", value) }
)

# Default list spacing in visual markdown editing mode
# Default spacing for lists created in the visual editor
.rs.uiPrefs$visualMarkdownEditingListSpacing <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_list_spacing") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_list_spacing", value) }
)

# Wrap text in visual markdown editing mode
# Whether to automatically wrap text when writing markdown
.rs.uiPrefs$visualMarkdownEditingWrap <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_wrap") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_wrap", value) }
)

# Wrap column for visual markdown editing mode
# The column to wrap text at when writing markdown
.rs.uiPrefs$visualMarkdownEditingWrapAtColumn <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_wrap_at_column") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_wrap_at_column", value) }
)

# Place visual markdown footnotes in
# Placement of footnotes within markdown output.
.rs.uiPrefs$visualMarkdownEditingReferencesLocation <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_references_location") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_references_location", value) }
)

# Write canonical visual mode markdown in source mode
# Whether to write canonical visual mode markdown when saving from source mode.
.rs.uiPrefs$visualMarkdownEditingCanonical <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_canonical") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_canonical", value) }
)

# Max content width for visual markdown editor (px)
# Maximum content width for visual editing mode, in pixels
.rs.uiPrefs$visualMarkdownEditingMaxContentWidth <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_max_content_width") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_max_content_width", value) }
)

# Show document outline in visual markdown editing mode
# Whether to show the document outline by default when opening R Markdown
.rs.uiPrefs$visualMarkdownEditingShowDocOutline <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_show_doc_outline") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_show_doc_outline", value) }
)

# Show margin in visual mode code blocks
# Whether to show the margin guide in the visual mode code blocks.
.rs.uiPrefs$visualMarkdownEditingShowMargin <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_show_margin") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_show_margin", value) }
)

# Show line numbers in visual mode code blocks
# Whether to show line numbers in the code editors used in visual mode
.rs.uiPrefs$visualMarkdownCodeEditorLineNumbers <- list(
   get = function() { .rs.getUserPref("visual_markdown_code_editor_line_numbers") },
   set = function(value) { .rs.setUserPref("visual_markdown_code_editor_line_numbers", value) }
)

# Font size for visual editing mode
# The default visual editing mode font size, in points
.rs.uiPrefs$visualMarkdownEditingFontSizePoints <- list(
   get = function() { .rs.getUserPref("visual_markdown_editing_font_size_points") },
   set = function(value) { .rs.setUserPref("visual_markdown_editing_font_size_points", value) }
)

# Editor for code chunks in visual editing mode
# The name of the editor to use to provide code editing in visual mode
.rs.uiPrefs$visualMarkdownCodeEditor <- list(
   get = function() { .rs.getUserPref("visual_markdown_code_editor") },
   set = function(value) { .rs.setUserPref("visual_markdown_code_editor", value) }
)

# Zotero libraries
# Zotero libraries to insert citations from.
.rs.uiPrefs$zoteroLibraries <- list(
   get = function() { .rs.getUserPref("zotero_libraries") },
   set = function(value) { .rs.setUserPref("zotero_libraries", value) }
)

# 
# Preferred emoji skintone
.rs.uiPrefs$emojiSkintone <- list(
   get = function() { .rs.getUserPref("emoji_skintone") },
   set = function(value) { .rs.setUserPref("emoji_skintone", value) }
)

# Disabled aria-live announcements
# List of aria-live announcements to disable.
.rs.uiPrefs$disabledAriaLiveAnnouncements <- list(
   get = function() { .rs.getUserPref("disabled_aria_live_announcements") },
   set = function(value) { .rs.setUserPref("disabled_aria_live_announcements", value) }
)

# Maximum number of console lines to announce
# Maximum number of lines of console output announced after a command.
.rs.uiPrefs$screenreaderConsoleAnnounceLimit <- list(
   get = function() { .rs.getUserPref("screenreader_console_announce_limit") },
   set = function(value) { .rs.setUserPref("screenreader_console_announce_limit", value) }
)

# List of path components ignored by file monitor
# List of path components; file monitor will ignore paths containing one or more
.rs.uiPrefs$fileMonitorIgnoredComponents <- list(
   get = function() { .rs.getUserPref("file_monitor_ignored_components") },
   set = function(value) { .rs.setUserPref("file_monitor_ignored_components", value) }
)

# Install R package dependencies one at a time
# Whether to install R package dependencies one at a time.
.rs.uiPrefs$installPkgDepsIndividually <- list(
   get = function() { .rs.getUserPref("install_pkg_deps_individually") },
   set = function(value) { .rs.setUserPref("install_pkg_deps_individually", value) }
)

# R graphics backend
# R graphics backend.
.rs.uiPrefs$graphicsBackend <- list(
   get = function() { .rs.getUserPref("graphics_backend") },
   set = function(value) { .rs.setUserPref("graphics_backend", value) }
)

# R graphics antialiasing method
# Type of anti-aliasing to be used for generated R plots.
.rs.uiPrefs$graphicsAntialiasing <- list(
   get = function() { .rs.getUserPref("graphics_antialiasing") },
   set = function(value) { .rs.setUserPref("graphics_antialiasing", value) }
)

# Fixed-width font list for RStudio Server
# List of fixed-width fonts to check for browser support.
.rs.uiPrefs$browserFixedWidthFonts <- list(
   get = function() { .rs.getUserPref("browser_fixed_width_fonts") },
   set = function(value) { .rs.setUserPref("browser_fixed_width_fonts", value) }
)

# 
# The Python type.
.rs.uiPrefs$pythonType <- list(
   get = function() { .rs.getUserPref("python_type") },
   set = function(value) { .rs.setUserPref("python_type", value) }
)

# 
# The Python version.
.rs.uiPrefs$pythonVersion <- list(
   get = function() { .rs.getUserPref("python_version") },
   set = function(value) { .rs.setUserPref("python_version", value) }
)

# 
# The path to the default Python interpreter.
.rs.uiPrefs$pythonPath <- list(
   get = function() { .rs.getUserPref("python_path") },
   set = function(value) { .rs.setUserPref("python_path", value) }
)

# Save Retry Timeout
# The maximum amount of seconds of retry for save operations.
.rs.uiPrefs$saveRetryTimeout <- list(
   get = function() { .rs.getUserPref("save_retry_timeout") },
   set = function(value) { .rs.setUserPref("save_retry_timeout", value) }
)

# Use R's native pipe operator, |>
# Whether the Insert Pipe Operator command should use the native R pipe operator,
.rs.uiPrefs$insertNativePipeOperator <- list(
   get = function() { .rs.getUserPref("insert_native_pipe_operator") },
   set = function(value) { .rs.setUserPref("insert_native_pipe_operator", value) }
)

# Remember recently used items in Command Palette
# Whether to keep track of recently used commands in the Command Palette
.rs.uiPrefs$commandPaletteMru <- list(
   get = function() { .rs.getUserPref("command_palette_mru") },
   set = function(value) { .rs.setUserPref("command_palette_mru", value) }
)

# Show memory usage in Environment Pane
# Whether to compute and show memory usage in the Environment Pane
.rs.uiPrefs$showMemoryUsage <- list(
   get = function() { .rs.getUserPref("show_memory_usage") },
   set = function(value) { .rs.setUserPref("show_memory_usage", value) }
)

# Interval for requerying memory stats (seconds)
# How many seconds to wait between automatic requeries of memory statistics (0 to
.rs.uiPrefs$memoryQueryIntervalSeconds <- list(
   get = function() { .rs.getUserPref("memory_query_interval_seconds") },
   set = function(value) { .rs.setUserPref("memory_query_interval_seconds", value) }
)

# Enable terminal Python integration
# Enable Python terminal hooks. When enabled, the RStudio-configured version of
.rs.uiPrefs$terminalPythonIntegration <- list(
   get = function() { .rs.getUserPref("terminal_python_integration") },
   set = function(value) { .rs.setUserPref("terminal_python_integration", value) }
)

# Session protocol debug logging
# Enable session protocol debug logging showing all session requests and events
.rs.uiPrefs$sessionProtocolDebug <- list(
   get = function() { .rs.getUserPref("session_protocol_debug") },
   set = function(value) { .rs.setUserPref("session_protocol_debug", value) }
)

# Automatically activate project Python environments
# When enabled, if the active project contains a Python virtual environment, then
.rs.uiPrefs$pythonProjectEnvironmentAutomaticActivate <- list(
   get = function() { .rs.getUserPref("python_project_environment_automatic_activate") },
   set = function(value) { .rs.setUserPref("python_project_environment_automatic_activate", value) }
)

# Check values in the Environment pane for null external pointers
# further.
.rs.uiPrefs$checkNullExternalPointers <- list(
   get = function() { .rs.getUserPref("check_null_external_pointers") },
   set = function(value) { .rs.setUserPref("check_null_external_pointers", value) }
)

# User Interface Language:
# The IDE's user-interface language.
.rs.uiPrefs$uiLanguage <- list(
   get = function() { .rs.getUserPref("ui_language") },
   set = function(value) { .rs.setUserPref("ui_language", value) }
)

# Auto hide menu bar
# Hide desktop menu bar until Alt key is pressed.
.rs.uiPrefs$autohideMenubar <- list(
   get = function() { .rs.getUserPref("autohide_menubar") },
   set = function(value) { .rs.setUserPref("autohide_menubar", value) }
)

# Use native file and message dialog boxes
# Whether RStudio Desktop will use the operating system's native File and Message
.rs.uiPrefs$nativeFileDialogs <- list(
   get = function() { .rs.getUserPref("native_file_dialogs") },
   set = function(value) { .rs.setUserPref("native_file_dialogs", value) }
)

# Discard pending console input on error
# When enabled, any pending console input will be discarded when an (uncaught) R
.rs.uiPrefs$discardPendingConsoleInputOnError <- list(
   get = function() { .rs.getUserPref("discard_pending_console_input_on_error") },
   set = function(value) { .rs.setUserPref("discard_pending_console_input_on_error", value) }
)

# Editor scroll speed sensitivity
# An integer value, 1-200, to set the editor scroll multiplier. The higher the
.rs.uiPrefs$editorScrollMultiplier <- list(
   get = function() { .rs.getUserPref("editor_scroll_multiplier") },
   set = function(value) { .rs.setUserPref("editor_scroll_multiplier", value) }
)

# Text rendering
# Control how text is rendered within the IDE surface.
.rs.uiPrefs$textRendering <- list(
   get = function() { .rs.getUserPref("text_rendering") },
   set = function(value) { .rs.setUserPref("text_rendering", value) }
)

# Disable Electron accessibility support
# Disable Electron accessibility support.
.rs.uiPrefs$disableRendererAccessibility <- list(
   get = function() { .rs.getUserPref("disable_renderer_accessibility") },
   set = function(value) { .rs.setUserPref("disable_renderer_accessibility", value) }
)

# Enable GitHub Copilot
# When enabled, RStudio will use GitHub Copilot to provide code suggestions.
.rs.uiPrefs$copilotEnabled <- list(
   get = function() { .rs.getUserPref("copilot_enabled") },
   set = function(value) { .rs.setUserPref("copilot_enabled", value) }
)

# Show Copilot code suggestions:
# Control when Copilot code suggestions are displayed in the editor.
.rs.uiPrefs$copilotCompletionsTrigger <- list(
   get = function() { .rs.getUserPref("copilot_completions_trigger") },
   set = function(value) { .rs.setUserPref("copilot_completions_trigger", value) }
)

# GitHub Copilot completions delay
# The delay (in milliseconds) before GitHub Copilot completions are requested
.rs.uiPrefs$copilotCompletionsDelay <- list(
   get = function() { .rs.getUserPref("copilot_completions_delay") },
   set = function(value) { .rs.setUserPref("copilot_completions_delay", value) }
)

# Pressing Tab key will prefer inserting:
# Control the behavior of the Tab key when both Copilot code suggestions and
.rs.uiPrefs$copilotTabKeyBehavior <- list(
   get = function() { .rs.getUserPref("copilot_tab_key_behavior") },
   set = function(value) { .rs.setUserPref("copilot_tab_key_behavior", value) }
)

# Index project files with GitHub Copilot
# When enabled, RStudio will index project files with GitHub Copilot.
.rs.uiPrefs$copilotIndexingEnabled <- list(
   get = function() { .rs.getUserPref("copilot_indexing_enabled") },
   set = function(value) { .rs.setUserPref("copilot_indexing_enabled", value) }
)

# 
# User-provided name for the currently opened R project.
.rs.uiPrefs$projectName <- list(
   get = function() { .rs.getUserPref("project_name") },
   set = function(value) { .rs.setUserPref("project_name", value) }
)

# Default working directory for background jobs
# Default working directory in background job dialog.
.rs.uiPrefs$runBackgroundJobDefaultWorkingDir <- list(
   get = function() { .rs.getUserPref("run_background_job_default_working_dir") },
   set = function(value) { .rs.setUserPref("run_background_job_default_working_dir", value) }
)

# Code formatter
# The formatter to use when reformatting code.
.rs.uiPrefs$codeFormatter <- list(
   get = function() { .rs.getUserPref("code_formatter") },
   set = function(value) { .rs.setUserPref("code_formatter", value) }
)

# Use strict transformers when formatting code
# When set, strict transformers will be used when formatting code. See the
.rs.uiPrefs$codeFormatterStylerStrict <- list(
   get = function() { .rs.getUserPref("code_formatter_styler_strict") },
   set = function(value) { .rs.setUserPref("code_formatter_styler_strict", value) }
)

# 
# The external command to be used when reformatting code.
.rs.uiPrefs$codeFormatterExternalCommand <- list(
   get = function() { .rs.getUserPref("code_formatter_external_command") },
   set = function(value) { .rs.setUserPref("code_formatter_external_command", value) }
)

# Reformat documents on save
# When set, the selected formatter will be used to reformat documents on save.
.rs.uiPrefs$reformatOnSave <- list(
   get = function() { .rs.getUserPref("reformat_on_save") },
   set = function(value) { .rs.setUserPref("reformat_on_save", value) }
)

# Default project user data directory
# The folder in which RStudio should store project .Rproj.user data.
.rs.uiPrefs$projectUserDataDirectory <- list(
   get = function() { .rs.getUserPref("project_user_data_directory") },
   set = function(value) { .rs.setUserPref("project_user_data_directory", value) }
)

