/*
 * RSConnect.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

import com.google.gwt.aria.client.Roles;
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
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeployInitiatedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentCancelledEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentCompletedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentFailedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentStartedEvent;
import org.rstudio.studio.client.rsconnect.model.PlotPublishMRUList;
import org.rstudio.studio.client.rsconnect.model.QmdPublishDetails;
import org.rstudio.studio.client.rsconnect.model.RSConnectApplicationInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectDirectoryState;
import org.rstudio.studio.client.rsconnect.model.RSConnectLintResults;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSettings;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSource;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.model.RenderedDocPreview;
import org.rstudio.studio.client.rsconnect.model.RmdPublishDetails;
import org.rstudio.studio.client.rsconnect.ui.RSAccountConnector;
import org.rstudio.studio.client.rsconnect.ui.RSConnectDeployDialog;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishWizard;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.GWT;
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
public class RSConnect implements SessionInitEvent.Handler,
                                  RSConnectActionEvent.Handler,
                                  RSConnectDeployInitiatedEvent.Handler,
                                  RSConnectDeploymentCompletedEvent.Handler,
                                  RSConnectDeploymentFailedEvent.Handler,
                                  RSConnectDeploymentCancelledEvent.Handler
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
                    Provider<UserPrefs> pUserPrefs,
                    Provider<UserState> pUserState,
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
      pUserPrefs_ = pUserPrefs;
      pUserState_ = pUserState;
      plotMru_ = plotMru;

      binder.bind(commands, this);

      events.addHandler(SessionInitEvent.TYPE, this);
      events.addHandler(RSConnectActionEvent.TYPE, this);
      events.addHandler(RSConnectDeployInitiatedEvent.TYPE, this);
      events.addHandler(RSConnectDeploymentCompletedEvent.TYPE, this);
      events.addHandler(RSConnectDeploymentFailedEvent.TYPE, this);
      events.addHandler(RSConnectDeploymentCancelledEvent.TYPE, this);

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
         constants_.publishingContentLabel(),
         event.getContentType() == CONTENT_TYPE_DOCUMENT ||
         event.getContentType() == CONTENT_TYPE_WEBSITE ||
         event.getContentType() == CONTENT_TYPE_QUARTO_WEBSITE ,
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

   private boolean supportedRPubsDocExtension(String filename)
   {
      if (StringUtil.isNullOrEmpty(filename))
         return false;

      String extension = FileSystemItem.getExtensionFromPath(filename).toLowerCase();
      return StringUtil.equals(extension, ".html") ||
             StringUtil.equals(extension, ".htm") ||
             StringUtil.equals(extension, ".nb.html");
   }

   private void publishAsRPubs(RSConnectActionEvent event)
   {
      // If previously published but the rendered file is now missing, give a warning instead
      // of trying to republish.
      if (event.getFromPrevious() != null &&
            !StringUtil.isNullOrEmpty(event.getFromPrevious().getBundleId()) &&
            StringUtil.isNullOrEmpty(event.getHtmlFile()))
      {
         display_.showErrorMessage(constants_.republishDocument(),
               constants_.republishDocumentMessage());
         return;
      }

      // If we don't have an html file, can't publish to RPubs, e.g. create a generic markdown
      // file (.md), don't preview it, and try to publish it to RPubs. Also, prevent publishing
      // unsupported output formats; relatively easy to get in this state; e.g. Knit and
      // publish HTML to RPubs, then Knit as PDF and try to republish.
      if (StringUtil.isNullOrEmpty(event.getHtmlFile()) ||
            (event.getContentType() == CONTENT_TYPE_DOCUMENT &&
                  !supportedRPubsDocExtension(event.getHtmlFile())))
      {
         showUnsupportedRPubsFormatMessage();
         return;
      }

      String ctx = constants_.publishRpubTitle(contentTypeDesc(event.getContentType()));
      RPubsUploadDialog dlg = new RPubsUploadDialog(
            constants_.publishWizardLabel(),
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
            pUserState_.get().enableRsconnectPublishUi().getGlobalValue());
      input.setCloudUIEnabled(
            pUserState_.get().enableCloudPublishUi().getGlobalValue());
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
            if (event.getFromPrevious().getServer() == "rpubs.com")
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
                     
                     boolean isQuarto = false;
                     if (event.getFromPreview() != null)
                     {
                        isQuarto = event.getFromPreview().isQuarto();
                     }

                     if (event.getFromPrevious().getAsStatic())
                        publishAsFiles(event,
                              new RSConnectPublishSource(event.getPath(),
                                    event.getHtmlFile(),
                                    arg.getWebsiteDir(),
                                    arg.getWebsiteOutputDir(),
                                    arg.isSelfContained(),
                                    true,
                                    arg.isShiny(),
                                    isQuarto,
                                    arg.getDescription(),
                                    event.getContentType()));
                     else
                        publishAsCode(event, arg.getWebsiteDir(),
                              arg.isShiny());
                  }
               });
               }
               break;
            case CONTENT_TYPE_QUARTO_WEBSITE:
               // Quarto website publishing metadata is extracted from the active Quarto project
               QuartoConfig config = session_.getSessionInfo().getQuartoConfig();
               FileSystemItem projectDir = FileSystemItem.createDir(config.project_dir);
               String websiteOutputDir = projectDir.completePath(config.project_output_dir);

               if (event.getFromPrevious().getAsStatic())
               {
                  publishAsFiles(event,
                     new RSConnectPublishSource(event.getPath(),
                        config.project_dir,
                        config.project_dir,
                        websiteOutputDir,
                        input.isSelfContained(),
                        true, // isStatic
                        false, // isShiny
                        true, // isQuarto
                        input.getDescription(),
                        event.getContentType()));
               }
               else
               {
                  publishAsCode(event, config.project_dir, false /* isShiny */);
               }
               break;


            case CONTENT_TYPE_PLUMBER_API:
               publishAsCode(event, null, false);
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
         else if (event.getContentType() == RSConnect.CONTENT_TYPE_QUARTO_WEBSITE)
         {
            QuartoConfig config = session_.getSessionInfo().getQuartoConfig();
            FileSystemItem projectDir = FileSystemItem.createDir(config.project_dir);

            // fill publish input from session
            input.setIsQuarto(true);
            input.setWebsiteDir(config.project_dir);
            input.setWebsiteOutputDir(projectDir.completePath(config.project_output_dir));
            showPublishUI(input);
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
      else if (input.isWebsiteContentType() ||
               (input.getContentType() == CONTENT_TYPE_DOCUMENT && input.isWebsiteRmd()))
      {
         if (input.hasDocOutput() || input.isWebsiteContentType())
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
               // need to disambiguate between code/output and/or
               // single/multi page
               publishWithWizard(input);
            }
            else if (!input.isSelfContained())
            {
               // we should generally hide the button in this case
               display_.showErrorMessage(constants_.contentNotPublishable(),
                     constants_.contentNotPublishableMessage());
            }
            else
            {
               // RStudio Connect is disabled, go straight to RPubs
               publishAsRPubs(event);
            }
         }
      }
      else if (input.getContentType() == CONTENT_TYPE_APP ||
               input.getContentType() == CONTENT_TYPE_APP_SINGLE)
      {
         publishAsCode(event, null, true);
      }
      else if (input.getContentType() == CONTENT_TYPE_PLUMBER_API)
      {
         if (!input.isConnectUIEnabled())
         {
            display_.showErrorMessage(constants_.apiNotPublishable(),
                     constants_.apiNotPublishableMessage());
         }
         else
         {
            publishAsCode(event, null, false);
         }
      }
   }

   private void publishAsCode(RSConnectActionEvent event, String websiteDir, boolean isShiny)
   {
      boolean isAPI = event.getContentType() == CONTENT_TYPE_PLUMBER_API;

      RSConnectPublishSource source = null;
      if (event.getContentType() == CONTENT_TYPE_APP ||
          event.getContentType() == CONTENT_TYPE_APP_SINGLE ||
          isAPI)
      {
         if (StringUtil.getExtension(event.getPath()).equalsIgnoreCase("r"))
         {
            FileSystemItem rFile = FileSystemItem.createFile(event.getPath());

            // use the directory for the deployment record when publishing APIs or
            // directory-based apps; use the file itself when publishing
            // single-file apps
            source = new RSConnectPublishSource(rFile.getParentPathString(),
                  event.getContentType() == CONTENT_TYPE_APP_SINGLE ?
                        rFile.getName() :
                        rFile.getParentPathString(),
                        isAPI);

         }
         else
         {
            source = new RSConnectPublishSource(event.getPath(),
                  event.getPath(),
                  isAPI);
         }
      }
      else
      {
         source = new RSConnectPublishSource(event.getPath(), websiteDir,
            false, false, isShiny,
            event.getContentType() == RSConnect.CONTENT_TYPE_QUARTO_WEBSITE, null, event.getContentType());
      }

      // detect quarto
      if (event.getFromPreview() != null)
      {
         source.setIsQuarto(event.getFromPreview().isQuarto());
      }

      publishAsFiles(event, source);
   }

   private void publishAsStatic(RSConnectPublishInput input)
   {
      RSConnectPublishSource source = null;
      if (input.getContentType() == RSConnect.CONTENT_TYPE_DOCUMENT ||
          input.isWebsiteContentType())
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
               input.isQuarto(),
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
                     constants_.lintFailed(),
                     constants_.lintFailedMessage(results.getErrorMessage()), false,
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
                     constants_.publishAnyway(), constants_.cancel(), false);
            }
            else if (results.hasLint())
            {
               display_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
                     constants_.publishContentIssuesFound(),
                     constants_.publishContentIssuesMessage()
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
                     constants_.reviewIssues(), constants_.publishAnyway(), true);
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
   public void onRSConnectDeploymentCancelled(
         RSConnectDeploymentCancelledEvent event)
   {
      display_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
            constants_.stopDeploymentQuestion(),
            constants_.onRSConnectDeploymentCancelledMessage(),
            false, // include cancel
            () -> {
                server_.cancelPublish(new ServerRequestCallback<Boolean>()
                {
                  @Override
                  public void onError(ServerError error)
                  {
                     display_.showErrorMessage(constants_.errorStoppingDeployment(),
                           error.getMessage());
                  }

                  @Override
                  public void onResponseReceived(Boolean result)
                  {
                     if (!result)
                     {
                       display_.showErrorMessage(constants_.couldNotCancelDeployment(),
                             constants_.deploymentNotCancelledMessage());
                     }
                  }
                });
            },
            null,
            null,
            constants_.stopDeployment(),
            constants_.cancel(),
            false);
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

      new ModalDialogBase(Roles.getAlertdialogRole())
      {
         @Override
         protected Widget createMainWidget()
         {
            setText(constants_.publishFailed());
            addOkButton(new ThemedButton(constants_.okCapitalized(), new ClickHandler()
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
            panel.add(new HTML(constants_.onRSConnectDeploymentFailedHtml(constants_.onRSConnectDeploymentFailedHtmlP1(),
                    constants_.onRSConnectDeploymentFailedHtmlP2(), serverUrl, constants_.onRSConnectDeploymentFailedHtmlP3(),
                    constants_.onRSConnectDeploymentFailedHtmlP4(), event.getData().getHttpStatus())));
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
            SessionUtils.showPublishUi(session_, pUserState_.get()));

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
         boolean isQuarto,
         boolean launch,
         JavaScriptObject record) /*-{
      $wnd.opener.deployToRSConnect(sourceFile, deployDir, deployFile,
                                    websiteDir, description, deployFiles,
                                    additionalFiles, ignoredFiles, isSelfContained,
                                    isShiny, asMultiple, asStatic, isQuarto, launch,
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
         return constants_.application();
      case RSConnect.CONTENT_TYPE_PLOT:
         return constants_.plot();
      case RSConnect.CONTENT_TYPE_HTML:
         return constants_.html();
      case RSConnect.CONTENT_TYPE_DOCUMENT:
         return constants_.document();
      case RSConnect.CONTENT_TYPE_PRES:
         return constants_.presentation();
      case RSConnect.CONTENT_TYPE_WEBSITE:
         return constants_.website();
      case RSConnect.CONTENT_TYPE_PLUMBER_API:
         return constants_.api();
      case RSConnect.CONTENT_TYPE_QUARTO_WEBSITE:
         return constants_.quartoWebsite();
      }
      return constants_.content();
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
               result.getSource().isQuarto(),
               launchBrowser,
               RSConnectDeploymentRecord.create(result.getAppName(),
                     result.getAppTitle(), result.getAppId(), result.getAccount(), ""));

         // we can't raise the main window if we aren't in desktop mode, so show
         // a dialog to guide the user there
         if (!Desktop.hasDesktopFrame())
         {
            display_.showMessage(GlobalDisplay.MSG_INFO, constants_.deploymentStarted(),
                    constants_.rstudioDeployingApp(result.getAppName()));
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
                     result.getAppTitle(), result.getAppId(), result.getAccount(), "")));
      }
   }

   // Private methods ---------------------------------------------------------
   private void showUnsupportedRPubsFormatMessage()
   {
      display_.showErrorMessage(constants_.unsupportedDocumentFormat(),
            constants_.showUnsupportedRPubsFormatMessageError());
   }

   private void uploadToRPubs(RSConnectPublishInput input,
         RSConnectPublishResult result,
         final ProgressIndicator indicator)
   {
      if (input.getContentType() == CONTENT_TYPE_DOCUMENT)
      {
         if (!input.hasDocOutput())
         {
            display_.showErrorMessage(constants_.publishDocument(),
                  constants_.uploadToRPubsErrorMessage());
            indicator.onCompleted();
            return;
         }
         else if (!supportedRPubsDocExtension(input.getDocOutput()))
         {
            showUnsupportedRPubsFormatMessage();
            indicator.onCompleted();
            return;
         }
      }

      RPubsUploader uploader = new RPubsUploader(rpubsServer_, display_,
            events_, "rpubs-" + rpubsCount_++);
      String contentType = contentTypeDesc(input.getContentType());
      indicator.onProgress(constants_.uploadingContent(contentType));
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
                             event.getRecord().getAppId(),
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
               display_.showErrorMessage(constants_.deploymentInProgress(),
                     constants_.onlyOneDeploymentAtATime());
            }
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage(constants_.errorDeployingApplication(),
                  constants_.couldNotDeployApplication(event.getRecord().getName(), error.getMessage()));
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
            display_.showErrorMessage(constants_.errorConfiguringApplication(),
                  constants_.couldNotDetermineAppDeployments(dir,error.getMessage()));
         }
      });
   }

   // Manage, step 2: Get the status of the applications from the server
   private void configureShinyApp(final String dir,
         JsArray<RSConnectDeploymentRecord> records)
   {
      if (records.length() == 0)
      {
         display_.showMessage(GlobalDisplay.MSG_INFO, constants_.noDeploymentsFound(),
               constants_.noApplicationDeploymentsFound(dir));
         return;
      }

      // If we know the most recent deployment of the directory, act on that
      // deployment by default
      final ArrayList<RSConnectDeploymentRecord> recordList = new ArrayList<>();
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
            if (record.getUrl() == lastRecord.getUrl())
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
            display_.showErrorMessage(constants_.errorListingApplications(),
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
            if (candidate.getName() == records.get(i).getName())
            {
               // show the management ui
               display_.openWindow(candidate.getConfigUrl());
               return;
            }
         }
      }
      display_.showMessage(GlobalDisplay.MSG_INFO,
            constants_.noRunningDeploymentsFound(), constants_.noApplicationDeploymentsFrom(dir));
   }

   private final native void exportNativeCallbacks() /*-{
      var thiz = this;
      $wnd.deployToRSConnect = $entry(
         function(sourceFile, deployDir, deployFile, websiteDir, description, deployFiles, additionalFiles, ignoredFiles, isSelfContained, isShiny, asMultiple, asStatic, isQuarto, launch, record) {
            thiz.@org.rstudio.studio.client.rsconnect.RSConnect::deployToRSConnect(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JsArrayString;Lcom/google/gwt/core/client/JsArrayString;Lcom/google/gwt/core/client/JsArrayString;ZZZZZZLcom/google/gwt/core/client/JavaScriptObject;)(sourceFile, deployDir, deployFile, websiteDir, description, deployFiles, additionalFiles, ignoredFiles, isSelfContained, isShiny, asMultiple, asStatic, isQuarto, launch, record);
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
                                  boolean isQuarto,
                                  boolean launch,
                                  JavaScriptObject jsoRecord)
   {
      // this can be invoked by a satellite, so bring the main frame to the
      // front if we can
      if (Desktop.hasDesktopFrame())
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
                  websiteDir, isSelfContained, asStatic, isShiny, isQuarto, description),
            new RSConnectPublishSettings(deployFilesList,
                  additionalFilesList, ignoredFilesList, asMultiple, asStatic),
            launch, record));
   }

   private void fillInputFromDoc(final RSConnectPublishInput input,
         final String docPath,
         final CommandWithArg<RSConnectPublishInput> onComplete)
   {
      boolean isQuarto = false;
      if (input.getOriginatingEvent() != null &&
          input.getOriginatingEvent().getFromPreview() != null)
      {
         isQuarto = input.getOriginatingEvent().getFromPreview().isQuarto();
      }

      if (isQuarto)
      {
         // Quarto metadata lookup can take a couple of seconds; ensure the
         // user can see some progress while we're doing it
         final ProgressIndicator indicator = display_.getProgressIndicator(constants_.error());
         indicator.onProgress(constants_.preparingForPublish());

         server_.quartoPublishDetails(
            docPath,
            new ServerRequestCallback<QmdPublishDetails>()
            {
               @Override
               public void onResponseReceived(QmdPublishDetails details)
               {
                  indicator.onCompleted();
                  RenderedDocPreview previewParams = input.getOriginatingEvent().getFromPreview();
                  if (previewParams != null)
                  {
                     if (StringUtil.isNullOrEmpty(details.website_output_dir))
                        previewParams.setOutputFile(details.output_file);
                     else
                        previewParams.setOutputFile(details.website_output_dir);
                     previewParams.setWebsiteDir(details.website_dir);
                  }
                  input.setIsMultiRmd(false);
                  input.setIsQuarto(true);
                  input.setIsShiny(details.is_shiny_qmd);
                  input.setIsSelfContained(details.is_self_contained);
                  input.setHasConnectAccount(details.has_connect_account);
                  input.setWebsiteDir(details.website_dir);
                  input.setWebsiteOutputDir(details.website_output_dir);

                  onComplete.execute(input);
               }

               @Override
               public void onError(ServerError error)
               {
                  indicator.onError(error.getMessage());
                  onComplete.execute(null);
               }
            }
         );
      }
      else
      {
         server_.getRmdPublishDetails(
            docPath,
            new ServerRequestCallback<RmdPublishDetails>()
            {
               @Override
               public void onResponseReceived(RmdPublishDetails details)
               {
                  input.setIsMultiRmd(details.is_multi_rmd);
                  input.setIsShiny(details.is_shiny_rmd);
                  input.setIsSelfContained(details.is_self_contained);
                  input.setIsQuarto(false);
                  input.setHasConnectAccount(details.has_connect_account);
                  input.setWebsiteDir(details.website_dir);
                  input.setWebsiteOutputDir(details.website_output_dir);
                  if (StringUtil.isNullOrEmpty(input.getDescription()))
                  {
                     if (!StringUtil.isNullOrEmpty(details.title))
                     {
                        // set the description from the document title, if we
                        // have it
                        input.setDescription(details.title);
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
                  display_.showErrorMessage(constants_.couldNotPublish(),
                     error.getMessage());
                  onComplete.execute(null);
               }
            });
      }
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
   private final Provider<UserPrefs> pUserPrefs_;
   private final Provider<UserState> pUserState_;
   private final PlotPublishMRUList plotMru_;

   private boolean launchBrowser_ = false;
   private boolean sessionInited_ = false;
   private boolean depsPending_ = false;
   private String lastDeployedServer_ = "";

   // incremented on each RPubs publish (to provide a unique context)
   private static int rpubsCount_ = 0;

   private RSConnectDirectoryState dirState_;
   private boolean dirStateDirty_ = false;

   public final static String SHINY_APPS_SERVICE_NAME = "ShinyApps.io";

   // will need to be updated to posit.cloud
   public final static String CLOUD_SERVICE_NAME = "rstudio.cloud";

   // No/unknown content type
   public final static int CONTENT_TYPE_NONE           = 0;

   // A single HTML file representing a plot
   public final static int CONTENT_TYPE_PLOT           = 1;

   // A document (.Rmd, .md, etc.),
   public final static int CONTENT_TYPE_DOCUMENT       = 2;

   // A Shiny application
   public final static int CONTENT_TYPE_APP            = 3;

   // A single-file Shiny application
   public final static int CONTENT_TYPE_APP_SINGLE     = 4;

   // Standalone HTML (from HTML widgets/viewer pane, etc.)
   public final static int CONTENT_TYPE_HTML           = 5;

   // A .Rpres presentation
   public final static int CONTENT_TYPE_PRES           = 6;

   // A page in an R Markdown website
   public final static int CONTENT_TYPE_WEBSITE        = 7;

   // Plumber API
   public final static int CONTENT_TYPE_PLUMBER_API    = 8;

   // A Quarto website
   public final static int CONTENT_TYPE_QUARTO_WEBSITE = 9;

   public final static String CONTENT_CATEGORY_PLOT = "plot";
   public final static String CONTENT_CATEGORY_SITE = "site";
   public final static String CONTENT_CATEGORY_API = "api";
   private static final RsconnectConstants constants_ = GWT.create(RsconnectConstants.class);
}
