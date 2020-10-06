/*
 * SourceControlPreferencesPane.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.FileChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.SshKeyWidget;
import org.rstudio.studio.client.common.vcs.VcsHelpLink;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

public class SourceControlPreferencesPane extends PreferencesPane
{
   @Inject
   public SourceControlPreferencesPane(PreferencesDialogResources res,
                                       Session session,
                                       GitServerOperations server,
                                       final GlobalDisplay globalDisplay,
                                       final Commands commands,
                                       RemoteFileSystemContext fsContext,
                                       FileDialogs fileDialogs)
   {
      res_ = res;

      chkVcsEnabled_ = new CheckBox(
            "Enable version control interface for RStudio projects");
      extraSpaced(chkVcsEnabled_);
      add(chkVcsEnabled_);
      chkVcsEnabled_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            manageControlVisibility();

            globalDisplay.showMessage(
               MessageDialog.INFO,
               (event.getValue() ? "Enable" : "Disable") + " Version Control",
               "You must restart RStudio for this change to take effect.");
         }
      });


      // git exe path chooser
      Command onGitExePathChosen = new Command()
      {
         @Override
         public void execute()
         {
            if (BrowseCap.isWindowsDesktop())
            {
               String gitExePath = gitExePathChooser_.getText();
               if (!gitExePath.endsWith("git.exe"))
               {
                  String message = "The program '" + gitExePath + "'" +
                     " is unlikely to be a valid git executable.\n" +
                     "Please select a git executable called 'git.exe'.";

                  globalDisplay.showMessage(
                        GlobalDisplay.MSG_WARNING,
                        "Invalid Git Executable",
                        message);
               }
            }
         }
      };

      gitExePathLabel_ = new FormLabel("Git executable:");
      gitExePathChooser_ = new FileChooserTextBox(gitExePathLabel_,
                                                  "(Not Found)",
                                                  ElementIds.TextBoxButtonId.GIT,
                                                  false,
                                                  null,
                                                  onGitExePathChosen);
      SessionInfo sessionInfo = session.getSessionInfo();
      if (sessionInfo.getAllowVcsExeEdit())
         addTextBoxChooser(gitExePathLabel_, gitExePathChooser_);

      // svn exe path chooser
      svnExePathLabel_ = new FormLabel("SVN executable:");
      svnExePathChooser_ = new FileChooserTextBox(svnExePathLabel_,
                                                  "(Not Found)",
                                                  ElementIds.TextBoxButtonId.SVN,
                                                  false,
                                                  null,
                                                  null);
      if (sessionInfo.getAllowVcsExeEdit())
         addTextBoxChooser(svnExePathLabel_, svnExePathChooser_);

      // terminal path
      terminalPathLabel_ = new FormLabel("Terminal executable:");
      terminalPathChooser_ = new FileChooserTextBox(terminalPathLabel_,
                                                    "(Not Found)",
                                                    ElementIds.TextBoxButtonId.VCS_TERMINAL,
                                                    false,
                                                    null,
                                                    null);
      if (haveTerminalPathPref())
         addTextBoxChooser(terminalPathLabel_, terminalPathChooser_);

      // ssh key widget
      sshKeyWidget_ = new SshKeyWidget(server, "330px");
      sshKeyWidget_.addStyleName(res_.styles().sshKeyWidget());
      nudgeRight(sshKeyWidget_);
      add(sshKeyWidget_);

      HelpLink vcsHelpLink = new VcsHelpLink();
      nudgeRight(vcsHelpLink);
      vcsHelpLink.addStyleName(res_.styles().newSection());
      add(vcsHelpLink);

      chkVcsEnabled_.setEnabled(false);
      gitExePathChooser_.setEnabled(false);
      svnExePathChooser_.setEnabled(false);
      terminalPathChooser_.setEnabled(false);
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      chkVcsEnabled_.setEnabled(true);
      gitExePathChooser_.setEnabled(true);
      svnExePathChooser_.setEnabled(true);
      terminalPathChooser_.setEnabled(true);

      chkVcsEnabled_.setValue(prefs.vcsEnabled().getValue());
      gitExePathChooser_.setText(prefs.gitExePath().getValue());
      svnExePathChooser_.setText(prefs.svnExePath().getValue());
      terminalPathChooser_.setText(prefs.terminalPath().getValue());

      sshKeyWidget_.setRsaSshKeyPath(prefs.rsaKeyPath().getValue(),
                                     prefs.haveRsaKey().getValue());
      sshKeyWidget_.setProgressIndicator(getProgressIndicator());

      manageControlVisibility();
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconSourceControl2x());
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Git/SVN";
   }

   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      RestartRequirement restartRequirement = super.onApply(prefs);

      prefs.vcsEnabled().setGlobalValue(chkVcsEnabled_.getValue());
      prefs.gitExePath().setGlobalValue(gitExePathChooser_.getText());
      prefs.svnExePath().setGlobalValue(svnExePathChooser_.getText());
      prefs.terminalPath().setGlobalValue(terminalPathChooser_.getText());

      return restartRequirement;
   }

   private boolean haveTerminalPathPref()
   {
      return Desktop.isDesktop() && BrowseCap.isLinux();
   }

   private void addTextBoxChooser(Label captionLabel, TextBoxWithButton chooser)
   {
      String textWidth = "250px";

      HorizontalPanel captionPanel = new HorizontalPanel();
      captionPanel.setWidth(textWidth);
      nudgeRight(captionPanel);

      captionPanel.add(captionLabel);
      captionPanel.setCellHorizontalAlignment(captionLabel,
            HasHorizontalAlignment.ALIGN_LEFT);

      add(tight(captionPanel));

      chooser.setTextWidth(textWidth);
      nudgeRight(chooser);
      textBoxWithChooser(chooser);
      spaced(chooser);
      add(chooser);
   }

   private void manageControlVisibility()
   {
      boolean vcsEnabled = chkVcsEnabled_.getValue();
      gitExePathLabel_.setVisible(vcsEnabled);
      gitExePathChooser_.setVisible(vcsEnabled);
      svnExePathLabel_.setVisible(vcsEnabled);
      svnExePathChooser_.setVisible(vcsEnabled);
      terminalPathLabel_.setVisible(vcsEnabled);
      terminalPathChooser_.setVisible(vcsEnabled && haveTerminalPathPref());
      sshKeyWidget_.setVisible(vcsEnabled);
   }

   private final PreferencesDialogResources res_;

   private final CheckBox chkVcsEnabled_;

   private FormLabel svnExePathLabel_;
   private FormLabel gitExePathLabel_;
   private TextBoxWithButton gitExePathChooser_;
   private TextBoxWithButton svnExePathChooser_;
   private FormLabel terminalPathLabel_;
   private TextBoxWithButton terminalPathChooser_;
   private SshKeyWidget sshKeyWidget_;
}
