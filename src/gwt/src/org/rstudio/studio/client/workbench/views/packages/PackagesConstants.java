/*
 * PackagesConstants.java
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
package org.rstudio.studio.client.workbench.views.packages;

public interface PackagesConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Create Package Library".
     *
     * @return translated "Create Package Library"
     */
    String createPackageLibraryCaption();

    /**
     * Translated "Would you like to create a personal library ''{0}'' to install packages into?".
     *
     * @return translated "Would you like to create a personal library ''{0}'' to install packages into?"
     */
    String createPackageLibraryMessage(String libraryPath);

    /**
     * Translated "Error Creating Library".
     *
     * @return translated "Error Creating Library"
     */
    String errorCreatingLibraryCaption();

    /**
     * Translated "Install Packages".
     *
     * @return translated "Install Packages"
     */
    String installPackagesCaption();

    /**
     * Translated "Unable to install packages (default library ''{0}'' is not writeable)".
     *
     * @return translated "Unable to install packages (default library ''{0}'' is not writeable)"
     */
    String unableToInstallPackagesMessage(String libraryPath);

    /**
     * Translated "Check for Updates".
     *
     * @return translated "Check for Updates"
     */
    String checkForUpdatesCaption();

    /**
     * Translated "All packages are up to date.".
     *
     * @return translated "All packages are up to date."
     */
    String checkForUpdatesMessage();

    /**
     * Translated "Export Project Bundle to Gzipped Tarball".
     *
     * @return translated "Export Project Bundle to Gzipped Tarball"
     */
    String exportProjectBundleCaption();

    /**
     * Translated "Up to Date".
     *
     * @return translated "Up to Date"
     */
    String upToDateCaption();

    /**
     * Translated "The Packrat library is up to date.".
     *
     * @return translated "The Packrat library is up to date."
     */
    String packratLibraryUpToDate();

    /**
     * Translated "Error checking Packrat library status".
     *
     * @return translated "Error checking Packrat library status"
     */
    String errorCheckingPackrat();

    /**
     * Translated "Error during {0}".
     *
     * @return translated "Error during {0}"
     */
    String errorCheckingPackratAction(String action);

    /**
     * Translated "Performing {0}...".
     *
     * @return translated "Performing {0}..."
     */
    String renvActionOnProgressMessage(String action);

    /**
     * Translated "The project is already up to date.".
     *
     * @return translated "The project is already up to date."
     */
    String projectUpToDateMessage();

    /**
     * Translated "Are you sure you wish to permanently uninstall the ''{0}'' package".
     *
     * @return translated "Are you sure you wish to permanently uninstall the ''{0}'' package"
     */
    String uninstallPackage(String packageName);

    /**
     * Translated "from library ''{0}''".
     *
     * @return translated "from library ''{0}''"
     */
    String libraryMessage(String library);

    /**
     * Translated "? This action cannot be undone.".
     *
     * @return translated "? This action cannot be undone."
     */
    String actionCannotBeUndoneMessage();

    /**
     * Translated "Uninstall Package ".
     *
     * @return translated "Uninstall Package "
     */
    String uninstallPackageCaption();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String errorCaption();

    /**
     * Translated "Retrieving package installation context...".
     *
     * @return translated "Retrieving package installation context..."
     */
    String retrievingPackageInstallationMessage();

    /**
     * Translated "One or more of the packages to be updated are currently loaded. Restarting R prior to install is highly recommended.\n\nRStudio can restart R before installing the requested packages. All work and data will be preserved during restart.\n\nDo you want to restart R prior to install?".
     *
     * @return translated "One or more of the packages to be updated are currently loaded. Restarting R prior to install is highly recommended.\n\nRStudio can restart R before installing the requested packages. All work and data will be preserved during restart.\n\nDo you want to restart R prior to install?"
     */
    String restartForInstallWithConfirmation();

    /**
     * Translated "Updating Loaded Packages".
     *
     * @return translated "Updating Loaded Packages"
     */
    String updatingLoadedPackagesCaption();

    /**
     * Translated "Error Listing Packages".
     *
     * @return translated "Error Listing Packages"
     */
    String errorListingPackagesCaption();

    /**
     * Translated "Packages".
     *
     * @return translated "Packages"
     */
    String packagesTitle();

    /**
     * Translated "Packages Tab".
     *
     * @return translated "Packages Tab"
     */
    String packagesTabLabel();

    /**
     * Translated "Filter by package name".
     *
     * @return translated "Filter by package name"
     */
    String filterByPackageNameLabel();

    /**
     * Translated "Browse package on CRAN".
     *
     * @return translated "Browse package on CRAN"
     */
    String browsePackageCRANLabel();

    /**
     * Translated "Browse package on Bioconductor".
     *
     * @return translated "Browse package on Bioconductor"
     */
    String browsePackageBioconductorLabel();

    /**
     * Translated "Browse package [{0}]".
     *
     * @return translated "Browse package [{0}]"
     */
    String browsePackageLabel(String browseUrl);

    /**
     * Translated "Browse package on {0} [{1}]"
     * 
     * @return translated "Browse package on {0} [{1}]"
     */
    String browsePackageOn(String remoteType, String browseUrl);

    /**
     * Translated "Browse package on GitHub".
     *
     * @return translated "Browse package on GitHub"
     */
    String browsePackageGitHubLabel();

    /**
     * Translated "Remove package".
     *
     * @return translated "Remove package"
     */
    String removePackageTitle();

    /**
     * Translated "Name".
     *
     * @return translated "Name"
     */
    String nameText();

    /**
     * Translated "Description".
     *
     * @return translated "Description"
     */
    String descriptionText();

    /**
     * Translated "Version".
     *
     * @return translated "Version"
     */
    String versionText();

    /**
     * Translated "Lockfile".
     *
     * @return translated "Lockfile"
     */
    String lockfileText();

    /**
     * Translated "Source".
     *
     * @return translated "Source"
     */
    String sourceText();

    /**
     * Translated "Package Not Loaded".
     *
     * @return translated "Package Not Loaded"
     */
    String packageNotLoadedCaption();

    /**
     * Translated "The package ''{0}'' cannot be loaded because it is not installed. Install the package to make it available for loading.".
     *
     * @return translated "The package ''{0}'' cannot be loaded because it is not installed. Install the package to make it available for loading."
     */
    String packageNotLoadedMessage(String packageName);

    /**
     * Translated "Help Not Available".
     *
     * @return translated "Help Not Available"
     */
    String helpNotAvailableCaption();

    /**
     * Translated "The package ''{0}'' is not installed. Install the package to make its help content available.".
     *
     * @return translated "The package ''{0}'' is not installed. Install the package to make its help content available."
     */
    String helpNotAvailableMessage(String packageName);

    /**
     * Translated "Project Library".
     *
     * @return translated "Project Library"
     */
    String projectLibraryText();

    /**
     * Translated "User Library".
     *
     * @return translated "User Library"
     */
    String userLibraryText();


    /**
     * Translated "System Library".
     *
     * @return translated "System Library"
     */
    String systemLibraryText();

    /**
     * Translated "System Library".
     *
     * @return translated "Library"
     */
    String libraryText();

    /**
     * Translated "Update Packages".
     *
     * @return translated "Update Packages"
     */
    String updatePackagesCaption();

    /**
     * Translated "Install Updates".
     *
     * @return translated "Install Updates"
     */
    String installUpdatesCaption();

    /**
     * Translated "Package".
     *
     * @return translated "Package"
     */
    String packageHeader();

    /**
     * Translated "Installed".
     *
     * @return translated "Installed"
     */
    String installedHeader();

    /**
     * Translated "Available".
     *
     * @return translated "Available"
     */
    String availableHeader();

    /**
     * Translated "Opening NEWS...".
     *
     * @return translated "Opening NEWS..."
     */
    String openingNewsProgressMessage();

    /**
     * Translated "Show package NEWS".
     *
     * @return translated "Show package NEWS"
     */
    String showPackageNewsTitle();

    /**
     * Translated "NEWS".
     *
     * @return translated "NEWS"
     */
    String newsHeader();

    /**
     * Translated "Error Opening NEWS".
     *
     * @return translated "Error Opening NEWS"
     */
    String errorOpeningNewsCaption();

    /**
     * Translated "This package does not have a NEWS file or RStudio was unable to determine an appropriate NEWS URL for this package.".
     *
     * @return translated "This package does not have a NEWS file or RStudio was unable to determine an appropriate NEWS URL for this package."
     */
    String errorOpeningNewsMessage();

    /**
     * Translated "Clean Unused Packages".
     *
     * @return translated "Clean Unused Packages"
     */
    String cleanUnusedPackagesCaption();

    /**
     * Translated "Remove Packages".
     *
     * @return translated "Remove Packages"
     */
    String removePackagesCaption();

    /**
     * Translated "Packrat Clean".
     *
     * @return translated "Packrat Clean"
     */
    String packratCleanCaption();

    /**
     * Translated "No unused packages were found in the library.".
     *
     * @return translated "No unused packages were found in the library."
     */
    String packratCleanMessage();

    /**
     * Translated "These packages are present in your library, but do not appear to be used by code in your project. Select any you''d like to clean up.".
     *
     * @return translated "These packages are present in your library, but do not appear to be used by code in your project. Select any you''d like to clean up."
     */
    String explanatoryMessage();

    /**
     * Translated "Install".
     *
     * @return translated "Install"
     */
    String installButtonCaption();

    /**
     * Translated "No Package Selected".
     *
     * @return translated "No Package Selected"
     */
    String noPackageSelectedCaption();

    /**
     * Translated "You must specify one or more packages to install.".
     *
     * @return translated "You must specify one or more packages to install."
     */
    String noPackageSelectedMessage();

    /**
     * Translated "Package Repository ({0})".
     *
     * @return translated "Package Repository ({0})"
     */
    String repositoryLabel(String repo);

    /**
     * Translated "Package Repository (".
     *
     * @return translated "Package Repository ("
     */
    String repositoryItemLabel();

    /**
     * Translated "Package Archive File (".
     *
     * @return translated "Package Archive File ("
     */
    String packageArchiveFileLabel();

    /**
     * Translated "Install from:".
     *
     * @return translated "Install from:"
     */
    String installFromCaption();

    /**
     * Translated "Configuring Repositories".
     *
     * @return translated "Configuring Repositories"
     */
    String configuringRepositoriesHelpCaption();

    /**
     * Translated "Packages (separate multiple with space or comma):".
     *
     * @return translated "Packages (separate multiple with space or comma):"
     */
    String packagesLabel();

    /**
     * Translated "Package archive:".
     *
     * @return translated "Package archive:"
     */
    String packageArchiveLabel();

    /**
     * Translated "Browse...".
     *
     * @return translated "Browse..."
     */
    String browseActionLabel();

    /**
     * Translated "Install to Library:".
     *
     * @return translated "Install to Library:"
     */
    String installToLibraryText();

    /**
     * Translated "Install dependencies".
     *
     * @return translated "Install dependencies"
     */
    String installDependenciesText();

    /**
     * Translated "Select Package Archive".
     *
     * @return translated "Select Package Archive"
     */
    String selectPackageArchiveCaption();

    /**
     * Translated "Select All".
     *
     * @return translated "Select All"
     */
    String selectAllLabel();

    /**
     * Translated "Select None".
     *
     * @return translated "Select None"
     */
    String selectNoneLabel();

}
