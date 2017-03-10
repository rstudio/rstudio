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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ModalDialogTracker;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.images.MessageDialogImages;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.rpubs.RPubsUploader;
import org.rstudio.studio.client.common.rpubs.model.RPubsServerOperations;
import org.rstudio.studio.client.common.rpubs.ui.RPubsUploadDialog;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeployInitiatedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentCompletedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentFailedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentStartedEvent;
import org.rstudio.studio.client.rsconnect.model.PlotPublishMRUList;
import org.rstudio.studio.client.rsconnect.model.RSConnectApplicationInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectDirectoryState;
import org.rstudio.studio.client.rsconnect.model.RSConnectLintResults;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSettings;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSource;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.model.RmdPublishDetails;
import org.rstudio.studio.client.rsconnect.ui.RSAccountConnector;
import org.rstudio.studio.client.rsconnect.ui.RSConnectDeployDialog;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishWizard;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RSConnect implements SessionInitHandler, 
                                  RSConnectActionEvent.Handler,
                                  RSConnectDeployInitiatedEvent.Handler,
                                  RSConnectDeploymentCompletedEvent.Handler,
                                  RSConnectDeploymentFailedEvent.Handler
{
   public interface Binder
           extends CommandBinder<Commands, RSConnect> {}

   @Inject
   public RSConnect(EventBus events, 
                    Commands commands, 
                    Session session,
                    GlobalDisplay display,
                    DependencyManager dependencyManager,
                    Binder binder, 
                    RSConnectServerOperations server,
                    SourceServerOperations sourceServer,
                    RPubsServerOperations rpubsServer,
                    RSAccountConnector connector,
                    Provider<UIPrefs> pUiPrefs,
                    PlotPublishMRUList plotMru)
   {
      commands_ = commands;
      display_ = display;
      dependencyManager_ = dependencyManager;
      session_ = session;
      server_ = server;
      sourceServer_ = sourceServer;
      rpubsServer_ = rpubsServer;
      events_ = events;
      connector_ = connector;
      pUiPrefs_ = pUiPrefs;
      plotMru_ = plotMru;

      binder.bind(commands, this);

      events.addHandler(SessionInitEvent.TYPE, this);
      events.addHandler(RSConnectActionEvent.TYPE, this); 
      events.addHandler(RSConnectDeployInitiatedEvent.TYPE, this); 
      events.addHandler(RSConnectDeploymentCompletedEvent.TYPE, this); 
      events.addHandler(RSConnectDeploymentFailedEvent.TYPE, this);
      
      // satellite windows don't get session init events, so initialize the
      // session here
      if (Satellite.isCurrentWindowSatellite())
      {
         ensureSessionInit();
      }
      
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
      // ignore if we're already waiting for a dependency check 
      if (depsPending_)
         return;
      
      // see if we have the requisite R packages
      depsPending_ = true; 
      dependencyManager_.withRSConnect(
         "Publishing content", 
         event.getContentType() == CONTENT_TYPE_DOCUMENT ||
         event.getContentType() == CONTENT_TYPE_WEBSITE,
         null, new CommandWithArg<Boolean>() {
            @Override
            public void execute(Boolean succeeded)
            {
               if (succeeded)
                  handleRSConnectAction(event); 
               
               depsPending_ = false;
            }
         });  
   }
   
   private void publishAsRPubs(RSConnectActionEvent event)
   {
      String ctx = "Publish " + contentTypeDesc(event.getContentType());
      RPubsUploadDialog dlg = new RPubsUploadDialog(
            "Publish Wizard", 
            ctx, 
            event.getFromPreview() != null ? 
                  event.getFromPreview().getSourceFile() : null,
            event.getHtmlFile(), 
            event.getFromPrevious() == null ? 
                  "" : event.getFromPrevious().getBundleId(),
            false);
      dlg.showModal();
   }
   
   private void showPublishUI(final RSConnectActionEvent event)
   {
      final RSConnectPublishInput input = new RSConnectPublishInput(event);

      // set these inside the wizard input so we don't need to pass around
      // session/prefs
      input.setConnectUIEnabled(
            pUiPrefs_.get().enableRStudioConnect().getGlobalValue());
      input.setExternalUIEnabled(
            session_.getSessionInfo().getAllowExternalPublish());
      input.setDescription(event.getDescription());
      
      if (event.getFromPrevious() != null)
      {
         switch (event.getContentType())
         {
         case CONTENT_TYPE_APP:
         case CONTENT_TYPE_APP_SINGLE:
            publishAsCode(event, null, true);
            break;
         case CONTENT_TYPE_PRES:
         case CONTENT_TYPE_PLOT:
         case CONTENT_TYPE_HTML:
         case CONTENT_TYPE_DOCUMENT:
         case CONTENT_TYPE_WEBSITE:
            if (event.getFromPrevious().getServer().equals("rpubs.com"))
            {
               publishAsRPubs(event);
            }
            else 
            {
               fillInputFromDoc(input, event.getPath(), 
                     new CommandWithArg<RSConnectPublishInput>()
               {
                  @Override
                  public void execute(RSConnectPublishInput arg)
                  {
                     if (arg == null)
                        return;
                     
                     if (event.getFromPrevious().getAsStatic())
                        publishAsFiles(event, 
                              new RSConnectPublishSource(event.getPath(), 
                                    event.getHtmlFile(), 
                                    arg.getWebsiteDir(),
                                    arg.isSelfContained(), 
                                    true,
                                    arg.isShiny(),
                                    arg.getDescription(),
                                    event.getContentType()));
                     else
                        publishAsCode(event, arg.getWebsiteDir(), 
                              arg.isShiny());
                  }
               });
            }
            break;
         }
      }
      else 
      {
         // plots and HTML are implicitly self-contained
         if (event.getContentType() == CONTENT_TYPE_PLOT ||
             event.getContentType() == CONTENT_TYPE_HTML ||
             event.getContentType() == CONTENT_TYPE_PRES)
         {
            input.setIsSelfContained(true);
         }
         
         // if R Markdown, get info on what we're publishing from the server
         if (event.getFromPreview() != null)
         {
            input.setSourceRmd(FileSystemItem.createFile(
                  event.getFromPreview().getSourceFile()));
            fillInputFromDoc(input, event.getFromPreview().getSourceFile(), 
                  new CommandWithArg<RSConnectPublishInput>()
            {
               @Override
               public void execute(RSConnectPublishInput arg)
               {
                  showPublishUI(arg);
               }
            });
         }
         else
         {
            showPublishUI(input);
         }
      }
   }
   
   private void showPublishUI(RSConnectPublishInput input)
   {
      final RSConnectActionEvent event = input.getOriginatingEvent();
      if (input.getContentType() == CONTENT_TYPE_PLOT ||
          input.getContentType() == CONTENT_TYPE_HTML ||
          input.getContentType() == CONTENT_TYPE_PRES)
      {
         if (!input.isConnectUIEnabled() && input.isExternalUIEnabled())
         {
            publishAsRPubs(event);
         }
         else if (input.isConnectUIEnabled() && input.isExternalUIEnabled())
         {
            publishWithWizard(input);
         }
         else if (input.isConnectUIEnabled() && !input.isExternalUIEnabled())
         {
            publishAsStatic(input);
         }
      }
      else if (input.getContentType() == CONTENT_TYPE_WEBSITE)
      {
         if (input.hasDocOutput())
         {
            publishWithWizard(input);
         }
         else
         {
            publishAsCode(event, input.getWebsiteDir(), false);
         }
      }
      else if (input.getContentType() == CONTENT_TYPE_DOCUMENT)
      {
         if (input.isShiny())
         {
            if (input.isMultiRmd())
            {
               // multiple Shiny doc
               publishWithWizard(input);
            }
            else
            {
               // single Shiny doc
               publishAsCode(event, input.getWebsiteDir(), true);
            }
         }
         else
         {
            if (input.isConnectUIEnabled())
            {
               if (input.hasDocOutput() || 
                   (input.isMultiRmd() && !input.isWebsiteRmd()))
               {
                  // need to disambiguate between code/output and/or
                  // single/multi page
                  publishWithWizard(input);
               }
               else
               {
                  // we don't have output, always publish the code
                  publishAsCode(event, input.getWebsiteDir(), false);
               }
            }
            else if (input.isSelfContained() && input.hasDocOutput())
            {
               // RStudio Connect is disabled, go straight to RPubs
               publishAsRPubs(event);
            }
            else 
            {
               // we should generally hide the button in this case
               display_.showErrorMessage("Content Not Publishable", 
                     "Only self-contained documents can currently be " + 
                     "published to RPubs.");
            }
         }
      }
      else if (input.getContentType() == CONTENT_TYPE_APP ||
               input.getContentType() == CONTENT_TYPE_APP_SINGLE)
      {
         publishAsCode(event, null, true);
      }
   }
   
   private void publishAsCode(RSConnectActionEvent event, String websiteDir,
         boolean isShiny)
   {
      RSConnectPublishSource source = null;
      if (event.getContentType() == CONTENT_TYPE_APP ||
          event.getContentType() == CONTENT_TYPE_APP_SINGLE)
      {
         if (StringUtil.getExtension(event.getPath()).equalsIgnoreCase("r"))
         {
            FileSystemItem rFile = FileSystemItem.createFile(event.getPath());
            // use the directory for the deployment record when publishing 
            // directory-based apps; use the file itself when publishing 
            // single-file apps
            source = new RSConnectPublishSource(rFile.getParentPathString(),
                  event.getContentType() == CONTENT_TYPE_APP ? 
                        rFile.getParentPathString() :
                        rFile.getName());
         }
         else
         {
            source = new RSConnectPublishSource(event.getPath(),
                  event.getPath());
         }
      }
      else
      {
         source = new RSConnectPublishSource(event.getPath(), websiteDir,
            false, false, isShiny, null, event.getContentType());
      }
         
      publishAsFiles(event, source);
   }
   
   private void publishAsStatic(RSConnectPublishInput input)
   {
      RSConnectPublishSource source = null;
      if (input.getContentType() == RSConnect.CONTENT_TYPE_DOCUMENT ||
          input.getContentType() == RSConnect.CONTENT_TYPE_WEBSITE)
      {
         source = new RSConnectPublishSource(
                     input.getOriginatingEvent().getFromPreview(),
                     input.getWebsiteDir(),
                     input.isSelfContained(),
                     true, 
                     input.isShiny(),
                     input.getDescription());
      }
      else
      {
         source = new RSConnectPublishSource(
               input.getOriginatingEvent().getHtmlFile(),
               input.getWebsiteDir(),
               input.isSelfContained(), 
               true,
               input.isShiny(),
               input.getDescription(),
               input.getContentType());
      }
      publishAsFiles(input.getOriginatingEvent(), source);
   }

   private void publishAsFiles(RSConnectActionEvent event,
         RSConnectPublishSource source)
   {
      RSConnectDeployDialog dialog = 
            new RSConnectDeployDialog(
                      event.getContentType(),
                      server_, this, display_, 
                      source,
                      event.getFromPrevious());
      dialog.showModal();
   }
   
   private void publishWithWizard(final RSConnectPublishInput input)
   {
      RSConnectPublishWizard wizard = 
            new RSConnectPublishWizard(input, 
                  new ProgressOperationWithInput<RSConnectPublishResult>()
            {
               @Override
               public void execute(RSConnectPublishResult result, 
                     ProgressIndicator indicator)
               {
                  switch (result.getPublishType())
                  {
                  case RSConnectPublishResult.PUBLISH_STATIC:
                  case RSConnectPublishResult.PUBLISH_CODE:
                     // always launch the browser--the wizard implies we're 
                     // doing a first-time publish, and we may need to do some
                     // post-publish configuration
                     fireRSConnectPublishEvent(result, true);
                     indicator.onCompleted();
                     break;
                  case RSConnectPublishResult.PUBLISH_RPUBS:
                     uploadToRPubs(input, result, indicator);
                     break;
                  }
               }
            });
      wizard.showModal();
   }
   
   @Override
   public void onRSConnectDeployInitiated(
         final RSConnectDeployInitiatedEvent event)
   {
      // shortcut: when deploying static content we don't need to do any linting
      if (event.getSettings().getAsStatic())
      {
         doDeployment(event);
         return;
      }

      // get lint results for the file or directory being deployed, as 
      // appropriate
      server_.getLintResults(event.getSource().getDeployKey(),
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

   @Override
   public void onRSConnectDeploymentFailed(
         final RSConnectDeploymentFailedEvent event)
   {
      String failedPath = event.getData().getPath();
      // if this looks like an API call, process the path to get the 'bare'
      // server URL
      int pos = failedPath.indexOf("__api__");
      if (pos < 1)
      {
         // if not, just get the host
         pos = failedPath.indexOf("/", 10) + 1;
      }
      if (pos > 0)
      {
         failedPath = failedPath.substring(0, pos);
      }
      final String serverUrl = failedPath;
      
      new ModalDialogBase()
      {
         @Override
         protected Widget createMainWidget()
         {
            setText("Publish Failed");
            addOkButton(new ThemedButton("OK", new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent arg0)
               {
                  closeDialog();
               }
            }));
            HorizontalPanel panel = new HorizontalPanel();
            Image errorImage = 
                  new Image(new ImageResource2x(MessageDialogImages.INSTANCE.dialog_error2x()));
            errorImage.getElement().getStyle().setMarginTop(1, Unit.EM);
            errorImage.getElement().getStyle().setMarginRight(1, Unit.EM);
            panel.add(errorImage);
            panel.add(new HTML("<p>Your content could not be published because " +
                  "of a problem on the server.</p>" + 
                  "<p>More information may be available on the server's home " +
                  "page:</p>" +
                  "<p><a href=\"" + serverUrl + "\">" + serverUrl + "</a>" +
                  "</p>" + 
                  "<p>If the error persists, contact the server's "  +
                  "administrator.</p>" +
                  "<p><small>Error code: " + event.getData().getHttpStatus() + 
                  "</small></p>"));
            return panel;
         }
      }.showModal();
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
         String sourceFile,
         String deployDir,
         String deployFile, 
         String websiteDir,
         String description,
         JsArrayString deployFiles,
         JsArrayString additionalFiles,
         JsArrayString ignoredFiles,
         boolean isSelfContained,
         boolean isShiny,
         boolean asMultiple,
         boolean asStatic,
         boolean launch, 
         JavaScriptObject record) /*-{
      $wnd.opener.deployToRSConnect(sourceFile, deployDir, deployFile, 
                                    websiteDir, description, deployFiles, 
                                    additionalFiles, ignoredFiles, isSelfContained, 
                                    isShiny, asMultiple, asStatic, launch, 
                                    record);
   }-*/;
   
   
   public static boolean showRSConnectUI()
   {
      return true;
   }
   
   public static String contentTypeDesc(int contentType)
   {
      switch(contentType)
      {
      case RSConnect.CONTENT_TYPE_APP:
      case RSConnect.CONTENT_TYPE_APP_SINGLE:
         return "Application";
      case RSConnect.CONTENT_TYPE_PLOT:
         return "Plot";
      case RSConnect.CONTENT_TYPE_HTML:
         return "HTML";
      case RSConnect.CONTENT_TYPE_DOCUMENT:
         return "Document";
      case RSConnect.CONTENT_TYPE_PRES:
         return "Presentation";
      case RSConnect.CONTENT_TYPE_WEBSITE:
         return "Website";
      }
      return "Content";
   }
 
   public void fireRSConnectPublishEvent(RSConnectPublishResult result,
         boolean launchBrowser)
   {
      if (Satellite.isCurrentWindowSatellite())
      {
         // in a satellite window, call back to the main window to do a 
         // deployment
         RSConnect.deployFromSatellite(
               result.getSource().getSourceFile(), 
               result.getSource().getDeployDir(), 
               result.getSource().getDeployFile(), 
               result.getSource().getWebsiteDir(),
               result.getSource().getDescription(),
               JsArrayUtil.toJsArrayString(
                     result.getSettings().getDeployFiles()),
               JsArrayUtil.toJsArrayString(
                     result.getSettings().getAdditionalFiles()),
               JsArrayUtil.toJsArrayString(
                     result.getSettings().getIgnoredFiles()),
               result.getSource().isSelfContained(),
               result.getSource().isShiny(),
               result.getSettings().getAsMultiple(),
               result.getSettings().getAsStatic(),
               launchBrowser, 
               RSConnectDeploymentRecord.create(result.getAppName(), 
                     result.getAppTitle(), result.getAccount(), ""));

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
               result.getSource(),
               result.getSettings(),
               launchBrowser,
               RSConnectDeploymentRecord.create(result.getAppName(), 
                     result.getAppTitle(), result.getAccount(), "")));
      }
   }
   
   // Private methods ---------------------------------------------------------
   
   private void uploadToRPubs(RSConnectPublishInput input, 
         RSConnectPublishResult result,
         final ProgressIndicator indicator)
   {
      RPubsUploader uploader = new RPubsUploader(rpubsServer_, display_, 
            events_, "rpubs-" + rpubsCount_++);
      String contentType = contentTypeDesc(input.getContentType());
      indicator.onProgress("Uploading " + contentType);
      uploader.setOnUploadComplete(new CommandWithArg<Boolean>()
      {
         @Override
         public void execute(Boolean arg)
         {
            indicator.onCompleted();
         }
      });
      uploader.performUpload(contentType, 
            input.getSourceRmd() == null ? null : 
               input.getSourceRmd().getPath(),
            input.getOriginatingEvent().getHtmlFile(), 
            input.getOriginatingEvent().getFromPrevious() == null ? "" :
               input.getOriginatingEvent().getFromPrevious().getBundleId(),
            false);
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
      }
      else if (event.getAction() == RSConnectActionEvent.ACTION_TYPE_CONFIGURE)
      {
         configureShinyApp(FilePathUtils.dirFromFile(event.getPath()));
      }
   }
   
   private void doDeployment(final RSConnectDeployInitiatedEvent event)
   {
      server_.publishContent(event.getSource(),
                             event.getRecord().getAccountName(), 
                             event.getRecord().getServer(),
                             event.getRecord().getName(), 
                             event.getRecord().getTitle(),
                             event.getSettings(),
      new ServerRequestCallback<Boolean>()
      {
         @Override
         public void onResponseReceived(Boolean status)
         {
            if (status)
            {
               dirState_.addDeployment(event.getSource().getDeployDir(), 
                     event.getRecord());
               dirStateDirty_ = true;
               if (event.getSource().getContentCategory() == 
                     RSConnect.CONTENT_CATEGORY_PLOT)
               {
                  plotMru_.addPlotMruEntry(event.getRecord().getAccountName(),
                        event.getRecord().getServer(),
                        event.getRecord().getName(),
                        event.getRecord().getTitle());
               }
               launchBrowser_ = event.getLaunchBrowser();
               events_.fireEvent(new RSConnectDeploymentStartedEvent(
                     event.getSource().isWebsiteRmd() ? "" :
                       event.getSource().getDeployKey(), 
                     event.getSource().getDescription()));
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
            "",
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
         function(sourceFile, deployDir, deployFile, websiteDir, description, deployFiles, additionalFiles, ignoredFiles, isSelfContained, isShiny, asMultiple, asStatic, launch, record) {
            thiz.@org.rstudio.studio.client.rsconnect.RSConnect::deployToRSConnect(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JsArrayString;Lcom/google/gwt/core/client/JsArrayString;Lcom/google/gwt/core/client/JsArrayString;ZZZZZLcom/google/gwt/core/client/JavaScriptObject;)(sourceFile, deployDir, deployFile, websiteDir, description, deployFiles, additionalFiles, ignoredFiles, isSelfContained, isShiny, asMultiple, asStatic, launch, record);
         }
      ); 
   }-*/;
   
   private void deployToRSConnect(String sourceFile, 
                                  String deployDir, 
                                  String deployFile, 
                                  String websiteDir,
                                  String description,
                                  JsArrayString deployFiles, 
                                  JsArrayString additionalFiles, 
                                  JsArrayString ignoredFiles, 
                                  boolean isSelfContained,
                                  boolean isShiny,
                                  boolean asMultiple, 
                                  boolean asStatic,
                                  boolean launch, 
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
            new RSConnectPublishSource(sourceFile, deployDir, deployFile,
                  websiteDir, isSelfContained, asStatic, isShiny, description),
            new RSConnectPublishSettings(deployFilesList, 
                  additionalFilesList, ignoredFilesList, asMultiple, asStatic), 
            launch, record));
   }
   
   private void fillInputFromDoc(final RSConnectPublishInput input, 
         final String docPath, 
         final CommandWithArg<RSConnectPublishInput> onComplete)
   {
      server_.getRmdPublishDetails(
            docPath, 
            new ServerRequestCallback<RmdPublishDetails>()
            {
               @Override
               public void onResponseReceived(RmdPublishDetails details)
               {
                  input.setIsMultiRmd(details.isMultiRmd());
                  input.setIsShiny(details.isShinyRmd());
                  input.setIsSelfContained(details.isSelfContained());
                  input.setHasConnectAccount(details.hasConnectAccount());
                  input.setWebsiteDir(details.websiteDir());
                  if (StringUtil.isNullOrEmpty(input.getDescription()))
                  {
                     if (details.getTitle() != null && 
                         !details.getTitle().isEmpty())
                     {
                        // set the description from the document title, if we
                        // have it
                        input.setDescription(details.getTitle());
                     }
                     else
                     {
                        // set the description from the document name
                        input.setDescription(
                              FilePathUtils.fileNameSansExtension(docPath));
                     }
                  }
                  onComplete.execute(input);
               }

               @Override
               public void onError(ServerError error)
               {
                  // this is unlikely since the RPC does little work, but
                  // we can't offer the right choices in the wizard if we
                  // don't know what we're working with.
                  display_.showErrorMessage("Could Not Publish", 
                        error.getMessage());
                  onComplete.execute(null);
               }
            });
   }
   
   private final Commands commands_;
   private final GlobalDisplay display_;
   private final Session session_;
   private final RSConnectServerOperations server_;
   private final RPubsServerOperations rpubsServer_;
   private final SourceServerOperations sourceServer_;
   private final DependencyManager dependencyManager_;
   private final EventBus events_;
   private final RSAccountConnector connector_;
   private final Provider<UIPrefs> pUiPrefs_;
   private final PlotPublishMRUList plotMru_;
   
   private boolean launchBrowser_ = false;
   private boolean sessionInited_ = false;
   private boolean depsPending_ = false;
   private String lastDeployedServer_ = "";
   
   // incremented on each RPubs publish (to provide a unique context)
   private static int rpubsCount_ = 0;
   
   private RSConnectDirectoryState dirState_;
   private boolean dirStateDirty_ = false;
   
   public final static String CLOUD_SERVICE_NAME = "ShinyApps.io";
   
   // No/unknown content type 
   public final static int CONTENT_TYPE_NONE       = 0;
   
   // A single HTML file representing a plot
   public final static int CONTENT_TYPE_PLOT       = 1;
   
   // A document (.Rmd, .md, etc.), 
   public final static int CONTENT_TYPE_DOCUMENT   = 2;
   
   // A Shiny application
   public final static int CONTENT_TYPE_APP        = 3;
   
   // A single-file Shiny application
   public final static int CONTENT_TYPE_APP_SINGLE = 4;
   
   // Standalone HTML (from HTML widgets/viewer pane, etc.)
   public final static int CONTENT_TYPE_HTML       = 5;
   
   // A .Rpres presentation
   public final static int CONTENT_TYPE_PRES       = 6;
   
   // A page in an R Markdown website
   public final static int CONTENT_TYPE_WEBSITE    = 7;
   
   public final static String CONTENT_CATEGORY_PLOT = "plot";
   public final static String CONTENT_CATEGORY_SITE = "site";
}
