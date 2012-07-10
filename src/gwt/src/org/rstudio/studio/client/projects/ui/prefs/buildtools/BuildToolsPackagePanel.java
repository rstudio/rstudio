/*
 * BuildToolsPackagePanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


package org.rstudio.studio.client.projects.ui.prefs.buildtools;

import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesDialogResources;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;


public class BuildToolsPackagePanel extends BuildToolsPanel
{
   public BuildToolsPackagePanel()
   {
      ProjectPreferencesDialogResources RES =
                              ProjectPreferencesDialogResources.INSTANCE;
      
      pathSelector_ = new DirectorySelector("Package directory:");
      pathSelector_.getElement().getStyle().setMarginBottom(12, Unit.PX);
      add(pathSelector_); 
      
      add(installAdditionalArguments_ = new AdditionalArguments(
                                       "R CMD INSTALL additional options:"));
      
      add(buildAdditionalArguments_ = new AdditionalArguments(
            "R CMD build additional options:"));
      
      add(checkAdditionalArguments_ = new AdditionalArguments(
            "R CMD check additional options:"));
      
      add(chkCleanupAfterCheck_ = new CheckBox(
            "Cleanup output after successful R CMD check"));
      
      roxygenizePanel_ = new VerticalPanel();
      roxygenizePanel_.addStyleName(RES.styles().buildToolsRoxygenize());
      Label roxLabel = new Label("Use Roxygen to automatically generate:");
      roxygenizePanel_.add(roxLabel);
      HorizontalPanel rocletPanel = new HorizontalPanel();
      rocletPanel.setWidth("100%");
      
      chkRoxygenizeRd_ = new CheckBox("Rd files");
      rocletPanel.add(chkRoxygenizeRd_);
      chkRoxygenizeCollate_ = new CheckBox("Collate field");
      rocletPanel.add(chkRoxygenizeCollate_);
      chkRoxygenizeNamespace_ = new CheckBox("NAMESPACE file");
      rocletPanel.add(chkRoxygenizeNamespace_);
      roxygenizePanel_.add(rocletPanel);
      add(roxygenizePanel_);
   }

   @Override
   void load(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      pathSelector_.setText(config.getPackagePath());
      installAdditionalArguments_.setText(config.getPackageInstallArgs());
      buildAdditionalArguments_.setText(config.getPackageBuildArgs());
      checkAdditionalArguments_.setText(config.getPackageCheckArgs());
      chkCleanupAfterCheck_.setValue(
                           options.getBuildOptions().getCleanupAfterCheck());
      
      boolean showRoxygenize = config.hasPackageRoxygenize() ||
                               options.getBuildContext().isRoxygen2Installed();
      if (showRoxygenize)
      {
         roxygenizePanel_.setVisible(true);
         chkRoxygenizeRd_.setValue(config.getPackageRoxygenzieRd());
         chkRoxygenizeCollate_.setValue(config.getPackageRoxygenizeCollate());
         chkRoxygenizeNamespace_.setValue(config.getPackageRoxygenizeNamespace());
      }
      else
      {
         roxygenizePanel_.setVisible(false);
      }
   }

   @Override
   void save(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setPackagePath(pathSelector_.getText());
      config.setPackageInstallArgs(installAdditionalArguments_.getText());
      config.setPackageBuildArgs(buildAdditionalArguments_.getText());
      config.setPackageCheckArgs(checkAdditionalArguments_.getText());
      config.setPackageRoxygenize(chkRoxygenizeRd_.getValue(),
                                  chkRoxygenizeCollate_.getValue(),
                                  chkRoxygenizeNamespace_.getValue());
      options.getBuildOptions().setCleanupAfterCheck(
                                             chkCleanupAfterCheck_.getValue());
   }

   private PathSelector pathSelector_;
   
   private AdditionalArguments installAdditionalArguments_;
   private AdditionalArguments buildAdditionalArguments_;
   private AdditionalArguments checkAdditionalArguments_;
   
   private CheckBox chkCleanupAfterCheck_;
   
   private VerticalPanel roxygenizePanel_;
   private CheckBox chkRoxygenizeRd_;
   private CheckBox chkRoxygenizeCollate_;
   private CheckBox chkRoxygenizeNamespace_;
   
}
