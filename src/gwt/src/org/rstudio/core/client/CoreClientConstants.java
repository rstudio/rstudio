/*
 * CoreClientConstants.java
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
package org.rstudio.core.client;

public interface CoreClientConstants extends com.google.gwt.i18n.client.Messages {

    @Key("cancelLabel")
    String cancelLabel();

    @Key("resetLabel")
    String resetLabel();

    @Key("noLabel")
    String noLabel();

    @Key("yesLabel")
    String yesLabel();

    @Key("okayLabel")
    String okayLabel();

    @Key("copyToClipboardLabel")
    String copyToClipboardLabel();

    @Key("notYetImplementedCaption")
    String notYetImplementedCaption();

    @Key("notYetImplementedMessage")
    String notYetImplementedMessage();

    @Key("popupBlockCaption")
    String popupBlockCaption();

    @Key("popupBlockTryAgainLabel")
    String popupBlockTryAgainLabel();

    @Key("secondLabel")
    String secondLabel(int second);

    @Key("secondPluralLabel")
    String secondPluralLabel(int seconds);

    @Key("minuteLabel")
    String minuteLabel(int minute);

    @Key("minutePluralLabel")
    String minutePluralLabel(int minutes);

    @Key("hourLabel")
    String hourLabel(int hour);

    @Key("hourPluralLabel")
    String hourPluralLabel(int hours);

    @Key("reportShortCutMessage")
    String reportShortCutMessage();

    @Key("multiGestureMessage")
    String multiGestureMessage();

    @Key("shortcutUnBoundMessage")
    String shortcutUnBoundMessage();

    @Key("nameEmptyMessage")
    String nameEmptyMessage();

    @Key("nameStartWithMessage")
    String nameStartWithMessage();

    @Key("nameIllegalCharacterMessage")
    String nameIllegalCharacterMessage();

    @Key("illegalNameMessage")
    String illegalNameMessage();

    @Key("fileNameLabel")
    String fileNameLabel();

    @Key("getFilenameLabel")
    String getFilenameLabel();

    @Key("nonexistentFileMessage")
    String nonexistentFileMessage();

    @Key("openProjectTitle")
    String openProjectTitle();

    @Key("openButtonTitle")
    String openButtonTitle();

    @Key("rProjectsFilter")
    String rProjectsFilter();

    @Key("newSessionCheckLabel")
    String newSessionCheckLabel();

    @Key("createButtonTitle")
    String createButtonTitle();

    @Key("pathBreadCrumbSelectPath")
    String pathBreadCrumbSelectPath();

    @Key("pathBreadCrumbButtonTitle")
    String pathBreadCrumbButtonTitle();

    @Key("projectIconDesc")
    String projectIconDesc();

    @Key("projectsLabel")
    String projectsLabel();
    

    @Key("anchorHomeText")
    String anchorHomeText();

    @Key("cloudHomeText")
    String cloudHomeText();

    @Key("browseFolderCaption")
    String browseFolderCaption();

    @Key("browseFolderLabel")
    String browseFolderLabel();

    @Key("showOverwriteCaption")
    String showOverwriteCaption();

    @Key("showOverwriteMessage")
    String showOverwriteMessage();

    @Key("rSessionMessage")
    String rSessionMessage();

    @Key("rStudioServerMessage")
    String rStudioServerMessage();

    @Key("okButtonTitle")
    String okButtonTitle();

    @Key("addButtonTitle")
    String addButtonTitle();

    @Key("progressIndicatorTitle")
    String progressIndicatorTitle();

    @Key("restartRequiredCaption")
    String restartRequiredCaption();

    @Key("promiseWithProgress")
    String promiseWithProgress();

    @Key("promiseWithProgressError")
    String promiseWithProgressError();

    @Key("documentsTabList")
    String documentsTabList();

    @Key("renameMenuItem")
    String renameMenuItem();

    @Key("copyPathMenuItem")
    String copyPathMenuItem();

    @Key("setWorkingDirMenuItem")
    String setWorkingDirMenuItem();

    @Key("closeMenuItem")
    String closeMenuItem();

    @Key("closeAllMenuItem")
    String closeAllMenuItem();

    @Key("closeOthersMenuItem")
    String closeOthersMenuItem();

    @Key("closeTabText")
    String closeTabText();

    @Key("docPropErrorMessage")
    String docPropErrorMessage();

    @Key("closePopupText")
    String closePopupText();

    @Key("themeButtonOnErrorMessage")
    String themeButtonOnErrorMessage();

    @Key("onSubmitErrorMessage")
    String onSubmitErrorMessage();

    @Key("installText")
    String installText();

    @Key("donnotShowAgain")
    String donnotShowAgain();

    @Key("showPanmirrorText")
    String showPanmirrorText();

    @Key("reloadNowText")
    String reloadNowText();

    @Key("installTinyTexText")
    String installTinyTexText();

    @Key("showReadOnlyWarningText")
    String showReadOnlyWarningText();

    @Key("showReadOnlyWarningGeneratedText")
    String showReadOnlyWarningGeneratedText();

    @Key("buttonAddCaption")
    String buttonAddCaption();

    @Key("buttonRemoveCaption")
    String buttonRemoveCaption();

    @Key("localReposText")
    String localReposText();

    @Key("localReposTitle")
    String localReposTitle();

    @Key("addLocalRepoText")
    String addLocalRepoText();

    @Key("errorCaption")
    String errorCaption();

    @Key("emptyLabel")
    String emptyLabel();

    @Key("keyboardShortcutsText")
    String keyboardShortcutsText();

    @Key("applyThemeButtonText")
    String applyThemeButtonText();

    @Key("radioButtonLabel")
    String radioButtonLabel();

    @Key("radioCustomizedLabel")
    String radioCustomizedLabel();

    @Key("filterWidgetLabel")
    String filterWidgetLabel();

    @Key("filterWidgetPlaceholderText")
    String filterWidgetPlaceholderText();

    @Key("resetButtonText")
    String resetButtonText();

    @Key("resetKeyboardShortcutsCaption")
    String resetKeyboardShortcutsCaption();

    @Key("resetKeyboardShortcutsProgress")
    String resetKeyboardShortcutsProgress();

    @Key("nameColumnText")
    String nameColumnText();

    @Key("editableTextColumn")
    String editableTextColumn();

    @Key("scopeTextColumn")
    String scopeTextColumn();

    @Key("tagNameErrorMessage")
    String tagNameErrorMessage();

    @Key("radioShowLabel")
    String radioShowLabel();

    @Key("customizeKeyboardHelpLink")
    String customizeKeyboardHelpLink();

    @Key("addMaskedCommandStylesText")
    String addMaskedCommandStylesText();

    @Key("addConflictCommandStylesText")
    String addConflictCommandStylesText();

    @Key("refreshAutomaticallyLabel")
    String refreshAutomaticallyLabel();

    @Key("stopButtonText")
    String stopButtonText();

    @Key("satelliteToolBarText")
    String satelliteToolBarText();

    @Key("searchWidgetClearText")
    String searchWidgetClearText();

    @Key("selectWidgetListBoxNone")
    String selectWidgetListBoxNone();

    @Key("shortcutHeaderText")
    String shortcutHeaderText();

    @Key("tabsGroupName")
    String tabsGroupName();

    @Key("panesGroupName")
    String panesGroupName();

    @Key("filesGroupName")
    String filesGroupName();

    @Key("mainMenuGroupName")
    String mainMenuGroupName();

    @Key("sourceNavigationGroupName")
    String sourceNavigationGroupName();

    @Key("executeGroupName")
    String executeGroupName();

    @Key("sourceEditorGroupName")
    String sourceEditorGroupName();

    @Key("debugGroupName")
    String debugGroupName();

    @Key("accessibilityGroupName")
    String accessibilityGroupName();

    @Key("sourceControlGroupName")
    String sourceControlGroupName();

    @Key("buildGroupName")
    String buildGroupName();

    @Key("consoleGroupName")
    String consoleGroupName();

    @Key("terminalGroupName")
    String terminalGroupName();

    @Key("otherGroupName")
    String otherGroupName();

    @Key("addShiftPTag")
    String addShiftPTag();

    @Key("useDefaultPrefix")
    String useDefaultPrefix();

    @Key("validateMessage")
    String validateMessage();

    @Key("notValidNumberMessage")
    String notValidNumberMessage();

    @Key("vimKeyboardShortcutsText")
    String vimKeyboardShortcutsText();

    @Key("nextButtonText")
    String nextButtonText();

    @Key("backButtonText")
    String backButtonText();

    @Key("dialogInfoText")
    String dialogInfoText();

    @Key("directoryContentsLabel")
    String directoryContentsLabel();

    @Key("newFolderTitle")
    String newFolderTitle();

    @Key("folderNameLabel")
    String folderNameLabel();

    @Key("dialogWarningText")
    String dialogWarningText();

    @Key("dialogQuestionText")
    String dialogQuestionText();

    @Key("dialogPopupBlockedText")
    String dialogPopupBlockedText();

    @Key("dialogErrorText")
    String dialogErrorText();

    @Key("manualRefreshLabel")
    String manualRefreshLabel();

    @Key("busyLabel")
    String busyLabel();

    @Key("redactedText")
    String redactedText();

    @Key("vimKeyboardShortcutHelpMessage")
    String vimKeyboardShortcutHelpMessage();

    @Key("showPopupBlockMessage")
    String showPopupBlockMessage(String hostName);

    @Key("rpcErrorMessage")
    String rpcErrorMessage(String statusCode, String desktop, String method);

    @Key("rpcErrorMessageCaption")
    String rpcErrorMessageCaption();

    @Key("rpcOverrideErrorMessage")
    String rpcOverrideErrorMessage(String desktop, String method);

    @Key("rpcOverrideErrorMessageServer")
    String rpcOverrideErrorMessageServer(String platform);

    @Key("rpcOverrideErrorMessageLink")
    String rpcOverrideErrorMessageLink();

    @Key("restartRequiredMessage")
    String restartRequiredMessage();

    @Key("minimizedTabListRole")
    String minimizedTabListRole(String accessibleName);

    @Key("closeText")
    String closeText();

    @Key("closeButtonText")
    String closeButtonText(String title);

    @Key("minimizeState")
    String minimizeState(String name);

    @Key("maximizeState")
    String maximizeState(String name);

    @Key("normalState")
    String normalState(String name);

    @Key("hideState")
    String hideState(String name);

    @Key("exclusiveState")
    String exclusiveState(String name);

    @Key("package1Message")
    String package1Message(String packages);

    @Key("packages2Message")
    String packages2Message(String package0, String package1);

    @Key("packages3Message")
    String packages3Message(String package0, String package1, String package2);

    @Key("otherPackagesMessage")
    String otherPackagesMessage(String package0, String package1, String package2);

    @Key("resetKeyboardShortcutsMessage")
    String resetKeyboardShortcutsMessage();

    @Key("rStudioGinjectorErrorMessage")
    String rStudioGinjectorErrorMessage(String label);

    @Key("rStudioGinjectorGreaterThanError")
    String rStudioGinjectorGreaterThanError(String label, int minValue);

    @Key("rStudioGinjectorLessThanError")
    String rStudioGinjectorLessThanError(String label, int maxValue);

    @Key("operationCompletedText")
    String operationCompletedText();

    @Key("completedText")
    String completedText(String labelText);

    @Key("clearLabel")
    String clearLabel();

    @Key("fileChooserTextBoxBrowseLabel")
    String fileChooserTextBoxBrowseLabel();

    @Key("chooseFileCaption")
    String chooseFileCaption();

    @Key("keyComboCtrl")
    String keyComboCtrl();

    @Key("keyComboAlt")
    String keyComboAlt();

    @Key("keyComboShift")
    String keyComboShift();

    @Key("keyComboCmd")
    String keyComboCmd();

    @Key("keyNameEnter")
    String keyNameEnter();

    @Key("keyNameLeft")
    String keyNameLeft();

    @Key("keyNameRight")
    String keyNameRight();

    @Key("keyNameUp")
    String keyNameUp();

    @Key("keyNameDown")
    String keyNameDown();

    @Key("keyNameTab")
    String keyNameTab();

    @Key("keyNamePageUp")
    String keyNamePageUp();

    @Key("keyNamePageDown")
    String keyNamePageDown();

    @Key("keyNameBackspace")
    String keyNameBackspace();

    @Key("keyNameSpace")
    String keyNameSpace();
    

}
