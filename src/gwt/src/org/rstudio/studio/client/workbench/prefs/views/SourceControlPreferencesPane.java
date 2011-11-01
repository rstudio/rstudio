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
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.TextBoxWithButton;
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
      
      Label gitBinDirLabel = new Label("Git bin directory:");
      gitBinDirLabel.addStyleName(res_.styles().nudgeRight());
      add(tight(gitBinDirLabel));
      
      add(gitBinDirChooser_ = new DirectoryChooserTextBox(null,
                                                         "(Not Found)",
                                                          null,
                                                          fileDialogs, 
                                                          fsContext));  
      gitBinDirChooser_.addStyleName(res_.styles().spaced());
      gitBinDirChooser_.addStyleName(res_.styles().nudgeRight());
      gitBinDirChooser_.addStyleName(res_.styles().textBoxWithChooser());
      
      
      
      chkVcsEnabled_.setEnabled(false);
      gitBinDirChooser_.setEnabled(false);
   }

   @Override
   protected void initializeRPrefs(RPrefs rPrefs)
   {
      // source control prefs
      SourceControlPrefs prefs = rPrefs.getSourceControlPrefs();
      
      chkVcsEnabled_.setEnabled(true);
      gitBinDirChooser_.setEnabled(true);
      
      
      chkVcsEnabled_.setValue(prefs.getVcsEnabled());
      gitBinDirChooser_.setText(prefs.getGitBinDir());
      
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
                                          gitBinDirChooser_.getText());
      
      rPrefs.setSourceControlPrefs(prefs);
   }

   private final PreferencesDialogResources res_;
   
   private final CheckBox chkVcsEnabled_;
   
   private TextBoxWithButton gitBinDirChooser_;

}
