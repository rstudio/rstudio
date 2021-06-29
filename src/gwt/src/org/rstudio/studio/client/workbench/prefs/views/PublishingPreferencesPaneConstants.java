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
}