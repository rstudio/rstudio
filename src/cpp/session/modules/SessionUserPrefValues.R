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

# Run .Rprofile on resume
# Whether to run .Rprofile again after resuming a suspended R session.
.rs.addFunction("uiPrefs.runRprofileOnResume", function()
{
   .rs.getUserPref("run_rprofile_on_resume")
})


# Save workspace on quit
# Whether to save the workspace to an .Rdata file after the R session ends.
.rs.addFunction("uiPrefs.saveWorkspace", function()
{
   .rs.getUserPref("save_workspace")
})


# Load workspace on start
# Whether to load the workspace when the R session begins.
.rs.addFunction("uiPrefs.loadWorkspace", function()
{
   .rs.getUserPref("load_workspace")
})


# Initial working directory
# The initial working directory for new R sessions.
.rs.addFunction("uiPrefs.initialWorkingDirectory", function()
{
   .rs.getUserPref("initial_working_directory")
})


# 
# The CRAN mirror to use.
.rs.addFunction("uiPrefs.cranMirror", function()
{
   .rs.getUserPref("cran_mirror")
})


# 
# The name of the default Bioconductor mirror.
.rs.addFunction("uiPrefs.bioconductorMirrorName", function()
{
   .rs.getUserPref("bioconductor_mirror_name")
})


# 
# The URL of the default Bioconductor mirror.
.rs.addFunction("uiPrefs.bioconductorMirrorUrl", function()
{
   .rs.getUserPref("bioconductor_mirror_url")
})


# Save R console history
# Whether to always save the R console history.
.rs.addFunction("uiPrefs.alwaysSaveHistory", function()
{
   .rs.getUserPref("always_save_history")
})


# Remove duplicates from console history
# Whether to remove duplicate entries from the R console history.
.rs.addFunction("uiPrefs.removeHistoryDuplicates", function()
{
   .rs.getUserPref("remove_history_duplicates")
})


# Show .Last.value in Environment pane
# Show the result of the last expression (.Last.value) in the Environment pane.
.rs.addFunction("uiPrefs.showLastDotValue", function()
{
   .rs.getUserPref("show_last_dot_value")
})


# Line ending format
# The line ending format to use when saving files.
.rs.addFunction("uiPrefs.lineEndingConversion", function()
{
   .rs.getUserPref("line_ending_conversion")
})


# Use newlines in Makefiles
# Whether to use newlines when saving Makefiles.
.rs.addFunction("uiPrefs.useNewlinesInMakefiles", function()
{
   .rs.getUserPref("use_newlines_in_makefiles")
})


# 
# The terminal shell to use on Windows.
.rs.addFunction("uiPrefs.windowsTerminalShell", function()
{
   .rs.getUserPref("windows_terminal_shell")
})


# 
# The terminal shell to use on POSIX operating systems (MacOS and Linux).
.rs.addFunction("uiPrefs.posixTerminalShell", function()
{
   .rs.getUserPref("posix_terminal_shell")
})


# 
# The fully qualified path to the custom shell command to use in the Terminal
.rs.addFunction("uiPrefs.customShellCommand", function()
{
   .rs.getUserPref("custom_shell_command")
})


# 
# The command-line options to pass to the custom shell command.
.rs.addFunction("uiPrefs.customShellOptions", function()
{
   .rs.getUserPref("custom_shell_options")
})


# Show line numbers in editor
# Show line numbers in RStudio's code editor.
.rs.addFunction("uiPrefs.showLineNumbers", function()
{
   .rs.getUserPref("show_line_numbers")
})


# Use relative line numbers in editor
# Show relative, rather than absolute, line numbers in RStudio's code editor.
.rs.addFunction("uiPrefs.relativeLineNumbers", function()
{
   .rs.getUserPref("relative_line_numbers")
})


# Highlight selected word in editor
# Highlight the selected word in RStudio's code editor.
.rs.addFunction("uiPrefs.highlightSelectedWord", function()
{
   .rs.getUserPref("highlight_selected_word")
})


# Highlight selected line in editor
# Highlight the selected line in RStudio's code editor.
.rs.addFunction("uiPrefs.highlightSelectedLine", function()
{
   .rs.getUserPref("highlight_selected_line")
})


# 
# Layout of panes in the RStudio workbench.
.rs.addFunction("uiPrefs.panes", function()
{
   .rs.getUserPref("panes")
})


# Allow source columns
# Whether to enable the ability to add source columns to display.
.rs.addFunction("uiPrefs.allowSourceColumns", function()
{
   .rs.getUserPref("allow_source_columns")
})


# Insert spaces for Tab
# Whether to insert spaces when pressing the Tab key.
.rs.addFunction("uiPrefs.useSpacesForTab", function()
{
   .rs.getUserPref("use_spaces_for_tab")
})


# Number of spaces for Tab
# The number of spaces to insert when pressing the Tab key.
.rs.addFunction("uiPrefs.numSpacesForTab", function()
{
   .rs.getUserPref("num_spaces_for_tab")
})


# Auto-detect indentation in files
# Whether to automatically detect indentation settings from file contents.
.rs.addFunction("uiPrefs.autoDetectIndentation", function()
{
   .rs.getUserPref("auto_detect_indentation")
})


# Show margin in editor
# Whether to show the margin guide in the RStudio code editor.
.rs.addFunction("uiPrefs.showMargin", function()
{
   .rs.getUserPref("show_margin")
})


# Use a blinking cursor
# Whether to flash the cursor off and on.
.rs.addFunction("uiPrefs.blinkingCursor", function()
{
   .rs.getUserPref("blinking_cursor")
})


# Margin column
# The number of columns of text after which the margin is shown.
.rs.addFunction("uiPrefs.marginColumn", function()
{
   .rs.getUserPref("margin_column")
})


# Show invisible characters in editor
# Whether to show invisible characters, such as spaces and tabs, in the RStudio
.rs.addFunction("uiPrefs.showInvisibles", function()
{
   .rs.getUserPref("show_invisibles")
})


# Indentation guides
# Style for indentation guides in the RStudio code editor.
.rs.addFunction("uiPrefs.indentGuides", function()
{
   .rs.getUserPref("indent_guides")
})


# Continue comments after adding new line
# Whether to continue comments (by inserting the comment character) after adding
.rs.addFunction("uiPrefs.continueCommentsOnNewline", function()
{
   .rs.getUserPref("continue_comments_on_newline")
})


# Whether web links in comments are clickable
# Whether web links in comments are clickable.
.rs.addFunction("uiPrefs.highlightWebLink", function()
{
   .rs.getUserPref("highlight_web_link")
})


# Keybinding set for editor
# The keybindings to use in the RStudio code editor.
.rs.addFunction("uiPrefs.editorKeybindings", function()
{
   .rs.getUserPref("editor_keybindings")
})


# Auto-insert matching parentheses and brackets
# Whether to insert matching pairs, such as () and [], when the first is typed.
.rs.addFunction("uiPrefs.insertMatching", function()
{
   .rs.getUserPref("insert_matching")
})


# Insert spaces around = in R code
# Whether to insert spaces around the equals sign in R code.
.rs.addFunction("uiPrefs.insertSpacesAroundEquals", function()
{
   .rs.getUserPref("insert_spaces_around_equals")
})


# Insert parentheses after functions
# Whether to insert parentheses after function completions.
.rs.addFunction("uiPrefs.insertParensAfterFunctionCompletion", function()
{
   .rs.getUserPref("insert_parens_after_function_completion")
})


# Complete multi-line statements with Tab
# Whether to attempt completion of multiple-line statements when pressing Tab.
.rs.addFunction("uiPrefs.tabMultilineCompletion", function()
{
   .rs.getUserPref("tab_multiline_completion")
})


# Use Tab to trigger autocompletion
# Whether to attempt completion of statements when pressing Tab.
.rs.addFunction("uiPrefs.tabCompletion", function()
{
   .rs.getUserPref("tab_completion")
})


# Show function help tooltips on idle
# Whether to show help tooltips for functions when the cursor has not been
.rs.addFunction("uiPrefs.showHelpTooltipOnIdle", function()
{
   .rs.getUserPref("show_help_tooltip_on_idle")
})


# Surround selections with
# Which kinds of delimiters can be used to surround the current selection.
.rs.addFunction("uiPrefs.surroundSelection", function()
{
   .rs.getUserPref("surround_selection")
})


# Enable code snippets
# Whether to enable code snippets in the RStudio code editor.
.rs.addFunction("uiPrefs.enableSnippets", function()
{
   .rs.getUserPref("enable_snippets")
})


# Use code completion for R
# When to use auto-completion for R code in the RStudio code editor.
.rs.addFunction("uiPrefs.codeCompletion", function()
{
   .rs.getUserPref("code_completion")
})


# Use code completion for other languages
# When to use auto-completion for other languages (such as JavaScript and SQL) in
.rs.addFunction("uiPrefs.codeCompletionOther", function()
{
   .rs.getUserPref("code_completion_other")
})


# Use code completion in the R console
# Whether to always use code completion in the R console.
.rs.addFunction("uiPrefs.consoleCodeCompletion", function()
{
   .rs.getUserPref("console_code_completion")
})


# Delay before completing code (ms)
# The number of milliseconds to wait before offering code suggestions.
.rs.addFunction("uiPrefs.codeCompletionDelay", function()
{
   .rs.getUserPref("code_completion_delay")
})


# Number of characters for code completion
# The number of characters in a symbol that can be entered before completions are
.rs.addFunction("uiPrefs.codeCompletionCharacters", function()
{
   .rs.getUserPref("code_completion_characters")
})


# Include all function arguments in completion list
# When set, RStudio will include all function arguments in the completion list,
.rs.addFunction("uiPrefs.codeCompletionIncludeAlreadyUsed", function()
{
   .rs.getUserPref("code_completion_include_already_used")
})


# Show function signature tooltips
# Whether to show function signature tooltips during autocompletion.
.rs.addFunction("uiPrefs.showFunctionSignatureTooltips", function()
{
   .rs.getUserPref("show_function_signature_tooltips")
})


# Show data preview in autocompletion help popup
# Whether a data preview is shown in the autocompletion help popup for datasets
.rs.addFunction("uiPrefs.showDataPreview", function()
{
   .rs.getUserPref("show_data_preview")
})


# Show diagnostics in R code
# Whether to show diagnostic messages (such as syntax and usage errors) for R
.rs.addFunction("uiPrefs.showDiagnosticsR", function()
{
   .rs.getUserPref("show_diagnostics_r")
})


# Show diagnostics in C++ code
# Whether to show diagnostic messages for C++ code as you type.
.rs.addFunction("uiPrefs.showDiagnosticsCpp", function()
{
   .rs.getUserPref("show_diagnostics_cpp")
})


# Show diagnostics in YAML code
# Whether to show diagnostic messages for YAML code as you type.
.rs.addFunction("uiPrefs.showDiagnosticsYaml", function()
{
   .rs.getUserPref("show_diagnostics_yaml")
})


# Show diagnostics in other languages
# Whether to show diagnostic messages for other types of code (not R, C++, or
.rs.addFunction("uiPrefs.showDiagnosticsOther", function()
{
   .rs.getUserPref("show_diagnostics_other")
})


# Show style diagnostics for R code
# Whether to show style diagnostics (suggestions for improving R code style)
.rs.addFunction("uiPrefs.styleDiagnostics", function()
{
   .rs.getUserPref("style_diagnostics")
})


# Check code for problems when saving
# Whether to check code for problems after saving it.
.rs.addFunction("uiPrefs.diagnosticsOnSave", function()
{
   .rs.getUserPref("diagnostics_on_save")
})


# Run R code diagnostics in the background
# Whether to run code diagnostics in the background, as you type.
.rs.addFunction("uiPrefs.backgroundDiagnostics", function()
{
   .rs.getUserPref("background_diagnostics")
})


# Run R code diagnostics after (ms)
# The number of milliseconds to delay before running code diagnostics in the
.rs.addFunction("uiPrefs.backgroundDiagnosticsDelayMs", function()
{
   .rs.getUserPref("background_diagnostics_delay_ms")
})


# Run diagnostics on R function calls
# Whether to run diagnostics in R function calls.
.rs.addFunction("uiPrefs.diagnosticsInRFunctionCalls", function()
{
   .rs.getUserPref("diagnostics_in_r_function_calls")
})


# Check arguments to R function calls
# Whether to check arguments to R function calls.
.rs.addFunction("uiPrefs.checkArgumentsToRFunctionCalls", function()
{
   .rs.getUserPref("check_arguments_to_r_function_calls")
})


# Check for unexpected assignments
# Whether to check for unexpected variable assignments inside R function calls.
.rs.addFunction("uiPrefs.checkUnexpectedAssignmentInFunctionCall", function()
{
   .rs.getUserPref("check_unexpected_assignment_in_function_call")
})


# Warn when R variable used but not defined
# Whether to generate a warning if a variable is used without being defined in
.rs.addFunction("uiPrefs.warnIfNoSuchVariableInScope", function()
{
   .rs.getUserPref("warn_if_no_such_variable_in_scope")
})


# Warn when R variable defined but not used
# Whether to generate a warning if a variable is defined without being used in
.rs.addFunction("uiPrefs.warnVariableDefinedButNotUsed", function()
{
   .rs.getUserPref("warn_variable_defined_but_not_used")
})


# Detect missing R packages in the editor
# Whether to automatically discover and offer to install missing R package
.rs.addFunction("uiPrefs.autoDiscoverPackageDependencies", function()
{
   .rs.getUserPref("auto_discover_package_dependencies")
})


# Ensure files end with a newline when saving
# Whether to ensure that source files end with a newline character.
.rs.addFunction("uiPrefs.autoAppendNewline", function()
{
   .rs.getUserPref("auto_append_newline")
})


# Strip trailing whitespace when saving
# Whether to strip trailing whitespace from each line when saving.
.rs.addFunction("uiPrefs.stripTrailingWhitespace", function()
{
   .rs.getUserPref("strip_trailing_whitespace")
})


# Restore cursor position when reopening files
# Whether to save the position of the cursor when a file is closed, restore it
.rs.addFunction("uiPrefs.restoreSourceDocumentCursorPosition", function()
{
   .rs.getUserPref("restore_source_document_cursor_position")
})


# Re-indent code when pasting
# Whether to automatically re-indent code when it's pasted into RStudio.
.rs.addFunction("uiPrefs.reindentOnPaste", function()
{
   .rs.getUserPref("reindent_on_paste")
})


# Vertically align function arguments
# Whether to vertically align arguments to R function calls during automatic
.rs.addFunction("uiPrefs.verticallyAlignArgumentsIndent", function()
{
   .rs.getUserPref("vertically_align_arguments_indent")
})


# Soft-wrap source files
# Whether to soft-wrap source files, wrapping the text for display without
.rs.addFunction("uiPrefs.softWrapRFiles", function()
{
   .rs.getUserPref("soft_wrap_r_files")
})


# Soft-wrap R Markdown files
# Whether to soft-wrap R Markdown files (and similar types such as R HTML and R
.rs.addFunction("uiPrefs.softWrapRmdFiles", function()
{
   .rs.getUserPref("soft_wrap_rmd_files")
})


# Focus console after executing R code
# Whether to focus the R console after executing an R command from a script.
.rs.addFunction("uiPrefs.focusConsoleAfterExec", function()
{
   .rs.getUserPref("focus_console_after_exec")
})


# Fold style in editor
# The style of folding to use.
.rs.addFunction("uiPrefs.foldStyle", function()
{
   .rs.getUserPref("fold_style")
})


# Save R scripts before sourcing
# Whether to automatically save scripts before executing them.
.rs.addFunction("uiPrefs.saveBeforeSourcing", function()
{
   .rs.getUserPref("save_before_sourcing")
})


# Syntax highlighting in R console
# Whether to use syntax highlighting in the R console.
.rs.addFunction("uiPrefs.syntaxColorConsole", function()
{
   .rs.getUserPref("syntax_color_console")
})


# Different color for error output in R console
# Whether to display error, warning, and message output in a different color.
.rs.addFunction("uiPrefs.highlightConsoleErrors", function()
{
   .rs.getUserPref("highlight_console_errors")
})


# Scroll past end of file
# Whether to allow scrolling past the end of a file.
.rs.addFunction("uiPrefs.scrollPastEndOfDocument", function()
{
   .rs.getUserPref("scroll_past_end_of_document")
})


# Highlight R function calls
# Whether to highlight R function calls in the code editor.
.rs.addFunction("uiPrefs.highlightRFunctionCalls", function()
{
   .rs.getUserPref("highlight_r_function_calls")
})


# Enable preview of named and hexadecimal colors
# Whether to show preview for named and hexadecimal colors.
.rs.addFunction("uiPrefs.colorPreview", function()
{
   .rs.getUserPref("color_preview")
})


# Use rainbow parentheses
# Whether to highlight parentheses in a variety of colors.
.rs.addFunction("uiPrefs.rainbowParentheses", function()
{
   .rs.getUserPref("rainbow_parentheses")
})


# Use rainbow fenced divs
# Whether to highlight fenced divs in a variety of colors.
.rs.addFunction("uiPrefs.rainbowFencedDivs", function()
{
   .rs.getUserPref("rainbow_fenced_divs")
})


# Maximum characters per line in R console
# The maximum number of characters to display in a single line in the R console.
.rs.addFunction("uiPrefs.consoleLineLengthLimit", function()
{
   .rs.getUserPref("console_line_length_limit")
})


# Maximum lines in R console
# The maximum number of console actions to store and display in the console
.rs.addFunction("uiPrefs.consoleMaxLines", function()
{
   .rs.getUserPref("console_max_lines")
})


# ANSI escape codes in R console
# How to treat ANSI escape codes in the console.
.rs.addFunction("uiPrefs.ansiConsoleMode", function()
{
   .rs.getUserPref("ansi_console_mode")
})


# Limit visible console output
# Whether to only show a limited window of the total console output
.rs.addFunction("uiPrefs.limitVisibleConsole", function()
{
   .rs.getUserPref("limit_visible_console")
})


# Show toolbar on R Markdown chunks
# Whether to show a toolbar on code chunks in R Markdown documents.
.rs.addFunction("uiPrefs.showInlineToolbarForRCodeChunks", function()
{
   .rs.getUserPref("show_inline_toolbar_for_r_code_chunks")
})


# Highlight code chunks in R Markdown files
# Whether to highlight code chunks in R Markdown documents with a different
.rs.addFunction("uiPrefs.highlightCodeChunks", function()
{
   .rs.getUserPref("highlight_code_chunks")
})


# Save files before building
# Whether to save all open, unsaved files before building the project.
.rs.addFunction("uiPrefs.saveFilesBeforeBuild", function()
{
   .rs.getUserPref("save_files_before_build")
})


# Save and reload R workspace on build
# Whether RStudio should save and reload the R workspace when building the
.rs.addFunction("uiPrefs.saveAndReloadWorkspaceOnBuild", function()
{
   .rs.getUserPref("save_and_reload_workspace_on_build")
})


# Editor font size (points)
# The default editor font size, in points.
.rs.addFunction("uiPrefs.fontSizePoints", function()
{
   .rs.getUserPref("font_size_points")
})


# Help panel font size (points)
# The help panel font size, in points.
.rs.addFunction("uiPrefs.helpFontSizePoints", function()
{
   .rs.getUserPref("help_font_size_points")
})


# Theme
# The name of the color theme to apply to the text editor in RStudio.
.rs.addFunction("uiPrefs.editorTheme", function()
{
   .rs.getUserPref("editor_theme")
})


# Enable editor fonts on RStudio Server
# Whether to use a custom editor font in RStudio Server.
.rs.addFunction("uiPrefs.serverEditorFontEnabled", function()
{
   .rs.getUserPref("server_editor_font_enabled")
})


# Editor font
# The name of the fixed-width editor font to use with RStudio Server.
.rs.addFunction("uiPrefs.serverEditorFont", function()
{
   .rs.getUserPref("server_editor_font")
})


# Default character encoding
# The default character encoding to use when saving files.
.rs.addFunction("uiPrefs.defaultEncoding", function()
{
   .rs.getUserPref("default_encoding")
})


# Show top toolbar
# Whether to show the toolbar at the top of the RStudio workbench.
.rs.addFunction("uiPrefs.toolbarVisible", function()
{
   .rs.getUserPref("toolbar_visible")
})


# Default new project location
# The directory path under which to place new projects by default.
.rs.addFunction("uiPrefs.defaultProjectLocation", function()
{
   .rs.getUserPref("default_project_location")
})


# Default open project location
# The default directory to use in file dialogs when opening a project.
.rs.addFunction("uiPrefs.defaultOpenProjectLocation", function()
{
   .rs.getUserPref("default_open_project_location")
})


