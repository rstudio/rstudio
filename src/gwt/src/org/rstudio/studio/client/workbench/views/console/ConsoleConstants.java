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
    String unknownLabel();
    String profilingCodeTitle();
    String consoleLabel();
    String consoleTabLabel();
    String consoleTabSecondLabel();
    String interruptRTitle();
    String interruptPythonTitle();
    String consoleTabDebugLabel();
    String consoleTabProfilerLabel();
    String consoleJobProgress();
    String consoleClearedMessage();
    String focusConsoleWarningMessage(String title);
    String errorString(String userMessage);
    String executingPythonCodeProgressCaption();
    String consoleOutputLabel();
    String noMatchesLabel();
    String notAllItemsShownText();
    String f1prompt();
    String errorRetrievingHelp();
    String noMatchingCommandsText();
    String openingHelpProgressMessage();
    String findingDefinitionProgressMessage();
    String helpCaption();
    String searchingForDefinitionMessage();
    String errorSearchingForFunctionMessage();
    String positionText(int position);
    String startText();
    String nullText();
    String endText();
    String sessionSuspendedTitle();
}
