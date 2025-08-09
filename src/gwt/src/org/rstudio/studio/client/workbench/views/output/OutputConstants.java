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
    String toolBarTitle(String title);
    String viewLogText();
    String compilePDFTaskName();
    String viewLogTitle();
    String closingCompilePDFProgressMessage();
    String closeCompilePDF();
    String compilingPDFProgressMessage();
    String stopRunningCompilesCaption();
    String stopPDFCompilationRunningMessage(String operation);
    String terminatingPDFCompilationCaption();
    String compilePDFCaption();
    String unableToTerminatePDFCompilationMessage();
    String dataOutputTitle();
    String dataOutputPaneTitle();
    String dataOutputTabLabel();
    String sqlResultsTitle();
    String findInFilesCaption();
    String searchInLabel();
    String findButtonCaption();
    String errorCaption();
    String errorMessage();
    String customFilterPatterValue();
    String overFlowMessage();
    String findResultsTitle();
    String findOutputTabLabel();
    String stopFindInFilesTitle();
    String findLabel();
    String replaceLabel();
    String replaceWithLabel();
    String stopReplaceTitle();
    String replaceAllText();
    String findInFilesResultsTitle();
    String noResultsFoundText();
    String resultsForWholeWordText();
    String resultsForText();
    String inText();
    String replaceResultsWholeWordText();
    String replaceResultsForText();
    String withText();
    String summaryLabel(int successCount, int errorCount);
    String stopReplaceMessage();
    String replaceAllQuestion();
    String removeText();
    String replaceText();
    String replaceMessage(int resultsCount, String query);
    String diagnosticsLabel();
    String switchActiveMarkerListTitle();
    String noMarkersText();
    String markersTitle();
    String clearMarkersTitle();
    String markersTabLabel();
    String rMarkdownTitle();
    String viewRMarkdownTitle();
    String stopRMarkdownRenderingCaption();
    String stopRMarkdownRenderingMessage(String targetFile);
    String stopLabel();
    String cancelLabel();
    String knitTerminateFailedMessage();
    String renderTitle();
    String deployTitle();
    String sourceCppTitle();
    String cPlusPlusTabTitle();
    String viewTestResultsTitle();
    String testsTaskName();
    String terminatingTestsProgressMessage();
    String errorTerminatingTestsCaption();
    String errorTerminatingTestsMessage();
    String cannotBeUndoneText();
    String replaceCannotBeUndoneText(String replaceText);
}
