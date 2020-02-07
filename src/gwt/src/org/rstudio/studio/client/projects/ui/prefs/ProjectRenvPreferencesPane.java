/*
 * ProjectRenvPreferencesPane.java
 *
 * Copyright (C) 2009-19 by RStudio, PBC
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
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectRenvOptions;
import org.rstudio.studio.client.renv.model.RenvServerOperations;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.projects.RenvContext;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ProjectRenvPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectRenvPreferencesPane(Session session,
                                     RenvServerOperations server,
                                     UserPrefs prefs,
                                     DependencyManager dependencyManager)
   {
      session_ = session;
      server_ = server;
      prefs_ = prefs;
      dependencyManager_ = dependencyManager;
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(ProjectPreferencesDialogResources.INSTANCE.iconRenv2x());
   }

   @Override
   public String getName()
   {
      return "renv";
   }
   
   private void createWidgets(RenvContext context)
   {
      // Enable / disable renv
      chkUseRenv_ = new CheckBox("Use renv with this project");
      chkUseRenv_.setValue(context.active);
      chkUseRenv_.addValueChangeHandler((ValueChangeEvent<Boolean> event) -> {

         if (event.getValue())
         {
            dependencyManager_.withRenv("Using renv", (Boolean success) -> manageUI(success));
         }
         else
         {
            manageUI(false);
         }

      });
      
      uiContainer_ = new FlowPanel();
      
      // Project settings
      cbUseCache_ = new CheckBox("Use global package cache");
      cbUseCache_.setValue(context.settings.getBool("use.cache"));
      
      cbVcsIgnoreLibrary_ = new CheckBox("Exclude project library from version control");
      cbVcsIgnoreLibrary_.setValue(context.settings.getBool("vcs.ignore.library"));
      
      uiContainer_.add(header("Project Settings"));
      uiContainer_.add(lessSpaced(cbVcsIgnoreLibrary_));
      uiContainer_.add(lessSpaced(cbUseCache_));
      uiContainer_.add(spaced(info("See ?renv::settings for more information.")));

      // User-level configuration
      cbSandboxEnabled_ = new CheckBox("Enable sandboxing of system library");
      cbSandboxEnabled_.setValue(prefs_.renvSandboxEnabled().getValue());
      
      cbShimsEnabled_ = new CheckBox("Enable renv shims");
      cbShimsEnabled_.setValue(prefs_.renvShimsEnabled().getValue());
      
      cbUpdatesCheck_ = new CheckBox("Check for package updates when session is initialized");
      cbUpdatesCheck_.setValue(prefs_.renvUpdatesCheck().getValue());
      
      uiContainer_.add(header("User Configuration"));
      uiContainer_.add(lessSpaced(cbSandboxEnabled_));
      uiContainer_.add(lessSpaced(cbShimsEnabled_));
      uiContainer_.add(lessSpaced(cbUpdatesCheck_));
      uiContainer_.add(spaced(info("See ?renv::config for more information.")));
      
      helpLink_ = new HelpLink("Learn more about renv", "renv", false);
      helpLink_.getElement().getStyle().setMarginTop(15, Unit.PX);
      nudgeRight(helpLink_);
      
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      String labelText =
            "RStudio uses the renv package to give your projects their " +
            "own project-local library of R packages, making your projects " +
            "more isolated, portable, and reproducible.";
            
      Label label = new Label(labelText);
      spaced(label);
      add(label);

      RenvContext context = options.getRenvContext();
      createWidgets(context);
      
      DockLayoutPanel panel = new DockLayoutPanel(Unit.PX);
      panel.setHeight("350px");
      panel.addNorth(chkUseRenv_, 30);
      panel.addSouth(helpLink_, 30);
      panel.add(uiContainer_);
      add(panel);
      
      manageUI(context.active);

   }
   
   private void manageUI(boolean enabled)
   {
      uiContainer_.setVisible(enabled);
   }

   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      RProjectRenvOptions renvOptions = options.getRenvOptions();
      
      renvOptions.useRenv = chkUseRenv_.getValue();
      
      renvOptions.projectUseCache = cbUseCache_.getValue();
      renvOptions.projectVcsIgnoreLibrary = cbVcsIgnoreLibrary_.getValue();
      
      boolean sandboxEnabled = cbSandboxEnabled_.getValue();
      boolean shimsEnabled = cbShimsEnabled_.getValue();
      boolean updatesCheck = cbUpdatesCheck_.getValue();
      
      // TODO: This doesn't seem to work?
      if (prefs_.renvSandboxEnabled().getValue() != sandboxEnabled)
         prefs_.renvSandboxEnabled().setProjectValue(sandboxEnabled);
      
      if (prefs_.renvShimsEnabled().getValue() != shimsEnabled)
         prefs_.renvShimsEnabled().setProjectValue(shimsEnabled);
      
      if (prefs_.renvUpdatesCheck().getValue() != updatesCheck)
         prefs_.renvUpdatesCheck().setProjectValue(updatesCheck);
      
      renvOptions.userSandboxEnabled = sandboxEnabled;
      renvOptions.userShimsEnabled = shimsEnabled;
      renvOptions.userUpdatesCheck = updatesCheck;
      
      return new RestartRequirement();
   }
   
   
   interface Resources extends ClientBundle
   {
      @Source("ProjectRenvPreferencesPane.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   
   public interface Styles extends CssResource
   {
   }
   
   static
   {
      RES.styles().ensureInjected();
   }
 
   private CheckBox chkUseRenv_;
   private FlowPanel uiContainer_;
   private HelpLink helpLink_;
   
   // Project settings
   private CheckBox cbUseCache_;
   private CheckBox cbVcsIgnoreLibrary_;
   
   // User config
   private CheckBox cbSandboxEnabled_;
   private CheckBox cbShimsEnabled_;
   private CheckBox cbUpdatesCheck_;
   
   
   // Injected ----
   private final Session session_;
   private final RenvServerOperations server_;
   private final UserPrefs prefs_;
   private final DependencyManager dependencyManager_;
   
   
   
}
