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

    @Key("createPackageLibraryCaption")
    String createPackageLibraryCaption();

    @Key("createPackageLibraryMessage")
    String createPackageLibraryMessage(String libraryPath);

    @Key("errorCreatingLibraryCaption")
    String errorCreatingLibraryCaption();

    @Key("installPackagesCaption")
    String installPackagesCaption();

    @Key("unableToInstallPackagesMessage")
    String unableToInstallPackagesMessage(String libraryPath);

    @Key("checkForUpdatesCaption")
    String checkForUpdatesCaption();

    @Key("checkForUpdatesMessage")
    String checkForUpdatesMessage();

    @Key("exportProjectBundleCaption")
    String exportProjectBundleCaption();

    @Key("upToDateCaption")
    String upToDateCaption();

    @Key("packratLibraryUpToDate")
    String packratLibraryUpToDate();

    @Key("errorCheckingPackrat")
    String errorCheckingPackrat();

    @Key("errorMessage")
    String errorCheckingPackrat(String action);

    @Key("renvActionOnProgressMessage")
    String renvActionOnProgressMessage(String action);

    @Key("projectUpToDateMessage")
    String projectUpToDateMessage();

    @Key("uninstallPackage")
    String uninstallPackage(String packageName);

    @Key("libraryMessage")
    String libraryMessage(String library);

    @Key("actionCannotBeUndoneMessage")
    String actionCannotBeUndoneMessage();

    @Key("uninstallPackageCaption")
    String uninstallPackageCaption();

    @Key("errorCaption")
    String errorCaption();

    @Key("retrievingPackageInstallationMessage")
    String retrievingPackageInstallationMessage();

    @Key("restartForInstallWithConfirmation")
    String restartForInstallWithConfirmation();

    @Key("updatingLoadedPackagesCaption")
    String updatingLoadedPackagesCaption();

    @Key("errorListingPackagesCaption")
    String errorListingPackagesCaption();

    @Key("packagesTitle")
    String packagesTitle();

    @Key("packagesTabLabel")
    String packagesTabLabel();

    @Key("filterByPackageNameLabel")
    String filterByPackageNameLabel();

    @Key("browsePackageCRANLabel")
    String browsePackageCRANLabel();

    @Key("browsePackageBioconductorLabel")
    String browsePackageBioconductorLabel();

    @Key("brosePackageLabel")
    String browsePackageLabel(String browseUrl);

    @Key("browsePackageOn")
    String browsePackageOn(String remoteType, String browseUrl);

    @Key("browsePackageGitHubLabel")
    String browsePackageGitHubLabel();

    @Key("removePackageTitle")
    String removePackageTitle();

    @Key("nameText")
    String nameText();

    @Key("descriptionText")
    String descriptionText();

    @Key("versionText")
    String versionText();

    @Key("lockfileText")
    String lockfileText();

    @Key("sourceText")
    String sourceText();

    @Key("packageNotLoadedCaption")
    String packageNotLoadedCaption();

    @Key("packageNotLoadedMessage")
    String packageNotLoadedMessage(String packageName);

    @Key("helpNotAvailableCaption")
    String helpNotAvailableCaption();

    @Key("helpNotAvailableMessage")
    String helpNotAvailableMessage(String packageName);

    @Key("projectLibraryText")
    String projectLibraryText();

    @Key("userLibraryText")
    String userLibraryText();

    @Key("systemLibraryText")
    String systemLibraryText();

    @Key("libraryText")
    String libraryText();

    @Key("updatePackagesCaption")
    String updatePackagesCaption();

    @Key("installUpdatesCaption")
    String installUpdatesCaption();

    @Key("packageHeader")
    String packageHeader();

    @Key("installedHeader")
    String installedHeader();

    @Key("availableHeader")
    String availableHeader();

    @Key("openingNewsProgressMessage")
    String openingNewsProgressMessage();

    @Key("showPackageNewsTitle")
    String showPackageNewsTitle();

    @Key("newsHeader")
    String newsHeader();

    @Key("errorOpeningNewsCaption")
    String errorOpeningNewsCaption();

    @Key("errorOpeningNewsMessage")
    String errorOpeningNewsMessage();

    @Key("cleanUnusedPackagesCaption")
    String cleanUnusedPackagesCaption();

    @Key("removePackagesCaption")
    String removePackagesCaption();

    @Key("packratCleanCaption")
    String packratCleanCaption();

    @Key("packratCleanMessage")
    String packratCleanMessage();

    @Key("explanatoryMessage")
    String explanatoryMessage();

    @Key("installButtonCaption")
    String installButtonCaption();

    @Key("noPackageSelectedCaption")
    String noPackageSelectedCaption();

    @Key("noPackageSelectedMessage")
    String noPackageSelectedMessage();

    @Key("repositoryLabel")
    String repositoryLabel(String repo);

    @Key("repositoryItemLabel")
    String repositoryItemLabel();

    @Key("packageArchiveFileLabel")
    String packageArchiveFileLabel();

    @Key("installFromCaption")
    String installFromCaption();

    @Key("configuringRepositoriesHelpCaption")
    String configuringRepositoriesHelpCaption();

    @Key("packagesLabel")
    String packagesLabel();

    @Key("packageArchiveLabel")
    String packageArchiveLabel();

    @Key("browseActionLabel")
    String browseActionLabel();

    @Key("installToLibraryText")
    String installToLibraryText();

    @Key("installDependenciesText")
    String installDependenciesText();

    @Key("selectPackageArchiveCaption")
    String selectPackageArchiveCaption();

    @Key("selectAllLabel")
    String selectAllLabel();

    @Key("selectNoneLabel")
    String selectNoneLabel();

}
