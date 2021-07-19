package org.rstudio.studio.client.workbench.prefs.views;

public interface SourceControlPreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "Enable version control interface for RStudio projects".
     *
     * @return translated "Enable version control interface for RStudio projects"
     */
    @DefaultStringValue("Enable version control interface for RStudio projects")
    @Key("chkVcsEnabledLabel")
    String chkVcsEnabledLabel();

    /**
     * Translated "Enable".
     *
     * @return translated "Enable"
     */
    @DefaultStringValue("Enable")
    @Key("globalDisplayEnable")
    String globalDisplayEnable();

    /**
     * Translated "Disable".
     *
     * @return translated "Disable"
     */
    @DefaultStringValue("Disable")
    @Key("globalDisplayDisable")
    String globalDisplayDisable();

    /**
     * Translated "Version Control ".
     *
     * @return translated "Version Control "
     */
    @DefaultStringValue("Version Control ")
    @Key("globalDisplayVC")
    String globalDisplayVC();

    /**
     * Translated "You must restart RStudio for this change to take effect.".
     *
     * @return translated "You must restart RStudio for this change to take effect."
     */
    @DefaultStringValue("You must restart RStudio for this change to take effect.")
    @Key("globalDisplayVCMessage")
    String globalDisplayVCMessage();

    /**
     * Translated "The program '".
     *
     * @return translated "The program '"
     */
    @DefaultStringValue("The program '")
    @Key("gitExePathMessage")
    String gitExePathMessage();

    /**
     * Translated "is unlikely to be a valid git executable.".
     *
     * @return translated "is unlikely to be a valid git executable."
     */
    @DefaultStringValue("is unlikely to be a valid git executable.\n")
    @Key("gitExePath")
    String gitExePath();

    /**
     * Translated "Please select a git executable called 'git.exe'.".
     *
     * @return translated "Please select a git executable called 'git.exe'."
     */
    @DefaultStringValue("Please select a git executable called 'git.exe'.")
    @Key("gitExeSelectPathMessage")
    String gitExeSelectPathMessage();

    /**
     * Translated "Please select a git executable called 'git.exe'.".
     *
     * @return translated "Please select a git executable called 'git.exe'."
     */
    @DefaultStringValue("Invalid Git Executable")
    @Key("gitGlobalDisplay")
    String gitGlobalDisplay();

    /**
     * Translated "Git executable:".
     *
     * @return translated "Git executable:"
     */
    @DefaultStringValue("Git executable:")
    @Key("gitExePathLabel")
    String gitExePathLabel();

    /**
     * Translated "(Not Found)".
     *
     * @return translated "(Not Found)"
     */
    @DefaultStringValue("(Not Found)")
    @Key("gitExePathNotFoundLabel")
    String gitExePathNotFoundLabel();

    /**
     * Translated "SVN executable:".
     *
     * @return translated "SVN executable:"
     */
    @DefaultStringValue("SVN executable:")
    @Key("svnExePathLabel")
    String svnExePathLabel();

    /**
     * Translated "Terminal executable:".
     *
     * @return translated "Terminal executable:"
     */
    @DefaultStringValue("Terminal executable:")
    @Key("terminalPathLabel")
    String terminalPathLabel();

    /**
     * Translated "Git/SVN".
     *
     * @return translated "Git/SVN"
     */
    @DefaultStringValue("Git/SVN")
    @Key("gitSVNPaneHeader")
    String gitSVNPaneHeader();

    /**
     * Translated "SSH RSA key:".
     *
     * @return translated "SSH RSA key:"
     */
    @DefaultStringValue("SSH RSA key:")
    @Key("sshKeyPathLabel")
    String sshKeyPathLabel();

    /**
     * Translated "View public key".
     *
     * @return translated "View public key"
     */
    @DefaultStringValue("View public key")
    @Key("publicKeyLinkCaption")
    String publicKeyLinkCaption();

    /**
     * Translated "Create RSA Key...".
     *
     * @return translated "Create RSA Key..."
     */
    @DefaultStringValue("Create RSA Key...")
    @Key("createKeyButtonLabel")
    String createKeyButtonLabel();

    /**
     * Translated "Reading public key...".
     *
     * @return translated "Reading public key..."
     */
    @DefaultStringValue("Reading public key...")
    @Key("progressIndicatorLabel")
    String progressIndicatorLabel();

    /**
     * Translated "Public Key".
     *
     * @return translated "Public Key"
     */
    @DefaultStringValue("Public Key")
    @Key("showPublicKeyDialogCaption")
    String showPublicKeyDialogCaption();

    /**
     * Translated "Error attempting to read key '".
     *
     * @return translated "Error attempting to read key '"
     */
    @DefaultStringValue("Error attempting to read key '")
    @Key("onErrorMessage")
    String onErrorMessage();

    /**
     * Translated "Using Version Control with RStudio".
     *
     * @return translated "Using Version Control with RStudio"
     */
    @DefaultStringValue("Using Version Control with RStudio")
    @Key("vCSHelpLink")
    String vCSHelpLink();

    /**
     * Translated "Create RSA Key".
     *
     * @return translated "Create RSA Key"
     */
    @DefaultStringValue("Create RSA Key")
    @Key("createKeyDialogCaption")
    String createKeyDialogCaption();

    /**
     * Translated "Create RSA Key".
     *
     * @return translated "Create RSA Key"
     */
    @DefaultStringValue("Creating RSA Key...")
    @Key("onProgressLabel")
    String onProgressLabel();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    @DefaultStringValue("Create")
    @Key("setOkButtonCaption")
    String setOkButtonCaption();

    /**
     * Translated "Non-Matching Passphrases".
     *
     * @return translated "Non-Matching Passphrases"
     */
    @DefaultStringValue("Non-Matching Passphrases")
    @Key("showErrorCaption")
    String showErrorCaption();

    /**
     * Translated "The passphrase and passphrase confirmation do not match.".
     *
     * @return translated "The passphrase and passphrase confirmation do not match."
     */
    @DefaultStringValue("The passphrase and passphrase confirmation do not match.")
    @Key("showErrorMessage")
    String showErrorMessage();

    /**
     * Translated "The RSA key will be created at:".
     *
     * @return translated "The RSA key will be created at:"
     */
    @DefaultStringValue("The RSA key will be created at:")
    @Key("pathCaption")
    String pathCaption();

    /**
     * Translated "SSH/RSA key management".
     *
     * @return translated "SSH/RSA key management"
     */
    @DefaultStringValue("SSH/RSA key management")
    @Key("pathHelpCaption")
    String pathHelpCaption();

    /**
     * Translated "Passphrase (optional):".
     *
     * @return translated "Passphrase (optional):"
     */
    @DefaultStringValue("Passphrase (optional):")
    @Key("passphraseLabel")
    String passphraseLabel();

    /**
     * Translated "Confirm:".
     *
     * @return translated "Confirm:"
     */
    @DefaultStringValue("Confirm:")
    @Key("passphraseConfirmLabel")
    String passphraseConfirmLabel();

    /**
     * Translated "Key Already Exists".
     *
     * @return translated "Key Already Exists"
     */
    @DefaultStringValue("Key Already Exists")
    @Key("confirmOverwriteKeyCaption")
    String confirmOverwriteKeyCaption();

    /**
     * Translated "An RSA key already exists at ".
     *
     * @return translated "An RSA key already exists at "
     */
    @DefaultStringValue("An RSA key already exists at ")
    @Key("confirmOverwriteKeyMessage")
    String confirmOverwriteKeyMessage();

    /**
     * Translated "Do you want to overwrite the existing key?".
     *
     * @return translated "Do you want to overwrite the existing key?"
     */
    @DefaultStringValue("Do you want to overwrite the existing key?")
    @Key("overwriteExistingKeyMessage")
    String overwriteExistingKeyMessage();
}
