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

    /**
     * Translated "{0} Tab".
     *
     * @return translated "{0} Tab"
     */
    String toolBarTitle(String title);

    /**
     * Translated "View Log".
     *
     * @return translated "View Log"
     */
    String viewLogText();

    /**
     * Translated "Compile PDF".
     *
     * @return translated "Compile PDF"
     */
    String compilePDFTaskName();

    /**
     * Translated "View the LaTeX compilation log".
     *
     * @return translated "View the LaTeX compilation log"
     */
    String viewLogTitle();

    /**
     * Translated "Closing Compile PDF...".
     *
     * @return translated "Closing Compile PDF..."
     */
    String closingCompilePDFProgressMessage();

    /**
     * Translated "close the Compile PDF tab".
     *
     * @return translated "close the Compile PDF tab"
     */
    String closeCompilePDF();

    /**
     * Translated "Compiling PDF...".
     *
     * @return translated "Compiling PDF..."
     */
    String compilingPDFProgressMessage();

    /**
     * Translated "Stop Running Compiles".
     *
     * @return translated "Stop Running Compiles"
     */
    String stopRunningCompilesCaption();

    /**
     * Translated "There is a PDF compilation currently running. If you {0} it will be terminated. Are you sure you want to stop the running PDF compilation?".
     *
     * @return translated "There is a PDF compilation currently running. If you {0} it will be terminated. Are you sure you want to stop the running PDF compilation?"
     */
    String stopPDFCompilationRunningMessage(String operation);

    /**
     * Translated "Terminating PDF compilation...".
     *
     * @return translated "Terminating PDF compilation..."
     */
    String terminatingPDFCompilationCaption();

    /**
     * Translated "Compile PDF".
     *
     * @return translated "Compile PDF"
     */
    String compilePDFCaption();

    /**
     * Translated "Unable to terminate PDF compilation. Please try again.".
     *
     * @return translated "Unable to terminate PDF compilation. Please try again."
     */
    String unableToTerminatePDFCompilationMessage();

    /**
     * Translated "Data Output".
     *
     * @return translated "Data Output"
     */
    String dataOutputTitle();

    /**
     * Translated "Data Output Pane".
     *
     * @return translated "Data Output Pane"
     */
    String dataOutputPaneTitle();

    /**
     * Translated "Data Output Tab".
     *
     * @return translated "Data Output Tab"
     */
    String dataOutputTabLabel();

    /**
     * Translated "SQL Results".
     *
     * @return translated "SQL Results"
     */
    String sqlResultsTitle();

    /**
     * Translated "Find in Files".
     *
     * @return translated "Find in Files"
     */
    String findInFilesCaption();

    /**
     * Translated "Search in:".
     *
     * @return translated "Search in:"
     */
    String searchInLabel();

    /**
     * Translated "Find".
     *
     * @return translated "Find"
     */
    String findButtonCaption();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String errorCaption();

    /**
     * Translated "You must specify a directory to search.".
     *
     * @return translated "You must specify a directory to search."
     */
    String errorMessage();

    /**
     * Translated "Custom Filter Pattern".
     *
     * @return translated "Custom Filter Pattern"
     */
    String customFilterPatterValue();

    /**
     * Translated "More than 1000 matching lines were found. Only the first 1000 lines are shown.".
     *
     * @return translated "More than 1000 matching lines were found. Only the first 1000 lines are shown."
     */
    String overFlowMessage();

    /**
     * Translated "Find Results".
     *
     * @return translated "Find Results"
     */
    String findResultsTitle();

    /**
     * Translated "Find Output Tab".
     *
     * @return translated "Find Output Tab"
     */
    String findOutputTabLabel();

    /**
     * Translated "Stop find in files".
     *
     * @return translated "Stop find in files"
     */
    String stopFindInFilesTitle();

    /**
     * Translated "Find".
     *
     * @return translated "Find"
     */
    String findLabel();

    /**
     * Translated "Replace".
     *
     * @return translated "Replace"
     */
    String replaceLabel();

    /**
     * Translated "Replace with: ".
     *
     * @return translated "Replace with: "
     */
    String replaceWithLabel();

    /**
     * Translated "Stop replace".
     *
     * @return translated "Stop replace"
     */
    String stopReplaceTitle();

    /**
     * Translated "Replace All".
     *
     * @return translated "Replace All"
     */
    String replaceAllText();

    /**
     * Translated "Find in Files Results".
     *
     * @return translated "Find in Files Results"
     */
    String findInFilesResultsTitle();

    /**
     * Translated "(No results found)".
     *
     * @return translated "(No results found)"
     */
    String noResultsFoundText();

    /**
     * Translated "Results for whole word ".
     *
     * @return translated "Results for whole word "
     */
    String resultsForWholeWordText();

    /**
     * Translated "Results for ".
     *
     * @return translated "Results for "
     */
    String resultsForText();

    /**
     * Translated "in ".
     *
     * @return translated "in "
     */
    String inText();

    /**
     * Translated "Replace results for whole word ".
     *
     * @return translated "Replace results for whole word "
     */
    String replaceResultsWholeWordText();

    /**
     * Translated "Replace results for ".
     *
     * @return translated "Replace results for "
     */
    String replaceResultsForText();

    /**
     * Translated "with ".
     *
     * @return translated "with "
     */
    String withText();

    /**
     * Translated ": {0} successful, {1} failed".
     *
     * @return translated ": {0} successful, {1} failed"
     */
    String summaryLabel(int successCount, int errorCount);

    /**
     * Translated "Are you sure you want to cancel the replace? Changes already made will not be reverted.".
     *
     * @return translated "Are you sure you want to cancel the replace? Changes already made will not be reverted."
     */
    String stopReplaceMessage();

    /**
     * Translated "Are you sure you wish to permanently replace all? This will ".
     *
     * @return translated "Are you sure you wish to permanently replace all? This will "
     */
    String replaceAllQuestion();

    /**
     * Translated "with ".
     *
     * @return translated "remove "
     */
    String removeText();

    /**
     * Translated "replace ".
     *
     * @return translated "replace "
     */
    String replaceText();

    /**
     * Translated "{0} occurrences of ''{1}''".
     *
     * @return translated "{0} occurrences of ''{1}''"
     */
    String replaceMessage(int resultsCount, String query);

    /**
     * Translated "Diagnostics".
     *
     * @return translated "Diagnostics"
     */
    String diagnosticsLabel();

    /**
     * Translated "Switch active marker list".
     *
     * @return translated "Switch active marker list"
     */
    String switchActiveMarkerListTitle();

    /**
     * Translated "(No markers)".
     *
     * @return translated "(No markers)"
     */
    String noMarkersText();

    /**
     * Translated "Markers".
     *
     * @return translated "Markers"
     */
    String markersTitle();

    /**
     * Translated "Clear markers".
     *
     * @return translated "Clear markers"
     */
    String clearMarkersTitle();

    /**
     * Translated "Markers Tab".
     *
     * @return translated "Markers Tab"
     */
    String markersTabLabel();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    String rMarkdownTitle();

    /**
     * Translated "View the R Markdown render log".
     *
     * @return translated "View the R Markdown render log"
     */
    String viewRMarkdownTitle();

    /**
     * Translated "Stop R Markdown Rendering".
     *
     * @return translated "Stop R Markdown Rendering"
     */
    String stopRMarkdownRenderingCaption();

    /**
     * Translated "The rendering of ''{0}'' is in progress. Do you want to terminate and close the tab?".
     *
     * @return translated "The rendering of ''{0}'' is in progress. Do you want to terminate and close the tab?"
     */
    String stopRMarkdownRenderingMessage(String targetFile);

    /**
     * Translated "Stop".
     *
     * @return translated "Stop"
     */
    String stopLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    String cancelLabel();

    /**
     * Translated "Knit Terminate Failed".
     *
     * @return translated "Knit Terminate Failed"
     */
    String knitTerminateFailedMessage();

    /**
     * Translated "Render".
     *
     * @return translated "Render"
     */
    String renderTitle();

    /**
     * Translated "Deploy".
     *
     * @return translated "Deploy"
     */
    String deployTitle();

    /**
     * Translated "Source Cpp".
     *
     * @return translated "Source Cpp"
     */
    String sourceCppTitle();

    /**
     * Translated "C++ Tab".
     *
     * @return translated "C++ Tab"
     */
    String cPlusPlusTabTitle();

    /**
     * Translated "View test results".
     *
     * @return translated "View test results"
     */
    String viewTestResultsTitle();

    /**
     * Translated "Tests".
     *
     * @return translated "Tests"
     */
    String testsTaskName();

    /**
     * Translated "Terminating Tests...".
     *
     * @return translated "Terminating Tests..."
     */
    String terminatingTestsProgressMessage();

    /**
     * Translated "Error Terminating Tests".
     *
     * @return translated "Error Terminating Tests"
     */
    String errorTerminatingTestsCaption();

    /**
     * Translated "Unable to terminate tests. Please try again.".
     *
     * @return translated "Unable to terminate tests. Please try again."
     */
    String errorTerminatingTestsMessage();

    /**
     * Translated "and cannot be undone.".
     *
     * @return translated "and cannot be undone."
     */
    String cannotBeUndoneText();

    /**
     * Translated "with ''{0}'' and cannot be undone.".
     *
     * @return translated "with ''{0}'' and cannot be undone."
     */
    String replaceCannotBeUndoneText(String replaceText);

}
