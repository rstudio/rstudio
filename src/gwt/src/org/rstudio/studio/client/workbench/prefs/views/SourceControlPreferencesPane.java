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

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.vcs.SshKeyChooser;
import org.rstudio.studio.client.common.vcs.VCSHelpLink;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
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
                                       VCSServerOperations server,
                                       RemoteFileSystemContext fsContext,
                                       FileDialogs fileDialogs)
   {
      res_ = res;
      server_ = server;

      chkVcsEnabled_ = new CheckBox(
            "Enable version control interface for RStudio projects");
      extraSpaced(chkVcsEnabled_);
      add(chkVcsEnabled_);
      
      
      // git bin dir chooser  
      gitBinDirChooser_ = new DirectoryChooserTextBox(null,
                                                      "(Not Found)",
                                                      null,
                                                      fileDialogs, 
                                                      fsContext);  
      addTextBoxChooser(new Label("Git bin directory:"), 
                        null, 
                        null, 
                        gitBinDirChooser_);
        
      /*
      TextBoxWithButton svnChooser = new DirectoryChooserTextBox(
                                                               null,
                                                               "(Not Found)",
                                                               null,
                                                               fileDialogs, 
                                                               fsContext);
      svnChooser.setText("");
      addTextBoxChooser(new Label("Svn bin directory:"), null, null, svnChooser);
      */
      
      // ssh key path
      String keyDir = session.getSessionInfo().getDefaultSSHKeyDir();
      sshKeyChooser_ = new SshKeyChooser(server_, keyDir, "250px");                               
      nudgeRight(sshKeyChooser_);
      add(sshKeyChooser_);
     
      // manipulate visiblity of ssh ui depending on mode/platform
      if (!SshKeyChooser.isSupportedForCurrentPlatform())
         sshKeyChooser_.setVisible(false);
         
      // if the ssh key chooser is visible then mark it as a new section
      if (sshKeyChooser_.isVisible())
         sshKeyChooser_.addStyleName(res_.styles().newSection());
      
      
      VCSHelpLink vcsHelpLink = new VCSHelpLink();
      nudgeRight(vcsHelpLink);
      if (sshKeyChooser_.isVisible())
         vcsHelpLink.addStyleName(res_.styles().usingVcsHelp());
      else
         vcsHelpLink.addStyleName(res_.styles().usingVcsHelpNoSsh()); 
      add(vcsHelpLink);
                                      
      chkVcsEnabled_.setEnabled(false);
      gitBinDirChooser_.setEnabled(false);
   }

   @Override
   protected void initialize(RPrefs rPrefs)
   {
      // source control prefs
      SourceControlPrefs prefs = rPrefs.getSourceControlPrefs();
      
      chkVcsEnabled_.setEnabled(true);
      gitBinDirChooser_.setEnabled(true);
      
      chkVcsEnabled_.setValue(prefs.getVcsEnabled());
      gitBinDirChooser_.setText(prefs.getGitBinDir());
      sshKeyChooser_.setSshKey(prefs.getSSHKeyPath()); 
      sshKeyChooser_.setProgressIndicator(getProgressIndicator());
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
                                          chkVcsEnabled_.getValue(),
                                          gitBinDirChooser_.getText(),
                                          sshKeyChooser_.getSshKey()); 
      
      rPrefs.setSourceControlPrefs(prefs);
   }
   
  
   
   private void addTextBoxChooser(Label captionLabel, 
                                  HyperlinkLabel link,
                                  String captionPanelStyle,
                                  TextBoxWithButton chooser)
   {
      String textWidth = "250px";
      
      HorizontalPanel captionPanel = new HorizontalPanel();
      captionPanel.setWidth(textWidth);
      nudgeRight(captionPanel);
      if (captionPanelStyle != null)
         captionPanel.addStyleName(captionPanelStyle);
      
      captionPanel.add(captionLabel);
      captionPanel.setCellHorizontalAlignment(
                                          captionLabel,
                                          HasHorizontalAlignment.ALIGN_LEFT);
      
      if (link != null)
      {
         HorizontalPanel linkPanel = new HorizontalPanel();
         linkPanel.add(link);
         captionPanel.add(linkPanel);
         captionPanel.setCellHorizontalAlignment(
                                           linkPanel, 
                                           HasHorizontalAlignment.ALIGN_RIGHT);
       
      }
      
      add(tight(captionPanel));
      
      chooser.setTextWidth(textWidth);
      nudgeRight(chooser);
      textBoxWithChooser(chooser);
      add(chooser);    
   }
   

   private final PreferencesDialogResources res_;
   
   private final VCSServerOperations server_;
 
   
   private final CheckBox chkVcsEnabled_;
   
   private TextBoxWithButton gitBinDirChooser_;
   
   private SshKeyChooser sshKeyChooser_;
}
