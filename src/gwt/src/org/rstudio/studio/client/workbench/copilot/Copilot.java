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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JSON;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.model.RProjectAssistantOptions;
import org.rstudio.studio.client.projects.ui.prefs.YesNoAskDefault;
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.copilot.model.CopilotConstants;
import org.rstudio.studio.client.workbench.assistant.model.AssistantEvent;
import org.rstudio.studio.client.workbench.assistant.model.AssistantEvent.AssistantEventType;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantDiagnosticsResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantSignInResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantSignInResponseResult;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantSignOutResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantStatusResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantDiagnostics;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantError;
import org.rstudio.studio.client.workbench.assistant.server.AssistantServerOperations;
import org.rstudio.studio.client.workbench.copilot.ui.CopilotDiagnosticsDialog;
import org.rstudio.studio.client.workbench.copilot.ui.CopilotSignInDialog;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
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
                  AssistantServerOperations server)
   {
      display_ = display;
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      events_ = events;
      prefs_ = prefs;
      session_ = session;
      server_ = server;
      
      binder.bind(commands_, this);
      
      events_.addHandler(SessionInitEvent.TYPE, new SessionInitEvent.Handler()
      {
         @Override
         public void onSessionInit(SessionInitEvent event)
         {
            assistantProjectOptions_ = session_.getSessionInfo().getAssistantProjectOptions();
         }
      });
      
      events_.addHandler(ProjectOptionsChangedEvent.TYPE, new ProjectOptionsChangedEvent.Handler()
      {
         @Override
         public void onProjectOptionsChanged(ProjectOptionsChangedEvent event)
         {
            assistantProjectOptions_ = event.getData().getAssistantOptions();
            if (!isEnabled())
            {
               events_.fireEvent(new AssistantEvent(AssistantEventType.ASSISTANT_DISABLED));
            }
         }
      });

   }
   
   public boolean isEnabled()
   {
      if (assistantProjectOptions_ != null && session_.getSessionInfo().getActiveProjectFile() != null)
      {
         switch (assistantProjectOptions_.copilot_enabled)
         {
         case YesNoAskDefault.YES_VALUE: return true;
         case YesNoAskDefault.NO_VALUE: return false;
         default: {}
         }
      }
      
      return prefs_.copilotEnabled().getGlobalValue();
   }
   
   @Handler
   public void onCopilotDiagnostics()
   {
      onCopilotDiagnostics(() -> {});
   }
   
   public void onCopilotDiagnostics(Command onCompleted)
   {
      server_.assistantDiagnostics(new ServerRequestCallback<AssistantDiagnosticsResponse>()
      {

         @Override
         public void onResponseReceived(AssistantDiagnosticsResponse response)
         {
            onCompleted.execute();

            if (response.error != null)
            {
               globalDisplay_.showErrorMessage(response.error.message);
            }
            else
            {
               AssistantDiagnostics diagnostics = response.result.cast();
               CopilotDiagnosticsDialog dialog = new CopilotDiagnosticsDialog(diagnostics.report);
               dialog.showModal();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            onCompleted.execute();
            Debug.logError(error);
         }
      });
   }
   
   @Handler
   public void onCopilotSignIn()
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

   public void onCopilotSignIn(CommandWithArg<AssistantStatusResponse> callback)
   {
      server_.assistantSignIn(new DelayedProgressRequestCallback<AssistantSignInResponse>(constants_.copilotSigningIn())
      {
         private ModalDialogBase signInDialog_;
         private Timer statusTimer_;

         @Override
         protected void onSuccess(AssistantSignInResponse response)
         {
            AssistantError error = response.error;
            if (error != null)
            {
               globalDisplay_.showMessage(
                     MessageDisplay.MSG_ERROR,
                     constants_.copilotSignInDialogTitle(),
                     constants_.copilotError(error.code, error.message));
               return;
            }

            AssistantSignInResponseResult result = response.result.cast();
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
                     server_.assistantStatus(new ServerRequestCallback<AssistantStatusResponse>()
                     {
                        @Override
                        public void onResponseReceived(AssistantStatusResponse response)
                        {
                           if (response.result.status == CopilotConstants.STATUS_OK)
                           {
                              signInDialog_.closeDialog();
                              callback.execute(response);
                           }
                           else if (response.result.status == CopilotConstants.STATUS_NOT_AUTHORIZED)
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

   public void onCopilotSignOut(CommandWithArg<AssistantSignOutResponse> callback)
   {
      server_.assistantSignOut(new DelayedProgressRequestCallback<AssistantSignOutResponse>(constants_.copilotSigningOut())
      {
         @Override
         protected void onSuccess(AssistantSignOutResponse response)
         {
            callback.execute(response);
         }
      });
   }

   @Handler
   public void onCopilotStatus()
   {
      onCopilotStatusImpl();
   }

   private void onCopilotStatusImpl()
   {
      server_.assistantStatus(new DelayedProgressRequestCallback<AssistantStatusResponse>(constants_.copilotCheckingStatus())
      {
         @Override
         protected void onSuccess(AssistantStatusResponse response)
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
                     AssistantResponseTypes.AssistantAgentNotRunningReason.reasonToString(reason));
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

   public String messageForError(AssistantError error)
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
      assistantProjectOptions_ = event.getData().getAssistantOptions();
   }
   
   interface CopilotCommandBinder
         extends CommandBinder<Commands, Copilot>
   {
   }
   
   private RProjectAssistantOptions assistantProjectOptions_;
   
   private final GlobalDisplay display_;
   private final Commands commands_;
   private final EventBus events_;
   private final UserPrefs prefs_;
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
   private final AssistantServerOperations server_;

   private static final CopilotUIConstants constants_ = GWT.create(CopilotUIConstants.class);
}
