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
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FileChooserTextBox;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.SourceControlPrefs;

public class SourceControlPreferencesPane extends PreferencesPane
{
   @Inject
   public SourceControlPreferencesPane(PreferencesDialogResources res,
                                       final GlobalDisplay globalDisplay,
                                       RemoteFileSystemContext fsContext,
                                       FileDialogs fileDialogs)
   {
      res_ = res;


      chkVcsEnabled_ = new CheckBox(
            "Enable version control interface for RStudio projects");
      add(chkVcsEnabled_);
      
      
      HorizontalPanel helpPanel = new HorizontalPanel();
      helpPanel.addStyleName(res_.styles().nudgeRight());
      helpPanel.addStyleName(res_.styles().usingVcsHelp());
      Image helpImage = new Image(ThemeResources.INSTANCE.help());
      helpImage.addStyleName(res_.styles().helpImage());
      helpPanel.add(helpImage);
      HyperlinkLabel helpLink = new HyperlinkLabel(
                                       "Using Version Control with RStudio");
      helpLink.addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            globalDisplay.openRStudioLink("using_version_control");
         }  
      });
      helpPanel.add(helpLink);
      add(helpPanel);
      
      // git bin dir chooser  
      gitBinDirChooser_ = new DirectoryChooserTextBox(null,
                                                      "(Not Found)",
                                                      null,
                                                      fileDialogs, 
                                                      fsContext);  
      addTextBoxChooser(new Label("Git bin directory:"), 
                        null, 
                        res_.styles().newSection(), 
                        gitBinDirChooser_);
        
      /*
      TextBoxWithButton svnChooser = new DirectoryChooserTextBox(
                                                               null,
                                                               "(Not Found)",
                                                               null,
                                                               fileDialogs, 
                                                               fsContext);
      svnChooser.setText("");
      addTextBoxChooser("Svn bin directory:", null, null, svnChooser);
      */
      
      // ssh key path
      sshKeyPathChooser_ = new FileChooserTextBox(null,
                                                  "(Not Found)",
                                                  null,
                                                  fileDialogs, 
                                                  fsContext);
      HyperlinkLabel publicKeyLink = new HyperlinkLabel("View public key");
      publicKeyLink.addStyleName(res_.styles().viewPublicKeyLink());
      publicKeyLink.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            
            
         }    
      });    
      Label sshKeyPathLabel = new Label("SSH key path:");
      addTextBoxChooser(sshKeyPathLabel, 
                        publicKeyLink, 
                        res_.styles().newSection(),
                        sshKeyPathChooser_);
      tight(sshKeyPathChooser_);
      
      // ssh key path action buttons
      HorizontalPanel sshButtonPanel = new HorizontalPanel();
      sshButtonPanel.addStyleName(res_.styles().sshButtonPanel());
      createKeyButton_ = new SmallButton();
      createKeyButton_.setText("Create New Key...");
      createKeyButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            // TODO Auto-generated method stub
            
         }
      });
      sshButtonPanel.add(createKeyButton_);
      
      uploadKeyButton_ = new SmallButton();
      uploadKeyButton_.setText("Upload Key...");
      uploadKeyButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            // TODO Auto-generated method stub
            
         }
      });
      if (!Desktop.isDesktop())
         sshButtonPanel.add(uploadKeyButton_);
      
      add(sshButtonPanel);
      
      // hide the ssh UI if we are on linux or mac desktop
      if (Desktop.isDesktop() && 
          (BrowseCap.isMacintosh() || BrowseCap.isLinux()))
      {
         sshKeyPathLabel.setVisible(false);
         publicKeyLink.setVisible(false);
         sshKeyPathChooser_.setVisible(false);
         sshButtonPanel.setVisible(false);
      }
                                      
      chkVcsEnabled_.setEnabled(false);
      gitBinDirChooser_.setEnabled(false);
      sshKeyPathChooser_.setEnabled(false);
      createKeyButton_.setEnabled(false);
      uploadKeyButton_.setEnabled(false);
   }

   @Override
   protected void initializeRPrefs(RPrefs rPrefs)
   {
      // source control prefs
      SourceControlPrefs prefs = rPrefs.getSourceControlPrefs();
      
      chkVcsEnabled_.setEnabled(true);
      gitBinDirChooser_.setEnabled(true);
      sshKeyPathChooser_.setEnabled(true);
      createKeyButton_.setEnabled(true);
      uploadKeyButton_.setEnabled(true);
      
      
      chkVcsEnabled_.setValue(prefs.getVcsEnabled());
      gitBinDirChooser_.setText(prefs.getGitBinDir());
      sshKeyPathChooser_.setText(prefs.getSSHKeyPath());
      
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconSourceControl();
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
                                          sshKeyPathChooser_.getText());
      
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
      captionPanel.addStyleName(res_.styles().nudgeRight());
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
      chooser.addStyleName(res_.styles().nudgeRight());
      chooser.addStyleName(res_.styles().textBoxWithChooser());
      add(chooser);    
   }

   private final PreferencesDialogResources res_;
   
   private final CheckBox chkVcsEnabled_;
   
   private TextBoxWithButton gitBinDirChooser_;
   
   private TextBoxWithButton sshKeyPathChooser_;
   
   private SmallButton createKeyButton_;
   private SmallButton uploadKeyButton_;
   
   

}