# Source with echo by default
# Whether to echo R code when sourcing it.
.rs.addFunction("uiPrefs.sourceWithEcho", function()
{
   .rs.getUserPref("source_with_echo")
})


# Default Sweave engine
# The default engine to use when processing Sweave documents.
.rs.addFunction("uiPrefs.defaultSweaveEngine", function()
{
   .rs.getUserPref("default_sweave_engine")
})


# Default LaTeX program
# The default program to use when processing LaTeX documents.
.rs.addFunction("uiPrefs.defaultLatexProgram", function()
{
   .rs.getUserPref("default_latex_program")
})


# Use Roxygen for documentation
# Whether to use Roxygen for documentation.
.rs.addFunction("uiPrefs.useRoxygen", function()
{
   .rs.getUserPref("use_roxygen")
})


# Enable data import
# Whether to use RStudio's data import feature.
.rs.addFunction("uiPrefs.useDataimport", function()
{
   .rs.getUserPref("use_dataimport")
})


# PDF previewer
# The program to use to preview PDF files after generation.
.rs.addFunction("uiPrefs.pdfPreviewer", function()
{
   .rs.getUserPref("pdf_previewer")
})


# Enable Rnw concordance
# Whether to always enable the concordance for RNW files.
.rs.addFunction("uiPrefs.alwaysEnableRnwConcordance", function()
{
   .rs.getUserPref("always_enable_rnw_concordance")
})


# Insert numbered LaTeX sections
# Whether to insert numbered sections in LaTeX.
.rs.addFunction("uiPrefs.insertNumberedLatexSections", function()
{
   .rs.getUserPref("insert_numbered_latex_sections")
})


# Spelling dictionary language
# The language of the spelling dictionary to use for spell checking.
.rs.addFunction("uiPrefs.spellingDictionaryLanguage", function()
{
   .rs.getUserPref("spelling_dictionary_language")
})


# Custom spelling dictionaries
# The list of custom dictionaries to use when spell checking.
.rs.addFunction("uiPrefs.spellingCustomDictionaries", function()
{
   .rs.getUserPref("spelling_custom_dictionaries")
})


# Lint document after load (ms)
# The number of milliseconds to wait before linting a document after it is
.rs.addFunction("uiPrefs.documentLoadLintDelay", function()
{
   .rs.getUserPref("document_load_lint_delay")
})


# Ignore uppercase words in spell check
# Whether to ignore words in uppercase when spell checking.
.rs.addFunction("uiPrefs.ignoreUppercaseWords", function()
{
   .rs.getUserPref("ignore_uppercase_words")
})


# Ignore words with numbers in spell check
# Whether to ignore words with numbers in them when spell checking.
.rs.addFunction("uiPrefs.ignoreWordsWithNumbers", function()
{
   .rs.getUserPref("ignore_words_with_numbers")
})


# Use real-time spellchecking
# Whether to enable real-time spellchecking by default.
.rs.addFunction("uiPrefs.realTimeSpellchecking", function()
{
   .rs.getUserPref("real_time_spellchecking")
})


# Navigate to build errors
# Whether to navigate to build errors.
.rs.addFunction("uiPrefs.navigateToBuildError", function()
{
   .rs.getUserPref("navigate_to_build_error")
})


# Enable the Packages pane
# Whether to enable RStudio's Packages pane.
.rs.addFunction("uiPrefs.packagesPaneEnabled", function()
{
   .rs.getUserPref("packages_pane_enabled")
})


# C++ template
# C++ template.
.rs.addFunction("uiPrefs.cppTemplate", function()
{
   .rs.getUserPref("cpp_template")
})


# Restore last opened documents on startup
# Whether to restore the last opened source documents when RStudio starts up.
.rs.addFunction("uiPrefs.restoreSourceDocuments", function()
{
   .rs.getUserPref("restore_source_documents")
})


# Handle errors only when user code present
# Whether to handle errors only when user code is on the stack.
.rs.addFunction("uiPrefs.handleErrorsInUserCodeOnly", function()
{
   .rs.getUserPref("handle_errors_in_user_code_only")
})


# Auto-expand error tracebacks
# Whether to automatically expand tracebacks when an error occurs.
.rs.addFunction("uiPrefs.autoExpandErrorTracebacks", function()
{
   .rs.getUserPref("auto_expand_error_tracebacks")
})


# Check for new version at startup
# Whether to check for new versions of RStudio when RStudio starts.
.rs.addFunction("uiPrefs.checkForUpdates", function()
{
   .rs.getUserPref("check_for_updates")
})


# Show internal functions when debugging
# Whether to show functions without source references in the Traceback pane while
.rs.addFunction("uiPrefs.showInternalFunctions", function()
{
   .rs.getUserPref("show_internal_functions")
})


# Run Shiny applications in
# Where to display Shiny applications when they are run.
.rs.addFunction("uiPrefs.shinyViewerType", function()
{
   .rs.getUserPref("shiny_viewer_type")
})


# Run Shiny applications in the background
# Whether to run Shiny applications as background jobs.
.rs.addFunction("uiPrefs.shinyBackgroundJobs", function()
{
   .rs.getUserPref("shiny_background_jobs")
})


# Run Plumber APIs in
# Where to display Shiny applications when they are run.
.rs.addFunction("uiPrefs.plumberViewerType", function()
{
   .rs.getUserPref("plumber_viewer_type")
})


# Document author
# The default name to use as the document author when creating new documents.
.rs.addFunction("uiPrefs.documentAuthor", function()
{
   .rs.getUserPref("document_author")
})


# Use current date when rendering document
# Use current date when rendering document
.rs.addFunction("uiPrefs.rmdAutoDate", function()
{
   .rs.getUserPref("rmd_auto_date")
})


# Path to preferred R Markdown template
# The path to the preferred R Markdown template.
.rs.addFunction("uiPrefs.rmdPreferredTemplatePath", function()
{
   .rs.getUserPref("rmd_preferred_template_path")
})


# Display R Markdown documents in
# Where to display R Markdown documents when they have completed rendering.
.rs.addFunction("uiPrefs.rmdViewerType", function()
{
   .rs.getUserPref("rmd_viewer_type")
})


# Show diagnostic info when publishing
# Whether to show verbose diagnostic information when publishing content.
.rs.addFunction("uiPrefs.showPublishDiagnostics", function()
{
   .rs.getUserPref("show_publish_diagnostics")
})


# 
# Whether to show UI for publishing content to Posit Cloud.
.rs.addFunction("uiPrefs.enableCloudPublishUi", function()
{
   .rs.getUserPref("enable_cloud_publish_ui")
})


# Check SSL certificates when publishing
# Whether to check remote server SSL certificates when publishing content.
.rs.addFunction("uiPrefs.publishCheckCertificates", function()
{
   .rs.getUserPref("publish_check_certificates")
})


# Use custom CA bundle when publishing
# Whether to use a custom certificate authority (CA) bundle when publishing
.rs.addFunction("uiPrefs.usePublishCaBundle", function()
{
   .rs.getUserPref("use_publish_ca_bundle")
})


# Path to custom CA bundle for publishing
# The path to the custom certificate authority (CA) bundle to use when publishing
.rs.addFunction("uiPrefs.publishCaBundle", function()
{
   .rs.getUserPref("publish_ca_bundle")
})


# Show chunk output inline in all documents
# Whether to show chunk output inline for ordinary R Markdown documents.
.rs.addFunction("uiPrefs.rmdChunkOutputInline", function()
{
   .rs.getUserPref("rmd_chunk_output_inline")
})


# Open document outline by default
# Whether to show the document outline by default when opening R Markdown
.rs.addFunction("uiPrefs.showDocOutlineRmd", function()
{
   .rs.getUserPref("show_doc_outline_rmd")
})


# Document outline font size
# The font size to use for items in the document outline.
.rs.addFunction("uiPrefs.documentOutlineFontSize", function()
{
   .rs.getUserPref("document_outline_font_size")
})


# Automatically run Setup chunk when needed
# Whether to automatically run an R Markdown document's Setup chunk before
.rs.addFunction("uiPrefs.autoRunSetupChunk", function()
{
   .rs.getUserPref("auto_run_setup_chunk")
})


# Hide console when running R Markdown chunks
# Whether to hide the R console when executing inline R Markdown chunks.
.rs.addFunction("uiPrefs.hideConsoleOnChunkExecute", function()
{
   .rs.getUserPref("hide_console_on_chunk_execute")
})


# Unit of R code execution
# The unit of R code to execute when the Execute command is invoked.
.rs.addFunction("uiPrefs.executionBehavior", function()
{
   .rs.getUserPref("execution_behavior")
})


# Show the Terminal tab
# Whether to show the Terminal tab.
.rs.addFunction("uiPrefs.showTerminalTab", function()
{
   .rs.getUserPref("show_terminal_tab")
})


# Use local echo in the Terminal
# Whether to use local echo in the Terminal.
.rs.addFunction("uiPrefs.terminalLocalEcho", function()
{
   .rs.getUserPref("terminal_local_echo")
})


# Use websockets in the Terminal
# Whether to use websockets to communicate with the shell in the Terminal tab.
.rs.addFunction("uiPrefs.terminalWebsockets", function()
{
   .rs.getUserPref("terminal_websockets")
})


# Close Terminal pane after shell exit
# Whether to close the terminal pane after the shell exits.
.rs.addFunction("uiPrefs.terminalCloseBehavior", function()
{
   .rs.getUserPref("terminal_close_behavior")
})


# Save and restore system environment in Terminal tab
# Whether to track and save changes to system environment variables in the
.rs.addFunction("uiPrefs.terminalTrackEnvironment", function()
{
   .rs.getUserPref("terminal_track_environment")
})


# Ignored environment variables
# Environment variables which should be ignored when tracking changed to
.rs.addFunction("uiPrefs.terminalIgnoredEnvironmentVariables", function()
{
   .rs.getUserPref("terminal_ignored_environment_variables")
})


# Enable Terminal hooks
# Enabled Terminal hooks? Required for Python terminal integration, which places
.rs.addFunction("uiPrefs.terminalHooks", function()
{
   .rs.getUserPref("terminal_hooks")
})


# Terminal bell style
# Terminal bell style
.rs.addFunction("uiPrefs.terminalBellStyle", function()
{
   .rs.getUserPref("terminal_bell_style")
})


# Terminal tab rendering engine
# Terminal rendering engine: canvas is faster, dom may be needed for some
.rs.addFunction("uiPrefs.terminalRenderer", function()
{
   .rs.getUserPref("terminal_renderer")
})


# Make links in Terminal clickable
# Whether web links displayed in the Terminal tab are made clickable.
.rs.addFunction("uiPrefs.terminalWeblinks", function()
{
   .rs.getUserPref("terminal_weblinks")
})


# Show R Markdown render command
# Whether to print the render command use to knit R Markdown documents in the R
.rs.addFunction("uiPrefs.showRmdRenderCommand", function()
{
   .rs.getUserPref("show_rmd_render_command")
})


# Enable dragging text in code editor
# Whether to enable moving text on the editing surface by clicking and dragging
.rs.addFunction("uiPrefs.enableTextDrag", function()
{
   .rs.getUserPref("enable_text_drag")
})


# Show hidden files in Files pane
# Whether to show hidden files in the Files pane.
.rs.addFunction("uiPrefs.showHiddenFiles", function()
{
   .rs.getUserPref("show_hidden_files")
})


# Files always shown in the Files Pane
# List of file names (case sensitive) that are always shown in the Files Pane,
.rs.addFunction("uiPrefs.alwaysShownFiles", function()
{
   .rs.getUserPref("always_shown_files")
})


# Extensions always shown in the Files Pane
# List of file extensions (beginning with ., not case sensitive) that are always
.rs.addFunction("uiPrefs.alwaysShownExtensions", function()
{
   .rs.getUserPref("always_shown_extensions")
})


# Sort file names naturally in Files pane
# Whether to sort file names naturally, so that e.g., file10.R comes after
.rs.addFunction("uiPrefs.sortFileNamesNaturally", function()
{
   .rs.getUserPref("sort_file_names_naturally")
})


# Synchronize the Files pane with the current working directory
# Whether to change the directory in the Files pane automatically when the
.rs.addFunction("uiPrefs.syncFilesPaneWorkingDir", function()
{
   .rs.getUserPref("sync_files_pane_working_dir")
})


# Jobs tab visibility
# The visibility of the Jobs tab.
.rs.addFunction("uiPrefs.jobsTabVisibility", function()
{
   .rs.getUserPref("jobs_tab_visibility")
})


# 
# Whether to show the Workbench Jobs tab in RStudio Pro and RStudio Workbench.
.rs.addFunction("uiPrefs.showLauncherJobsTab", function()
{
   .rs.getUserPref("show_launcher_jobs_tab")
})


# 
# How to sort jobs in the Workbench Jobs tab in RStudio Pro and RStudio
.rs.addFunction("uiPrefs.launcherJobsSort", function()
{
   .rs.getUserPref("launcher_jobs_sort")
})


# 
# How to detect busy status in the Terminal.
.rs.addFunction("uiPrefs.busyDetection", function()
{
   .rs.getUserPref("busy_detection")
})


# 
# A list of apps that should not be considered busy in the Terminal.
.rs.addFunction("uiPrefs.busyExclusionList", function()
{
   .rs.getUserPref("busy_exclusion_list")
})


# Working directory for knitting
# The working directory to use when knitting R Markdown documents.
.rs.addFunction("uiPrefs.knitWorkingDir", function()
{
   .rs.getUserPref("knit_working_dir")
})


# Show in Document Outline
# Which objects to show in the document outline pane.
.rs.addFunction("uiPrefs.docOutlineShow", function()
{
   .rs.getUserPref("doc_outline_show")
})


# Preview LaTeX equations on idle
# When to preview LaTeX mathematical equations when cursor has not moved
.rs.addFunction("uiPrefs.latexPreviewOnCursorIdle", function()
{
   .rs.getUserPref("latex_preview_on_cursor_idle")
})


# Wrap around when going to previous/next tab
# Whether to wrap around when going to the previous or next editor tab.
.rs.addFunction("uiPrefs.wrapTabNavigation", function()
{
   .rs.getUserPref("wrap_tab_navigation")
})


# Global theme
# The theme to use for the main RStudio user interface.
.rs.addFunction("uiPrefs.globalTheme", function()
{
   .rs.getUserPref("global_theme")
})


# Ignore whitespace in VCS diffs
# Whether to ignore whitespace when generating diffs of version controlled files.
.rs.addFunction("uiPrefs.gitDiffIgnoreWhitespace", function()
{
   .rs.getUserPref("git_diff_ignore_whitespace")
})


# Sign git commits
# Whether to sign git commits.
.rs.addFunction("uiPrefs.gitSignedCommits", function()
{
   .rs.getUserPref("git_signed_commits")
})


# Double click to select in the Console
# Whether double-clicking should select a word in the Console pane.
.rs.addFunction("uiPrefs.consoleDoubleClickSelect", function()
{
   .rs.getUserPref("console_double_click_select")
})


# Warn when automatic session suspension is paused
# Whether the 'Auto Suspension Blocked' icon should appear in the R Console
.rs.addFunction("uiPrefs.consoleSuspendBlockedNotice", function()
{
   .rs.getUserPref("console_suspend_blocked_notice")
})


# Number of seconds to delay warning
# How long to wait before warning that automatic session suspension has been
.rs.addFunction("uiPrefs.consoleSuspendBlockedNoticeDelay", function()
{
   .rs.getUserPref("console_suspend_blocked_notice_delay")
})


# Create a Git repo in new projects
# Whether a git repo should be initialized inside new projects by default.
.rs.addFunction("uiPrefs.newProjGitInit", function()
{
   .rs.getUserPref("new_proj_git_init")
})


# Create an renv environment in new projects
# Whether an renv environment should be created inside new projects by default.
.rs.addFunction("uiPrefs.newProjUseRenv", function()
{
   .rs.getUserPref("new_proj_use_renv")
})


# Root document for PDF compilation
# The root document to use when compiling PDF documents.
.rs.addFunction("uiPrefs.rootDocument", function()
{
   .rs.getUserPref("root_document")
})


# Show user home page in RStudio Workbench
# When to show the server home page in RStudio Workbench.
.rs.addFunction("uiPrefs.showUserHomePage", function()
{
   .rs.getUserPref("show_user_home_page")
})


# 
# Whether to reuse sessions when opening projects in RStudio Workbench.
.rs.addFunction("uiPrefs.reuseSessionsForProjectLinks", function()
{
   .rs.getUserPref("reuse_sessions_for_project_links")
})


# Enable version control if available
# Whether to enable RStudio's version control system interface.
.rs.addFunction("uiPrefs.vcsEnabled", function()
{
   .rs.getUserPref("vcs_enabled")
})


# Auto-refresh state from version control
# Automatically refresh VCS status?
.rs.addFunction("uiPrefs.vcsAutorefresh", function()
{
   .rs.getUserPref("vcs_autorefresh")
})


# Path to Git executable
# The path to the Git executable to use.
.rs.addFunction("uiPrefs.gitExePath", function()
{
   .rs.getUserPref("git_exe_path")
})


# Path to Subversion executable
# The path to the Subversion executable to use.
.rs.addFunction("uiPrefs.svnExePath", function()
{
   .rs.getUserPref("svn_exe_path")
})


# 
# The path to the terminal executable to use.
.rs.addFunction("uiPrefs.terminalPath", function()
{
   .rs.getUserPref("terminal_path")
})


# 
# The path to the SSH key file to use.
.rs.addFunction("uiPrefs.rsaKeyPath", function()
{
   .rs.getUserPref("rsa_key_path")
})


# 
# The encryption type to use for the SSH key file.
.rs.addFunction("uiPrefs.sshKeyType", function()
{
   .rs.getUserPref("ssh_key_type")
})


# Use the devtools R package if available
# Whether to use the devtools R package.
.rs.addFunction("uiPrefs.useDevtools", function()
{
   .rs.getUserPref("use_devtools")
})


# Always use --preclean when installing package
# Always use --preclean when installing package.
.rs.addFunction("uiPrefs.cleanBeforeInstall", function()
{
   .rs.getUserPref("clean_before_install")
})


# Download R packages securely
# Whether to use secure downloads when fetching R packages.
.rs.addFunction("uiPrefs.useSecureDownload", function()
{
   .rs.getUserPref("use_secure_download")
})


# Clean up temporary files after R CMD CHECK
# Whether to clean up temporary files after running R CMD CHECK.
.rs.addFunction("uiPrefs.cleanupAfterRCmdCheck", function()
{
   .rs.getUserPref("cleanup_after_r_cmd_check")
})


# View directory after R CMD CHECK
# Whether to view the directory after running R CMD CHECK.
.rs.addFunction("uiPrefs.viewDirAfterRCmdCheck", function()
{
   .rs.getUserPref("view_dir_after_r_cmd_check")
})


# Hide object files in the Files pane
# Whether to hide object files in the Files pane.
.rs.addFunction("uiPrefs.hideObjectFiles", function()
{
   .rs.getUserPref("hide_object_files")
})


# Restore last project when starting RStudio
# Whether to restore the last project when starting RStudio.
.rs.addFunction("uiPrefs.restoreLastProject", function()
{
   .rs.getUserPref("restore_last_project")
})


# Number of seconds for safe project startup
# The number of seconds after which a project is deemed to have successfully
.rs.addFunction("uiPrefs.projectSafeStartupSeconds", function()
{
   .rs.getUserPref("project_safe_startup_seconds")
})


# Use tinytex to compile .tex files
# Use tinytex to compile .tex files.
.rs.addFunction("uiPrefs.useTinytex", function()
{
   .rs.getUserPref("use_tinytex")
})


# Clean output after running Texi2Dvi
# Whether to clean output after running Texi2Dvi.
.rs.addFunction("uiPrefs.cleanTexi2dviOutput", function()
{
   .rs.getUserPref("clean_texi2dvi_output")
})


# Shell escape LaTeX documents
# Whether to enable shell escaping with LaTeX documents.
.rs.addFunction("uiPrefs.latexShellEscape", function()
{
   .rs.getUserPref("latex_shell_escape")
})


# Restore project R version in RStudio Pro and RStudio Workbench
# Whether to restore the last version of R used by the project in RStudio Pro and
.rs.addFunction("uiPrefs.restoreProjectRVersion", function()
{
   .rs.getUserPref("restore_project_r_version")
})


# Clang verbosity level (0 - 2)
# The verbosity level to use with Clang (0 - 2)
.rs.addFunction("uiPrefs.clangVerbose", function()
{
   .rs.getUserPref("clang_verbose")
})


# Submit crash reports to RStudio
# Whether to automatically submit crash reports to RStudio.
.rs.addFunction("uiPrefs.submitCrashReports", function()
{
   .rs.getUserPref("submit_crash_reports")
})


