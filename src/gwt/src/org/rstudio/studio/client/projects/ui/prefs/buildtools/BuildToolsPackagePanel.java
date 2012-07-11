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

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.projects.model.RProjectBuildOptions;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesDialogResources;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
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
      HorizontalPanel rocletPanel = new HorizontalPanel();
      chkUseRoxygen_ = new CheckBox("Generate documentation with Roxygen");
      rocletPanel.add(chkUseRoxygen_);
      btnConfigureRoxygen_ = new ThemedButton("Configure...");
      btnConfigureRoxygen_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            new BuildToolsRoxygenOptionsDialog(
               roxygenOptions_,
               new OperationWithInput<BuildToolsRoxygenOptions>() {

                  @Override
                  public void execute(BuildToolsRoxygenOptions input)
                  {
                     roxygenOptions_ = input;
                     chkUseRoxygen_.setValue(input.getRocletRd() || 
                                             input.getRocletCollate() || 
                                             input.getRocletNamespace());
                     
                  }
                  
               }).showModal();
            
         } 
      });
      rocletPanel.add(btnConfigureRoxygen_);
      
      roxygenizePanel_.add(rocletPanel);
      add(roxygenizePanel_);
      
      devtoolsPanel_ = new VerticalPanel();
      devtoolsPanel_.addStyleName(RES.styles().buildToolsDevtools());
      chkDevtoolsLoadAll_ = new CheckBox(
            "Automatically execute devtools load_all when changes occur");
      devtoolsPanel_.add(chkDevtoolsLoadAll_);
      add(devtoolsPanel_);
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
      
      roxygenOptions_ = new BuildToolsRoxygenOptions(
            config.getPackageRoxygenzieRd(),
            config.getPackageRoxygenizeCollate(),
            config.getPackageRoxygenizeNamespace(),
            options.getBuildOptions().getAutoRogyginizeOptions());
       
      boolean showRoxygenize = config.hasPackageRoxygenize() ||
                               options.getBuildContext().isRoxygen2Installed();
      roxygenizePanel_.setVisible(showRoxygenize);
      chkUseRoxygen_.setValue(config.hasPackageRoxygenize());
      chkUseRoxygen_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            if (event.getValue())
            {
               if (!roxygenOptions_.hasActiveRoclet())
                  roxygenOptions_.setRocletRd(true);
               btnConfigureRoxygen_.click();
            }
            else
            {
               roxygenOptions_.clearRoclets();
            }
         }
      });
      
      boolean showDevtools = options.getBuildContext().isDevtoolsInstalled();
      devtoolsPanel_.setVisible(showDevtools);
      chkDevtoolsLoadAll_.setValue(
                           options.getBuildOptions().getAutoExecuteLoadAll());
   }

   @Override
   void save(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setPackagePath(pathSelector_.getText());
      config.setPackageInstallArgs(installAdditionalArguments_.getText());
      config.setPackageBuildArgs(buildAdditionalArguments_.getText());
      config.setPackageCheckArgs(checkAdditionalArguments_.getText());
      config.setPackageRoxygenize(roxygenOptions_.getRocletRd(),
                                  roxygenOptions_.getRocletCollate(),
                                  roxygenOptions_.getRocletNamespace());
      RProjectBuildOptions buildOptions = options.getBuildOptions();
      buildOptions.setCleanupAfterCheck(chkCleanupAfterCheck_.getValue());
      buildOptions.setAutoRoxyginizeOptions(
                                       roxygenOptions_.getAutoRoxygenize());
      buildOptions.setAutoExecuteLoadAll(chkDevtoolsLoadAll_.getValue());
   }

   private PathSelector pathSelector_;
   
   private AdditionalArguments installAdditionalArguments_;
   private AdditionalArguments buildAdditionalArguments_;
   private AdditionalArguments checkAdditionalArguments_;
   
   private CheckBox chkCleanupAfterCheck_;
   
   private BuildToolsRoxygenOptions roxygenOptions_;
   
   private VerticalPanel roxygenizePanel_;
   private CheckBox chkUseRoxygen_;
   private ThemedButton btnConfigureRoxygen_;
   
   private VerticalPanel devtoolsPanel_;
   private CheckBox chkDevtoolsLoadAll_;
}
