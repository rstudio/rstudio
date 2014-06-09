/*
 * ProjectPackratPreferencesPane.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ProjectPackratPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectPackratPreferencesPane(WorkbenchContext workbenchContext,
                                        final Session session,
                                        final EventBus eventBus)
   {
      workbenchContext_ = workbenchContext;
      session_ = session;
      eventBus_ = eventBus;
   }
   
   private void addBootstrapUI()
   {
      Label label = new Label(
            "Packrat is a dependency management tool that makes your " +
            "R code more isolated, portable, and reproducible by " +
            "giving your project its own privately managed package " +
            "library."
        );
        extraSpaced(label);
        add(label);
        
        ThemedButton button = new ThemedButton(
           "Use Packrat with this Project",
           new ClickHandler() {

              @Override
              public void onClick(ClickEvent event)
              {
                 forceClosed(new Command() { public void execute()
                 {
                    // determine whether we need to add a project arg
                    String projectArg = "";
                    FileSystemItem projectDir = session_.getSessionInfo()
                                                     .getActiveProjectDir();
                    FileSystemItem workingDir = workbenchContext_
                                                     .getCurrentWorkingDir();
                    if (!projectDir.equalTo(workingDir))
                       projectArg = "project = '" + projectDir.getPath() + "'";
                    
                    String cmd = "packrat::bootstrap(" + projectArg + ")";
                    
                    eventBus_.fireEvent(new SendToConsoleEvent(cmd, 
                                                               true, 
                                                               true));
                 }});
              }
              
           });
        extraSpaced(button);
        add(button);
        
        HelpLink helpLink = new HelpLink("Learn more about Packrat", "packrat");
        nudgeRight(helpLink);
        add(helpLink);
   }
   
   private void addOptionsUI()
   {
      
   }

   @Override
   public ImageResource getIcon()
   {
      return ProjectPreferencesDialogResources.INSTANCE.iconPackrat();
   }

   @Override
   public String getName()
   {
      return "Packrat";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      if (!options.getPackratContext().isPackified())
      {
         addBootstrapUI();
      }
      else
      {
         addOptionsUI();
      }
      
   }

   @Override
   public boolean onApply(RProjectOptions options)
   {
      return false;
   }
  
   private final WorkbenchContext workbenchContext_;
   private final Session session_;
   private final EventBus eventBus_;
   
}
