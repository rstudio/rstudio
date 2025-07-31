/*
 * StudioClientProjectConstants.java
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

package org.rstudio.studio.client.projects;

public interface StudioClientProjectConstants extends com.google.gwt.i18n.client.Messages {

    @Key("openProjectLabel")
    String openProjectLabel();

    @Key("openProjectCaption")
    String openProjectCaption();

    @Key("projectFilter")
    String projectFilter();

    @Key("showVCSMenuLabel")
    String showVCSMenuLabel(String vcsName);

    @Key("zoomVCSMenuLabel")
    String zoomVCSMenuLabel(String vcsName);

    @Key("updateButtonLabel")
    String updateButtonLabel();

    @Key("updateMenuLabel")
    String updateMenuLabel();

    @Key("newProjectCaption")
    String newProjectCaption();

    @Key("errorCaption")
    String errorCaption();

    @Key("newProjectProjectIndicator")
    String newProjectProjectIndicator();

    @Key("creatingProjectError")
    String creatingProjectError();

    @Key("savingDefaultsLabel")
    String savingDefaultsLabel();

    @Key("cloneSVNRepoLabel")
    String cloneSVNRepoLabel();

    @Key("cloneGitRepoLabel")
    String cloneGitRepoLabel();

    @Key("vcsCloneFailMessage")
    String vcsCloneFailMessage();

    @Key("invalidPackageMessage")
    String invalidPackageMessage(String packageName);

    @Key("invalidPackageName")
    String invalidPackageName(String packageName);

    @Key("creatingProjectLabel")
    String creatingProjectLabel();

    @Key("projectFailedMessage")
    String projectFailedMessage();

    @Key("creatingProjectCaption")
    String creatingProjectCaption();

    @Key("creatingProjectWithLabel")
    String creatingProjectWithLabel(String pkg);

    @Key("projectContext")
    String projectContext(String pkg);

    @Key("errorInstallingCaption")
    String errorInstallingCaption(String pkg);

    @Key("errorInstallingCaptionMessage")
    String errorInstallingCaptionMessage(String pkg);

    @Key("creatingProjectResultMessage")
    String creatingProjectResultMessage();

    @Key("initializingGitRepoMessage")
    String initializingGitRepoMessage();

    @Key("initializingRenvMessage")
    String initializingRenvMessage();

    @Key("executeOpenProjectMessage")
    String executeOpenProjectMessage();

    @Key("noActiveProjectCaption")
    String noActiveProjectCaption();

    @Key("noActiveProjectMessage")
    String noActiveProjectMessage();

    @Key("versionControlProjectSetupMessage")
    String versionControlProjectSetupMessage();

    @Key("errorReadingOptionsCaption")
    String errorReadingOptionsCaption();

    @Key("readingOptionsMessage")
    String readingOptionsMessage();

    @Key("confirmOpenProjectCaption")
    String confirmOpenProjectCaption();

    @Key("openProjectPathMessage")
    String openProjectPathMessage(String projectPath);

    @Key("openProjectError")
    String openProjectError(String project, String message);

    @Key("openProjectErrorMessage")
    String openProjectErrorMessage();

    @Key("errorOpeningProjectCaption")
    String errorOpeningProjectCaption();

    @Key("switchProjectsCaption")
    String switchProjectsCaption();

    @Key("onShowDiagnosticsProject")
    String onShowDiagnosticsProject();

    @Key("projectOpenError")
    String projectOpenError(String projectFilePath);

    @Key("noneLabel")
    String noneLabel();

    @Key("createPackageFormLabel")
    String createPackageFormLabel();

    @Key("addButtonCaption")
    String addButtonCaption();

    @Key("removeButtonCaption")
    String removeButtonCaption();

    @Key("addSourceFileCaption")
    String addSourceFileCaption();

    @Key("existingDirectoryTitle")
    String existingDirectoryTitle();

    @Key("existingDirectorySubTitle")
    String existingDirectorySubTitle();

    @Key("existingDirectoryPageCaption")
    String existingDirectoryPageCaption();

    @Key("projectWorkingDirectoryTitle")
    String projectWorkingDirectoryTitle();

    @Key("validateMessage")
    String validateMessage();

    @Key("homeDirectoryErrorMessage")
    String homeDirectoryErrorMessage();

    @Key("cloneGitRepo")
    String cloneGitRepo();

    @Key("cloneGitRepoPageCaption")
    String cloneGitRepoPageCaption();

    @Key("newDirectoryTitle")
    String newDirectoryTitle();

    @Key("newDirectorySubTitle")
    String newDirectorySubTitle();

    @Key("newDirectoryPageCaption")
    String newDirectoryPageCaption();

    @Key("titleName")
    String titleName();

    @Key("newProjectTitle")
    String newProjectTitle();

    @Key("newProjectSubTitle")
    String newProjectSubTitle();

    @Key("createNewProjectPageCaption")
    String createNewProjectPageCaption();

    @Key("newProjectParentLabel")
    String newProjectParentLabel();

    @Key("createGitRepoLabel")
    String createGitRepoLabel();

    @Key("chkRenvInitLabel")
    String chkRenvInitLabel();

    @Key("chkRenvInitUserAction")
    String chkRenvInitUserAction();

    @Key("directoryNameLabel")
    String directoryNameLabel();

    @Key("specifyProjectDirectoryName")
    String specifyProjectDirectoryName();

    @Key("newProjectWizardCaption")
    String newProjectWizardCaption();

    @Key("createProjectCaption")
    String createProjectCaption();

    @Key("openNewSessionLabel")
    String openNewSessionLabel();

    @Key("createProjectFromLabel")
    String createProjectFromLabel();

    @Key("newPackageTitle")
    String newPackageTitle();

    @Key("createNewPackageSubTitle")
    String createNewPackageSubTitle();

    @Key("createRPackagePageCaption")
    String createRPackagePageCaption();

    @Key("typeLabel")
    String typeLabel();

    @Key("packageNameLabel")
    String packageNameLabel();

    @Key("rcppPackageOption")
    String rcppPackageOption();

    @Key("validateAsyncMessage")
    String validateAsyncMessage(String packageName);

    @Key("fileAlreadyExistsMessage")
    String fileAlreadyExistsMessage(String path);

    @Key("directoryAlreadyExistsMessage")
    String directoryAlreadyExistsMessage(String path);

    @Key("pleaseEnterDirectoryNameMessage")
    String pleaseEnterDirectoryNameMessage();

    @Key("quartoBookTitle")
    String quartoBookTitle();

    @Key("quartoBookSubTitle")
    String quartoBookSubTitle();

    @Key("quartoBookPageCaption")
    String quartoBookPageCaption();

    @Key("quartoManuscriptTitle")
    String quartoManuscriptTitle();

    @Key("quartoManuscriptSubTitle")
    String quartoManuscriptSubTitle();

    @Key("quartoManuscriptPageCaption")
    String quartoManuscriptPageCaption();

   

    @Key("quartoProjectTitle")
    String quartoProjectTitle();

    @Key("quartoProjectSubTitle")
    String quartoProjectSubTitle();

    @Key("quartoProjectPageCaption")
    String quartoProjectPageCaption();

    @Key("typeText")
    String typeText();

    @Key("projectTypeDefault")
    String projectTypeDefault();

    @Key("projectTypeWebsite")
    String projectTypeWebsite();

    @Key("projectTypeBook")
    String projectTypeBook();
    

    @Key("projectTypeManuscript")
    String projectTypeManuscript();

    @Key("engineLabel")
    String engineLabel();

    @Key("engineSelectNone")
    String engineSelectNone();

    @Key("kernelLabel")
    String kernelLabel();

    @Key("chkUseVenvLabel")
    String chkUseVenvLabel();

    @Key("txtVenvPackagesNone")
    String txtVenvPackagesNone();

    @Key("quartoProjectTypeOption")
    String quartoProjectTypeOption();

    @Key("quartoProjectEngineOption")
    String quartoProjectEngineOption();

    @Key("quartoProjectKernelOption")
    String quartoProjectKernelOption();

    @Key("quartoBlogTitle")
    String quartoBlogTitle();

    @Key("quartoBlogSubTitle")
    String quartoBlogSubTitle();

    @Key("quartoBlogPageCaption")
    String quartoBlogPageCaption();

    @Key("quartoWebsiteTitle")
    String quartoWebsiteTitle();

    @Key("quartoWebsiteSubTitle")
    String quartoWebsiteSubTitle();

    @Key("quartoWebsitePageCaption")
    String quartoWebsitePageCaption();

    @Key("shinyApplicationTitle")
    String shinyApplicationTitle();

    @Key("shinyApplicationSubTitle")
    String shinyApplicationSubTitle();

    @Key("shinyApplicationPageCaption")
    String shinyApplicationPageCaption();

    @Key("svnPageTitle")
    String svnPageTitle();

    @Key("svnPageSubTitle")
    String svnPageSubTitle();

    @Key("svnPagePageCaption")
    String svnPagePageCaption();

    @Key("versionControlTitle")
    String versionControlTitle();

    @Key("versionControlSubTitle")
    String versionControlSubTitle();

    @Key("versionControlPageCaption")
    String versionControlPageCaption();

    @Key("acceptNavigationHTML")
    String acceptNavigationHTML(String title, String location);

    @Key("optionsLabel")
    String optionsLabel();

    @Key("preferencesLabel")
    String preferencesLabel();

    @Key("vcsHelpLink")
    String vcsHelpLink(String title);

    @Key("installationNotDetectedHTML")
    String installtionNotDetectedHTML(String title);

    @Key("titleNotFound")
    String titleNotFound(String title);

    @Key("okLabel")
    String okLabel();

    @Key("repoURLLabel")
    String repoURLLabel();

    @Key("usernameLabel")
    String usernameLabel();

    @Key("projDirNameLabel")
    String projDirNameLabel();

    @Key("existingRepoDestDirLabel")
    String existingRepoDestDirLabel();

    @Key("specifyRepoURLErrorMessage")
    String specifyRepoURLErrorMessage();

    @Key("pdfGenerationCaption")
    String pdfGenerationCaption();

    @Key("pdfPreviewCaption")
    String pdfPreviewCaption();

    @Key("compilePDFLabel")
    String compilePDFLabel();

    @Key("compilePDFEmptyLabel")
    String compilePDFEmptyLabel();

    @Key("browseActionLabel")
    String browseActionLabel();

    @Key("rootDocumentChooserTitle")
    String rootDocumentChooserTitle();

    @Key("chooseFileCaption")
    String chooseFileCaption();

    @Key("enableCodeIndexingLabel")
    String enableCodeIndexingLabel();

    @Key("chkSpacesForTabLabel")
    String chkSpacesForTabLabel();

    @Key("tabWidthLabel")
    String tabWidthLabel();

    @Key("useNativePipeOperatorLabel")
    String useNativePipeOperatorLabel();

    @Key("chkAutoAppendNewlineLabel")
    String chkAutoAppendNewlineLabel();

    @Key("chkStripTrailingWhitespaceLabel")
    String chkStripTrailingWhitespaceLabel();

    @Key("textEncodingLabel")
    String textEncodingLabel();

    @Key("changeLabel")
    String changeLabel();

    @Key("codingEditingLabel")
    String codingEditingLabel();

    @Key("projectGeneralInfoLabel")
    String projectGeneralInfoLabel();

    @Key("restoreWorkspaceText")
    String restoreWorkspaceText();

    @Key("saveWorkspaceText")
    String saveWorkspaceText();

    @Key("alwaysSaveHistoryText")
    String alwaysSaveHistoryText();

    @Key("disableExecuteRprofileText")
    String disableExecuteRprofileText();

    @Key("quitChildProcessesOnExitText")
    String quitChildProcessesOnExitText();

    @Key("generalText")
    String generalText();

    @Key("initializePackratMessage")
    String initializePackratMessage();

    @Key("chkUsePackratLabel")
    String chkUsePackratLabel();

    @Key("chkAutoSnapshotLabel")
    String chkAutoSnapshotLabel();

    @Key("chkVcsIgnoreLibLabel")
    String chkVcsIgnoreLibLabel(String vcsName);

    @Key("chkVcsIgnoreSrcLabel")
    String chkVcsIgnoreSrcLabel(String vcsName);

    @Key("panelExternalPackagesText")
    String panelExternalPackagesText();

    @Key("panelExternalPackagesTitle")
    String panelExternalPackagesTitle();

    @Key("packratHelpLink")
    String packratHelpLink();

    @Key("verifyPrerequisitesLabel")
    String verifyPrerequisitesLabel();

    @Key("packratManagePackages")
    String packratManagePackages();

    @Key("projectOptionsCaption")
    String projectOptionsCaption();

    @Key("restartRStudioCaption")
    String restartRStudioCaption();

    @Key("restartRStudioMessage")
    String restartRStudioMessage();

    @Key("useDefaultText")
    String useDefaultText();

    @Key("environmentsText")
    String environmentsText();

    @Key("rstudioInitializeLabel")
    String rstudioInitializeLabel();

    @Key("renvHelpLink")
    String renvHelpLink();

    @Key("visualModeCaption")
    String visualModeCaption();

    @Key("rMarkdownInfoLabel")
    String rMarkdownInfoLabel();

    @Key("wrapColumnLabel")
    String wrapColumnLabel();

    @Key("wrapPanelText")
    String wrapPanelText();

    @Key("referencesDefaultItem")
    String referencesDefaultItem();

    @Key("referencesFormLabel")
    String referencesFormLabel();

    @Key("canonicalDefaultItem")
    String canonicalDefaultItem();

    @Key("canonicalTrueItem")
    String canonicalTrueItem();

    @Key("canonicalFalseItem")
    String canonicalFalseItem();

    @Key("canonicalFormLabel")
    String canonicalFormLabel();

    @Key("markdownPerFileOptionsCaption")
    String markdownPerFileOptionsCaption();

    @Key("visualModeZoteroCaption")
    String visualModeZoteroCaption();

    @Key("rMarkdownText")
    String rMarkdownText();

    @Key("sharingText")
    String sharingText();

    @Key("vcsSelectLabel")
    String vcsSelectLabel();

    @Key("originLabel")
    String originLabel();

    @Key("lblOrigin")
    String lblOrigin();

    @Key("repoCaption")
    String repoCaption();

    @Key("confirmGitRepoLabel")
    String confirmGitRepoLabel();

    @Key("confirmGitRepoCaption")
    String confirmGitRepoCaption();

    @Key("confirmGitRepoMessage")
    String confirmGitRepoMessage();

    @Key("noneProjectSourceControlLabel")
    String noneProjectSourceControlLabel();

    @Key("dictionariesCaption")
    String dictionariesCaption();

    @Key("dictionariesInfoLabel")
    String dictionariesInfoLabel();

    @Key("spellingText")
    String spellingText();

    @Key("yesLabel")
    String yesLabel();

    @Key("noLabel")
    String noLabel();

    @Key("askLabel")
    String askLabel();

    @Key("pathSelectorLabel")
    String pathSelectorLabel();

    @Key("validateScriptCaption")
    String validateScriptCaption();

    @Key("validateScriptMessage")
    String validateScriptMessage();

    @Key("pathSelectorMakefileDirLabel")
    String pathSelectorMakefileDirLabel();

    @Key("txtMakefileArgs")
    String txtMakefileArgs();

    @Key("pathSelectorPackageDir")
    String pathSelectorPackageDir();

    @Key("chkUseDevtoolsCaption")
    String chkUseDevtoolsCaption();

    @Key("cleanBeforeInstallLabel")
    String cleanBeforeInstallLabel();

    @Key("chkUseRoxygenCaption")
    String chkUseRoxygenCaption();

    @Key("btnConfigureRoxygenLabel")
    String btnConfigureRoxygenLabel();

    @Key("installMdashArgument")
    String installMdashArgument();

    @Key("checkPackageMdashArgument")
    String checkPackageMdashArgument();

    @Key("buildSourceMdashArgument")
    String buildSourceMdashArgument();

    @Key("buildBinaryMdashArgument")
    String buildBinaryMdashArgument();

    @Key("browseLabel")
    String browseLabel();

    @Key("projectRootLabel")
    String projectRootLabel();

    @Key("chooseDirectoryCaption")
    String chooseDirectoryCaption();

    @Key("noneFileSelectorLabel")
    String noneFileSelectorLabel();

    @Key("roxygenOptionsCaption")
    String roxygenOptionsCaption();

    @Key("pathSelectorSiteDir")
    String pathSelectorSiteDir();

    @Key("websiteOutputFormatLabel")
    String websiteOutputFormatLabel();

    @Key("allLabel")
    String allLabel();

    @Key("chkPreviewAfterBuildingCaption")
    String chkPreviewAfterBuildingCaption();

    @Key("chkLivePreviewSiteCaption")
    String chkLivePreviewSiteCaption();

    @Key("infoLabel")
    String infoLabel();

    @Key("chkPreviewAfterBuilding")
    String chkPreviewAfterBuilding();

    @Key("allFormatsLabel")
    String allFormatsLabel();

    @Key("buildToolsLabel")
    String buildToolsLabel();

    @Key("projectBuildToolsLabel")
    String projectBuildToolsLabel();

    @Key("packageLabel")
    String packageLabel();

    @Key("gitLabel")
    String gitLabel();

    @Key("closeProjectLabel")
    String closeProjectLabel();

    @Key("placeholderLabel")
    String placeholderLabel();

    @Key("chkUseCacheLabel")
    String chkUseCacheLabel();

    @Key("useCondaenv")
    String useCondaenv();
    

    @Key("editingTitle")
    String editingTitle();
 

    @Key("indexingTitle")
    String indexingTitle();
    

    @Key("savingTitle")
    String savingTitle();
    

    @Key("workspaceTitle")
    String workspaceTitle();
    

    @Key("miscellaneousTitle")
    String miscellaneousTitle();

    @Key("generalTitle")
    String generalTitle();
    
    

    @Key("customProjectNameLabel")
    String customProjectNameLabel();
    

    @Key("scratchPathLabel")
    String scratchPathLabel();
    
}
