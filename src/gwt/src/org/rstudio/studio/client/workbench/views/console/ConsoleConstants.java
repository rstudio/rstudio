/*
 * ConsoleConstants.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
    String unknownLabel();

    /**
     * Translated "Profiling Code".
     *
     * @return translated "Profiling Code"
     */
    @DefaultMessage("Profiling Code")
    String profilingCodeTitle();

    /**
     * Translated "Console".
     *
     * @return translated "Console"
     */
    @DefaultMessage("Console")
    String consoleLabel();

    /**
     * Translated "Console Tab".
     *
     * @return translated "Console Tab"
     */
    @DefaultMessage("Console Tab")
    String consoleTabLabel();

    /**
     * Translated "Console Tab Second".
     *
     * @return translated "Console Tab Second"
     */
    @DefaultMessage("Console Tab Second")
    String consoleTabSecondLabel();

    /**
     * Translated "Interrupt R".
     *
     * @return translated "Interrupt R"
     */
    @DefaultMessage("Interrupt R")
    String interruptRTitle();

    /**
     * Translated "Interrupt Python".
     *
     * @return translated "Interrupt Python"
     */
    @DefaultMessage("Interrupt Python")
    String interruptPythonTitle();

    /**
     * Translated "Console Tab Debug".
     *
     * @return translated "Console Tab Debug"
     */
    @DefaultMessage("Console Tab Debug")
    String consoleTabDebugLabel();

    /**
     * Translated "Console Tab Profiler".
     *
     * @return translated "Console Tab Profiler"
     */
    @DefaultMessage("Console Tab Profiler")
    String consoleTabProfilerLabel();

    /**
     * Translated "Console Tab Job Progress".
     *
     * @return translated "Console Tab Job Progress"
     */
    @DefaultMessage("Console Tab Job Progress")
    String consoleJobProgress();

    /**
     * Translated "Console cleared".
     *
     * @return translated "Console cleared"
     */
    @DefaultMessage("Console cleared")
    String consoleClearedMessage();

    /**
     * Translated "Warning: Focus console output command unavailable when {0} option is enabled.".
     *
     * @return translated "Warning: Focus console output command unavailable when {0} option is enabled."
     */
    @DefaultMessage("Warning: Focus console output command unavailable when {0} option is enabled.")
    String focusConsoleWarningMessage(String title);

    /**
     * Translated "Error: {0}\n".
     *
     * @return translated "Error: {0}\n"
     */
    @DefaultMessage("Error: {0}\\n")
    String errorString(String userMessage);

    /**
     * Translated "Executing Python code".
     *
     * @return translated "Executing Python code"
     */
    @DefaultMessage("Executing Python code")
    String executingPythonCodeProgressCaption();

    /**
     * Translated "Console Output".
     *
     * @return translated "Console Output"
     */
    @DefaultMessage("Console Output")
    String consoleOutputLabel();

    /**
     * Translated "(No matches)".
     *
     * @return translated "(No matches)"
     */
    @DefaultMessage("(No matches)")
    String noMatchesLabel();

    /**
     * Translated "... Not all items shown".
     *
     * @return translated "... Not all items shown"
     */
    @DefaultMessage("... Not all items shown")
    String notAllItemsShownText();

    /**
     * Translated "Press F1 for additional help".
     *
     * @return translated "Press F1 for additional help"
     */
    @DefaultMessage("Press F1 for additional help")
    String f1prompt();

    /**
     * Translated "Error Retrieving Help".
     *
     * @return translated "Error Retrieving Help"
     */
    @DefaultMessage("Error Retrieving Help")
    String errorRetrievingHelp();

    /**
     * Translated "(No matching commands)".
     *
     * @return translated "(No matching commands)"
     */
    @DefaultMessage("(No matching commands)")
    String noMatchingCommandsText();

    /**
     * Translated "Opening help...".
     *
     * @return translated "Opening help..."
     */
    @DefaultMessage("Opening help...")
    String openingHelpProgressMessage();

    /**
     * Translated "Finding definition...".
     *
     * @return translated "Finding definition..."
     */
    @DefaultMessage("Finding definition...")
    String findingDefinitionProgressMessage();

    /**
     * Translated "Help".
     *
     * @return translated "Help"
     */
    @DefaultMessage("Help")
    String helpCaption();

    /**
     * Translated "Searching for definition...".
     *
     * @return translated "Searching for definition..."
     */
    @DefaultMessage("Searching for definition...")
    String searchingForDefinitionMessage();

    /**
     * Translated "Error Searching for Function".
     *
     * @return translated "Error Searching for Function"
     */
    @DefaultMessage("Error Searching for Function")
    String errorSearchingForFunctionMessage();

    /**
     * Translated "Position {0}".
     *
     * @return translated "Position {0}"
     */
    @DefaultMessage("Position {0}")
    String positionText(int position);

    /**
     * Translated "Start: ".
     *
     * @return translated "Start: "
     */
    @DefaultMessage("Start: ")
    String startText();

    /**
     * Translated "null".
     *
     * @return translated "null"
     */
    @DefaultMessage("null")
    String nullText();

    /**
     * Translated "End: ".
     *
     * @return translated "End: "
     */
    @DefaultMessage("End: ")
    String endText();

    /**
     * Translated "Session Suspended".
     *
     * @return translated "Session Suspended"
     */
    @DefaultMessage("Session Suspended")
    String sessionSuspendedTitle();
}
