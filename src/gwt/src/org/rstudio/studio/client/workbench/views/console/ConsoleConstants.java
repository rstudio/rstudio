/*
 * ConsoleConstants.java
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console;

public interface ConsoleConstants extends com.google.gwt.i18n.client.Messages {

    /**
     *
     * Translated "(unknown)".
     *
     * @return translated "(unknown)"
     */
    @DefaultMessage("(unknown)")
    @Key("unknownLabel")
    String unknownLabel();

    /**
     * Translated "Profiling Code".
     *
     * @return translated "Profiling Code"
     */
    @DefaultMessage("Profiling Code")
    @Key("profilingCodeTitle")
    String profilingCodeTitle();

    /**
     * Translated "Console".
     *
     * @return translated "Console"
     */
    @DefaultMessage("Console")
    @Key("consoleLabel")
    String consoleLabel();

    /**
     * Translated "Console Tab".
     *
     * @return translated "Console Tab"
     */
    @DefaultMessage("Console Tab")
    @Key("consoleTabLabel")
    String consoleTabLabel();

    /**
     * Translated "Console Tab Second".
     *
     * @return translated "Console Tab Second"
     */
    @DefaultMessage("Console Tab Second")
    @Key("consoleTabSecondLabel")
    String consoleTabSecondLabel();

    /**
     * Translated "Interrupt R".
     *
     * @return translated "Interrupt R"
     */
    @DefaultMessage("Interrupt R")
    @Key("interruptRTitle")
    String interruptRTitle();

    /**
     * Translated "Interrupt Python".
     *
     * @return translated "Interrupt Python"
     */
    @DefaultMessage("Interrupt Python")
    @Key("interruptPythonTitle")
    String interruptPythonTitle();

    /**
     * Translated "Console Tab Debug".
     *
     * @return translated "Console Tab Debug"
     */
    @DefaultMessage("Console Tab Debug")
    @Key("consoleTabDebugLabel")
    String consoleTabDebugLabel();

    /**
     * Translated "Console Tab Profiler".
     *
     * @return translated "Console Tab Profiler"
     */
    @DefaultMessage("Console Tab Profiler")
    @Key("consoleTabProfilerLabel")
    String consoleTabProfilerLabel();

    /**
     * Translated "Console Tab Job Progress".
     *
     * @return translated "Console Tab Job Progress"
     */
    @DefaultMessage("Console Tab Job Progress")
    @Key("consoleJobProgress")
    String consoleJobProgress();

    /**
     * Translated "Console cleared".
     *
     * @return translated "Console cleared"
     */
    @DefaultMessage("Console cleared")
    @Key("consoleClearedMessage")
    String consoleClearedMessage();

    /**
     * Translated "Warning: Focus console output command unavailable when {0} option is enabled.".
     *
     * @return translated "Warning: Focus console output command unavailable when {0} option is enabled."
     */
    @DefaultMessage("Warning: Focus console output command unavailable when {0} option is enabled.")
    @Key("focusConsoleWarningMessage")
    String focusConsoleWarningMessage(String title);

    /**
     * Translated "Error: {0}\n".
     *
     * @return translated "Error: {0}\n"
     */
    @DefaultMessage("Error: {0}\\n")
    @Key("errorString")
    String errorString(String userMessage);

    /**
     * Translated "Executing Python code".
     *
     * @return translated "Executing Python code"
     */
    @DefaultMessage("Executing Python code")
    @Key("executingPythonCodeProgressCaption")
    String executingPythonCodeProgressCaption();

    /**
     * Translated "Console Output".
     *
     * @return translated "Console Output"
     */
    @DefaultMessage("Console Output")
    @Key("consoleOutputLabel")
    String consoleOutputLabel();

    /**
     * Translated "(No matches)".
     *
     * @return translated "(No matches)"
     */
    @DefaultMessage("(No matches)")
    @Key("noMatchesLabel")
    String noMatchesLabel();

    /**
     * Translated "... Not all items shown".
     *
     * @return translated "... Not all items shown"
     */
    @DefaultMessage("... Not all items shown")
    @Key("notAllItemsShownText")
    String notAllItemsShownText();

    /**
     * Translated "Press F1 for additional help".
     *
     * @return translated "Press F1 for additional help"
     */
    @DefaultMessage("Press F1 for additional help")
    @Key("f1prompt")
    String f1prompt();

    /**
     * Translated "Error Retrieving Help".
     *
     * @return translated "Error Retrieving Help"
     */
    @DefaultMessage("Error Retrieving Help")
    @Key("errorRetrievingHelp")
    String errorRetrievingHelp();

    /**
     * Translated "(No matching commands)".
     *
     * @return translated "(No matching commands)"
     */
    @DefaultMessage("(No matching commands)")
    @Key("noMatchingCommandsText")
    String noMatchingCommandsText();

    /**
     * Translated "Opening help...".
     *
     * @return translated "Opening help..."
     */
    @DefaultMessage("Opening help...")
    @Key("openingHelpProgressMessage")
    String openingHelpProgressMessage();

    /**
     * Translated "Finding definition...".
     *
     * @return translated "Finding definition..."
     */
    @DefaultMessage("Finding definition...")
    @Key("findingDefinitionProgressMessage")
    String findingDefinitionProgressMessage();

    /**
     * Translated "Help".
     *
     * @return translated "Help"
     */
    @DefaultMessage("Help")
    @Key("helpCaption")
    String helpCaption();

    /**
     * Translated "Searching for definition...".
     *
     * @return translated "Searching for definition..."
     */
    @DefaultMessage("Searching for definition...")
    @Key("searchingForDefinitionMessage")
    String searchingForDefinitionMessage();

    /**
     * Translated "Error Searching for Function".
     *
     * @return translated "Error Searching for Function"
     */
    @DefaultMessage("Error Searching for Function")
    @Key("errorSearchingForFunctionMessage")
    String errorSearchingForFunctionMessage();

    /**
     * Translated "Position {0}".
     *
     * @return translated "Position {0}"
     */
    @DefaultMessage("Position {0}")
    @Key("positionText")
    String positionText(int position);

    /**
     * Translated "Start: ".
     *
     * @return translated "Start: "
     */
    @DefaultMessage("Start: ")
    @Key("startText")
    String startText();

    /**
     * Translated "null".
     *
     * @return translated "null"
     */
    @DefaultMessage("null")
    @Key("nullText")
    String nullText();

    /**
     * Translated "End: ".
     *
     * @return translated "End: "
     */
    @DefaultMessage("End: ")
    @Key("endText")
    String endText();

    /**
     * Translated "Session Suspended".
     *
     * @return translated "Session Suspended"
     */
    @DefaultMessage("Session Suspended")
    @Key("sessionSuspendedTitle")
    String sessionSuspendedTitle();

    /**
     * Translated "Run the code in the console"
     * 
     * @return translated "Run the code in the console"
     */
    @DefaultMessage("Run the code in the console")
    @Key("runCodeInConsole")
    String runCodeInConsole();
}
