/*
 * ProjectRenvPreferencesPane.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectRenvOptions;
import org.rstudio.studio.client.renv.model.RenvServerOperations;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.projects.RenvContext;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ProjectRenvPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectRenvPreferencesPane(Session session,
                                     RenvServerOperations server,
                                     DependencyManager dependencyManager)
   {
      session_ = session;
      server_ = server;
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
      return "Environments";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      String labelText =
            "RStudio uses the renv package to give your projects their " +
            "own privately-managed package library, making your R code " +
            "more isolated, portable, and reproducible.";

      Label label = new Label(labelText);
      spaced(label);
      add(label);

      RenvContext context = options.getRenvContext();

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

      spaced(chkUseRenv_);
      add(chkUseRenv_);

      // TODO: UI for other renv options / settings

      manageUI(context.active);

      HelpLink helpLink = new HelpLink("Learn more about renv", "renv", false);
      helpLink.getElement().getStyle().setMarginTop(15, Unit.PX);
      nudgeRight(helpLink);
      add(helpLink);
   }

   private void manageUI(boolean enabled)
   {
      // TODO: manage visibility of UI components based on whether renv is enabled
   }

   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      RProjectRenvOptions renvOptions = options.getRenvOptions();

      renvOptions.useRenv = chkUseRenv_.getValue();

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

   // Injected ----
   @SuppressWarnings("unused")
   private final Session session_;
   @SuppressWarnings("unused")
   private final RenvServerOperations server_;
   private final DependencyManager dependencyManager_;



}
