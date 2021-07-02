package org.rstudio.studio.client.workbench.prefs.views;

public interface PublishingPreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "Publishing Accounts".
     *
     * @return translated "Publishing Accounts"
     */
    @DefaultStringValue("Publishing Accounts")
    @Key("accountListLabel")
    String accountListLabel();

    /**
     * Translated "Connect...".
     *
     * @return translated "Connect..."
     */
    @DefaultStringValue("Connect...")
    @Key("connectButtonLabel")
    String connectButtonLabel();

    /**
     * Translated "Reconnect...".
     *
     * @return translated "Reconnect..."
     */
    @DefaultStringValue("Reconnect...")
    @Key("reconnectButtonLabel")
    String reconnectButtonLabel();

    /**
     * Translated "Disconnect".
     *
     * @return translated "Disconnect"
     */
    @DefaultStringValue("Disconnect")
    @Key("disconnectButtonLabel")
    String disconnectButtonLabel();

    /**
     * Translated "Account records appear to exist, but cannot be viewed because a ".
     *
     * @return translated "Account records appear to exist, but cannot be viewed because a "
     */
    @DefaultStringValue("Account records appear to exist, but cannot be viewed because a ")
    @Key("missingPkgPanelMessage")
    String missingPkgPanelMessage();

    /**
     * Translated "required package is not installed.".
     *
     * @return translated "required package is not installed."
     */
    @DefaultStringValue("required package is not installed.")
    @Key("missingPkgRequiredMessage")
    String missingPkgRequiredMessage();

    /**
     * Translated "Install Missing Packages".
     *
     * @return translated "Install Missing Packages"
     */
    @DefaultStringValue("Install Missing Packages")
    @Key("installPkgsMessage")
    String installPkgsMessage();

    /**
     * Translated "Viewing publish accounts".
     *
     * @return translated "Viewing publish accounts"
     */
    @DefaultStringValue("Viewing publish accounts")
    @Key("withRSConnectLabel")
    String withRSConnectLabel();

    /**
     * Translated "Enable publishing to RStudio Connect".
     *
     * @return translated "Enable publishing to RStudio Connect"
     */
    @DefaultStringValue("Enable publishing to RStudio Connect")
    @Key("chkEnableRSConnectLabel")
    String chkEnableRSConnectLabel();

    /**
     * Translated "Information about RStudio Connect".
     *
     * @return translated "Information about RStudio Connect"
     */
    @DefaultStringValue("Information about RStudio Connect")
    @Key("checkBoxWithHelpTitle")
    String checkBoxWithHelpTitle();

    /**
     * Translated "Settings".
     *
     * @return translated "Settings"
     */
    @DefaultStringValue("Settings")
    @Key("settingsHeaderLabel")
    String settingsHeaderLabel();

    /**
     * Translated "Enable publishing documents, apps, and APIs".
     *
     * @return translated "Enable publishing documents, apps, and APIs"
     */
    @DefaultStringValue("Enable publishing documents, apps, and APIs")
    @Key("chkEnablePublishingLabel")
    String chkEnablePublishingLabel();

    /**
     * Translated "Show diagnostic information when publishing".
     *
     * @return translated "Show diagnostic information when publishing"
     */
    @DefaultStringValue("Show diagnostic information when publishing")
    @Key("showPublishDiagnosticsLabel")
    String showPublishDiagnosticsLabel();

    /**
     * Translated "SSL Certificates".
     *
     * @return translated "SSL Certificates"
     */
    @DefaultStringValue("SSL Certificates")
    @Key("sSLCertificatesHeaderLabel")
    String sSLCertificatesHeaderLabel();

    /**
     * Translated "Check SSL certificates when publishing".
     *
     * @return translated "Check SSL certificates when publishing"
     */
    @DefaultStringValue("Check SSL certificates when publishing")
    @Key("publishCheckCertificatesLabel")
    String publishCheckCertificatesLabel();

    /**
     * Translated "Check SSL certificates when publishing".
     *
     * @return translated "Check SSL certificates when publishing"
     */
    @DefaultStringValue("Use custom CA bundle")
    @Key("usePublishCaBundleLabel")
    String usePublishCaBundleLabel();

    /**
     * Translated "(none)".
     *
     * @return translated "(none)"
     */
    @DefaultStringValue("(none)")
    @Key("caBundlePath")
    String caBundlePath();

    /**
     * Translated "Troubleshooting Deployments".
     *
     * @return translated "Troubleshooting Deployments"
     */
    @DefaultStringValue("Troubleshooting Deployments")
    @Key("helpLinkTroubleshooting")
    String helpLinkTroubleshooting();

    /**
     * Translated "Publishing".
     *
     * @return translated "Publishing"
     */
    @DefaultStringValue("Publishing")
    @Key("publishingPaneHeader")
    String publishingPaneHeader();

    /**
     * Translated "Error Disconnecting Account".
     *
     * @return translated "Error Disconnecting Account"
     */
    @DefaultStringValue("Error Disconnecting Account")
    @Key("showErrorCaption")
    String showErrorCaption();

    /**
     * Translated "Please select an account to disconnect.".
     *
     * @return translated "Please select an account to disconnect."
     */
    @DefaultStringValue("Please select an account to disconnect.")
    @Key("showErrorMessage")
    String showErrorMessage();

    /**
     * Translated "Confirm Remove Account".
     *
     * @return translated "Confirm Remove Account"
     */
    @DefaultStringValue("Confirm Remove Account")
    @Key("removeAccountGlobalDisplay")
    String removeAccountGlobalDisplay();

    /**
     * Translated "Are you sure you want to disconnect the '".
     *
     * @return translated "Are you sure you want to disconnect the '"
     */
    @DefaultStringValue("Are you sure you want to disconnect the '")
    @Key("removeAccountMessage")
    String removeAccountMessage();

    /**
     * Translated "' account on '".
     *
     * @return translated "' account on '"
     */
    @DefaultStringValue("' account on '")
    @Key("removeAccountOnMessage")
    String removeAccountOnMessage();

    /**
     * Translated "? This won't delete the account on the server.".
     *
     * @return translated "? This won't delete the account on the server."
     */
    @DefaultStringValue("? This won't delete the account on the server.")
    @Key("willNotDeleteMessage")
    String willNotDeleteMessage();

    /**
     * Translated "Disconnect Account".
     *
     * @return translated "Disconnect Account"
     */
    @DefaultStringValue("Disconnect Account")
    @Key("onConfirmDisconnectYesLabel")
    String onConfirmDisconnectYesLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultStringValue("Cancel")
    @Key("onConfirmDisconnectNoLabel")
    String onConfirmDisconnectNoLabel();

    /**
     * Translated "Error Disconnecting Account".
     *
     * @return translated "Error Disconnecting Account"
     */
    @DefaultStringValue("Error Disconnecting Account")
    @Key("disconnectingErrorMessage")
    String disconnectingErrorMessage();

    /**
     * Translated "Connecting a publishing account".
     *
     * @return translated "Connecting a publishing account"
     */
    @DefaultStringValue("Connecting a publishing account")
    @Key("getAccountCountLabel")
    String getAccountCountLabel();

    /**
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    @DefaultStringValue("Connect Account")
    @Key("connectAccountCaption")
    String connectAccountCaption();

    /**
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    @DefaultStringValue("Connect Account")
    @Key("connectAccountOkCaption")
    String connectAccountOkCaption();

    /**
     * Translated "Connect Publishing Account".
     *
     * @return translated "Connect Publishing Account"
     */
    @DefaultStringValue("Connect Publishing Account")
    @Key("newRSConnectAccountPageTitle")
    String newRSConnectAccountPageTitle();

    /**
     * Translated "Pick an account".
     *
     * @return translated "Pick an account"
     */
    @DefaultStringValue("Pick an account")
    @Key("newRSConnectAccountPageSubTitle")
    String newRSConnectAccountPageSubTitle();

    /**
     * Translated "Connect Publishing Account".
     *
     * @return translated "Connect Publishing Account"
     */
    @DefaultStringValue("Connect Publishing Account")
    @Key("newRSConnectAccountPageCaption")
    String newRSConnectAccountPageCaption();

    /**
     * Translated "Choose Account Type".
     *
     * @return translated "Choose Account Type"
     */
    @DefaultStringValue("Choose Account Type")
    @Key("wizardNavigationPageTitle")
    String wizardNavigationPageTitle();

    /**
     * Translated "Choose Account Type".
     *
     * @return translated "Choose Account Type"
     */
    @DefaultStringValue("Choose Account Type")
    @Key("wizardNavigationPageSubTitle")
    String wizardNavigationPageSubTitle();

    /**
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    @DefaultStringValue("Connect Account")
    @Key("wizardNavigationPageCaption")
    String wizardNavigationPageCaption();

    /**
     * Translated "RStudio Connect is a server product from RStudio ".
     *
     * @return translated "RStudio Connect is a server product from RStudio "
     */
    @DefaultStringValue("RStudio Connect is a server product from RStudio ")
    @Key("serviceDescription")
    String serviceDescription();

    /**
     * Translated "for secure sharing of applications, reports, plots, and APIs.".
     *
     * @return translated "for secure sharing of applications, reports, plots, and APIs."
     */
    @DefaultStringValue("for secure sharing of applications, reports, plots, and APIs.")
    @Key("serviceMessageDescription")
    String serviceMessageDescription();

    /**
     * Translated "A cloud service run by RStudio. Publish Shiny applications ".
     *
     * @return translated "A cloud service run by RStudio. Publish Shiny applications "
     */
    @DefaultStringValue("A cloud service run by RStudio. Publish Shiny applications ")
    @Key("newRSConnectCloudPageSubTitle")
    String newRSConnectCloudPageSubTitle();

    /**
     * Translated "and interactive documents to the Internet.".
     *
     * @return translated "and interactive documents to the Internet."
     */
    @DefaultStringValue("and interactive documents to the Internet.")
    @Key("newRSConnectCloudPageSub")
    String newRSConnectCloudPageSub();

    /**
     * Translated "Connect ShinyApps.io Account".
     *
     * @return translated "Connect ShinyApps.io Account"
     */
    @DefaultStringValue("Connect ShinyApps.io Account")
    @Key("newRSConnectCloudPageCaption")
    String newRSConnectCloudPageCaption();

    /**
     * Translated "Converting Theme".
     *
     * @return translated "Converting Theme"
     */
    @DefaultStringValue("Converting Theme")
    @Key("withThemesCaption")
    String withThemesCaption();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultStringValue("R Markdown")
    @Key("withRMarkdownCaption")
    String withRMarkdownCaption();

    /**
     * Translated "Install Shiny Package".
     *
     * @return translated "Install Shiny Package"
     */
    @DefaultStringValue("Install Shiny Package")
    @Key("installShinyCaption")
    String installShinyCaption();

    /**
     * Translated "requires installation of an updated version ".
     *
     * @return translated "requires installation of an updated version "
     */
    @DefaultStringValue("requires installation of an updated version ")
    @Key("installShinyUserAction")
    String installShinyUserAction();

    /**
     * Translated "of the shiny package.\n\nDo you want to install shiny now?".
     *
     * @return translated "of the shiny package.\n\nDo you want to install shiny now?"
     */
    @DefaultStringValue("of the shiny package.\\n\\nDo you want to install shiny now?")
    @Key("installShinyMessage")
    String installShinyMessage();

    /**
     * Translated "Checking installed packages".
     *
     * @return translated "Checking installed packages"
     */
    @DefaultStringValue("Checking installed packages")
    @Key("installPkgsCaption")
    String installPkgsCaption();

    /**
     * Translated "Checking installed packages".
     *
     * @return translated "Checking installed packages"
     */
    @DefaultStringValue("Checking installed packages")
    @Key("withShinyAddinsCaption")
    String withShinyAddinsCaption();

    /**
     * Translated "Checking installed packages".
     *
     * @return translated "Checking installed packages"
     */
    @DefaultStringValue("Executing addins")
    @Key("withShinyAddinsUserAction")
    String withShinyAddinsUserAction();

    /**
     * Translated "Preparing Import from CSV".
     *
     * @return translated "Preparing Import from CSV"
     */
    @DefaultStringValue("Preparing Import from CSV")
    @Key("withDataImportCSVCaption")
    String withDataImportCSVCaption();

    /**
     * Translated "Preparing Import from SPSS, SAS and Stata".
     *
     * @return translated "Preparing Import from SPSS, SAS and Stata"
     */
    @DefaultStringValue("Preparing Import from SPSS, SAS and Stata")
    @Key("withDataImportSAV")
    String withDataImportSAV();

    /**
     * Translated "Preparing Import from Excel".
     *
     * @return translated "Preparing Import from Excel"
     */
    @DefaultStringValue("Preparing Import from Excel")
    @Key("withDataImportXLS")
    String withDataImportXLS();

    /**
     * Translated "Preparing Import from XML".
     *
     * @return translated "Preparing Import from XML"
     */
    @DefaultStringValue("Preparing Import from XML")
    @Key("withDataImportXML")
    String withDataImportXML();

    /**
     * Translated "Preparing Import from JSON".
     *
     * @return translated "Preparing Import from JSON"
     */
    @DefaultStringValue("Preparing Import from JSON")
    @Key("withDataImportJSON")
    String withDataImportJSON();

    /**
     * Translated "Preparing Import from JDBC".
     *
     * @return translated "Preparing Import from JDBC"
     */
    @DefaultStringValue("Preparing Import from JDBC")
    @Key("withDataImportJDBC")
    String withDataImportJDBC();

    /**
     * Translated "Preparing Import from ODBC".
     *
     * @return translated "Preparing Import from ODBC"
     */
    @DefaultStringValue("Preparing Import from ODBC")
    @Key("withDataImportODBC")
    String withDataImportODBC();

    /**
     * Translated "Preparing Profiler".
     *
     * @return translated "Preparing Profiler"
     */
    @DefaultStringValue("Preparing Profiler")
    @Key("withProfvis")
    String withProfvis();

    /**
     * Translated "Preparing Connection".
     *
     * @return translated "Preparing Connection"
     */
    @DefaultStringValue("Preparing Connection")
    @Key("withConnectionPackage")
    String withConnectionPackage();

    /**
     * Translated "Database Connectivity".
     *
     * @return translated "Database Connectivity"
     */
    @DefaultStringValue("Database Connectivity")
    @Key("withConnectionPackageContext")
    String withConnectionPackageContext();

    /**
     * Translated "Preparing Keyring".
     *
     * @return translated "Preparing Keyring"
     */
    @DefaultStringValue("Preparing Keyring")
    @Key("withKeyring")
    String withKeyring();

    /**
     * Translated "Using keyring".
     *
     * @return translated "Using keyring"
     */
    @DefaultStringValue("Using keyring")
    @Key("withKeyringUserAction")
    String withKeyringUserAction();

    /**
     * Translated "Preparing ".
     *
     * @return translated "Preparing "
     */
    @DefaultStringValue("Preparing ")
    @Key("withOdbc")
    String withOdbc();

    /**
     * Translated "Preparing ".
     *
     * @return translated "Preparing "
     */
    @DefaultStringValue("Using ")
    @Key("withOdbcUserAction")
    String withOdbcUserAction();

    /**
     * Translated "Starting tutorial".
     *
     * @return translated "Starting tutorial"
     */
    @DefaultStringValue("Starting tutorial")
    @Key("withTutorialDependencies")
    String withTutorialDependencies();

    /**
     * Translated "Starting a tutorial".
     *
     * @return translated "Starting a tutorial"
     */
    @DefaultStringValue("Starting a tutorial")
    @Key("withTutorialDependenciesUserAction")
    String withTutorialDependenciesUserAction();

    /**
     * Translated "Using the AGG renderer".
     *
     * @return translated "Using the AGG renderer"
     */
    @DefaultStringValue("Using the AGG renderer")
    @Key("withRagg")
    String withRagg();

    /**
     * Translated "is not available\n".
     *
     * @return translated "is not available\n"
     */
    @DefaultStringValue("is not available\n")
    @Key("unsatisfiedVersions")
    String unsatisfiedVersions();

    /**
     * Translated "is required but ".
     *
     * @return translated "is required but "
     */
    @DefaultStringValue("is required but ")
    @Key("requiredVersion")
    String requiredVersion();

    /**
     * Translated "is available".
     *
     * @return translated "is available"
     */
    @DefaultStringValue("is available")
    @Key("requiredAvailableVersion")
    String requiredAvailableVersion();

    /**
     * Translated "Packages Not Found".
     *
     * @return translated "Packages Not Found"
     */
    @DefaultStringValue("Packages Not Found")
    @Key("packageNotFoundUserAction")
    String packageNotFoundUserAction();

    /**
     * Translated "Required package versions could not be found:\n\n".
     *
     * @return translated "Required package versions could not be found:\n\n"
     */
    @DefaultStringValue("Required package versions could not be found:\n\n")
    @Key("packageNotFoundMessage")
    String packageNotFoundMessage();

    /**
     * Translated "Check that getOption("repos") refers to a CRAN ".
     *
     * @return translated "Check that getOption("repos") refers to a CRAN "
     */
    @DefaultStringValue("Check that getOption(\"repos\") refers to a CRAN ")
    @Key("packageNotFound")
    String packageNotFound();

    /**
     * Translated "repository that contains the needed package versions.".
     *
     * @return translated "repository that contains the needed package versions."
     */
    @DefaultStringValue("repository that contains the needed package versions.")
    @Key("neededPackageMessage")
    String neededPackageMessage();

    /**
     * Translated "Dependency installation failed".
     *
     * @return translated "Dependency installation failed"
     */
    @DefaultStringValue("Dependency installation failed")
    @Key("onErrorMessage")
    String onErrorMessage();

    /**
     * Translated "Could not determine available packages".
     *
     * @return translated "Could not determine available packages"
     */
    @DefaultStringValue("Could not determine available packages")
    @Key("availablePackageErrorMessage")
    String availablePackageErrorMessage();

    /**
     * Translated "requires an updated version of the ".
     *
     * @return translated "requires an updated version of the "
     */
    @DefaultStringValue("requires an updated version of the ")
    @Key("confirmPackageInstallation")
    String confirmPackageInstallation();

    /**
     * Translated "package. ".
     *
     * @return translated "package. "
     */
    @DefaultStringValue("package. ")
    @Key("packageMessage")
    String packageMessage();

    /**
     * Translated "\n\nDo you want to install this package now?".
     *
     * @return translated "\n\nDo you want to install this package now?"
     */
    @DefaultStringValue("\n\nDo you want to install this package now?")
    @Key("installPackageMessage")
    String installPackageMessage();

    /**
     * Translated "requires updated versions of the following packages: ".
     *
     * @return translated "requires updated versions of the following packages: "
     */
    @DefaultStringValue("requires updated versions of the following packages: ")
    @Key("updatedVersionMessage")
    String updatedVersionMessage();

    /**
     * Translated "\n\nDo you want to install these packages now?".
     *
     * @return translated "\n\nDo you want to install these packages now?"
     */
    @DefaultStringValue("\n\n Do you want to install these packages now?")
    @Key("installPkgMessage")
    String installPkgMessage();

    /**
     * Translated "Install Required Packages".
     *
     * @return translated "Install Required Packages"
     */
    @DefaultStringValue("Install Required Packages")
    @Key("installRequiredCaption")
    String installRequiredCaption();
}
