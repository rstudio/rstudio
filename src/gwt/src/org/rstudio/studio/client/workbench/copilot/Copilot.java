/*
 * Copilot.java
 *
 * Copyright (C) 2023 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.copilot;

import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.DialogOptions;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.JSON;
import org.rstudio.core.client.Markdown;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.DialogBuilder;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ModalDialogTracker;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dialog.WebDialogBuilderFactory;
import org.rstudio.studio.client.projects.model.RProjectCopilotOptions;
import org.rstudio.studio.client.projects.ui.prefs.YesNoAskDefault;
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.copilot.model.CopilotConstants;
import org.rstudio.studio.client.workbench.copilot.model.CopilotEvent;
import org.rstudio.studio.client.workbench.copilot.model.CopilotEvent.CopilotEventType;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotDiagnosticsResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotInstallAgentResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotSignInResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotSignInResponseResult;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotSignOutResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotStatusResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotVerifyInstalledResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotDiagnostics;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotError;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.copilot.ui.CopilotInstallDialog;
import org.rstudio.studio.client.workbench.copilot.ui.CopilotSignInDialog;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Copilot implements ProjectOptionsChangedEvent.Handler
{
   @Inject
   public Copilot(GlobalDisplay display,
                  Commands commands,
                  GlobalDisplay globalDisplay,
                  EventBus events,
                  UserPrefs prefs,
                  Session session,
                  CopilotCommandBinder binder,
                  CopilotServerOperations server)
   {
      display_ = display;
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      events_ = events;
      prefs_ = prefs;
      session_ = session;
      server_ = server;
      
      binder.bind(commands_, this);
      
      // Detect attempts to modify the Copilot enabled preference through the
      // command palette, and ensure that Copilot is installed when doing so.
      prefs_.copilotEnabled().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         boolean ignoreNextChange_ = false;
         
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            if (ignoreNextChange_)
            {
               ignoreNextChange_ = false;
               return;
            }
            
            // Don't do anything if a Global Options or Project Options entry
            // is being shown.
            List<PopupPanel> modalDialogs = ModalDialogTracker.getModalDialogs();
            for (PopupPanel modalDialog : modalDialogs)
            {
               Element el = modalDialog.getElement();
               String id = el.getId();
               if (id == ElementIds.getDialogGlobalPrefs())
                  return;
            }
            
            boolean enabled = prefs_.copilotEnabled().getValue();
            if (enabled)
            {
               ensureAgentInstalled(new CommandWithArg<Boolean>()
               {
                  @Override
                  public void execute(Boolean isInstalled)
                  {
                     if (!isInstalled)
                     {
                        // Avoid recursion.
                        ignoreNextChange_ = true;
                        
                        // Eagerly change the preference here, so that we can
                        // respond to changes in the agent status.
                        prefs_.copilotEnabled().setGlobalValue(false);
                        prefs_.writeUserPrefs((completed) -> {});
                     }
                  }
               });
            }
         }
      });
    
      events_.addHandler(SessionInitEvent.TYPE, new SessionInitEvent.Handler()
      {
         @Override
         public void onSessionInit(SessionInitEvent event)
         {
            copilotProjectOptions_ = session_.getSessionInfo().getCopilotProjectOptions();
         }
      });
      
      events_.addHandler(ProjectOptionsChangedEvent.TYPE, new ProjectOptionsChangedEvent.Handler()
      {
         @Override
         public void onProjectOptionsChanged(ProjectOptionsChangedEvent event)
         {
            copilotProjectOptions_ = event.getData().getCopilotOptions();
            if (!isEnabled())
            {
               events_.fireEvent(new CopilotEvent(CopilotEventType.COPILOT_DISABLED));
            }
         }
      });

   }
   
   public boolean isEnabled()
   {
      if (copilotProjectOptions_ != null && session_.getSessionInfo().getActiveProjectFile() != null)
      {
         switch (copilotProjectOptions_.copilot_enabled)
         {
         case YesNoAskDefault.YES_VALUE: return true;
         case YesNoAskDefault.NO_VALUE: return false;
         default: {}
         }
      }
      
      return prefs_.copilotEnabled().getGlobalValue();
   }
   
   public void ensureAgentInstalled(CommandWithArg<Boolean> callback)
   {
      String progressLabel = constants_.copilotVerifyingInstallation();
      server_.copilotVerifyInstalled(
            new DelayedProgressRequestCallback<CopilotVerifyInstalledResponse>(progressLabel)
      {
         @Override
         protected void onSuccess(CopilotVerifyInstalledResponse response)
         {
            if (response.installed)
            {
               callback.execute(true);
            }
            else
            {
               installAgentWithPrompt(callback);
            }
         }
      });
   }
   
   private void installAgentWithPrompt(CommandWithArg<Boolean> callback)
   {
      server_.copilotVerifyInstalled(new ServerRequestCallback<CopilotVerifyInstalledResponse>()
      {
         @Override
         public void onResponseReceived(CopilotVerifyInstalledResponse response)
         {
            installAgentWithPromptImpl(response.installed, callback);
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            installAgentWithPromptImpl(false, callback);
         }
      });
   }
   
   private void installAgentWithPromptImpl(boolean isAlreadyInstalled, CommandWithArg<Boolean> callback)
   {
      CopilotInstallDialog dialog = new CopilotInstallDialog(isAlreadyInstalled);
      
      dialog.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            CommandWithArg<Boolean> wrappedCallback = (result) ->
            {
               dialog.closeDialog();
               callback.execute(result);
            };
            
            installAgent(
                  dialog.getProgressIndicator(),
                  wrappedCallback);
         }
      });
      
      dialog.addCancelHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            dialog.closeDialog();
            callback.execute(false);
         }
      });
      
      dialog.showModal();
   }
   
   private void installAgent(ProgressIndicator indicator,
                             CommandWithArg<Boolean> callback)
   {
      indicator.onProgress(constants_.copilotInstalling());
      server_.copilotInstallAgent(
            new ServerRequestCallback<CopilotInstallAgentResponse>()
            {
               @Override
               public void onResponseReceived(CopilotInstallAgentResponse response)
               {
                  indicator.onCompleted();
                  
                  String error = response.error;
                  if (error != null)
                  {
                     display_.showErrorMessage(constants_.copilotErrorInstalling(error));
                     callback.execute(false);
                  }
                  else
                  {
                     display_.showMessage(
                           MessageDisplay.MSG_INFO,
                           constants_.copilotInstallAgentDialogTitle(),
                           constants_.copilotInstallAgentSuccess());
                     callback.execute(true);
                  }
                  
               }

               @Override
               public void onError(ServerError error)
               {
                  indicator.onError(error.getUserMessage());
                  Debug.logError(error);
               }
            });
   }
   
   @Handler
   public void onCopilotInstallAgent()
   {
      installAgentWithPrompt(installed -> {});
   }
   
   @Handler
   public void onCopilotDiagnostics()
   {
      onCopilotDiagnostics(() -> {});
   }
   
   public void onCopilotDiagnostics(Command onCompleted)
   {
      ensureAgentInstalled((installed) ->
      {
         if (installed)
         {
            server_.copilotDiagnostics(new ServerRequestCallback<CopilotDiagnosticsResponse>()
            {
               
               @Override
               public void onResponseReceived(CopilotDiagnosticsResponse response)
               {
                  onCompleted.execute();
                  CopilotDiagnostics diagnostics = response.result.cast();
                  
                  // Prefer using a web dialog even on Desktop, as we want to allow customization
                  // of how the UI is presented. In particular, we want to allow users to select
                  // and copy text if they need to.
                  DialogOptions options = new DialogOptions();
                  options.width = "auto";
                  options.height = "auto";
                  options.userSelect = "text";

                  String report = Markdown.markdownToHtml(diagnostics.report);
                  HTML widget = new HTML(report);
                  widget.getElement().getStyle().setPadding(12, Unit.PX);
                  WebDialogBuilderFactory builder = GWT.create(WebDialogBuilderFactory.class);
                  DialogBuilder dialog = builder.create(
                        GlobalDisplay.MSG_INFO,
                        constants_.copilotDiagnosticsTitle(),
                        widget,
                        options);

                  dialog.showModal();
               }
               
               @Override
               public void onError(ServerError error)
               {
                  onCompleted.execute();
                  Debug.logError(error);
               }
            });
         }
      });
   }
   
   @Handler
   public void onCopilotSignIn()
   {
      ensureAgentInstalled((installed) ->
      {
         if (installed)
         {
            onCopilotSignIn((response) ->
            {
               if (response.error != null)
               {
                  globalDisplay_.showErrorMessage(response.error.getEndUserMessage());
               }
               else
               {
                  globalDisplay_.showMessage(
                        MessageDisplay.MSG_INFO,
                        constants_.copilotSignInDialogTitle(),
                        constants_.copilotSignedIn(response.result.user));
               }
            });
         }
      });
   }
   
   public void onCopilotSignIn(CommandWithArg<CopilotStatusResponse> callback)
   {
      server_.copilotSignIn(new DelayedProgressRequestCallback<CopilotSignInResponse>(constants_.copilotSigningIn())
      {
         private ModalDialogBase signInDialog_;
         private Timer statusTimer_;
         
         @Override
         protected void onSuccess(CopilotSignInResponse response)
         {
            CopilotError error = response.error;
            if (error != null)
            {
               globalDisplay_.showMessage(
                     MessageDisplay.MSG_ERROR,
                     constants_.copilotSignInDialogTitle(),
                     constants_.copilotError(error.code, error.message));
               return;
            }
            
            CopilotSignInResponseResult result = response.result.cast();
            if (result.status == CopilotConstants.STATUS_PROMPT_USER_DEVICE_FLOW)
            {
               // Generate the dialog.
               signInDialog_ = new CopilotSignInDialog(result.verificationUri, result.userCode);
               signInDialog_.showModal();
               
               // Start polling for status, to see when the user has finished authenticating.
               statusTimer_ = new Timer()
               {
                  @Override
                  public void run()
                  {
                     server_.copilotStatus(new ServerRequestCallback<CopilotStatusResponse>()
                     {
                        @Override
                        public void onResponseReceived(CopilotStatusResponse response)
                        {
                           if (response.result.status == CopilotConstants.STATUS_OK)
                           {
                              signInDialog_.closeDialog();
                              callback.execute(response);
                           }
                           else
                           {
                              statusTimer_.schedule(1000);
                           }
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                        }
                     });
                  }
               };
               
               statusTimer_.schedule(1000);
            }
            else if (result.status == CopilotConstants.STATUS_ALREADY_SIGNED_IN)
            {
               String message = constants_.copilotAlreadySignedIn(result.user);
               globalDisplay_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.copilotSignInDialogTitle(),
                     message);
            }
            else
            {
               showGenericResponseMessage(
                     constants_.copilotSignInDialogTitle(),
                     response.result);
            }
         }
      });
   }
   
   @Handler
   public void onCopilotSignOut()
   {
      onCopilotSignOut((response) ->
      {
         String status = response.result.status;
         if (StringUtil.equals(status, CopilotConstants.STATUS_NOT_SIGNED_IN))
         {
            display_.showMessage(
                  MessageDisplay.MSG_INFO,
                  constants_.copilotSignOutDialogTitle(),
                  constants_.copilotSignedOut());
         }
         else
         {
            showGenericResponseMessage(
                  constants_.copilotSignOutDialogTitle(),
                  response.result);
         }
      });
   }
   
   public void onCopilotSignOut(CommandWithArg<CopilotSignOutResponse> callback)
   {
      server_.copilotSignOut(new DelayedProgressRequestCallback<CopilotSignOutResponse>(constants_.copilotSigningOut())
      {
         @Override
         protected void onSuccess(CopilotSignOutResponse response)
         {
            callback.execute(response);
         }
      });
   }
   
   @Handler
   public void onCopilotStatus()
   {
      ensureAgentInstalled((installed) ->
      {
         onCopilotStatusImpl();
      });
   }
   
   private void onCopilotStatusImpl()
   {
      server_.copilotStatus(new DelayedProgressRequestCallback<CopilotStatusResponse>(constants_.copilotCheckingStatus())
      {
         @Override
         protected void onSuccess(CopilotStatusResponse response)
         {
            if (response == null)
            {
               display_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.copilotCheckStatusDialogTitle(),
                     constants_.copilotEmptyResponse());
               
            }
            else if (response.error != null)
            {
               String output = response.output;
               if (StringUtil.isNullOrEmpty(output))
                  output = constants_.copilotNoOutput();
               
               String message = constants_.copilotErrorStartingAgent(
                     response.error.getEndUserMessage(), output);
               
               display_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.copilotCheckStatusDialogTitle(),
                     message);
            }
            else if (response.reason != null)
            {
               int reason = (int) response.reason.valueOf();
               display_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.copilotCheckStatusDialogTitle(),
                     CopilotResponseTypes.CopilotAgentNotRunningReason.reasonToString(reason));
            }
            else if (response.result.status == CopilotConstants.STATUS_NOT_SIGNED_IN)
            {
               display_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.copilotStatusDialogTitle(),
                     constants_.copilotNotSignedIn());
            }
            else if (response.result.status == CopilotConstants.STATUS_OK)
            {
               display_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.copilotStatusDialogTitle(),
                     constants_.copilotCurrentlySignedIn(response.result.user));
            }
            else
            {
               showGenericResponseMessage(
                     constants_.copilotCheckStatusDialogTitle(),
                     response.result);
            }
            
            Debug.logObject(response);
         }
      });
   }
   
   public String messageForError(CopilotError error)
   {
      Integer code = error.code;
      
      if (code == CopilotConstants.ErrorCodes.NOT_SIGNED_IN)
      {
         return constants_.copilotNotSignedInShort();
      }
      else
      {
         return error.message;
      }
   }
   
   private void showGenericResponseMessage(String title, Object object)
   {
      globalDisplay_.showMessage(
            MessageDisplay.MSG_INFO,
            title,
            JSON.stringify(object, 4));
   }
   
   @Override
   public void onProjectOptionsChanged(ProjectOptionsChangedEvent event)
   {
      copilotProjectOptions_ = event.getData().getCopilotOptions();
   }
   
   interface CopilotCommandBinder
         extends CommandBinder<Commands, Copilot>
   {
   }
   
   private RProjectCopilotOptions copilotProjectOptions_;
   
   private final GlobalDisplay display_;
   private final Commands commands_;
   private final EventBus events_;
   private final UserPrefs prefs_;
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
   private final CopilotServerOperations server_;

   private static final CopilotUIConstants constants_ = GWT.create(CopilotUIConstants.class);
}
