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

    @Key("toolBarTitle")
    String toolBarTitle(String title);

    @Key("viewLogText")
    String viewLogText();

    @Key("compilePDFTaskName")
    String compilePDFTaskName();

    @Key("viewLogTitle")
    String viewLogTitle();

    @Key("closingCompilePDFProgressMessage")
    String closingCompilePDFProgressMessage();

    @Key("closeCompilePDF")
    String closeCompilePDF();

    @Key("compilingPDFProgressMessage")
    String compilingPDFProgressMessage();

    @Key("stopRunningCompilesCaption")
    String stopRunningCompilesCaption();

    @Key("stopPDFCompilationRunningMessage")
    String stopPDFCompilationRunningMessage(String operation);

    @Key("terminatingPDFCompilationCaption")
    String terminatingPDFCompilationCaption();

    @Key("compilePDFCaption")
    String compilePDFCaption();

    @Key("unableToTerminatePDFCompilationMessage")
    String unableToTerminatePDFCompilationMessage();

    @Key("dataOutputTitle")
    String dataOutputTitle();

    @Key("dataOutputPaneTitle")
    String dataOutputPaneTitle();

    @Key("dataOutputTabLabel")
    String dataOutputTabLabel();

    @Key("sqlResultsTitle")
    String sqlResultsTitle();

    @Key("findInFilesCaption")
    String findInFilesCaption();

    @Key("searchInLabel")
    String searchInLabel();

    @Key("findButtonCaption")
    String findButtonCaption();

    @Key("errorCaption")
    String errorCaption();

    @Key("errorMessage")
    String errorMessage();

    @Key("customFilterPatterValue")
    String customFilterPatterValue();

    @Key("overFlowMessage")
    String overFlowMessage();

    @Key("findResultsTitle")
    String findResultsTitle();

    @Key("findOutputTabLabel")
    String findOutputTabLabel();

    @Key("stopFindInFilesTitle")
    String stopFindInFilesTitle();

    @Key("findLabel")
    String findLabel();

    @Key("replaceLabel")
    String replaceLabel();

    @Key("replaceWithLabel")
    String replaceWithLabel();

    @Key("stopReplaceTitle")
    String stopReplaceTitle();

    @Key("replaceAllText")
    String replaceAllText();

    @Key("findInFilesResultsTitle")
    String findInFilesResultsTitle();

    @Key("noResultsFoundText")
    String noResultsFoundText();

    @Key("resultsForWholeWordText")
    String resultsForWholeWordText();

    @Key("resultsForText")
    String resultsForText();

    @Key("inText")
    String inText();

    @Key("replaceResultsWholeWordText")
    String replaceResultsWholeWordText();

    @Key("replaceResultsForText")
    String replaceResultsForText();

    @Key("withText")
    String withText();

    @Key("summaryLabel")
    String summaryLabel(int successCount, int errorCount);

    @Key("stopReplaceMessage")
    String stopReplaceMessage();

    @Key("replaceAllQuestion")
    String replaceAllQuestion();

    @Key("removeText")
    String removeText();

    @Key("replaceText")
    String replaceText();

    @Key("replaceMessage")
    String replaceMessage(int resultsCount, String query);

    @Key("diagnosticsLabel")
    String diagnosticsLabel();

    @Key("switchActiveMarkerListTitle")
    String switchActiveMarkerListTitle();

    @Key("noMarkersText")
    String noMarkersText();

    @Key("markersTitle")
    String markersTitle();

    @Key("clearMarkersTitle")
    String clearMarkersTitle();

    @Key("markersTabLabel")
    String markersTabLabel();

    @Key("rMarkdownTitle")
    String rMarkdownTitle();

    @Key("viewRMarkdownTitle")
    String viewRMarkdownTitle();

    @Key("stopRMarkdownRenderingCaption")
    String stopRMarkdownRenderingCaption();

    @Key("stopRMarkdownRenderingMessage")
    String stopRMarkdownRenderingMessage(String targetFile);

    @Key("stopLabel")
    String stopLabel();

    @Key("cancelLabel")
    String cancelLabel();

    @Key("knitTerminateFailedMessage")
    String knitTerminateFailedMessage();

    @Key("renderTitle")
    String renderTitle();

    @Key("deployTitle")
    String deployTitle();

    @Key("sourceCppTitle")
    String sourceCppTitle();

    @Key("cPlusPlusTabTitle")
    String cPlusPlusTabTitle();

    @Key("viewTestResultsTitle")
    String viewTestResultsTitle();

    @Key("testsTaskName")
    String testsTaskName();

    @Key("terminatingTestsProgressMessage")
    String terminatingTestsProgressMessage();

    @Key("errorTerminatingTestsCaption")
    String errorTerminatingTestsCaption();

    @Key("errorTerminatingTestsMessage")
    String errorTerminatingTestsMessage();

    @Key("cannotBeUndoneText")
    String cannotBeUndoneText();

    @Key("replaceCannotBeUndoneText")
    String replaceCannotBeUndoneText(String replaceText);

}
