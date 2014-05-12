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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.shiny.model.ShinyAppsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.events.ShinyAppsActionEvent;
import org.rstudio.studio.client.shiny.events.ShinyAppsDeployInitiatedEvent;
import org.rstudio.studio.client.shiny.events.ShinyAppsDeploymentCompletedEvent;
import org.rstudio.studio.client.shiny.events.ShinyAppsDeploymentStartedEvent;
import org.rstudio.studio.client.shiny.model.ShinyAppsApplicationInfo;
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
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShinyApps implements SessionInitHandler, 
                                  ShinyAppsActionEvent.Handler,
                                  ShinyAppsDeployInitiatedEvent.Handler,
                                  ShinyAppsDeploymentCompletedEvent.Handler
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
      events.addHandler(ShinyAppsDeploymentCompletedEvent.TYPE, this); 
   }
   
   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      // "Manage accounts" can be invoked any time the package is available
      commands_.shinyAppsManageAccounts().setVisible(
            session_.getSessionInfo().getShinyappsInstalled());
      
      // This object keeps track of the most recent deployment we made of each
      // directory, and is used to default directory deployments to last-used
      // settings.
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
         final String dir = FilePathUtils.dirFromFile(event.getPath());
         ShinyAppsDeploymentRecord record = dirState_.getLastDeployment(dir);
         final String lastAccount = record == null ? null : record.getAccount();
         final String lastAppName = record == null ? null : record.getName();

         ShinyAppsDeployDialog dialog = 
               new ShinyAppsDeployDialog(
                         server_, display_, events_, 
                         dir, lastAccount, lastAppName);
         dialog.showModal();
      }
      else if (event.getAction() == ShinyAppsActionEvent.ACTION_TYPE_TERMINATE)
      {
         terminateShinyApp(FilePathUtils.dirFromFile(event.getPath()));
      }
   }
   
   @Override
   public void onShinyAppsDeployInitiated(
         final ShinyAppsDeployInitiatedEvent event)
   {
      server_.deployShinyApp(event.getPath(), 
                             event.getRecord().getAccount(), 
                             event.getRecord().getName(), 
      new ServerRequestCallback<Boolean>()
      {
         @Override
         public void onResponseReceived(Boolean status)
         {
            if (status)
            {
               dirState_.addDeployment(event.getPath(), event.getRecord());
               dirStateDirty_ = true;
               launchBrowser_ = event.getLaunchBrowser();
               events_.fireEvent(new ShinyAppsDeploymentStartedEvent(
                     event.getPath()));
            }
            else
            {
               display_.showErrorMessage("Deployment In Progress", 
                     "Another deployment is currently in progress; only one " + 
                     "deployment can be performed at a time.");
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Deploying Application", 
                  "Could not deploy application '" + 
                  event.getRecord().getName() + 
                  "': " + error.getMessage());
         }
      });
   }

   @Override
   public void onShinyAppsDeploymentCompleted(
         ShinyAppsDeploymentCompletedEvent event)
   {
      if (launchBrowser_ && event.succeeded())
      {
         display_.openWindow(event.getUrl());
      }
   }

   @Handler
   public void onShinyAppsManageAccounts()
   {
      ShinyAppsAccountManagerDialog dialog = 
            new ShinyAppsAccountManagerDialog(server_, display_);
      dialog.showModal();
   }
   
   // Terminate, step 1: create a list of apps deployed from this directory
   private void terminateShinyApp(final String dir)
   {
      server_.getShinyAppsDeployments(dir, 
            new ServerRequestCallback<JsArray<ShinyAppsDeploymentRecord>>()
      {
         @Override
         public void onResponseReceived(
               JsArray<ShinyAppsDeploymentRecord> records)
         {
            terminateShinyApp(dir, records);
         }
         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Terminating Application",
                  "Could not determine application deployments for '" +
                   dir + "':" + error.getMessage());
         }
      });
   }
   
   // Terminate, step 2: Get the status of the applications from the server
   private void terminateShinyApp(final String dir, 
         JsArray<ShinyAppsDeploymentRecord> records)
   {
      if (records.length() == 0)
      {
         display_.showMessage(GlobalDisplay.MSG_INFO, "No Deployments Found", 
               "No application deployments were found for '" + dir + "'");
         return;
      }
      
      // If we know the most recent deployment of the directory, act on that
      // deployment by default
      final ArrayList<ShinyAppsDeploymentRecord> recordList = 
            new ArrayList<ShinyAppsDeploymentRecord>();
      ShinyAppsDeploymentRecord lastRecord = dirState_.getLastDeployment(dir);
      if (lastRecord != null)
      {
         recordList.add(lastRecord);
      }
      for (int i = 0; i < records.length(); i++)
      {
         ShinyAppsDeploymentRecord record = records.get(i);
         if (lastRecord == null)
         {
            recordList.add(record);
         }
         else
         {
            if (record.getUrl().equals(lastRecord.getUrl()))
               recordList.set(0, record);
         }
      }
      
      // We need to further filter the list by deployments that are 
      // eligible for termination (i.e. are currently running)
      server_.getShinyAppsAppList(recordList.get(0).getAccount(),
            new ServerRequestCallback<JsArray<ShinyAppsApplicationInfo>>()
      {
         @Override
         public void onResponseReceived(JsArray<ShinyAppsApplicationInfo> apps)
         {
            terminateShinyApp(dir, apps, recordList);
         }
         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Listing Applications",
                  error.getMessage());
         }
      });
   }
   
   // Terminate, step 3: compare the deployments and apps active on the server
   // until we find a running app from the current directory
   private void terminateShinyApp(String dir, 
         JsArray<ShinyAppsApplicationInfo> apps, 
         List<ShinyAppsDeploymentRecord> records)
   {
      for (int i = 0; i < records.size(); i++)
      {
         for (int j = 0; j < apps.length(); j++)
         {
            ShinyAppsApplicationInfo candidate = apps.get(j);
            if (candidate.getName().equals(records.get(i).getName()) &&
                candidate.getStatus().equals("running"))
            {
               terminateShinyApp(records.get(i).getAccount(), candidate);
               return;
            }
         }
      }
      display_.showMessage(GlobalDisplay.MSG_INFO, 
            "No Running Deployments Found", "No applications deployed from '" +
             dir + "' appear to be running.");
   }
   
   // Terminate, step 4: confirm that we've selected the right app for
   // termination
   private void terminateShinyApp(final String accountName, 
                                  final ShinyAppsApplicationInfo target)
   {
      display_.showYesNoMessage(GlobalDisplay.MSG_QUESTION, 
            "Confirm Terminate Application", 
            "Terminate the application '" + target.getName() + "' running " +
            "at " + target.getUrl() + "?", 
            new Operation() {
               @Override
               public void execute()
               {
                  terminateShinyApp(accountName, target.getName());
               }
            }, 
            true
      );
   }
   
   // Terminate, step 5: perform the termination 
   private void terminateShinyApp(String accountName, final String appName)
   {
      events_.fireEvent(new SendToConsoleEvent("shinyapps::terminateApp(\"" +
            appName + "\", \"" + accountName + "\")", true));
   }
   
   private final Commands commands_;
   private final GlobalDisplay display_;
   private final Session session_;
   private final ShinyAppsServerOperations server_;
   private final EventBus events_;
   private boolean launchBrowser_ = false;
   
   private ShinyAppsDirectoryState dirState_;
   private boolean dirStateDirty_ = false;
}
