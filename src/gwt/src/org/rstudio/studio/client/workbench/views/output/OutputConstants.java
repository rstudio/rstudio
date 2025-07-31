/*
 * OutputConstants.java
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
package org.rstudio.studio.client.workbench.views.output;

public interface OutputConstants extends com.google.gwt.i18n.client.Messages {

    @DefaultMessage("{0} Tab")
    @Key("toolBarTitle")
    String toolBarTitle(String title);

    @DefaultMessage("View Log")
    @Key("viewLogText")
    String viewLogText();

    @DefaultMessage("Compile PDF")
    @Key("compilePDFTaskName")
    String compilePDFTaskName();

    @DefaultMessage("View the LaTeX compilation log")
    @Key("viewLogTitle")
    String viewLogTitle();

    @DefaultMessage("Closing Compile PDF...")
    @Key("closingCompilePDFProgressMessage")
    String closingCompilePDFProgressMessage();

    @DefaultMessage("close the Compile PDF tab")
    @Key("closeCompilePDF")
    String closeCompilePDF();

    @DefaultMessage("Compiling PDF...")
    @Key("compilingPDFProgressMessage")
    String compilingPDFProgressMessage();

    @DefaultMessage("Stop Running Compiles")
    @Key("stopRunningCompilesCaption")
    String stopRunningCompilesCaption();

    @DefaultMessage("There is a PDF compilation currently running. If you {0} it will be terminated. Are you sure you want to stop the running PDF compilation?")
    @Key("stopPDFCompilationRunningMessage")
    String stopPDFCompilationRunningMessage(String operation);

    @DefaultMessage("Terminating PDF compilation...")
    @Key("terminatingPDFCompilationCaption")
    String terminatingPDFCompilationCaption();

    @DefaultMessage("Compile PDF")
    @Key("compilePDFCaption")
    String compilePDFCaption();

    @DefaultMessage("Unable to terminate PDF compilation. Please try again.")
    @Key("unableToTerminatePDFCompilationMessage")
    String unableToTerminatePDFCompilationMessage();

    @DefaultMessage("Data Output")
    @Key("dataOutputTitle")
    String dataOutputTitle();

    @DefaultMessage("Data Output Pane")
    @Key("dataOutputPaneTitle")
    String dataOutputPaneTitle();

    @DefaultMessage("Data Output Tab")
    @Key("dataOutputTabLabel")
    String dataOutputTabLabel();

    @DefaultMessage("SQL Results")
    @Key("sqlResultsTitle")
    String sqlResultsTitle();

    @DefaultMessage("Find in Files")
    @Key("findInFilesCaption")
    String findInFilesCaption();

    @DefaultMessage("Search in:")
    @Key("searchInLabel")
    String searchInLabel();

    @DefaultMessage("Find")
    @Key("findButtonCaption")
    String findButtonCaption();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("You must specify a directory to search.")
    @Key("errorMessage")
    String errorMessage();

    @DefaultMessage("Custom Filter Pattern")
    @Key("customFilterPatterValue")
    String customFilterPatterValue();

    @DefaultMessage("More than 1000 matching lines were found. Only the first 1000 lines are shown.")
    @Key("overFlowMessage")
    String overFlowMessage();

    @DefaultMessage("Find Results")
    @Key("findResultsTitle")
    String findResultsTitle();

    @DefaultMessage("Find Output Tab")
    @Key("findOutputTabLabel")
    String findOutputTabLabel();

    @DefaultMessage("Stop find in files")
    @Key("stopFindInFilesTitle")
    String stopFindInFilesTitle();

    @DefaultMessage("Find")
    @Key("findLabel")
    String findLabel();

    @DefaultMessage("Replace")
    @Key("replaceLabel")
    String replaceLabel();

    @DefaultMessage("Replace with: ")
    @Key("replaceWithLabel")
    String replaceWithLabel();

    @DefaultMessage("Stop replace")
    @Key("stopReplaceTitle")
    String stopReplaceTitle();

    @DefaultMessage("Replace All")
    @Key("replaceAllText")
    String replaceAllText();

    @DefaultMessage("Find in Files Results")
    @Key("findInFilesResultsTitle")
    String findInFilesResultsTitle();

    @DefaultMessage("(No results found)")
    @Key("noResultsFoundText")
    String noResultsFoundText();

    @DefaultMessage("Results for whole word ")
    @Key("resultsForWholeWordText")
    String resultsForWholeWordText();

    @DefaultMessage("Results for ")
    @Key("resultsForText")
    String resultsForText();

    @DefaultMessage("in ")
    @Key("inText")
    String inText();

    @DefaultMessage("Replace results for whole word ")
    @Key("replaceResultsWholeWordText")
    String replaceResultsWholeWordText();

    @DefaultMessage("Replace results for ")
    @Key("replaceResultsForText")
    String replaceResultsForText();

    @DefaultMessage("with ")
    @Key("withText")
    String withText();

    @DefaultMessage(": {0} successful, {1} failed")
    @Key("summaryLabel")
    String summaryLabel(int successCount, int errorCount);

    @DefaultMessage("Are you sure you want to cancel the replace? Changes already made will not be reverted.")
    @Key("stopReplaceMessage")
    String stopReplaceMessage();

    @DefaultMessage("Are you sure you wish to permanently replace all? This will ")
    @Key("replaceAllQuestion")
    String replaceAllQuestion();

    @DefaultMessage("remove ")
    @Key("removeText")
    String removeText();

    @DefaultMessage("replace ")
    @Key("replaceText")
    String replaceText();

    @DefaultMessage("{0} occurrences of ''{1}''")
    @Key("replaceMessage")
    String replaceMessage(int resultsCount, String query);

    @DefaultMessage("Diagnostics")
    @Key("diagnosticsLabel")
    String diagnosticsLabel();

    @DefaultMessage("Switch active marker list")
    @Key("switchActiveMarkerListTitle")
    String switchActiveMarkerListTitle();

    @DefaultMessage("(No markers)")
    @Key("noMarkersText")
    String noMarkersText();

    @DefaultMessage("Markers")
    @Key("markersTitle")
    String markersTitle();

    @DefaultMessage("Clear markers")
    @Key("clearMarkersTitle")
    String clearMarkersTitle();

    @DefaultMessage("Markers Tab")
    @Key("markersTabLabel")
    String markersTabLabel();

    @DefaultMessage("R Markdown")
    @Key("rMarkdownTitle")
    String rMarkdownTitle();

    @DefaultMessage("View the R Markdown render log")
    @Key("viewRMarkdownTitle")
    String viewRMarkdownTitle();

    @DefaultMessage("Stop R Markdown Rendering")
    @Key("stopRMarkdownRenderingCaption")
    String stopRMarkdownRenderingCaption();

    @DefaultMessage("The rendering of ''{0}'' is in progress. Do you want to terminate and close the tab?")
    @Key("stopRMarkdownRenderingMessage")
    String stopRMarkdownRenderingMessage(String targetFile);

    @DefaultMessage("Stop")
    @Key("stopLabel")
    String stopLabel();

    @DefaultMessage("Cancel")
    @Key("cancelLabel")
    String cancelLabel();

    @DefaultMessage("Knit Terminate Failed")
    @Key("knitTerminateFailedMessage")
    String knitTerminateFailedMessage();

    @DefaultMessage("Render")
    @Key("renderTitle")
    String renderTitle();

    @DefaultMessage("Deploy")
    @Key("deployTitle")
    String deployTitle();

    @DefaultMessage("Source Cpp")
    @Key("sourceCppTitle")
    String sourceCppTitle();

    @DefaultMessage("C++ Tab")
    @Key("cPlusPlusTabTitle")
    String cPlusPlusTabTitle();

    @DefaultMessage("View test results")
    @Key("viewTestResultsTitle")
    String viewTestResultsTitle();

    @DefaultMessage("Tests")
    @Key("testsTaskName")
    String testsTaskName();

    @DefaultMessage("Terminating Tests...")
    @Key("terminatingTestsProgressMessage")
    String terminatingTestsProgressMessage();

    @DefaultMessage("Error Terminating Tests")
    @Key("errorTerminatingTestsCaption")
    String errorTerminatingTestsCaption();

    @DefaultMessage("Unable to terminate tests. Please try again.")
    @Key("errorTerminatingTestsMessage")
    String errorTerminatingTestsMessage();

    @DefaultMessage("and cannot be undone.")
    @Key("cannotBeUndoneText")
    String cannotBeUndoneText();

    @DefaultMessage("with ''{0}'' and cannot be undone.")
    @Key("replaceCannotBeUndoneText")
    String replaceCannotBeUndoneText(String replaceText);

}