# 
# The R version to use by default.
.rs.addFunction("uiPrefs.defaultRVersion", function()
{
   .rs.getUserPref("default_r_version")
})


# Maximum number of columns in data viewer
# The maximum number of columns to show at once in the data viewer.
.rs.addFunction("uiPrefs.dataViewerMaxColumns", function()
{
   .rs.getUserPref("data_viewer_max_columns")
})


# Maximum number of character in data viewer cells
# The maximum number of characters to show in a data viewer cell.
.rs.addFunction("uiPrefs.dataViewerMaxCellSize", function()
{
   .rs.getUserPref("data_viewer_max_cell_size")
})


# Enable support for screen readers
# Support accessibility aids such as screen readers.
.rs.addFunction("uiPrefs.enableScreenReader", function()
{
   .rs.getUserPref("enable_screen_reader")
})


# Seconds to wait before updating ARIA live region
# Number of milliseconds to wait after last keystroke before updating live
.rs.addFunction("uiPrefs.typingStatusDelayMs", function()
{
   .rs.getUserPref("typing_status_delay_ms")
})


# Reduced animation/motion mode
# Reduce use of animations in the user interface.
.rs.addFunction("uiPrefs.reducedMotion", function()
{
   .rs.getUserPref("reduced_motion")
})


# Tab key always moves focus
# Tab key moves focus out of text editing controls instead of inserting tabs.
.rs.addFunction("uiPrefs.tabKeyMoveFocus", function()
{
   .rs.getUserPref("tab_key_move_focus")
})


# Tab key moves focus directly from find text to replace text in find panel
# In source editor find panel, tab key moves focus directly from find text to
.rs.addFunction("uiPrefs.findPanelLegacyTabSequence", function()
{
   .rs.getUserPref("find_panel_legacy_tab_sequence")
})


# Show focus outline around focused panel
# Show which panel contains keyboard focus.
.rs.addFunction("uiPrefs.showPanelFocusRectangle", function()
{
   .rs.getUserPref("show_panel_focus_rectangle")
})


# Autosave mode on idle
# How to deal with changes to documents on idle.
.rs.addFunction("uiPrefs.autoSaveOnIdle", function()
{
   .rs.getUserPref("auto_save_on_idle")
})


# Idle period for document autosave (ms)
# The idle period, in milliseconds, after which documents should be auto-saved.
.rs.addFunction("uiPrefs.autoSaveIdleMs", function()
{
   .rs.getUserPref("auto_save_idle_ms")
})


# Save documents when editor loses input focus
# Whether to automatically save when the editor loses focus.
.rs.addFunction("uiPrefs.autoSaveOnBlur", function()
{
   .rs.getUserPref("auto_save_on_blur")
})


# Initial working directory for new terminals
# Initial directory for new terminals.
.rs.addFunction("uiPrefs.terminalInitialDirectory", function()
{
   .rs.getUserPref("terminal_initial_directory")
})


# Show full path to project in RStudio Desktop windows
# Whether to show the full path to project in desktop window title.
.rs.addFunction("uiPrefs.fullProjectPathInWindowTitle", function()
{
   .rs.getUserPref("full_project_path_in_window_title")
})


# Use visual editing by default for new markdown documents
# Whether to enable visual editing by default for new markdown documents
.rs.addFunction("uiPrefs.visualMarkdownEditingIsDefault", function()
{
   .rs.getUserPref("visual_markdown_editing_is_default")
})


# Default list spacing in visual markdown editing mode
# Default spacing for lists created in the visual editor
.rs.addFunction("uiPrefs.visualMarkdownEditingListSpacing", function()
{
   .rs.getUserPref("visual_markdown_editing_list_spacing")
})


# Wrap text in visual markdown editing mode
# Whether to automatically wrap text when writing markdown
.rs.addFunction("uiPrefs.visualMarkdownEditingWrap", function()
{
   .rs.getUserPref("visual_markdown_editing_wrap")
})


# Wrap column for visual markdown editing mode
# The column to wrap text at when writing markdown
.rs.addFunction("uiPrefs.visualMarkdownEditingWrapAtColumn", function()
{
   .rs.getUserPref("visual_markdown_editing_wrap_at_column")
})


# Place visual markdown footnotes in
# Placement of footnotes within markdown output.
.rs.addFunction("uiPrefs.visualMarkdownEditingReferencesLocation", function()
{
   .rs.getUserPref("visual_markdown_editing_references_location")
})


# Write canonical visual mode markdown in source mode
# Whether to write canonical visual mode markdown when saving from source mode.
.rs.addFunction("uiPrefs.visualMarkdownEditingCanonical", function()
{
   .rs.getUserPref("visual_markdown_editing_canonical")
})


# Max content width for visual markdown editor (px)
# Maximum content width for visual editing mode, in pixels
.rs.addFunction("uiPrefs.visualMarkdownEditingMaxContentWidth", function()
{
   .rs.getUserPref("visual_markdown_editing_max_content_width")
})


# Show document outline in visual markdown editing mode
# Whether to show the document outline by default when opening R Markdown
.rs.addFunction("uiPrefs.visualMarkdownEditingShowDocOutline", function()
{
   .rs.getUserPref("visual_markdown_editing_show_doc_outline")
})


# Show margin in visual mode code blocks
# Whether to show the margin guide in the visual mode code blocks.
.rs.addFunction("uiPrefs.visualMarkdownEditingShowMargin", function()
{
   .rs.getUserPref("visual_markdown_editing_show_margin")
})


# Show line numbers in visual mode code blocks
# Whether to show line numbers in the code editors used in visual mode
.rs.addFunction("uiPrefs.visualMarkdownCodeEditorLineNumbers", function()
{
   .rs.getUserPref("visual_markdown_code_editor_line_numbers")
})


# Font size for visual editing mode
# The default visual editing mode font size, in points
.rs.addFunction("uiPrefs.visualMarkdownEditingFontSizePoints", function()
{
   .rs.getUserPref("visual_markdown_editing_font_size_points")
})


# Editor for code chunks in visual editing mode
# The name of the editor to use to provide code editing in visual mode
.rs.addFunction("uiPrefs.visualMarkdownCodeEditor", function()
{
   .rs.getUserPref("visual_markdown_code_editor")
})


# Zotero libraries
# Zotero libraries to insert citations from.
.rs.addFunction("uiPrefs.zoteroLibraries", function()
{
   .rs.getUserPref("zotero_libraries")
})


# 
# Preferred emoji skintone
.rs.addFunction("uiPrefs.emojiSkintone", function()
{
   .rs.getUserPref("emoji_skintone")
})


# Disabled aria-live announcements
# List of aria-live announcements to disable.
.rs.addFunction("uiPrefs.disabledAriaLiveAnnouncements", function()
{
   .rs.getUserPref("disabled_aria_live_announcements")
})


# Maximum number of console lines to announce
# Maximum number of lines of console output announced after a command.
.rs.addFunction("uiPrefs.screenreaderConsoleAnnounceLimit", function()
{
   .rs.getUserPref("screenreader_console_announce_limit")
})


# List of path components ignored by file monitor
# List of path components; file monitor will ignore paths containing one or more
.rs.addFunction("uiPrefs.fileMonitorIgnoredComponents", function()
{
   .rs.getUserPref("file_monitor_ignored_components")
})


# Install R package dependencies one at a time
# Whether to install R package dependencies one at a time.
.rs.addFunction("uiPrefs.installPkgDepsIndividually", function()
{
   .rs.getUserPref("install_pkg_deps_individually")
})


# R graphics backend
# R graphics backend.
.rs.addFunction("uiPrefs.graphicsBackend", function()
{
   .rs.getUserPref("graphics_backend")
})


# R graphics antialiasing method
# Type of anti-aliasing to be used for generated R plots.
.rs.addFunction("uiPrefs.graphicsAntialiasing", function()
{
   .rs.getUserPref("graphics_antialiasing")
})


# Fixed-width font list for RStudio Server
# List of fixed-width fonts to check for browser support.
.rs.addFunction("uiPrefs.browserFixedWidthFonts", function()
{
   .rs.getUserPref("browser_fixed_width_fonts")
})


# 
# The Python type.
.rs.addFunction("uiPrefs.pythonType", function()
{
   .rs.getUserPref("python_type")
})


# 
# The Python version.
.rs.addFunction("uiPrefs.pythonVersion", function()
{
   .rs.getUserPref("python_version")
})


# 
# The path to the default Python interpreter.
.rs.addFunction("uiPrefs.pythonPath", function()
{
   .rs.getUserPref("python_path")
})


# Save Retry Timeout
# The maximum amount of seconds of retry for save operations.
.rs.addFunction("uiPrefs.saveRetryTimeout", function()
{
   .rs.getUserPref("save_retry_timeout")
})


# Use R's native pipe operator, |>
# Whether the Insert Pipe Operator command should use the native R pipe operator,
.rs.addFunction("uiPrefs.insertNativePipeOperator", function()
{
   .rs.getUserPref("insert_native_pipe_operator")
})


# Remember recently used items in Command Palette
# Whether to keep track of recently used commands in the Command Palette
.rs.addFunction("uiPrefs.commandPaletteMru", function()
{
   .rs.getUserPref("command_palette_mru")
})


# Show memory usage in Environment Pane
# Whether to compute and show memory usage in the Environment Pane
.rs.addFunction("uiPrefs.showMemoryUsage", function()
{
   .rs.getUserPref("show_memory_usage")
})


# Interval for requerying memory stats (seconds)
# How many seconds to wait between automatic requeries of memory statistics (0 to
.rs.addFunction("uiPrefs.memoryQueryIntervalSeconds", function()
{
   .rs.getUserPref("memory_query_interval_seconds")
})


# Enable terminal Python integration
# Enable Python terminal hooks. When enabled, the RStudio-configured version of
.rs.addFunction("uiPrefs.terminalPythonIntegration", function()
{
   .rs.getUserPref("terminal_python_integration")
})


# Session protocol debug logging
# Enable session protocol debug logging showing all session requests and events
.rs.addFunction("uiPrefs.sessionProtocolDebug", function()
{
   .rs.getUserPref("session_protocol_debug")
})


# Automatically activate project Python environments
# When enabled, if the active project contains a Python virtual environment, then
.rs.addFunction("uiPrefs.pythonProjectEnvironmentAutomaticActivate", function()
{
   .rs.getUserPref("python_project_environment_automatic_activate")
})


# Check values in the Environment pane for null external pointers
# When enabled, RStudio will detect R objects containing null external pointers
.rs.addFunction("uiPrefs.checkNullExternalPointers", function()
{
   .rs.getUserPref("check_null_external_pointers")
})


# User Interface Language:
# The IDE's user-interface language.
.rs.addFunction("uiPrefs.uiLanguage", function()
{
   .rs.getUserPref("ui_language")
})


# Auto hide menu bar
# Hide desktop menu bar until Alt key is pressed.
.rs.addFunction("uiPrefs.autohideMenubar", function()
{
   .rs.getUserPref("autohide_menubar")
})


# Use native file and message dialog boxes
# Whether RStudio Desktop will use the operating system's native File and Message
.rs.addFunction("uiPrefs.nativeFileDialogs", function()
{
   .rs.getUserPref("native_file_dialogs")
})


# Discard pending console input on error
# When enabled, any pending console input will be discarded when an (uncaught) R
.rs.addFunction("uiPrefs.discardPendingConsoleInputOnError", function()
{
   .rs.getUserPref("discard_pending_console_input_on_error")
})


# Editor scroll speed sensitivity
# An integer value, 1-200, to set the editor scroll multiplier. The higher the
.rs.addFunction("uiPrefs.editorScrollMultiplier", function()
{
   .rs.getUserPref("editor_scroll_multiplier")
})


# Text rendering
# Control how text is rendered within the IDE surface.
.rs.addFunction("uiPrefs.textRendering", function()
{
   .rs.getUserPref("text_rendering")
})


# Disable Electron accessibility support
# Disable Electron accessibility support.
.rs.addFunction("uiPrefs.disableRendererAccessibility", function()
{
   .rs.getUserPref("disable_renderer_accessibility")
})


# Enable GitHub Copilot
# When enabled, RStudio will use GitHub Copilot to provide code suggestions.
.rs.addFunction("uiPrefs.copilotEnabled", function()
{
   .rs.getUserPref("copilot_enabled")
})


# Show Copilot code suggestions:
# Control when Copilot code suggestions are displayed in the editor.
.rs.addFunction("uiPrefs.copilotCompletionsTrigger", function()
{
   .rs.getUserPref("copilot_completions_trigger")
})


# GitHub Copilot completions delay
# The delay (in milliseconds) before GitHub Copilot completions are requested
.rs.addFunction("uiPrefs.copilotCompletionsDelay", function()
{
   .rs.getUserPref("copilot_completions_delay")
})


# Pressing Tab key will prefer inserting:
# Control the behavior of the Tab key when both Copilot code suggestions and
.rs.addFunction("uiPrefs.copilotTabKeyBehavior", function()
{
   .rs.getUserPref("copilot_tab_key_behavior")
})


# Index project files with GitHub Copilot
# When enabled, RStudio will index project files with GitHub Copilot.
.rs.addFunction("uiPrefs.copilotIndexingEnabled", function()
{
   .rs.getUserPref("copilot_indexing_enabled")
})


# 
# User-provided name for the currently opened R project.
.rs.addFunction("uiPrefs.projectName", function()
{
   .rs.getUserPref("project_name")
})


# Default working directory for background jobs
# Default working directory in background job dialog.
.rs.addFunction("uiPrefs.runBackgroundJobDefaultWorkingDir", function()
{
   .rs.getUserPref("run_background_job_default_working_dir")
})


# Code formatter
# The formatter to use when reformatting code.
.rs.addFunction("uiPrefs.codeFormatter", function()
{
   .rs.getUserPref("code_formatter")
})


# Use strict transformers when formatting code
# When set, strict transformers will be used when formatting code. See the
.rs.addFunction("uiPrefs.codeFormatterStylerStrict", function()
{
   .rs.getUserPref("code_formatter_styler_strict")
})


# 
# The external command to be used when reformatting code.
.rs.addFunction("uiPrefs.codeFormatterExternalCommand", function()
{
   .rs.getUserPref("code_formatter_external_command")
})


# Reformat documents on save
# When set, the selected formatter will be used to reformat documents on save.
.rs.addFunction("uiPrefs.reformatOnSave", function()
{
   .rs.getUserPref("reformat_on_save")
})


# Default project user data directory
# The folder in which RStudio should store project .Rproj.user data.
.rs.addFunction("uiPrefs.projectUserDataDirectory", function()
{
   .rs.getUserPref("project_user_data_directory")
})


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

# Run .Rprofile on resume
# Whether to run .Rprofile again after resuming a suspended R session.
.rs.addFunction("uiPrefs.runRprofileOnResume", function()
{
   .rs.getUserPref("run_rprofile_on_resume")
})


# Save workspace on quit
# Whether to save the workspace to an .Rdata file after the R session ends.
.rs.addFunction("uiPrefs.saveWorkspace", function()
{
   .rs.getUserPref("save_workspace")
})


# Load workspace on start
# Whether to load the workspace when the R session begins.
.rs.addFunction("uiPrefs.loadWorkspace", function()
{
   .rs.getUserPref("load_workspace")
})


# Initial working directory
# The initial working directory for new R sessions.
.rs.addFunction("uiPrefs.initialWorkingDirectory", function()
{
   .rs.getUserPref("initial_working_directory")
})


# 
# The CRAN mirror to use.
.rs.addFunction("uiPrefs.cranMirror", function()
{
   .rs.getUserPref("cran_mirror")
})


# 
# The name of the default Bioconductor mirror.
.rs.addFunction("uiPrefs.bioconductorMirrorName", function()
{
   .rs.getUserPref("bioconductor_mirror_name")
})


# 
# The URL of the default Bioconductor mirror.
.rs.addFunction("uiPrefs.bioconductorMirrorUrl", function()
{
   .rs.getUserPref("bioconductor_mirror_url")
})


# Save R console history
# Whether to always save the R console history.
.rs.addFunction("uiPrefs.alwaysSaveHistory", function()
{
   .rs.getUserPref("always_save_history")
})


# Remove duplicates from console history
# Whether to remove duplicate entries from the R console history.
.rs.addFunction("uiPrefs.removeHistoryDuplicates", function()
{
   .rs.getUserPref("remove_history_duplicates")
})


# Show .Last.value in Environment pane
# Show the result of the last expression (.Last.value) in the Environment pane.
.rs.addFunction("uiPrefs.showLastDotValue", function()
{
   .rs.getUserPref("show_last_dot_value")
})


# Line ending format
# The line ending format to use when saving files.
.rs.addFunction("uiPrefs.lineEndingConversion", function()
{
   .rs.getUserPref("line_ending_conversion")
})


# Use newlines in Makefiles
# Whether to use newlines when saving Makefiles.
.rs.addFunction("uiPrefs.useNewlinesInMakefiles", function()
{
   .rs.getUserPref("use_newlines_in_makefiles")
})


# 
# The terminal shell to use on Windows.
.rs.addFunction("uiPrefs.windowsTerminalShell", function()
{
   .rs.getUserPref("windows_terminal_shell")
})


# 
# The terminal shell to use on POSIX operating systems (MacOS and Linux).
.rs.addFunction("uiPrefs.posixTerminalShell", function()
{
   .rs.getUserPref("posix_terminal_shell")
})


# 
# tab.
.rs.addFunction("uiPrefs.customShellCommand", function()
{
   .rs.getUserPref("custom_shell_command")
})


# 
# The command-line options to pass to the custom shell command.
.rs.addFunction("uiPrefs.customShellOptions", function()
{
   .rs.getUserPref("custom_shell_options")
})


# Show line numbers in editor
# Show line numbers in RStudio's code editor.
.rs.addFunction("uiPrefs.showLineNumbers", function()
{
   .rs.getUserPref("show_line_numbers")
})


# Use relative line numbers in editor
# Show relative, rather than absolute, line numbers in RStudio's code editor.
.rs.addFunction("uiPrefs.relativeLineNumbers", function()
{
   .rs.getUserPref("relative_line_numbers")
})


# Highlight selected word in editor
# Highlight the selected word in RStudio's code editor.
.rs.addFunction("uiPrefs.highlightSelectedWord", function()
{
   .rs.getUserPref("highlight_selected_word")
})


# Highlight selected line in editor
# Highlight the selected line in RStudio's code editor.
.rs.addFunction("uiPrefs.highlightSelectedLine", function()
{
   .rs.getUserPref("highlight_selected_line")
})


# 
# Layout of panes in the RStudio workbench.
.rs.addFunction("uiPrefs.panes", function()
{
   .rs.getUserPref("panes")
})


# Allow source columns
# Whether to enable the ability to add source columns to display.
.rs.addFunction("uiPrefs.allowSourceColumns", function()
{
   .rs.getUserPref("allow_source_columns")
})


# Insert spaces for Tab
# Whether to insert spaces when pressing the Tab key.
.rs.addFunction("uiPrefs.useSpacesForTab", function()
{
   .rs.getUserPref("use_spaces_for_tab")
})


# Number of spaces for Tab
# The number of spaces to insert when pressing the Tab key.
.rs.addFunction("uiPrefs.numSpacesForTab", function()
{
   .rs.getUserPref("num_spaces_for_tab")
})


# Auto-detect indentation in files
# Whether to automatically detect indentation settings from file contents.
.rs.addFunction("uiPrefs.autoDetectIndentation", function()
{
   .rs.getUserPref("auto_detect_indentation")
})


# Show margin in editor
# Whether to show the margin guide in the RStudio code editor.
.rs.addFunction("uiPrefs.showMargin", function()
{
   .rs.getUserPref("show_margin")
})


# Use a blinking cursor
# Whether to flash the cursor off and on.
.rs.addFunction("uiPrefs.blinkingCursor", function()
{
   .rs.getUserPref("blinking_cursor")
})


# Margin column
# The number of columns of text after which the margin is shown.
.rs.addFunction("uiPrefs.marginColumn", function()
{
   .rs.getUserPref("margin_column")
})


# Show invisible characters in editor
# code editor.
.rs.addFunction("uiPrefs.showInvisibles", function()
{
   .rs.getUserPref("show_invisibles")
})


# Indentation guides
# Style for indentation guides in the RStudio code editor.
.rs.addFunction("uiPrefs.indentGuides", function()
{
   .rs.getUserPref("indent_guides")
})


# Continue comments after adding new line
# a new line.
.rs.addFunction("uiPrefs.continueCommentsOnNewline", function()
{
   .rs.getUserPref("continue_comments_on_newline")
})


