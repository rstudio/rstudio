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
    String createPackageLibraryCaption();
    String createPackageLibraryMessage(String libraryPath);
    String errorCreatingLibraryCaption();
    String installPackagesCaption();
    String unableToInstallPackagesMessage(String libraryPath);
    String checkForUpdatesCaption();
    String checkForUpdatesMessage();
    String exportProjectBundleCaption();
    String upToDateCaption();
    String packratLibraryUpToDate();
    String errorCheckingPackrat();
    String errorCheckingPackratAction(String action);
    String renvActionOnProgressMessage(String action);
    String projectUpToDateMessage();
    String uninstallPackage(String packageName);
    String libraryMessage(String library);
    String actionCannotBeUndoneMessage();
    String uninstallPackageCaption();
    String errorCaption();
    String retrievingPackageInstallationMessage();
    String restartForInstallWithConfirmation();
    String updatingLoadedPackagesCaption();
    String errorListingPackagesCaption();
    String packagesTitle();
    String packagesTabLabel();
    String filterByPackageNameLabel();
    String browsePackageCRANLabel();
    String browsePackageBioconductorLabel();
    String browsePackageLabel(String browseUrl);
    String browsePackageOn(String remoteType, String browseUrl);
    String browsePackageGitHubLabel();
    String removePackageTitle();
    String nameText();
    String descriptionText();
    String versionText();
    String lockfileText();
    String sourceText();
    String packageNotLoadedCaption();
    String packageNotLoadedMessage(String packageName);
    String helpNotAvailableCaption();
    String helpNotAvailableMessage(String packageName);
    String projectLibraryText();
    String userLibraryText();
    String systemLibraryText();
    String libraryText();
    String updatePackagesCaption();
    String installUpdatesCaption();
    String packageHeader();
    String installedHeader();
    String availableHeader();
    String openingNewsProgressMessage();
    String showPackageNewsTitle();
    String newsHeader();
    String errorOpeningNewsCaption();
    String errorOpeningNewsMessage();
    String cleanUnusedPackagesCaption();
    String removePackagesCaption();
    String packratCleanCaption();
    String packratCleanMessage();
    String explanatoryMessage();
    String installButtonCaption();
    String noPackageSelectedCaption();
    String noPackageSelectedMessage();
    String repositoryLabel(String repo);
    String repositoryItemLabel();
    String packageArchiveFileLabel();
    String installFromCaption();
    String configuringRepositoriesHelpCaption();
    String packagesLabel();
    String packageArchiveLabel();
    String browseActionLabel();
    String installToLibraryText();
    String installDependenciesText();
    String selectPackageArchiveCaption();
    String selectAllLabel();
    String selectNoneLabel();
}
