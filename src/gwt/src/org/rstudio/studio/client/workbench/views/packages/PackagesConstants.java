/*
 * PackagesConstants.java
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
package org.rstudio.studio.client.workbench.views.packages;

public interface PackagesConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Create Package Library".
     *
     * @return translated "Create Package Library"
     */
    @DefaultMessage("Create Package Library")
    @Key("createPackageLibraryCaption")
    String createPackageLibraryCaption();

    /**
     * Translated "Would you like to create a personal library ''{0}'' to install packages into?".
     *
     * @return translated "Would you like to create a personal library ''{0}'' to install packages into?"
     */
    @DefaultMessage("Would you like to create a personal library ''{0}'' to install packages into?")
    @Key("createPackageLibraryMessage")
    String createPackageLibraryMessage(String libraryPath);

    /**
     * Translated "Error Creating Library".
     *
     * @return translated "Error Creating Library"
     */
    @DefaultMessage("Error Creating Library")
    @Key("errorCreatingLibraryCaption")
    String errorCreatingLibraryCaption();

    /**
     * Translated "Install Packages".
     *
     * @return translated "Install Packages"
     */
    @DefaultMessage("Install Packages")
    @Key("installPackagesCaption")
    String installPackagesCaption();

    /**
     * Translated "Unable to install packages (default library ''{0}'' is not writeable)".
     *
     * @return translated "Unable to install packages (default library ''{0}'' is not writeable)"
     */
    @DefaultMessage("Unable to install packages (default library ''{0}'' is not writeable)")
    @Key("unableToInstallPackagesMessage")
    String unableToInstallPackagesMessage(String libraryPath);

    /**
     * Translated "Check for Updates".
     *
     * @return translated "Check for Updates"
     */
    @DefaultMessage("Check for Updates")
    @Key("checkForUpdatesCaption")
    String checkForUpdatesCaption();

    /**
     * Translated "All packages are up to date.".
     *
     * @return translated "All packages are up to date."
     */
    @DefaultMessage("All packages are up to date.")
    @Key("checkForUpdatesMessage")
    String checkForUpdatesMessage();

    /**
     * Translated "Export Project Bundle to Gzipped Tarball".
     *
     * @return translated "Export Project Bundle to Gzipped Tarball"
     */
    @DefaultMessage("Export Project Bundle to Gzipped Tarball")
    @Key("exportProjectBundleCaption")
    String exportProjectBundleCaption();

    /**
     * Translated "Up to Date".
     *
     * @return translated "Up to Date"
     */
    @DefaultMessage("Up to Date")
    @Key("upToDateCaption")
    String upToDateCaption();

    /**
     * Translated "The Packrat library is up to date.".
     *
     * @return translated "The Packrat library is up to date."
     */
    @DefaultMessage("The Packrat library is up to date.")
    @Key("packratLibraryUpToDate")
    String packratLibraryUpToDate();

    /**
     * Translated "Error checking Packrat library status".
     *
     * @return translated "Error checking Packrat library status"
     */
    @DefaultMessage("Error checking Packrat library status")
    @Key("errorCheckingPackrat")
    String errorCheckingPackrat();

    /**
     * Translated "Error during {0}".
     *
     * @return translated "Error during {0}"
     */
    @DefaultMessage("Error during {0}")
    @Key("errorMessage")
    String errorCheckingPackrat(String action);

    /**
     * Translated "Performing {0}...".
     *
     * @return translated "Performing {0}..."
     */
    @DefaultMessage("Performing {0}...")
    @Key("renvActionOnProgressMessage")
    String renvActionOnProgressMessage(String action);

    /**
     * Translated "The project is already up to date.".
     *
     * @return translated "The project is already up to date."
     */
    @DefaultMessage("The project is already up to date.")
    @Key("projectUpToDateMessage")
    String projectUpToDateMessage();

    /**
     * Translated "Are you sure you wish to permanently uninstall the ''{0}'' package".
     *
     * @return translated "Are you sure you wish to permanently uninstall the ''{0}'' package"
     */
    @DefaultMessage("Are you sure you wish to permanently uninstall the ''{0}'' package")
    @Key("uninstallPackage")
    String uninstallPackage(String packageName);

    /**
     * Translated "from library ''{0}''".
     *
     * @return translated "from library ''{0}''"
     */
    @DefaultMessage("from library ''{0}''")
    @Key("libraryMessage")
    String libraryMessage(String library);

    /**
     * Translated "? This action cannot be undone.".
     *
     * @return translated "? This action cannot be undone."
     */
    @DefaultMessage("? This action cannot be undone.")
    @Key("actionCannotBeUndoneMessage")
    String actionCannotBeUndoneMessage();

    /**
     * Translated "Uninstall Package ".
     *
     * @return translated "Uninstall Package "
     */
    @DefaultMessage("Uninstall Package ")
    @Key("uninstallPackageCaption")
    String uninstallPackageCaption();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "Retrieving package installation context...".
     *
     * @return translated "Retrieving package installation context..."
     */
    @DefaultMessage("Retrieving package installation context...")
    @Key("retrievingPackageInstallationMessage")
    String retrievingPackageInstallationMessage();

    /**
     * Translated "One or more of the packages to be updated are currently loaded. Restarting R prior to install is highly recommended.\n\nRStudio can restart R before installing the requested packages. All work and data will be preserved during restart.\n\nDo you want to restart R prior to install?".
     *
     * @return translated "One or more of the packages to be updated are currently loaded. Restarting R prior to install is highly recommended.\n\nRStudio can restart R before installing the requested packages. All work and data will be preserved during restart.\n\nDo you want to restart R prior to install?"
     */
    @DefaultMessage("One or more of the packages to be updated are currently loaded. Restarting R prior to install is highly recommended.\\n\\nRStudio can restart R before installing the requested packages. All work and data will be preserved during restart.\\n\\nDo you want to restart R prior to install?")
    @Key("restartForInstallWithConfirmation")
    String restartForInstallWithConfirmation();

    /**
     * Translated "Updating Loaded Packages".
     *
     * @return translated "Updating Loaded Packages"
     */
    @DefaultMessage("Updating Loaded Packages")
    @Key("updatingLoadedPackagesCaption")
    String updatingLoadedPackagesCaption();

    /**
     * Translated "Error Listing Packages".
     *
     * @return translated "Error Listing Packages"
     */
    @DefaultMessage("Error Listing Packages")
    @Key("errorListingPackagesCaption")
    String errorListingPackagesCaption();

    /**
     * Translated "Packages".
     *
     * @return translated "Packages"
     */
    @DefaultMessage("Packages")
    @Key("packagesTitle")
    String packagesTitle();

    /**
     * Translated "Packages Tab".
     *
     * @return translated "Packages Tab"
     */
    @DefaultMessage("Packages Tab")
    @Key("packagesTabLabel")
    String packagesTabLabel();

    /**
     * Translated "Filter by package name".
     *
     * @return translated "Filter by package name"
     */
    @DefaultMessage("Filter by package name")
    @Key("filterByPackageNameLabel")
    String filterByPackageNameLabel();

    /**
     * Translated "Browse package on CRAN".
     *
     * @return translated "Browse package on CRAN"
     */
    @DefaultMessage("Browse package on CRAN")
    @Key("browsePackageCRANLabel")
    String browsePackageCRANLabel();

    /**
     * Translated "Browse package on Bioconductor".
     *
     * @return translated "Browse package on Bioconductor"
     */
    @DefaultMessage("Browse package on CRAN")
    @Key("browsePackageBioconductorLabel")
    String browsePackageBioconductorLabel();

    /**
     * Translated "Browse package [{0}]".
     *
     * @return translated "Browse package [{0}]"
     */
    @DefaultMessage("Browse package [{0}]")
    @Key("brosePackageLabel")
    String brosePackageLabel(String browseUrl);

    /**
     * Translated "Browse package on GitHub".
     *
     * @return translated "Browse package on GitHub"
     */
    @DefaultMessage("Browse package on GitHub")
    @Key("browsePackageGitHubLabel")
    String browsePackageGitHubLabel();

    /**
     * Translated "Remove package".
     *
     * @return translated "Remove package"
     */
    @DefaultMessage("Remove package")
    @Key("removePackageTitle")
    String removePackageTitle();

    /**
     * Translated "Name".
     *
     * @return translated "Name"
     */
    @DefaultMessage("Name")
    @Key("nameText")
    String nameText();

    /**
     * Translated "Description".
     *
     * @return translated "Description"
     */
    @DefaultMessage("Description")
    @Key("descriptionText")
    String descriptionText();

    /**
     * Translated "Version".
     *
     * @return translated "Version"
     */
    @DefaultMessage("Version")
    @Key("versionText")
    String versionText();

    /**
     * Translated "Lockfile".
     *
     * @return translated "Lockfile"
     */
    @DefaultMessage("Lockfile")
    @Key("lockfileText")
    String lockfileText();

    /**
     * Translated "Source".
     *
     * @return translated "Source"
     */
    @DefaultMessage("Source")
    @Key("sourceText")
    String sourceText();

    /**
     * Translated "Package Not Loaded".
     *
     * @return translated "Package Not Loaded"
     */
    @DefaultMessage("Package Not Loaded")
    @Key("packageNotLoadedCaption")
    String packageNotLoadedCaption();

    /**
     * Translated "The package ''{0}'' cannot be loaded because it is not installed. Install the package to make it available for loading.".
     *
     * @return translated "The package ''{0}'' cannot be loaded because it is not installed. Install the package to make it available for loading."
     */
    @DefaultMessage("The package ''{0}'' cannot be loaded because it is not installed. Install the package to make it available for loading.")
    @Key("packageNotLoadedMessage")
    String packageNotLoadedMessage(String packageName);

    /**
     * Translated "Help Not Available".
     *
     * @return translated "Help Not Available"
     */
    @DefaultMessage("Help Not Available")
    @Key("helpNotAvailableCaption")
    String helpNotAvailableCaption();

    /**
     * Translated "The package ''{0}'' is not installed. Install the package to make its help content available.".
     *
     * @return translated "The package ''{0}'' is not installed. Install the package to make its help content available."
     */
    @DefaultMessage("The package ''{0}'' is not installed. Install the package to make its help content available.")
    @Key("helpNotAvailableMessage")
    String helpNotAvailableMessage(String packageName);

    /**
     * Translated "Project Library".
     *
     * @return translated "Project Library"
     */
    @DefaultMessage("Project Library")
    @Key("projectLibraryText")
    String projectLibraryText();

    /**
     * Translated "User Library".
     *
     * @return translated "User Library"
     */
    @DefaultMessage("User Library")
    @Key("userLibraryText")
    String userLibraryText();


    /**
     * Translated "System Library".
     *
     * @return translated "System Library"
     */
    @DefaultMessage("System Library")
    @Key("systemLibraryText")
    String systemLibraryText();

    /**
     * Translated "System Library".
     *
     * @return translated "Library"
     */
    @DefaultMessage("Library")
    @Key("libraryText")
    String libraryText();

    /**
     * Translated "Update Packages".
     *
     * @return translated "Update Packages"
     */
    @DefaultMessage("Update Packages")
    @Key("updatePackagesCaption")
    String updatePackagesCaption();

    /**
     * Translated "Install Updates".
     *
     * @return translated "Install Updates"
     */
    @DefaultMessage("Install Updates")
    @Key("installUpdatesCaption")
    String installUpdatesCaption();

    /**
     * Translated "Package".
     *
     * @return translated "Package"
     */
    @DefaultMessage("Package")
    @Key("packageHeader")
    String packageHeader();

    /**
     * Translated "Installed".
     *
     * @return translated "Installed"
     */
    @DefaultMessage("Installed")
    @Key("installedHeader")
    String installedHeader();

    /**
     * Translated "Available".
     *
     * @return translated "Available"
     */
    @DefaultMessage("Available")
    @Key("availableHeader")
    String availableHeader();

    /**
     * Translated "Opening NEWS...".
     *
     * @return translated "Opening NEWS..."
     */
    @DefaultMessage("Opening NEWS...")
    @Key("openingNewsProgressMessage")
    String openingNewsProgressMessage();

    /**
     * Translated "Show package NEWS".
     *
     * @return translated "Show package NEWS"
     */
    @DefaultMessage("Show package NEWS")
    @Key("showPackageNewsTitle")
    String showPackageNewsTitle();

    /**
     * Translated "NEWS".
     *
     * @return translated "NEWS"
     */
    @DefaultMessage("NEWS")
    @Key("newsHeader")
    String newsHeader();

    /**
     * Translated "Error Opening NEWS".
     *
     * @return translated "Error Opening NEWS"
     */
    @DefaultMessage("Error Opening NEWS")
    @Key("errorOpeningNewsCaption")
    String errorOpeningNewsCaption();

    /**
     * Translated "This package does not have a NEWS file or RStudio was unable to determine an appropriate NEWS URL for this package.".
     *
     * @return translated "This package does not have a NEWS file or RStudio was unable to determine an appropriate NEWS URL for this package."
     */
    @DefaultMessage("This package does not have a NEWS file or RStudio was unable to determine an appropriate NEWS URL for this package.")
    @Key("errorOpeningNewsMessage")
    String errorOpeningNewsMessage();

    /**
     * Translated "Clean Unused Packages".
     *
     * @return translated "Clean Unused Packages"
     */
    @DefaultMessage("Clean Unused Packages")
    @Key("cleanUnusedPackagesCaption")
    String cleanUnusedPackagesCaption();

    /**
     * Translated "Remove Packages".
     *
     * @return translated "Remove Packages"
     */
    @DefaultMessage("Remove Packages")
    @Key("removePackagesCaption")
    String removePackagesCaption();

    /**
     * Translated "Packrat Clean".
     *
     * @return translated "Packrat Clean"
     */
    @DefaultMessage("Packrat Clean")
    @Key("packratCleanCaption")
    String packratCleanCaption();

    /**
     * Translated "No unused packages were found in the library.".
     *
     * @return translated "No unused packages were found in the library."
     */
    @DefaultMessage("No unused packages were found in the library.")
    @Key("packratCleanMessage")
    String packratCleanMessage();

    /**
     * Translated "These packages are present in your library, but do not appear to be used by code in your project. Select any you''d like to clean up.".
     *
     * @return translated "These packages are present in your library, but do not appear to be used by code in your project. Select any you''d like to clean up."
     */
    @DefaultMessage("These packages are present in your library, but do not appear to be used by code in your project. Select any you''d like to clean up.")
    @Key("explanatoryMessage")
    String explanatoryMessage();

    /**
     * Translated "Install".
     *
     * @return translated "Install"
     */
    @DefaultMessage("Install")
    @Key("installButtonCaption")
    String installButtonCaption();

    /**
     * Translated "No Package Selected".
     *
     * @return translated "No Package Selected"
     */
    @DefaultMessage("No Package Selected")
    @Key("noPackageSelectedCaption")
    String noPackageSelectedCaption();

    /**
     * Translated "You must specify the package to install.".
     *
     * @return translated "You must specify the package to install."
     */
    @DefaultMessage("You must specify the package to install.")
    @Key("noPackageSelectedMessage")
    String noPackageSelectedMessage();

    /**
     * Translated "Repository ({0})".
     *
     * @return translated "Repository ({0})"
     */
    @DefaultMessage("Repository ({0})")
    @Key("repositoryLabel")
    String repositoryLabel(String repo);

    /**
     * Translated "Repository (".
     *
     * @return translated "Repository ("
     */
    @DefaultMessage("Repository (")
    @Key("repositoryItemLabel")
    String repositoryItemLabel();

    /**
     * Translated "Package Archive File (".
     *
     * @return translated "Package Archive File ("
     */
    @DefaultMessage("Package Archive File (")
    @Key("packageArchiveFileLabel")
    String packageArchiveFileLabel();

    /**
     * Translated "Install from:".
     *
     * @return translated "Install from:"
     */
    @DefaultMessage("Install from:")
    @Key("installFromCaption")
    String installFromCaption();

    /**
     * Translated "Configuring Repositories".
     *
     * @return translated "Configuring Repositories"
     */
    @DefaultMessage("Configuring Repositories")
    @Key("configuringRepositoriesHelpCaption")
    String configuringRepositoriesHelpCaption();

    /**
     * Translated "Packages (separate multiple with space or comma):".
     *
     * @return translated "Packages (separate multiple with space or comma):"
     */
    @DefaultMessage("Packages (separate multiple with space or comma):")
    @Key("packagesLabel")
    String packagesLabel();

    /**
     * Translated "Package archive:".
     *
     * @return translated "Package archive:"
     */
    @DefaultMessage("Package archive:")
    @Key("packageArchiveLabel")
    String packageArchiveLabel();

    /**
     * Translated "Browse...".
     *
     * @return translated "Browse..."
     */
    @DefaultMessage("Browse...")
    @Key("browseActionLabel")
    String browseActionLabel();

    /**
     * Translated "Install to Library:".
     *
     * @return translated "Install to Library:"
     */
    @DefaultMessage("Install to Library:")
    @Key("installToLibraryText")
    String installToLibraryText();

    /**
     * Translated "Install dependencies".
     *
     * @return translated "Install dependencies"
     */
    @DefaultMessage("Install dependencies")
    @Key("installDependenciesText")
    String installDependenciesText();

    /**
     * Translated "Select Package Archive".
     *
     * @return translated "Select Package Archive"
     */
    @DefaultMessage("Select Package Archive")
    @Key("selectPackageArchiveCaption")
    String selectPackageArchiveCaption();

    /**
     * Translated "Select All".
     *
     * @return translated "Select All"
     */
    @DefaultMessage("Select All")
    @Key("selectAllLabel")
    String selectAllLabel();

    /**
     * Translated "Select None".
     *
     * @return translated "Select None"
     */
    @DefaultMessage("Select None")
    @Key("selectNoneLabel")
    String selectNoneLabel();

}
