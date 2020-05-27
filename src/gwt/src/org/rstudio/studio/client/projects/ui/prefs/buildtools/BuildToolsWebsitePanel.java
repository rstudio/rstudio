/*
 * BuildToolsWebsitePanel.java
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


package org.rstudio.studio.client.projects.ui.prefs.buildtools;

import java.util.ArrayList;

import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.projects.model.RProjectBuildContext;
import org.rstudio.studio.client.projects.model.RProjectBuildOptions;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;

public class BuildToolsWebsitePanel extends BuildToolsPanel
{
   public BuildToolsWebsitePanel()
   {
      pathSelector_ = new DirectorySelector("Site directory:");
      add(pathSelector_);    
       
      websiteOutputFormat_ = new SelectWidget("Book output format(s):",
                                              new String[]{"all"});
      websiteOutputFormat_.addStyleName(RES.styles().websiteOutputFormat());
      add(websiteOutputFormat_);
      websiteOutputFormat_.setVisible(false);
      
      chkPreviewAfterBuilding_ = checkBox("Preview site after building");
      chkPreviewAfterBuilding_.addStyleName(RES.styles().previewWebsite());
      add(chkPreviewAfterBuilding_); 
      
      chkLivePreviewSite_ = checkBox(
            "Re-knit current preview when supporting files change");
      chkLivePreviewSite_.addStyleName(RES.styles().previewWebsite());
      add(chkLivePreviewSite_); 
      
      Label infoLabel = new Label(
         "Supporting files include Rmd partials, R scripts, YAML config files, etc.");
      infoLabel.addStyleName(RES.styles().infoLabel());
      add(infoLabel);
   }

   @Override
   void load(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      pathSelector_.setText(config.getWebsitePath());  
      
      RProjectBuildOptions buildOptions = options.getBuildOptions();
      chkPreviewAfterBuilding_.setValue(buildOptions.getPreviewWebsite());
      chkLivePreviewSite_.setValue(buildOptions.getLivePreviewWebsite());
      
      RProjectBuildContext buildContext = options.getBuildContext();
      if (buildContext.isBookdownSite())
      {
         // change caption
         chkPreviewAfterBuilding_.setText("Preview book after building");
         
         // get all available output formats
         JsArrayString formatsJson = buildContext.getWebsiteOutputFormats();
         ArrayList<String> formatNames = new ArrayList<String>();
         ArrayList<String> formats = new ArrayList<String>();
        
         // always include "All Formats"
         formatNames.add("(All Formats)");
         formats.add("all");
         for (int i = 0; i<formatsJson.length(); i++) 
         {
            formatNames.add(formatsJson.get(i));
            formats.add(formatsJson.get(i));
         }
         websiteOutputFormat_.setChoices(formatNames.toArray(new String[]{}),
                                         formats.toArray(new String[]{}));
         websiteOutputFormat_.setValue(buildOptions.getWebsiteOutputFormat());
         websiteOutputFormat_.setVisible(true);
      }
   }

   @Override
   void save(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setWebsitePath(pathSelector_.getText());
      
      RProjectBuildOptions buildOptions = options.getBuildOptions();
      buildOptions.setPreviewWebsite(chkPreviewAfterBuilding_.getValue());
      buildOptions.setLivePreviewWebsite(chkLivePreviewSite_.getValue());
      buildOptions.setWebsiteOutputFormat(websiteOutputFormat_.getValue());
   }

   private PathSelector pathSelector_;
   
   private CheckBox chkPreviewAfterBuilding_;
   private CheckBox chkLivePreviewSite_;
   
   
   private SelectWidget websiteOutputFormat_;
}