# Whether web links in comments are clickable
# Whether web links in comments are clickable.
.rs.addFunction("uiPrefs.highlightWebLink", function()
{
   .rs.getUserPref("highlight_web_link")
})


# Keybinding set for editor
# The keybindings to use in the RStudio code editor.
.rs.addFunction("uiPrefs.editorKeybindings", function()
{
   .rs.getUserPref("editor_keybindings")
})


# Auto-insert matching parentheses and brackets
# Whether to insert matching pairs, such as () and [], when the first is typed.
.rs.addFunction("uiPrefs.insertMatching", function()
{
   .rs.getUserPref("insert_matching")
})


# Insert spaces around = in R code
# Whether to insert spaces around the equals sign in R code.
.rs.addFunction("uiPrefs.insertSpacesAroundEquals", function()
{
   .rs.getUserPref("insert_spaces_around_equals")
})


# Insert parentheses after functions
# Whether to insert parentheses after function completions.
.rs.addFunction("uiPrefs.insertParensAfterFunctionCompletion", function()
{
   .rs.getUserPref("insert_parens_after_function_completion")
})


# Complete multi-line statements with Tab
# Whether to attempt completion of multiple-line statements when pressing Tab.
.rs.addFunction("uiPrefs.tabMultilineCompletion", function()
{
   .rs.getUserPref("tab_multiline_completion")
})


# Use Tab to trigger autocompletion
# Whether to attempt completion of statements when pressing Tab.
.rs.addFunction("uiPrefs.tabCompletion", function()
{
   .rs.getUserPref("tab_completion")
})


# Show function help tooltips on idle
# recently moved.
.rs.addFunction("uiPrefs.showHelpTooltipOnIdle", function()
{
   .rs.getUserPref("show_help_tooltip_on_idle")
})


# Surround selections with
# Which kinds of delimiters can be used to surround the current selection.
.rs.addFunction("uiPrefs.surroundSelection", function()
{
   .rs.getUserPref("surround_selection")
})


# Enable code snippets
# Whether to enable code snippets in the RStudio code editor.
.rs.addFunction("uiPrefs.enableSnippets", function()
{
   .rs.getUserPref("enable_snippets")
})


# Use code completion for R
# When to use auto-completion for R code in the RStudio code editor.
.rs.addFunction("uiPrefs.codeCompletion", function()
{
   .rs.getUserPref("code_completion")
})


# Use code completion for other languages
# the RStudio code editor.
.rs.addFunction("uiPrefs.codeCompletionOther", function()
{
   .rs.getUserPref("code_completion_other")
})


# Use code completion in the R console
# Whether to always use code completion in the R console.
.rs.addFunction("uiPrefs.consoleCodeCompletion", function()
{
   .rs.getUserPref("console_code_completion")
})


# Delay before completing code (ms)
# The number of milliseconds to wait before offering code suggestions.
.rs.addFunction("uiPrefs.codeCompletionDelay", function()
{
   .rs.getUserPref("code_completion_delay")
})


# Number of characters for code completion
# offered.
.rs.addFunction("uiPrefs.codeCompletionCharacters", function()
{
   .rs.getUserPref("code_completion_characters")
})


# Include all function arguments in completion list
# even if those arguments have already appear to be used in the current function
.rs.addFunction("uiPrefs.codeCompletionIncludeAlreadyUsed", function()
{
   .rs.getUserPref("code_completion_include_already_used")
})


# Show function signature tooltips
# Whether to show function signature tooltips during autocompletion.
.rs.addFunction("uiPrefs.showFunctionSignatureTooltips", function()
{
   .rs.getUserPref("show_function_signature_tooltips")
})


# Show data preview in autocompletion help popup
# and values.
.rs.addFunction("uiPrefs.showDataPreview", function()
{
   .rs.getUserPref("show_data_preview")
})


# Show diagnostics in R code
# code as you type.
.rs.addFunction("uiPrefs.showDiagnosticsR", function()
{
   .rs.getUserPref("show_diagnostics_r")
})


# Show diagnostics in C++ code
# Whether to show diagnostic messages for C++ code as you type.
.rs.addFunction("uiPrefs.showDiagnosticsCpp", function()
{
   .rs.getUserPref("show_diagnostics_cpp")
})


# Show diagnostics in YAML code
# Whether to show diagnostic messages for YAML code as you type.
.rs.addFunction("uiPrefs.showDiagnosticsYaml", function()
{
   .rs.getUserPref("show_diagnostics_yaml")
})


# Show diagnostics in other languages
# YAML).
.rs.addFunction("uiPrefs.showDiagnosticsOther", function()
{
   .rs.getUserPref("show_diagnostics_other")
})


# Show style diagnostics for R code
# Whether to show style diagnostics (suggestions for improving R code style)
.rs.addFunction("uiPrefs.styleDiagnostics", function()
{
   .rs.getUserPref("style_diagnostics")
})


# Check code for problems when saving
# Whether to check code for problems after saving it.
.rs.addFunction("uiPrefs.diagnosticsOnSave", function()
{
   .rs.getUserPref("diagnostics_on_save")
})


# Run R code diagnostics in the background
# Whether to run code diagnostics in the background, as you type.
.rs.addFunction("uiPrefs.backgroundDiagnostics", function()
{
   .rs.getUserPref("background_diagnostics")
})


# Run R code diagnostics after (ms)
# background.
.rs.addFunction("uiPrefs.backgroundDiagnosticsDelayMs", function()
{
   .rs.getUserPref("background_diagnostics_delay_ms")
})


# Run diagnostics on R function calls
# Whether to run diagnostics in R function calls.
.rs.addFunction("uiPrefs.diagnosticsInRFunctionCalls", function()
{
   .rs.getUserPref("diagnostics_in_r_function_calls")
})


# Check arguments to R function calls
# Whether to check arguments to R function calls.
.rs.addFunction("uiPrefs.checkArgumentsToRFunctionCalls", function()
{
   .rs.getUserPref("check_arguments_to_r_function_calls")
})


# Check for unexpected assignments
# Whether to check for unexpected variable assignments inside R function calls.
.rs.addFunction("uiPrefs.checkUnexpectedAssignmentInFunctionCall", function()
{
   .rs.getUserPref("check_unexpected_assignment_in_function_call")
})


# Warn when R variable used but not defined
# the current scope.
.rs.addFunction("uiPrefs.warnIfNoSuchVariableInScope", function()
{
   .rs.getUserPref("warn_if_no_such_variable_in_scope")
})


# Warn when R variable defined but not used
# the current scope
.rs.addFunction("uiPrefs.warnVariableDefinedButNotUsed", function()
{
   .rs.getUserPref("warn_variable_defined_but_not_used")
})


# Detect missing R packages in the editor
# dependencies.
.rs.addFunction("uiPrefs.autoDiscoverPackageDependencies", function()
{
   .rs.getUserPref("auto_discover_package_dependencies")
})


# Ensure files end with a newline when saving
# Whether to ensure that source files end with a newline character.
.rs.addFunction("uiPrefs.autoAppendNewline", function()
{
   .rs.getUserPref("auto_append_newline")
})


# Strip trailing whitespace when saving
# Whether to strip trailing whitespace from each line when saving.
.rs.addFunction("uiPrefs.stripTrailingWhitespace", function()
{
   .rs.getUserPref("strip_trailing_whitespace")
})


# Restore cursor position when reopening files
# when the file is opened.
.rs.addFunction("uiPrefs.restoreSourceDocumentCursorPosition", function()
{
   .rs.getUserPref("restore_source_document_cursor_position")
})


# Re-indent code when pasting
# Whether to automatically re-indent code when it's pasted into RStudio.
.rs.addFunction("uiPrefs.reindentOnPaste", function()
{
   .rs.getUserPref("reindent_on_paste")
})


# Vertically align function arguments
# indentation.
.rs.addFunction("uiPrefs.verticallyAlignArgumentsIndent", function()
{
   .rs.getUserPref("vertically_align_arguments_indent")
})


# Soft-wrap source files
# inserting newline characters.
.rs.addFunction("uiPrefs.softWrapRFiles", function()
{
   .rs.getUserPref("soft_wrap_r_files")
})


# Soft-wrap R Markdown files
# Notebooks)
.rs.addFunction("uiPrefs.softWrapRmdFiles", function()
{
   .rs.getUserPref("soft_wrap_rmd_files")
})


# Focus console after executing R code
# Whether to focus the R console after executing an R command from a script.
.rs.addFunction("uiPrefs.focusConsoleAfterExec", function()
{
   .rs.getUserPref("focus_console_after_exec")
})


# Fold style in editor
# The style of folding to use.
.rs.addFunction("uiPrefs.foldStyle", function()
{
   .rs.getUserPref("fold_style")
})


# Save R scripts before sourcing
# Whether to automatically save scripts before executing them.
.rs.addFunction("uiPrefs.saveBeforeSourcing", function()
{
   .rs.getUserPref("save_before_sourcing")
})


# Syntax highlighting in R console
# Whether to use syntax highlighting in the R console.
.rs.addFunction("uiPrefs.syntaxColorConsole", function()
{
   .rs.getUserPref("syntax_color_console")
})


# Different color for error output in R console
# Whether to display error, warning, and message output in a different color.
.rs.addFunction("uiPrefs.highlightConsoleErrors", function()
{
   .rs.getUserPref("highlight_console_errors")
})


# Scroll past end of file
# Whether to allow scrolling past the end of a file.
.rs.addFunction("uiPrefs.scrollPastEndOfDocument", function()
{
   .rs.getUserPref("scroll_past_end_of_document")
})


# Highlight R function calls
# Whether to highlight R function calls in the code editor.
.rs.addFunction("uiPrefs.highlightRFunctionCalls", function()
{
   .rs.getUserPref("highlight_r_function_calls")
})


# Enable preview of named and hexadecimal colors
# Whether to show preview for named and hexadecimal colors.
.rs.addFunction("uiPrefs.colorPreview", function()
{
   .rs.getUserPref("color_preview")
})


# Use rainbow parentheses
# Whether to highlight parentheses in a variety of colors.
.rs.addFunction("uiPrefs.rainbowParentheses", function()
{
   .rs.getUserPref("rainbow_parentheses")
})


# Use rainbow fenced divs
# Whether to highlight fenced divs in a variety of colors.
.rs.addFunction("uiPrefs.rainbowFencedDivs", function()
{
   .rs.getUserPref("rainbow_fenced_divs")
})


# Maximum characters per line in R console
# The maximum number of characters to display in a single line in the R console.
.rs.addFunction("uiPrefs.consoleLineLengthLimit", function()
{
   .rs.getUserPref("console_line_length_limit")
})


# Maximum lines in R console
# scrollback buffer.
.rs.addFunction("uiPrefs.consoleMaxLines", function()
{
   .rs.getUserPref("console_max_lines")
})


# ANSI escape codes in R console
# How to treat ANSI escape codes in the console.
.rs.addFunction("uiPrefs.ansiConsoleMode", function()
{
   .rs.getUserPref("ansi_console_mode")
})


# Limit visible console output
# Whether to only show a limited window of the total console output
.rs.addFunction("uiPrefs.limitVisibleConsole", function()
{
   .rs.getUserPref("limit_visible_console")
})


# Show toolbar on R Markdown chunks
# Whether to show a toolbar on code chunks in R Markdown documents.
.rs.addFunction("uiPrefs.showInlineToolbarForRCodeChunks", function()
{
   .rs.getUserPref("show_inline_toolbar_for_r_code_chunks")
})


# Highlight code chunks in R Markdown files
# background color.
.rs.addFunction("uiPrefs.highlightCodeChunks", function()
{
   .rs.getUserPref("highlight_code_chunks")
})


# Save files before building
# Whether to save all open, unsaved files before building the project.
.rs.addFunction("uiPrefs.saveFilesBeforeBuild", function()
{
   .rs.getUserPref("save_files_before_build")
})


# Save and reload R workspace on build
# project.
.rs.addFunction("uiPrefs.saveAndReloadWorkspaceOnBuild", function()
{
   .rs.getUserPref("save_and_reload_workspace_on_build")
})


# Editor font size (points)
# The default editor font size, in points.
.rs.addFunction("uiPrefs.fontSizePoints", function()
{
   .rs.getUserPref("font_size_points")
})


# Help panel font size (points)
# The help panel font size, in points.
.rs.addFunction("uiPrefs.helpFontSizePoints", function()
{
   .rs.getUserPref("help_font_size_points")
})


# Theme
# The name of the color theme to apply to the text editor in RStudio.
.rs.addFunction("uiPrefs.editorTheme", function()
{
   .rs.getUserPref("editor_theme")
})


# Enable editor fonts on RStudio Server
# Whether to use a custom editor font in RStudio Server.
.rs.addFunction("uiPrefs.serverEditorFontEnabled", function()
{
   .rs.getUserPref("server_editor_font_enabled")
})


# Editor font
# The name of the fixed-width editor font to use with RStudio Server.
.rs.addFunction("uiPrefs.serverEditorFont", function()
{
   .rs.getUserPref("server_editor_font")
})


# Default character encoding
# The default character encoding to use when saving files.
.rs.addFunction("uiPrefs.defaultEncoding", function()
{
   .rs.getUserPref("default_encoding")
})


# Show top toolbar
# Whether to show the toolbar at the top of the RStudio workbench.
.rs.addFunction("uiPrefs.toolbarVisible", function()
{
   .rs.getUserPref("toolbar_visible")
})


# Default new project location
# The directory path under which to place new projects by default.
.rs.addFunction("uiPrefs.defaultProjectLocation", function()
{
   .rs.getUserPref("default_project_location")
})


# Default open project location
# The default directory to use in file dialogs when opening a project.
.rs.addFunction("uiPrefs.defaultOpenProjectLocation", function()
{
   .rs.getUserPref("default_open_project_location")
})


# Source with echo by default
# Whether to echo R code when sourcing it.
.rs.addFunction("uiPrefs.sourceWithEcho", function()
{
   .rs.getUserPref("source_with_echo")
})


# Default Sweave engine
# The default engine to use when processing Sweave documents.
.rs.addFunction("uiPrefs.defaultSweaveEngine", function()
{
   .rs.getUserPref("default_sweave_engine")
})


# Default LaTeX program
# The default program to use when processing LaTeX documents.
.rs.addFunction("uiPrefs.defaultLatexProgram", function()
{
   .rs.getUserPref("default_latex_program")
})


# Use Roxygen for documentation
# Whether to use Roxygen for documentation.
.rs.addFunction("uiPrefs.useRoxygen", function()
{
   .rs.getUserPref("use_roxygen")
})


# Enable data import
# Whether to use RStudio's data import feature.
.rs.addFunction("uiPrefs.useDataimport", function()
{
   .rs.getUserPref("use_dataimport")
})


# PDF previewer
# The program to use to preview PDF files after generation.
.rs.addFunction("uiPrefs.pdfPreviewer", function()
{
   .rs.getUserPref("pdf_previewer")
})


# Enable Rnw concordance
# Whether to always enable the concordance for RNW files.
.rs.addFunction("uiPrefs.alwaysEnableRnwConcordance", function()
{
   .rs.getUserPref("always_enable_rnw_concordance")
})


# Insert numbered LaTeX sections
# Whether to insert numbered sections in LaTeX.
.rs.addFunction("uiPrefs.insertNumberedLatexSections", function()
{
   .rs.getUserPref("insert_numbered_latex_sections")
})


# Spelling dictionary language
# The language of the spelling dictionary to use for spell checking.
.rs.addFunction("uiPrefs.spellingDictionaryLanguage", function()
{
   .rs.getUserPref("spelling_dictionary_language")
})


# Custom spelling dictionaries
# The list of custom dictionaries to use when spell checking.
.rs.addFunction("uiPrefs.spellingCustomDictionaries", function()
{
   .rs.getUserPref("spelling_custom_dictionaries")
})


# Lint document after load (ms)
# loaded.
.rs.addFunction("uiPrefs.documentLoadLintDelay", function()
{
   .rs.getUserPref("document_load_lint_delay")
})


# Ignore uppercase words in spell check
# Whether to ignore words in uppercase when spell checking.
.rs.addFunction("uiPrefs.ignoreUppercaseWords", function()
{
   .rs.getUserPref("ignore_uppercase_words")
})


# Ignore words with numbers in spell check
# Whether to ignore words with numbers in them when spell checking.
.rs.addFunction("uiPrefs.ignoreWordsWithNumbers", function()
{
   .rs.getUserPref("ignore_words_with_numbers")
})


# Use real-time spellchecking
# Whether to enable real-time spellchecking by default.
.rs.addFunction("uiPrefs.realTimeSpellchecking", function()
{
   .rs.getUserPref("real_time_spellchecking")
})


# Navigate to build errors
# Whether to navigate to build errors.
.rs.addFunction("uiPrefs.navigateToBuildError", function()
{
   .rs.getUserPref("navigate_to_build_error")
})


# Enable the Packages pane
# Whether to enable RStudio's Packages pane.
.rs.addFunction("uiPrefs.packagesPaneEnabled", function()
{
   .rs.getUserPref("packages_pane_enabled")
})


# C++ template
# C++ template.
.rs.addFunction("uiPrefs.cppTemplate", function()
{
   .rs.getUserPref("cpp_template")
})


# Restore last opened documents on startup
# Whether to restore the last opened source documents when RStudio starts up.
.rs.addFunction("uiPrefs.restoreSourceDocuments", function()
{
   .rs.getUserPref("restore_source_documents")
})


# Handle errors only when user code present
# Whether to handle errors only when user code is on the stack.
.rs.addFunction("uiPrefs.handleErrorsInUserCodeOnly", function()
{
   .rs.getUserPref("handle_errors_in_user_code_only")
})


# Auto-expand error tracebacks
# Whether to automatically expand tracebacks when an error occurs.
.rs.addFunction("uiPrefs.autoExpandErrorTracebacks", function()
{
   .rs.getUserPref("auto_expand_error_tracebacks")
})


# Check for new version at startup
# Whether to check for new versions of RStudio when RStudio starts.
.rs.addFunction("uiPrefs.checkForUpdates", function()
{
   .rs.getUserPref("check_for_updates")
})


# Show internal functions when debugging
# debugging.
.rs.addFunction("uiPrefs.showInternalFunctions", function()
{
   .rs.getUserPref("show_internal_functions")
})


# Run Shiny applications in
# Where to display Shiny applications when they are run.
.rs.addFunction("uiPrefs.shinyViewerType", function()
{
   .rs.getUserPref("shiny_viewer_type")
})


# Run Shiny applications in the background
# Whether to run Shiny applications as background jobs.
.rs.addFunction("uiPrefs.shinyBackgroundJobs", function()
{
   .rs.getUserPref("shiny_background_jobs")
})


# Run Plumber APIs in
# Where to display Shiny applications when they are run.
.rs.addFunction("uiPrefs.plumberViewerType", function()
{
   .rs.getUserPref("plumber_viewer_type")
})


# Document author
# The default name to use as the document author when creating new documents.
.rs.addFunction("uiPrefs.documentAuthor", function()
{
   .rs.getUserPref("document_author")
})


# Use current date when rendering document
# Use current date when rendering document
.rs.addFunction("uiPrefs.rmdAutoDate", function()
{
   .rs.getUserPref("rmd_auto_date")
})


# Path to preferred R Markdown template
# The path to the preferred R Markdown template.
.rs.addFunction("uiPrefs.rmdPreferredTemplatePath", function()
{
   .rs.getUserPref("rmd_preferred_template_path")
})


# Display R Markdown documents in
# Where to display R Markdown documents when they have completed rendering.
.rs.addFunction("uiPrefs.rmdViewerType", function()
{
   .rs.getUserPref("rmd_viewer_type")
})


# Show diagnostic info when publishing
# Whether to show verbose diagnostic information when publishing content.
.rs.addFunction("uiPrefs.showPublishDiagnostics", function()
{
   .rs.getUserPref("show_publish_diagnostics")
})


# 
# Whether to show UI for publishing content to Posit Cloud.
.rs.addFunction("uiPrefs.enableCloudPublishUi", function()
{
   .rs.getUserPref("enable_cloud_publish_ui")
})


# Check SSL certificates when publishing
# Whether to check remote server SSL certificates when publishing content.
.rs.addFunction("uiPrefs.publishCheckCertificates", function()
{
   .rs.getUserPref("publish_check_certificates")
})


# Use custom CA bundle when publishing
# content.
.rs.addFunction("uiPrefs.usePublishCaBundle", function()
{
   .rs.getUserPref("use_publish_ca_bundle")
})


# Path to custom CA bundle for publishing
# content.
.rs.addFunction("uiPrefs.publishCaBundle", function()
{
   .rs.getUserPref("publish_ca_bundle")
})


