package org.rstudio.studio.client.workbench.prefs.views;

public interface PythonPreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "(No interpreter selected)".
     *
     * @return translated "(No interpreter selected)"
     */
    @DefaultStringValue("(No interpreter selected)")
    @Key("pythonPreferencesText")
    String pythonPreferencesText();

    /**
     * Translated "(NOTE: This project has already been configured with ".
     *
     * @return translated "(NOTE: This project has already been configured with "
     */
    @DefaultStringValue("(NOTE: This project has already been configured with ")
    @Key("overrideText")
    String overrideText();

    /**
     * Translated "its own Python interpreter. Use the Project Options ".
     *
     * @return translated "its own Python interpreter. Use the Project Options "
     */
    @DefaultStringValue("its own Python interpreter. Use the Project Options ")
    @Key("overrideInterpreterText")
    String overrideInterpreterText();

    /**
     * Translated "dialog to change the version of Python used in this project.)".
     *
     * @return translated "dialog to change the version of Python used in this project.)"
     */
    @DefaultStringValue("dialog to change the version of Python used in this project.)")
    @Key("overrideChangeVersionText")
    String overrideChangeVersionText();

    /**
     * Translated "Python".
     *
     * @return translated "Python"
     */
    @DefaultStringValue("Python")
    @Key("headerPythonLabel")
    String headerPythonLabel();

    /**
     * Translated "The active Python interpreter has been changed by an R startup script.".
     *
     * @return translated "The active Python interpreter has been changed by an R startup script."
     */
    @DefaultStringValue("The active Python interpreter has been changed by an R startup script.")
    @Key("mismatchWarningBarText")
    String mismatchWarningBarText();

    /**
     * Translated "Finding interpreters...".
     *
     * @return translated "Finding interpreters..."
     */
    @DefaultStringValue("Finding interpreters...")
    @Key("progressIndicatorText")
    String progressIndicatorText();

    /**
     * Translated "Finding interpreters...".
     *
     * @return translated "Finding interpreters..."
     */
    @DefaultStringValue("Python interpreter:")
    @Key("tbPythonInterpreterText")
    String tbPythonInterpreterText();

    /**
     * Translated "Select...".
     *
     * @return translated "Select..."
     */
    @DefaultStringValue("Select...")
    @Key("tbPythonActionText")
    String tbPythonActionText();

    /**
     * Translated "Error finding Python interpreters: ".
     *
     * @return translated "Error finding Python interpreters: "
     */
    @DefaultStringValue("Error finding Python interpreters: ")
    @Key("onErrorMessage")
    String onErrorMessage();

    /**
     * Translated "The selected Python interpreter appears to be invalid.".
     *
     * @return translated "The selected Python interpreter appears to be invalid."
     */
    @DefaultStringValue("The selected Python interpreter appears to be invalid.")
    @Key("invalidReasonLabel")
    String invalidReasonLabel();

    /**
     * Translated "Using Python in RStudio".
     *
     * @return translated "Using Python in RStudio"
     */
    @DefaultStringValue("Using Python in RStudio")
    @Key("helpButtonLabel")
    String helpButtonLabel();

    /**
     * Translated "Automatically activate project-local Python environments".
     *
     * @return translated "Automatically activate project-local Python environments"
     */
    @DefaultStringValue("Automatically activate project-local Python environments")
    @Key("cbAutoUseProjectInterpreter")
    String cbAutoUseProjectInterpreter();

    /**
     * Translated "When enabled, RStudio will automatically find and activate a ".
     *
     * @return translated "When enabled, RStudio will automatically find and activate a "
     */
    @DefaultStringValue("When enabled, RStudio will automatically find and activate a ")
    @Key("cbAutoUseProjectInterpreterMessage")
    String cbAutoUseProjectInterpreterMessage();

    /**
     * Translated "Python environment located within the project root directory (if any).".
     *
     * @return translated "Python environment located within the project root directory (if any)."
     */
    @DefaultStringValue("Python environment located within the project root directory (if any).")
    @Key("cbAutoUseProjectInterpreterMsg")
    String cbAutoUseProjectInterpreterMsg();

    /**
     * Translated "Python Interpreters".
     *
     * @return translated "Python Interpreters"
     */
    @DefaultStringValue("Python Interpreters")
    @Key("interpretersCaption")
    String interpretersCaption();

    /**
     * Translated "Select".
     *
     * @return translated "Select"
     */
    @DefaultStringValue("Select")
    @Key("okButtonCaption")
    String okButtonCaption();

    /**
     * Translated "(None available)".
     *
     * @return translated "(None available)"
     */
    @DefaultStringValue("(None available)")
    @Key("noneAvailableListBox")
    String noneAvailableListBox();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultStringValue("General")
    @Key("tabPanelCaption")
    String tabPanelCaption();

    /**
     * Translated "Clear".
     *
     * @return translated "Clear"
     */
    @DefaultStringValue("Clear")
    @Key("clearLabel")
    String clearLabel();

    /**
     * Translated "System".
     *
     * @return translated "System"
     */
    @DefaultStringValue("System")
    @Key("systemTab")
    String systemTab();

    /**
     * Translated "Virtual Environments".
     *
     * @return translated "Virtual Environments"
     */
    @DefaultStringValue("Virtual Environments")
    @Key("virtualEnvTab")
    String virtualEnvTab();

    /**
     * Translated "Conda Environments".
     *
     * @return translated "Conda Environments"
     */
    @DefaultStringValue("Conda Environments")
    @Key("condaEnvTab")
    String condaEnvTab();
}
