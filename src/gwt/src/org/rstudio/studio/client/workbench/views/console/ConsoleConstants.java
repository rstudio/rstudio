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

    @DefaultMessage("(unknown)")
    @Key("unknownLabel")
    String unknownLabel();

    @DefaultMessage("Profiling Code")
    @Key("profilingCodeTitle")
    String profilingCodeTitle();

    @DefaultMessage("Console")
    @Key("consoleLabel")
    String consoleLabel();

    @DefaultMessage("Console Tab")
    @Key("consoleTabLabel")
    String consoleTabLabel();

    @DefaultMessage("Console Tab Second")
    @Key("consoleTabSecondLabel")
    String consoleTabSecondLabel();

    @DefaultMessage("Interrupt R")
    @Key("interruptRTitle")
    String interruptRTitle();

    @DefaultMessage("Interrupt Python")
    @Key("interruptPythonTitle")
    String interruptPythonTitle();

    @DefaultMessage("Console Tab Debug")
    @Key("consoleTabDebugLabel")
    String consoleTabDebugLabel();

    @DefaultMessage("Console Tab Profiler")
    @Key("consoleTabProfilerLabel")
    String consoleTabProfilerLabel();

    @DefaultMessage("Console Tab Job Progress")
    @Key("consoleJobProgress")
    String consoleJobProgress();

    @DefaultMessage("Console cleared")
    @Key("consoleClearedMessage")
    String consoleClearedMessage();

    @DefaultMessage("Warning: Focus console output command unavailable when {0} option is enabled.")
    @Key("focusConsoleWarningMessage")
    String focusConsoleWarningMessage(String title);

    @DefaultMessage("Error: {0}\\n")
    @Key("errorString")
    String errorString(String userMessage);

    @DefaultMessage("Executing Python code")
    @Key("executingPythonCodeProgressCaption")
    String executingPythonCodeProgressCaption();

    @DefaultMessage("Console Output")
    @Key("consoleOutputLabel")
    String consoleOutputLabel();

    @DefaultMessage("(No matches)")
    @Key("noMatchesLabel")
    String noMatchesLabel();

    @DefaultMessage("... Not all items shown")
    @Key("notAllItemsShownText")
    String notAllItemsShownText();

    @DefaultMessage("Press F1 for additional help")
    @Key("f1prompt")
    String f1prompt();

    @DefaultMessage("Error Retrieving Help")
    @Key("errorRetrievingHelp")
    String errorRetrievingHelp();

    @DefaultMessage("(No matching commands)")
    @Key("noMatchingCommandsText")
    String noMatchingCommandsText();

    @DefaultMessage("Opening help...")
    @Key("openingHelpProgressMessage")
    String openingHelpProgressMessage();

    @DefaultMessage("Finding definition...")
    @Key("findingDefinitionProgressMessage")
    String findingDefinitionProgressMessage();

    @DefaultMessage("Help")
    @Key("helpCaption")
    String helpCaption();

    @DefaultMessage("Searching for definition...")
    @Key("searchingForDefinitionMessage")
    String searchingForDefinitionMessage();

    @DefaultMessage("Error Searching for Function")
    @Key("errorSearchingForFunctionMessage")
    String errorSearchingForFunctionMessage();

    @DefaultMessage("Position {0}")
    @Key("positionText")
    String positionText(int position);

    @DefaultMessage("Start: ")
    @Key("startText")
    String startText();

    @DefaultMessage("null")
    @Key("nullText")
    String nullText();

    @DefaultMessage("End: ")
    @Key("endText")
    String endText();

    @DefaultMessage("Session Suspended")
    @Key("sessionSuspendedTitle")
    String sessionSuspendedTitle();
}
