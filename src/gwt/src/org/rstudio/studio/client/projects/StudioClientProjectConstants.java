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

    /**
     * Translated "Open project in a new R session".
     *
     * @return translated "Open project in a new R session"
     */
    String openProjectLabel();

    /**
     * Translated "Open Project".
     *
     * @return translated "Open Project"
     */
    String openProjectCaption();

    /**
     * Translated "R Projects (*.Rproj)".
     *
     * @return translated "R Projects (*.Rproj)"
     */
    String projectFilter();

    /**
     * Translated "Show _{0}".
     *
     * @return translated "Show _{0}"
     */
    String showVCSMenuLabel(String vcsName);

    /**
     * Translated "Zoom _{0}".
     *
     * @return translated "Zoom _{0}"
     */
    String zoomVCSMenuLabel(String vcsName);

    /**
     * Translated "Update".
     *
     * @return translated "Update"
     */
    String updateButtonLabel();

    /**
     * Translated "_Update".
     *
     * @return translated "_Update"
     */
    String updateMenuLabel();

    /**
     * Translated "Save Current Workspace".
     *
     * @return translated "Save Current Workspace"
     */
    String newProjectCaption();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String errorCaption();

    /**
     * Translated "New Project...".
     *
     * @return translated "New Project..."
     */
    String newProjectProjectIndicator();

    /**
     * Translated "Error Creating Project".
     *
     * @return translated "Error Creating Project"
     */
    String creatingProjectError();

    /**
     * Translated "Saving defaults...".
     *
     * @return translated "Saving defaults..."
     */
    String savingDefaultsLabel();

    /**
     * Translated "Checking out SVN repository...".
     *
     * @return translated "Checking out SVN repository..."
     */
    String cloneSVNRepoLabel();

    /**
     * Translated "Cloning Git repository...".
     *
     * @return translated "Cloning Git repository..."
     */
    String cloneGitRepoLabel();

    /**
     * Translated "vcsClone failed".
     *
     * @return translated "vcsClone failed"
     */
    String vcsCloneFailMessage();

    /**
     * Translated "Invalid package name ''{0}'': package names must start with a letter, and contain only letters and numbers.".
     *
     * @return translated "Invalid package name ''{0}'': package names must start with a letter, and contain only letters and numbers."
     */
    String invalidPackageMessage(String packageName);

    /**
     * Translated "Invalid package name {0}".
     *
     * @return translated "Invalid package name {0}"
     */
    String invalidPackageName(String packageName);

    /**
     * Translated "Creating project...".
     *
     * @return translated "Creating project..."
     */
    String creatingProjectLabel();

    /**
     * Translated "Quarto create project failed".
     *
     * @return translated "Quarto create project failed"
     */
    String projectFailedMessage();

    /**
     * Translated "Creating project".
     *
     * @return translated "Creating project"
     */
    String creatingProjectCaption();

    /**
     * Translated "Creating a project with {0}".
     *
     * @return translated "Creating a project with {0}"
     */
    String creatingProjectWithLabel(String pkg);

    /**
     * Translated "{0} Project".
     *
     * @return translated "{0} Project"
     */
    String projectContext(String pkg);

    /**
     * Translated "Error installing {0}".
     *
     * @return translated "Error installing {0}"
     */
    String errorInstallingCaption(String pkg);

    /**
     * Translated "Installation of package ''{0}'' failed, and so the project cannot be created. Try installing the package manually with ''install.packages("{0}")''.".
     *
     * @return translated "Installation of package ''{0}'' failed, and so the project cannot be created. Try installing the package manually with ''install.packages("{0}")''."
     */
    String errorInstallingCaptionMessage(String pkg);

    /**
     * Translated "creating project".
     *
     * @return translated "creating project"
     */
    String creatingProjectResultMessage();

    /**
     * Translated "Initializing git repository...".
     *
     * @return translated "Initializing git repository..."
     */
    String initializingGitRepoMessage();

    /**
     * Translated "Initializing renv...".
     *
     * @return translated "Initializing renv..."
     */
    String initializingRenvMessage();

    /**
     * Translated "Preparing to open project...".
     *
     * @return translated "Preparing to open project..."
     */
    String executeOpenProjectMessage();

    /**
     * Translated "No Active Project".
     *
     * @return translated "No Active Project"
     */
    String noActiveProjectCaption();

    /**
     * Translated "Build tools can only be configured from within an RStudio project.".
     *
     * @return translated "Build tools can only be configured from within an RStudio project."
     */
    String noActiveProjectMessage();

    /**
     * Translated "Version control features can only be accessed from within an RStudio project. Note that if you have an existing directory under version control you can associate an RStudio project with that directory using the New Project dialog.".
     *
     * @return translated "Version control features can only be accessed from within an RStudio project. Note that if you have an existing directory under version control you can associate an RStudio project with that directory using the New Project dialog."
     */
    String versionControlProjectSetupMessage();

    /**
     * Translated "Error Reading Options".
     *
     * @return translated "Error Reading Options"
     */
    String errorReadingOptionsCaption();

    /**
     * Translated "Reading options...".
     *
     * @return translated "Reading options..."
     */
    String readingOptionsMessage();

    /**
     * Translated "Confirm Open Project".
     *
     * @return translated "Confirm Open Project"
     */
    String confirmOpenProjectCaption();

    /**
     * Translated "Do you want to open the project {0}?".
     *
     * @return translated "Do you want to open the project {0}?"
     */
    String openProjectPathMessage(String projectPath);

    /**
     * Translated "Project ''{0}'' could not be opened: {1}".
     *
     * @return translated "Project ''{0}'' could not be opened: {1}"
     */
    String openProjectError(String project, String message);

    /**
     * Translated "\n\nEnsure the project URL is correct; if it is, contact the project owner to request access.".
     *
     * @return translated "\n\nEnsure the project URL is correct; if it is, contact the project owner to request access."
     */
    String openProjectErrorMessage();

    /**
     * Translated "Error Opening Project".
     *
     * @return translated "Error Opening Project"
     */
    String errorOpeningProjectCaption();

    /**
     * Translated "Switch Projects".
     *
     * @return translated "Switch Projects"
     */
    String switchProjectsCaption();

    /**
     * Translated "Analyzing project sources...".
     *
     * @return translated "Analyzing project sources..."
     */
    String onShowDiagnosticsProject();

    /**
     * Translated "Project ''{0}'' does not exist (it has been moved or deleted), or it is not writeable".
     *
     * @return translated "Project ''{0}'' does not exist (it has been moved or deleted), or it is not writeable"
     */
    String projectOpenError(String projectFilePath);

    /**
     * Translated "none".
     *
     * @return translated "none"
     */
    String noneLabel();

    /**
     * Translated "Create package based on source files:".
     *
     * @return translated "Create package based on source files:"
     */
    String createPackageFormLabel();

    /**
     * Translated "Add...".
     *
     * @return translated "Add..."
     */
    String addButtonCaption();

    /**
     * Translated "Remove".
     *
     * @return translated "Remove"
     */
    String removeButtonCaption();

    /**
     * Translated "Add Source File".
     *
     * @return translated "Add Source File"
     */
    String addSourceFileCaption();

    /**
     * Translated "Existing Directory".
     *
     * @return translated "Existing Directory"
     */
    String existingDirectoryTitle();

    /**
     * Translated "Associate a project with an existing working directory".
     *
     * @return translated "Associate a project with an existing working directory"
     */
    String existingDirectorySubTitle();

    /**
     * Translated "Create Project from Existing Directory".
     *
     * @return translated "Create Project from Existing Directory"
     */
    String existingDirectoryPageCaption();

    /**
     * Translated "Project working directory:".
     *
     * @return translated "Project working directory:"
     */
    String projectWorkingDirectoryTitle();

    /**
     * Translated "You must specify an existing working directory to create the new project within.".
     *
     * @return translated "You must specify an existing working directory to create the new project within."
     */
    String validateMessage();

    /**
     * Translated "Your home directory cannot be treated as an RStudio Project; select a different directory.".
     *
     * @return translated "Your home directory cannot be treated as an RStudio Project; select a different directory."
     */
    String homeDirectoryErrorMessage();


    /**
     * Translated "Clone a project from a Git repository".
     *
     * @return translated "Clone a project from a Git repository"
     */
    String cloneGitRepo();

    /**
     * Translated "Clone Git Repository".
     *
     * @return translated "Clone Git Repository"
     */
    String cloneGitRepoPageCaption();

    /**
     * Translated "New Directory".
     *
     * @return translated "New Directory"
     */
    String newDirectoryTitle();

    /**
     * Translated "Start a project in a brand new working directory".
     *
     * @return translated "Start a project in a brand new working directory"
     */
    String newDirectorySubTitle();

    /**
     * Translated "Project Type".
     *
     * @return translated "Project Type"
     */
    String newDirectoryPageCaption();

    /**
     * Translated "title".
     *
     * @return translated "title"
     */
    String titleName();

    /**
     * Translated "New Project".
     *
     * @return translated "New Project"
     */
    String newProjectTitle();

    /**
     * Translated "Create a new project in an empty directory".
     *
     * @return translated "Create a new project in an empty directory"
     */
    String newProjectSubTitle();

    /**
     * Translated "Create New Project".
     *
     * @return translated "Create New Project"
     */
    String createNewProjectPageCaption();

    /**
     * Translated "Create project as subdirectory of:".
     *
     * @return translated "Create project as subdirectory of:"
     */
    String newProjectParentLabel();

    /**
     * Translated "Create a git repository".
     *
     * @return translated "Create a git repository"
     */
    String createGitRepoLabel();

    /**
     * Translated "Use renv with this project".
     *
     * @return translated "Use renv with this project"
     */
    String chkRenvInitLabel();

    /**
     * Translated "Using renv".
     *
     * @return translated "Using renv"
     */
    String chkRenvInitUserAction();

    /**
     * Translated "Directory name:".
     *
     * @return translated "Directory name:"
     */
    String directoryNameLabel();

    /**
     * Translated "You must specify a name for the new project directory.".
     *
     * @return translated "You must specify a name for the new project directory."
     */
    String specifyProjectDirectoryName();

    /**
     * Translated "New Project Wizard".
     *
     * @return translated "New Project Wizard"
     */
    String newProjectWizardCaption();

    /**
     * Translated "Create Project".
     *
     * @return translated "Create Project"
     */
    String createProjectCaption();

    /**
     * Translated "Open in new session".
     *
     * @return translated "Open in new session"
     */
    String openNewSessionLabel();

    /**
     * Translated "Create project from:".
     *
     * @return translated "Create project from:"
     */
    String createProjectFromLabel();

    /**
     * Translated "R Package".
     *
     * @return translated "R Package"
     */
    String newPackageTitle();

    /**
     * Translated "Create a new R package".
     *
     * @return translated "Create a new R package"
     */
    String createNewPackageSubTitle();

    /**
     * Translated "Create R Package".
     *
     * @return translated "Create R Package"
     */
    String createRPackagePageCaption();

    /**
     * Translated "Type:".
     *
     * @return translated "Type:"
     */
    String typeLabel();

    /**
     * Translated "Package name:".
     *
     * @return translated "Package name:"
     */
    String packageNameLabel();

    /**
     * Translated "Package name:".
     *
     * @return translated "Package w/ Rcpp"
     */
    String rcppPackageOption();

    /**
     * Translated "Invalid package name ''{0}''. Package names should start with a letter, and contain only letters and numbers.".
     *
     * @return translated "Invalid package name ''{0}''. Package names should start with a letter, and contain only letters and numbers."
     */
    String validateAsyncMessage(String packageName);

    /**
     * Translated "A file already exists at path ''{0}''.".
     *
     * @return translated "A file already exists at path ''{0}''."
     */
    String fileAlreadyExistsMessage(String path);

    /**
     * Translated "Directory ''{0}'' already exists and is not empty.".
     *
     * @return translated "Directory ''{0}'' already exists and is not empty."
     */
    String directoryAlreadyExistsMessage(String path);

    /**
     * Translated "Please enter a new directory name.".
     *
     * @return translated "Please enter a new directory name."
     */
    String pleaseEnterDirectoryNameMessage();

    /**
     * Translated "Quarto Book".
     *
     * @return translated "Quarto Book"
     */
    String quartoBookTitle();

    /**
     * Translated "Create a new Quarto book project".
     *
     * @return translated "Create a new Quarto book project"
     */
    String quartoBookSubTitle();

    /**
     * Translated "Create Quarto Book".
     *
     * @return translated "Create Quarto Book"
     */
    String quartoBookPageCaption();

    /**
     * Translated "Quarto Manuscript".
     *
     * @return translated "Quarto Manuscript"
     */
    String quartoManuscriptTitle();

    /**
     * Translated "Create a new Quarto manuscript project".
     *
     * @return translated "Create a new Quarto manuscript project"
     */
    String quartoManuscriptSubTitle();

    /**
     * Translated "Create Quarto Manuscript".
     *
     * @return translated "Create Quarto Manuscript"
     */
    String quartoManuscriptPageCaption();

   
    /**
     * Translated "Quarto Project".
     *
     * @return translated "Quarto Project"
     */
    String quartoProjectTitle();

    /**
     * Translated "Create a new Quarto project".
     *
     * @return translated "Create a new Quarto project"
     */
    String quartoProjectSubTitle();

    /**
     * Translated "Create Quarto Project".
     *
     * @return translated "Create Quarto Project"
     */
    String quartoProjectPageCaption();

    /**
     * Translated "Type:".
     *
     * @return translated "Type:"
     */
    String typeText();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    String projectTypeDefault();

    /**
     * Translated "Website".
     *
     * @return translated "Website"
     */
    String projectTypeWebsite();

    /**
     * Translated "Book".
     *
     * @return translated "Book"
     */
    String projectTypeBook();
    
    /**
     * Translated "Manuscript".
     *
     * @return translated "Manuscript"
     */
    String projectTypeManuscript();

    /**
     * Translated "Engine:".
     *
     * @return translated "Engine:"
     */
    String engineLabel();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    String engineSelectNone();

    /**
     * Translated "Kernel:".
     *
     * @return translated "Kernel:"
     */
    String kernelLabel();

    /**
     * Translated "Use venv with packages: ".
     *
     * @return translated "Use venv with packages: "
     */
    String chkUseVenvLabel();

    /**
     * Translated "(none)".
     *
     * @return translated "(none)"
     */
    String txtVenvPackagesNone();

    /**
     * Translated "type".
     *
     * @return translated "type"
     */
    String quartoProjectTypeOption();

    /**
     * Translated "engine".
     *
     * @return translated "engine"
     */
    String quartoProjectEngineOption();

    /**
     * Translated "kernel".
     *
     * @return translated "kernel"
     */
    String quartoProjectKernelOption();

    /**
     * Translated "Quarto Blog".
     *
     * @return translated "Quarto Blog"
     */
    String quartoBlogTitle();

    /**
     * Translated "Create a new Quarto blog project".
     *
     * @return translated "Create a new Quarto blog project"
     */
    String quartoBlogSubTitle();

    /**
     * Translated "Create Quarto Blog".
     *
     * @return translated "Create Quarto Blog"
     */
    String quartoBlogPageCaption();

    /**
     * Translated "Quarto Website".
     *
     * @return translated "Quarto Website"
     */
    String quartoWebsiteTitle();

    /**
     * Translated "Create a new Quarto website project".
     *
     * @return translated "Create a new Quarto website project"
     */
    String quartoWebsiteSubTitle();

    /**
     * Translated "Create Quarto Website".
     *
     * @return translated "Create Quarto Website"
     */
    String quartoWebsitePageCaption();

    /**
     * Translated "Shiny Application".
     *
     * @return translated "Shiny Application"
     */
    String shinyApplicationTitle();

    /**
     * Translated "Create a new Shiny application".
     *
     * @return translated "Create a new Shiny application"
     */
    String shinyApplicationSubTitle();

    /**
     * Translated "Create Shiny Application".
     *
     * @return translated "Create Shiny Application"
     */
    String shinyApplicationPageCaption();

    /**
     * Translated "Subversion".
     *
     * @return translated "Subversion"
     */
    String svnPageTitle();

    /**
     * Translated "Checkout a project from a Subversion repository".
     *
     * @return translated "Checkout a project from a Subversion repository"
     */
    String svnPageSubTitle();

    /**
     * Translated "Checkout Subversion Repository".
     *
     * @return translated "Checkout Subversion Repository"
     */
    String svnPagePageCaption();

    /**
     * Translated "Version Control".
     *
     * @return translated "Version Control"
     */
    String versionControlTitle();

    /**
     * Translated "Checkout a project from a version control repository".
     *
     * @return translated "Checkout a project from a version control repository"
     */
    String versionControlSubTitle();

    /**
     * Translated "Create Project from Version Control".
     *
     * @return translated "Create Project from Version Control"
     */
    String versionControlPageCaption();

    /**
     * Translated "<p>{0} was not detected on the system path.</p><p>To create projects from {0} repositories you should install {0} and then restart RStudio.</p><p>Note that if {0} is installed and not on the path, then you can specify its location using the {1} dialog.</p>".
     *
     * @return translated "<p>{0} was not detected on the system path.</p><p>To create projects from {0} repositories you should install {0} and then restart RStudio.</p><p>Note that if {0} is installed and not on the path, then you can specify its location using the {1} dialog.</p>"
     */
    String acceptNavigationHTML(String title, String location);

    /**
     * Translated "Options".
     *
     * @return translated "Options"
     */
    String optionsLabel();

    /**
     * Translated "Preferences".
     *
     * @return translated "Preferences"
     */
    String preferencesLabel();

    /**
     * Translated "Using {0} with RStudio".
     *
     * @return translated "Using {0} with RStudio"
     */
    String vcsHelpLink(String title);

    /**
     * Translated "Preferences".
     *
     * @return translated "Preferences"
     */
    String installationNotDetectedHTML(String title);

    /**
     * Translated "{0} Not Found".
     *
     * @return translated "{0} Not Found"
     */
    String titleNotFound(String title);

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    String okLabel();

    /**
     * Translated "Repository URL:".
     *
     * @return translated "Repository URL:"
     */
    String repoURLLabel();

    /**
     * Translated "Username (if required for this repository URL):".
     *
     * @return translated "Username (if required for this repository URL):"
     */
    String usernameLabel();

    /**
     * Translated "Project directory name:".
     *
     * @return translated "Project directory name:"
     */
    String projDirNameLabel();

    /**
     * Translated "Create project as subdirectory of:".
     *
     * @return translated "Create project as subdirectory of:"
     */
    String existingRepoDestDirLabel();

    /**
     * Translated "You must specify a repository URL and directory to create the new project within.".
     *
     * @return translated "You must specify a repository URL and directory to create the new project within."
     */
    String specifyRepoURLErrorMessage();

    /**
     * Translated "PDF Generation".
     *
     * @return translated "PDF Generation"
     */
    String pdfGenerationCaption();

    /**
     * Translated "PDF Generation".
     *
     * @return translated "PDF Preview"
     */
    String pdfPreviewCaption();

    /**
     * Translated "Compile PDF root document:".
     *
     * @return translated "Compile PDF root document:"
     */
    String compilePDFLabel();

    /**
     * Translated "(Current Document)".
     *
     * @return translated "(Current Document)"
     */
    String compilePDFEmptyLabel();

    /**
     * Translated "Browse...".
     *
     * @return translated "Browse..."
     */
    String browseActionLabel();

    /**
     * Translated "Get help on Compile PDF root document".
     *
     * @return translated "Get help on Compile PDF root document"
     */
    String rootDocumentChooserTitle();

    /**
     * Translated "Choose File".
     *
     * @return translated "Choose File"
     */
    String chooseFileCaption();

    /**
     * Translated "Index source files (for code search/navigation)".
     *
     * @return translated "Index source files (for code search/navigation)"
     */
    String enableCodeIndexingLabel();

    /**
     * Translated "Insert spaces for tab".
     *
     * @return translated "Insert spaces for tab"
     */
    String chkSpacesForTabLabel();

    /**
     * Translated "Tab width".
     *
     * @return translated "Tab width"
     */
    String tabWidthLabel();

    /**
     * Translated "Use native pipe operator, |> (requires R 4.1+)"
     *
     * @return translated "Use native pipe operator, |> (requires R 4.1+)"
     */
    String useNativePipeOperatorLabel();

    /**
     * Translated "Ensure that source files end with newline".
     *
     * @return translated "Ensure that source files end with newline"
     */
    String chkAutoAppendNewlineLabel();

    /**
     * Translated "Strip trailing horizontal whitespace when saving".
     *
     * @return translated "Strip trailing horizontal whitespace when saving"
     */
    String chkStripTrailingWhitespaceLabel();

    /**
     * Translated "Text encoding:".
     *
     * @return translated "Text encoding:"
     */
    String textEncodingLabel();

    /**
     * Translated "Change...".
     *
     * @return translated "Change..."
     */
    String changeLabel();

    /**
     * Translated "Code Editing".
     *
     * @return translated "Code Editing"
     */
    String codingEditingLabel();

    /**
     * Translated "Use (Default) to inherit the global default setting".
     *
     * @return translated "Use (Default) to inherit the global default setting."
     */
    String projectGeneralInfoLabel();

    /**
     * Translated "Restore .RData into workspace at startup".
     *
     * @return translated "Restore .RData into workspace at startup"
     */
    String restoreWorkspaceText();

    /**
     * Translated "Save workspace to .RData on exit".
     *
     * @return translated "Save workspace to .RData on exit"
     */
    String saveWorkspaceText();

    /**
     * Translated "Always save history (even if not saving .RData)".
     *
     * @return translated "Always save history (even if not saving .RData)"
     */
    String alwaysSaveHistoryText();

    /**
     * Translated "Disable .Rprofile execution on session start/resume".
     *
     * @return translated "Disable .Rprofile execution on session start/resume"
     */
    String disableExecuteRprofileText();

    /**
     * Translated "Quit child processes on exit".
     *
     * @return translated "Quit child processes on exit"
     */
    String quitChildProcessesOnExitText();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String generalText();

    /**
     * Translated "Packrat is a dependency management tool that makes your R code more isolated, portable, and reproducible by giving your project its own privately managed package library.".
     *
     * @return translated "Packrat is a dependency management tool that makes your R code more isolated, portable, and reproducible by giving your project its own privately managed package library."
     */
    String initializePackratMessage();

    /**
     * Translated "Use packrat with this project".
     *
     * @return translated "Use packrat with this project"
     */
    String chkUsePackratLabel();

    /**
     * Translated "Automatically snapshot local changes".
     *
     * @return translated "Automatically snapshot local changes"
     */
    String chkAutoSnapshotLabel();

    /**
     * Translated "{0} ignore packrat library".
     *
     * @return translated "{0} ignore packrat library"
     */
    String chkVcsIgnoreLibLabel(String vcsName);

    /**
     * Translated "{0} ignore packrat sources".
     *
     * @return translated "{0} ignore packrat sources"
     */
    String chkVcsIgnoreSrcLabel(String vcsName);

    /**
     * Translated "External packages (comma separated):".
     *
     * @return translated "External packages (comma separated):"
     */
    String panelExternalPackagesText();

    /**
     * Translated "Help on external packages".
     *
     * @return translated "Help on external packages"
     */
    String panelExternalPackagesTitle();

    /**
     * Translated "Learn more about Packrat".
     *
     * @return translated "Learn more about Packrat"
     */
    String packratHelpLink();

    /**
     * Translated "Verifying prerequisites...".
     *
     * @return translated "Verifying prerequisites..."
     */
    String verifyPrerequisitesLabel();

    /**
     * Translated "Managing packages with packrat".
     *
     * @return translated "Managing packages with packrat"
     */
    String packratManagePackages();

    /**
     * Translated "Project Options".
     *
     * @return translated "Project Options"
     */
    String projectOptionsCaption();

    /**
     * Translated "Confirm Restart RStudio".
     *
     * @return translated "Confirm Restart RStudio"
     */
    String restartRStudioCaption();

    /**
     * Translated "You need to restart RStudio in order for this change to take effect. Do you want to do this now?".
     *
     * @return translated "You need to restart RStudio in order for this change to take effect. Do you want to do this now?"
     */
    String restartRStudioMessage();

    /**
     * Translated "(Use default)".
     *
     * @return translated "(Use default)"
     */
    String useDefaultText();

    /**
     * Translated "Environments".
     *
     * @return translated "Environments"
     */
    String environmentsText();

    /**
     * Translated "RStudio uses the renv package to give your projects their own privately-managed package library, making your R code more isolated, portable, and reproducible.".
     *
     * @return translated "RStudio uses the renv package to give your projects their own privately-managed package library, making your R code more isolated, portable, and reproducible."
     */
    String rstudioInitializeLabel();

    /**
     * Translated "Learn more about renv".
     *
     * @return translated "Learn more about renv"
     */
    String renvHelpLink();

    /**
     * Translated "Visual Mode: Markdown Output".
     *
     * @return translated "Visual Mode: Markdown Output"
     */
    String visualModeCaption();

    /**
     * Translated "Use (Default) to inherit the global default setting".
     *
     * @return translated "Use (Default) to inherit the global default setting."
     */
    String rMarkdownInfoLabel();

    /**
     * Translated "Wrap at column:".
     *
     * @return translated "Wrap at column:"
     */
    String wrapColumnLabel();

    /**
     * Translated "Automatic text wrapping (line breaks)".
     *
     * @return translated "Automatic text wrapping (line breaks)"
     */
    String wrapPanelText();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    String referencesDefaultItem();

    /**
     * Translated "Write references at end of current".
     *
     * @return translated "Write references at end of current"
     */
    String referencesFormLabel();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    String canonicalDefaultItem();

    /**
     * Translated "true".
     *
     * @return translated "true"
     */
    String canonicalTrueItem();

    /**
     * Translated "false".
     *
     * @return translated "false"
     */
    String canonicalFalseItem();

    /**
     * Translated "Write canonical visual mode markdown in source mode".
     *
     * @return translated "Write canonical visual mode markdown in source mode"
     */
    String canonicalFormLabel();

    /**
     * Translated "Learn more about markdown writer options".
     *
     * @return translated "Learn more about markdown writer options"
     */
    String markdownPerFileOptionsCaption();

    /**
     * Translated "Visual Mode: Zotero".
     *
     * @return translated "Visual Mode: Zotero"
     */
    String visualModeZoteroCaption();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    String rMarkdownText();

    /**
     * Translated "Sharing".
     *
     * @return translated "Sharing"
     */
    String sharingText();

    /**
     * Translated "Version control system:".
     *
     * @return translated "Version control system:"
     */
    String vcsSelectLabel();

    /**
     * Translated "Origin: ".
     *
     * @return translated "Origin: "
     */
    String originLabel();

    /**
     * Translated "Origin:".
     *
     * @return translated "Origin:"
     */
    String lblOrigin();

    /**
     * Translated "Repo:".
     *
     * @return translated "Repo:"
     */
    String repoCaption();

    /**
     * Translated "Checking for git repository...".
     *
     * @return translated "Checking for git repository..."
     */
    String confirmGitRepoLabel();

    /**
     * Translated "Confirm New Git Repository".
     *
     * @return translated "Confirm New Git Repository"
     */
    String confirmGitRepoCaption();

    /**
     * Translated "Do you want to initialize a new git repository for this project?".
     *
     * @return translated "Do you want to initialize a new git repository for this project?"
     */
    String confirmGitRepoMessage();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    String noneProjectSourceControlLabel();

    /**
     * Translated "Dictionaries".
     *
     * @return translated "Dictionaries"
     */
    String dictionariesCaption();

    /**
     * Translated "Use (Default) to inherit the global default dictionary".
     *
     * @return translated "Use (Default) to inherit the global default dictionary."
     */
    String dictionariesInfoLabel();

    /**
     * Translated "Spelling".
     *
     * @return translated "Spelling"
     */
    String spellingText();

    /**
     * Translated "Yes".
     *
     * @return translated "Yes"
     */
    String yesLabel();

    /**
     * Translated "No".
     *
     * @return translated "No"
     */
    String noLabel();

    /**
     * Translated "Ask".
     *
     * @return translated "Ask"
     */
    String askLabel();

    /**
     * Translated "Custom build script:".
     *
     * @return translated "Custom build script:"
     */
    String pathSelectorLabel();

    /**
     * Translated "Script Not Specified".
     *
     * @return translated "Script Not Specified"
     */
    String validateScriptCaption();

    /**
     * Translated "You must specify a path to the custom build script.".
     *
     * @return translated "You must specify a path to the custom build script."
     */
    String validateScriptMessage();

    /**
     * Translated "Makefile directory:".
     *
     * @return translated "Makefile directory:"
     */
    String pathSelectorMakefileDirLabel();

    /**
     * Translated "Additional arguments:".
     *
     * @return translated "Additional arguments:"
     */
    String txtMakefileArgs();

    /**
     * Translated "Package directory:".
     *
     * @return translated "Package directory:"
     */
    String pathSelectorPackageDir();

    /**
     * Translated "Use devtools package functions if available".
     *
     * @return translated "Use devtools package functions if available"
     */
    String chkUseDevtoolsCaption();

    /**
     * Translated "Always use --preclean when installing package".
     *
     * @return translated "Always use --preclean when installing package"
     */
    String cleanBeforeInstallLabel();

    /**
     * Translated "Generate documentation with Roxygen".
     *
     * @return translated "Generate documentation with Roxygen"
     */
    String chkUseRoxygenCaption();

    /**
     * Translated "Configure...".
     *
     * @return translated "Configure..."
     */
    String btnConfigureRoxygenLabel();

    /**
     * Translated "Install Package &mdash; R CMD INSTALL additional options:".
     *
     * @return translated "Install Package &mdash; R CMD INSTALL additional options:"
     */
    String installMdashArgument();

    /**
     * Translated "Check Package &mdash; R CMD check additional options:".
     *
     * @return translated "Check Package &mdash; R CMD check additional options:"
     */
    String checkPackageMdashArgument();

    /**
     * Translated "Check Package &mdash; R CMD check additional options:".
     *
     * @return translated "Check Package &mdash; R CMD check additional options:"
     */
    String buildSourceMdashArgument();

    /**
     * Translated "Build Binary Package &mdash; R CMD INSTALL additional options:".
     *
     * @return translated "Build Binary Package &mdash; R CMD INSTALL additional options:"
     */
    String buildBinaryMdashArgument();

    /**
     * Translated "Browse...".
     *
     * @return translated "Browse..."
     */
    String browseLabel();

    /**
     * Translated "(Project Root)".
     *
     * @return translated "(Project Root)"
     */
    String projectRootLabel();

    /**
     * Translated "Choose Directory".
     *
     * @return translated "Choose Directory"
     */
    String chooseDirectoryCaption();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    String noneFileSelectorLabel();

    /**
     * Translated "Roxygen Options".
     *
     * @return translated "Roxygen Options"
     */
    String roxygenOptionsCaption();

    /**
     * Translated "Site directory:".
     *
     * @return translated "Site directory:"
     */
    String pathSelectorSiteDir();

    /**
     * Translated "Book output format(s):".
     *
     * @return translated "Book output format(s):"
     */
    String websiteOutputFormatLabel();

    /**
     * Translated "all".
     *
     * @return translated "all"
     */
    String allLabel();

    /**
     * Translated "Preview site after building".
     *
     * @return translated "Preview site after building"
     */
    String chkPreviewAfterBuildingCaption();

    /**
     * Translated "Re-knit current preview when supporting files change".
     *
     * @return translated "Re-knit current preview when supporting files change"
     */
    String chkLivePreviewSiteCaption();

    /**
     * Translated "Supporting files include Rmd partials, R scripts, YAML config files, etc.".
     *
     * @return translated "Supporting files include Rmd partials, R scripts, YAML config files, etc."
     */
    String infoLabel();

    /**
     * Translated "Preview book after building".
     *
     * @return translated "Preview book after building"
     */
    String chkPreviewAfterBuilding();

    /**
     * Translated "(All Formats)".
     *
     * @return translated "(All Formats)"
     */
    String allFormatsLabel();

    /**
     * Translated "Build Tools".
     *
     * @return translated "Build Tools"
     */
    String buildToolsLabel();

    /**
     * Translated "Project build tools:".
     *
     * @return translated "Project build tools:"
     */
    String projectBuildToolsLabel();

    /**
     * Translated "Package".
     *
     * @return translated "Package"
     */
    String packageLabel();

    /**
     * Translated "Git/SVN".
     *
     * @return translated "Git/SVN"
     */
    String gitLabel();

    /**
     * Translated "Close Project".
     *
     * @return translated "Close Project"
     */
    String closeProjectLabel();

    /**
     * Translated "placeholder".
     *
     * @return translated "placeholder"
     */
    String placeholderLabel();

    /**
     * Translated "Use global cache for installed packages".
     *
     * @return translated "Use global cache for installed packages"
     */
    String chkUseCacheLabel();

    /**
     * Translated "Use condaenv with packages:".
     *
     * @return translated "Use condaenv with packages:"
     */
    String useCondaenv();
    
    /**
     * Translated "Editing".
     *
     * @return translated "Editing"
     */
    String editingTitle();
 
    /**
     * Translated "Indexing".
     *
     * @return translated "Indexing"
     */
    String indexingTitle();
    
    /**
     * Translated "Saving".
     *
     * @return translated "Saving"
     */
    String savingTitle();
    
    /**
     * Translated "Workspace".
     *
     * @return translated "Workspace"
     */
    String workspaceTitle();
    
    /**
     * Translated "Miscellaneous".
     *
     * @return translated "Miscellaneous"
     */
    String miscellaneousTitle();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String generalTitle();
    
    
    /**
     * Translated "Project display name (defaults to folder name):".
     *
     * @return translated "Project display name (defaults to folder name):"
     */
    String customProjectNameLabel();
    
    /**
     * Translated "Project scratch path:".
     *
     * @return translated "Project scratch path:"
     */
    String scratchPathLabel();
    
}
