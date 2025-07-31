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

    @DefaultMessage("Create Package Library")
    @Key("createPackageLibraryCaption")
    String createPackageLibraryCaption();

    @DefaultMessage("Would you like to create a personal library ''{0}'' to install packages into?")
    @Key("createPackageLibraryMessage")
    String createPackageLibraryMessage(String libraryPath);

    @DefaultMessage("Error Creating Library")
    @Key("errorCreatingLibraryCaption")
    String errorCreatingLibraryCaption();

    @DefaultMessage("Install Packages")
    @Key("installPackagesCaption")
    String installPackagesCaption();

    @DefaultMessage("Unable to install packages (default library ''{0}'' is not writeable)")
    @Key("unableToInstallPackagesMessage")
    String unableToInstallPackagesMessage(String libraryPath);

    @DefaultMessage("Check for Updates")
    @Key("checkForUpdatesCaption")
    String checkForUpdatesCaption();

    @DefaultMessage("All packages are up to date.")
    @Key("checkForUpdatesMessage")
    String checkForUpdatesMessage();

    @DefaultMessage("Export Project Bundle to Gzipped Tarball")
    @Key("exportProjectBundleCaption")
    String exportProjectBundleCaption();

    @DefaultMessage("Up to Date")
    @Key("upToDateCaption")
    String upToDateCaption();

    @DefaultMessage("The Packrat library is up to date.")
    @Key("packratLibraryUpToDate")
    String packratLibraryUpToDate();

    @DefaultMessage("Error checking Packrat library status")
    @Key("errorCheckingPackrat")
    String errorCheckingPackrat();

    @DefaultMessage("Error during {0}")
    @Key("errorMessage")
    String errorCheckingPackrat(String action);

    @DefaultMessage("Performing {0}...")
    @Key("renvActionOnProgressMessage")
    String renvActionOnProgressMessage(String action);

    @DefaultMessage("The project is already up to date.")
    @Key("projectUpToDateMessage")
    String projectUpToDateMessage();

    @DefaultMessage("Are you sure you wish to permanently uninstall the ''{0}'' package")
    @Key("uninstallPackage")
    String uninstallPackage(String packageName);

    @DefaultMessage("from library ''{0}''")
    @Key("libraryMessage")
    String libraryMessage(String library);

    @DefaultMessage("? This action cannot be undone.")
    @Key("actionCannotBeUndoneMessage")
    String actionCannotBeUndoneMessage();

    @DefaultMessage("Uninstall Package ")
    @Key("uninstallPackageCaption")
    String uninstallPackageCaption();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("Retrieving package installation context...")
    @Key("retrievingPackageInstallationMessage")
    String retrievingPackageInstallationMessage();

    @DefaultMessage("One or more of the packages to be updated are currently loaded. Restarting R prior to install is highly recommended.\\n\\nRStudio can restart R before installing the requested packages. All work and data will be preserved during restart.\\n\\nDo you want to restart R prior to install?")
    @Key("restartForInstallWithConfirmation")
    String restartForInstallWithConfirmation();

    @DefaultMessage("Updating Loaded Packages")
    @Key("updatingLoadedPackagesCaption")
    String updatingLoadedPackagesCaption();

    @DefaultMessage("Error Listing Packages")
    @Key("errorListingPackagesCaption")
    String errorListingPackagesCaption();

    @DefaultMessage("Packages")
    @Key("packagesTitle")
    String packagesTitle();

    @DefaultMessage("Packages Tab")
    @Key("packagesTabLabel")
    String packagesTabLabel();

    @DefaultMessage("Filter by package name")
    @Key("filterByPackageNameLabel")
    String filterByPackageNameLabel();

    @DefaultMessage("Browse package on CRAN")
    @Key("browsePackageCRANLabel")
    String browsePackageCRANLabel();

    @DefaultMessage("Browse package on CRAN")
    @Key("browsePackageBioconductorLabel")
    String browsePackageBioconductorLabel();

    @DefaultMessage("Browse package [{0}]")
    @Key("brosePackageLabel")
    String browsePackageLabel(String browseUrl);

    @DefaultMessage("Browse package on {0} [{1}]")
    @Key("browsePackageOn")
    String browsePackageOn(String remoteType, String browseUrl);

    @DefaultMessage("Browse package on GitHub")
    @Key("browsePackageGitHubLabel")
    String browsePackageGitHubLabel();

    @DefaultMessage("Remove package")
    @Key("removePackageTitle")
    String removePackageTitle();

    @DefaultMessage("Name")
    @Key("nameText")
    String nameText();

    @DefaultMessage("Description")
    @Key("descriptionText")
    String descriptionText();

    @DefaultMessage("Version")
    @Key("versionText")
    String versionText();

    @DefaultMessage("Lockfile")
    @Key("lockfileText")
    String lockfileText();

    @DefaultMessage("Source")
    @Key("sourceText")
    String sourceText();

    @DefaultMessage("Package Not Loaded")
    @Key("packageNotLoadedCaption")
    String packageNotLoadedCaption();

    @DefaultMessage("The package ''{0}'' cannot be loaded because it is not installed. Install the package to make it available for loading.")
    @Key("packageNotLoadedMessage")
    String packageNotLoadedMessage(String packageName);

    @DefaultMessage("Help Not Available")
    @Key("helpNotAvailableCaption")
    String helpNotAvailableCaption();

    @DefaultMessage("The package ''{0}'' is not installed. Install the package to make its help content available.")
    @Key("helpNotAvailableMessage")
    String helpNotAvailableMessage(String packageName);

    @DefaultMessage("Project Library")
    @Key("projectLibraryText")
    String projectLibraryText();

    @DefaultMessage("User Library")
    @Key("userLibraryText")
    String userLibraryText();

    @DefaultMessage("System Library")
    @Key("systemLibraryText")
    String systemLibraryText();

    @DefaultMessage("Library")
    @Key("libraryText")
    String libraryText();

    @DefaultMessage("Update Packages")
    @Key("updatePackagesCaption")
    String updatePackagesCaption();

    @DefaultMessage("Install Updates")
    @Key("installUpdatesCaption")
    String installUpdatesCaption();

    @DefaultMessage("Package")
    @Key("packageHeader")
    String packageHeader();

    @DefaultMessage("Installed")
    @Key("installedHeader")
    String installedHeader();

    @DefaultMessage("Available")
    @Key("availableHeader")
    String availableHeader();

    @DefaultMessage("Opening NEWS...")
    @Key("openingNewsProgressMessage")
    String openingNewsProgressMessage();

    @DefaultMessage("Show package NEWS")
    @Key("showPackageNewsTitle")
    String showPackageNewsTitle();

    @DefaultMessage("NEWS")
    @Key("newsHeader")
    String newsHeader();

    @DefaultMessage("Error Opening NEWS")
    @Key("errorOpeningNewsCaption")
    String errorOpeningNewsCaption();

    @DefaultMessage("This package does not have a NEWS file or RStudio was unable to determine an appropriate NEWS URL for this package.")
    @Key("errorOpeningNewsMessage")
    String errorOpeningNewsMessage();

    @DefaultMessage("Clean Unused Packages")
    @Key("cleanUnusedPackagesCaption")
    String cleanUnusedPackagesCaption();

    @DefaultMessage("Remove Packages")
    @Key("removePackagesCaption")
    String removePackagesCaption();

    @DefaultMessage("Packrat Clean")
    @Key("packratCleanCaption")
    String packratCleanCaption();

    @DefaultMessage("No unused packages were found in the library.")
    @Key("packratCleanMessage")
    String packratCleanMessage();

    @DefaultMessage("These packages are present in your library, but do not appear to be used by code in your project. Select any you''d like to clean up.")
    @Key("explanatoryMessage")
    String explanatoryMessage();

    @DefaultMessage("Install")
    @Key("installButtonCaption")
    String installButtonCaption();

    @DefaultMessage("No Package Selected")
    @Key("noPackageSelectedCaption")
    String noPackageSelectedCaption();

    @DefaultMessage("You must specify one or more packages to install.")
    @Key("noPackageSelectedMessage")
    String noPackageSelectedMessage();

    @DefaultMessage("Package Repository ({0})")
    @Key("repositoryLabel")
    String repositoryLabel(String repo);

    @DefaultMessage("Package Repository (")
    @Key("repositoryItemLabel")
    String repositoryItemLabel();

    @DefaultMessage("Package Archive File (")
    @Key("packageArchiveFileLabel")
    String packageArchiveFileLabel();

    @DefaultMessage("Install from:")
    @Key("installFromCaption")
    String installFromCaption();

    @DefaultMessage("Configuring Repositories")
    @Key("configuringRepositoriesHelpCaption")
    String configuringRepositoriesHelpCaption();

    @DefaultMessage("Packages (separate multiple with space or comma):")
    @Key("packagesLabel")
    String packagesLabel();

    @DefaultMessage("Package archive:")
    @Key("packageArchiveLabel")
    String packageArchiveLabel();

    @DefaultMessage("Browse...")
    @Key("browseActionLabel")
    String browseActionLabel();

    @DefaultMessage("Install to Library:")
    @Key("installToLibraryText")
    String installToLibraryText();

    @DefaultMessage("Install dependencies")
    @Key("installDependenciesText")
    String installDependenciesText();

    @DefaultMessage("Select Package Archive")
    @Key("selectPackageArchiveCaption")
    String selectPackageArchiveCaption();

    @DefaultMessage("Select All")
    @Key("selectAllLabel")
    String selectAllLabel();

    @DefaultMessage("Select None")
    @Key("selectNoneLabel")
    String selectNoneLabel();

}
