/*
 * RSConnect.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.ModalDialogTracker;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.rpubs.ui.RPubsUploadDialog;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeployInitiatedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentStartedEvent;
import org.rstudio.studio.client.rsconnect.model.RSConnectApplicationInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectDirectoryState;
import org.rstudio.studio.client.rsconnect.model.RSConnectLintResults;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.model.RmdPublishDetails;
import org.rstudio.studio.client.rsconnect.ui.PublishStaticDialog;
import org.rstudio.studio.client.rsconnect.ui.RSAccountConnector;
import org.rstudio.studio.client.rsconnect.ui.RSConnectDeployDialog;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishWizard;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.events.RSConnectDeploymentCompletedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
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
                    RSConnectServerOperations server,
                    SourceServerOperations sourceServer,
                    RSAccountConnector connector,
                    Provider<UIPrefs> pUiPrefs)
                    
   {
      commands_ = commands;
      display_ = display;
      dependencyManager_ = dependencyManager;
      session_ = session;
      server_ = server;
      sourceServer_ = sourceServer;
      events_ = events;
      satellite_ = satellite;
      connector_ = connector;
      pUiPrefs_ = pUiPrefs;

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
      // see if we have the requisite R packages
      dependencyManager_.withRSConnect(
         "Publishing content", null, new Command() {

            @Override
            public void execute()
            {
               handleRSConnectAction(event); 
            }
         });  
   }
   
   private void publishAsRPubs(RSConnectActionEvent event)
   {
      String ctx = "Publish " + contentTypeDesc(event.getContentType());
      RPubsUploadDialog dlg = new RPubsUploadDialog(
            ctx, ctx, event.getHtmlFile(), event.getFromPrevious() != null);
      dlg.showModal();
   }
   
   private void publishAsStatic(RSConnectActionEvent event,
                                RSConnectPublishInput input)
   {
      PublishStaticDialog dialog = new PublishStaticDialog(server_, 
            display_, event.getFromPrevious());
      dialog.showModal();
   }
   
   private void showPublishUI(final RSConnectActionEvent event)
   {
      if (event.getFromPrevious() != null)
      {
         if (event.getContentType() == CONTENT_TYPE_APP)
         {
            publishAsCode(event);
         }
         // TODO: handle redeployments of R Markdown files
      }
      else 
      {
         final RSConnectPublishInput input = new RSConnectPublishInput();
         input.setContentType(event.getContentType());

         // set these inside the wizard input so we don't need to pass around
         // session/prefs
         input.setConnectUIEnabled(
               pUiPrefs_.get().enableRStudioConnect().getGlobalValue());
         input.setExternalUIEnabled(
               session_.getSessionInfo().getAllowExternalPublish());
         
         // if R Markdown, get info on what we're publishing from the server
         if (event.getFromRmdPreview() != null)
         {
            input.setSourceRmd(FileSystemItem.createFile(
                  event.getFromRmdPreview().getTargetFile()));
            server_.getRmdPublishDetails(
                  event.getFromRmdPreview().getTargetFile(), 
                  new ServerRequestCallback<RmdPublishDetails>()
                  {
                     @Override
                     public void onResponseReceived(RmdPublishDetails details)
                     {
                        input.setIsMultiRmd(details.isMultiRmd());
                        input.setIsShiny(details.isShinyRmd());
                        showPublishUI(event, input);
                     }
                     @Override
                     public void onError(ServerError error)
                     {
                        // this is unlikely since the RPC does little work, but
                        // we can't offer the right choices in the wizard if we
                        // don't know what we're working with.
                        display_.showErrorMessage("Could Not Publish", 
                              error.getMessage());
                     }
                  });
         }
         else
         {
            showPublishUI(event, input);
         }
      }
   }
   
   private void showPublishUI(RSConnectActionEvent event, 
                              RSConnectPublishInput input)
   {
      if (input.getContentType() == CONTENT_TYPE_PLOT)
      {
         if (!input.isConnectUIEnabled() && input.isExternalUIEnabled())
         {
            publishAsRPubs(event);
         }
         else if (input.isConnectUIEnabled() && input.isExternalUIEnabled())
         {
            publishWithWizard(event, input);
         }
         else if (input.isConnectUIEnabled() && !input.isExternalUIEnabled())
         {
            publishAsStatic(event, input);
         }
      }
      else if (input.getContentType() == CONTENT_TYPE_RMD)
      {
         if (input.isShiny())
         {
            if (input.isMultiRmd())
            {
               // multiple Shiny doc
               publishWithWizard(event, input);
            }
            else
            {
               // single Shiny doc
               publishAsCode(event);
            }
         }
         else
         {
            if (input.isConnectUIEnabled())
            {
               publishWithWizard(event, input);
            }
            else
            {
               // RStudio Connect is disabled, go straight to RPubs
               publishAsRPubs(event);
            }
         }
      }
      else if (input.getContentType() == CONTENT_TYPE_APP)
      {
         publishAsCode(event);
      }
   }
   
   private void publishAsCode(RSConnectActionEvent event)
   {
      // TODO: thread ignored files through properly
      RSConnectDeployDialog dialog = 
            new RSConnectDeployDialog(
                      server_, this, display_, 
                      FilePathUtils.dirFromFile(event.getPath()), 
                      event.getPath(),
                      new String[]{},
                      event.getFromPrevious());
      dialog.showModal();
   }
   
   private void publishWithWizard(RSConnectActionEvent event,
                                  RSConnectPublishInput input)
   {
      RSConnectPublishWizard wizard = 
            new RSConnectPublishWizard(input, 
                  new ProgressOperationWithInput<RSConnectPublishResult>()
            {
               @Override
               public void execute(RSConnectPublishResult result, 
                     ProgressIndicator indicator)
               {
                  fireRSConnectPublishEvent(result, true);
                  indicator.onCompleted();
               }
            });
      wizard.showModal();
   }
   
   private void handleRSConnectAction(RSConnectActionEvent event)
   {
      if (event.getAction() == RSConnectActionEvent.ACTION_TYPE_DEPLOY)
      {
         // ignore this request if there's already a modal up 
         if (ModalDialogTracker.numModalsShowing() > 0)
            return;
         
         // show publish UI appropriate to the type of content being deployed
         showPublishUI(event);

         /*
         sourceServer_.getDocumentProperties(event.getPath(), 
               new ServerRequestCallback<JsObject>()
         {
            @Override
            public void onResponseReceived(JsObject properties)
            {
               String files = properties.getString(IGNORED_RESOURCES);
               if (files != null && !files.isEmpty())
               {
                  files.split("\\|");
               }
            }
         });
         */;

      }
      else if (event.getAction() == RSConnectActionEvent.ACTION_TYPE_CONFIGURE)
      {
         configureShinyApp(FilePathUtils.dirFromFile(event.getPath()));
      }
   }
   
   @Override
   public void onRSConnectDeployInitiated(
         final RSConnectDeployInitiatedEvent event)
   {
      // get lint results for the file or directory being deployed, as appropriate
      String deployTarget = event.getPath();
      if (StringUtil.getExtension(event.getSourceFile()).toLowerCase().equals("rmd")) 
      {
         FileSystemItem sourceFSI = FileSystemItem.createDir(deployTarget);
         deployTarget = sourceFSI.completePath(event.getSourceFile());
      }

      server_.getLintResults(deployTarget, 
            new ServerRequestCallback<RSConnectLintResults>()
      {
         @Override
         public void onResponseReceived(RSConnectLintResults results)
         {
            if (results.getErrorMessage().length() > 0)
            {
               display_.showYesNoMessage(GlobalDisplay.MSG_QUESTION, 
                     "Lint Failed", 
                     "The content you tried to publish could not be checked " +
                     "for errors. Do you want to proceed? \n\n" +
                     results.getErrorMessage(), false, 
                     new ProgressOperation() 
                     {
                        @Override
                        public void execute(ProgressIndicator indicator)
                        {
                           // "Publish Anyway"
                           doDeployment(event);
                           indicator.onCompleted();
                        }
                     }, 
                     new ProgressOperation() 
                     {
                        @Override
                        public void execute(ProgressIndicator indicator)
                        {
                           // "Cancel"
                           indicator.onCompleted();
                        }
                     },
                     "Publish Anyway", "Cancel", false);
            }
            else if (results.hasLint())
            {
               display_.showYesNoMessage(GlobalDisplay.MSG_QUESTION, 
                     "Publish Content Issues Found", 
                     "Some issues were found in your content, which may " +
                     "prevent it from working correctly after publishing. " +
                     "Do you want to review these issues or publish anyway? "
                     , false, 
                     new ProgressOperation()
                     {
                        @Override
                        public void execute(ProgressIndicator indicator)
                        {
                           // "Review Issues" -- we automatically show the
                           // markers so they're already behind the dialog.
                           indicator.onCompleted();
                        }
                     }, 
                     new ProgressOperation() {
                        @Override
                        public void execute(ProgressIndicator indicator)
                        {
                           // "Publish Anyway"
                           doDeployment(event);
                           indicator.onCompleted();
                        }
                     }, 
                     "Review Issues", "Publish Anyway", true);
            }
            else
            {
               // no lint and no errors -- good to go for deployment
               doDeployment(event);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            // we failed to lint, which is not encouraging, but we don't want to
            // fail the whole deployment lest a balky linter prevent people from
            // getting their work published, so forge on ahead.
            doDeployment(event);
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

   public void ensureSessionInit()
   {
      if (sessionInited_)
         return;
      
      // "Manage accounts" can be invoked any time we're permitted to
      // publish 
      commands_.rsconnectManageAccounts().setVisible(
            SessionUtils.showPublishUi(session_, pUiPrefs_.get()));
      
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
         JsArrayString deployFiles,
         JsArrayString additionalFiles,
         JsArrayString ignoredFiles,
         String file, 
         boolean launch, 
         JavaScriptObject record) /*-{
      $wnd.opener.deployToRSConnect(path, deployFiles, additionalFiles, ignoredFiles, file, launch, record);
   }-*/;
   
   public static String contentTypeDesc(int contentType)
   {
      switch(contentType)
      {
      case RSConnect.CONTENT_TYPE_APP:
         return "Application";
      case RSConnect.CONTENT_TYPE_PLOT:
         return "Plot";
      case RSConnect.CONTENT_TYPE_RMD:
         return "Document";
      }
      return "Content";
   }
 
   public void fireRSConnectPublishEvent(RSConnectPublishResult result,
         boolean launchBrowser)
   {
      if (satellite_.isCurrentWindowSatellite())
      {
         // in a satellite window, call back to the main window to do a 
         // deployment
         RSConnect.deployFromSatellite(
               result.getSourceDir(), 
               JsArrayUtil.toJsArrayString(result.getDeployFiles()),
               JsArrayUtil.toJsArrayString(result.getAdditionalFiles()),
               JsArrayUtil.toJsArrayString(result.getIgnoredFiles()),
               result.getSourceFile(), 
               launchBrowser, 
               RSConnectDeploymentRecord.create(result.getAppName(), 
                     result.getAccount(), ""));

         // we can't raise the main window if we aren't in desktop mode, so show
         // a dialog to guide the user there
         if (!Desktop.isDesktop())
         {
            display_.showMessage(GlobalDisplay.MSG_INFO, "Deployment Started",
                  "RStudio is deploying " + result.getAppName() + ". " + 
                  "Check the Deploy console tab in the main window for " + 
                  "status updates. ");
         }
      }
      else
      {
         // in the main window, initiate the deployment directly
         events_.fireEvent(new RSConnectDeployInitiatedEvent(
               result.getSourceDir(),
               result.getDeployFiles(),
               result.getAdditionalFiles(),
               result.getIgnoredFiles(),
               result.getSourceFile(),
               launchBrowser,
               RSConnectDeploymentRecord.create(result.getAppName(), 
                     result.getAccount(), "")));
      }
   }

   // Private methods ---------------------------------------------------------
   
   private void doDeployment(final RSConnectDeployInitiatedEvent event)
   {
      server_.deployShinyApp(event.getPath(), 
                             event.getDeployFiles(),
                             event.getSourceFile(),
                             event.getRecord().getAccountName(), 
                             event.getRecord().getServer(),
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
   // Manage, step 1: create a list of apps deployed from this directory
   private void configureShinyApp(final String dir)
   {
      server_.getRSConnectDeployments(dir, 
            new ServerRequestCallback<JsArray<RSConnectDeploymentRecord>>()
      {
         @Override
         public void onResponseReceived(
               JsArray<RSConnectDeploymentRecord> records)
         {
            configureShinyApp(dir, records);
         }
         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Configuring Application",
                  "Could not determine application deployments for '" +
                   dir + "':" + error.getMessage());
         }
      });
   }
   
   // Manage, step 2: Get the status of the applications from the server
   private void configureShinyApp(final String dir, 
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
      server_.getRSConnectAppList(recordList.get(0).getAccountName(),
            recordList.get(0).getServer(),
            new ServerRequestCallback<JsArray<RSConnectApplicationInfo>>()
      {
         @Override
         public void onResponseReceived(JsArray<RSConnectApplicationInfo> apps)
         {
            configureShinyApp(dir, apps, recordList);
         }
         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error Listing Applications",
                  error.getMessage());
         }
      });
   }
   
   // Manage, step 3: compare the deployments and apps active on the server
   // until we find a running app from the current directory
   private void configureShinyApp(String dir, 
         JsArray<RSConnectApplicationInfo> apps, 
         List<RSConnectDeploymentRecord> records)
   {
      for (int i = 0; i < records.size(); i++)
      {
         for (int j = 0; j < apps.length(); j++)
         {
            RSConnectApplicationInfo candidate = apps.get(j);
            if (candidate.getName().equals(records.get(i).getName()))
            {
               // show the management ui
               display_.openWindow(candidate.getConfigUrl());
               return;
            }
         }
      }
      display_.showMessage(GlobalDisplay.MSG_INFO, 
            "No Running Deployments Found", "No applications deployed from '" +
             dir + "' appear to be running.");
   }
   
   private final native void exportNativeCallbacks() /*-{
      var thiz = this;     
      $wnd.deployToRSConnect = $entry(
         function(path, deployFiles, additionalFiles, ignoredFiles, file, launch, record) {
            thiz.@org.rstudio.studio.client.rsconnect.RSConnect::deployToRSConnect(Ljava/lang/String;Lcom/google/gwt/core/client/JsArrayString;Lcom/google/gwt/core/client/JsArrayString;Lcom/google/gwt/core/client/JsArrayString;Ljava/lang/String;ZLcom/google/gwt/core/client/JavaScriptObject;)(path, deployFiles, additionalFiles, ignoredFiles, file, launch, record);
         }
      ); 
   }-*/;
   
   private void deployToRSConnect(String path, JsArrayString deployFiles, 
                                  JsArrayString additionalFiles, 
                                  JsArrayString ignoredFiles, 
                                  String file, boolean launch, 
                                  JavaScriptObject jsoRecord)
   {
      // this can be invoked by a satellite, so bring the main frame to the
      // front if we can
      if (Desktop.isDesktop())
         Desktop.getFrame().bringMainFrameToFront();
      else
         WindowEx.get().focus();
      
      ArrayList<String> deployFilesList = 
            JsArrayUtil.fromJsArrayString(deployFiles);
      ArrayList<String> additionalFilesList = 
            JsArrayUtil.fromJsArrayString(additionalFiles);
      ArrayList<String> ignoredFilesList = 
            JsArrayUtil.fromJsArrayString(ignoredFiles);
      
      RSConnectDeploymentRecord record = jsoRecord.cast();
      events_.fireEvent(new RSConnectDeployInitiatedEvent(
            path, deployFilesList, additionalFilesList, ignoredFilesList, 
            file, launch, record));
   }
   
   private final Commands commands_;
   private final GlobalDisplay display_;
   private final Session session_;
   private final RSConnectServerOperations server_;
   private final SourceServerOperations sourceServer_;
   private final DependencyManager dependencyManager_;
   private final EventBus events_;
   private final Satellite satellite_;
   private final RSAccountConnector connector_;
   private final Provider<UIPrefs> pUiPrefs_;
   
   private boolean launchBrowser_ = false;
   private boolean sessionInited_ = false;
   
   private RSConnectDirectoryState dirState_;
   private boolean dirStateDirty_ = false;
   
   public final static String CLOUD_SERVICE_NAME = "ShinyApps.io";
   public static final String IGNORED_RESOURCES = "ignored_resources";
   
   public final static int CONTENT_TYPE_NONE = 0;
   public final static int CONTENT_TYPE_PLOT = 1;
   public final static int CONTENT_TYPE_RMD  = 2;
   public final static int CONTENT_TYPE_APP  = 3;
}
