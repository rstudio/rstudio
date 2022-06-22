/*
 * OutputConstants.java
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
package org.rstudio.studio.client.workbench.views.output;

public interface OutputConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "{0} Tab".
     *
     * @return translated "{0} Tab"
     */
    @DefaultMessage("{0} Tab")
    @Key("toolBarTitle")
    String toolBarTitle(String title);

    /**
     * Translated "View Log".
     *
     * @return translated "View Log"
     */
    @DefaultMessage("View Log")
    @Key("viewLogText")
    String viewLogText();

    /**
     * Translated "Compile PDF".
     *
     * @return translated "Compile PDF"
     */
    @DefaultMessage("Compile PDF")
    @Key("compilePDFTaskName")
    String compilePDFTaskName();

    /**
     * Translated "View the LaTeX compilation log".
     *
     * @return translated "View the LaTeX compilation log"
     */
    @DefaultMessage("View the LaTeX compilation log")
    @Key("viewLogTitle")
    String viewLogTitle();

    /**
     * Translated "Closing Compile PDF...".
     *
     * @return translated "Closing Compile PDF..."
     */
    @DefaultMessage("Closing Compile PDF...")
    @Key("closingCompilePDFProgressMessage")
    String closingCompilePDFProgressMessage();

    /**
     * Translated "close the Compile PDF tab".
     *
     * @return translated "close the Compile PDF tab"
     */
    @DefaultMessage("close the Compile PDF tab")
    @Key("closeCompilePDF")
    String closeCompilePDF();

    /**
     * Translated "Compiling PDF...".
     *
     * @return translated "Compiling PDF..."
     */
    @DefaultMessage("Compiling PDF...")
    @Key("compilingPDFProgressMessage")
    String compilingPDFProgressMessage();

    /**
     * Translated "Stop Running Compiles".
     *
     * @return translated "Stop Running Compiles"
     */
    @DefaultMessage("Stop Running Compiles")
    @Key("stopRunningCompilesCaption")
    String stopRunningCompilesCaption();

    /**
     * Translated "There is a PDF compilation currently running. If you {0} it will be terminated. Are you sure you want to stop the running PDF compilation?".
     *
     * @return translated "There is a PDF compilation currently running. If you {0} it will be terminated. Are you sure you want to stop the running PDF compilation?"
     */
    @DefaultMessage("There is a PDF compilation currently running. If you {0} it will be terminated. Are you sure you want to stop the running PDF compilation?")
    @Key("stopPDFCompilationRunningMessage")
    String stopPDFCompilationRunningMessage(String operation);

    /**
     * Translated "Terminating PDF compilation...".
     *
     * @return translated "Terminating PDF compilation..."
     */
    @DefaultMessage("Terminating PDF compilation...")
    @Key("terminatingPDFCompilationCaption")
    String terminatingPDFCompilationCaption();

    /**
     * Translated "Compile PDF".
     *
     * @return translated "Compile PDF"
     */
    @DefaultMessage("Compile PDF")
    @Key("compilePDFCaption")
    String compilePDFCaption();

    /**
     * Translated "Unable to terminate PDF compilation. Please try again.".
     *
     * @return translated "Unable to terminate PDF compilation. Please try again."
     */
    @DefaultMessage("Unable to terminate PDF compilation. Please try again.")
    @Key("unableToTerminatePDFCompilationMessage")
    String unableToTerminatePDFCompilationMessage();

    /**
     * Translated "Data Output".
     *
     * @return translated "Data Output"
     */
    @DefaultMessage("Data Output")
    @Key("dataOutputTitle")
    String dataOutputTitle();

    /**
     * Translated "Data Output Pane".
     *
     * @return translated "Data Output Pane"
     */
    @DefaultMessage("Data Output Pane")
    @Key("dataOutputPaneTitle")
    String dataOutputPaneTitle();

    /**
     * Translated "Data Output Tab".
     *
     * @return translated "Data Output Tab"
     */
    @DefaultMessage("Data Output Tab")
    @Key("dataOutputTabLabel")
    String dataOutputTabLabel();

    /**
     * Translated "SQL Results".
     *
     * @return translated "SQL Results"
     */
    @DefaultMessage("SQL Results")
    @Key("sqlResultsTitle")
    String sqlResultsTitle();

    /**
     * Translated "Find in Files".
     *
     * @return translated "Find in Files"
     */
    @DefaultMessage("Find in Files")
    @Key("findInFilesCaption")
    String findInFilesCaption();

    /**
     * Translated "Search in:".
     *
     * @return translated "Search in:"
     */
    @DefaultMessage("Search in:")
    @Key("searchInLabel")
    String searchInLabel();

    /**
     * Translated "Find".
     *
     * @return translated "Find"
     */
    @DefaultMessage("Find")
    @Key("findButtonCaption")
    String findButtonCaption();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "You must specify a directory to search.".
     *
     * @return translated "You must specify a directory to search."
     */
    @DefaultMessage("You must specify a directory to search.")
    @Key("errorMessage")
    String errorMessage();

    /**
     * Translated "Custom Filter Pattern".
     *
     * @return translated "Custom Filter Pattern"
     */
    @DefaultMessage("Custom Filter Pattern")
    @Key("customFilterPatterValue")
    String customFilterPatterValue();

    /**
     * Translated "More than 1000 matching lines were found. Only the first 1000 lines are shown.".
     *
     * @return translated "More than 1000 matching lines were found. Only the first 1000 lines are shown."
     */
    @DefaultMessage("More than 1000 matching lines were found. Only the first 1000 lines are shown.")
    @Key("overFlowMessage")
    String overFlowMessage();

    /**
     * Translated "Find Results".
     *
     * @return translated "Find Results"
     */
    @DefaultMessage("Find Results")
    @Key("findResultsTitle")
    String findResultsTitle();

    /**
     * Translated "Find Output Tab".
     *
     * @return translated "Find Output Tab"
     */
    @DefaultMessage("Find Output Tab")
    @Key("findOutputTabLabel")
    String findOutputTabLabel();

    /**
     * Translated "Stop find in files".
     *
     * @return translated "Stop find in files"
     */
    @DefaultMessage("Stop find in files")
    @Key("stopFindInFilesTitle")
    String stopFindInFilesTitle();

    /**
     * Translated "Find".
     *
     * @return translated "Find"
     */
    @DefaultMessage("Find")
    @Key("findLabel")
    String findLabel();

    /**
     * Translated "Replace".
     *
     * @return translated "Replace"
     */
    @DefaultMessage("Replace")
    @Key("replaceLabel")
    String replaceLabel();

    /**
     * Translated "Replace with: ".
     *
     * @return translated "Replace with: "
     */
    @DefaultMessage("Replace with: ")
    @Key("replaceWithLabel")
    String replaceWithLabel();

    /**
     * Translated "Stop replace".
     *
     * @return translated "Stop replace"
     */
    @DefaultMessage("Stop replace")
    @Key("stopReplaceTitle")
    String stopReplaceTitle();

    /**
     * Translated "Replace All".
     *
     * @return translated "Replace All"
     */
    @DefaultMessage("Replace All")
    @Key("replaceAllText")
    String replaceAllText();

    /**
     * Translated "Find in Files Results".
     *
     * @return translated "Find in Files Results"
     */
    @DefaultMessage("Find in Files Results")
    @Key("findInFilesResultsTitle")
    String findInFilesResultsTitle();

    /**
     * Translated "(No results found)".
     *
     * @return translated "(No results found)"
     */
    @DefaultMessage("(No results found)")
    @Key("noResultsFoundText")
    String noResultsFoundText();

    /**
     * Translated "Results for whole word ".
     *
     * @return translated "Results for whole word "
     */
    @DefaultMessage("Results for whole word ")
    @Key("resultsForWholeWordText")
    String resultsForWholeWordText();

    /**
     * Translated "Results for ".
     *
     * @return translated "Results for "
     */
    @DefaultMessage("Results for ")
    @Key("resultsForText")
    String resultsForText();

    /**
     * Translated "in ".
     *
     * @return translated "in "
     */
    @DefaultMessage("in ")
    @Key("inText")
    String inText();

    /**
     * Translated "Replace results for whole word ".
     *
     * @return translated "Replace results for whole word "
     */
    @DefaultMessage("Replace results for whole word ")
    @Key("replaceResultsWholeWordText")
    String replaceResultsWholeWordText();

    /**
     * Translated "Replace results for ".
     *
     * @return translated "Replace results for "
     */
    @DefaultMessage("Replace results for ")
    @Key("replaceResultsForText")
    String replaceResultsForText();

    /**
     * Translated "with ".
     *
     * @return translated "with "
     */
    @DefaultMessage("with ")
    @Key("withText")
    String withText();

    /**
     * Translated ": {0} successful, {1} failed".
     *
     * @return translated ": {0} successful, {1} failed"
     */
    @DefaultMessage(": {0} successful, {1} failed")
    @Key("summaryLabel")
    String summaryLabel(int successCount, int errorCount);

    /**
     * Translated "Are you sure you want to cancel the replace? Changes already made will not be reverted.".
     *
     * @return translated "Are you sure you want to cancel the replace? Changes already made will not be reverted."
     */
    @DefaultMessage("Are you sure you want to cancel the replace? Changes already made will not be reverted.")
    @Key("stopReplaceMessage")
    String stopReplaceMessage();

    /**
     * Translated "Are you sure you wish to permanently replace all? This will ".
     *
     * @return translated "Are you sure you wish to permanently replace all? This will "
     */
    @DefaultMessage("Are you sure you wish to permanently replace all? This will ")
    @Key("replaceAllQuestion")
    String replaceAllQuestion();

    /**
     * Translated "with ".
     *
     * @return translated "remove "
     */
    @DefaultMessage("remove ")
    @Key("removeText")
    String removeText();

    /**
     * Translated "replace ".
     *
     * @return translated "replace "
     */
    @DefaultMessage("replace ")
    @Key("replaceText")
    String replaceText();

    /**
     * Translated "{0} occurrences of ''{1}''".
     *
     * @return translated "{0} occurrences of ''{1}''"
     */
    @DefaultMessage("{0} occurrences of ''{1}''")
    @Key("replaceMessage")
    String replaceMessage(int resultsCount, String query);

    /**
     * Translated "Diagnostics".
     *
     * @return translated "Diagnostics"
     */
    @DefaultMessage("Diagnostics")
    @Key("diagnosticsLabel")
    String diagnosticsLabel();

    /**
     * Translated "Switch active marker list".
     *
     * @return translated "Switch active marker list"
     */
    @DefaultMessage("Switch active marker list")
    @Key("switchActiveMarkerListTitle")
    String switchActiveMarkerListTitle();

    /**
     * Translated "(No markers)".
     *
     * @return translated "(No markers)"
     */
    @DefaultMessage("(No markers)")
    @Key("noMarkersText")
    String noMarkersText();

    /**
     * Translated "Markers".
     *
     * @return translated "Markers"
     */
    @DefaultMessage("Markers")
    @Key("markersTitle")
    String markersTitle();

    /**
     * Translated "Clear markers".
     *
     * @return translated "Clear markers"
     */
    @DefaultMessage("Clear markers")
    @Key("clearMarkersTitle")
    String clearMarkersTitle();

    /**
     * Translated "Markers Tab".
     *
     * @return translated "Markers Tab"
     */
    @DefaultMessage("Markers Tab")
    @Key("markersTabLabel")
    String markersTabLabel();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultMessage("R Markdown")
    @Key("rMarkdownTitle")
    String rMarkdownTitle();

    /**
     * Translated "View the R Markdown render log".
     *
     * @return translated "View the R Markdown render log"
     */
    @DefaultMessage("View the R Markdown render log")
    @Key("viewRMarkdownTitle")
    String viewRMarkdownTitle();

    /**
     * Translated "Stop R Markdown Rendering".
     *
     * @return translated "Stop R Markdown Rendering"
     */
    @DefaultMessage("Stop R Markdown Rendering")
    @Key("stopRMarkdownRenderingCaption")
    String stopRMarkdownRenderingCaption();

    /**
     * Translated "The rendering of ''{0}'' is in progress. Do you want to terminate and close the tab?".
     *
     * @return translated "The rendering of ''{0}'' is in progress. Do you want to terminate and close the tab?"
     */
    @DefaultMessage("The rendering of ''{0}'' is in progress. Do you want to terminate and close the tab?")
    @Key("stopRMarkdownRenderingMessage")
    String stopRMarkdownRenderingMessage(String targetFile);

    /**
     * Translated "Stop".
     *
     * @return translated "Stop"
     */
    @DefaultMessage("Stop")
    @Key("stopLabel")
    String stopLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("cancelLabel")
    String cancelLabel();

    /**
     * Translated "Knit Terminate Failed".
     *
     * @return translated "Knit Terminate Failed"
     */
    @DefaultMessage("Knit Terminate Failed")
    @Key("knitTerminateFailedMessage")
    String knitTerminateFailedMessage();

    /**
     * Translated "Render".
     *
     * @return translated "Render"
     */
    @DefaultMessage("Render")
    @Key("renderTitle")
    String renderTitle();

    /**
     * Translated "Deploy".
     *
     * @return translated "Deploy"
     */
    @DefaultMessage("Deploy")
    @Key("deployTitle")
    String deployTitle();

    /**
     * Translated "Source Cpp".
     *
     * @return translated "Source Cpp"
     */
    @DefaultMessage("Source Cpp")
    @Key("sourceCppTitle")
    String sourceCppTitle();

    /**
     * Translated "C++ Tab".
     *
     * @return translated "C++ Tab"
     */
    @DefaultMessage("C++ Tab")
    @Key("cPlusPlusTabTitle")
    String cPlusPlusTabTitle();

    /**
     * Translated "View test results".
     *
     * @return translated "View test results"
     */
    @DefaultMessage("View test results")
    @Key("viewTestResultsTitle")
    String viewTestResultsTitle();

    /**
     * Translated "Tests".
     *
     * @return translated "Tests"
     */
    @DefaultMessage("Tests")
    @Key("testsTaskName")
    String testsTaskName();

    /**
     * Translated "Terminating Tests...".
     *
     * @return translated "Terminating Tests..."
     */
    @DefaultMessage("Terminating Tests...")
    @Key("terminatingTestsProgressMessage")
    String terminatingTestsProgressMessage();

    /**
     * Translated "Error Terminating Tests".
     *
     * @return translated "Error Terminating Tests"
     */
    @DefaultMessage("Error Terminating Tests")
    @Key("errorTerminatingTestsCaption")
    String errorTerminatingTestsCaption();

    /**
     * Translated "Unable to terminate tests. Please try again.".
     *
     * @return translated "Unable to terminate tests. Please try again."
     */
    @DefaultMessage("Unable to terminate tests. Please try again.")
    @Key("errorTerminatingTestsMessage")
    String errorTerminatingTestsMessage();

    /**
     * Translated "and cannot be undone.".
     *
     * @return translated "and cannot be undone."
     */
    @DefaultMessage("and cannot be undone.")
    @Key("cannotBeUndoneText")
    String cannotBeUndoneText();

    /**
     * Translated "with ''{0}'' and cannot be undone.".
     *
     * @return translated "with ''{0}'' and cannot be undone."
     */
    @DefaultMessage("with ''{0}'' and cannot be undone.")
    @Key("replaceCannotBeUndoneText")
    String replaceCannotBeUndoneText(String replaceText);

}
