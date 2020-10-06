/*
 * ProjectPreferencesPane.java
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

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.PreferencesDialogPaneBase;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;


public abstract class ProjectPreferencesPane 
                     extends PreferencesDialogPaneBase<RProjectOptions>
{
   
   @Inject
   void injectMembers(GlobalDisplay globalDisplay,
                      Session session, 
                      EventBus eventBus)
   {
      globalDisplay_ = globalDisplay;
      session_ = session;
      eventBus_ = eventBus;
   }
   
   protected void addHeader(String caption)
   {
      PreferencesDialogBaseResources baseRes =
                              PreferencesDialogBaseResources.INSTANCE;
      Label headerLabel = new Label(caption);
      headerLabel.addStyleName(baseRes.styles().headerLabel());
      nudgeRight(headerLabel);
      add(headerLabel);
   }

   protected void promptToRestart()
   {
      globalDisplay_.showYesNoMessage(
         MessageDialog.QUESTION,
         "Confirm Restart RStudio", 
         "You need to restart RStudio in order for this change to take " +
         "effect. Do you want to do this now?",
         new Operation()
         {
            @Override
            public void execute()
            {
               forceClosed(new Command() {

                  @Override
                  public void execute()
                  {
                     SwitchToProjectEvent event = new SwitchToProjectEvent(
                           session_.getSessionInfo().getActiveProjectFile());
                     eventBus_.fireEvent(event);
                     
                  }
                  
               });
            }  
         },
         true);
   }
   
   protected static final ProjectPreferencesDialogResources RESOURCES =
                           ProjectPreferencesDialogResources.INSTANCE;
   
   private GlobalDisplay globalDisplay_;
   private Session session_;
   private EventBus eventBus_;
}
