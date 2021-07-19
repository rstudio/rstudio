package org.rstudio.studio.client.workbench.prefs.views;

public interface PackagesPreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "Package Management".
     *
     * @return translated "Package Management"
     */
    @DefaultStringValue("Package Management")
    @Key("packageManagementTitle")
    String packageManagementTitle();

    /**
     * Translated "CRAN repositories modified outside package preferences.".
     *
     * @return translated "CRAN repositories modified outside package preferences."
     */
    @DefaultStringValue("CRAN repositories modified outside package preferences.")
    @Key("packagesInfoBarText")
    String packagesInfoBarText();

    /**
     * Translated "Primary CRAN repository:".
     *
     * @return translated "Primary CRAN repository:"
     */
    @DefaultStringValue("Primary CRAN repository:")
    @Key("cranMirrorTextBoxTitle")
    String cranMirrorTextBoxTitle();

    /**
     * Translated "Change...".
     *
     * @return translated "Change..."
     */
    @DefaultStringValue("Change...")
    @Key("cranMirrorChangeLabel")
    String cranMirrorChangeLabel();

    /**
     * Translated "Secondary repositories:".
     *
     * @return translated "Secondary repositories:"
     */
    @DefaultStringValue("Secondary repositories:")
    @Key("secondaryReposTitle")
    String secondaryReposTitle();

    /**
     * Translated "Enable packages pane".
     *
     * @return translated "Enable packages pane"
     */
    @DefaultStringValue("Enable packages pane")
    @Key("chkEnablePackagesTitle")
    String chkEnablePackagesTitle();

    /**
     * Translated "Use secure download method for HTTP".
     *
     * @return translated "Use secure download method for HTTP"
     */
    @DefaultStringValue("Use secure download method for HTTP")
    @Key("useSecurePackageDownloadTitle")
    String useSecurePackageDownloadTitle();

    /**
     * Translated "secure_download".
     *
     * @return translated "secure_download"
     */
    @DefaultStringValue("secure_download")
    @Key("useSecurePackageTopic")
    String useSecurePackageTopic();

    /**
     * Translated "Help on secure package downloads for R".
     *
     * @return translated "Help on secure package downloads for R"
     */
    @DefaultStringValue("Help on secure package downloads for R")
    @Key("useSecurePackageTitle")
    String useSecurePackageTitle();

    /**
     * Translated "Use Internet Explorer library/proxy for HTTP".
     *
     * @return translated "Use Internet Explorer library/proxy for HTTP"
     */
    @DefaultStringValue("Use Internet Explorer library/proxy for HTTP")
    @Key("useInternetTitle")
    String useInternetTitle();

    /**
     * Translated "Managing Packages".
     *
     * @return translated "Managing Packages"
     */
    @DefaultStringValue("Managing Packages")
    @Key("managePackagesTitle")
    String managePackagesTitle();

    /**
     * Translated "Package Development".
     *
     * @return translated "Package Development"
     */
    @DefaultStringValue("Package Development")
    @Key("developmentTitle")
    String developmentTitle();

    /**
     * Translated "Use devtools package functions if available".
     *
     * @return translated "Use devtools package functions if available"
     */
    @DefaultStringValue("Use devtools package functions if available")
    @Key("useDevtoolsLabel")
    String useDevtoolsLabel();

    /**
     * Translated "Save all files prior to building packages".
     *
     * @return translated "Save all files prior to building packages"
     */
    @DefaultStringValue("Save all files prior to building packages")
    @Key("developmentSaveLabel")
    String developmentSaveLabel();

    /**
     * Translated "Automatically navigate editor to build errors".
     *
     * @return translated "Automatically navigate editor to build errors"
     */
    @DefaultStringValue("Automatically navigate editor to build errors")
    @Key("developmentNavigateLabel")
    String developmentNavigateLabel();

    /**
     * Translated "Hide object files in package src directory".
     *
     * @return translated "Hide object files in package src directory"
     */
    @DefaultStringValue("Hide object files in package src directory")
    @Key("developmentHideLabel")
    String developmentHideLabel();

    /**
     * Translated "Cleanup output after successful R CMD check".
     *
     * @return translated "Cleanup output after successful R CMD check"
     */
    @DefaultStringValue("Cleanup output after successful R CMD check")
    @Key("developmentCleanupLabel")
    String developmentCleanupLabel();

    /**
     * Translated "View Rcheck directory after failed R CMD check".
     *
     * @return translated "View Rcheck directory after failed R CMD check"
     */
    @DefaultStringValue("View Rcheck directory after failed R CMD check")
    @Key("developmentViewLabel")
    String developmentViewLabel();

    /**
     * Translated "Use Rcpp template when creating C++ files".
     *
     * @return translated "Use Rcpp template when creating C++ files"
     */
    @DefaultStringValue("Use Rcpp template when creating C++ files")
    @Key("developmentRcppLabel")
    String developmentRcppLabel();

    /**
     * Translated "Always use LF line-endings in Unix Makefiles".
     *
     * @return translated "Always use LF line-endings in Unix Makefiles"
     */
    @DefaultStringValue("Always use LF line-endings in Unix Makefiles")
    @Key("developmentUseLFLabel")
    String developmentUseLFLabel();

    /**
     * Translated "Packages".
     *
     * @return translated "Packages"
     */
    @DefaultStringValue("Packages")
    @Key("tabPackagesPanelTitle")
    String tabPackagesPanelTitle();

    /**
     * Translated "Management".
     *
     * @return translated "Management"
     */
    @DefaultStringValue("Management")
    @Key("managementPanelTitle")
    String managementPanelTitle();

    /**
     * Translated "Development".
     *
     * @return translated "Development"
     */
    @DefaultStringValue("Development")
    @Key("developmentManagementPanelTitle")
    String developmentManagementPanelTitle();

    /**
     * Translated "Restart R Required".
     *
     * @return translated "Restart R Required"
     */
    @DefaultStringValue("Restart R Required")
    @Key("cranMirrorTextBoxRestartCaption")
    String cranMirrorTextBoxRestartCaption();

    /**
     * Translated "You must restart your R session for this setting ".
     *
     * @return translated "You must restart your R session for this setting "
     */
    @DefaultStringValue("You must restart your R session for this setting ")
    @Key("cranMirrorTextBoxRestartMessage")
    String cranMirrorTextBoxRestartMessage();

    /**
     * Translated "to take effect.".
     *
     * @return translated "to take effect."
     */
    @DefaultStringValue("to take effect.")
    @Key("cranMirrorTextBoxMessage")
    String cranMirrorTextBoxMessage();

    /**
     * Translated "Retrieving list of CRAN mirrors...".
     *
     * @return translated "Retrieving list of CRAN mirrors..."
     */
    @DefaultStringValue("Retrieving list of CRAN mirrors...")
    @Key("chooseMirrorDialogMessage")
    String chooseMirrorDialogMessage();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultStringValue("Error")
    @Key("showErrorCaption")
    String showErrorCaption();

    /**
     * Translated "Please select a CRAN Mirror".
     *
     * @return translated "Please select a CRAN Mirror"
     */
    @DefaultStringValue("Please select a CRAN Mirror")
    @Key("showErrorMessage")
    String showErrorMessage();

    /**
     * Translated "Validating CRAN repository...".
     *
     * @return translated "Validating CRAN repository..."
     */
    @DefaultStringValue("Validating CRAN repository...")
    @Key("progressIndicatorMessage")
    String progressIndicatorMessage();

    /**
     * Translated "The given URL does not appear to be a valid CRAN repository".
     *
     * @return translated "The given URL does not appear to be a valid CRAN repository"
     */
    @DefaultStringValue("The given URL does not appear to be a valid CRAN repository")
    @Key("progressIndicatorError")
    String progressIndicatorError();

    /**
     * Translated "Custom:".
     *
     * @return translated "Custom:"
     */
    @DefaultStringValue("Custom:")
    @Key("customLabel")
    String customLabel();

    /**
     * Translated "CRAN Mirrors:".
     *
     * @return translated "CRAN Mirrors:"
     */
    @DefaultStringValue("CRAN Mirrors:")
    @Key("mirrorsLabel")
    String mirrorsLabel();

    /**
     * Translated "Choose Primary Repository".
     *
     * @return translated "Choose Primary Repository"
     */
    @DefaultStringValue("Choose Primary Repository")
    @Key("headerLabel")
    String headerLabel();

    /**
     * Translated "Add...".
     *
     * @return translated "Add..."
     */
    @DefaultStringValue("Add...")
    @Key("buttonAddLabel")
    String buttonAddLabel();

    /**
     * Translated "Remove...".
     *
     * @return translated "Remove..."
     */
    @DefaultStringValue("Remove...")
    @Key("buttonRemoveLabel")
    String buttonRemoveLabel();

    /**
     * Translated "Up".
     *
     * @return translated "Up"
     */
    @DefaultStringValue("Up")
    @Key("buttonUpLabel")
    String buttonUpLabel();

    /**
     * Translated "Down".
     *
     * @return translated "Down"
     */
    @DefaultStringValue("Down")
    @Key("buttonDownLabel")
    String buttonDownLabel();

    /**
     * Translated "Developing Packages".
     *
     * @return translated "Developing Packages"
     */
    @DefaultStringValue("Developing Packages")
    @Key("developingPkgHelpLink")
    String developingPkgHelpLink();

    /**
     * Translated "Retrieving list of secondary repositories...".
     *
     * @return translated "Retrieving list of secondary repositories..."
     */
    @DefaultStringValue("Retrieving list of secondary repositories...")
    @Key("secondaryReposDialog")
    String secondaryReposDialog();

    /**
     * Translated "Please select or input a CRAN repository".
     *
     * @return translated "Please select or input a CRAN repository"
     */
    @DefaultStringValue("Please select or input a CRAN repository")
    @Key("validateSyncLabel")
    String validateSyncLabel();

    /**
     * Translated "The repository ".
     *
     * @return translated "The repository "
     */
    @DefaultStringValue("The repository ")
    @Key("showErrorRepoMessage")
    String showErrorRepoMessage();

    /**
     * Translated "is already included".
     *
     * @return translated "is already included"
     */
    @DefaultStringValue("is already included")
    @Key("alreadyIncludedMessage")
    String alreadyIncludedMessage();

    /**
     * Translated "Validating CRAN repository...".
     *
     * @return translated "Validating CRAN repository..."
     */
    @DefaultStringValue("Validating CRAN repository...")
    @Key("validateAsyncProgress")
    String validateAsyncProgress();

    /**
     * Translated "The given URL does not appear to be a valid CRAN repository".
     *
     * @return translated "The given URL does not appear to be a valid CRAN repository"
     */
    @DefaultStringValue("The given URL does not appear to be a valid CRAN repository")
    @Key("onResponseReceived")
    String onResponseReceived();

    /**
     * Translated "Name:".
     *
     * @return translated "Name:"
     */
    @DefaultStringValue("Name:")
    @Key("nameLabel")
    String nameLabel();

    /**
     * Translated "Url:".
     *
     * @return translated "Url:"
     */
    @DefaultStringValue("Url:")
    @Key("urlLabel")
    String urlLabel();

    /**
     * Translated "Available repositories:".
     *
     * @return translated "Available repositories:"
     */
    @DefaultStringValue("Available repositories:")
    @Key("reposLabel")
    String reposLabel();

    /**
     * Translated "Add Secondary Repository".
     *
     * @return translated "Add Secondary Repository"
     */
    @DefaultStringValue("Add Secondary Repository")
    @Key("secondaryRepoLabel")
    String secondaryRepoLabel();
}
