/*
 * BuildToolsWebsitePanel.java
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


package org.rstudio.studio.client.projects.ui.prefs.buildtools;

import java.util.ArrayList;

import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
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
      pathSelector_ = new DirectorySelector(constants_.pathSelectorSiteDir());
      add(pathSelector_);    
       
      websiteOutputFormat_ = new SelectWidget(constants_.websiteOutputFormatLabel(),
                                              new String[]{constants_.allLabel()});
      websiteOutputFormat_.addStyleName(RES.styles().websiteOutputFormat());
      add(websiteOutputFormat_);
      websiteOutputFormat_.setVisible(false);
      
      chkPreviewAfterBuilding_ = checkBox(constants_.chkPreviewAfterBuildingCaption());
      chkPreviewAfterBuilding_.addStyleName(RES.styles().previewWebsite());
      add(chkPreviewAfterBuilding_); 
      
      chkLivePreviewSite_ = checkBox(
            constants_.chkLivePreviewSiteCaption());
      chkLivePreviewSite_.addStyleName(RES.styles().previewWebsite());
      add(chkLivePreviewSite_); 
      
      Label infoLabel = new Label(
         constants_.infoLabel());
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
         chkPreviewAfterBuilding_.setText(constants_.chkPreviewAfterBuilding());
         
         // get all available output formats
         JsArrayString formatsJson = buildContext.getWebsiteOutputFormats();
         ArrayList<String> formatNames = new ArrayList<>();
         ArrayList<String> formats = new ArrayList<>();
        
         // always include "All Formats"
         formatNames.add(constants_.allFormatsLabel());
         formats.add(constants_.allLabel());
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
   private static final StudioClientProjectConstants constants_ = com.google.gwt.core.client.GWT.create(StudioClientProjectConstants.class);
}
