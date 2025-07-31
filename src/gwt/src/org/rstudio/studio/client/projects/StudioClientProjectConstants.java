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

    @DefaultMessage("Open project in a new R session")
    @Key("openProjectLabel")
    String openProjectLabel();

    @DefaultMessage("Open Project")
    @Key("openProjectCaption")
    String openProjectCaption();

    @DefaultMessage("R Projects (*.Rproj)")
    @Key("projectFilter")
    String projectFilter();

    @DefaultMessage("Show _{0}")
    @Key("showVCSMenuLabel")
    String showVCSMenuLabel(String vcsName);

    @DefaultMessage("Zoom _{0}")
    @Key("zoomVCSMenuLabel")
    String zoomVCSMenuLabel(String vcsName);

    @DefaultMessage("Update")
    @Key("updateButtonLabel")
    String updateButtonLabel();

    @DefaultMessage("_Update")
    @Key("updateMenuLabel")
    String updateMenuLabel();

    @DefaultMessage("Save Current Workspace")
    @Key("newProjectCaption")
    String newProjectCaption();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("New Project...")
    @Key("newProjectProjectIndicator")
    String newProjectProjectIndicator();

    @DefaultMessage("Error Creating Project")
    @Key("creatingProjectError")
    String creatingProjectError();

    @DefaultMessage("Saving defaults...")
    @Key("savingDefaultsLabel")
    String savingDefaultsLabel();

    @DefaultMessage("Checking out SVN repository...")
    @Key("cloneSVNRepoLabel")
    String cloneSVNRepoLabel();

    @DefaultMessage("Cloning Git repository...")
    @Key("cloneGitRepoLabel")
    String cloneGitRepoLabel();

    @DefaultMessage("vcsClone failed")
    @Key("vcsCloneFailMessage")
    String vcsCloneFailMessage();

    @DefaultMessage("Invalid package name ''{0}'': package names must start with a letter, and contain only letters and numbers.")
    @Key("invalidPackageMessage")
    String invalidPackageMessage(String packageName);

    @DefaultMessage("Invalid package name {0}")
    @Key("invalidPackageName")
    String invalidPackageName(String packageName);

    @DefaultMessage("Creating project...")
    @Key("creatingProjectLabel")
    String creatingProjectLabel();

    @DefaultMessage("Quarto create project failed")
    @Key("projectFailedMessage")
    String projectFailedMessage();

    @DefaultMessage("Creating project")
    @Key("creatingProjectCaption")
    String creatingProjectCaption();

    @DefaultMessage("Creating a project with {0}")
    @Key("creatingProjectWithLabel")
    String creatingProjectWithLabel(String pkg);

    @DefaultMessage("{0} Project")
    @Key("projectContext")
    String projectContext(String pkg);

    @DefaultMessage("Error installing {0}")
    @Key("errorInstallingCaption")
    String errorInstallingCaption(String pkg);

    @DefaultMessage("Installation of package ''{0}'' failed, and so the project cannot be created. Try installing the package manually with ''install.packages(\"{0}\")''.")
    @Key("errorInstallingCaptionMessage")
    String errorInstallingCaptionMessage(String pkg);

    @DefaultMessage("creating project")
    @Key("creatingProjectResultMessage")
    String creatingProjectResultMessage();

    @DefaultMessage("Initializing git repository...")
    @Key("initializingGitRepoMessage")
    String initializingGitRepoMessage();

    @DefaultMessage("Initializing renv...")
    @Key("initializingRenvMessage")
    String initializingRenvMessage();

    @DefaultMessage("Preparing to open project...")
    @Key("executeOpenProjectMessage")
    String executeOpenProjectMessage();

    @DefaultMessage("No Active Project")
    @Key("noActiveProjectCaption")
    String noActiveProjectCaption();

    @DefaultMessage("Build tools can only be configured from within an RStudio project.")
    @Key("noActiveProjectMessage")
    String noActiveProjectMessage();

    @DefaultMessage("Version control features can only be accessed from within an RStudio project. Note that if you have an existing directory under version control you can associate an RStudio project with that directory using the New Project dialog.")
    @Key("versionControlProjectSetupMessage")
    String versionControlProjectSetupMessage();

    @DefaultMessage("Error Reading Options")
    @Key("errorReadingOptionsCaption")
    String errorReadingOptionsCaption();

    @DefaultMessage("Reading options...")
    @Key("readingOptionsMessage")
    String readingOptionsMessage();

    @DefaultMessage("Confirm Open Project")
    @Key("confirmOpenProjectCaption")
    String confirmOpenProjectCaption();

    @DefaultMessage("Do you want to open the project {0}?")
    @Key("openProjectPathMessage")
    String openProjectPathMessage(String projectPath);

    @DefaultMessage("Project ''{0}'' could not be opened: {1}")
    @Key("openProjectError")
    String openProjectError(String project, String message);

    @DefaultMessage("\n\nEnsure the project URL is correct; if it is, contact the project owner to request access.")
    @Key("openProjectErrorMessage")
    String openProjectErrorMessage();

    @DefaultMessage("Error Opening Project")
    @Key("errorOpeningProjectCaption")
    String errorOpeningProjectCaption();

    @DefaultMessage("Switch Projects")
    @Key("switchProjectsCaption")
    String switchProjectsCaption();

    @DefaultMessage("Analyzing project sources...")
    @Key("onShowDiagnosticsProject")
    String onShowDiagnosticsProject();

    @DefaultMessage("Project ''{0}'' does not exist (it has been moved or deleted), or it is not writeable")
    @Key("projectOpenError")
    String projectOpenError(String projectFilePath);

    @DefaultMessage("none")
    @Key("noneLabel")
    String noneLabel();

    @DefaultMessage("Create package based on source files:")
    @Key("createPackageFormLabel")
    String createPackageFormLabel();

    @DefaultMessage("Add...")
    @Key("addButtonCaption")
    String addButtonCaption();

    @DefaultMessage("Remove")
    @Key("removeButtonCaption")
    String removeButtonCaption();

    @DefaultMessage("Add Source File")
    @Key("addSourceFileCaption")
    String addSourceFileCaption();

    @DefaultMessage("Existing Directory")
    @Key("existingDirectoryTitle")
    String existingDirectoryTitle();

    @DefaultMessage("Associate a project with an existing working directory")
    @Key("existingDirectorySubTitle")
    String existingDirectorySubTitle();

    @DefaultMessage("Create Project from Existing Directory")
    @Key("existingDirectoryPageCaption")
    String existingDirectoryPageCaption();

    @DefaultMessage("Project working directory:")
    @Key("projectWorkingDirectoryTitle")
    String projectWorkingDirectoryTitle();

    @DefaultMessage("You must specify an existing working directory to create the new project within.")
    @Key("validateMessage")
    String validateMessage();

    @DefaultMessage("Your home directory cannot be treated as an RStudio Project; select a different directory.")
    @Key("homeDirectoryErrorMessage")
    String homeDirectoryErrorMessage();

    @DefaultMessage("Clone a project from a Git repository")
    @Key("cloneGitRepo")
    String cloneGitRepo();

    @DefaultMessage("Clone Git Repository")
    @Key("cloneGitRepoPageCaption")
    String cloneGitRepoPageCaption();

    @DefaultMessage("New Directory")
    @Key("newDirectoryTitle")
    String newDirectoryTitle();

    @DefaultMessage("Start a project in a brand new working directory")
    @Key("newDirectorySubTitle")
    String newDirectorySubTitle();

    @DefaultMessage("Project Type")
    @Key("newDirectoryPageCaption")
    String newDirectoryPageCaption();

    @DefaultMessage("title")
    @Key("titleName")
    String titleName();

    @DefaultMessage("New Project")
    @Key("newProjectTitle")
    String newProjectTitle();

    @DefaultMessage("Create a new project in an empty directory")
    @Key("newProjectSubTitle")
    String newProjectSubTitle();

    @DefaultMessage("Create New Project")
    @Key("createNewProjectPageCaption")
    String createNewProjectPageCaption();

    @DefaultMessage("Create project as subdirectory of:")
    @Key("newProjectParentLabel")
    String newProjectParentLabel();

    @DefaultMessage("Create a git repository")
    @Key("createGitRepoLabel")
    String createGitRepoLabel();

    @DefaultMessage("Use renv with this project")
    @Key("chkRenvInitLabel")
    String chkRenvInitLabel();

    @DefaultMessage("Using renv")
    @Key("chkRenvInitUserAction")
    String chkRenvInitUserAction();

    @DefaultMessage("Directory name:")
    @Key("directoryNameLabel")
    String directoryNameLabel();

    @DefaultMessage("You must specify a name for the new project directory.")
    @Key("specifyProjectDirectoryName")
    String specifyProjectDirectoryName();

    @DefaultMessage("New Project Wizard")
    @Key("newProjectWizardCaption")
    String newProjectWizardCaption();

    @DefaultMessage("Create Project")
    @Key("createProjectCaption")
    String createProjectCaption();

    @DefaultMessage("Open in new session")
    @Key("openNewSessionLabel")
    String openNewSessionLabel();

    @DefaultMessage("Create project from:")
    @Key("createProjectFromLabel")
    String createProjectFromLabel();

    @DefaultMessage("R Package")
    @Key("newPackageTitle")
    String newPackageTitle();

    @DefaultMessage("Create a new R package")
    @Key("createNewPackageSubTitle")
    String createNewPackageSubTitle();

    @DefaultMessage("Create R Package")
    @Key("createRPackagePageCaption")
    String createRPackagePageCaption();

    @DefaultMessage("Type:")
    @Key("typeLabel")
    String typeLabel();

    @DefaultMessage("Package name:")
    @Key("packageNameLabel")
    String packageNameLabel();

    @DefaultMessage("Package w/ Rcpp")
    @Key("rcppPackageOption")
    String rcppPackageOption();

    @DefaultMessage("Invalid package name ''{0}''. Package names should start with a letter, and contain only letters and numbers.")
    @Key("validateAsyncMessage")
    String validateAsyncMessage(String packageName);

    @DefaultMessage("A file already exists at path ''{0}''.")
    @Key("fileAlreadyExistsMessage")
    String fileAlreadyExistsMessage(String path);

    @DefaultMessage("Directory ''{0}'' already exists and is not empty.")
    @Key("directoryAlreadyExistsMessage")
    String directoryAlreadyExistsMessage(String path);

    @DefaultMessage("Please enter a new directory name.")
    @Key("pleaseEnterDirectoryNameMessage")
    String pleaseEnterDirectoryNameMessage();

    @DefaultMessage("Quarto Book")
    @Key("quartoBookTitle")
    String quartoBookTitle();

    @DefaultMessage("Create a new Quarto book project")
    @Key("quartoBookSubTitle")
    String quartoBookSubTitle();

    @DefaultMessage("Create Quarto Book")
    @Key("quartoBookPageCaption")
    String quartoBookPageCaption();

    @DefaultMessage("Quarto Manuscript")
    @Key("quartoManuscriptTitle")
    String quartoManuscriptTitle();

    @DefaultMessage("Create a new Quarto manuscript project")
    @Key("quartoManuscriptSubTitle")
    String quartoManuscriptSubTitle();

    @DefaultMessage("Create Quarto Manuscript")
    @Key("quartoManuscriptPageCaption")
    String quartoManuscriptPageCaption();

   

    @DefaultMessage("Quarto Project")
    @Key("quartoProjectTitle")
    String quartoProjectTitle();

    @DefaultMessage("Create a new Quarto project")
    @Key("quartoProjectSubTitle")
    String quartoProjectSubTitle();

    @DefaultMessage("Create Quarto Project")
    @Key("quartoProjectPageCaption")
    String quartoProjectPageCaption();

    @DefaultMessage("Type:")
    @Key("typeText")
    String typeText();

    @DefaultMessage("(Default)")
    @Key("projectTypeDefault")
    String projectTypeDefault();

    @DefaultMessage("Website")
    @Key("projectTypeWebsite")
    String projectTypeWebsite();

    @DefaultMessage("Book")
    @Key("projectTypeBook")
    String projectTypeBook();
    

    @DefaultMessage("Manuscript")
    @Key("projectTypeManuscript")
    String projectTypeManuscript();

    @DefaultMessage("Engine:")
    @Key("engineLabel")
    String engineLabel();

    @DefaultMessage("(None)")
    @Key("engineSelectNone")
    String engineSelectNone();

    @DefaultMessage("Kernel:")
    @Key("kernelLabel")
    String kernelLabel();

    @DefaultMessage("Use venv with packages: ")
    @Key("chkUseVenvLabel")
    String chkUseVenvLabel();

    @DefaultMessage("(none)")
    @Key("txtVenvPackagesNone")
    String txtVenvPackagesNone();

    @DefaultMessage("type")
    @Key("quartoProjectTypeOption")
    String quartoProjectTypeOption();

    @DefaultMessage("engine")
    @Key("quartoProjectEngineOption")
    String quartoProjectEngineOption();

    @DefaultMessage("kernel")
    @Key("quartoProjectKernelOption")
    String quartoProjectKernelOption();

    @DefaultMessage("Quarto Blog")
    @Key("quartoBlogTitle")
    String quartoBlogTitle();

    @DefaultMessage("Create a new Quarto blog project")
    @Key("quartoBlogSubTitle")
    String quartoBlogSubTitle();

    @DefaultMessage("Create Quarto Blog")
    @Key("quartoBlogPageCaption")
    String quartoBlogPageCaption();

    @DefaultMessage("Quarto Website")
    @Key("quartoWebsiteTitle")
    String quartoWebsiteTitle();

    @DefaultMessage("Create a new Quarto website project")
    @Key("quartoWebsiteSubTitle")
    String quartoWebsiteSubTitle();

    @DefaultMessage("Create Quarto Website")
    @Key("quartoWebsitePageCaption")
    String quartoWebsitePageCaption();

    @DefaultMessage("Shiny Application")
    @Key("shinyApplicationTitle")
    String shinyApplicationTitle();

    @DefaultMessage("Create a new Shiny application")
    @Key("shinyApplicationSubTitle")
    String shinyApplicationSubTitle();

    @DefaultMessage("Create Shiny Application")
    @Key("shinyApplicationPageCaption")
    String shinyApplicationPageCaption();

    @DefaultMessage("Subversion")
    @Key("svnPageTitle")
    String svnPageTitle();

    @DefaultMessage("Checkout a project from a Subversion repository")
    @Key("svnPageSubTitle")
    String svnPageSubTitle();

    @DefaultMessage("Checkout Subversion Repository")
    @Key("svnPagePageCaption")
    String svnPagePageCaption();

    @DefaultMessage("Version Control")
    @Key("versionControlTitle")
    String versionControlTitle();

    @DefaultMessage("Checkout a project from a version control repository")
    @Key("versionControlSubTitle")
    String versionControlSubTitle();

    @DefaultMessage("Create Project from Version Control")
    @Key("versionControlPageCaption")
    String versionControlPageCaption();

    @DefaultMessage("<p>{0} was not detected on the system path.</p><p>To create projects from {0} repositories you should install {0} and then restart RStudio.</p><p>Note that if {0} is installed and not on the path, then you can specify its location using the {1} dialog.</p>")
    @Key("acceptNavigationHTML")
    String acceptNavigationHTML(String title, String location);

    @DefaultMessage("Options")
    @Key("optionsLabel")
    String optionsLabel();

    @DefaultMessage("Preferences")
    @Key("preferencesLabel")
    String preferencesLabel();

    @DefaultMessage("Using {0} with RStudio")
    @Key("vcsHelpLink")
    String vcsHelpLink(String title);

    @DefaultMessage("<p>An installation of {0} was not detected on this system.</p><p>To create projects from {0} repositories you should request that your server administrator install the {0} package.</p>")
    @Key("installationNotDetectedHTML")
    String installtionNotDetectedHTML(String title);

    @DefaultMessage("{0} Not Found")
    @Key("titleNotFound")
    String titleNotFound(String title);

    @DefaultMessage("OK")
    @Key("okLabel")
    String okLabel();

    @DefaultMessage("Repository URL:")
    @Key("repoURLLabel")
    String repoURLLabel();

    @DefaultMessage("Username (if required for this repository URL):")
    @Key("usernameLabel")
    String usernameLabel();

    @DefaultMessage("Project directory name:")
    @Key("projDirNameLabel")
    String projDirNameLabel();

    @DefaultMessage("Create project as subdirectory of:")
    @Key("existingRepoDestDirLabel")
    String existingRepoDestDirLabel();

    @DefaultMessage("You must specify a repository URL and directory to create the new project within.")
    @Key("specifyRepoURLErrorMessage")
    String specifyRepoURLErrorMessage();

    @DefaultMessage("PDF Generation")
    @Key("pdfGenerationCaption")
    String pdfGenerationCaption();

    @DefaultMessage("PDF Preview")
    @Key("pdfPreviewCaption")
    String pdfPreviewCaption();

    @DefaultMessage("Compile PDF root document:")
    @Key("compilePDFLabel")
    String compilePDFLabel();

    @DefaultMessage("(Current Document)")
    @Key("compilePDFEmptyLabel")
    String compilePDFEmptyLabel();

    @DefaultMessage("Browse...")
    @Key("browseActionLabel")
    String browseActionLabel();

    @DefaultMessage("Get help on Compile PDF root document")
    @Key("rootDocumentChooserTitle")
    String rootDocumentChooserTitle();

    @DefaultMessage("Choose File")
    @Key("chooseFileCaption")
    String chooseFileCaption();

    @DefaultMessage("Index source files (for code search/navigation)")
    @Key("enableCodeIndexingLabel")
    String enableCodeIndexingLabel();

    @DefaultMessage("Insert spaces for tab")
    @Key("chkSpacesForTabLabel")
    String chkSpacesForTabLabel();

    @DefaultMessage("Tab width")
    @Key("tabWidthLabel")
    String tabWidthLabel();

    @DefaultMessage("Use native pipe operator, |> (requires R 4.1+)")
    @Key("useNativePipeOperatorLabel")
    String useNativePipeOperatorLabel();

    @DefaultMessage("Ensure that source files end with newline")
    @Key("chkAutoAppendNewlineLabel")
    String chkAutoAppendNewlineLabel();

    @DefaultMessage("Strip trailing horizontal whitespace when saving")
    @Key("chkStripTrailingWhitespaceLabel")
    String chkStripTrailingWhitespaceLabel();

    @DefaultMessage("Text encoding:")
    @Key("textEncodingLabel")
    String textEncodingLabel();

    @DefaultMessage("Change...")
    @Key("changeLabel")
    String changeLabel();

    @DefaultMessage("Code Editing")
    @Key("codingEditingLabel")
    String codingEditingLabel();

    @DefaultMessage("Use (Default) to inherit the global default setting.")
    @Key("projectGeneralInfoLabel")
    String projectGeneralInfoLabel();

    @DefaultMessage("Restore .RData into workspace at startup")
    @Key("restoreWorkspaceText")
    String restoreWorkspaceText();

    @DefaultMessage("Save workspace to .RData on exit")
    @Key("saveWorkspaceText")
    String saveWorkspaceText();

    @DefaultMessage("Always save history (even if not saving .RData)")
    @Key("alwaysSaveHistoryText")
    String alwaysSaveHistoryText();

    @DefaultMessage("Disable .Rprofile execution on session start/resume")
    @Key("disableExecuteRprofileText")
    String disableExecuteRprofileText();

    @DefaultMessage("Quit child processes on exit")
    @Key("quitChildProcessesOnExitText")
    String quitChildProcessesOnExitText();

    @DefaultMessage("General")
    @Key("generalText")
    String generalText();

    @DefaultMessage("Packrat is a dependency management tool that makes your R code more isolated, portable, and reproducible by giving your project its own privately managed package library.")
    @Key("initializePackratMessage")
    String initializePackratMessage();

    @DefaultMessage("Use packrat with this project")
    @Key("chkUsePackratLabel")
    String chkUsePackratLabel();

    @DefaultMessage("Automatically snapshot local changes")
    @Key("chkAutoSnapshotLabel")
    String chkAutoSnapshotLabel();

    @DefaultMessage("{0} ignore packrat library")
    @Key("chkVcsIgnoreLibLabel")
    String chkVcsIgnoreLibLabel(String vcsName);

    @DefaultMessage("{0} ignore packrat sources")
    @Key("chkVcsIgnoreSrcLabel")
    String chkVcsIgnoreSrcLabel(String vcsName);

    @DefaultMessage("External packages (comma separated):")
    @Key("panelExternalPackagesText")
    String panelExternalPackagesText();

    @DefaultMessage("Help on external packages")
    @Key("panelExternalPackagesTitle")
    String panelExternalPackagesTitle();

    @DefaultMessage("Learn more about Packrat")
    @Key("packratHelpLink")
    String packratHelpLink();

    @DefaultMessage("Verifying prerequisites...")
    @Key("verifyPrerequisitesLabel")
    String verifyPrerequisitesLabel();

    @DefaultMessage("Managing packages with packrat")
    @Key("packratManagePackages")
    String packratManagePackages();

    @DefaultMessage("Project Options")
    @Key("projectOptionsCaption")
    String projectOptionsCaption();

    @DefaultMessage("Confirm Restart RStudio")
    @Key("restartRStudioCaption")
    String restartRStudioCaption();

    @DefaultMessage("You need to restart RStudio in order for this change to take effect. Do you want to do this now?")
    @Key("restartRStudioMessage")
    String restartRStudioMessage();

    @DefaultMessage("(Use default)")
    @Key("useDefaultText")
    String useDefaultText();

    @DefaultMessage("Environments")
    @Key("environmentsText")
    String environmentsText();

    @DefaultMessage("RStudio uses the renv package to give your projects their own privately-managed package library, making your R code more isolated, portable, and reproducible.")
    @Key("rstudioInitializeLabel")
    String rstudioInitializeLabel();

    @DefaultMessage("Learn more about renv")
    @Key("renvHelpLink")
    String renvHelpLink();

    @DefaultMessage("Visual Mode: Markdown Output")
    @Key("visualModeCaption")
    String visualModeCaption();

    @DefaultMessage("Use (Default) to inherit the global default setting.")
    @Key("rMarkdownInfoLabel")
    String rMarkdownInfoLabel();

    @DefaultMessage("Wrap at column:")
    @Key("wrapColumnLabel")
    String wrapColumnLabel();

    @DefaultMessage("Automatic text wrapping (line breaks)")
    @Key("wrapPanelText")
    String wrapPanelText();

    @DefaultMessage("(Default)")
    @Key("referencesDefaultItem")
    String referencesDefaultItem();

    @DefaultMessage("Write references at end of current")
    @Key("referencesFormLabel")
    String referencesFormLabel();

    @DefaultMessage("(Default)")
    @Key("canonicalDefaultItem")
    String canonicalDefaultItem();

    @DefaultMessage("true")
    @Key("canonicalTrueItem")
    String canonicalTrueItem();

    @DefaultMessage("false")
    @Key("canonicalFalseItem")
    String canonicalFalseItem();

    @DefaultMessage("Write canonical visual mode markdown in source mode")
    @Key("canonicalFormLabel")
    String canonicalFormLabel();

    @DefaultMessage("Learn more about markdown writer options")
    @Key("markdownPerFileOptionsCaption")
    String markdownPerFileOptionsCaption();

    @DefaultMessage("Visual Mode: Zotero")
    @Key("visualModeZoteroCaption")
    String visualModeZoteroCaption();

    @DefaultMessage("R Markdown")
    @Key("rMarkdownText")
    String rMarkdownText();

    @DefaultMessage("Sharing")
    @Key("sharingText")
    String sharingText();

    @DefaultMessage("Version control system:")
    @Key("vcsSelectLabel")
    String vcsSelectLabel();

    @DefaultMessage("Origin: ")
    @Key("originLabel")
    String originLabel();

    @DefaultMessage("Origin:")
    @Key("lblOrigin")
    String lblOrigin();

    @DefaultMessage("Repo:")
    @Key("repoCaption")
    String repoCaption();

    @DefaultMessage("Checking for git repository...")
    @Key("confirmGitRepoLabel")
    String confirmGitRepoLabel();

    @DefaultMessage("Confirm New Git Repository")
    @Key("confirmGitRepoCaption")
    String confirmGitRepoCaption();

    @DefaultMessage("Do you want to initialize a new git repository for this project?")
    @Key("confirmGitRepoMessage")
    String confirmGitRepoMessage();

    @DefaultMessage("(None)")
    @Key("noneProjectSourceControlLabel")
    String noneProjectSourceControlLabel();

    @DefaultMessage("Dictionaries")
    @Key("dictionariesCaption")
    String dictionariesCaption();

    @DefaultMessage("Use (Default) to inherit the global default dictionary.")
    @Key("dictionariesInfoLabel")
    String dictionariesInfoLabel();

    @DefaultMessage("Spelling")
    @Key("spellingText")
    String spellingText();

    @DefaultMessage("Yes")
    @Key("yesLabel")
    String yesLabel();

    @DefaultMessage("No")
    @Key("noLabel")
    String noLabel();

    @DefaultMessage("Ask")
    @Key("askLabel")
    String askLabel();

    @DefaultMessage("Custom build script:")
    @Key("pathSelectorLabel")
    String pathSelectorLabel();

    @DefaultMessage("Script Not Specified")
    @Key("validateScriptCaption")
    String validateScriptCaption();

    @DefaultMessage("You must specify a path to the custom build script.")
    @Key("validateScriptMessage")
    String validateScriptMessage();

    @DefaultMessage("Makefile directory:")
    @Key("pathSelectorMakefileDirLabel")
    String pathSelectorMakefileDirLabel();

    @DefaultMessage("Additional arguments:")
    @Key("txtMakefileArgs")
    String txtMakefileArgs();

    @DefaultMessage("Package directory:")
    @Key("pathSelectorPackageDir")
    String pathSelectorPackageDir();

    @DefaultMessage("Use devtools package functions if available")
    @Key("chkUseDevtoolsCaption")
    String chkUseDevtoolsCaption();

    @DefaultMessage("Always use --preclean when installing package")
    @Key("cleanBeforeInstallLabel")
    String cleanBeforeInstallLabel();

    @DefaultMessage("Generate documentation with Roxygen")
    @Key("chkUseRoxygenCaption")
    String chkUseRoxygenCaption();

    @DefaultMessage("Configure...")
    @Key("btnConfigureRoxygenLabel")
    String btnConfigureRoxygenLabel();

    @DefaultMessage("Install Package &mdash; R CMD INSTALL additional options:")
    @Key("installMdashArgument")
    String installMdashArgument();

    @DefaultMessage("Check Package &mdash; R CMD check additional options:")
    @Key("checkPackageMdashArgument")
    String checkPackageMdashArgument();

    @DefaultMessage("Build Source Package &mdash; R CMD build additional options:")
    @Key("buildSourceMdashArgument")
    String buildSourceMdashArgument();

    @DefaultMessage("Build Binary Package &mdash; R CMD INSTALL additional options:")
    @Key("buildBinaryMdashArgument")
    String buildBinaryMdashArgument();

    @DefaultMessage("Browse...")
    @Key("browseLabel")
    String browseLabel();

    @DefaultMessage("(Project Root)")
    @Key("projectRootLabel")
    String projectRootLabel();

    @DefaultMessage("Choose Directory")
    @Key("chooseDirectoryCaption")
    String chooseDirectoryCaption();

    @DefaultMessage("(None)")
    @Key("noneFileSelectorLabel")
    String noneFileSelectorLabel();

    @DefaultMessage("Roxygen Options")
    @Key("roxygenOptionsCaption")
    String roxygenOptionsCaption();

    @DefaultMessage("Site directory:")
    @Key("pathSelectorSiteDir")
    String pathSelectorSiteDir();

    @DefaultMessage("Book output format(s):")
    @Key("websiteOutputFormatLabel")
    String websiteOutputFormatLabel();

    @DefaultMessage("all")
    @Key("allLabel")
    String allLabel();

    @DefaultMessage("Preview site after building")
    @Key("chkPreviewAfterBuildingCaption")
    String chkPreviewAfterBuildingCaption();

    @DefaultMessage("Re-knit current preview when supporting files change")
    @Key("chkLivePreviewSiteCaption")
    String chkLivePreviewSiteCaption();

    @DefaultMessage("Supporting files include Rmd partials, R scripts, YAML config files, etc.")
    @Key("infoLabel")
    String infoLabel();

    @DefaultMessage("Preview book after building")
    @Key("chkPreviewAfterBuilding")
    String chkPreviewAfterBuilding();

    @DefaultMessage("(All Formats)")
    @Key("allFormatsLabel")
    String allFormatsLabel();

    @DefaultMessage("Build Tools")
    @Key("buildToolsLabel")
    String buildToolsLabel();

    @DefaultMessage("Project build tools:")
    @Key("projectBuildToolsLabel")
    String projectBuildToolsLabel();

    @DefaultMessage("Package")
    @Key("packageLabel")
    String packageLabel();

    @DefaultMessage("Git/SVN")
    @Key("gitLabel")
    String gitLabel();

    @DefaultMessage("Close Project")
    @Key("closeProjectLabel")
    String closeProjectLabel();

    @DefaultMessage("placeholder")
    @Key("placeholderLabel")
    String placeholderLabel();

    @DefaultMessage("Use global cache for installed packages")
    @Key("chkUseCacheLabel")
    String chkUseCacheLabel();

    @DefaultMessage("Use condaenv with packages:")
    @Key("useCondaenv")
    String useCondaenv();
    

    @DefaultMessage("Editing")
    @Key("editingTitle")
    String editingTitle();
 

    @DefaultMessage("Indexing")
    @Key("indexingTitle")
    String indexingTitle();
    

    @DefaultMessage("Saving")
    @Key("savingTitle")
    String savingTitle();
    

    @DefaultMessage("Workspace")
    @Key("workspaceTitle")
    String workspaceTitle();
    

    @DefaultMessage("Miscellaneous")
    @Key("miscellaneousTitle")
    String miscellaneousTitle();

    @DefaultMessage("General")
    @Key("generalTitle")
    String generalTitle();
    
    

    @DefaultMessage("Project display name (defaults to folder name):")
    @Key("customProjectNameLabel")
    String customProjectNameLabel();
    

    @DefaultMessage("Project scratch path:")
    @Key("scratchPathLabel")
    String scratchPathLabel();
    
}
