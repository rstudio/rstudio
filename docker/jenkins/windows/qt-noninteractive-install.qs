function Controller() {
    installer.autoRejectMessageBoxes();
    installer.installationFinished.connect(function() {
        gui.clickButton(buttons.NextButton);
    })
}

Controller.prototype.WelcomePageCallback = function() {
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.CredentialsPageCallback = function() {
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.IntroductionPageCallback = function() {
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.TargetDirectoryPageCallback = function()
{
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.ComponentSelectionPageCallback = function() {
    var widget = gui.currentPageWidget();
    widget.deselectAll();
    widget.selectComponent("qt.qt5.5100.win32_msvc2015");
    widget.selectComponent("qt.qt5.5100.win64_msvc2015_64");
    widget.selectComponent("qt.qt5.5100.qtwebengine");
    widget.selectComponent("qt.qt5.5100.qtwebengine.win32_msvc2015");
    widget.selectComponent("qt.qt5.5100.qtwebengine.win64_msvc2015_64");
    widget.deselectComponent("qt.qt5.5100.src");
    widget.deselectComponent("qt.qt5.5100.doc");
    widget.deselectComponent("qt.qt5.5100.examples");
    widget.deselectComponent("qt.qt5.5100.doc.qtwebengine");
    widget.deselectComponent("qt.qt5.5100.examples.qtwebengine");
    widget.deselectComponent("qt.tools.qtcreator");
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.LicenseAgreementPageCallback = function() {
    gui.currentPageWidget().AcceptLicenseRadioButton.setChecked(true);
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.StartMenuDirectoryPageCallback = function() {
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.ReadyForInstallationPageCallback = function()
{
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.FinishedPageCallback = function() {
var checkBoxForm = gui.currentPageWidget().LaunchQtCreatorCheckBoxForm
if (checkBoxForm && checkBoxForm.launchQtCreatorCheckBox) {
    checkBoxForm.launchQtCreatorCheckBox.checked = false;
}
    gui.clickButton(buttons.FinishButton);
}