# Show chunk output inline in all documents
# Whether to show chunk output inline for ordinary R Markdown documents.
.rs.addFunction("uiPrefs.rmdChunkOutputInline", function()
{
   .rs.getUserPref("rmd_chunk_output_inline")
})


# Open document outline by default
# documents.
.rs.addFunction("uiPrefs.showDocOutlineRmd", function()
{
   .rs.getUserPref("show_doc_outline_rmd")
})


# Document outline font size
# The font size to use for items in the document outline.
.rs.addFunction("uiPrefs.documentOutlineFontSize", function()
{
   .rs.getUserPref("document_outline_font_size")
})


# Automatically run Setup chunk when needed
# running other chunks.
.rs.addFunction("uiPrefs.autoRunSetupChunk", function()
{
   .rs.getUserPref("auto_run_setup_chunk")
})


# Hide console when running R Markdown chunks
# Whether to hide the R console when executing inline R Markdown chunks.
.rs.addFunction("uiPrefs.hideConsoleOnChunkExecute", function()
{
   .rs.getUserPref("hide_console_on_chunk_execute")
})


# Unit of R code execution
# The unit of R code to execute when the Execute command is invoked.
.rs.addFunction("uiPrefs.executionBehavior", function()
{
   .rs.getUserPref("execution_behavior")
})


# Show the Terminal tab
# Whether to show the Terminal tab.
.rs.addFunction("uiPrefs.showTerminalTab", function()
{
   .rs.getUserPref("show_terminal_tab")
})


# Use local echo in the Terminal
# Whether to use local echo in the Terminal.
.rs.addFunction("uiPrefs.terminalLocalEcho", function()
{
   .rs.getUserPref("terminal_local_echo")
})


# Use websockets in the Terminal
# Whether to use websockets to communicate with the shell in the Terminal tab.
.rs.addFunction("uiPrefs.terminalWebsockets", function()
{
   .rs.getUserPref("terminal_websockets")
})


# Close Terminal pane after shell exit
# Whether to close the terminal pane after the shell exits.
.rs.addFunction("uiPrefs.terminalCloseBehavior", function()
{
   .rs.getUserPref("terminal_close_behavior")
})


# Save and restore system environment in Terminal tab
# Terminal.
.rs.addFunction("uiPrefs.terminalTrackEnvironment", function()
{
   .rs.getUserPref("terminal_track_environment")
})


# Ignored environment variables
# environment variables within a Terminal. Environment variables in this list
.rs.addFunction("uiPrefs.terminalIgnoredEnvironmentVariables", function()
{
   .rs.getUserPref("terminal_ignored_environment_variables")
})


# Enable Terminal hooks
# the active version of Python on the PATH in new Terminal sessions.
.rs.addFunction("uiPrefs.terminalHooks", function()
{
   .rs.getUserPref("terminal_hooks")
})


# Terminal bell style
# Terminal bell style
.rs.addFunction("uiPrefs.terminalBellStyle", function()
{
   .rs.getUserPref("terminal_bell_style")
})


# Terminal tab rendering engine
# browsers or graphics cards
.rs.addFunction("uiPrefs.terminalRenderer", function()
{
   .rs.getUserPref("terminal_renderer")
})


# Make links in Terminal clickable
# Whether web links displayed in the Terminal tab are made clickable.
.rs.addFunction("uiPrefs.terminalWeblinks", function()
{
   .rs.getUserPref("terminal_weblinks")
})


# Show R Markdown render command
# Markdown tab.
.rs.addFunction("uiPrefs.showRmdRenderCommand", function()
{
   .rs.getUserPref("show_rmd_render_command")
})


# Enable dragging text in code editor
# it.
.rs.addFunction("uiPrefs.enableTextDrag", function()
{
   .rs.getUserPref("enable_text_drag")
})


# Show hidden files in Files pane
# Whether to show hidden files in the Files pane.
.rs.addFunction("uiPrefs.showHiddenFiles", function()
{
   .rs.getUserPref("show_hidden_files")
})


# Files always shown in the Files Pane
# regardless of whether hidden files are shown
.rs.addFunction("uiPrefs.alwaysShownFiles", function()
{
   .rs.getUserPref("always_shown_files")
})


# Extensions always shown in the Files Pane
# shown in the Files Pane, regardless of whether hidden files are shown
.rs.addFunction("uiPrefs.alwaysShownExtensions", function()
{
   .rs.getUserPref("always_shown_extensions")
})


# Sort file names naturally in Files pane
# file9.R
.rs.addFunction("uiPrefs.sortFileNamesNaturally", function()
{
   .rs.getUserPref("sort_file_names_naturally")
})


# Synchronize the Files pane with the current working directory
# working directory in R changes.
.rs.addFunction("uiPrefs.syncFilesPaneWorkingDir", function()
{
   .rs.getUserPref("sync_files_pane_working_dir")
})


# Jobs tab visibility
# The visibility of the Jobs tab.
.rs.addFunction("uiPrefs.jobsTabVisibility", function()
{
   .rs.getUserPref("jobs_tab_visibility")
})


# 
# Whether to show the Workbench Jobs tab in RStudio Pro and RStudio Workbench.
.rs.addFunction("uiPrefs.showLauncherJobsTab", function()
{
   .rs.getUserPref("show_launcher_jobs_tab")
})


# 
# Workbench.
.rs.addFunction("uiPrefs.launcherJobsSort", function()
{
   .rs.getUserPref("launcher_jobs_sort")
})


# 
# How to detect busy status in the Terminal.
.rs.addFunction("uiPrefs.busyDetection", function()
{
   .rs.getUserPref("busy_detection")
})


# 
# A list of apps that should not be considered busy in the Terminal.
.rs.addFunction("uiPrefs.busyExclusionList", function()
{
   .rs.getUserPref("busy_exclusion_list")
})


# Working directory for knitting
# The working directory to use when knitting R Markdown documents.
.rs.addFunction("uiPrefs.knitWorkingDir", function()
{
   .rs.getUserPref("knit_working_dir")
})


# Show in Document Outline
# Which objects to show in the document outline pane.
.rs.addFunction("uiPrefs.docOutlineShow", function()
{
   .rs.getUserPref("doc_outline_show")
})


# Preview LaTeX equations on idle
# recently.
.rs.addFunction("uiPrefs.latexPreviewOnCursorIdle", function()
{
   .rs.getUserPref("latex_preview_on_cursor_idle")
})


# Wrap around when going to previous/next tab
# Whether to wrap around when going to the previous or next editor tab.
.rs.addFunction("uiPrefs.wrapTabNavigation", function()
{
   .rs.getUserPref("wrap_tab_navigation")
})


# Global theme
# The theme to use for the main RStudio user interface.
.rs.addFunction("uiPrefs.globalTheme", function()
{
   .rs.getUserPref("global_theme")
})


# Ignore whitespace in VCS diffs
# Whether to ignore whitespace when generating diffs of version controlled files.
.rs.addFunction("uiPrefs.gitDiffIgnoreWhitespace", function()
{
   .rs.getUserPref("git_diff_ignore_whitespace")
})


# Sign git commits
# Whether to sign git commits.
.rs.addFunction("uiPrefs.gitSignedCommits", function()
{
   .rs.getUserPref("git_signed_commits")
})


# Double click to select in the Console
# Whether double-clicking should select a word in the Console pane.
.rs.addFunction("uiPrefs.consoleDoubleClickSelect", function()
{
   .rs.getUserPref("console_double_click_select")
})


# Warn when automatic session suspension is paused
# toolbar.
.rs.addFunction("uiPrefs.consoleSuspendBlockedNotice", function()
{
   .rs.getUserPref("console_suspend_blocked_notice")
})


# Number of seconds to delay warning
# paused. Higher values for less frequent notices.
.rs.addFunction("uiPrefs.consoleSuspendBlockedNoticeDelay", function()
{
   .rs.getUserPref("console_suspend_blocked_notice_delay")
})


# Create a Git repo in new projects
# Whether a git repo should be initialized inside new projects by default.
.rs.addFunction("uiPrefs.newProjGitInit", function()
{
   .rs.getUserPref("new_proj_git_init")
})


# Create an renv environment in new projects
# Whether an renv environment should be created inside new projects by default.
.rs.addFunction("uiPrefs.newProjUseRenv", function()
{
   .rs.getUserPref("new_proj_use_renv")
})


# Root document for PDF compilation
# The root document to use when compiling PDF documents.
.rs.addFunction("uiPrefs.rootDocument", function()
{
   .rs.getUserPref("root_document")
})


# Show user home page in RStudio Workbench
# When to show the server home page in RStudio Workbench.
.rs.addFunction("uiPrefs.showUserHomePage", function()
{
   .rs.getUserPref("show_user_home_page")
})


# 
# Whether to reuse sessions when opening projects in RStudio Workbench.
.rs.addFunction("uiPrefs.reuseSessionsForProjectLinks", function()
{
   .rs.getUserPref("reuse_sessions_for_project_links")
})


# Enable version control if available
# Whether to enable RStudio's version control system interface.
.rs.addFunction("uiPrefs.vcsEnabled", function()
{
   .rs.getUserPref("vcs_enabled")
})


# Auto-refresh state from version control
# Automatically refresh VCS status?
.rs.addFunction("uiPrefs.vcsAutorefresh", function()
{
   .rs.getUserPref("vcs_autorefresh")
})


# Path to Git executable
# The path to the Git executable to use.
.rs.addFunction("uiPrefs.gitExePath", function()
{
   .rs.getUserPref("git_exe_path")
})


# Path to Subversion executable
# The path to the Subversion executable to use.
.rs.addFunction("uiPrefs.svnExePath", function()
{
   .rs.getUserPref("svn_exe_path")
})


# 
# The path to the terminal executable to use.
.rs.addFunction("uiPrefs.terminalPath", function()
{
   .rs.getUserPref("terminal_path")
})


# 
# The path to the SSH key file to use.
.rs.addFunction("uiPrefs.rsaKeyPath", function()
{
   .rs.getUserPref("rsa_key_path")
})


# 
# The encryption type to use for the SSH key file.
.rs.addFunction("uiPrefs.sshKeyType", function()
{
   .rs.getUserPref("ssh_key_type")
})


# Use the devtools R package if available
# Whether to use the devtools R package.
.rs.addFunction("uiPrefs.useDevtools", function()
{
   .rs.getUserPref("use_devtools")
})


# Always use --preclean when installing package
# Always use --preclean when installing package.
.rs.addFunction("uiPrefs.cleanBeforeInstall", function()
{
   .rs.getUserPref("clean_before_install")
})


# Download R packages securely
# Whether to use secure downloads when fetching R packages.
.rs.addFunction("uiPrefs.useSecureDownload", function()
{
   .rs.getUserPref("use_secure_download")
})


# Clean up temporary files after R CMD CHECK
# Whether to clean up temporary files after running R CMD CHECK.
.rs.addFunction("uiPrefs.cleanupAfterRCmdCheck", function()
{
   .rs.getUserPref("cleanup_after_r_cmd_check")
})


# View directory after R CMD CHECK
# Whether to view the directory after running R CMD CHECK.
.rs.addFunction("uiPrefs.viewDirAfterRCmdCheck", function()
{
   .rs.getUserPref("view_dir_after_r_cmd_check")
})


# Hide object files in the Files pane
# Whether to hide object files in the Files pane.
.rs.addFunction("uiPrefs.hideObjectFiles", function()
{
   .rs.getUserPref("hide_object_files")
})


# Restore last project when starting RStudio
# Whether to restore the last project when starting RStudio.
.rs.addFunction("uiPrefs.restoreLastProject", function()
{
   .rs.getUserPref("restore_last_project")
})


# Number of seconds for safe project startup
# started.
.rs.addFunction("uiPrefs.projectSafeStartupSeconds", function()
{
   .rs.getUserPref("project_safe_startup_seconds")
})


# Use tinytex to compile .tex files
# Use tinytex to compile .tex files.
.rs.addFunction("uiPrefs.useTinytex", function()
{
   .rs.getUserPref("use_tinytex")
})


# Clean output after running Texi2Dvi
# Whether to clean output after running Texi2Dvi.
.rs.addFunction("uiPrefs.cleanTexi2dviOutput", function()
{
   .rs.getUserPref("clean_texi2dvi_output")
})


# Shell escape LaTeX documents
# Whether to enable shell escaping with LaTeX documents.
.rs.addFunction("uiPrefs.latexShellEscape", function()
{
   .rs.getUserPref("latex_shell_escape")
})


# Restore project R version in RStudio Pro and RStudio Workbench
# RStudio Workbench.
.rs.addFunction("uiPrefs.restoreProjectRVersion", function()
{
   .rs.getUserPref("restore_project_r_version")
})


# Clang verbosity level (0 - 2)
# The verbosity level to use with Clang (0 - 2)
.rs.addFunction("uiPrefs.clangVerbose", function()
{
   .rs.getUserPref("clang_verbose")
})


# Submit crash reports to RStudio
# Whether to automatically submit crash reports to RStudio.
.rs.addFunction("uiPrefs.submitCrashReports", function()
{
   .rs.getUserPref("submit_crash_reports")
})


# 
# The R version to use by default.
.rs.addFunction("uiPrefs.defaultRVersion", function()
{
   .rs.getUserPref("default_r_version")
})


# Maximum number of columns in data viewer
# The maximum number of columns to show at once in the data viewer.
.rs.addFunction("uiPrefs.dataViewerMaxColumns", function()
{
   .rs.getUserPref("data_viewer_max_columns")
})


# Maximum number of character in data viewer cells
# The maximum number of characters to show in a data viewer cell.
.rs.addFunction("uiPrefs.dataViewerMaxCellSize", function()
{
   .rs.getUserPref("data_viewer_max_cell_size")
})


# Enable support for screen readers
# Support accessibility aids such as screen readers.
.rs.addFunction("uiPrefs.enableScreenReader", function()
{
   .rs.getUserPref("enable_screen_reader")
})


# Seconds to wait before updating ARIA live region
# region.
.rs.addFunction("uiPrefs.typingStatusDelayMs", function()
{
   .rs.getUserPref("typing_status_delay_ms")
})


# Reduced animation/motion mode
# Reduce use of animations in the user interface.
.rs.addFunction("uiPrefs.reducedMotion", function()
{
   .rs.getUserPref("reduced_motion")
})


# Tab key always moves focus
# Tab key moves focus out of text editing controls instead of inserting tabs.
.rs.addFunction("uiPrefs.tabKeyMoveFocus", function()
{
   .rs.getUserPref("tab_key_move_focus")
})


# Tab key moves focus directly from find text to replace text in find panel
# replace text.
.rs.addFunction("uiPrefs.findPanelLegacyTabSequence", function()
{
   .rs.getUserPref("find_panel_legacy_tab_sequence")
})


# Show focus outline around focused panel
# Show which panel contains keyboard focus.
.rs.addFunction("uiPrefs.showPanelFocusRectangle", function()
{
   .rs.getUserPref("show_panel_focus_rectangle")
})


# Autosave mode on idle
# How to deal with changes to documents on idle.
.rs.addFunction("uiPrefs.autoSaveOnIdle", function()
{
   .rs.getUserPref("auto_save_on_idle")
})


# Idle period for document autosave (ms)
# The idle period, in milliseconds, after which documents should be auto-saved.
.rs.addFunction("uiPrefs.autoSaveIdleMs", function()
{
   .rs.getUserPref("auto_save_idle_ms")
})


# Save documents when editor loses input focus
# Whether to automatically save when the editor loses focus.
.rs.addFunction("uiPrefs.autoSaveOnBlur", function()
{
   .rs.getUserPref("auto_save_on_blur")
})


# Initial working directory for new terminals
# Initial directory for new terminals.
.rs.addFunction("uiPrefs.terminalInitialDirectory", function()
{
   .rs.getUserPref("terminal_initial_directory")
})


# Show full path to project in RStudio Desktop windows
# Whether to show the full path to project in desktop window title.
.rs.addFunction("uiPrefs.fullProjectPathInWindowTitle", function()
{
   .rs.getUserPref("full_project_path_in_window_title")
})


# Use visual editing by default for new markdown documents
# Whether to enable visual editing by default for new markdown documents
.rs.addFunction("uiPrefs.visualMarkdownEditingIsDefault", function()
{
   .rs.getUserPref("visual_markdown_editing_is_default")
})


# Default list spacing in visual markdown editing mode
# Default spacing for lists created in the visual editor
.rs.addFunction("uiPrefs.visualMarkdownEditingListSpacing", function()
{
   .rs.getUserPref("visual_markdown_editing_list_spacing")
})


# Wrap text in visual markdown editing mode
# Whether to automatically wrap text when writing markdown
.rs.addFunction("uiPrefs.visualMarkdownEditingWrap", function()
{
   .rs.getUserPref("visual_markdown_editing_wrap")
})


# Wrap column for visual markdown editing mode
# The column to wrap text at when writing markdown
.rs.addFunction("uiPrefs.visualMarkdownEditingWrapAtColumn", function()
{
   .rs.getUserPref("visual_markdown_editing_wrap_at_column")
})


# Place visual markdown footnotes in
# Placement of footnotes within markdown output.
.rs.addFunction("uiPrefs.visualMarkdownEditingReferencesLocation", function()
{
   .rs.getUserPref("visual_markdown_editing_references_location")
})


# Write canonical visual mode markdown in source mode
# Whether to write canonical visual mode markdown when saving from source mode.
.rs.addFunction("uiPrefs.visualMarkdownEditingCanonical", function()
{
   .rs.getUserPref("visual_markdown_editing_canonical")
})


# Max content width for visual markdown editor (px)
# Maximum content width for visual editing mode, in pixels
.rs.addFunction("uiPrefs.visualMarkdownEditingMaxContentWidth", function()
{
   .rs.getUserPref("visual_markdown_editing_max_content_width")
})


# Show document outline in visual markdown editing mode
# documents in visual mode.
.rs.addFunction("uiPrefs.visualMarkdownEditingShowDocOutline", function()
{
   .rs.getUserPref("visual_markdown_editing_show_doc_outline")
})


# Show margin in visual mode code blocks
# Whether to show the margin guide in the visual mode code blocks.
.rs.addFunction("uiPrefs.visualMarkdownEditingShowMargin", function()
{
   .rs.getUserPref("visual_markdown_editing_show_margin")
})


# Show line numbers in visual mode code blocks
# Whether to show line numbers in the code editors used in visual mode
.rs.addFunction("uiPrefs.visualMarkdownCodeEditorLineNumbers", function()
{
   .rs.getUserPref("visual_markdown_code_editor_line_numbers")
})


# Font size for visual editing mode
# The default visual editing mode font size, in points
.rs.addFunction("uiPrefs.visualMarkdownEditingFontSizePoints", function()
{
   .rs.getUserPref("visual_markdown_editing_font_size_points")
})


# Editor for code chunks in visual editing mode
# The name of the editor to use to provide code editing in visual mode
.rs.addFunction("uiPrefs.visualMarkdownCodeEditor", function()
{
   .rs.getUserPref("visual_markdown_code_editor")
})


# Zotero libraries
# Zotero libraries to insert citations from.
.rs.addFunction("uiPrefs.zoteroLibraries", function()
{
   .rs.getUserPref("zotero_libraries")
})


# 
# Preferred emoji skintone
.rs.addFunction("uiPrefs.emojiSkintone", function()
{
   .rs.getUserPref("emoji_skintone")
})


# Disabled aria-live announcements
# List of aria-live announcements to disable.
.rs.addFunction("uiPrefs.disabledAriaLiveAnnouncements", function()
{
   .rs.getUserPref("disabled_aria_live_announcements")
})


# Maximum number of console lines to announce
# Maximum number of lines of console output announced after a command.
.rs.addFunction("uiPrefs.screenreaderConsoleAnnounceLimit", function()
{
   .rs.getUserPref("screenreader_console_announce_limit")
})


# List of path components ignored by file monitor
# of these components.
.rs.addFunction("uiPrefs.fileMonitorIgnoredComponents", function()
{
   .rs.getUserPref("file_monitor_ignored_components")
})


# Install R package dependencies one at a time
# Whether to install R package dependencies one at a time.
.rs.addFunction("uiPrefs.installPkgDepsIndividually", function()
{
   .rs.getUserPref("install_pkg_deps_individually")
})


# R graphics backend
# R graphics backend.
.rs.addFunction("uiPrefs.graphicsBackend", function()
{
   .rs.getUserPref("graphics_backend")
})


# R graphics antialiasing method
# Type of anti-aliasing to be used for generated R plots.
.rs.addFunction("uiPrefs.graphicsAntialiasing", function()
{
   .rs.getUserPref("graphics_antialiasing")
})


# Fixed-width font list for RStudio Server
# List of fixed-width fonts to check for browser support.
.rs.addFunction("uiPrefs.browserFixedWidthFonts", function()
{
   .rs.getUserPref("browser_fixed_width_fonts")
})


