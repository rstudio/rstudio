/*
 * StudioClientProjectConstants.java
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

package org.rstudio.studio.client.projects;

public interface StudioClientProjectConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Open project in a new R session".
     *
     * @return translated "Open project in a new R session"
     */
    @DefaultMessage("Open project in a new R session")
    @Key("openProjectLabel")
    String openProjectLabel();

    /**
     * Translated "Open Project".
     *
     * @return translated "Open Project"
     */
    @DefaultMessage("Open Project")
    @Key("openProjectCaption")
    String openProjectCaption();

    /**
     * Translated "R Projects (*.Rproj)".
     *
     * @return translated "R Projects (*.Rproj)"
     */
    @DefaultMessage("R Projects (*.Rproj)")
    @Key("projectFilter")
    String projectFilter();

    /**
     * Translated "Show _{0}".
     *
     * @return translated "Show _{0}"
     */
    @DefaultMessage("Show _{0}")
    @Key("showVCSMenuLabel")
    String showVCSMenuLabel(String vcsName);

    /**
     * Translated "Zoom _{0}".
     *
     * @return translated "Zoom _{0}"
     */
    @DefaultMessage("Zoom _{0}")
    @Key("zoomVCSMenuLabel")
    String zoomVCSMenuLabel(String vcsName);

    /**
     * Translated "Update".
     *
     * @return translated "Update"
     */
    @DefaultMessage("Update")
    @Key("updateButtonLabel")
    String updateButtonLabel();

    /**
     * Translated "_Update".
     *
     * @return translated "_Update"
     */
    @DefaultMessage("_Update")
    @Key("updateMenuLabel")
    String updateMenuLabel();

    /**
     * Translated "Save Current Workspace".
     *
     * @return translated "Save Current Workspace"
     */
    @DefaultMessage("Save Current Workspace")
    @Key("newProjectCaption")
    String newProjectCaption();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "New Project...".
     *
     * @return translated "New Project..."
     */
    @DefaultMessage("New Project...")
    @Key("newProjectProjectIndicator")
    String newProjectProjectIndicator();

    /**
     * Translated "Error Creating Project".
     *
     * @return translated "Error Creating Project"
     */
    @DefaultMessage("Error Creating Project")
    @Key("creatingProjectError")
    String creatingProjectError();

    /**
     * Translated "Saving defaults...".
     *
     * @return translated "Saving defaults..."
     */
    @DefaultMessage("Saving defaults...")
    @Key("savingDefaultsLabel")
    String savingDefaultsLabel();

    /**
     * Translated "Checking out SVN repository...".
     *
     * @return translated "Checking out SVN repository..."
     */
    @DefaultMessage("Checking out SVN repository...")
    @Key("cloneSVNRepoLabel")
    String cloneSVNRepoLabel();

    /**
     * Translated "Cloning Git repository...".
     *
     * @return translated "Cloning Git repository..."
     */
    @DefaultMessage("Cloning Git repository...")
    @Key("cloneGitRepoLabel")
    String cloneGitRepoLabel();

    /**
     * Translated "vcsClone failed".
     *
     * @return translated "vcsClone failed"
     */
    @DefaultMessage("vcsClone failed")
    @Key("vcsCloneFailMessage")
    String vcsCloneFailMessage();

    /**
     * Translated "Invalid package name ''{0}'': package names must start with a letter, and contain only letters and numbers.".
     *
     * @return translated "Invalid package name ''{0}'': package names must start with a letter, and contain only letters and numbers."
     */
    @DefaultMessage("Invalid package name ''{0}'': package names must start with a letter, and contain only letters and numbers.")
    @Key("invalidPackageMessage")
    String invalidPackageMessage(String packageName);

    /**
     * Translated "Invalid package name {0}".
     *
     * @return translated "Invalid package name {0}"
     */
    @DefaultMessage("Invalid package name {0}")
    @Key("invalidPackageName")
    String invalidPackageName(String packageName);

    /**
     * Translated "Creating project...".
     *
     * @return translated "Creating project..."
     */
    @DefaultMessage("Creating project...")
    @Key("creatingProjectLabel")
    String creatingProjectLabel();

    /**
     * Translated "Quarto create project failed".
     *
     * @return translated "Quarto create project failed"
     */
    @DefaultMessage("Quarto create project failed")
    @Key("projectFailedMessage")
    String projectFailedMessage();

    /**
     * Translated "Creating project".
     *
     * @return translated "Creating project"
     */
    @DefaultMessage("Creating project")
    @Key("creatingProjectCaption")
    String creatingProjectCaption();

    /**
     * Translated "Creating a project with {0}".
     *
     * @return translated "Creating a project with {0}"
     */
    @DefaultMessage("Creating a project with {0}")
    @Key("creatingProjectWithLabel")
    String creatingProjectWithLabel(String pkg);

    /**
     * Translated "{0} Project".
     *
     * @return translated "{0} Project"
     */
    @DefaultMessage("{0} Project")
    @Key("projectContext")
    String projectContext(String pkg);

    /**
     * Translated "Error installing {0}".
     *
     * @return translated "Error installing {0}"
     */
    @DefaultMessage("Error installing {0}")
    @Key("errorInstallingCaption")
    String errorInstallingCaption(String pkg);

    /**
     * Translated "Installation of package ''{0}'' failed, and so the project cannot be created. Try installing the package manually with ''install.packages("{0}")''.".
     *
     * @return translated "Installation of package ''{0}'' failed, and so the project cannot be created. Try installing the package manually with ''install.packages("{0}")''."
     */
    @DefaultMessage("Installation of package ''{0}'' failed, and so the project cannot be created. Try installing the package manually with ''install.packages(\"{0}\")''.")
    @Key("errorInstallingCaptionMessage")
    String errorInstallingCaptionMessage(String pkg);

    /**
     * Translated "creating project".
     *
     * @return translated "creating project"
     */
    @DefaultMessage("creating project")
    @Key("creatingProjectResultMessage")
    String creatingProjectResultMessage();

    /**
     * Translated "Initializing git repository...".
     *
     * @return translated "Initializing git repository..."
     */
    @DefaultMessage("Initializing git repository...")
    @Key("initializingGitRepoMessage")
    String initializingGitRepoMessage();

    /**
     * Translated "Initializing renv...".
     *
     * @return translated "Initializing renv..."
     */
    @DefaultMessage("Initializing renv...")
    @Key("initializingRenvMessage")
    String initializingRenvMessage();

    /**
     * Translated "Preparing to open project...".
     *
     * @return translated "Preparing to open project..."
     */
    @DefaultMessage("Preparing to open project...")
    @Key("executeOpenProjectMessage")
    String executeOpenProjectMessage();

    /**
     * Translated "No Active Project".
     *
     * @return translated "No Active Project"
     */
    @DefaultMessage("No Active Project")
    @Key("noActiveProjectCaption")
    String noActiveProjectCaption();

    /**
     * Translated "Build tools can only be configured from within an RStudio project.".
     *
     * @return translated "Build tools can only be configured from within an RStudio project."
     */
    @DefaultMessage("Build tools can only be configured from within an RStudio project.")
    @Key("noActiveProjectMessage")
    String noActiveProjectMessage();

    /**
     * Translated "Version control features can only be accessed from within an RStudio project. Note that if you have an existing directory under version control you can associate an RStudio project with that directory using the New Project dialog.".
     *
     * @return translated "Version control features can only be accessed from within an RStudio project. Note that if you have an existing directory under version control you can associate an RStudio project with that directory using the New Project dialog."
     */
    @DefaultMessage("Version control features can only be accessed from within an RStudio project. Note that if you have an existing directory under version control you can associate an RStudio project with that directory using the New Project dialog.")
    @Key("versionControlProjectSetupMessage")
    String versionControlProjectSetupMessage();

    /**
     * Translated "Error Reading Options".
     *
     * @return translated "Error Reading Options"
     */
    @DefaultMessage("Error Reading Options")
    @Key("errorReadingOptionsCaption")
    String errorReadingOptionsCaption();

    /**
     * Translated "Reading options...".
     *
     * @return translated "Reading options..."
     */
    @DefaultMessage("Reading options...")
    @Key("readingOptionsMessage")
    String readingOptionsMessage();

    /**
     * Translated "Confirm Open Project".
     *
     * @return translated "Confirm Open Project"
     */
    @DefaultMessage("Confirm Open Project")
    @Key("confirmOpenProjectCaption")
    String confirmOpenProjectCaption();

    /**
     * Translated "Do you want to open the project {0}?".
     *
     * @return translated "Do you want to open the project {0}?"
     */
    @DefaultMessage("Do you want to open the project {0}?")
    @Key("openProjectPathMessage")
    String openProjectPathMessage(String projectPath);

    /**
     * Translated "Project ''{0}'' could not be opened: {1}".
     *
     * @return translated "Project ''{0}'' could not be opened: {1}"
     */
    @DefaultMessage("Project ''{0}'' could not be opened: {1}")
    @Key("openProjectError")
    String openProjectError(String project, String message);

    /**
     * Translated "\n\nEnsure the project URL is correct; if it is, contact the project owner to request access.".
     *
     * @return translated "\n\nEnsure the project URL is correct; if it is, contact the project owner to request access."
     */
    @DefaultMessage("\n\nEnsure the project URL is correct; if it is, contact the project owner to request access.")
    @Key("openProjectErrorMessage")
    String openProjectErrorMessage();

    /**
     * Translated "Error Opening Project".
     *
     * @return translated "Error Opening Project"
     */
    @DefaultMessage("Error Opening Project")
    @Key("errorOpeningProjectCaption")
    String errorOpeningProjectCaption();

    /**
     * Translated "Switch Projects".
     *
     * @return translated "Switch Projects"
     */
    @DefaultMessage("Switch Projects")
    @Key("switchProjectsCaption")
    String switchProjectsCaption();

    /**
     * Translated "Analyzing project sources...".
     *
     * @return translated "Analyzing project sources..."
     */
    @DefaultMessage("Analyzing project sources...")
    @Key("onShowDiagnosticsProject")
    String onShowDiagnosticsProject();

    /**
     * Translated "Project ''{0}'' does not exist (it has been moved or deleted), or it is not writeable".
     *
     * @return translated "Project ''{0}'' does not exist (it has been moved or deleted), or it is not writeable"
     */
    @DefaultMessage("Project ''{0}'' does not exist (it has been moved or deleted), or it is not writeable")
    @Key("projectOpenError")
    String projectOpenError(String projectFilePath);

    /**
     * Translated "none".
     *
     * @return translated "none"
     */
    @DefaultMessage("none")
    @Key("noneLabel")
    String noneLabel();

    /**
     * Translated "None".
     *
     * @return translated "None"
     */
    @DefaultMessage("None")
    @Key("noneProjectLabel")
    String noneProjectLabel();

    /**
     * Translated "Package".
     *
     * @return translated "Package"
     */
    @DefaultMessage("Package")
    @Key("buildTypePackageLabel")
    String buildTypePackageLabel();

    /**
     * Translated "Website".
     *
     * @return translated "Website"
     */
    @DefaultMessage("Website")
    @Key("buildTypeWebsiteLabel")
    String buildTypeWebsiteLabel();

    /**
     * Translated "Custom".
     *
     * @return translated "Custom"
     */
    @DefaultMessage("Custom")
    @Key("buildTypeCustomLabel")
    String buildTypeCustomLabel();

    /**
     * Translated "namespace".
     *
     * @return translated "namespace"
     */
    @DefaultMessage("namespace")
    @Key("rOxygenizenamespace")
    String rOxygenizenamespace();

    /**
     * Translated "Default".
     *
     * @return translated "Default"
     */
    @DefaultMessage("Default")
    @Key("defaultLabel")
    String defaultLabel();

    /**
     * Translated "Column".
     *
     * @return translated "Column"
     */
    @DefaultMessage("Column")
    @Key("columnLabel")
    String columnLabel();

    /**
     * Translated "Sentence".
     *
     * @return translated "Sentence"
     */
    @DefaultMessage("Sentence")
    @Key("sentenceLabel")
    String sentenceLabel();

    /**
     * Translated "Block".
     *
     * @return translated "Block"
     */
    @DefaultMessage("Block")
    @Key("blockLabel")
    String blockLabel();

    /**
     * Translated "Section".
     *
     * @return translated "Section"
     */
    @DefaultMessage("Section")
    @Key("sectionLabel")
    String sectionLabel();

    /**
     * Translated "Document".
     *
     * @return translated "Document"
     */
    @DefaultMessage("Document")
    @Key("documentLabel")
    String documentLabel();

    /**
     * Translated "collate".
     *
     * @return translated "collate"
     */
    @DefaultMessage("collate")
    @Key("collateLabel")
    String collateLabel();

    /**
     * Translated "vignette".
     *
     * @return translated "vignette"
     */
    @DefaultMessage("vignette")
    @Key("vignetteLabel")
    String vignetteLabel();

    /**
     * Translated "Create package based on source files:".
     *
     * @return translated "Create package based on source files:"
     */
    @DefaultMessage("Create package based on source files:")
    @Key("createPackageFormLabel")
    String createPackageFormLabel();

    /**
     * Translated "Add...".
     *
     * @return translated "Add..."
     */
    @DefaultMessage("Add...")
    @Key("addButtonCaption")
    String addButtonCaption();

    /**
     * Translated "Remove".
     *
     * @return translated "Remove"
     */
    @DefaultMessage("Remove")
    @Key("removeButtonCaption")
    String removeButtonCaption();

    /**
     * Translated "Add Source File".
     *
     * @return translated "Add Source File"
     */
    @DefaultMessage("Add Source File")
    @Key("addSourceFileCaption")
    String addSourceFileCaption();

    /**
     * Translated "Existing Directory".
     *
     * @return translated "Existing Directory"
     */
    @DefaultMessage("Existing Directory")
    @Key("existingDirectoryTitle")
    String existingDirectoryTitle();

    /**
     * Translated "Associate a project with an existing working directory".
     *
     * @return translated "Associate a project with an existing working directory"
     */
    @DefaultMessage("Associate a project with an existing working directory")
    @Key("existingDirectorySubTitle")
    String existingDirectorySubTitle();

    /**
     * Translated "Create Project from Existing Directory".
     *
     * @return translated "Create Project from Existing Directory"
     */
    @DefaultMessage("Create Project from Existing Directory")
    @Key("existingDirectoryPageCaption")
    String existingDirectoryPageCaption();

    /**
     * Translated "Project working directory:".
     *
     * @return translated "Project working directory:"
     */
    @DefaultMessage("Project working directory:")
    @Key("projectWorkingDirectoryTitle")
    String projectWorkingDirectoryTitle();

    /**
     * Translated "You must specify an existing working directory to create the new project within.".
     *
     * @return translated "You must specify an existing working directory to create the new project within."
     */
    @DefaultMessage("You must specify an existing working directory to create the new project within.")
    @Key("validateMessage")
    String validateMessage();

    /**
     * Translated "Your home directory cannot be treated as an RStudio Project; select a different directory.".
     *
     * @return translated "Your home directory cannot be treated as an RStudio Project; select a different directory."
     */
    @DefaultMessage("Your home directory cannot be treated as an RStudio Project; select a different directory.")
    @Key("homeDirectoryErrorMessage")
    String homeDirectoryErrorMessage();


    /**
     * Translated "Clone a project from a Git repository".
     *
     * @return translated "Clone a project from a Git repository"
     */
    @DefaultMessage("Clone a project from a Git repository")
    @Key("cloneGitRepo")
    String cloneGitRepo();

    /**
     * Translated "Clone Git Repository".
     *
     * @return translated "Clone Git Repository"
     */
    @DefaultMessage("Clone Git Repository")
    @Key("cloneGitRepoPageCaption")
    String cloneGitRepoPageCaption();

    /**
     * Translated "New Directory".
     *
     * @return translated "New Directory"
     */
    @DefaultMessage("New Directory")
    @Key("newDirectoryTitle")
    String newDirectoryTitle();

    /**
     * Translated "Start a project in a brand new working directory".
     *
     * @return translated "Start a project in a brand new working directory"
     */
    @DefaultMessage("Start a project in a brand new working directory")
    @Key("newDirectorySubTitle")
    String newDirectorySubTitle();

    /**
     * Translated "Project Type".
     *
     * @return translated "Project Type"
     */
    @DefaultMessage("Project Type")
    @Key("newDirectoryPageCaption")
    String newDirectoryPageCaption();

    /**
     * Translated "title".
     *
     * @return translated "title"
     */
    @DefaultMessage("title")
    @Key("titleName")
    String titleName();

    /**
     * Translated "New Project".
     *
     * @return translated "New Project"
     */
    @DefaultMessage("New Project")
    @Key("newProjectTitle")
    String newProjectTitle();

    /**
     * Translated "Create a new project in an empty directory".
     *
     * @return translated "Create a new project in an empty directory"
     */
    @DefaultMessage("Create a new project in an empty directory")
    @Key("newProjectSubTitle")
    String newProjectSubTitle();

    /**
     * Translated "Create New Project".
     *
     * @return translated "Create New Project"
     */
    @DefaultMessage("Create New Project")
    @Key("createNewProjectPageCaption")
    String createNewProjectPageCaption();

    /**
     * Translated "Create project as subdirectory of:".
     *
     * @return translated "Create project as subdirectory of:"
     */
    @DefaultMessage("Create project as subdirectory of:")
    @Key("newProjectParentLabel")
    String newProjectParentLabel();

    /**
     * Translated "Create a git repository".
     *
     * @return translated "Create a git repository"
     */
    @DefaultMessage("Create a git repository")
    @Key("createGitRepoLabel")
    String createGitRepoLabel();

    /**
     * Translated "Use renv with this project".
     *
     * @return translated "Use renv with this project"
     */
    @DefaultMessage("Use renv with this project")
    @Key("chkRenvInitLabel")
    String chkRenvInitLabel();

    /**
     * Translated "Using renv".
     *
     * @return translated "Using renv"
     */
    @DefaultMessage("Using renv")
    @Key("chkRenvInitUserAction")
    String chkRenvInitUserAction();

    /**
     * Translated "Directory name:".
     *
     * @return translated "Directory name:"
     */
    @DefaultMessage("Directory name:")
    @Key("directoryNameLabel")
    String directoryNameLabel();

    /**
     * Translated "You must specify a name for the new project directory.".
     *
     * @return translated "You must specify a name for the new project directory."
     */
    @DefaultMessage("You must specify a name for the new project directory.")
    @Key("specifyProjectDirectoryName")
    String specifyProjectDirectoryName();

    /**
     * Translated "New Project Wizard".
     *
     * @return translated "New Project Wizard"
     */
    @DefaultMessage("New Project Wizard")
    @Key("newProjectWizardCaption")
    String newProjectWizardCaption();

    /**
     * Translated "Create Project".
     *
     * @return translated "Create Project"
     */
    @DefaultMessage("Create Project")
    @Key("createProjectCaption")
    String createProjectCaption();

    /**
     * Translated "Open in new session".
     *
     * @return translated "Open in new session"
     */
    @DefaultMessage("Open in new session")
    @Key("openNewSessionLabel")
    String openNewSessionLabel();

    /**
     * Translated "Create project from:".
     *
     * @return translated "Create project from:"
     */
    @DefaultMessage("Create project from:")
    @Key("createProjectFromLabel")
    String createProjectFromLabel();

    /**
     * Translated "R Package".
     *
     * @return translated "R Package"
     */
    @DefaultMessage("R Package")
    @Key("newPackageTitle")
    String newPackageTitle();

    /**
     * Translated "Create a new R package".
     *
     * @return translated "Create a new R package"
     */
    @DefaultMessage("Create a new R package")
    @Key("createNewPackageSubTitle")
    String createNewPackageSubTitle();

    /**
     * Translated "Create R Package".
     *
     * @return translated "Create R Package"
     */
    @DefaultMessage("Create R Package")
    @Key("createRPackagePageCaption")
    String createRPackagePageCaption();

    /**
     * Translated "Type:".
     *
     * @return translated "Type:"
     */
    @DefaultMessage("Type:")
    @Key("typeLabel")
    String typeLabel();

    /**
     * Translated "Package name:".
     *
     * @return translated "Package name:"
     */
    @DefaultMessage("Package name:")
    @Key("packageNameLabel")
    String packageNameLabel();

    /**
     * Translated "Package name:".
     *
     * @return translated "Package w/ Rcpp"
     */
    @DefaultMessage("Package w/ Rcpp")
    @Key("rcppPackageOption")
    String rcppPackageOption();

    /**
     * Translated "Invalid package name ''{0}''. Package names should start with a letter, and contain only letters and numbers.".
     *
     * @return translated "Invalid package name ''{0}''. Package names should start with a letter, and contain only letters and numbers."
     */
    @DefaultMessage("Invalid package name ''{0}''. Package names should start with a letter, and contain only letters and numbers.")
    @Key("validateAsyncMessage")
    String validateAsyncMessage(String packageName);

    /**
     * Translated "A file already exists at path ''{0}''".
     *
     * @return translated "A file already exists at path ''{0}''"
     */
    @DefaultMessage("A file already exists at path ''{0}''")
    @Key("onResponseReceivedErrorMessage")
    String onResponseReceivedErrorMessage(String path);

    /**
     * Translated "Directory ''{0}'' already exists and is not empty.".
     *
     * @return translated "Directory ''{0}'' already exists and is not empty."
     */
    @DefaultMessage("Directory ''{0}'' already exists and is not empty.")
    @Key("directoryAlreadyExistsMessage")
    String directoryAlreadyExistsMessage(String path);

    /**
     * Translated "Quarto Book".
     *
     * @return translated "Quarto Book"
     */
    @DefaultMessage("Quarto Book")
    @Key("quartoBookTitle")
    String quartoBookTitle();

    /**
     * Translated "Create a new Quarto book project".
     *
     * @return translated "Create a new Quarto book project"
     */
    @DefaultMessage("Create a new Quarto book project")
    @Key("quartoBookSubTitle")
    String quartoBookSubTitle();

    /**
     * Translated "Create Quarto Book".
     *
     * @return translated "Create Quarto Book"
     */
    @DefaultMessage("Create Quarto Book")
    @Key("quartoBookPageCaption")
    String quartoBookPageCaption();

    /**
     * Translated "Quarto Project".
     *
     * @return translated "Quarto Project"
     */
    @DefaultMessage("Quarto Project")
    @Key("quartoProjectTitle")
    String quartoProjectTitle();

    /**
     * Translated "Create a new Quarto project".
     *
     * @return translated "Create a new Quarto project"
     */
    @DefaultMessage("Create a new Quarto project")
    @Key("quartoProjectSubTitle")
    String quartoProjectSubTitle();

    /**
     * Translated "Create Quarto Project".
     *
     * @return translated "Create Quarto Project"
     */
    @DefaultMessage("Create Quarto Project")
    @Key("quartoProjectPageCaption")
    String quartoProjectPageCaption();

    /**
     * Translated "Type:".
     *
     * @return translated "Type:"
     */
    @DefaultMessage("Type:")
    @Key("typeText")
    String typeText();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    @DefaultMessage("(Default)")
    @Key("projectTypeDefault")
    String projectTypeDefault();

    /**
     * Translated "Website".
     *
     * @return translated "Website"
     */
    @DefaultMessage("Website")
    @Key("projectTypeWebsite")
    String projectTypeWebsite();

    /**
     * Translated "Book".
     *
     * @return translated "Book"
     */
    @DefaultMessage("Book")
    @Key("projectTypeBook")
    String projectTypeBook();

    /**
     * Translated "Engine:".
     *
     * @return translated "Engine:"
     */
    @DefaultMessage("Engine:")
    @Key("engineLabel")
    String engineLabel();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    @DefaultMessage("(None)")
    @Key("engineSelectNone")
    String engineSelectNone();

    /**
     * Translated "Kernel:".
     *
     * @return translated "Kernel:"
     */
    @DefaultMessage("Kernel:")
    @Key("kernelLabel")
    String kernelLabel();

    /**
     * Translated "Use venv with packages: ".
     *
     * @return translated "Use venv with packages: "
     */
    @DefaultMessage("Use venv with packages: ")
    @Key("chkUseVenvLabel")
    String chkUseVenvLabel();

    /**
     * Translated "(none)".
     *
     * @return translated "(none)"
     */
    @DefaultMessage("(none)")
    @Key("txtVenvPackagesNone")
    String txtVenvPackagesNone();

    /**
     * Translated "type".
     *
     * @return translated "type"
     */
    @DefaultMessage("type")
    @Key("quartoProjectTypeOption")
    String quartoProjectTypeOption();

    /**
     * Translated "engine".
     *
     * @return translated "engine"
     */
    @DefaultMessage("engine")
    @Key("quartoProjectEngineOption")
    String quartoProjectEngineOption();

    /**
     * Translated "kernel".
     *
     * @return translated "kernel"
     */
    @DefaultMessage("kernel")
    @Key("quartoProjectKernelOption")
    String quartoProjectKernelOption();

    /**
     * Translated "Quarto Blog".
     *
     * @return translated "Quarto Blog"
     */
    @DefaultMessage("Quarto Blog")
    @Key("quartoBlogTitle")
    String quartoBlogTitle();

    /**
     * Translated "Create a new Quarto blog project".
     *
     * @return translated "Create a new Quarto blog project"
     */
    @DefaultMessage("Create a new Quarto blog project")
    @Key("quartoBlogSubTitle")
    String quartoBlogSubTitle();

    /**
     * Translated "Create Quarto Blog".
     *
     * @return translated "Create Quarto Blog"
     */
    @DefaultMessage("Create Quarto Blog")
    @Key("quartoBlogPageCaption")
    String quartoBlogPageCaption();
    
    /**
     * Translated "Quarto Website".
     *
     * @return translated "Quarto Website"
     */
    @DefaultMessage("Quarto Website")
    @Key("quartoWebsiteTitle")
    String quartoWebsiteTitle();

    /**
     * Translated "Create a new Quarto website project".
     *
     * @return translated "Create a new Quarto website project"
     */
    @DefaultMessage("Create a new Quarto website project")
    @Key("quartoWebsiteSubTitle")
    String quartoWebsiteSubTitle();

    /**
     * Translated "Create Quarto Website".
     *
     * @return translated "Create Quarto Website"
     */
    @DefaultMessage("Create Quarto Website")
    @Key("quartoWebsitePageCaption")
    String quartoWebsitePageCaption();

    /**
     * Translated "Shiny Application".
     *
     * @return translated "Shiny Application"
     */
    @DefaultMessage("Shiny Application")
    @Key("shinyApplicationTitle")
    String shinyApplicationTitle();

    /**
     * Translated "Create a new Shiny application".
     *
     * @return translated "Create a new Shiny application"
     */
    @DefaultMessage("Create a new Shiny application")
    @Key("shinyApplicationSubTitle")
    String shinyApplicationSubTitle();

    /**
     * Translated "Create Shiny Application".
     *
     * @return translated "Create Shiny Application"
     */
    @DefaultMessage("Create Shiny Application")
    @Key("shinyApplicationPageCaption")
    String shinyApplicationPageCaption();

    /**
     * Translated "Subversion".
     *
     * @return translated "Subversion"
     */
    @DefaultMessage("Subversion")
    @Key("svnPageTitle")
    String svnPageTitle();

    /**
     * Translated "Checkout a project from a Subversion repository".
     *
     * @return translated "Checkout a project from a Subversion repository"
     */
    @DefaultMessage("Checkout a project from a Subversion repository")
    @Key("svnPageSubTitle")
    String svnPageSubTitle();

    /**
     * Translated "Checkout Subversion Repository".
     *
     * @return translated "Checkout Subversion Repository"
     */
    @DefaultMessage("Checkout Subversion Repository")
    @Key("svnPagePageCaption")
    String svnPagePageCaption();

    /**
     * Translated "Version Control".
     *
     * @return translated "Version Control"
     */
    @DefaultMessage("Version Control")
    @Key("versionControlTitle")
    String versionControlTitle();

    /**
     * Translated "Checkout a project from a version control repository".
     *
     * @return translated "Checkout a project from a version control repository"
     */
    @DefaultMessage("Checkout a project from a version control repository")
    @Key("versionControlSubTitle")
    String versionControlSubTitle();

    /**
     * Translated "Create Project from Version Control".
     *
     * @return translated "Create Project from Version Control"
     */
    @DefaultMessage("Create Project from Version Control")
    @Key("versionControlPageCaption")
    String versionControlPageCaption();

    /**
     * Translated "<p>{0} was not detected on the system path.</p><p>To create projects from {0} repositories you should install {0} and then restart RStudio.</p><p>Note that if {0} is installed and not on the path, then you can specify its location using the {1} dialog.</p>".
     *
     * @return translated "<p>{0} was not detected on the system path.</p><p>To create projects from {0} repositories you should install {0} and then restart RStudio.</p><p>Note that if {0} is installed and not on the path, then you can specify its location using the {1} dialog.</p>"
     */
    @DefaultMessage("<p>{0} was not detected on the system path.</p><p>To create projects from {0} repositories you should install {0} and then restart RStudio.</p><p>Note that if {0} is installed and not on the path, then you can specify its location using the {1} dialog.</p>")
    @Key("acceptNavigationHTML")
    String acceptNavigationHTML(String title, String location);

    /**
     * Translated "Options".
     *
     * @return translated "Options"
     */
    @DefaultMessage("Options")
    @Key("optionsLabel")
    String optionsLabel();

    /**
     * Translated "Preferences".
     *
     * @return translated "Preferences"
     */
    @DefaultMessage("Preferences")
    @Key("preferencesLabel")
    String preferencesLabel();

    /**
     * Translated "Using {0} with RStudio".
     *
     * @return translated "Using {0} with RStudio"
     */
    @DefaultMessage("Using {0} with RStudio")
    @Key("vcsHelpLink")
    String vcsHelpLink(String title);

    /**
     * Translated "Preferences".
     *
     * @return translated "Preferences"
     */
    @DefaultMessage("<p>An installation of {0} was not detected on this system.</p><p>To create projects from {0} repositories you should request that your server administrator install the {0} package.</p>")
    @Key("installationNotDetectedHTML")
    String installtionNotDetectedHTML(String title);

    /**
     * Translated "{0} Not Found".
     *
     * @return translated "{0} Not Found"
     */
    @DefaultMessage("{0} Not Found")
    @Key("titleNotFound")
    String titleNotFound(String title);

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    @Key("okLabel")
    String okLabel();

    /**
     * Translated "Repository URL:".
     *
     * @return translated "Repository URL:"
     */
    @DefaultMessage("Repository URL:")
    @Key("repoURLLabel")
    String repoURLLabel();

    /**
     * Translated "Username (if required for this repository URL):".
     *
     * @return translated "Username (if required for this repository URL):"
     */
    @DefaultMessage("Username (if required for this repository URL):")
    @Key("usernameLabel")
    String usernameLabel();

    /**
     * Translated "Project directory name:".
     *
     * @return translated "Project directory name:"
     */
    @DefaultMessage("Project directory name:")
    @Key("projDirNameLabel")
    String projDirNameLabel();

    /**
     * Translated "Create project as subdirectory of:".
     *
     * @return translated "Create project as subdirectory of:"
     */
    @DefaultMessage("Create project as subdirectory of:")
    @Key("existingRepoDestDirLabel")
    String existingRepoDestDirLabel();

    /**
     * Translated "You must specify a repository URL and directory to create the new project within.".
     *
     * @return translated "You must specify a repository URL and directory to create the new project within."
     */
    @DefaultMessage("You must specify a repository URL and directory to create the new project within.")
    @Key("specifyRepoURLErrorMessage")
    String specifyRepoURLErrorMessage();

    /**
     * Translated "PDF Generation".
     *
     * @return translated "PDF Generation"
     */
    @DefaultMessage("PDF Generation")
    @Key("pdfGenerationCaption")
    String pdfGenerationCaption();

    /**
     * Translated "PDF Generation".
     *
     * @return translated "PDF Preview"
     */
    @DefaultMessage("PDF Preview")
    @Key("pdfPreviewCaption")
    String pdfPreviewCaption();

    /**
     * Translated "Compile PDF root document:".
     *
     * @return translated "Compile PDF root document:"
     */
    @DefaultMessage("Compile PDF root document:")
    @Key("compilePDFLabel")
    String compilePDFLabel();

    /**
     * Translated "(Current Document)".
     *
     * @return translated "(Current Document)"
     */
    @DefaultMessage("(Current Document)")
    @Key("compilePDFEmptyLabel")
    String compilePDFEmptyLabel();

    /**
     * Translated "Browse...".
     *
     * @return translated "Browse..."
     */
    @DefaultMessage("Browse...")
    @Key("browseActionLabel")
    String browseActionLabel();

    /**
     * Translated "Get help on Compile PDF root document".
     *
     * @return translated "Get help on Compile PDF root document"
     */
    @DefaultMessage("Get help on Compile PDF root document")
    @Key("rootDocumentChooserTitle")
    String rootDocumentChooserTitle();

    /**
     * Translated "Choose File".
     *
     * @return translated "Choose File"
     */
    @DefaultMessage("Choose File")
    @Key("chooseFileCaption")
    String chooseFileCaption();

    /**
     * Translated "Index source files (for code search/navigation)".
     *
     * @return translated "Index source files (for code search/navigation)"
     */
    @DefaultMessage("Index source files (for code search/navigation)")
    @Key("enableCodeIndexingLabel")
    String enableCodeIndexingLabel();

    /**
     * Translated "Insert spaces for tab".
     *
     * @return translated "Insert spaces for tab"
     */
    @DefaultMessage("Insert spaces for tab")
    @Key("chkSpacesForTabLabel")
    String chkSpacesForTabLabel();

    /**
     * Translated "Tab width".
     *
     * @return translated "Tab width"
     */
    @DefaultMessage("Tab width")
    @Key("tabWidthLabel")
    String tabWidthLabel();

    /**
     * Translated "Use native pipe operator, |> (requires R 4.1+)"
     *
     * @return translated "Use native pipe operator, |> (requires R 4.1+)"
     */
    @DefaultMessage("Use native pipe operator, |> (requires R 4.1+)")
    @Key("insertNativePipeOperatorLabel")
    String insertNativePipeOperatorLabel();

    /**
     * Translated "Ensure that source files end with newline".
     *
     * @return translated "Ensure that source files end with newline"
     */
    @DefaultMessage("Ensure that source files end with newline")
    @Key("chkAutoAppendNewlineLabel")
    String chkAutoAppendNewlineLabel();

    /**
     * Translated "Strip trailing horizontal whitespace when saving".
     *
     * @return translated "Strip trailing horizontal whitespace when saving"
     */
    @DefaultMessage("Strip trailing horizontal whitespace when saving")
    @Key("chkStripTrailingWhitespaceLabel")
    String chkStripTrailingWhitespaceLabel();

    /**
     * Translated "Text encoding:".
     *
     * @return translated "Text encoding:"
     */
    @DefaultMessage("Text encoding:")
    @Key("textEncodingLabel")
    String textEncodingLabel();

    /**
     * Translated "Change...".
     *
     * @return translated "Change..."
     */
    @DefaultMessage("Change...")
    @Key("changeLabel")
    String changeLabel();

    /**
     * Translated "Code Editing".
     *
     * @return translated "Code Editing"
     */
    @DefaultMessage("Code Editing")
    @Key("codingEditingLabel")
    String codingEditingLabel();

    /**
     * Translated "Use (Default) to inherit the global default setting".
     *
     * @return translated "Use (Default) to inherit the global default setting"
     */
    @DefaultMessage("Use (Default) to inherit the global default setting")
    @Key("projectGeneralInfoLabel")
    String projectGeneralInfoLabel();

    /**
     * Translated "Restore .RData into workspace at startup".
     *
     * @return translated "Restore .RData into workspace at startup"
     */
    @DefaultMessage("Restore .RData into workspace at startup")
    @Key("restoreWorkspaceText")
    String restoreWorkspaceText();

    /**
     * Translated "Save workspace to .RData on exit".
     *
     * @return translated "Save workspace to .RData on exit"
     */
    @DefaultMessage("Save workspace to .RData on exit")
    @Key("saveWorkspaceText")
    String saveWorkspaceText();

    /**
     * Translated "Always save history (even if not saving .RData)".
     *
     * @return translated "Always save history (even if not saving .RData)"
     */
    @DefaultMessage("Always save history (even if not saving .RData)")
    @Key("alwaysSaveHistoryText")
    String alwaysSaveHistoryText();

    /**
     * Translated "Disable .Rprofile execution on session start/resume".
     *
     * @return translated "Disable .Rprofile execution on session start/resume"
     */
    @DefaultMessage("Disable .Rprofile execution on session start/resume")
    @Key("disableExecuteRprofileText")
    String disableExecuteRprofileText();

    /**
     * Translated "Quit child processes on exit".
     *
     * @return translated "Quit child processes on exit"
     */
    @DefaultMessage("Quit child processes on exit")
    @Key("quitChildProcessesOnExitText")
    String quitChildProcessesOnExitText();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultMessage("General")
    @Key("generalText")
    String generalText();

    /**
     * Translated "Packrat is a dependency management tool that makes your R code more isolated, portable, and reproducible by giving your project its own privately managed package library.".
     *
     * @return translated "Packrat is a dependency management tool that makes your R code more isolated, portable, and reproducible by giving your project its own privately managed package library."
     */
    @DefaultMessage("Packrat is a dependency management tool that makes your R code more isolated, portable, and reproducible by giving your project its own privately managed package library.")
    @Key("initializePackratMessage")
    String initializePackratMessage();

    /**
     * Translated "Use packrat with this project".
     *
     * @return translated "Use packrat with this project"
     */
    @DefaultMessage("Use packrat with this project")
    @Key("chkUsePackratLabel")
    String chkUsePackratLabel();

    /**
     * Translated "Automatically snapshot local changes".
     *
     * @return translated "Automatically snapshot local changes"
     */
    @DefaultMessage("Automatically snapshot local changes")
    @Key("chkAutoSnapshotLabel")
    String chkAutoSnapshotLabel();

    /**
     * Translated "{0} ignore packrat library".
     *
     * @return translated "{0} ignore packrat library"
     */
    @DefaultMessage("{0} ignore packrat library")
    @Key("chkVcsIgnoreLibLabel")
    String chkVcsIgnoreLibLabel(String vcsName);

    /**
     * Translated "{0} ignore packrat sources".
     *
     * @return translated "{0} ignore packrat sources"
     */
    @DefaultMessage("{0} ignore packrat sources")
    @Key("chkVcsIgnoreSrcLabel")
    String chkVcsIgnoreSrcLabel(String vcsName);

    /**
     * Translated "External packages (comma separated):".
     *
     * @return translated "External packages (comma separated):"
     */
    @DefaultMessage("External packages (comma separated):")
    @Key("panelExternalPackagesText")
    String panelExternalPackagesText();

    /**
     * Translated "Help on external packages".
     *
     * @return translated "Help on external packages"
     */
    @DefaultMessage("Help on external packages")
    @Key("panelExternalPackagesTitle")
    String panelExternalPackagesTitle();

    /**
     * Translated "Learn more about Packrat".
     *
     * @return translated "Learn more about Packrat"
     */
    @DefaultMessage("Learn more about Packrat")
    @Key("packratHelpLink")
    String packratHelpLink();

    /**
     * Translated "Verifying prerequisites...".
     *
     * @return translated "Verifying prerequisites..."
     */
    @DefaultMessage("Verifying prerequisites...")
    @Key("verifyPrerequisitesLabel")
    String verifyPrerequisitesLabel();

    /**
     * Translated "Managing packages with packrat".
     *
     * @return translated "Managing packages with packrat"
     */
    @DefaultMessage("Managing packages with packrat")
    @Key("packratManagePackages")
    String packratManagePackages();

    /**
     * Translated "Project Options".
     *
     * @return translated "Project Options"
     */
    @DefaultMessage("Project Options")
    @Key("projectOptionsCaption")
    String projectOptionsCaption();

    /**
     * Translated "Confirm Restart RStudio".
     *
     * @return translated "Confirm Restart RStudio"
     */
    @DefaultMessage("Confirm Restart RStudio")
    @Key("restartRStudioCaption")
    String restartRStudioCaption();

    /**
     * Translated "You need to restart RStudio in order for this change to take effect. Do you want to do this now?".
     *
     * @return translated "You need to restart RStudio in order for this change to take effect. Do you want to do this now?"
     */
    @DefaultMessage("You need to restart RStudio in order for this change to take effect. Do you want to do this now?")
    @Key("restartRStudioMessage")
    String restartRStudioMessage();

    /**
     * Translated "(Use default)".
     *
     * @return translated "(Use default)"
     */
    @DefaultMessage("(Use default)")
    @Key("useDefaultText")
    String useDefaultText();

    /**
     * Translated "Environments".
     *
     * @return translated "Environments"
     */
    @DefaultMessage("Environments")
    @Key("environmentsText")
    String environmentsText();

    /**
     * Translated "RStudio uses the renv package to give your projects their own privately-managed package library, making your R code more isolated, portable, and reproducible.".
     *
     * @return translated "RStudio uses the renv package to give your projects their own privately-managed package library, making your R code more isolated, portable, and reproducible."
     */
    @DefaultMessage("RStudio uses the renv package to give your projects their own privately-managed package library, making your R code more isolated, portable, and reproducible.")
    @Key("rstudioInitializeLabel")
    String rstudioInitializeLabel();

    /**
     * Translated "Learn more about renv".
     *
     * @return translated "Learn more about renv"
     */
    @DefaultMessage("Learn more about renv")
    @Key("renvHelpLink")
    String renvHelpLink();

    /**
     * Translated "Visual Mode: Markdown Output".
     *
     * @return translated "Visual Mode: Markdown Output"
     */
    @DefaultMessage("Visual Mode: Markdown Output")
    @Key("visualModeCaption")
    String visualModeCaption();

    /**
     * Translated "Use (Default) to inherit the global default setting".
     *
     * @return translated "Use (Default) to inherit the global default setting"
     */
    @DefaultMessage("Use (Default) to inherit the global default setting")
    @Key("rMarkdownInfoLabel")
    String rMarkdownInfoLabel();

    /**
     * Translated "Wrap at column:".
     *
     * @return translated "Wrap at column:"
     */
    @DefaultMessage("Wrap at column:")
    @Key("wrapColumnLabel")
    String wrapColumnLabel();

    /**
     * Translated "Automatic text wrapping (line breaks)".
     *
     * @return translated "Automatic text wrapping (line breaks)"
     */
    @DefaultMessage("Automatic text wrapping (line breaks)")
    @Key("wrapPanelText")
    String wrapPanelText();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    @DefaultMessage("(Default)")
    @Key("referencesDefaultItem")
    String referencesDefaultItem();

    /**
     * Translated "Write references at end of current".
     *
     * @return translated "Write references at end of current"
     */
    @DefaultMessage("Write references at end of current")
    @Key("referencesFormLabel")
    String referencesFormLabel();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    @DefaultMessage("(Default)")
    @Key("canonicalDefaultItem")
    String canonicalDefaultItem();

    /**
     * Translated "true".
     *
     * @return translated "true"
     */
    @DefaultMessage("true")
    @Key("canonicalTrueItem")
    String canonicalTrueItem();

    /**
     * Translated "false".
     *
     * @return translated "false"
     */
    @DefaultMessage("false")
    @Key("canonicalFalseItem")
    String canonicalFalseItem();

    /**
     * Translated "Write canonical visual mode markdown in source mode".
     *
     * @return translated "Write canonical visual mode markdown in source mode"
     */
    @DefaultMessage("Write canonical visual mode markdown in source mode")
    @Key("canonicalFormLabel")
    String canonicalFormLabel();

    /**
     * Translated "Learn more about markdown writer options".
     *
     * @return translated "Learn more about markdown writer options"
     */
    @DefaultMessage("Learn more about markdown writer options")
    @Key("markdownPerFileOptionsCaption")
    String markdownPerFileOptionsCaption();

    /**
     * Translated "Visual Mode: Zotero".
     *
     * @return translated "Visual Mode: Zotero"
     */
    @DefaultMessage("Visual Mode: Zotero")
    @Key("visualModeZoteroCaption")
    String visualModeZoteroCaption();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultMessage("R Markdown")
    @Key("rMarkdownText")
    String rMarkdownText();

    /**
     * Translated "Sharing".
     *
     * @return translated "Sharing"
     */
    @DefaultMessage("Sharing")
    @Key("sharingText")
    String sharingText();

    /**
     * Translated "Version control system:".
     *
     * @return translated "Version control system:"
     */
    @DefaultMessage("Version control system:")
    @Key("vcsSelectLabel")
    String vcsSelectLabel();

    /**
     * Translated "Origin: ".
     *
     * @return translated "Origin: "
     */
    @DefaultMessage("Origin: ")
    @Key("originLabel")
    String originLabel();

    /**
     * Translated "Origin:".
     *
     * @return translated "Origin:"
     */
    @DefaultMessage("Origin:")
    @Key("lblOrigin")
    String lblOrigin();

    /**
     * Translated "Repo:".
     *
     * @return translated "Repo:"
     */
    @DefaultMessage("Repo:")
    @Key("repoCaption")
    String repoCaption();

    /**
     * Translated "Checking for git repository...".
     *
     * @return translated "Checking for git repository..."
     */
    @DefaultMessage("Checking for git repository...")
    @Key("confirmGitRepoLabel")
    String confirmGitRepoLabel();

    /**
     * Translated "Confirm New Git Repository".
     *
     * @return translated "Confirm New Git Repository"
     */
    @DefaultMessage("Confirm New Git Repository")
    @Key("confirmGitRepoCaption")
    String confirmGitRepoCaption();

    /**
     * Translated "Do you want to initialize a new git repository for this project?".
     *
     * @return translated "Do you want to initialize a new git repository for this project?"
     */
    @DefaultMessage("Do you want to initialize a new git repository for this project?")
    @Key("confirmGitRepoMessage")
    String confirmGitRepoMessage();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    @DefaultMessage("(None)")
    @Key("noneProjectSourceControlLabel")
    String noneProjectSourceControlLabel();

    /**
     * Translated "Dictionaries".
     *
     * @return translated "Dictionaries"
     */
    @DefaultMessage("Dictionaries")
    @Key("dictionariesCaption")
    String dictionariesCaption();

    /**
     * Translated "Use (Default) to inherit the global default dictionary".
     *
     * @return translated "Use (Default) to inherit the global default dictionary"
     */
    @DefaultMessage("Use (Default) to inherit the global default dictionary")
    @Key("dictionariesInfoLabel")
    String dictionariesInfoLabel();

    /**
     * Translated "Spelling".
     *
     * @return translated "Spelling"
     */
    @DefaultMessage("Spelling")
    @Key("spellingText")
    String spellingText();

    /**
     * Translated "Yes".
     *
     * @return translated "Yes"
     */
    @DefaultMessage("Yes")
    @Key("yesLabel")
    String yesLabel();

    /**
     * Translated "No".
     *
     * @return translated "No"
     */
    @DefaultMessage("No")
    @Key("noLabel")
    String noLabel();

    /**
     * Translated "Ask".
     *
     * @return translated "Ask"
     */
    @DefaultMessage("Ask")
    @Key("askLabel")
    String askLabel();

    /**
     * Translated "Custom build script:".
     *
     * @return translated "Custom build script:"
     */
    @DefaultMessage("Custom build script:")
    @Key("pathSelectorLabel")
    String pathSelectorLabel();

    /**
     * Translated "Script Not Specified".
     *
     * @return translated "Script Not Specified"
     */
    @DefaultMessage("Script Not Specified")
    @Key("validateScriptCaption")
    String validateScriptCaption();

    /**
     * Translated "You must specify a path to the custom build script.".
     *
     * @return translated "You must specify a path to the custom build script."
     */
    @DefaultMessage("You must specify a path to the custom build script.")
    @Key("validateScriptMessage")
    String validateScriptMessage();

    /**
     * Translated "Makefile directory:".
     *
     * @return translated "Makefile directory:"
     */
    @DefaultMessage("Makefile directory:")
    @Key("pathSelectorMakefileDirLabel")
    String pathSelectorMakefileDirLabel();

    /**
     * Translated "Additional arguments:".
     *
     * @return translated "Additional arguments:"
     */
    @DefaultMessage("Additional arguments:")
    @Key("txtMakefileArgs")
    String txtMakefileArgs();

    /**
     * Translated "Package directory:".
     *
     * @return translated "Package directory:"
     */
    @DefaultMessage("Package directory:")
    @Key("pathSelectorPackageDir")
    String pathSelectorPackageDir();

    /**
     * Translated "Use devtools package functions if available".
     *
     * @return translated "Use devtools package functions if available"
     */
    @DefaultMessage("Use devtools package functions if available")
    @Key("chkUseDevtoolsCaption")
    String chkUseDevtoolsCaption();

    /**
     * Translated "Clean before install".
     *
     * @return translated "Clean before install"
     */
    @DefaultMessage("Clean before install")
    @Key("cleanBeforeInstallLabel")
    String cleanBeforeInstallLabel();

    /**
     * Translated "Generate documentation with Roxygen".
     *
     * @return translated "Generate documentation with Roxygen"
     */
    @DefaultMessage("Generate documentation with Roxygen")
    @Key("chkUseRoxygenCaption")
    String chkUseRoxygenCaption();

    /**
     * Translated "Configure...".
     *
     * @return translated "Configure..."
     */
    @DefaultMessage("Configure...")
    @Key("btnConfigureRoxygenLabel")
    String btnConfigureRoxygenLabel();

    /**
     * Translated "Install Package &mdash; R CMD INSTALL additional options:".
     *
     * @return translated "Install Package &mdash; R CMD INSTALL additional options:"
     */
    @DefaultMessage("Install Package &mdash; R CMD INSTALL additional options:")
    @Key("installMdashArgument")
    String installMdashArgument();

    /**
     * Translated "Check Package &mdash; R CMD check additional options:".
     *
     * @return translated "Check Package &mdash; R CMD check additional options:"
     */
    @DefaultMessage("Check Package &mdash; R CMD check additional options:")
    @Key("checkPackageMdashArgument")
    String checkPackageMdashArgument();

    /**
     * Translated "Check Package &mdash; R CMD check additional options:".
     *
     * @return translated "Check Package &mdash; R CMD check additional options:"
     */
    @DefaultMessage("Build Source Package &mdash; R CMD build additional options:")
    @Key("buildSourceMdashArgument")
    String buildSourceMdashArgument();

    /**
     * Translated "Build Binary Package &mdash; R CMD INSTALL additional options:".
     *
     * @return translated "Build Binary Package &mdash; R CMD INSTALL additional options:"
     */
    @DefaultMessage("Build Binary Package &mdash; R CMD INSTALL additional options:")
    @Key("buildBinaryMdashArgument")
    String buildBinaryMdashArgument();

    /**
     * Translated "Browse...".
     *
     * @return translated "Browse..."
     */
    @DefaultMessage("Browse...")
    @Key("browseLabel")
    String browseLabel();

    /**
     * Translated "(Project Root)".
     *
     * @return translated "(Project Root)"
     */
    @DefaultMessage("(Project Root)")
    @Key("projectRootLabel")
    String projectRootLabel();

    /**
     * Translated "Choose Directory".
     *
     * @return translated "Choose Directory"
     */
    @DefaultMessage("Choose Directory")
    @Key("chooseDirectoryCaption")
    String chooseDirectoryCaption();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    @DefaultMessage("(None)")
    @Key("noneFileSelectorLabel")
    String noneFileSelectorLabel();

    /**
     * Translated "Roxygen Options".
     *
     * @return translated "Roxygen Options"
     */
    @DefaultMessage("Roxygen Options")
    @Key("roxygenOptionsCaption")
    String roxygenOptionsCaption();

    /**
     * Translated "Site directory:".
     *
     * @return translated "Site directory:"
     */
    @DefaultMessage("Site directory:")
    @Key("pathSelectorSiteDir")
    String pathSelectorSiteDir();

    /**
     * Translated "Book output format(s):".
     *
     * @return translated "Book output format(s):"
     */
    @DefaultMessage("Book output format(s):")
    @Key("websiteOutputFormatLabel")
    String websiteOutputFormatLabel();

    /**
     * Translated "all".
     *
     * @return translated "all"
     */
    @DefaultMessage("all")
    @Key("allLabel")
    String allLabel();

    /**
     * Translated "Preview site after building".
     *
     * @return translated "Preview site after building"
     */
    @DefaultMessage("Preview site after building")
    @Key("chkPreviewAfterBuildingCaption")
    String chkPreviewAfterBuildingCaption();

    /**
     * Translated "Re-knit current preview when supporting files change".
     *
     * @return translated "Re-knit current preview when supporting files change"
     */
    @DefaultMessage("Re-knit current preview when supporting files change")
    @Key("chkLivePreviewSiteCaption")
    String chkLivePreviewSiteCaption();

    /**
     * Translated "Supporting files include Rmd partials, R scripts, YAML config files, etc.".
     *
     * @return translated "Supporting files include Rmd partials, R scripts, YAML config files, etc."
     */
    @DefaultMessage("Supporting files include Rmd partials, R scripts, YAML config files, etc.")
    @Key("infoLabel")
    String infoLabel();

    /**
     * Translated "Preview book after building".
     *
     * @return translated "Preview book after building"
     */
    @DefaultMessage("Preview book after building")
    @Key("chkPreviewAfterBuilding")
    String chkPreviewAfterBuilding();

    /**
     * Translated "(All Formats)".
     *
     * @return translated "(All Formats)"
     */
    @DefaultMessage("(All Formats)")
    @Key("allFormatsLabel")
    String allFormatsLabel();

    /**
     * Translated "Build Tools".
     *
     * @return translated "Build Tools"
     */
    @DefaultMessage("Build Tools")
    @Key("buildToolsLabel")
    String buildToolsLabel();

    /**
     * Translated "Project build tools:".
     *
     * @return translated "Project build tools:"
     */
    @DefaultMessage("Project build tools:")
    @Key("projectBuildToolsLabel")
    String projectBuildToolsLabel();

    /**
     * Translated "Package".
     *
     * @return translated "Package"
     */
    @DefaultMessage("Package")
    @Key("packageLabel")
    String packageLabel();

    /**
     * Translated "Git/SVN".
     *
     * @return translated "Git/SVN"
     */
    @DefaultMessage("Git/SVN")
    @Key("gitLabel")
    String gitLabel();

    /**
     * Translated "Close Project".
     *
     * @return translated "Close Project"
     */
    @DefaultMessage("Close Project")
    @Key("closeProjectLabel")
    String closeProjectLabel();

    /**
     * Translated "placeholder".
     *
     * @return translated "placeholder"
     */
    @DefaultMessage("placeholder")
    @Key("placeholderLabel")
    String placeholderLabel();

    /**
     * Translated "Use global cache for installed packages".
     *
     * @return translated "Use global cache for installed packages"
     */
    @DefaultMessage("Use global cache for installed packages")
    @Key("chkUseCacheLabel")
    String chkUseCacheLabel();

    /**
     * Translated "Use condaenv with packages:".
     *
     * @return translated "Use condaenv with packages:"
     */
    @DefaultMessage("Use condaenv with packages:")
    @Key("useCondaenv")
    String useCondaenv();
}
