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

    @Key("unknownLabel")
    String unknownLabel();

    @Key("profilingCodeTitle")
    String profilingCodeTitle();

    @Key("consoleLabel")
    String consoleLabel();

    @Key("consoleTabLabel")
    String consoleTabLabel();

    @Key("consoleTabSecondLabel")
    String consoleTabSecondLabel();

    @Key("interruptRTitle")
    String interruptRTitle();

    @Key("interruptPythonTitle")
    String interruptPythonTitle();

    @Key("consoleTabDebugLabel")
    String consoleTabDebugLabel();

    @Key("consoleTabProfilerLabel")
    String consoleTabProfilerLabel();

    @Key("consoleJobProgress")
    String consoleJobProgress();

    @Key("consoleClearedMessage")
    String consoleClearedMessage();

    @Key("focusConsoleWarningMessage")
    String focusConsoleWarningMessage(String title);

    @Key("errorString")
    String errorString(String userMessage);

    @Key("executingPythonCodeProgressCaption")
    String executingPythonCodeProgressCaption();

    @Key("consoleOutputLabel")
    String consoleOutputLabel();

    @Key("noMatchesLabel")
    String noMatchesLabel();

    @Key("notAllItemsShownText")
    String notAllItemsShownText();

    @Key("f1prompt")
    String f1prompt();

    @Key("errorRetrievingHelp")
    String errorRetrievingHelp();

    @Key("noMatchingCommandsText")
    String noMatchingCommandsText();

    @Key("openingHelpProgressMessage")
    String openingHelpProgressMessage();

    @Key("findingDefinitionProgressMessage")
    String findingDefinitionProgressMessage();

    @Key("helpCaption")
    String helpCaption();

    @Key("searchingForDefinitionMessage")
    String searchingForDefinitionMessage();

    @Key("errorSearchingForFunctionMessage")
    String errorSearchingForFunctionMessage();

    @Key("positionText")
    String positionText(int position);

    @Key("startText")
    String startText();

    @Key("nullText")
    String nullText();

    @Key("endText")
    String endText();

    @Key("sessionSuspendedTitle")
    String sessionSuspendedTitle();
}