# 
# The Python type.
.rs.addFunction("uiPrefs.pythonType", function()
{
   .rs.getUserPref("python_type")
})


# 
# The Python version.
.rs.addFunction("uiPrefs.pythonVersion", function()
{
   .rs.getUserPref("python_version")
})


# 
# The path to the default Python interpreter.
.rs.addFunction("uiPrefs.pythonPath", function()
{
   .rs.getUserPref("python_path")
})


# Save Retry Timeout
# The maximum amount of seconds of retry for save operations.
.rs.addFunction("uiPrefs.saveRetryTimeout", function()
{
   .rs.getUserPref("save_retry_timeout")
})


# Use R's native pipe operator, |>
# |>
.rs.addFunction("uiPrefs.insertNativePipeOperator", function()
{
   .rs.getUserPref("insert_native_pipe_operator")
})


# Remember recently used items in Command Palette
# Whether to keep track of recently used commands in the Command Palette
.rs.addFunction("uiPrefs.commandPaletteMru", function()
{
   .rs.getUserPref("command_palette_mru")
})


# Show memory usage in Environment Pane
# Whether to compute and show memory usage in the Environment Pane
.rs.addFunction("uiPrefs.showMemoryUsage", function()
{
   .rs.getUserPref("show_memory_usage")
})


# Interval for requerying memory stats (seconds)
# disable)
.rs.addFunction("uiPrefs.memoryQueryIntervalSeconds", function()
{
   .rs.getUserPref("memory_query_interval_seconds")
})


# Enable terminal Python integration
# Python will be placed on the PATH.
.rs.addFunction("uiPrefs.terminalPythonIntegration", function()
{
   .rs.getUserPref("terminal_python_integration")
})


# Session protocol debug logging
# Enable session protocol debug logging showing all session requests and events
.rs.addFunction("uiPrefs.sessionProtocolDebug", function()
{
   .rs.getUserPref("session_protocol_debug")
})


# Automatically activate project Python environments
# RStudio will automatically activate this environment on startup.
.rs.addFunction("uiPrefs.pythonProjectEnvironmentAutomaticActivate", function()
{
   .rs.getUserPref("python_project_environment_automatic_activate")
})


# Check values in the Environment pane for null external pointers
# when building the Environment pane, and avoid introspecting their contents
.rs.addFunction("uiPrefs.checkNullExternalPointers", function()
{
   .rs.getUserPref("check_null_external_pointers")
})


# User Interface Language:
# The IDE's user-interface language.
.rs.addFunction("uiPrefs.uiLanguage", function()
{
   .rs.getUserPref("ui_language")
})


# Auto hide menu bar
# Hide desktop menu bar until Alt key is pressed.
.rs.addFunction("uiPrefs.autohideMenubar", function()
{
   .rs.getUserPref("autohide_menubar")
})


# Use native file and message dialog boxes
# dialog boxes.
.rs.addFunction("uiPrefs.nativeFileDialogs", function()
{
   .rs.getUserPref("native_file_dialogs")
})


# Discard pending console input on error
# error occurs.
.rs.addFunction("uiPrefs.discardPendingConsoleInputOnError", function()
{
   .rs.getUserPref("discard_pending_console_input_on_error")
})


# Editor scroll speed sensitivity
# value, the faster the scrolling.
.rs.addFunction("uiPrefs.editorScrollMultiplier", function()
{
   .rs.getUserPref("editor_scroll_multiplier")
})


# Text rendering
# Control how text is rendered within the IDE surface.
.rs.addFunction("uiPrefs.textRendering", function()
{
   .rs.getUserPref("text_rendering")
})


# Disable Electron accessibility support
# Disable Electron accessibility support.
.rs.addFunction("uiPrefs.disableRendererAccessibility", function()
{
   .rs.getUserPref("disable_renderer_accessibility")
})


# Enable GitHub Copilot
# When enabled, RStudio will use GitHub Copilot to provide code suggestions.
.rs.addFunction("uiPrefs.copilotEnabled", function()
{
   .rs.getUserPref("copilot_enabled")
})


# Show Copilot code suggestions:
# Control when Copilot code suggestions are displayed in the editor.
.rs.addFunction("uiPrefs.copilotCompletionsTrigger", function()
{
   .rs.getUserPref("copilot_completions_trigger")
})


# GitHub Copilot completions delay
# after the cursor position has changed.
.rs.addFunction("uiPrefs.copilotCompletionsDelay", function()
{
   .rs.getUserPref("copilot_completions_delay")
})


# Pressing Tab key will prefer inserting:
# RStudio code completions are visible.
.rs.addFunction("uiPrefs.copilotTabKeyBehavior", function()
{
   .rs.getUserPref("copilot_tab_key_behavior")
})


# Index project files with GitHub Copilot
# When enabled, RStudio will index project files with GitHub Copilot.
.rs.addFunction("uiPrefs.copilotIndexingEnabled", function()
{
   .rs.getUserPref("copilot_indexing_enabled")
})


# 
# User-provided name for the currently opened R project.
.rs.addFunction("uiPrefs.projectName", function()
{
   .rs.getUserPref("project_name")
})


# Default working directory for background jobs
# Default working directory in background job dialog.
.rs.addFunction("uiPrefs.runBackgroundJobDefaultWorkingDir", function()
{
   .rs.getUserPref("run_background_job_default_working_dir")
})


# Code formatter
# The formatter to use when reformatting code.
.rs.addFunction("uiPrefs.codeFormatter", function()
{
   .rs.getUserPref("code_formatter")
})


# Use strict transformers when formatting code
# `styler` package documentation for more details.
.rs.addFunction("uiPrefs.codeFormatterStylerStrict", function()
{
   .rs.getUserPref("code_formatter_styler_strict")
})


# 
# The external command to be used when reformatting code.
.rs.addFunction("uiPrefs.codeFormatterExternalCommand", function()
{
   .rs.getUserPref("code_formatter_external_command")
})


# Reformat documents on save
# When set, the selected formatter will be used to reformat documents on save.
.rs.addFunction("uiPrefs.reformatOnSave", function()
{
   .rs.getUserPref("reformat_on_save")
})


# Default project user data directory
# The folder in which RStudio should store project .Rproj.user data.
.rs.addFunction("uiPrefs.projectUserDataDirectory", function()
{
   .rs.getUserPref("project_user_data_directory")
})


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

# Run .Rprofile on resume
# Whether to run .Rprofile again after resuming a suspended R session.
.rs.addFunction("uiPrefs.runRprofileOnResume", function()
{
   .rs.getUserPref("run_rprofile_on_resume")
})


# Save workspace on quit
# Whether to save the workspace to an .Rdata file after the R session ends.
.rs.addFunction("uiPrefs.saveWorkspace", function()
{
   .rs.getUserPref("save_workspace")
})


# Load workspace on start
# Whether to load the workspace when the R session begins.
.rs.addFunction("uiPrefs.loadWorkspace", function()
{
   .rs.getUserPref("load_workspace")
})


# Initial working directory
# The initial working directory for new R sessions.
.rs.addFunction("uiPrefs.initialWorkingDirectory", function()
{
   .rs.getUserPref("initial_working_directory")
})


# 
# The CRAN mirror to use.
.rs.addFunction("uiPrefs.cranMirror", function()
{
   .rs.getUserPref("cran_mirror")
})


# 
# The name of the default Bioconductor mirror.
.rs.addFunction("uiPrefs.bioconductorMirrorName", function()
{
   .rs.getUserPref("bioconductor_mirror_name")
})


# 
# The URL of the default Bioconductor mirror.
.rs.addFunction("uiPrefs.bioconductorMirrorUrl", function()
{
   .rs.getUserPref("bioconductor_mirror_url")
})


# Save R console history
# Whether to always save the R console history.
.rs.addFunction("uiPrefs.alwaysSaveHistory", function()
{
   .rs.getUserPref("always_save_history")
})


# Remove duplicates from console history
# Whether to remove duplicate entries from the R console history.
.rs.addFunction("uiPrefs.removeHistoryDuplicates", function()
{
   .rs.getUserPref("remove_history_duplicates")
})


# Show .Last.value in Environment pane
# Show the result of the last expression (.Last.value) in the Environment pane.
.rs.addFunction("uiPrefs.showLastDotValue", function()
{
   .rs.getUserPref("show_last_dot_value")
})


# Line ending format
# The line ending format to use when saving files.
.rs.addFunction("uiPrefs.lineEndingConversion", function()
{
   .rs.getUserPref("line_ending_conversion")
})


# Use newlines in Makefiles
# Whether to use newlines when saving Makefiles.
.rs.addFunction("uiPrefs.useNewlinesInMakefiles", function()
{
   .rs.getUserPref("use_newlines_in_makefiles")
})


# 
# The terminal shell to use on Windows.
.rs.addFunction("uiPrefs.windowsTerminalShell", function()
{
   .rs.getUserPref("windows_terminal_shell")
})


# 
# The terminal shell to use on POSIX operating systems (MacOS and Linux).
.rs.addFunction("uiPrefs.posixTerminalShell", function()
{
   .rs.getUserPref("posix_terminal_shell")
})


# 
# The fully qualified path to the custom shell command to use in the Terminal
.rs.addFunction("uiPrefs.customShellCommand", function()
{
   .rs.getUserPref("custom_shell_command")
})


# 
# The command-line options to pass to the custom shell command.
.rs.addFunction("uiPrefs.customShellOptions", function()
{
   .rs.getUserPref("custom_shell_options")
})


# Show line numbers in editor
# Show line numbers in RStudio's code editor.
.rs.addFunction("uiPrefs.showLineNumbers", function()
{
   .rs.getUserPref("show_line_numbers")
})


# Use relative line numbers in editor
# Show relative, rather than absolute, line numbers in RStudio's code editor.
.rs.addFunction("uiPrefs.relativeLineNumbers", function()
{
   .rs.getUserPref("relative_line_numbers")
})


# Highlight selected word in editor
# Highlight the selected word in RStudio's code editor.
.rs.addFunction("uiPrefs.highlightSelectedWord", function()
{
   .rs.getUserPref("highlight_selected_word")
})


# Highlight selected line in editor
# Highlight the selected line in RStudio's code editor.
.rs.addFunction("uiPrefs.highlightSelectedLine", function()
{
   .rs.getUserPref("highlight_selected_line")
})


# 
# Layout of panes in the RStudio workbench.
.rs.addFunction("uiPrefs.panes", function()
{
   .rs.getUserPref("panes")
})


# Allow source columns
# Whether to enable the ability to add source columns to display.
.rs.addFunction("uiPrefs.allowSourceColumns", function()
{
   .rs.getUserPref("allow_source_columns")
})


# Insert spaces for Tab
# Whether to insert spaces when pressing the Tab key.
.rs.addFunction("uiPrefs.useSpacesForTab", function()
{
   .rs.getUserPref("use_spaces_for_tab")
})


# Number of spaces for Tab
# The number of spaces to insert when pressing the Tab key.
.rs.addFunction("uiPrefs.numSpacesForTab", function()
{
   .rs.getUserPref("num_spaces_for_tab")
})


# Auto-detect indentation in files
# Whether to automatically detect indentation settings from file contents.
.rs.addFunction("uiPrefs.autoDetectIndentation", function()
{
   .rs.getUserPref("auto_detect_indentation")
})


# Show margin in editor
# Whether to show the margin guide in the RStudio code editor.
.rs.addFunction("uiPrefs.showMargin", function()
{
   .rs.getUserPref("show_margin")
})


# Use a blinking cursor
# Whether to flash the cursor off and on.
.rs.addFunction("uiPrefs.blinkingCursor", function()
{
   .rs.getUserPref("blinking_cursor")
})


# Margin column
# The number of columns of text after which the margin is shown.
.rs.addFunction("uiPrefs.marginColumn", function()
{
   .rs.getUserPref("margin_column")
})


# Show invisible characters in editor
# Whether to show invisible characters, such as spaces and tabs, in the RStudio
.rs.addFunction("uiPrefs.showInvisibles", function()
{
   .rs.getUserPref("show_invisibles")
})


# Indentation guides
# Style for indentation guides in the RStudio code editor.
.rs.addFunction("uiPrefs.indentGuides", function()
{
   .rs.getUserPref("indent_guides")
})


# Continue comments after adding new line
# Whether to continue comments (by inserting the comment character) after adding
.rs.addFunction("uiPrefs.continueCommentsOnNewline", function()
{
   .rs.getUserPref("continue_comments_on_newline")
})


# Whether web links in comments are clickable
# Whether web links in comments are clickable.
.rs.addFunction("uiPrefs.highlightWebLink", function()
{
   .rs.getUserPref("highlight_web_link")
})


# Keybinding set for editor
# The keybindings to use in the RStudio code editor.
.rs.addFunction("uiPrefs.editorKeybindings", function()
{
   .rs.getUserPref("editor_keybindings")
})


# Auto-insert matching parentheses and brackets
# Whether to insert matching pairs, such as () and [], when the first is typed.
.rs.addFunction("uiPrefs.insertMatching", function()
{
   .rs.getUserPref("insert_matching")
})


# Insert spaces around = in R code
# Whether to insert spaces around the equals sign in R code.
.rs.addFunction("uiPrefs.insertSpacesAroundEquals", function()
{
   .rs.getUserPref("insert_spaces_around_equals")
})


# Insert parentheses after functions
# Whether to insert parentheses after function completions.
.rs.addFunction("uiPrefs.insertParensAfterFunctionCompletion", function()
{
   .rs.getUserPref("insert_parens_after_function_completion")
})


# Complete multi-line statements with Tab
# Whether to attempt completion of multiple-line statements when pressing Tab.
.rs.addFunction("uiPrefs.tabMultilineCompletion", function()
{
   .rs.getUserPref("tab_multiline_completion")
})


# Use Tab to trigger autocompletion
# Whether to attempt completion of statements when pressing Tab.
.rs.addFunction("uiPrefs.tabCompletion", function()
{
   .rs.getUserPref("tab_completion")
})


# Show function help tooltips on idle
# Whether to show help tooltips for functions when the cursor has not been
.rs.addFunction("uiPrefs.showHelpTooltipOnIdle", function()
{
   .rs.getUserPref("show_help_tooltip_on_idle")
})


# Surround selections with
# Which kinds of delimiters can be used to surround the current selection.
.rs.addFunction("uiPrefs.surroundSelection", function()
{
   .rs.getUserPref("surround_selection")
})


# Enable code snippets
# Whether to enable code snippets in the RStudio code editor.
.rs.addFunction("uiPrefs.enableSnippets", function()
{
   .rs.getUserPref("enable_snippets")
})


# Use code completion for R
# When to use auto-completion for R code in the RStudio code editor.
.rs.addFunction("uiPrefs.codeCompletion", function()
{
   .rs.getUserPref("code_completion")
})


# Use code completion for other languages
# When to use auto-completion for other languages (such as JavaScript and SQL) in
.rs.addFunction("uiPrefs.codeCompletionOther", function()
{
   .rs.getUserPref("code_completion_other")
})


# Use code completion in the R console
# Whether to always use code completion in the R console.
.rs.addFunction("uiPrefs.consoleCodeCompletion", function()
{
   .rs.getUserPref("console_code_completion")
})


# Delay before completing code (ms)
# The number of milliseconds to wait before offering code suggestions.
.rs.addFunction("uiPrefs.codeCompletionDelay", function()
{
   .rs.getUserPref("code_completion_delay")
})


# Number of characters for code completion
# The number of characters in a symbol that can be entered before completions are
.rs.addFunction("uiPrefs.codeCompletionCharacters", function()
{
   .rs.getUserPref("code_completion_characters")
})


# Include all function arguments in completion list
# invocation.
.rs.addFunction("uiPrefs.codeCompletionIncludeAlreadyUsed", function()
{
   .rs.getUserPref("code_completion_include_already_used")
})


# Show function signature tooltips
# Whether to show function signature tooltips during autocompletion.
.rs.addFunction("uiPrefs.showFunctionSignatureTooltips", function()
{
   .rs.getUserPref("show_function_signature_tooltips")
})


# Show data preview in autocompletion help popup
# Whether a data preview is shown in the autocompletion help popup for datasets
.rs.addFunction("uiPrefs.showDataPreview", function()
{
   .rs.getUserPref("show_data_preview")
})


# Show diagnostics in R code
# Whether to show diagnostic messages (such as syntax and usage errors) for R
.rs.addFunction("uiPrefs.showDiagnosticsR", function()
{
   .rs.getUserPref("show_diagnostics_r")
})


# Show diagnostics in C++ code
# Whether to show diagnostic messages for C++ code as you type.
.rs.addFunction("uiPrefs.showDiagnosticsCpp", function()
{
   .rs.getUserPref("show_diagnostics_cpp")
})


# Show diagnostics in YAML code
# Whether to show diagnostic messages for YAML code as you type.
.rs.addFunction("uiPrefs.showDiagnosticsYaml", function()
{
   .rs.getUserPref("show_diagnostics_yaml")
})


# Show diagnostics in other languages
# Whether to show diagnostic messages for other types of code (not R, C++, or
.rs.addFunction("uiPrefs.showDiagnosticsOther", function()
{
   .rs.getUserPref("show_diagnostics_other")
})


# Show style diagnostics for R code
# Whether to show style diagnostics (suggestions for improving R code style)
.rs.addFunction("uiPrefs.styleDiagnostics", function()
{
   .rs.getUserPref("style_diagnostics")
})


# Check code for problems when saving
# Whether to check code for problems after saving it.
.rs.addFunction("uiPrefs.diagnosticsOnSave", function()
{
   .rs.getUserPref("diagnostics_on_save")
})


# Run R code diagnostics in the background
# Whether to run code diagnostics in the background, as you type.
.rs.addFunction("uiPrefs.backgroundDiagnostics", function()
{
   .rs.getUserPref("background_diagnostics")
})


# Run R code diagnostics after (ms)
# The number of milliseconds to delay before running code diagnostics in the
.rs.addFunction("uiPrefs.backgroundDiagnosticsDelayMs", function()
{
   .rs.getUserPref("background_diagnostics_delay_ms")
})


# Run diagnostics on R function calls
# Whether to run diagnostics in R function calls.
.rs.addFunction("uiPrefs.diagnosticsInRFunctionCalls", function()
{
   .rs.getUserPref("diagnostics_in_r_function_calls")
})


# Check arguments to R function calls
# Whether to check arguments to R function calls.
.rs.addFunction("uiPrefs.checkArgumentsToRFunctionCalls", function()
{
   .rs.getUserPref("check_arguments_to_r_function_calls")
})


# Check for unexpected assignments
# Whether to check for unexpected variable assignments inside R function calls.
.rs.addFunction("uiPrefs.checkUnexpectedAssignmentInFunctionCall", function()
{
   .rs.getUserPref("check_unexpected_assignment_in_function_call")
})


# Warn when R variable used but not defined
# Whether to generate a warning if a variable is used without being defined in
.rs.addFunction("uiPrefs.warnIfNoSuchVariableInScope", function()
{
   .rs.getUserPref("warn_if_no_such_variable_in_scope")
})


# Warn when R variable defined but not used
# Whether to generate a warning if a variable is defined without being used in
.rs.addFunction("uiPrefs.warnVariableDefinedButNotUsed", function()
{
   .rs.getUserPref("warn_variable_defined_but_not_used")
})


# Detect missing R packages in the editor
# Whether to automatically discover and offer to install missing R package
.rs.addFunction("uiPrefs.autoDiscoverPackageDependencies", function()
{
   .rs.getUserPref("auto_discover_package_dependencies")
})


# Ensure files end with a newline when saving
# Whether to ensure that source files end with a newline character.
.rs.addFunction("uiPrefs.autoAppendNewline", function()
{
   .rs.getUserPref("auto_append_newline")
})


# Strip trailing whitespace when saving
# Whether to strip trailing whitespace from each line when saving.
.rs.addFunction("uiPrefs.stripTrailingWhitespace", function()
{
   .rs.getUserPref("strip_trailing_whitespace")
})


# Restore cursor position when reopening files
# Whether to save the position of the cursor when a file is closed, restore it
.rs.addFunction("uiPrefs.restoreSourceDocumentCursorPosition", function()
{
   .rs.getUserPref("restore_source_document_cursor_position")
})


# Re-indent code when pasting
# Whether to automatically re-indent code when it's pasted into RStudio.
.rs.addFunction("uiPrefs.reindentOnPaste", function()
{
   .rs.getUserPref("reindent_on_paste")
})


# Vertically align function arguments
# Whether to vertically align arguments to R function calls during automatic
.rs.addFunction("uiPrefs.verticallyAlignArgumentsIndent", function()
{
   .rs.getUserPref("vertically_align_arguments_indent")
})


