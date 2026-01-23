/*
 * Assistant.java
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
package org.rstudio.studio.client.workbench.assistant;

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
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.assistant.model.AssistantConstants;
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
import org.rstudio.studio.client.workbench.assistant.ui.AssistantDiagnosticsDialog;
import org.rstudio.studio.client.workbench.assistant.ui.AssistantSignInDialog;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Assistant implements ProjectOptionsChangedEvent.Handler
{
   @Inject
   public Assistant(GlobalDisplay display,
                    Commands commands,
                    GlobalDisplay globalDisplay,
                    EventBus events,
                    UserPrefs prefs,
                    Session session,
                    AssistantCommandBinder binder,
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
      String assistantType = getAssistantType();
      return !StringUtil.equals(assistantType, UserPrefsAccessor.ASSISTANT_NONE);
   }

   public String getAssistantType()
   {
      // Check project options first
      if (assistantProjectOptions_ != null && session_.getSessionInfo().getActiveProjectFile() != null)
      {
         String projectAssistant = assistantProjectOptions_.assistant;
         if (projectAssistant != null && !projectAssistant.isEmpty() && !projectAssistant.equals("default"))
         {
            return projectAssistant;
         }
      }

      // Fall back to global preference
      return prefs_.assistant().getGlobalValue();
   }

   @Handler
   public void onAssistantDiagnostics()
   {
      showDiagnostics(() -> {});
   }

   public void showDiagnostics(Command onCompleted)
   {
      showDiagnostics(getAssistantType(), onCompleted);
   }

   public void showDiagnostics(String assistantType, Command onCompleted)
   {
      server_.assistantDiagnostics(assistantType, new ServerRequestCallback<AssistantDiagnosticsResponse>()
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
               AssistantDiagnosticsDialog dialog = new AssistantDiagnosticsDialog(diagnostics.report, getDisplayName(assistantType));
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
   public void onAssistantSignIn()
   {
      String assistantType = getAssistantType();
      signIn(assistantType, (response) ->
      {
         if (response.error != null)
         {
            globalDisplay_.showErrorMessage(response.error.getEndUserMessage());
         }
         else
         {
            globalDisplay_.showMessage(
                  MessageDisplay.MSG_INFO,
                  constants_.assistantSignInDialogTitle(getDisplayName(assistantType)),
                  constants_.assistantSignedIn(response.result.user));
         }
      });
   }

   public void signIn(String assistantType, CommandWithArg<AssistantStatusResponse> callback)
   {
      server_.assistantSignIn(assistantType, new DelayedProgressRequestCallback<AssistantSignInResponse>(constants_.assistantSigningIn())
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
                     constants_.assistantSignInDialogTitle(getDisplayName(assistantType)),
                     constants_.assistantError(error.code, error.message));
               return;
            }

            AssistantSignInResponseResult result = response.result.cast();
            if (result.status == AssistantConstants.STATUS_PROMPT_USER_DEVICE_FLOW)
            {
               // Generate the dialog.
               signInDialog_ = new AssistantSignInDialog(result.verificationUri, result.userCode, getDisplayName(assistantType));
               signInDialog_.showModal();

               // Start polling for status, to see when the user has finished authenticating.
               statusTimer_ = new Timer()
               {
                  @Override
                  public void run()
                  {
                     server_.assistantStatus(assistantType, new ServerRequestCallback<AssistantStatusResponse>()
                     {
                        @Override
                        public void onResponseReceived(AssistantStatusResponse response)
                        {
                           if (response.result.status == AssistantConstants.STATUS_OK)
                           {
                              signInDialog_.closeDialog();
                              callback.execute(response);
                           }
                           else if (response.result.status == AssistantConstants.STATUS_NOT_AUTHORIZED)
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
            else if (result.status == AssistantConstants.STATUS_ALREADY_SIGNED_IN)
            {
               String message = constants_.assistantAlreadySignedIn(result.user);
               globalDisplay_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.assistantSignInDialogTitle(getDisplayName(assistantType)),
                     message);
            }
            else
            {
               showGenericResponseMessage(
                     constants_.assistantSignInDialogTitle(getDisplayName(assistantType)),
                     response.result);
            }
         }
      });
   }

   @Handler
   public void onAssistantSignOut()
   {
      String assistantType = getAssistantType();
      signOut(assistantType, (response) ->
      {
         String status = response.result.status;
         if (StringUtil.equals(status, AssistantConstants.STATUS_NOT_SIGNED_IN))
         {
            display_.showMessage(
                  MessageDisplay.MSG_INFO,
                  constants_.assistantSignOutDialogTitle(getDisplayName(assistantType)),
                  constants_.assistantSignedOut(getDisplayName(assistantType)));
         }
         else
         {
            showGenericResponseMessage(
                  constants_.assistantSignOutDialogTitle(getDisplayName(assistantType)),
                  response.result);
         }
      });
   }

   public void signOut(String assistantType, CommandWithArg<AssistantSignOutResponse> callback)
   {
      server_.assistantSignOut(assistantType, new DelayedProgressRequestCallback<AssistantSignOutResponse>(constants_.assistantSigningOut())
      {
         @Override
         protected void onSuccess(AssistantSignOutResponse response)
         {
            callback.execute(response);
         }
      });
   }

   @Handler
   public void onAssistantStatus()
   {
      checkStatus(getAssistantType());
   }

   public void checkStatus(String assistantType)
   {
      String displayName = getDisplayName(assistantType);
      server_.assistantStatus(assistantType, new DelayedProgressRequestCallback<AssistantStatusResponse>(constants_.assistantCheckingStatus())
      {
         @Override
         protected void onSuccess(AssistantStatusResponse response)
         {
            if (response == null)
            {
               display_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.assistantCheckStatusDialogTitle(displayName),
                     constants_.assistantEmptyResponse(displayName));

            }
            else if (response.error != null)
            {
               String output = response.output;
               if (StringUtil.isNullOrEmpty(output))
                  output = constants_.assistantNoOutput();

               String message = constants_.assistantErrorStartingAgent(
                     displayName, response.error.getEndUserMessage(), output);

               display_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.assistantCheckStatusDialogTitle(displayName),
                     message);
            }
            else if (response.reason != null)
            {
               int reason = (int) response.reason.valueOf();
               display_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.assistantCheckStatusDialogTitle(displayName),
                     AssistantResponseTypes.AssistantAgentNotRunningReason.reasonToString(reason, displayName));
            }
            else if (response.result.status == AssistantConstants.STATUS_NOT_SIGNED_IN)
            {
               display_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.assistantStatusDialogTitle(displayName),
                     constants_.assistantNotSignedIn(displayName));
            }
            else if (response.result.status == AssistantConstants.STATUS_OK)
            {
               display_.showMessage(
                     MessageDisplay.MSG_INFO,
                     constants_.assistantStatusDialogTitle(displayName),
                     constants_.assistantCurrentlySignedIn(response.result.user));
            }
            else
            {
               showGenericResponseMessage(
                     constants_.assistantCheckStatusDialogTitle(displayName),
                     response.result);
            }

            Debug.logObject(response);
         }
      });
   }

   public String messageForError(AssistantError error)
   {
      Integer code = error.code;

      if (code == AssistantConstants.ErrorCodes.NOT_SIGNED_IN)
      {
         return constants_.assistantNotSignedInShort();
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

   interface AssistantCommandBinder
         extends CommandBinder<Commands, Assistant>
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

   private static final AssistantUIConstants constants_ = GWT.create(AssistantUIConstants.class);

   // Display names for assistant types
   public static final String DISPLAY_NAME_COPILOT = "GitHub Copilot";
   public static final String DISPLAY_NAME_POSIT = "Posit AI";
   public static final String DISPLAY_NAME_NONE = "None";
   public static final String DISPLAY_NAME_UNKNOWN = "Assistant";

   /**
    * Convert an assistant type (e.g., "copilot", "posit", "none") to a display name
    * (e.g., "GitHub Copilot", "Posit AI", "None").
    */
   public static String getDisplayName(String assistantType)
   {
      if (StringUtil.equals(assistantType, UserPrefsAccessor.ASSISTANT_COPILOT))
         return DISPLAY_NAME_COPILOT;
      else if (StringUtil.equals(assistantType, UserPrefsAccessor.ASSISTANT_POSIT))
         return DISPLAY_NAME_POSIT;
      else if (StringUtil.equals(assistantType, UserPrefsAccessor.ASSISTANT_NONE))
         return DISPLAY_NAME_NONE;
      else
         return DISPLAY_NAME_UNKNOWN;
   }
}
