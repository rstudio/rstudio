/*
 * ProjectAssistantPreferencesPane.java
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
package org.rstudio.studio.client.projects.ui.prefs.buildtools;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JSON;
import org.rstudio.core.client.SingleShotTimer;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectAssistantOptions;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesPane;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.assistant.Assistant;
import org.rstudio.studio.client.workbench.assistant.model.AssistantConstants;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantStatusResponse;
import org.rstudio.studio.client.workbench.assistant.server.AssistantServerOperations;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessorConstants;
import org.rstudio.studio.client.workbench.prefs.views.AssistantPreferencesPane;
import org.rstudio.studio.client.workbench.views.chat.PaiUtil;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ProjectAssistantPreferencesPane extends ProjectPreferencesPane
{
   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      RProjectAssistantOptions assistantOptions = options.getAssistantOptions();

      // Save assistant selection
      String selectedAssistant = selAssistant_.getValue();
      assistantOptions.assistant = selectedAssistant;

      // Save chat provider selection
      String selectedChatProvider = selChatProvider_.getValue();
      assistantOptions.chat_provider = selectedChatProvider;

      RestartRequirement requirement = new RestartRequirement();

      // Check if assistant changed
      String originalAssistant = options_.getAssistantOptions().assistant;
      if (originalAssistant == null)
         originalAssistant = "default";
      if (!originalAssistant.equals(assistantOptions.assistant))
         requirement.setSessionRestartRequired(true);

      // Check if chat provider changed
      String originalChatProvider = options_.getAssistantOptions().chat_provider;
      if (originalChatProvider == null)
         originalChatProvider = "default";
      if (!originalChatProvider.equals(assistantOptions.chat_provider))
         requirement.setSessionRestartRequired(true);

      return requirement;
   }

   @Inject
   public ProjectAssistantPreferencesPane(EventBus events,
                                        Session session,
                                        UserPrefs prefs,
                                        AriaLiveService ariaLive,
                                        Assistant assistant,
                                        AssistantServerOperations server,
                                        ProjectsServerOperations projectServer,
                                        PaiUtil paiUtil)
   {
      events_ = events;
      session_ = session;
      prefs_ = prefs;
      assistant_ = assistant;
      server_ = server;
      projectServer_ = projectServer;
      paiUtil_ = paiUtil;

      // Create assistant selector - conditionally include Posit AI option
      boolean paiEnabled = paiUtil_.isPaiEnabled();
      String[] assistantLabels;
      String[] assistantValues;
      if (paiEnabled)
      {
         assistantLabels = new String[] {
               constants_.defaultInParentheses(),
               constants_.none(),
               prefsConstants_.assistantEnum_posit(),
               prefsConstants_.assistantEnum_copilot()
         };
         assistantValues = new String[] {
               ASSISTANT_DEFAULT,
               UserPrefsAccessor.ASSISTANT_NONE,
               UserPrefsAccessor.ASSISTANT_POSIT,
               UserPrefsAccessor.ASSISTANT_COPILOT
         };
      }
      else
      {
         assistantLabels = new String[] {
               constants_.defaultInParentheses(),
               constants_.none(),
               prefsConstants_.assistantEnum_copilot()
         };
         assistantValues = new String[] {
               ASSISTANT_DEFAULT,
               UserPrefsAccessor.ASSISTANT_NONE,
               UserPrefsAccessor.ASSISTANT_COPILOT
         };
      }
      selAssistant_ = new SelectWidget(
            constants_.assistantSelectLabel(),
            assistantLabels,
            assistantValues,
            false,
            true,
            false);
      selAssistant_.setValue(prefs_.assistant().getGlobalValue());

      // Container for dynamic assistant-specific content
      assistantDetailsPanel_ = new SimplePanel();

      lblAssistantStatus_ = new Label(constants_.assistantLoadingMessage());

      statusButtons_ = new ArrayList<SmallButton>();

      btnSignIn_ = new SmallButton(constants_.assistantSignInLabel());
      btnSignIn_.addStyleName(RES.styles().button());
      statusButtons_.add(btnSignIn_);

      btnSignOut_ = new SmallButton(constants_.assistantSignOutLabel());
      btnSignOut_.addStyleName(RES.styles().button());
      statusButtons_.add(btnSignOut_);

      btnActivate_ = new SmallButton(constants_.copilotActivateLabel());
      Roles.getLinkRole().set(btnActivate_.getElement());
      btnActivate_.getElement().setPropertyString("href", "https://github.com/settings/copilot");
      btnActivate_.addStyleName(RES.styles().button());
      statusButtons_.add(btnActivate_);

      btnRefresh_ = new SmallButton(constants_.assistantRefreshLabel());
      btnRefresh_.addStyleName(RES.styles().button());
      statusButtons_.add(btnRefresh_);

      btnDiagnostics_ = new SmallButton(constants_.assistantDiagnosticsLabel());
      btnDiagnostics_.addStyleName(RES.styles().button());
      statusButtons_.add(btnDiagnostics_);

      linkCopilotTos_ = new HelpLink(
            constants_.copilotTermsOfServiceLinkLabel(),
            "github-copilot-terms-of-service",
            false);

      lblCopilotTos_ = new Label(constants_.copilotTermsOfServiceLabel());
      lblCopilotTos_.addStyleName(RES.styles().copilotTosLabel());

      // Create chat provider selector - conditionally include Posit AI option
      // Note: Use "(Default)" instead of "(None)" since project settings inherit from global
      String[] chatProviderLabels;
      String[] chatProviderValues;
      if (paiEnabled)
      {
         chatProviderLabels = new String[] {
               constants_.defaultInParentheses(),
               constants_.none(),
               prefsConstants_.chatProviderEnum_posit()
         };
         chatProviderValues = new String[] {
               CHAT_PROVIDER_DEFAULT,
               UserPrefsAccessor.CHAT_PROVIDER_NONE,
               UserPrefsAccessor.CHAT_PROVIDER_POSIT
         };
      }
      else
      {
         chatProviderLabels = new String[] {
               constants_.defaultInParentheses(),
               constants_.none()
         };
         chatProviderValues = new String[] {
               CHAT_PROVIDER_DEFAULT,
               UserPrefsAccessor.CHAT_PROVIDER_NONE
         };
      }
      selChatProvider_ = new SelectWidget(
            constants_.assistantChatProviderLabel(),
            chatProviderLabels,
            chatProviderValues,
            false,
            true,
            false);
      selChatProvider_.setValue(CHAT_PROVIDER_DEFAULT);
   }

   @Override
   public void onUnload()
   {
      if (assistantRuntimeStatusHandler_ != null)
      {
         assistantRuntimeStatusHandler_.removeHandler();
         assistantRuntimeStatusHandler_ = null;
      }
      super.onUnload();
   }
   
   private void initDisplay(RProjectOptions options)
   {
      // Chat section (displayed first)
      add(headerLabel(constants_.assistantChatTab()));
      add(selChatProvider_);

      // Add info label showing global chat provider when using default
      chatProviderInfoPanel_ = createChatProviderInfoPanel();
      add(chatProviderInfoPanel_);

      // Set up change handler for chat provider to show/hide info panel
      selChatProvider_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            String value = selChatProvider_.getValue();
            chatProviderInfoPanel_.setVisible(value.equals(CHAT_PROVIDER_DEFAULT));
         }
      });

      // Set initial visibility based on current value
      chatProviderInfoPanel_.setVisible(selChatProvider_.getValue().equals(CHAT_PROVIDER_DEFAULT));

      // Completions section
      add(spacedBefore(headerLabel(constants_.assistantCompletionsTab())));

      // Add assistant selector
      add(selAssistant_);

      // Create the status panel (shared between Copilot and Posit AI)
      statusPanel_ = createStatusPanel();

      // Create the assistant detail panels
      defaultPanel_ = createDefaultPanel();
      nonePanel_ = createNonePanel();
      positAiPanel_ = createPositAiPanel();
      copilotPanel_ = createCopilotPanel(options);

      // Add container for dynamic content
      add(assistantDetailsPanel_);

      // Add Copilot Terms of Service panel at the bottom (absolute positioning)
      copilotTosPanel_ = new VerticalPanel();
      copilotTosPanel_.getElement().getStyle().setBottom(0, Unit.PX);
      copilotTosPanel_.getElement().getStyle().setPosition(Position.ABSOLUTE);
      copilotTosPanel_.add(spaced(lblCopilotTos_));
      copilotTosPanel_.add(spaced(linkCopilotTos_));
      add(copilotTosPanel_);

      // Set up panel swapping based on assistant selection
      ChangeHandler assistantChangedHandler = new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            String value = selAssistant_.getValue();
            if (value.equals(ASSISTANT_DEFAULT))
            {
               assistantDetailsPanel_.setWidget(defaultPanel_);
               copilotTosPanel_.setVisible(false);
               positAiRefreshed_ = false;
               copilotRefreshed_ = false;
            }
            else if (value.equals(UserPrefsAccessor.ASSISTANT_NONE))
            {
               assistantDetailsPanel_.setWidget(nonePanel_);
               copilotTosPanel_.setVisible(false);
               positAiRefreshed_ = false;
               copilotRefreshed_ = false;
            }
            else if (value.equals(UserPrefsAccessor.ASSISTANT_POSIT))
            {
               // Move status panel to Posit AI panel
               positAiPanel_.insert(spaced(statusPanel_), 0);
               assistantDetailsPanel_.setWidget(positAiPanel_);
               copilotTosPanel_.setVisible(false);
               copilotRefreshed_ = false;

               // Refresh Posit AI status when panel is shown
               if (!positAiRefreshed_)
               {
                  positAiRefreshed_ = true;

                  // Check if Posit AI is installed
                  server_.assistantVerifyInstalled(
                     UserPrefsAccessor.ASSISTANT_POSIT,
                     new ServerRequestCallback<Boolean>()
                     {
                        @Override
                        public void onResponseReceived(Boolean isInstalled)
                        {
                           if (isInstalled)
                           {
                              refresh(UserPrefsAccessor.ASSISTANT_POSIT);
                           }
                           else
                           {
                              lblAssistantStatus_.setText(constants_.assistantAgentNotEnabled());
                           }
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                           lblAssistantStatus_.setText(constants_.assistantStartupError());
                        }
                     });
               }
            }
            else if (value.equals(UserPrefsAccessor.ASSISTANT_COPILOT))
            {
               // Move status panel to Copilot panel
               if (session_.getSessionInfo().getCopilotEnabled())
               {
                  copilotPanel_.insert(spaced(statusPanel_), 0);
               }
               assistantDetailsPanel_.setWidget(copilotPanel_);
               copilotTosPanel_.setVisible(true);
               positAiRefreshed_ = false;

               // Refresh Copilot status when panel is shown
               if (!copilotRefreshed_)
               {
                  copilotRefreshed_ = true;

                  // Check if Copilot is installed
                  server_.assistantVerifyInstalled(
                     UserPrefsAccessor.ASSISTANT_COPILOT,
                     new ServerRequestCallback<Boolean>()
                     {
                        @Override
                        public void onResponseReceived(Boolean isInstalled)
                        {
                           if (isInstalled)
                           {
                              refresh(UserPrefsAccessor.ASSISTANT_COPILOT);
                           }
                           else
                           {
                              lblAssistantStatus_.setText(constants_.assistantAgentNotEnabled());
                           }
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                           lblAssistantStatus_.setText(constants_.assistantStartupError());
                        }
                     });
               }
            }
         }
      };

      selAssistant_.addChangeHandler(assistantChangedHandler);
      assistantChangedHandler.onChange(null); // Initialize
   }

   private VerticalPanel createDefaultPanel()
   {
      VerticalPanel panel = new VerticalPanel();

      // Show what global assistant is currently active
      String globalAssistant = prefs_.assistant().getGlobalValue();
      String globalAssistantName;
      if (globalAssistant.equals(UserPrefsAccessor.ASSISTANT_COPILOT))
         globalAssistantName = prefsConstants_.assistantEnum_copilot();
      else if (globalAssistant.equals(UserPrefsAccessor.ASSISTANT_POSIT))
         globalAssistantName = prefsConstants_.assistantEnum_posit();
      else
         globalAssistantName = constants_.none();

      Label lblInfo = new Label(constants_.projectAssistantDefaultInfo(globalAssistantName));
      panel.add(spaced(lblInfo));
      return panel;
   }

   private VerticalPanel createNonePanel()
   {
      VerticalPanel panel = new VerticalPanel();
      Label lblInfo = new Label(constants_.assistantNoneInfo());
      panel.add(spaced(lblInfo));
      return panel;
   }

   private HorizontalPanel createStatusPanel()
   {
      HorizontalPanel panel = new HorizontalPanel();
      panel.add(lblAssistantStatus_);
      for (SmallButton button : statusButtons_)
         panel.add(button);
      return panel;
   }

   private VerticalPanel createPositAiPanel()
   {
      VerticalPanel panel = new VerticalPanel();
      // Status panel will be added dynamically when this assistant is selected
      return panel;
   }

   private VerticalPanel createCopilotPanel(RProjectOptions options)
   {
      VerticalPanel panel = new VerticalPanel();

      if (!session_.getSessionInfo().getCopilotEnabled())
      {
         panel.add(new Label(constants_.copilotDisabledByAdmin()));
      }
      // Status panel will be added dynamically when this assistant is selected

      return panel;
   }

   private VerticalPanel createChatProviderInfoPanel()
   {
      VerticalPanel panel = new VerticalPanel();

      // Show what global chat provider is currently active
      String globalChatProvider = prefs_.chatProvider().getGlobalValue();
      String globalChatProviderName;
      if (globalChatProvider.equals(UserPrefsAccessor.CHAT_PROVIDER_POSIT))
         globalChatProviderName = prefsConstants_.chatProviderEnum_posit();
      else
         globalChatProviderName = constants_.none();

      Label lblInfo = new Label(constants_.projectChatProviderDefaultInfo(globalChatProviderName));
      panel.add(spaced(lblInfo));
      return panel;
   }

   private void initModel()
   {
      btnSignIn_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            String selectedType = getSelectedAssistantType();
            assistant_.signIn(selectedType, (response) -> refresh(selectedType));
         }
      });

      btnSignOut_.addClickHandler(new ClickHandler()
      {

         @Override
         public void onClick(ClickEvent event)
         {
            String selectedType = getSelectedAssistantType();
            assistant_.signOut(selectedType, (response) -> refresh(selectedType));
         }
      });

      btnActivate_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            String href = btnActivate_.getElement().getPropertyString("href");
            Window.open(href, "_blank", "");
         }
      });

      btnRefresh_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            refresh(getSelectedAssistantType());
         }
      });
      
      btnDiagnostics_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ProgressIndicator indicator = getProgressIndicator();
            indicator.onProgress(constants_.assistantDiagnosticReportProgressLabel());
            assistant_.showDiagnostics(getSelectedAssistantType(), () ->
            {
               indicator.onCompleted();
            });
         }
      });
   }

   private String getSelectedAssistantType()
   {
      // For project settings, "none" means use default (global preference)
      // Map it to the actual global preference value
      String selected = selAssistant_.getValue();
      if (selected.equals(UserPrefsAccessor.ASSISTANT_NONE))
      {
         return prefs_.assistant().getGlobalValue();
      }
      return selected;
   }
   
   private void refresh(String assistantType)
   {
      hideButtons();

      // Use overloaded method to pass assistantType if provided
      ServerRequestCallback<AssistantStatusResponse> callback = new ServerRequestCallback<AssistantStatusResponse>()
      {
         @Override
         public void onResponseReceived(AssistantStatusResponse response)
         {
            hideButtons();

            if (response == null)
            {
               lblAssistantStatus_.setText(constants_.assistantUnexpectedError());
            }
            else if (response.result == null)
            {
               if (response.error != null && response.error.getCode() == AssistantConstants.ErrorCodes.AGENT_NOT_INITIALIZED)
               {
                  // Assistant still starting up, so wait a second and refresh again
                  SingleShotTimer.fire(1000, () -> {
                     refresh(assistantType);
                  });
               }
               else if (response.error != null && response.error.getCode() != AssistantConstants.ErrorCodes.AGENT_SHUT_DOWN)
               {
                  lblAssistantStatus_.setText(constants_.assistantStartupError());
                  showButtons(btnRefresh_, btnDiagnostics_);
               }
               else if (AssistantResponseTypes.AssistantAgentNotRunningReason.isError(response.reason))
               {
                  int reason = (int) response.reason.valueOf();
                  lblAssistantStatus_.setText(AssistantResponseTypes.AssistantAgentNotRunningReason.reasonToString(reason, Assistant.getDisplayName(assistantType)));
                  showButtons(btnRefresh_, btnDiagnostics_);
               }
               else
               {
                  lblAssistantStatus_.setText(constants_.assistantAgentNotRunning());
                  showButtons(btnSignIn_, btnRefresh_, btnDiagnostics_);
               }
            }
            else if (response.result.status == AssistantConstants.STATUS_OK ||
                     response.result.status == AssistantConstants.STATUS_ALREADY_SIGNED_IN)
            {
               showButtons(btnSignOut_, btnRefresh_, btnDiagnostics_);
               lblAssistantStatus_.setText(constants_.assistantSignedInAsLabel(response.result.user));
            }
            else if (response.result.status == AssistantConstants.STATUS_NOT_AUTHORIZED)
            {
               showButtons(btnActivate_, btnSignOut_, btnRefresh_, btnDiagnostics_);
               lblAssistantStatus_.setText(constants_.copilotAccountNotActivated(response.result.user));
            }
            else if (response.result.status == AssistantConstants.STATUS_NOT_SIGNED_IN)
            {
               showButtons(btnSignIn_, btnRefresh_, btnDiagnostics_);
               lblAssistantStatus_.setText(constants_.assistantNotSignedIn());
            }
            else
            {
               String message = constants_.assistantUnknownResponse(JSON.stringify(response));
               lblAssistantStatus_.setText(message);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      };

      // If no assistantType specified, use current preference
      String type = assistantType.isEmpty() ? prefs_.assistant().getGlobalValue() : assistantType;
      server_.assistantStatus(type, callback);
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      options_ = options;

      // Load assistant selection from project options
      String projectAssistant = options.getAssistantOptions().assistant;
      if (projectAssistant == null || projectAssistant.isEmpty())
         selAssistant_.setValue(ASSISTANT_DEFAULT);
      else
         selAssistant_.setValue(projectAssistant);

      // Load chat provider selection from project options
      String projectChatProvider = options.getAssistantOptions().chat_provider;
      if (projectChatProvider == null || projectChatProvider.isEmpty())
         selChatProvider_.setValue(CHAT_PROVIDER_DEFAULT);
      else
         selChatProvider_.setValue(projectChatProvider);

      initDisplay(options);
      initModel();

      // Note: Status refresh is now handled in the onChange handler
      // which is triggered by assistantChangedHandler.onChange(null) in initDisplay()
   }
   
   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconAssistant2x());
   }

   @Override
   public String getName()
   {
      return constants_.assistantPaneName();
   }

   private void hideButtons()
   {
      for (SmallButton button : statusButtons_)
      {
         button.setEnabled(false);
         button.setVisible(false);
      }
   }
   
   private void showButtons(SmallButton... buttons)
   {
      for (SmallButton button : buttons)
      {
         button.setEnabled(true);
         button.setVisible(true);
      }
   }
   
   // State
   private RProjectOptions options_;
   private HandlerRegistration assistantRuntimeStatusHandler_;
   private boolean copilotRefreshed_ = false;
   private boolean positAiRefreshed_ = false;

   // Assistant panels (created in initDisplay)
   private VerticalPanel defaultPanel_;
   private VerticalPanel nonePanel_;
   private VerticalPanel positAiPanel_;
   private VerticalPanel copilotPanel_;
   private VerticalPanel copilotTosPanel_;
   private HorizontalPanel statusPanel_;

   // UI
   private final SelectWidget selAssistant_;
   private final SelectWidget selChatProvider_;
   private final SimplePanel assistantDetailsPanel_;
   private final Label lblAssistantStatus_;
   private final List<SmallButton> statusButtons_;
   private final SmallButton btnSignIn_;
   private final SmallButton btnSignOut_;
   private final SmallButton btnActivate_;
   private final SmallButton btnRefresh_;
   private final SmallButton btnDiagnostics_;
   private final HelpLink linkCopilotTos_;
   private final Label lblCopilotTos_;
   private VerticalPanel chatProviderInfoPanel_;
   
   // Injected
   private final EventBus events_;
   private final Session session_;
   private final UserPrefs prefs_;
   private final Assistant assistant_;
   private final AssistantServerOperations server_;
   private final ProjectsServerOperations projectServer_;
   private final PaiUtil paiUtil_;
   
   private static final AssistantPreferencesPane.Resources RES = AssistantPreferencesPane.RES;
   private static final UserPrefsAccessorConstants prefsConstants_ = GWT.create(UserPrefsAccessorConstants.class);
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);

   // Internal constants for project-level "(Default)" options
   private static final String ASSISTANT_DEFAULT = "default";
   private static final String CHAT_PROVIDER_DEFAULT = "default";
}