# Soft-wrap source files
# Whether to soft-wrap source files, wrapping the text for display without
.rs.addFunction("uiPrefs.softWrapRFiles", function()
{
   .rs.getUserPref("soft_wrap_r_files")
})


# Soft-wrap R Markdown files
# Whether to soft-wrap R Markdown files (and similar types such as R HTML and R
.rs.addFunction("uiPrefs.softWrapRmdFiles", function()
{
   .rs.getUserPref("soft_wrap_rmd_files")
})


# Focus console after executing R code
# Whether to focus the R console after executing an R command from a script.
.rs.addFunction("uiPrefs.focusConsoleAfterExec", function()
{
   .rs.getUserPref("focus_console_after_exec")
})


# Fold style in editor
# The style of folding to use.
.rs.addFunction("uiPrefs.foldStyle", function()
{
   .rs.getUserPref("fold_style")
})


# Save R scripts before sourcing
# Whether to automatically save scripts before executing them.
.rs.addFunction("uiPrefs.saveBeforeSourcing", function()
{
   .rs.getUserPref("save_before_sourcing")
})


# Syntax highlighting in R console
# Whether to use syntax highlighting in the R console.
.rs.addFunction("uiPrefs.syntaxColorConsole", function()
{
   .rs.getUserPref("syntax_color_console")
})


# Different color for error output in R console
# Whether to display error, warning, and message output in a different color.
.rs.addFunction("uiPrefs.highlightConsoleErrors", function()
{
   .rs.getUserPref("highlight_console_errors")
})


# Scroll past end of file
# Whether to allow scrolling past the end of a file.
.rs.addFunction("uiPrefs.scrollPastEndOfDocument", function()
{
   .rs.getUserPref("scroll_past_end_of_document")
})


# Highlight R function calls
# Whether to highlight R function calls in the code editor.
.rs.addFunction("uiPrefs.highlightRFunctionCalls", function()
{
   .rs.getUserPref("highlight_r_function_calls")
})


# Enable preview of named and hexadecimal colors
# Whether to show preview for named and hexadecimal colors.
.rs.addFunction("uiPrefs.colorPreview", function()
{
   .rs.getUserPref("color_preview")
})


# Use rainbow parentheses
# Whether to highlight parentheses in a variety of colors.
.rs.addFunction("uiPrefs.rainbowParentheses", function()
{
   .rs.getUserPref("rainbow_parentheses")
})


# Use rainbow fenced divs
# Whether to highlight fenced divs in a variety of colors.
.rs.addFunction("uiPrefs.rainbowFencedDivs", function()
{
   .rs.getUserPref("rainbow_fenced_divs")
})


# Maximum characters per line in R console
# The maximum number of characters to display in a single line in the R console.
.rs.addFunction("uiPrefs.consoleLineLengthLimit", function()
{
   .rs.getUserPref("console_line_length_limit")
})


# Maximum lines in R console
# The maximum number of console actions to store and display in the console
.rs.addFunction("uiPrefs.consoleMaxLines", function()
{
   .rs.getUserPref("console_max_lines")
})


# ANSI escape codes in R console
# How to treat ANSI escape codes in the console.
.rs.addFunction("uiPrefs.ansiConsoleMode", function()
{
   .rs.getUserPref("ansi_console_mode")
})


# Limit visible console output
# Whether to only show a limited window of the total console output
.rs.addFunction("uiPrefs.limitVisibleConsole", function()
{
   .rs.getUserPref("limit_visible_console")
})


# Show toolbar on R Markdown chunks
# Whether to show a toolbar on code chunks in R Markdown documents.
.rs.addFunction("uiPrefs.showInlineToolbarForRCodeChunks", function()
{
   .rs.getUserPref("show_inline_toolbar_for_r_code_chunks")
})


# Highlight code chunks in R Markdown files
# Whether to highlight code chunks in R Markdown documents with a different
.rs.addFunction("uiPrefs.highlightCodeChunks", function()
{
   .rs.getUserPref("highlight_code_chunks")
})


# Save files before building
# Whether to save all open, unsaved files before building the project.
.rs.addFunction("uiPrefs.saveFilesBeforeBuild", function()
{
   .rs.getUserPref("save_files_before_build")
})


# Save and reload R workspace on build
# Whether RStudio should save and reload the R workspace when building the
.rs.addFunction("uiPrefs.saveAndReloadWorkspaceOnBuild", function()
{
   .rs.getUserPref("save_and_reload_workspace_on_build")
})


# Editor font size (points)
# The default editor font size, in points.
.rs.addFunction("uiPrefs.fontSizePoints", function()
{
   .rs.getUserPref("font_size_points")
})


# Help panel font size (points)
# The help panel font size, in points.
.rs.addFunction("uiPrefs.helpFontSizePoints", function()
{
   .rs.getUserPref("help_font_size_points")
})


# Theme
# The name of the color theme to apply to the text editor in RStudio.
.rs.addFunction("uiPrefs.editorTheme", function()
{
   .rs.getUserPref("editor_theme")
})


# Enable editor fonts on RStudio Server
# Whether to use a custom editor font in RStudio Server.
.rs.addFunction("uiPrefs.serverEditorFontEnabled", function()
{
   .rs.getUserPref("server_editor_font_enabled")
})


# Editor font
# The name of the fixed-width editor font to use with RStudio Server.
.rs.addFunction("uiPrefs.serverEditorFont", function()
{
   .rs.getUserPref("server_editor_font")
})


# Default character encoding
# The default character encoding to use when saving files.
.rs.addFunction("uiPrefs.defaultEncoding", function()
{
   .rs.getUserPref("default_encoding")
})


# Show top toolbar
# Whether to show the toolbar at the top of the RStudio workbench.
.rs.addFunction("uiPrefs.toolbarVisible", function()
{
   .rs.getUserPref("toolbar_visible")
})


# Default new project location
# The directory path under which to place new projects by default.
.rs.addFunction("uiPrefs.defaultProjectLocation", function()
{
   .rs.getUserPref("default_project_location")
})


# Default open project location
# The default directory to use in file dialogs when opening a project.
.rs.addFunction("uiPrefs.defaultOpenProjectLocation", function()
{
   .rs.getUserPref("default_open_project_location")
})


# Source with echo by default
# Whether to echo R code when sourcing it.
.rs.addFunction("uiPrefs.sourceWithEcho", function()
{
   .rs.getUserPref("source_with_echo")
})


# Default Sweave engine
# The default engine to use when processing Sweave documents.
.rs.addFunction("uiPrefs.defaultSweaveEngine", function()
{
   .rs.getUserPref("default_sweave_engine")
})


# Default LaTeX program
# The default program to use when processing LaTeX documents.
.rs.addFunction("uiPrefs.defaultLatexProgram", function()
{
   .rs.getUserPref("default_latex_program")
})


# Use Roxygen for documentation
# Whether to use Roxygen for documentation.
.rs.addFunction("uiPrefs.useRoxygen", function()
{
   .rs.getUserPref("use_roxygen")
})


# Enable data import
# Whether to use RStudio's data import feature.
.rs.addFunction("uiPrefs.useDataimport", function()
{
   .rs.getUserPref("use_dataimport")
})


# PDF previewer
# The program to use to preview PDF files after generation.
.rs.addFunction("uiPrefs.pdfPreviewer", function()
{
   .rs.getUserPref("pdf_previewer")
})


# Enable Rnw concordance
# Whether to always enable the concordance for RNW files.
.rs.addFunction("uiPrefs.alwaysEnableRnwConcordance", function()
{
   .rs.getUserPref("always_enable_rnw_concordance")
})


# Insert numbered LaTeX sections
# Whether to insert numbered sections in LaTeX.
.rs.addFunction("uiPrefs.insertNumberedLatexSections", function()
{
   .rs.getUserPref("insert_numbered_latex_sections")
})


# Spelling dictionary language
# The language of the spelling dictionary to use for spell checking.
.rs.addFunction("uiPrefs.spellingDictionaryLanguage", function()
{
   .rs.getUserPref("spelling_dictionary_language")
})


# Custom spelling dictionaries
# The list of custom dictionaries to use when spell checking.
.rs.addFunction("uiPrefs.spellingCustomDictionaries", function()
{
   .rs.getUserPref("spelling_custom_dictionaries")
})


# Lint document after load (ms)
# The number of milliseconds to wait before linting a document after it is
.rs.addFunction("uiPrefs.documentLoadLintDelay", function()
{
   .rs.getUserPref("document_load_lint_delay")
})


# Ignore uppercase words in spell check
# Whether to ignore words in uppercase when spell checking.
.rs.addFunction("uiPrefs.ignoreUppercaseWords", function()
{
   .rs.getUserPref("ignore_uppercase_words")
})


# Ignore words with numbers in spell check
# Whether to ignore words with numbers in them when spell checking.
.rs.addFunction("uiPrefs.ignoreWordsWithNumbers", function()
{
   .rs.getUserPref("ignore_words_with_numbers")
})


# Use real-time spellchecking
# Whether to enable real-time spellchecking by default.
.rs.addFunction("uiPrefs.realTimeSpellchecking", function()
{
   .rs.getUserPref("real_time_spellchecking")
})


# Navigate to build errors
# Whether to navigate to build errors.
.rs.addFunction("uiPrefs.navigateToBuildError", function()
{
   .rs.getUserPref("navigate_to_build_error")
})


# Enable the Packages pane
# Whether to enable RStudio's Packages pane.
.rs.addFunction("uiPrefs.packagesPaneEnabled", function()
{
   .rs.getUserPref("packages_pane_enabled")
})


# C++ template
# C++ template.
.rs.addFunction("uiPrefs.cppTemplate", function()
{
   .rs.getUserPref("cpp_template")
})


# Restore last opened documents on startup
# Whether to restore the last opened source documents when RStudio starts up.
.rs.addFunction("uiPrefs.restoreSourceDocuments", function()
{
   .rs.getUserPref("restore_source_documents")
})


# Handle errors only when user code present
# Whether to handle errors only when user code is on the stack.
.rs.addFunction("uiPrefs.handleErrorsInUserCodeOnly", function()
{
   .rs.getUserPref("handle_errors_in_user_code_only")
})


# Auto-expand error tracebacks
# Whether to automatically expand tracebacks when an error occurs.
.rs.addFunction("uiPrefs.autoExpandErrorTracebacks", function()
{
   .rs.getUserPref("auto_expand_error_tracebacks")
})


# Check for new version at startup
# Whether to check for new versions of RStudio when RStudio starts.
.rs.addFunction("uiPrefs.checkForUpdates", function()
{
   .rs.getUserPref("check_for_updates")
})


# Show internal functions when debugging
# Whether to show functions without source references in the Traceback pane while
.rs.addFunction("uiPrefs.showInternalFunctions", function()
{
   .rs.getUserPref("show_internal_functions")
})


# Run Shiny applications in
# Where to display Shiny applications when they are run.
.rs.addFunction("uiPrefs.shinyViewerType", function()
{
   .rs.getUserPref("shiny_viewer_type")
})


# Run Shiny applications in the background
# Whether to run Shiny applications as background jobs.
.rs.addFunction("uiPrefs.shinyBackgroundJobs", function()
{
   .rs.getUserPref("shiny_background_jobs")
})


# Run Plumber APIs in
# Where to display Shiny applications when they are run.
.rs.addFunction("uiPrefs.plumberViewerType", function()
{
   .rs.getUserPref("plumber_viewer_type")
})


# Document author
# The default name to use as the document author when creating new documents.
.rs.addFunction("uiPrefs.documentAuthor", function()
{
   .rs.getUserPref("document_author")
})


# Use current date when rendering document
# Use current date when rendering document
.rs.addFunction("uiPrefs.rmdAutoDate", function()
{
   .rs.getUserPref("rmd_auto_date")
})


# Path to preferred R Markdown template
# The path to the preferred R Markdown template.
.rs.addFunction("uiPrefs.rmdPreferredTemplatePath", function()
{
   .rs.getUserPref("rmd_preferred_template_path")
})


# Display R Markdown documents in
# Where to display R Markdown documents when they have completed rendering.
.rs.addFunction("uiPrefs.rmdViewerType", function()
{
   .rs.getUserPref("rmd_viewer_type")
})


# Show diagnostic info when publishing
# Whether to show verbose diagnostic information when publishing content.
.rs.addFunction("uiPrefs.showPublishDiagnostics", function()
{
   .rs.getUserPref("show_publish_diagnostics")
})


# 
# Whether to show UI for publishing content to Posit Cloud.
.rs.addFunction("uiPrefs.enableCloudPublishUi", function()
{
   .rs.getUserPref("enable_cloud_publish_ui")
})


# Check SSL certificates when publishing
# Whether to check remote server SSL certificates when publishing content.
.rs.addFunction("uiPrefs.publishCheckCertificates", function()
{
   .rs.getUserPref("publish_check_certificates")
})


# Use custom CA bundle when publishing
# Whether to use a custom certificate authority (CA) bundle when publishing
.rs.addFunction("uiPrefs.usePublishCaBundle", function()
{
   .rs.getUserPref("use_publish_ca_bundle")
})


# Path to custom CA bundle for publishing
# The path to the custom certificate authority (CA) bundle to use when publishing
.rs.addFunction("uiPrefs.publishCaBundle", function()
{
   .rs.getUserPref("publish_ca_bundle")
})


# Show chunk output inline in all documents
# Whether to show chunk output inline for ordinary R Markdown documents.
.rs.addFunction("uiPrefs.rmdChunkOutputInline", function()
{
   .rs.getUserPref("rmd_chunk_output_inline")
})


# Open document outline by default
# Whether to show the document outline by default when opening R Markdown
.rs.addFunction("uiPrefs.showDocOutlineRmd", function()
{
   .rs.getUserPref("show_doc_outline_rmd")
})


# Document outline font size
# The font size to use for items in the document outline.
.rs.addFunction("uiPrefs.documentOutlineFontSize", function()
{
   .rs.getUserPref("document_outline_font_size")
})


# Automatically run Setup chunk when needed
# Whether to automatically run an R Markdown document's Setup chunk before
.rs.addFunction("uiPrefs.autoRunSetupChunk", function()
{
   .rs.getUserPref("auto_run_setup_chunk")
})


# Hide console when running R Markdown chunks
# Whether to hide the R console when executing inline R Markdown chunks.
.rs.addFunction("uiPrefs.hideConsoleOnChunkExecute", function()
{
   .rs.getUserPref("hide_console_on_chunk_execute")
})


# Unit of R code execution
# The unit of R code to execute when the Execute command is invoked.
.rs.addFunction("uiPrefs.executionBehavior", function()
{
   .rs.getUserPref("execution_behavior")
})


# Show the Terminal tab
# Whether to show the Terminal tab.
.rs.addFunction("uiPrefs.showTerminalTab", function()
{
   .rs.getUserPref("show_terminal_tab")
})


# Use local echo in the Terminal
# Whether to use local echo in the Terminal.
.rs.addFunction("uiPrefs.terminalLocalEcho", function()
{
   .rs.getUserPref("terminal_local_echo")
})


# Use websockets in the Terminal
# Whether to use websockets to communicate with the shell in the Terminal tab.
.rs.addFunction("uiPrefs.terminalWebsockets", function()
{
   .rs.getUserPref("terminal_websockets")
})


# Close Terminal pane after shell exit
# Whether to close the terminal pane after the shell exits.
.rs.addFunction("uiPrefs.terminalCloseBehavior", function()
{
   .rs.getUserPref("terminal_close_behavior")
})


# Save and restore system environment in Terminal tab
# Whether to track and save changes to system environment variables in the
.rs.addFunction("uiPrefs.terminalTrackEnvironment", function()
{
   .rs.getUserPref("terminal_track_environment")
})


# Ignored environment variables
# will not be saved when a Terminal instance is saved and restored.
.rs.addFunction("uiPrefs.terminalIgnoredEnvironmentVariables", function()
{
   .rs.getUserPref("terminal_ignored_environment_variables")
})


# Enable Terminal hooks
# Enabled Terminal hooks? Required for Python terminal integration, which places
.rs.addFunction("uiPrefs.terminalHooks", function()
{
   .rs.getUserPref("terminal_hooks")
})


# Terminal bell style
# Terminal bell style
.rs.addFunction("uiPrefs.terminalBellStyle", function()
{
   .rs.getUserPref("terminal_bell_style")
})


# Terminal tab rendering engine
# Terminal rendering engine: canvas is faster, dom may be needed for some
.rs.addFunction("uiPrefs.terminalRenderer", function()
{
   .rs.getUserPref("terminal_renderer")
})


# Make links in Terminal clickable
# Whether web links displayed in the Terminal tab are made clickable.
.rs.addFunction("uiPrefs.terminalWeblinks", function()
{
   .rs.getUserPref("terminal_weblinks")
})


# Show R Markdown render command
# Whether to print the render command use to knit R Markdown documents in the R
.rs.addFunction("uiPrefs.showRmdRenderCommand", function()
{
   .rs.getUserPref("show_rmd_render_command")
})


# Enable dragging text in code editor
# Whether to enable moving text on the editing surface by clicking and dragging
.rs.addFunction("uiPrefs.enableTextDrag", function()
{
   .rs.getUserPref("enable_text_drag")
})


# Show hidden files in Files pane
# Whether to show hidden files in the Files pane.
.rs.addFunction("uiPrefs.showHiddenFiles", function()
{
   .rs.getUserPref("show_hidden_files")
})


# Files always shown in the Files Pane
# List of file names (case sensitive) that are always shown in the Files Pane,
.rs.addFunction("uiPrefs.alwaysShownFiles", function()
{
   .rs.getUserPref("always_shown_files")
})


# Extensions always shown in the Files Pane
# List of file extensions (beginning with ., not case sensitive) that are always
.rs.addFunction("uiPrefs.alwaysShownExtensions", function()
{
   .rs.getUserPref("always_shown_extensions")
})


# Sort file names naturally in Files pane
# Whether to sort file names naturally, so that e.g., file10.R comes after
.rs.addFunction("uiPrefs.sortFileNamesNaturally", function()
{
   .rs.getUserPref("sort_file_names_naturally")
})


# Synchronize the Files pane with the current working directory
# Whether to change the directory in the Files pane automatically when the
.rs.addFunction("uiPrefs.syncFilesPaneWorkingDir", function()
{
   .rs.getUserPref("sync_files_pane_working_dir")
})


# Jobs tab visibility
# The visibility of the Jobs tab.
.rs.addFunction("uiPrefs.jobsTabVisibility", function()
{
   .rs.getUserPref("jobs_tab_visibility")
})


# 
# Whether to show the Workbench Jobs tab in RStudio Pro and RStudio Workbench.
.rs.addFunction("uiPrefs.showLauncherJobsTab", function()
{
   .rs.getUserPref("show_launcher_jobs_tab")
})


# 
# How to sort jobs in the Workbench Jobs tab in RStudio Pro and RStudio
.rs.addFunction("uiPrefs.launcherJobsSort", function()
{
   .rs.getUserPref("launcher_jobs_sort")
})


# 
# How to detect busy status in the Terminal.
.rs.addFunction("uiPrefs.busyDetection", function()
{
   .rs.getUserPref("busy_detection")
})


# 
# A list of apps that should not be considered busy in the Terminal.
.rs.addFunction("uiPrefs.busyExclusionList", function()
{
   .rs.getUserPref("busy_exclusion_list")
})


# Working directory for knitting
# The working directory to use when knitting R Markdown documents.
.rs.addFunction("uiPrefs.knitWorkingDir", function()
{
   .rs.getUserPref("knit_working_dir")
})


# Show in Document Outline
# Which objects to show in the document outline pane.
.rs.addFunction("uiPrefs.docOutlineShow", function()
{
   .rs.getUserPref("doc_outline_show")
})


# Preview LaTeX equations on idle
# When to preview LaTeX mathematical equations when cursor has not moved
.rs.addFunction("uiPrefs.latexPreviewOnCursorIdle", function()
{
   .rs.getUserPref("latex_preview_on_cursor_idle")
})


# Wrap around when going to previous/next tab
# Whether to wrap around when going to the previous or next editor tab.
.rs.addFunction("uiPrefs.wrapTabNavigation", function()
{
   .rs.getUserPref("wrap_tab_navigation")
})


# Global theme
# The theme to use for the main RStudio user interface.
.rs.addFunction("uiPrefs.globalTheme", function()
{
   .rs.getUserPref("global_theme")
})


# Ignore whitespace in VCS diffs
# Whether to ignore whitespace when generating diffs of version controlled files.
.rs.addFunction("uiPrefs.gitDiffIgnoreWhitespace", function()
{
   .rs.getUserPref("git_diff_ignore_whitespace")
})


