/*
 * SourceControlPreferencesPane.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.FileChooserTextBox;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.vcs.VCSHelpLink;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.SourceControlPrefs;

public class SourceControlPreferencesPane extends PreferencesPane
{
   @Inject
   public SourceControlPreferencesPane(PreferencesDialogResources res,
                                       Session session,
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
      gitExePathChooser_ = new FileChooserTextBox("",
                                                  "(Not Found)",
                                                  null,
                                                  null);
      gitExePathLabel_ = new Label("Git executable:");
      addTextBoxChooser(gitExePathLabel_, null, null, gitExePathChooser_);
      
      // use git bash
      chkUseGitBash_ = new CheckBox("Use Git Bash as shell for Git projects");
      if (haveGitBashPref())
      {
         extraSpaced(chkUseGitBash_);
         add(chkUseGitBash_);
      }
      
        
      // svn exe path chooser
      svnExePathLabel_ = new Label("SVN executable:");
      svnExePathChooser_ = new FileChooserTextBox("",
                                                  "(Not Found)",
                                                  null,
                                                  null);
      addTextBoxChooser(svnExePathLabel_, null, null, svnExePathChooser_);
      
      
      // terminal path
      terminalPathLabel_ = new Label("Terminal executable:");
      terminalPathChooser_ = new FileChooserTextBox("", 
                                                    "(Not Found)", 
                                                    null, 
                                                    null);
      if (haveTerminalPathPref())
         addTextBoxChooser(terminalPathLabel_, null, null, terminalPathChooser_);
     
      // show ssh key button
      showSshKeyButton_ = new ThemedButton(
         "View Public Key...", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               commands.versionControlShowRsaKey().execute();
            }
         });
      showSshKeyButton_.addStyleName(res_.styles().viewSshKey());
      add(showSshKeyButton_);
            
       
      VCSHelpLink vcsHelpLink = new VCSHelpLink();
      nudgeRight(vcsHelpLink); 
      vcsHelpLink.addStyleName(res_.styles().newSection()); 
      add(vcsHelpLink);
                                      
      chkVcsEnabled_.setEnabled(false);
      gitExePathChooser_.setEnabled(false);
      svnExePathChooser_.setEnabled(false);
      terminalPathChooser_.setEnabled(false);
      chkUseGitBash_.setEnabled(false);
      showSshKeyButton_.setEnabled(false);
   }

   @Override
   protected void initialize(RPrefs rPrefs)
   {
      // source control prefs
      SourceControlPrefs prefs = rPrefs.getSourceControlPrefs();
      originalPrefs_ = prefs;

      chkVcsEnabled_.setEnabled(true);
      gitExePathChooser_.setEnabled(true);
      svnExePathChooser_.setEnabled(true);
      terminalPathChooser_.setEnabled(true);
      chkUseGitBash_.setEnabled(true);
      showSshKeyButton_.setEnabled(true);

      chkVcsEnabled_.setValue(prefs.getVcsEnabled());
      gitExePathChooser_.setText(prefs.getGitExePath());
      svnExePathChooser_.setText(prefs.getSvnExePath());
      terminalPathChooser_.setText(prefs.getTerminalPath());
      chkUseGitBash_.setValue(prefs.getUseGitBash());

      manageControlVisibility();
   }

   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconSourceControl();
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Version Control";
   }

   @Override
   public void onApply(RPrefs rPrefs)
   {
      super.onApply(rPrefs);

      SourceControlPrefs prefs = SourceControlPrefs.create(
            chkVcsEnabled_.getValue(), gitExePathChooser_.getText(),
            svnExePathChooser_.getText(), terminalPathChooser_.getText(),
            chkUseGitBash_.getValue());

      rPrefs.setSourceControlPrefs(prefs);
   }
   
   private boolean haveTerminalPathPref()
   {
      return Desktop.isDesktop() && BrowseCap.isLinux();
   }
   
   private boolean haveGitBashPref()
   {
      return Desktop.isDesktop() && BrowseCap.isWindows();
   }

   private void addTextBoxChooser(Label captionLabel, HyperlinkLabel link,
         String captionPanelStyle, TextBoxWithButton chooser)
   {
      String textWidth = "250px";

      HorizontalPanel captionPanel = new HorizontalPanel();
      captionPanel.setWidth(textWidth);
      nudgeRight(captionPanel);
      if (captionPanelStyle != null)
         captionPanel.addStyleName(captionPanelStyle);

      captionPanel.add(captionLabel);
      captionPanel.setCellHorizontalAlignment(captionLabel,
            HasHorizontalAlignment.ALIGN_LEFT);

      if (link != null)
      {
         HorizontalPanel linkPanel = new HorizontalPanel();
         linkPanel.add(link);
         captionPanel.add(linkPanel);
         captionPanel.setCellHorizontalAlignment(linkPanel,
               HasHorizontalAlignment.ALIGN_RIGHT);

      }

      add(tight(captionPanel));

      chooser.setTextWidth(textWidth);
      nudgeRight(chooser);
      textBoxWithChooser(chooser);
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
      chkUseGitBash_.setVisible(vcsEnabled && haveGitBashPref());
      showSshKeyButton_.setVisible(vcsEnabled
            && originalPrefs_.haveRsaPublicKey());
   }

   private final PreferencesDialogResources res_;

   private final CheckBox chkVcsEnabled_;

   private SourceControlPrefs originalPrefs_;

   private Label svnExePathLabel_;
   private Label gitExePathLabel_;
   private TextBoxWithButton gitExePathChooser_;
   private TextBoxWithButton svnExePathChooser_;
   private Label terminalPathLabel_;
   private TextBoxWithButton terminalPathChooser_;
   private CheckBox chkUseGitBash_;
   private ThemedButton showSshKeyButton_;
}
