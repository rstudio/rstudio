/*
 * BuildToolsPackagePanel.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.projects.ui.prefs.buildtools;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.PackagesHelpLink;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.RProjectBuildOptions;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesDialogResources;
import org.rstudio.studio.client.workbench.WorkbenchContext;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;


public class BuildToolsPackagePanel extends BuildToolsPanel
{
   public BuildToolsPackagePanel()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      ProjectPreferencesDialogResources RES =
                              ProjectPreferencesDialogResources.INSTANCE;
      
      pathSelector_ = new DirectorySelector(constants_.pathSelectorPackageDir());
      pathSelector_.getElement().getStyle().setMarginBottom(10, Unit.PX);
      add(pathSelector_); 
      pathSelector_.addValueChangeHandler(new ValueChangeHandler<String>() {

         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            if (pathSelector_.getText() ==
                           workbenchContext_.getActiveProjectDir().getPath()) 
            {
               pathSelector_.setText("");
            }
         }
         
      });
      
      cleanBeforeInstall_ = checkBox(constants_.cleanBeforeInstallLabel());
      cleanBeforeInstall_.addStyleName(RES.styles().buildToolsCleanBeforeInstall());
      add(cleanBeforeInstall_);
      
      chkUseDevtools_ = checkBox(constants_.chkUseDevtoolsCaption());
      chkUseDevtools_.addStyleName(RES.styles().buildToolsDevtools());
      add(chkUseDevtools_);
      
      roxygenizePanel_ = new VerticalPanel();
      roxygenizePanel_.addStyleName(RES.styles().buildToolsRoxygenize());
      HorizontalPanel rocletPanel = new HorizontalPanel();
      chkUseRoxygen_ = checkBox(constants_.chkUseRoxygenCaption());
      rocletPanel.add(chkUseRoxygen_);
      btnConfigureRoxygen_ = new SmallButton(constants_.btnConfigureRoxygenLabel());
      btnConfigureRoxygen_.getElement().getStyle().setMarginLeft(12, Unit.PX);
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
                                             input.getRocletNamespace() ||
                                             input.getRocletVignette());
                     
                  }
                  
               }).showModal();
            
         } 
      });
      rocletPanel.add(btnConfigureRoxygen_);
      roxygenizePanel_.add(rocletPanel);
      add(roxygenizePanel_);

      add(installAdditionalArguments_ = new AdditionalArguments(
            constants_.installMdashArgument()));
     
      add(checkAdditionalArguments_ = new AdditionalArguments(
            constants_.checkPackageMdashArgument()));
      
      add(buildAdditionalArguments_ = new AdditionalArguments(
            constants_.buildSourceMdashArgument()));
           
      add(buildBinaryAdditionalArguments_ = new AdditionalArguments(
            constants_.buildBinaryMdashArgument()));
      
      HelpLink packagesHelpLink = new PackagesHelpLink();
      packagesHelpLink.getElement().getStyle().setMarginTop(7, Unit.PX);
      add(packagesHelpLink);
     }
   
   @Inject
   public void initialize(WorkbenchContext workbenchContext)
   {
      workbenchContext_ = workbenchContext;
   }
   
   @Override
   protected void provideDefaults()
   {
      installAdditionalArguments_.setText("--no-multiarch --with-keep.source");
      chkUseDevtools_.setValue(true);
      cleanBeforeInstall_.setValue(true);
   }

   @Override
   void load(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      pathSelector_.setText(config.getPackagePath());
      installAdditionalArguments_.setText(config.getPackageInstallArgs());
      buildAdditionalArguments_.setText(config.getPackageBuildArgs());
      buildBinaryAdditionalArguments_.setText(config.getPackageBuildBinaryArgs());
      checkAdditionalArguments_.setText(config.getPackageCheckArgs());
   
      roxygenOptions_ = new BuildToolsRoxygenOptions(
            config.getPackageRoxygenzieRd(),
            config.getPackageRoxygenizeCollate(),
            config.getPackageRoxygenizeNamespace(),
            config.getPackageRoxygenizeVignette(),
            options.getBuildOptions().getAutoRogyginizeOptions());
       
      boolean showRoxygenize = config.hasPackageRoxygenize() ||
                               options.getBuildContext().isRoxygen2Installed();
      roxygenizePanel_.setVisible(showRoxygenize);
      cleanBeforeInstall_.setValue(config.getPackageCleanBeforeInstall());
      chkUseDevtools_.setValue(config.getPackageUseDevtools());
      chkUseRoxygen_.setValue(config.hasPackageRoxygenize());
      chkUseRoxygen_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            if (event.getValue())
            {
               if (!roxygenOptions_.hasActiveRoclet())
               {
                  roxygenOptions_.setRocletRd(true);
                  roxygenOptions_.setRocletCollate(true);
                  roxygenOptions_.setRocletNamespace(true);
               }
               btnConfigureRoxygen_.click();
            }
            else
            {
               roxygenOptions_.clearRoclets();
            }
         }
      });
   }

   @Override
   void save(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setPackageUseDevtools(chkUseDevtools_.getValue());
      config.setPackageCleanBeforeInstall(cleanBeforeInstall_.getValue());
      config.setPackagePath(pathSelector_.getText());
      config.setPackageInstallArgs(installAdditionalArguments_.getText());
      config.setPackageBuildArgs(buildAdditionalArguments_.getText());
      config.setPackageBuildBinaryArgs(buildBinaryAdditionalArguments_.getText());
      config.setPackageCheckArgs(checkAdditionalArguments_.getText());
      config.setPackageRoxygenize(roxygenOptions_.getRocletRd(),
                                  roxygenOptions_.getRocletCollate(),
                                  roxygenOptions_.getRocletNamespace(),
                                  roxygenOptions_.getRocletVignette());
      RProjectBuildOptions buildOptions = options.getBuildOptions();
      buildOptions.setAutoRoxyginizeOptions(
                                       roxygenOptions_.getAutoRoxygenize());
   }

   private PathSelector pathSelector_;
   
   private AdditionalArguments installAdditionalArguments_;
   private AdditionalArguments buildAdditionalArguments_;
   private AdditionalArguments buildBinaryAdditionalArguments_;
   private AdditionalArguments checkAdditionalArguments_;
     
   private BuildToolsRoxygenOptions roxygenOptions_;
   
   private VerticalPanel roxygenizePanel_;
   private CheckBox chkUseRoxygen_;
   private CheckBox cleanBeforeInstall_;
   private CheckBox chkUseDevtools_;
   private SmallButton btnConfigureRoxygen_;
   
   private WorkbenchContext workbenchContext_;
   private static final StudioClientProjectConstants constants_ = com.google.gwt.core.client.GWT.create(StudioClientProjectConstants.class);
}