# Sign git commits
# Whether to sign git commits.
.rs.addFunction("uiPrefs.gitSignedCommits", function()
{
   .rs.getUserPref("git_signed_commits")
})


# Double click to select in the Console
# Whether double-clicking should select a word in the Console pane.
.rs.addFunction("uiPrefs.consoleDoubleClickSelect", function()
{
   .rs.getUserPref("console_double_click_select")
})


# Warn when automatic session suspension is paused
# Whether the 'Auto Suspension Blocked' icon should appear in the R Console
.rs.addFunction("uiPrefs.consoleSuspendBlockedNotice", function()
{
   .rs.getUserPref("console_suspend_blocked_notice")
})


# Number of seconds to delay warning
# How long to wait before warning that automatic session suspension has been
.rs.addFunction("uiPrefs.consoleSuspendBlockedNoticeDelay", function()
{
   .rs.getUserPref("console_suspend_blocked_notice_delay")
})


# Create a Git repo in new projects
# Whether a git repo should be initialized inside new projects by default.
.rs.addFunction("uiPrefs.newProjGitInit", function()
{
   .rs.getUserPref("new_proj_git_init")
})


# Create an renv environment in new projects
# Whether an renv environment should be created inside new projects by default.
.rs.addFunction("uiPrefs.newProjUseRenv", function()
{
   .rs.getUserPref("new_proj_use_renv")
})


# Root document for PDF compilation
# The root document to use when compiling PDF documents.
.rs.addFunction("uiPrefs.rootDocument", function()
{
   .rs.getUserPref("root_document")
})


# Show user home page in RStudio Workbench
# When to show the server home page in RStudio Workbench.
.rs.addFunction("uiPrefs.showUserHomePage", function()
{
   .rs.getUserPref("show_user_home_page")
})


# 
# Whether to reuse sessions when opening projects in RStudio Workbench.
.rs.addFunction("uiPrefs.reuseSessionsForProjectLinks", function()
{
   .rs.getUserPref("reuse_sessions_for_project_links")
})


# Enable version control if available
# Whether to enable RStudio's version control system interface.
.rs.addFunction("uiPrefs.vcsEnabled", function()
{
   .rs.getUserPref("vcs_enabled")
})


# Auto-refresh state from version control
# Automatically refresh VCS status?
.rs.addFunction("uiPrefs.vcsAutorefresh", function()
{
   .rs.getUserPref("vcs_autorefresh")
})


# Path to Git executable
# The path to the Git executable to use.
.rs.addFunction("uiPrefs.gitExePath", function()
{
   .rs.getUserPref("git_exe_path")
})


# Path to Subversion executable
# The path to the Subversion executable to use.
.rs.addFunction("uiPrefs.svnExePath", function()
{
   .rs.getUserPref("svn_exe_path")
})


# 
# The path to the terminal executable to use.
.rs.addFunction("uiPrefs.terminalPath", function()
{
   .rs.getUserPref("terminal_path")
})


# 
# The path to the SSH key file to use.
.rs.addFunction("uiPrefs.rsaKeyPath", function()
{
   .rs.getUserPref("rsa_key_path")
})


# 
# The encryption type to use for the SSH key file.
.rs.addFunction("uiPrefs.sshKeyType", function()
{
   .rs.getUserPref("ssh_key_type")
})


# Use the devtools R package if available
# Whether to use the devtools R package.
.rs.addFunction("uiPrefs.useDevtools", function()
{
   .rs.getUserPref("use_devtools")
})


# Always use --preclean when installing package
# Always use --preclean when installing package.
.rs.addFunction("uiPrefs.cleanBeforeInstall", function()
{
   .rs.getUserPref("clean_before_install")
})


# Download R packages securely
# Whether to use secure downloads when fetching R packages.
.rs.addFunction("uiPrefs.useSecureDownload", function()
{
   .rs.getUserPref("use_secure_download")
})


# Clean up temporary files after R CMD CHECK
# Whether to clean up temporary files after running R CMD CHECK.
.rs.addFunction("uiPrefs.cleanupAfterRCmdCheck", function()
{
   .rs.getUserPref("cleanup_after_r_cmd_check")
})


# View directory after R CMD CHECK
# Whether to view the directory after running R CMD CHECK.
.rs.addFunction("uiPrefs.viewDirAfterRCmdCheck", function()
{
   .rs.getUserPref("view_dir_after_r_cmd_check")
})


# Hide object files in the Files pane
# Whether to hide object files in the Files pane.
.rs.addFunction("uiPrefs.hideObjectFiles", function()
{
   .rs.getUserPref("hide_object_files")
})


# Restore last project when starting RStudio
# Whether to restore the last project when starting RStudio.
.rs.addFunction("uiPrefs.restoreLastProject", function()
{
   .rs.getUserPref("restore_last_project")
})


# Number of seconds for safe project startup
# The number of seconds after which a project is deemed to have successfully
.rs.addFunction("uiPrefs.projectSafeStartupSeconds", function()
{
   .rs.getUserPref("project_safe_startup_seconds")
})


# Use tinytex to compile .tex files
# Use tinytex to compile .tex files.
.rs.addFunction("uiPrefs.useTinytex", function()
{
   .rs.getUserPref("use_tinytex")
})


# Clean output after running Texi2Dvi
# Whether to clean output after running Texi2Dvi.
.rs.addFunction("uiPrefs.cleanTexi2dviOutput", function()
{
   .rs.getUserPref("clean_texi2dvi_output")
})


# Shell escape LaTeX documents
# Whether to enable shell escaping with LaTeX documents.
.rs.addFunction("uiPrefs.latexShellEscape", function()
{
   .rs.getUserPref("latex_shell_escape")
})


# Restore project R version in RStudio Pro and RStudio Workbench
# Whether to restore the last version of R used by the project in RStudio Pro and
.rs.addFunction("uiPrefs.restoreProjectRVersion", function()
{
   .rs.getUserPref("restore_project_r_version")
})


# Clang verbosity level (0 - 2)
# The verbosity level to use with Clang (0 - 2)
.rs.addFunction("uiPrefs.clangVerbose", function()
{
   .rs.getUserPref("clang_verbose")
})


# Submit crash reports to RStudio
# Whether to automatically submit crash reports to RStudio.
.rs.addFunction("uiPrefs.submitCrashReports", function()
{
   .rs.getUserPref("submit_crash_reports")
})


# 
# The R version to use by default.
.rs.addFunction("uiPrefs.defaultRVersion", function()
{
   .rs.getUserPref("default_r_version")
})


# Maximum number of columns in data viewer
# The maximum number of columns to show at once in the data viewer.
.rs.addFunction("uiPrefs.dataViewerMaxColumns", function()
{
   .rs.getUserPref("data_viewer_max_columns")
})


# Maximum number of character in data viewer cells
# The maximum number of characters to show in a data viewer cell.
.rs.addFunction("uiPrefs.dataViewerMaxCellSize", function()
{
   .rs.getUserPref("data_viewer_max_cell_size")
})


# Enable support for screen readers
# Support accessibility aids such as screen readers.
.rs.addFunction("uiPrefs.enableScreenReader", function()
{
   .rs.getUserPref("enable_screen_reader")
})


# Seconds to wait before updating ARIA live region
# Number of milliseconds to wait after last keystroke before updating live
.rs.addFunction("uiPrefs.typingStatusDelayMs", function()
{
   .rs.getUserPref("typing_status_delay_ms")
})


# Reduced animation/motion mode
# Reduce use of animations in the user interface.
.rs.addFunction("uiPrefs.reducedMotion", function()
{
   .rs.getUserPref("reduced_motion")
})


# Tab key always moves focus
# Tab key moves focus out of text editing controls instead of inserting tabs.
.rs.addFunction("uiPrefs.tabKeyMoveFocus", function()
{
   .rs.getUserPref("tab_key_move_focus")
})


# Tab key moves focus directly from find text to replace text in find panel
# In source editor find panel, tab key moves focus directly from find text to
.rs.addFunction("uiPrefs.findPanelLegacyTabSequence", function()
{
   .rs.getUserPref("find_panel_legacy_tab_sequence")
})


# Show focus outline around focused panel
# Show which panel contains keyboard focus.
.rs.addFunction("uiPrefs.showPanelFocusRectangle", function()
{
   .rs.getUserPref("show_panel_focus_rectangle")
})


# Autosave mode on idle
# How to deal with changes to documents on idle.
.rs.addFunction("uiPrefs.autoSaveOnIdle", function()
{
   .rs.getUserPref("auto_save_on_idle")
})


# Idle period for document autosave (ms)
# The idle period, in milliseconds, after which documents should be auto-saved.
.rs.addFunction("uiPrefs.autoSaveIdleMs", function()
{
   .rs.getUserPref("auto_save_idle_ms")
})


# Save documents when editor loses input focus
# Whether to automatically save when the editor loses focus.
.rs.addFunction("uiPrefs.autoSaveOnBlur", function()
{
   .rs.getUserPref("auto_save_on_blur")
})


# Initial working directory for new terminals
# Initial directory for new terminals.
.rs.addFunction("uiPrefs.terminalInitialDirectory", function()
{
   .rs.getUserPref("terminal_initial_directory")
})


# Show full path to project in RStudio Desktop windows
# Whether to show the full path to project in desktop window title.
.rs.addFunction("uiPrefs.fullProjectPathInWindowTitle", function()
{
   .rs.getUserPref("full_project_path_in_window_title")
})


# Use visual editing by default for new markdown documents
# Whether to enable visual editing by default for new markdown documents
.rs.addFunction("uiPrefs.visualMarkdownEditingIsDefault", function()
{
   .rs.getUserPref("visual_markdown_editing_is_default")
})


# Default list spacing in visual markdown editing mode
# Default spacing for lists created in the visual editor
.rs.addFunction("uiPrefs.visualMarkdownEditingListSpacing", function()
{
   .rs.getUserPref("visual_markdown_editing_list_spacing")
})


# Wrap text in visual markdown editing mode
# Whether to automatically wrap text when writing markdown
.rs.addFunction("uiPrefs.visualMarkdownEditingWrap", function()
{
   .rs.getUserPref("visual_markdown_editing_wrap")
})


# Wrap column for visual markdown editing mode
# The column to wrap text at when writing markdown
.rs.addFunction("uiPrefs.visualMarkdownEditingWrapAtColumn", function()
{
   .rs.getUserPref("visual_markdown_editing_wrap_at_column")
})


# Place visual markdown footnotes in
# Placement of footnotes within markdown output.
.rs.addFunction("uiPrefs.visualMarkdownEditingReferencesLocation", function()
{
   .rs.getUserPref("visual_markdown_editing_references_location")
})


# Write canonical visual mode markdown in source mode
# Whether to write canonical visual mode markdown when saving from source mode.
.rs.addFunction("uiPrefs.visualMarkdownEditingCanonical", function()
{
   .rs.getUserPref("visual_markdown_editing_canonical")
})


# Max content width for visual markdown editor (px)
# Maximum content width for visual editing mode, in pixels
.rs.addFunction("uiPrefs.visualMarkdownEditingMaxContentWidth", function()
{
   .rs.getUserPref("visual_markdown_editing_max_content_width")
})


# Show document outline in visual markdown editing mode
# Whether to show the document outline by default when opening R Markdown
.rs.addFunction("uiPrefs.visualMarkdownEditingShowDocOutline", function()
{
   .rs.getUserPref("visual_markdown_editing_show_doc_outline")
})


# Show margin in visual mode code blocks
# Whether to show the margin guide in the visual mode code blocks.
.rs.addFunction("uiPrefs.visualMarkdownEditingShowMargin", function()
{
   .rs.getUserPref("visual_markdown_editing_show_margin")
})


# Show line numbers in visual mode code blocks
# Whether to show line numbers in the code editors used in visual mode
.rs.addFunction("uiPrefs.visualMarkdownCodeEditorLineNumbers", function()
{
   .rs.getUserPref("visual_markdown_code_editor_line_numbers")
})


# Font size for visual editing mode
# The default visual editing mode font size, in points
.rs.addFunction("uiPrefs.visualMarkdownEditingFontSizePoints", function()
{
   .rs.getUserPref("visual_markdown_editing_font_size_points")
})


# Editor for code chunks in visual editing mode
# The name of the editor to use to provide code editing in visual mode
.rs.addFunction("uiPrefs.visualMarkdownCodeEditor", function()
{
   .rs.getUserPref("visual_markdown_code_editor")
})


# Zotero libraries
# Zotero libraries to insert citations from.
.rs.addFunction("uiPrefs.zoteroLibraries", function()
{
   .rs.getUserPref("zotero_libraries")
})


# 
# Preferred emoji skintone
.rs.addFunction("uiPrefs.emojiSkintone", function()
{
   .rs.getUserPref("emoji_skintone")
})


# Disabled aria-live announcements
# List of aria-live announcements to disable.
.rs.addFunction("uiPrefs.disabledAriaLiveAnnouncements", function()
{
   .rs.getUserPref("disabled_aria_live_announcements")
})


# Maximum number of console lines to announce
# Maximum number of lines of console output announced after a command.
.rs.addFunction("uiPrefs.screenreaderConsoleAnnounceLimit", function()
{
   .rs.getUserPref("screenreader_console_announce_limit")
})


# List of path components ignored by file monitor
# List of path components; file monitor will ignore paths containing one or more
.rs.addFunction("uiPrefs.fileMonitorIgnoredComponents", function()
{
   .rs.getUserPref("file_monitor_ignored_components")
})


# Install R package dependencies one at a time
# Whether to install R package dependencies one at a time.
.rs.addFunction("uiPrefs.installPkgDepsIndividually", function()
{
   .rs.getUserPref("install_pkg_deps_individually")
})


# R graphics backend
# R graphics backend.
.rs.addFunction("uiPrefs.graphicsBackend", function()
{
   .rs.getUserPref("graphics_backend")
})


# R graphics antialiasing method
# Type of anti-aliasing to be used for generated R plots.
.rs.addFunction("uiPrefs.graphicsAntialiasing", function()
{
   .rs.getUserPref("graphics_antialiasing")
})


# Fixed-width font list for RStudio Server
# List of fixed-width fonts to check for browser support.
.rs.addFunction("uiPrefs.browserFixedWidthFonts", function()
{
   .rs.getUserPref("browser_fixed_width_fonts")
})


# 
# The Python type.
.rs.addFunction("uiPrefs.pythonType", function()
{
   .rs.getUserPref("python_type")
})


# 
# The Python version.
.rs.addFunction("uiPrefs.pythonVersion", function()
{
   .rs.getUserPref("python_version")
})


# 
# The path to the default Python interpreter.
.rs.addFunction("uiPrefs.pythonPath", function()
{
   .rs.getUserPref("python_path")
})


# Save Retry Timeout
# The maximum amount of seconds of retry for save operations.
.rs.addFunction("uiPrefs.saveRetryTimeout", function()
{
   .rs.getUserPref("save_retry_timeout")
})


# Use R's native pipe operator, |>
# Whether the Insert Pipe Operator command should use the native R pipe operator,
.rs.addFunction("uiPrefs.insertNativePipeOperator", function()
{
   .rs.getUserPref("insert_native_pipe_operator")
})


# Remember recently used items in Command Palette
# Whether to keep track of recently used commands in the Command Palette
.rs.addFunction("uiPrefs.commandPaletteMru", function()
{
   .rs.getUserPref("command_palette_mru")
})


# Show memory usage in Environment Pane
# Whether to compute and show memory usage in the Environment Pane
.rs.addFunction("uiPrefs.showMemoryUsage", function()
{
   .rs.getUserPref("show_memory_usage")
})


# Interval for requerying memory stats (seconds)
# How many seconds to wait between automatic requeries of memory statistics (0 to
.rs.addFunction("uiPrefs.memoryQueryIntervalSeconds", function()
{
   .rs.getUserPref("memory_query_interval_seconds")
})


# Enable terminal Python integration
# Enable Python terminal hooks. When enabled, the RStudio-configured version of
.rs.addFunction("uiPrefs.terminalPythonIntegration", function()
{
   .rs.getUserPref("terminal_python_integration")
})


# Session protocol debug logging
# Enable session protocol debug logging showing all session requests and events
.rs.addFunction("uiPrefs.sessionProtocolDebug", function()
{
   .rs.getUserPref("session_protocol_debug")
})


# Automatically activate project Python environments
# When enabled, if the active project contains a Python virtual environment, then
.rs.addFunction("uiPrefs.pythonProjectEnvironmentAutomaticActivate", function()
{
   .rs.getUserPref("python_project_environment_automatic_activate")
})


# Check values in the Environment pane for null external pointers
# further.
.rs.addFunction("uiPrefs.checkNullExternalPointers", function()
{
   .rs.getUserPref("check_null_external_pointers")
})


# User Interface Language:
# The IDE's user-interface language.
.rs.addFunction("uiPrefs.uiLanguage", function()
{
   .rs.getUserPref("ui_language")
})


# Auto hide menu bar
# Hide desktop menu bar until Alt key is pressed.
.rs.addFunction("uiPrefs.autohideMenubar", function()
{
   .rs.getUserPref("autohide_menubar")
})


# Use native file and message dialog boxes
# Whether RStudio Desktop will use the operating system's native File and Message
.rs.addFunction("uiPrefs.nativeFileDialogs", function()
{
   .rs.getUserPref("native_file_dialogs")
})


# Discard pending console input on error
# When enabled, any pending console input will be discarded when an (uncaught) R
.rs.addFunction("uiPrefs.discardPendingConsoleInputOnError", function()
{
   .rs.getUserPref("discard_pending_console_input_on_error")
})


# Editor scroll speed sensitivity
# An integer value, 1-200, to set the editor scroll multiplier. The higher the
.rs.addFunction("uiPrefs.editorScrollMultiplier", function()
{
   .rs.getUserPref("editor_scroll_multiplier")
})


# Text rendering
# Control how text is rendered within the IDE surface.
.rs.addFunction("uiPrefs.textRendering", function()
{
   .rs.getUserPref("text_rendering")
})


# Disable Electron accessibility support
# Disable Electron accessibility support.
.rs.addFunction("uiPrefs.disableRendererAccessibility", function()
{
   .rs.getUserPref("disable_renderer_accessibility")
})


# Enable GitHub Copilot
# When enabled, RStudio will use GitHub Copilot to provide code suggestions.
.rs.addFunction("uiPrefs.copilotEnabled", function()
{
   .rs.getUserPref("copilot_enabled")
})


# Show Copilot code suggestions:
# Control when Copilot code suggestions are displayed in the editor.
.rs.addFunction("uiPrefs.copilotCompletionsTrigger", function()
{
   .rs.getUserPref("copilot_completions_trigger")
})


# GitHub Copilot completions delay
# The delay (in milliseconds) before GitHub Copilot completions are requested
.rs.addFunction("uiPrefs.copilotCompletionsDelay", function()
{
   .rs.getUserPref("copilot_completions_delay")
})


# Pressing Tab key will prefer inserting:
# Control the behavior of the Tab key when both Copilot code suggestions and
.rs.addFunction("uiPrefs.copilotTabKeyBehavior", function()
{
   .rs.getUserPref("copilot_tab_key_behavior")
})


# Index project files with GitHub Copilot
# When enabled, RStudio will index project files with GitHub Copilot.
.rs.addFunction("uiPrefs.copilotIndexingEnabled", function()
{
   .rs.getUserPref("copilot_indexing_enabled")
})


# 
# User-provided name for the currently opened R project.
.rs.addFunction("uiPrefs.projectName", function()
{
   .rs.getUserPref("project_name")
})


# Default working directory for background jobs
# Default working directory in background job dialog.
.rs.addFunction("uiPrefs.runBackgroundJobDefaultWorkingDir", function()
{
   .rs.getUserPref("run_background_job_default_working_dir")
})


# Code formatter
# The formatter to use when reformatting code.
.rs.addFunction("uiPrefs.codeFormatter", function()
{
   .rs.getUserPref("code_formatter")
})


# Use strict transformers when formatting code
# When set, strict transformers will be used when formatting code. See the
.rs.addFunction("uiPrefs.codeFormatterStylerStrict", function()
{
   .rs.getUserPref("code_formatter_styler_strict")
})


# 
# The external command to be used when reformatting code.
.rs.addFunction("uiPrefs.codeFormatterExternalCommand", function()
{
   .rs.getUserPref("code_formatter_external_command")
})


# Reformat documents on save
# When set, the selected formatter will be used to reformat documents on save.
.rs.addFunction("uiPrefs.reformatOnSave", function()
{
   .rs.getUserPref("reformat_on_save")
})


# Default project user data directory
# The folder in which RStudio should store project .Rproj.user data.
.rs.addFunction("uiPrefs.projectUserDataDirectory", function()
{
   .rs.getUserPref("project_user_data_directory")
})


