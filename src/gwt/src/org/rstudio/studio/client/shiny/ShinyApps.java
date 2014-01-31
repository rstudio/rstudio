/*
 * ShinyApps.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.shiny;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.shiny.model.ShinyAppsServerOperations;
import org.rstudio.studio.client.shiny.events.ShinyAppsActionEvent;
import org.rstudio.studio.client.shiny.events.ShinyAppsDeployInitiatedEvent;
import org.rstudio.studio.client.shiny.model.ShinyAppsDeploymentRecord;
import org.rstudio.studio.client.shiny.model.ShinyAppsDirectoryState;
import org.rstudio.studio.client.shiny.ui.ShinyAppsAccountManagerDialog;
import org.rstudio.studio.client.shiny.ui.ShinyAppsDeployDialog;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShinyApps implements SessionInitHandler, 
                                  ShinyAppsActionEvent.Handler,
                                  ShinyAppsDeployInitiatedEvent.Handler
{
   public interface Binder
           extends CommandBinder<Commands, ShinyApps> {}

   @Inject
   public ShinyApps(EventBus events, 
                    Commands commands, 
                    Session session,
                    GlobalDisplay display, 
                    Binder binder, 
                    ShinyAppsServerOperations server)
   {
      commands_ = commands;
      display_ = display;
      session_ = session;
      server_ = server;
      events_ = events;

      binder.bind(commands, this);

      events.addHandler(SessionInitEvent.TYPE, this);
      events.addHandler(ShinyAppsActionEvent.TYPE, this); 
      events.addHandler(ShinyAppsDeployInitiatedEvent.TYPE, this); 
   }
   
   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      // Deployment-related ShinyApps commands are invisible by default; they
      // will be set to visible by the source pane if a Shiny file is open and
      // the ShinyApps package is installed.
      commands_.shinyAppsDeploy().setVisible(false);
      commands_.shinyAppsTerminate().setVisible(false);
      
      // "Manage accounts" can be invoked any time the package is available
      commands_.shinyAppsManageAccounts().setVisible(
            session_.getSessionInfo().getShinyappsInstalled());
      
      // This object keeps track of the most recent deployment we made of each
      // directory.
      new JSObjectStateValue(
            "shinyapps",
            "shinyAppsDirectories",
            ClientState.PERSISTENT,
            session_.getSessionInfo().getClientState(),
            false)
       {
          @Override
          protected void onInit(JsObject value)
          {
             dirState_ = (ShinyAppsDirectoryState) (value == null ?
                   ShinyAppsDirectoryState.create() :
                   value.cast());
          }
   
          @Override
          protected JsObject getValue()
          {
             dirStateDirty_ = false;
             return (JsObject) (dirState_ == null ?
                   ShinyAppsDirectoryState.create().cast() :
                   dirState_.cast());
          }
   
          @Override
          protected boolean hasChanged()
          {
             return dirStateDirty_;
          }
       };
   }
   
   @Override
   public void onShinyAppsAction(ShinyAppsActionEvent event)
   {
      if (event.getAction() == ShinyAppsActionEvent.ACTION_TYPE_DEPLOY)
      {
         String dir = FilePathUtils.dirFromFile(event.getPath());
         ShinyAppsDeploymentRecord record = dirState_.getLastDeployment(dir);
         String lastAccount = null;
         String lastAppName = null;
         if (record != null)
         {
            lastAccount = record.getAccount();
            lastAppName = record.getName();
         }
         ShinyAppsDeployDialog dialog = 
               new ShinyAppsDeployDialog(
                         server_, display_, events_, 
                         dir, lastAccount, lastAppName);
         dialog.showModal();
      }
   }
   
   @Override
   public void onShinyAppsDeployInitiated(ShinyAppsDeployInitiatedEvent event)
   {
      dirState_.addDeployment(event.getPath(), event.getRecord());
      dirStateDirty_ = true;
   }

   @Handler
   public void onShinyAppsManageAccounts()
   {
      ShinyAppsAccountManagerDialog dialog = 
            new ShinyAppsAccountManagerDialog(server_, display_);
      dialog.showModal();
   }
   
   private final Commands commands_;
   private final GlobalDisplay display_;
   private final Session session_;
   private final ShinyAppsServerOperations server_;
   private final EventBus events_;
   
   private ShinyAppsDirectoryState dirState_;
   private boolean dirStateDirty_ = false;
}
