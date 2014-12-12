/*
 * RSConnect.java
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
package org.rstudio.studio.client.rsconnect;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.ModalDialogTracker;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeployInitiatedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentStartedEvent;
import org.rstudio.studio.client.rsconnect.model.RSConnectApplicationInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectDirectoryState;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.ui.RSConnectAccountManagerDialog;
import org.rstudio.studio.client.rsconnect.ui.RSConnectDeployDialog;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.events.RSConnectDeploymentCompletedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RSConnect implements SessionInitHandler, 
                                  RSConnectActionEvent.Handler,
                                  RSConnectDeployInitiatedEvent.Handler,
                                  RSConnectDeploymentCompletedEvent.Handler
{
   public interface Binder
           extends CommandBinder<Commands, RSConnect> {}

   @Inject
   public RSConnect(EventBus events, 
                    Commands commands, 
                    Session session,
                    Satellite satellite,
                    GlobalDisplay display,
                    DependencyManager dependencyManager,
                    Binder binder, 
                    RSConnectServerOperations server)
                    
   {
      commands_ = commands;
      display_ = display;
      dependencyManager_ = dependencyManager;
      session_ = session;
      server_ = server;
      events_ = events;
      satellite_ = satellite;

      binder.bind(commands, this);

      events.addHandler(SessionInitEvent.TYPE, this);
      events.addHandler(RSConnectActionEvent.TYPE, this); 
      events.addHandler(RSConnectDeployInitiatedEvent.TYPE, this); 
      events.addHandler(RSConnectDeploymentCompletedEvent.TYPE, this); 
      
      exportNativeCallbacks();
   }
   
   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      ensureSessionInit();
   }
   
   @Override
   public void onRSConnectAction(final RSConnectActionEvent event)
   {
      dependencyManager_.withRSConnect(
         "Publishing shiny applications", new Command() {

            @Override
            public void execute()
            {
               handleRSConnectAction(event); 
            }
         });  
   }
   
   private void handleRSConnectAction(RSConnectActionEvent event)
   {
      if (event.getAction() == RSConnectActionEvent.ACTION_TYPE_DEPLOY)
      {
         // ignore this request if there's already a modal up 
         if (ModalDialogTracker.numModalsShowing() > 0)
            return;    

         final String dir = FilePathUtils.dirFromFile(event.getPath());
         RSConnectDeploymentRecord record = dirState_.getLastDeployment(dir);
         final String lastAccount = record == null ? null : record.getAccount();
         final String lastAppName = record == null ? null : record.getName();
         
         // don't consider this to be a deployment of a specific file unless
         // we're deploying R Markdown content
         String file = "";
         if (event.getPath().toLowerCase().endsWith(".rmd"))
         {
            file = FilePathUtils.friendlyFileName(event.getPath());
         }

         RSConnectDeployDialog dialog = 
               new RSConnectDeployDialog(
                         server_, display_, events_, 
                         dir, file, lastAccount, lastAppName,
                         satellite_.isCurrentWindowSatellite());
         dialog.showModal();
      }
      else if (event.getAction() == RSConnectActionEvent.ACTION_TYPE_TERMINATE)
      {
         terminateShinyApp(FilePathUtils.dirFromFile(event.getPath()));
      }
   }
   
   @Override
   public void onRSConnectDeployInitiated(
         final RSConnectDeployInitiatedEvent event)
   {
      server_.deployShinyApp(event.getPath(), 
                             event.getSourceFile(),
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
               events_.fireEvent(new RSConnectDeploymentStartedEvent(
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
   public void onRSConnectDeploymentCompleted(
         RSConnectDeploymentCompletedEvent event)
   {
      if (launchBrowser_ && event.succeeded())
      {
         display_.openWindow(event.getUrl());
      }
   }

   @Handler
   public void onRsconnectManageAccounts()
   {
      dependencyManager_.withRSConnect(
         "Publishing shiny applications", new Command() {
            @Override
            public void execute()
            {
               RSConnectAccountManagerDialog dialog = 
                     new RSConnectAccountManagerDialog(server_, display_);
               dialog.showModal();
            }
         });
   }
   
   public void ensureSessionInit()
   {
      if (sessionInited_)
         return;
      
      // "Manage accounts" can be invoked any time the package is available
      commands_.rsconnectManageAccounts().setVisible(
            session_.getSessionInfo().getRSConnectAvailable());
      
      // This object keeps track of the most recent deployment we made of each
      // directory, and is used to default directory deployments to last-used
      // settings.
      new JSObjectStateValue(
            "rsconnect",
            "rsconnectDirectories",
            ClientState.PERSISTENT,
            session_.getSessionInfo().getClientState(),
            false)
       {
          @Override
          protected void onInit(JsObject value)
          {
             dirState_ = (RSConnectDirectoryState) (value == null ?
                   RSConnectDirectoryState.create() :
                   value.cast());
          }
   
          @Override
          protected JsObject getValue()
          {
             dirStateDirty_ = false;
             return (JsObject) (dirState_ == null ?
                   RSConnectDirectoryState.create().cast() :
                   dirState_.cast());
          }
   
          @Override
          protected boolean hasChanged()
          {
             return dirStateDirty_;
          }
       };
       
       sessionInited_ = true;
   }
   
   public static native void deployFromSatellite(
         String path,
         String file, 
         boolean launch, 
         JavaScriptObject record) /*-{
      $wnd.opener.deployToRSConnect(path, file, launch, record);
   }-*/;
   
   // Private methods ---------------------------------------------------------
   
   // Terminate, step 1: create a list of apps deployed from this directory
   private void terminateShinyApp(final String dir)
   {
      server_.getRSConnectDeployments(dir, 
            new ServerRequestCallback<JsArray<RSConnectDeploymentRecord>>()
      {
         @Override
         public void onResponseReceived(
               JsArray<RSConnectDeploymentRecord> records)
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
         JsArray<RSConnectDeploymentRecord> records)
   {
      if (records.length() == 0)
      {
         display_.showMessage(GlobalDisplay.MSG_INFO, "No Deployments Found", 
               "No application deployments were found for '" + dir + "'");
         return;
      }
      
      // If we know the most recent deployment of the directory, act on that
      // deployment by default
      final ArrayList<RSConnectDeploymentRecord> recordList = 
            new ArrayList<RSConnectDeploymentRecord>();
      RSConnectDeploymentRecord lastRecord = dirState_.getLastDeployment(dir);
      if (lastRecord != null)
      {
         recordList.add(lastRecord);
      }
      for (int i = 0; i < records.length(); i++)
      {
         RSConnectDeploymentRecord record = records.get(i);
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
      server_.getRSConnectAppList(recordList.get(0).getAccount(),
            new ServerRequestCallback<JsArray<RSConnectApplicationInfo>>()
      {
         @Override
         public void onResponseReceived(JsArray<RSConnectApplicationInfo> apps)
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
         JsArray<RSConnectApplicationInfo> apps, 
         List<RSConnectDeploymentRecord> records)
   {
      for (int i = 0; i < records.size(); i++)
      {
         for (int j = 0; j < apps.length(); j++)
         {
            RSConnectApplicationInfo candidate = apps.get(j);
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
                                  final RSConnectApplicationInfo target)
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
      events_.fireEvent(new SendToConsoleEvent("rsconnect::terminateApp(\"" +
            appName + "\", \"" + accountName + "\")", true));
   }
   
   private final native void exportNativeCallbacks() /*-{
      var thiz = this;     
      $wnd.deployToRSConnect = $entry(
         function(path, file, launch, record) {
            thiz.@org.rstudio.studio.client.rsconnect.RSConnect::deployToRSConnect(Ljava/lang/String;Ljava/lang/String;ZLcom/google/gwt/core/client/JavaScriptObject;)(path, file, launch, record);
         }
      ); 
   }-*/;
   
   private void deployToRSConnect(String path, String file, boolean launch, 
                                  JavaScriptObject jsoRecord)
   {
      // this can be invoked by a satellite, so bring the main frame to the
      // front if we can
      if (Desktop.isDesktop())
         Desktop.getFrame().bringMainFrameToFront();
      else
         WindowEx.get().focus();
      
      RSConnectDeploymentRecord record = jsoRecord.cast();
      events_.fireEvent(new RSConnectDeployInitiatedEvent(
            path, file, launch, record));
   }
   
   private final Commands commands_;
   private final GlobalDisplay display_;
   private final Session session_;
   private final RSConnectServerOperations server_;
   private final DependencyManager dependencyManager_;
   private final EventBus events_;
   private final Satellite satellite_;
   private boolean launchBrowser_ = false;
   private boolean sessionInited_ = false;
   
   private RSConnectDirectoryState dirState_;
   private boolean dirStateDirty_ = false;
}
