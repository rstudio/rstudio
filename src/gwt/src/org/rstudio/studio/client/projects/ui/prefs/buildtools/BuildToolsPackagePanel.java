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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.CheckBox;


public class BuildToolsPackagePanel extends BuildToolsPanel
{
   public BuildToolsPackagePanel()
   {
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
   }

   @Override
   void save(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setPackagePath(pathSelector_.getText());
      config.setPackageInstallArgs(installAdditionalArguments_.getText());
      config.setPackageBuildArgs(buildAdditionalArguments_.getText());
      config.setPackageCheckArgs(checkAdditionalArguments_.getText());
      options.getBuildOptions().setCleanupAfterCheck(
                                             chkCleanupAfterCheck_.getValue());
   }

   private PathSelector pathSelector_;
   
   private AdditionalArguments installAdditionalArguments_;
   private AdditionalArguments buildAdditionalArguments_;
   private AdditionalArguments checkAdditionalArguments_;
   
   private CheckBox chkCleanupAfterCheck_;
}
