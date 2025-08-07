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
    @DefaultMessage("{0} Tab")
    String toolBarTitle(String title);

    /**
     * Translated "View Log".
     *
     * @return translated "View Log"
     */
    @DefaultMessage("View Log")
    String viewLogText();

    /**
     * Translated "Compile PDF".
     *
     * @return translated "Compile PDF"
     */
    @DefaultMessage("Compile PDF")
    String compilePDFTaskName();

    /**
     * Translated "View the LaTeX compilation log".
     *
     * @return translated "View the LaTeX compilation log"
     */
    @DefaultMessage("View the LaTeX compilation log")
    String viewLogTitle();

    /**
     * Translated "Closing Compile PDF...".
     *
     * @return translated "Closing Compile PDF..."
     */
    @DefaultMessage("Closing Compile PDF...")
    String closingCompilePDFProgressMessage();

    /**
     * Translated "close the Compile PDF tab".
     *
     * @return translated "close the Compile PDF tab"
     */
    @DefaultMessage("close the Compile PDF tab")
    String closeCompilePDF();

    /**
     * Translated "Compiling PDF...".
     *
     * @return translated "Compiling PDF..."
     */
    @DefaultMessage("Compiling PDF...")
    String compilingPDFProgressMessage();

    /**
     * Translated "Stop Running Compiles".
     *
     * @return translated "Stop Running Compiles"
     */
    @DefaultMessage("Stop Running Compiles")
    String stopRunningCompilesCaption();

    /**
     * Translated "There is a PDF compilation currently running. If you {0} it will be terminated. Are you sure you want to stop the running PDF compilation?".
     *
     * @return translated "There is a PDF compilation currently running. If you {0} it will be terminated. Are you sure you want to stop the running PDF compilation?"
     */
    @DefaultMessage("There is a PDF compilation currently running. If you {0} it will be terminated. Are you sure you want to stop the running PDF compilation?")
    String stopPDFCompilationRunningMessage(String operation);

    /**
     * Translated "Terminating PDF compilation...".
     *
     * @return translated "Terminating PDF compilation..."
     */
    @DefaultMessage("Terminating PDF compilation...")
    String terminatingPDFCompilationCaption();

    /**
     * Translated "Compile PDF".
     *
     * @return translated "Compile PDF"
     */
    @DefaultMessage("Compile PDF")
    String compilePDFCaption();

    /**
     * Translated "Unable to terminate PDF compilation. Please try again.".
     *
     * @return translated "Unable to terminate PDF compilation. Please try again."
     */
    @DefaultMessage("Unable to terminate PDF compilation. Please try again.")
    String unableToTerminatePDFCompilationMessage();

    /**
     * Translated "Data Output".
     *
     * @return translated "Data Output"
     */
    @DefaultMessage("Data Output")
    String dataOutputTitle();

    /**
     * Translated "Data Output Pane".
     *
     * @return translated "Data Output Pane"
     */
    @DefaultMessage("Data Output Pane")
    String dataOutputPaneTitle();

    /**
     * Translated "Data Output Tab".
     *
     * @return translated "Data Output Tab"
     */
    @DefaultMessage("Data Output Tab")
    String dataOutputTabLabel();

    /**
     * Translated "SQL Results".
     *
     * @return translated "SQL Results"
     */
    @DefaultMessage("SQL Results")
    String sqlResultsTitle();

    /**
     * Translated "Find in Files".
     *
     * @return translated "Find in Files"
     */
    @DefaultMessage("Find in Files")
    String findInFilesCaption();

    /**
     * Translated "Search in:".
     *
     * @return translated "Search in:"
     */
    @DefaultMessage("Search in:")
    String searchInLabel();

    /**
     * Translated "Find".
     *
     * @return translated "Find"
     */
    @DefaultMessage("Find")
    String findButtonCaption();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    String errorCaption();

    /**
     * Translated "You must specify a directory to search.".
     *
     * @return translated "You must specify a directory to search."
     */
    @DefaultMessage("You must specify a directory to search.")
    String errorMessage();

    /**
     * Translated "Custom Filter Pattern".
     *
     * @return translated "Custom Filter Pattern"
     */
    @DefaultMessage("Custom Filter Pattern")
    String customFilterPatterValue();

    /**
     * Translated "More than 1000 matching lines were found. Only the first 1000 lines are shown.".
     *
     * @return translated "More than 1000 matching lines were found. Only the first 1000 lines are shown."
     */
    @DefaultMessage("More than 1000 matching lines were found. Only the first 1000 lines are shown.")
    String overFlowMessage();

    /**
     * Translated "Find Results".
     *
     * @return translated "Find Results"
     */
    @DefaultMessage("Find Results")
    String findResultsTitle();

    /**
     * Translated "Find Output Tab".
     *
     * @return translated "Find Output Tab"
     */
    @DefaultMessage("Find Output Tab")
    String findOutputTabLabel();

    /**
     * Translated "Stop find in files".
     *
     * @return translated "Stop find in files"
     */
    @DefaultMessage("Stop find in files")
    String stopFindInFilesTitle();

    /**
     * Translated "Find".
     *
     * @return translated "Find"
     */
    @DefaultMessage("Find")
    String findLabel();

    /**
     * Translated "Replace".
     *
     * @return translated "Replace"
     */
    @DefaultMessage("Replace")
    String replaceLabel();

    /**
     * Translated "Replace with: ".
     *
     * @return translated "Replace with: "
     */
    @DefaultMessage("Replace with: ")
    String replaceWithLabel();

    /**
     * Translated "Stop replace".
     *
     * @return translated "Stop replace"
     */
    @DefaultMessage("Stop replace")
    String stopReplaceTitle();

    /**
     * Translated "Replace All".
     *
     * @return translated "Replace All"
     */
    @DefaultMessage("Replace All")
    String replaceAllText();

    /**
     * Translated "Find in Files Results".
     *
     * @return translated "Find in Files Results"
     */
    @DefaultMessage("Find in Files Results")
    String findInFilesResultsTitle();

    /**
     * Translated "(No results found)".
     *
     * @return translated "(No results found)"
     */
    @DefaultMessage("(No results found)")
    String noResultsFoundText();

    /**
     * Translated "Results for whole word ".
     *
     * @return translated "Results for whole word "
     */
    @DefaultMessage("Results for whole word ")
    String resultsForWholeWordText();

    /**
     * Translated "Results for ".
     *
     * @return translated "Results for "
     */
    @DefaultMessage("Results for ")
    String resultsForText();

    /**
     * Translated "in ".
     *
     * @return translated "in "
     */
    @DefaultMessage("in ")
    String inText();

    /**
     * Translated "Replace results for whole word ".
     *
     * @return translated "Replace results for whole word "
     */
    @DefaultMessage("Replace results for whole word ")
    String replaceResultsWholeWordText();

    /**
     * Translated "Replace results for ".
     *
     * @return translated "Replace results for "
     */
    @DefaultMessage("Replace results for ")
    String replaceResultsForText();

    /**
     * Translated "with ".
     *
     * @return translated "with "
     */
    @DefaultMessage("with ")
    String withText();

    /**
     * Translated ": {0} successful, {1} failed".
     *
     * @return translated ": {0} successful, {1} failed"
     */
    @DefaultMessage(": {0} successful, {1} failed")
    String summaryLabel(int successCount, int errorCount);

    /**
     * Translated "Are you sure you want to cancel the replace? Changes already made will not be reverted.".
     *
     * @return translated "Are you sure you want to cancel the replace? Changes already made will not be reverted."
     */
    @DefaultMessage("Are you sure you want to cancel the replace? Changes already made will not be reverted.")
    String stopReplaceMessage();

    /**
     * Translated "Are you sure you wish to permanently replace all? This will ".
     *
     * @return translated "Are you sure you wish to permanently replace all? This will "
     */
    @DefaultMessage("Are you sure you wish to permanently replace all? This will ")
    String replaceAllQuestion();

    /**
     * Translated "with ".
     *
     * @return translated "remove "
     */
    @DefaultMessage("remove ")
    String removeText();

    /**
     * Translated "replace ".
     *
     * @return translated "replace "
     */
    @DefaultMessage("replace ")
    String replaceText();

    /**
     * Translated "{0} occurrences of ''{1}''".
     *
     * @return translated "{0} occurrences of ''{1}''"
     */
    @DefaultMessage("{0} occurrences of ''{1}''")
    String replaceMessage(int resultsCount, String query);

    /**
     * Translated "Diagnostics".
     *
     * @return translated "Diagnostics"
     */
    @DefaultMessage("Diagnostics")
    String diagnosticsLabel();

    /**
     * Translated "Switch active marker list".
     *
     * @return translated "Switch active marker list"
     */
    @DefaultMessage("Switch active marker list")
    String switchActiveMarkerListTitle();

    /**
     * Translated "(No markers)".
     *
     * @return translated "(No markers)"
     */
    @DefaultMessage("(No markers)")
    String noMarkersText();

    /**
     * Translated "Markers".
     *
     * @return translated "Markers"
     */
    @DefaultMessage("Markers")
    String markersTitle();

    /**
     * Translated "Clear markers".
     *
     * @return translated "Clear markers"
     */
    @DefaultMessage("Clear markers")
    String clearMarkersTitle();

    /**
     * Translated "Markers Tab".
     *
     * @return translated "Markers Tab"
     */
    @DefaultMessage("Markers Tab")
    String markersTabLabel();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultMessage("R Markdown")
    String rMarkdownTitle();

    /**
     * Translated "View the R Markdown render log".
     *
     * @return translated "View the R Markdown render log"
     */
    @DefaultMessage("View the R Markdown render log")
    String viewRMarkdownTitle();

    /**
     * Translated "Stop R Markdown Rendering".
     *
     * @return translated "Stop R Markdown Rendering"
     */
    @DefaultMessage("Stop R Markdown Rendering")
    String stopRMarkdownRenderingCaption();

    /**
     * Translated "The rendering of ''{0}'' is in progress. Do you want to terminate and close the tab?".
     *
     * @return translated "The rendering of ''{0}'' is in progress. Do you want to terminate and close the tab?"
     */
    @DefaultMessage("The rendering of ''{0}'' is in progress. Do you want to terminate and close the tab?")
    String stopRMarkdownRenderingMessage(String targetFile);

    /**
     * Translated "Stop".
     *
     * @return translated "Stop"
     */
    @DefaultMessage("Stop")
    String stopLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    String cancelLabel();

    /**
     * Translated "Knit Terminate Failed".
     *
     * @return translated "Knit Terminate Failed"
     */
    @DefaultMessage("Knit Terminate Failed")
    String knitTerminateFailedMessage();

    /**
     * Translated "Render".
     *
     * @return translated "Render"
     */
    @DefaultMessage("Render")
    String renderTitle();

    /**
     * Translated "Deploy".
     *
     * @return translated "Deploy"
     */
    @DefaultMessage("Deploy")
    String deployTitle();

    /**
     * Translated "Source Cpp".
     *
     * @return translated "Source Cpp"
     */
    @DefaultMessage("Source Cpp")
    String sourceCppTitle();

    /**
     * Translated "C++ Tab".
     *
     * @return translated "C++ Tab"
     */
    @DefaultMessage("C++ Tab")
    String cPlusPlusTabTitle();

    /**
     * Translated "View test results".
     *
     * @return translated "View test results"
     */
    @DefaultMessage("View test results")
    String viewTestResultsTitle();

    /**
     * Translated "Tests".
     *
     * @return translated "Tests"
     */
    @DefaultMessage("Tests")
    String testsTaskName();

    /**
     * Translated "Terminating Tests...".
     *
     * @return translated "Terminating Tests..."
     */
    @DefaultMessage("Terminating Tests...")
    String terminatingTestsProgressMessage();

    /**
     * Translated "Error Terminating Tests".
     *
     * @return translated "Error Terminating Tests"
     */
    @DefaultMessage("Error Terminating Tests")
    String errorTerminatingTestsCaption();

    /**
     * Translated "Unable to terminate tests. Please try again.".
     *
     * @return translated "Unable to terminate tests. Please try again."
     */
    @DefaultMessage("Unable to terminate tests. Please try again.")
    String errorTerminatingTestsMessage();

    /**
     * Translated "and cannot be undone.".
     *
     * @return translated "and cannot be undone."
     */
    @DefaultMessage("and cannot be undone.")
    String cannotBeUndoneText();

    /**
     * Translated "with ''{0}'' and cannot be undone.".
     *
     * @return translated "with ''{0}'' and cannot be undone."
     */
    @DefaultMessage("with ''{0}'' and cannot be undone.")
    String replaceCannotBeUndoneText(String replaceText);

}
