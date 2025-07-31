/*
 * StudioClientCommonConstants.java
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

package org.rstudio.studio.client.common;

public interface StudioClientCommonConstants extends com.google.gwt.i18n.client.Messages {

    @Key("errorCaption")
    String errorCaption();

    @Key("stopTitle")
    String stopTitle();

    @Key("outputLeftLabel")
    String outputLeftLabel();

    @Key("issuesRightLabel")
    String issuesRightLabel();

    @Key("clearAllBreakpointsCaption")
    String clearAllBreakpointsCaption();

    @Key("hideTracebackText")
    String hideTracebackText();

    @Key("showTracebackText")
    String showTracebackText();

    @Key("convertingThemeProgressCaption")
    String convertingThemeProgressCaption();

    @Key("withDataImportMongoProgressCaption")
    String withDataImportMongoProgressCaption();

    @Key("testthatMessage")
    String testthatMessage();

    @Key("shinytestMessage")
    String shinytestMessage();

    @Key("preparingTestsProgressCaption")
    String preparingTestsProgressCaption();

    @Key("testingToolsContext")
    String testingToolsContext();

    @Key("noFilesSelectedCaption")
    String noFilesSelectedCaption();

    @Key("noFilesSelectedMessage")
    String noFilesSelectedMessage();

    @Key("downloadButtonCaption")
    String downloadButtonCaption();

    @Key("rCodeBrowserLabel")
    String rCodeBrowserLabel();

    @Key("rDataFrameLabel")
    String rDataFrameLabel();

    @Key("publicFolderDesc")
    String publicFolderDesc();

    @Key("folderDesc")
    String folderDesc();

    @Key("textFileDesc")
    String textFileDesc();

    @Key("imageFileDesc")
    String imageFileDesc();

    @Key("parentFolderDesc")
    String parentFolderDesc();

    @Key("exploreObjectDesc")
    String exploreObjectDesc();

    @Key("rSourceViewerDesc")
    String rSourceViewerDesc();

    @Key("profilerDesc")
    String profilerDesc();

    @Key("rScriptLabel")
    String rScriptLabel();

    @Key("rdFile")
    String rdFile();

    @Key("namespaceLabel")
    String namespaceLabel();

    @Key("rHistoryLabel")
    String rHistoryLabel();

    @Key("rMarkdownLabel")
    String rMarkdownLabel();

    @Key("rNotebookLabel")
    String rNotebookLabel();

    @Key("markdownLabel")
    String markdownLabel();

    @Key("fileDownloadErrorCaption")
    String fileDownloadErrorCaption();

    @Key("fileDownloadErrorMessage")
    String fileDownloadErrorMessage();

    @Key("openLabel")
    String openLabel();

    @Key("saveLabel")
    String saveLabel();

    @Key("chooseLabel")
    String chooseLabel();

    @Key("andText")
    String andText();

    @Key("typesetLatexLabel")
    String typesetLatexLabel();

    @Key("latexHelpLinkLabel")
    String latexHelpLinkLabel();

    @Key("errorSettingCranMirror")
    String errorSettingCranMirror();

    @Key("cranMirrorCannotChange")
    String cranMirrorCannotChange();

    @Key("insertRoxygenSkeletonMessage")
    String insertRoxygenSkeletonMessage();

    @Key("unableToInsertSkeletonMessage")
    String unableToInsertSkeletonMessage();

    @Key("cannotUpdateRoxygenMessage")
    String cannotUpdateRoxygenMessage();

    @Key("confirmChangeCaption")
    String confirmChangeCaption();

    @Key("packageRequiredMessage")
    String packageRequiredMessage(String packageName, String name);

    @Key("uploadErrorTitle")
    String uploadErrorTitle();

    @Key("unableToContinueMessage")
    String unableToContinueMessage();

    @Key("uploadingDocumentRPubsMessage")
    String uploadingDocumentRPubsMessage();

    @Key("publishToRPubs")
    String publishToRPubs();

    @Key("rPubsServiceMessage")
    String rPubsServiceMessage();

    @Key("alreadyPublishedRPubs")
    String alreadyPublishedRPubs();

    @Key("importantMessage")
    String importantMessage();

    @Key("publishMessage")
    String publishMessage();

    @Key("importantRPubsMessage")
    String importantRPubsMessage(String importantMessage, String publishMessage);

    @Key("publishButtonTitle")
    String publishButtonTitle();

    @Key("updateExistingButtonTitle")
    String updateExistingButtonTitle();

    @Key("createNewButtonTitle")
    String createNewButtonTitle();

    @Key("cancelTitle")
    String cancelTitle();

    @Key("okTitle")
    String okTitle();

    @Key("usingKeyringCaption")
    String usingKeyringCaption();

    @Key("keyringDesc")
    String keyringDesc();

    @Key("installKeyringMessage")
    String installKeyringMessage();

    @Key("keyringCaption")
    String keyringCaption();

    @Key("installLabel")
    String installLabel();

    @Key("enterValueMessage")
    String enterValueMessage();

    @Key("consoleOutputOverLimitMessage")
    String consoleOutputOverLimitMessage();

    @Key("consoleWriteError")
    String consoleWriteError(String error);

    @Key("lineText")
    String lineText();

    @Key("viewErrorLogfile")
    String viewErrorLogfile();

    @Key("getSyncProgressMessage")
    String getSyncProgressMessage();

    @Key("pdfPageText")
    String pdfPageText();

    @Key("pdfFromClickText")
    String pdfFromClickText();

    @Key("passwordTitle")
    String passwordTitle();

    @Key("patTitle")
    String patTitle();

    @Key("patPrompt")
    String patPrompt();

    @Key("usernameTitle")
    String usernameTitle();

    @Key("pressLabel")
    String pressLabel(String cmdText);

    @Key("closeButtonLabel")
    String closeButtonLabel();

    @Key("noneLabel")
    String noneLabel();

    @Key("gettingIgnoredFilesProgressMessage")
    String gettingIgnoredFilesProgressMessage();

    @Key("settingIgnoredFilesProgressMessage")
    String settingIgnoredFilesProgressMessage();

    @Key("multipleDirectoriesCaption")
    String multipleDirectoriesCaption();

    @Key("selectedFilesNotInSameDirectoryMessage")
    String selectedFilesNotInSameDirectoryMessage();

    @Key("directoryLabel")
    String directoryLabel();

    @Key("ignoredFilesLabel")
    String ignoredFilesLabel();

    @Key("ignoreCaption")
    String ignoreCaption();

    @Key("specifyingIgnoredFilesHelpCaption")
    String specifyingIgnoredFilesHelpCaption();

    @Key("loadingFileContentsProgressCaption")
    String loadingFileContentsProgressCaption();

    @Key("saveAsText")
    String saveAsText();

    @Key("saveFileCaption")
    String saveFileCaption(String name);

    @Key("savingFileProgressCaption")
    String savingFileProgressCaption();

    @Key("viewFileTabLabel")
    String viewFileTabLabel();

    @Key("atText")
    String atText();

    @Key("warningMessage")
    String warningMessage();

    @Key("rPresentationLabel")
    String rPresentationLabel();

    @Key("sourceMarkerItemTableList")
    String sourceMarkerItemTableList();

    @Key("previewButtonText")
    String previewButtonText();

    @Key("genericContentLabel")
    String genericContentLabel();

    @Key("objectExplorerLabel")
    String objectExplorerLabel();

    @Key("rProfilerLabel")
    String rProfilerLabel();

    @Key("checkPreviewButtonText")
    String checkPreviewButtonText();

    @Key("weaveRnwLabel")
    String weaveRnwLabel();

    @Key("weaveRnwHelpTitle")
    String weaveRnwHelpTitle();

    @Key("sshRSAKeyFormLabel")
    String sshRSAKeyFormLabel();

    @Key("viewPublicKeyCaption")
    String viewPublicKeyCaption();

    @Key("createRSAKeyButtonLabel")
    String createRSAKeyButtonLabel();

    @Key("readingPublicKeyProgressCaption")
    String readingPublicKeyProgressCaption();

    @Key("clearAllBreakpointsMessage")
    String clearAllBreakpointsMessage();

    @Key("functionNameText")
    String functionNameText(String functionName, String source);

    @Key("showFileExportLabel")
    String showFileExportLabel(String description);

    @Key("accountListLabel")
    String accountListLabel();

    @Key("connectButtonLabel")
    String connectButtonLabel();

    @Key("reconnectButtonLabel")
    String reconnectButtonLabel();

    @Key("disconnectButtonLabel")
    String disconnectButtonLabel();

    @Key("missingPkgPanelMessage")
    String missingPkgPanelMessage();

    @Key("missingPkgRequiredMessage")
    String missingPkgRequiredMessage();

    @Key("installPkgsMessage")
    String installPkgsMessage();

    @Key("withRSConnectLabel")
    String withRSConnectLabel();

    @Key("chkEnableRSConnectLabel")
    String chkEnableRSConnectLabel();

    @Key("checkBoxWithHelpTitle")
    String checkBoxWithHelpTitle();

    @Key("settingsHeaderLabel")
    String settingsHeaderLabel();

    @Key("chkEnablePublishingLabel")
    String chkEnablePublishingLabel();

    @Key("showPublishDiagnosticsLabel")
    String showPublishDiagnosticsLabel();

    @Key("sSLCertificatesHeaderLabel")
    String sSLCertificatesHeaderLabel();

    @Key("publishCheckCertificatesLabel")
    String publishCheckCertificatesLabel();

    @Key("usePublishCaBundleLabel")
    String usePublishCaBundleLabel();

    @Key("caBundlePath")
    String caBundlePath();

    @Key("helpLinkTroubleshooting")
    String helpLinkTroubleshooting();

    @Key("publishingPaneHeader")
    String publishingPaneHeader();

    @Key("showErrorCaption")
    String showErrorCaption();

    @Key("showErrorMessage")
    String showErrorMessage();

    @Key("removeAccountGlobalDisplay")
    String removeAccountGlobalDisplay();

    @Key("onConfirmDisconnectYesLabel")
    String onConfirmDisconnectYesLabel();

    @Key("onConfirmDisconnectNoLabel")
    String onConfirmDisconnectNoLabel();

    @Key("disconnectingErrorMessage")
    String disconnectingErrorMessage();

    @Key("getAccountCountLabel")
    String getAccountCountLabel();

    @Key("connectAccountCaption")
    String connectAccountCaption();

    @Key("connectAccountOkCaption")
    String connectAccountOkCaption();

    @Key("newRSConnectAccountPageTitle")
    String newRSConnectAccountPageTitle();

    @Key("newRSConnectAccountPageSubTitle")
    String newRSConnectAccountPageSubTitle();

    @Key("newRSConnectAccountPageCaption")
    String newRSConnectAccountPageCaption();

    @Key("wizardNavigationPageTitle")
    String wizardNavigationPageTitle();

    @Key("wizardNavigationPageSubTitle")
    String wizardNavigationPageSubTitle();

    @Key("wizardNavigationPageCaption")
    String wizardNavigationPageCaption();

    @Key("serviceDescription")
    String serviceDescription();

    @Key("serviceMessageDescription")
    String serviceMessageDescription();

    @Key("newRSConnectCloudPageSubTitle")
    String newRSConnectCloudPageSubTitle();

    @Key("newRSConnectCloudPageSub")
    String newRSConnectCloudPageSub();

    @Key("newRSConnectCloudPageCaption")
    String newRSConnectCloudPageCaption();

    @Key("withThemesCaption")
    String withThemesCaption();

    @Key("withRMarkdownCaption")
    String withRMarkdownCaption();

    @Key("installShinyCaption")
    String installShinyCaption();

    @Key("installShinyUserAction")
    String installShinyUserAction(String userAction);

    @Key("installPkgsCaption")
    String installPkgsCaption();

    @Key("withShinyAddinsCaption")
    String withShinyAddinsCaption();

    @Key("withShinyAddinsUserAction")
    String withShinyAddinsUserAction();

    @Key("withStylerCaption")
    String withStylerCaption();

    @Key("withStylerUserAction")
    String withStylerUserAction();

    @Key("withDataImportCSVCaption")
    String withDataImportCSVCaption();

    @Key("withDataImportSAV")
    String withDataImportSAV();

    @Key("withDataImportXLS")
    String withDataImportXLS();

    @Key("withDataImportXML")
    String withDataImportXML();

    @Key("withDataImportJSON")
    String withDataImportJSON();

    @Key("withDataImportJDBC")
    String withDataImportJDBC();

    @Key("withDataImportODBC")
    String withDataImportODBC();

    @Key("withProfvis")
    String withProfvis();

    @Key("withConnectionPackage")
    String withConnectionPackage();

    @Key("withConnectionPackageContext")
    String withConnectionPackageContext();

    @Key("withKeyring")
    String withKeyring();

    @Key("withKeyringUserAction")
    String withKeyringUserAction();

    @Key("withOdbc")
    String withOdbc();

    @Key("withOdbcUserAction")
    String withOdbcUserAction();

    @Key("withTutorialDependencies")
    String withTutorialDependencies();

    @Key("withTutorialDependenciesUserAction")
    String withTutorialDependenciesUserAction();

    @Key("withRagg")
    String withRagg();

    @Key("unsatisfiedVersions")
    String unsatisfiedVersions();

    @Key("requiredVersion")
    String requiredVersion(String version);

    @Key("packageNotFoundUserAction")
    String packageNotFoundUserAction();

    @Key("packageNotFoundMessage")
    String packageNotFoundMessage(String unsatisfiedVersions);

    @Key("onErrorMessage")
    String onErrorMessage();

    @Key("availablePackageErrorMessage")
    String availablePackageErrorMessage();

    @Key("confirmPackageInstallation")
    String confirmPackageInstallation(String name);

    @Key("updatedVersionMessage")
    String updatedVersionMessage(String dependency);

    @Key("installRequiredCaption")
    String installRequiredCaption();

    @Key("globalDisplayEnable")
    String globalDisplayEnable();

    @Key("globalDisplayDisable")
    String globalDisplayDisable();

    @Key("globalDisplayVC")
    String globalDisplayVC();

    @Key("globalDisplayVCMessage")
    String globalDisplayVCMessage();

    @Key("terminalPathLabel")
    String terminalPathLabel();

    @Key("showPublicKeyDialogCaption")
    String showPublicKeyDialogCaption();

    @Key("onSSHErrorMessage")
    String onSSHErrorMessage(String keyPath, String errorMessage);

    @Key("vCSHelpLink")
    String vCSHelpLink();

    @Key("createKeyDialogCaption")
    String createKeyDialogCaption();

    @Key("onProgressLabel")
    String onProgressLabel();

    @Key("setOkButtonCaption")
    String setOkButtonCaption();

    @Key("showErrorCaption")
    String showValidateErrorCaption();

    @Key("showErrorMessage")
    String showValidateErrorMessage();

    @Key("pathCaption")
    String pathCaption();

    @Key("pathHelpCaption")
    String pathHelpCaption();

    @Key("sshKeyTypeLabel")
    String sshKeyTypeLabel();

    @Key("sshKeyRSAOption")
    String sshKeyRSAOption();

    @Key("sshKeyEd25519Option")
    String sshKeyEd25519Option();

    @Key("passphraseLabel")
    String passphraseLabel();

    @Key("passphraseConfirmLabel")
    String passphraseConfirmLabel();

    @Key("confirmOverwriteKeyCaption")
    String confirmOverwriteKeyCaption();

    @Key("confirmOverwriteKeyMessage")
    String confirmOverwriteKeyMessage(String path);

    @Key("spellingLanguageSelectWidgetLabel")
    String spellingLanguageSelectWidgetLabel();

    @Key("addHelpButtonLabel")
    String addHelpButtonLabel();

    @Key("progressDownloadingLabel")
    String progressDownloadingLabel();

    @Key("progressDownloadingLanguagesLabel")
    String progressDownloadingLanguagesLabel();

    @Key("onErrorDownloadingCaption")
    String onErrorDownloadingCaption();

    @Key("includeDefaultOption")
    String includeDefaultOption();

    @Key("allLanguagesInstalledOption")
    String allLanguagesInstalledOption();

    @Key("installIndexOption")
    String installIndexOption();

    @Key("buttonAddLabel")
    String buttonAddLabel();

    @Key("buttonRemoveLabel")
    String buttonRemoveLabel();

    @Key("labelWithHelpText")
    String labelWithHelpText();

    @Key("labelWithHelpTitle")
    String labelWithHelpTitle();

    @Key("fileDialogsCaption")
    String fileDialogsCaption();

    @Key("fileDialogsFilter")
    String fileDialogsFilter();

    @Key("onProgressAddingLabel")
    String onProgressAddingLabel();

    @Key("removeDictionaryCaption")
    String removeDictionaryCaption();

    @Key("removeDictionaryMessage")
    String removeDictionaryMessage(String dictionary);

    @Key("progressRemoveIndicator")
    String progressRemoveIndicator();

    @Key("consoleBufferedMessage")
    String consoleBufferedMessage(int bufferSize);

}
