/*
 * ProjectBuildToolsPreferencesPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import java.util.HashMap;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesDialogResources;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesPane;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public class ProjectBuildToolsPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectBuildToolsPreferencesPane(final Session session,
                                      GlobalDisplay globalDisplay)
   {
      session_ = session;
      globalDisplay_ = globalDisplay;
     
      buildTypeSelect_ = new BuildTypeSelectWidget();
      spaced(buildTypeSelect_);
      add(buildTypeSelect_);
      
      buildToolsPanel_ = new SimplePanel();
      buildToolsPanel_.addStyleName(RES.styles().buildToolsPanel());
      buildToolsPanel_.getElement().getStyle().setMarginLeft(
                                 BrowseCap.isFirefox() ? 1 : 4, Unit.PX);
      add(buildToolsPanel_);
      
      packagePanel_ = new BuildToolsPackagePanel();
      buildToolsPanels_.put(RProjectConfig.BUILD_TYPE_PACKAGE, 
                            packagePanel_);
      
      buildToolsPanels_.put(RProjectConfig.BUILD_TYPE_MAKEFILE, 
                            new BuildToolsMakefilePanel());
      
      buildToolsPanels_.put(RProjectConfig.BUILD_TYPE_WEBSITE, 
                            new BuildToolsWebsitePanel());
      
      buildToolsPanels_.put(RProjectConfig.BUILD_TYPE_CUSTOM, 
                            new BuildToolsCustomPanel());
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(RES.iconBuild2x());
   }

   @Override
   public String getName()
   {
      return "Build Tools";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      initialConfig_ = options.getConfig();
      
      String buildType = initialConfig_.getBuildType();
      buildTypeSelect_.setValue(initialConfig_.getBuildType());
      
      for (BuildToolsPanel panel : buildToolsPanels_.values())
         panel.load(options);
      
      manageBuildToolsPanel(buildType);
      
      // if the initial type isn't package then we need to provide package
      // defaults if the user switches to a package
      providePackageBuildTypeDefaults_ = 
            !buildType.equals(RProjectConfig.BUILD_TYPE_PACKAGE);
   }
   
   @Override
   public boolean onApply(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
         
      config.setBuildType(buildTypeSelect_.getValue());
      
      for (BuildToolsPanel panel : buildToolsPanels_.values())
         panel.save(options);
     
      // require reload if the build type or roxygen settings changed
      String initialBuildType = initialConfig_.getBuildType();
      String selectedBuildType = buildTypeSelect_.getValue();
        
      return !initialBuildType.equals(selectedBuildType);
   }
   
   
   @Override
   public boolean validate()
   {
      String buildType = buildTypeSelect_.getValue();
      if (buildToolsPanels_.containsKey(buildType))
         return buildToolsPanels_.get(buildType).validate();
      else
         return true;
   }
   
   private void manageBuildToolsPanel(String buildType)
   {
      if (buildToolsPanels_.containsKey(buildType))
         buildToolsPanel_.setWidget(buildToolsPanels_.get(buildType));
      else
         buildToolsPanel_.setWidget(null);
   }

   
   private class BuildTypeSelectWidget extends SelectWidget
   {
      public BuildTypeSelectWidget()
      {
         super("Project build tools:",
               new String[]{"(" + RProjectConfig.BUILD_TYPE_NONE + ")", 
                             RProjectConfig.BUILD_TYPE_PACKAGE, 
                             RProjectConfig.BUILD_TYPE_MAKEFILE,
                             RProjectConfig.BUILD_TYPE_WEBSITE,
                             RProjectConfig.BUILD_TYPE_CUSTOM},
               new String[]{RProjectConfig.BUILD_TYPE_NONE, 
                            RProjectConfig.BUILD_TYPE_PACKAGE, 
                            RProjectConfig.BUILD_TYPE_MAKEFILE,
                            RProjectConfig.BUILD_TYPE_WEBSITE,
                            RProjectConfig.BUILD_TYPE_CUSTOM},
               false,
               true,
               false);
         
         addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event)
            {
               String buildType = getValue();
               if (buildType.equals(RProjectConfig.BUILD_TYPE_PACKAGE) &&
                   providePackageBuildTypeDefaults_)
               {
                  providePackageBuildTypeDefaults_ = false;
                  packagePanel_.provideDefaults();
               }
               
               manageBuildToolsPanel(buildType);
            }
         });
      }
   }
   
   private boolean providePackageBuildTypeDefaults_ = false;
   private final BuildToolsPanel packagePanel_;
  
   @SuppressWarnings("unused")
   private final Session session_;
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   
   private RProjectConfig initialConfig_;
   private BuildTypeSelectWidget buildTypeSelect_;
   
   private SimplePanel buildToolsPanel_;
   private HashMap<String, BuildToolsPanel> buildToolsPanels_ = 
                                 new HashMap<String, BuildToolsPanel>();
  
   private static final ProjectPreferencesDialogResources RES = 
                                    ProjectPreferencesDialogResources.INSTANCE;
}
