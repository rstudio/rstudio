/*
 * AssistantPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.DialogOptions;
import org.rstudio.core.client.JSON;
import org.rstudio.core.client.SingleShotTimer;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DialogBuilder;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.dialog.WebDialogBuilderFactory;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.assistant.Assistant;
import org.rstudio.studio.client.workbench.assistant.model.AssistantConstants;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes;
import org.rstudio.studio.client.workbench.assistant.model.AssistantResponseTypes.AssistantStatusResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantRuntimeStatusChangedEvent;
import org.rstudio.studio.client.workbench.assistant.server.AssistantServerOperations;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessorConstants;
import org.rstudio.studio.client.workbench.views.chat.PaiUtil;
import org.rstudio.studio.client.workbench.views.chat.PositAiInstallManager;
import org.rstudio.studio.client.workbench.views.chat.server.ChatServerOperations;
import org.rstudio.studio.client.workbench.views.chat.server.ChatServerOperations.ChatVerifyInstalledResponse;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.FontStyle;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;


public class AssistantPreferencesPane extends PreferencesPane
{
   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      // Save assistant selection and sync deprecated copilot_enabled preference
      String selectedAssistant = selAssistant_.getValue();
      prefs.assistant().setGlobalValue(selectedAssistant);
      prefs.copilotEnabled().setGlobalValue(
            selectedAssistant.equals(UserPrefsAccessor.ASSISTANT_COPILOT));

      prefs.copilotTabKeyBehavior().setGlobalValue(selAssistantTabKeyBehavior_.getValue());
      prefs.copilotCompletionsTrigger().setGlobalValue(selAssistantCompletionsTrigger_.getValue());
      prefs.chatProvider().setGlobalValue(selChatProvider_.getValue());

      RestartRequirement requirement = super.onApply(prefs);
      if (initialCopilotWorkspaceEnabled_ != prefs.copilotProjectWorkspace().getGlobalValue())
         requirement.setSessionRestartRequired(true);

      return requirement;
   }

   @Inject
   public AssistantPreferencesPane(EventBus events,
                                 Session session,
                                 UserPrefs prefs,
                                 Commands commands,
                                 AriaLiveService ariaLive,
                                 Assistant assistant,
                                 AssistantServerOperations server,
                                 ProjectsServerOperations projectServer,
                                 GlobalDisplay globalDisplay,
                                 PaiUtil paiUtil,
                                 ChatServerOperations chatServer)
   {
      events_ = events;
      session_ = session;
      prefs_ = prefs;
      commands_ = commands;
      assistant_ = assistant;
      server_ = server;
      projectServer_ = projectServer;
      globalDisplay_ = globalDisplay;
      paiUtil_ = paiUtil;
      chatServer_ = chatServer;
      installManager_ = new PositAiInstallManager(chatServer);

      // Create assistant selector - conditionally include Posit AI option
      boolean paiEnabled = paiUtil_.isPaiEnabled();
      String[] assistantLabels;
      String[] assistantValues;
      if (paiEnabled)
      {
         assistantLabels = new String[] {
               prefsConstants_.assistantEnum_none(),
               prefsConstants_.assistantEnum_posit(),
               prefsConstants_.assistantEnum_copilot()
         };
         assistantValues = new String[] {
               UserPrefsAccessor.ASSISTANT_NONE,
               UserPrefsAccessor.ASSISTANT_POSIT,
               UserPrefsAccessor.ASSISTANT_COPILOT
         };
      }
      else
      {
         assistantLabels = new String[] {
               prefsConstants_.assistantEnum_none(),
               prefsConstants_.assistantEnum_copilot()
         };
         assistantValues = new String[] {
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

      lblAssistantStatus_ = new Label();
      lblAssistantStatus_.addStyleName(RES.styles().assistantStatusLabel());

      imgRefreshSpinner_ = new Image(CoreResources.INSTANCE.progress_gray());
      imgRefreshSpinner_.addStyleName(RES.styles().refreshSpinner());
      imgRefreshSpinner_.setVisible(false);

      statusButtons_ = new ArrayList<SmallButton>();
      
      btnShowError_ = new SmallButton(constants_.assistantShowErrorLabel());
      btnShowError_.addStyleName(RES.styles().button());
      statusButtons_.add(btnShowError_);

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

      btnInstall_ = new SmallButton(constants_.positAiInstallButton());
      btnInstall_.addStyleName(RES.styles().button());
      statusButtons_.add(btnInstall_);

      btnRefresh_ = new SmallButton(constants_.assistantRefreshLabel());
      btnRefresh_.addStyleName(RES.styles().button());
      statusButtons_.add(btnRefresh_);

      btnDiagnostics_ = new SmallButton(constants_.assistantDiagnosticsLabel());
      btnDiagnostics_.addStyleName(RES.styles().button());
      statusButtons_.add(btnDiagnostics_);

      btnProjectOptions_ = new SmallButton(constants_.assistantProjectOptionsLabel());
      btnProjectOptions_.addStyleName(RES.styles().button());
      statusButtons_.add(btnProjectOptions_);

      // Label for when project has overridden the assistant selection
      lblProjectOverride_ = new Label();
      lblProjectOverride_.getElement().getStyle().setFontStyle(FontStyle.ITALIC);

      cbAssistantShowMessages_ = checkboxPref(prefs_.assistantShowMessages(), true);
      selAssistantTabKeyBehavior_ = new SelectWidget(
            prefsConstants_.assistantTabKeyBehaviorTitle(),
            new String[] {
                  prefsConstants_.assistantTabKeyBehaviorEnum_suggestion(),
                  prefsConstants_.assistantTabKeyBehaviorEnum_completions()
            },
            new String[] {
                  UserPrefsAccessor.ASSISTANT_TAB_KEY_BEHAVIOR_SUGGESTION,
                  UserPrefsAccessor.ASSISTANT_TAB_KEY_BEHAVIOR_COMPLETIONS
            },
            false,
            true,
            false);

      selAssistantTabKeyBehavior_.setValue(prefs_.assistantTabKeyBehavior().getGlobalValue());

      selAssistantCompletionsTrigger_ = new SelectWidget(
            prefsConstants_.assistantCompletionsTriggerTitle(),
            new String[] {
                  prefsConstants_.assistantCompletionsTriggerEnum_auto(),
                  prefsConstants_.assistantCompletionsTriggerEnum_manual()
            },
            new String[] {
                  UserPrefsAccessor.ASSISTANT_COMPLETIONS_TRIGGER_AUTO,
                  UserPrefsAccessor.ASSISTANT_COMPLETIONS_TRIGGER_MANUAL
            },
            false,
            true,
            false);

      selAssistantCompletionsTrigger_.setValue(prefs_.assistantCompletionsTrigger().getGlobalValue());

      nvwAssistantCompletionsDelay_ = numericPref(
            constants_.assistantCompletionsDelayLabel(),
            10,
            5000,
            prefs_.assistantCompletionsDelay());

      cbAssistantNesEnabled_ = checkboxPref(prefs_.assistantNesEnabled(), true);
      cbAssistantNesAutoshow_ = checkboxPref(prefs_.assistantNesAutoshow(), true);

      // Create chat provider selector - conditionally include Posit AI option
      String[] chatProviderLabels;
      String[] chatProviderValues;
      if (paiEnabled)
      {
         chatProviderLabels = new String[] {
               prefsConstants_.chatProviderEnum_none(),
               prefsConstants_.chatProviderEnum_posit()
         };
         chatProviderValues = new String[] {
               UserPrefsAccessor.CHAT_PROVIDER_NONE,
               UserPrefsAccessor.CHAT_PROVIDER_POSIT
         };
      }
      else
      {
         chatProviderLabels = new String[] {
               prefsConstants_.chatProviderEnum_none()
         };
         chatProviderValues = new String[] {
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
      selChatProvider_.setValue(prefs_.chatProvider().getGlobalValue());

      linkCopilotTos_ = new HelpLink(
            constants_.copilotTermsOfServiceLinkLabel(),
            "github-copilot-terms-of-service",
            false);
      
      lblCopilotTos_ = new Label(constants_.copilotTermsOfServiceLabel());
      lblCopilotTos_.addStyleName(RES.styles().copilotTosLabel());

      assistantRuntimeStatusHandler_ = events_.addHandler(AssistantRuntimeStatusChangedEvent.TYPE, (event) ->
      {
         assistantStarted_ = event.getStatus() == AssistantRuntimeStatusChangedEvent.RUNNING;
      });

      projectOptionsChangedHandler_ = events_.addHandler(ProjectOptionsChangedEvent.TYPE, (event) ->
      {
         onProjectOptionsChanged(event.getData());
      });
   }

   @Override
   public void onUnload()
   {
      if (assistantRuntimeStatusHandler_ != null)
      {
         assistantRuntimeStatusHandler_.removeHandler();
         assistantRuntimeStatusHandler_ = null;
      }
      if (projectOptionsChangedHandler_ != null)
      {
         projectOptionsChangedHandler_.removeHandler();
         projectOptionsChangedHandler_ = null;
      }
      super.onUnload();
   }
   
   private void initDisplay()
   {
      // Chat section (displayed first)
      add(headerLabel(constants_.assistantChatTab()));
      add(selChatProvider_);

      // Add change handler for chat provider to check for Posit AI installation
      selChatProvider_.addChangeHandler((event) ->
      {
         String value = selChatProvider_.getValue();
         if (value.equals(UserPrefsAccessor.CHAT_PROVIDER_POSIT))
         {
            // First check if Posit AI is installed
            chatServer_.chatVerifyInstalled(new ServerRequestCallback<ChatVerifyInstalledResponse>()
            {
               @Override
               public void onResponseReceived(ChatVerifyInstalledResponse result)
               {
                  if (!result.installed)
                  {
                     // Offer to install Posit AI
                     checkPositAiInstallation(/* forAssistant= */ false);
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  // On error, still try to check for updates which will handle the install prompt
                  checkPositAiInstallation(/* forAssistant= */ false);
               }
            });
         }
      });

      // Completions section
      add(spacedBefore(headerLabel(constants_.assistantCompletionsTab())));

      // Add assistant selector
      add(selAssistant_);

      // Create project override panel (shown when project has a specific assistant configured)
      projectOverridePanel_ = new HorizontalPanel();
      projectOverridePanel_.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
      projectOverridePanel_.add(lblProjectOverride_);
      SmallButton btnOpenProjectOptions = new SmallButton(constants_.assistantProjectOptionsLabel());
      btnOpenProjectOptions.addClickHandler((event) -> commands_.projectOptions().execute());
      btnOpenProjectOptions.getElement().getStyle().setMarginLeft(8, Unit.PX);
      projectOverridePanel_.add(btnOpenProjectOptions);
      projectOverridePanel_.setVisible(false);

      // Create the status panel (shared between Copilot and Posit AI)
      statusPanel_ = createStatusPanel();

      // Create the common settings panel (shared between Copilot and Posit AI)
      commonSettingsPanel_ = createCommonSettingsPanel();

      // Create Copilot-specific "Other" panel
      copilotOtherPanel_ = createCopilotOtherPanel();

      // Create the three panels
      nonePanel_ = createNonePanel();
      positAiPanel_ = createPositAiPanel();
      copilotPanel_ = createCopilotPanel();

      // Add container for dynamic content
      add(assistantDetailsPanel_);

      // Create Copilot Terms of Service panel at the bottom (absolute positioning)
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
            if (value.equals(UserPrefsAccessor.ASSISTANT_NONE))
            {
               // Insert project override panel at the top of nonePanel_ if there's a project override
               nonePanel_.insert(spaced(projectOverridePanel_), 0);
               assistantDetailsPanel_.setWidget(nonePanel_);
               copilotTosPanel_.setVisible(false);
               disableCopilot(UserPrefsAccessor.ASSISTANT_NONE);
               positAiRefreshed_ = false;
            }
            else if (value.equals(UserPrefsAccessor.ASSISTANT_POSIT))
            {
               // Move status panel, project override panel, and common settings panel to Posit AI panel
               positAiPanel_.insert(spaced(statusPanel_), 0);
               positAiPanel_.insert(spaced(projectOverridePanel_), 1);
               positAiPanel_.add(commonSettingsPanel_);
               assistantDetailsPanel_.setWidget(positAiPanel_);
               copilotTosPanel_.setVisible(false);
               disableCopilot(UserPrefsAccessor.ASSISTANT_POSIT);

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
                           if (isInstalled || event == null)
                           {
                              refresh(UserPrefsAccessor.ASSISTANT_POSIT);
                           }
                           else
                           {
                              // Offer to install Posit AI (only if user changed the selection)
                              checkPositAiInstallation(/* forAssistant= */ true);
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
               // Move status panel, project override panel, common settings panel and Copilot-specific "Other" panel to Copilot panel
               if (session_.getSessionInfo().getCopilotEnabled())
               {
                  copilotPanel_.insert(spaced(statusPanel_), 0);
                  copilotPanel_.insert(spaced(projectOverridePanel_), 1);
               }
               else
               {
                  copilotPanel_.insert(spaced(projectOverridePanel_), 0);
               }
               copilotPanel_.add(commonSettingsPanel_);
               copilotPanel_.add(copilotOtherPanel_);
               assistantDetailsPanel_.setWidget(copilotPanel_);
               copilotTosPanel_.setVisible(true);
               positAiRefreshed_ = false;

               // Refresh Copilot status when panel is shown
               if (!copilotRefreshed_)
               {
                  copilotRefreshed_ = true;

                  // Check if Copilot is installed (passing assistantType so backend knows
                  // which language server to check, even if preference isn't saved yet)
                  server_.assistantVerifyInstalled(
                     UserPrefsAccessor.ASSISTANT_COPILOT,
                     new ServerRequestCallback<Boolean>()
                     {
                        @Override
                        public void onResponseReceived(Boolean isInstalled)
                        {
                           if (isInstalled)
                           {
                              // Copilot is installed - refresh status by passing assistantType
                              // so the backend can start the agent for the selected assistant
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
      panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
      panel.add(imgRefreshSpinner_);
      panel.setCellWidth(imgRefreshSpinner_, "24px");
      panel.add(lblAssistantStatus_);
      for (SmallButton button : statusButtons_)
         panel.add(button);
      return panel;
   }

   private VerticalPanel createPositAiPanel()
   {
      VerticalPanel panel = new VerticalPanel();
      // Status panel and common settings will be added dynamically
      return panel;
   }

   private VerticalPanel createCopilotPanel()
   {
      VerticalPanel panel = new VerticalPanel();

      if (!session_.getSessionInfo().getCopilotEnabled())
      {
         panel.add(new Label(constants_.copilotDisabledByAdmin()));
      }
      // Status panel, common settings and "Other" section will be added dynamically

      return panel;
   }

   private VerticalPanel createCopilotOtherPanel()
   {
      VerticalPanel panel = new VerticalPanel();
      panel.add(spacedBefore(headerLabel(constants_.otherCaption())));
      panel.add(cbAssistantShowMessages_);
      return panel;
   }

   private VerticalPanel createCommonSettingsPanel()
   {
      VerticalPanel panel = new VerticalPanel();

      // Completions section
      panel.add(spacedBefore(headerLabel(constants_.assistantCompletionsHeader())));
      panel.add(selAssistantCompletionsTrigger_);
      panel.add(nvwAssistantCompletionsDelay_);

      // Suggestions section (Next Edit Suggestions)
      panel.add(spacedBefore(headerLabel(constants_.assistantSuggestionsHeader())));
      panel.add(cbAssistantNesEnabled_);
      panel.add(cbAssistantNesAutoshow_);

      String modifier = BrowseCap.isMacintosh() ? "Cmd" : "Ctrl";
      Label lblNesShortcutHint = new Label(constants_.assistantSuggestionsShortcutHint(modifier));
      lblNesShortcutHint.getElement().getStyle().setFontStyle(FontStyle.ITALIC);
      panel.add(spaced(lblNesShortcutHint));

      return panel;
   }
   
   private void initModel()
   {
      selAssistantCompletionsTrigger_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            String value = selAssistantCompletionsTrigger_.getValue();
            if (value == UserPrefsAccessor.ASSISTANT_COMPLETIONS_TRIGGER_AUTO)
            {
               nvwAssistantCompletionsDelay_.setVisible(true);
            }
            else
            {
               nvwAssistantCompletionsDelay_.setVisible(false);
            }
         }
      });
      
      btnShowError_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            // Prefer using a web dialog even on Desktop, as we want to allow customization
            // of how the UI is presented. In particular, we want to allow users to select
            // and copy text if they need to.
            DialogOptions options = new DialogOptions();
            options.width = "auto";
            options.height = "auto";
            options.userSelect = "text";
            
            WebDialogBuilderFactory builder = GWT.create(WebDialogBuilderFactory.class);
            DialogBuilder dialog = builder.create(
                  GlobalDisplay.MSG_INFO,
                  constants_.assistantStatusDialogCaption(),
                  assistantStartupError_,
                  options);
            
            dialog.showModal();
         }
      });
      
      btnSignIn_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            String selectedType = selAssistant_.getValue();
            assistant_.signIn(selectedType, (response) -> refresh(selectedType));
         }
      });

      btnSignOut_.addClickHandler(new ClickHandler()
      {

         @Override
         public void onClick(ClickEvent event)
         {
            String selectedType = selAssistant_.getValue();
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
            refresh(selAssistant_.getValue());
         }
      });

      btnDiagnostics_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ProgressIndicator indicator = getProgressIndicator();
            indicator.onProgress(constants_.assistantDiagnosticReportProgressLabel());
            assistant_.showDiagnostics(selAssistant_.getValue(), () ->
            {
               indicator.onCompleted();
            });
         }
      });
      
      btnProjectOptions_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            commands_.projectOptions().execute();
         }
      });

      btnInstall_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            checkPositAiInstallation(/* forAssistant= */ true);
         }
      });
   }

   private void refresh(String assistantType)
   {
      imgRefreshSpinner_.setVisible(true);
      reset();

      // Use overloaded method to pass assistantType if provided
      ServerRequestCallback<AssistantStatusResponse> callback = new ServerRequestCallback<AssistantStatusResponse>()
      {
         @Override
         public void onResponseReceived(AssistantStatusResponse response)
         {
            imgRefreshSpinner_.setVisible(false);
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
                  if (!StringUtil.isNullOrEmpty(response.output))
                  {
                     assistantStartupError_ = response.output;
                     showButtons(btnShowError_, btnRefresh_, btnDiagnostics_);
                  }
                  else
                  {
                     showButtons(btnRefresh_, btnDiagnostics_);
                  }
               }
               else if (AssistantResponseTypes.AssistantAgentNotRunningReason.isError(response.reason))
               {
                  int reason = (int) response.reason.valueOf();
                  lblAssistantStatus_.setText(AssistantResponseTypes.AssistantAgentNotRunningReason.reasonToString(reason, Assistant.getDisplayName(assistantType)));

                  // Show Install button for Posit AI when not installed
                  if (reason == AssistantResponseTypes.AssistantAgentNotRunningReason.NotInstalled &&
                      assistantType.equals(UserPrefsAccessor.ASSISTANT_POSIT))
                  {
                     showButtons(btnInstall_, btnRefresh_);
                  }
                  else
                  {
                     showButtons(btnRefresh_, btnDiagnostics_);
                  }
               }
               else if (projectOptions_ != null &&
                        UserPrefsAccessor.ASSISTANT_NONE.equals(projectOptions_.getAssistantOptions().assistant))
               {
                  lblAssistantStatus_.setText(constants_.assistantDisabledInProject(Assistant.getDisplayName(assistantType)));
                  showButtons(btnProjectOptions_);
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
            hideButtons();
            lblAssistantStatus_.setText(constants_.assistantUnexpectedError());
            showButtons(btnRefresh_, btnDiagnostics_);
         }
      };

      // If no assistantType specified, use current preference
      String type = assistantType.isEmpty() ? prefs_.assistant().getGlobalValue() : assistantType;
      server_.assistantStatus(type, callback);
   }

   private void disableCopilot(String newAssistant)
   {
      // Eagerly disable Copilot so the agent stops immediately
      if (prefs_.copilotEnabled().getValue())
      {
         prefs_.copilotEnabled().setGlobalValue(false);
         prefs_.assistant().setGlobalValue(newAssistant);
         prefs_.writeUserPrefs((completed) -> {});
         copilotRefreshed_ = false;
      }
   }

   /**
    * Checks if Posit AI needs to be installed and prompts the user to install it.
    *
    * @param forAssistant True if this check is for the assistant (completions) preference,
    *                     false if it's for the chat provider preference.
    */
   private void checkPositAiInstallation(boolean forAssistant)
   {
      // Remember the previous value so we can revert if user declines
      final String previousAssistantValue = forAssistant ?
         prefs_.assistant().getGlobalValue() : null;
      final String previousChatProviderValue = !forAssistant ?
         prefs_.chatProvider().getGlobalValue() : null;

      installManager_.checkForUpdates(new PositAiInstallManager.UpdateCheckCallback()
      {
         @Override
         public void onNoUpdateAvailable()
         {
            // Posit AI is already installed and up-to-date
            if (forAssistant)
            {
               refresh(UserPrefsAccessor.ASSISTANT_POSIT);
            }
         }

         @Override
         public void onUpdateAvailable(String currentVersion, String newVersion, boolean isInitialInstall)
         {
            showInstallUpdatePrompt(newVersion, isInitialInstall, forAssistant,
               previousAssistantValue, previousChatProviderValue);
         }

         @Override
         public void onIncompatibleVersion()
         {
            // No compatible version available - show error and revert
            globalDisplay_.showErrorMessage(
               constants_.positAiIncompatibleTitle(),
               constants_.positAiIncompatibleMessage(),
               (Operation) () -> {
                  revertPositAiPreference(forAssistant, previousAssistantValue, previousChatProviderValue);
               });
         }

         @Override
         public void onCheckFailed(String errorMessage)
         {
            // Check failed - this often happens when calling from Preferences pane
            // before the preference is saved. Since we know Posit AI isn't installed
            // (we got here because assistantVerifyInstalled returned false, or user
            // just selected Posit AI), offer to install without version info.
            showInstallUpdatePrompt(null, true, forAssistant,
               previousAssistantValue, previousChatProviderValue);
         }
      });
   }

   /**
    * Shows the install/update prompt dialog.
    */
   private void showInstallUpdatePrompt(String newVersion, boolean isInitialInstall,
                                        boolean forAssistant,
                                        String previousAssistantValue,
                                        String previousChatProviderValue)
   {
      String title = isInitialInstall ?
         constants_.positAiInstallTitle() :
         constants_.positAiUpdateTitle();
      String message = isInitialInstall ?
         (newVersion != null ?
            constants_.positAiInstallMessage(newVersion) :
            constants_.positAiInstallMessageNoVersion()) :
         constants_.positAiUpdateMessage(newVersion);
      String yesLabel = isInitialInstall ?
         constants_.positAiInstallButton() :
         constants_.positAiUpdateButton();

      globalDisplay_.showYesNoMessage(
         GlobalDisplay.MSG_QUESTION,
         title,
         message,
         false,  // includeCancel
         (Operation) () -> {
            // User chose to install/update
            performPositAiInstall(forAssistant, previousAssistantValue, previousChatProviderValue);
         },
         (Operation) () -> {
            // User declined - revert the preference
            revertPositAiPreference(forAssistant, previousAssistantValue, previousChatProviderValue);
         },
         null,  // cancelOperation - not used since includeCancel is false
         yesLabel,
         constants_.positAiCancelButton(),
         true);  // yesIsDefault
   }

   /**
    * Performs the Posit AI installation with progress dialog.
    */
   private void performPositAiInstall(boolean forAssistant,
                                      String previousAssistantValue,
                                      String previousChatProviderValue)
   {
      // Save the appropriate preference first - the server requires either
      // chatProvider or assistant to be set to "posit" before it will allow installation
      if (forAssistant)
      {
         prefs_.assistant().setGlobalValue(UserPrefsAccessor.ASSISTANT_POSIT);
      }
      else
      {
         prefs_.chatProvider().setGlobalValue(UserPrefsAccessor.CHAT_PROVIDER_POSIT);
      }

      // Write prefs and then start installation
      prefs_.writeUserPrefs((completed) -> {
         doInstall(forAssistant, previousAssistantValue, previousChatProviderValue);
      });
   }

   /**
    * Actually performs the installation after preferences are saved.
    */
   private void doInstall(boolean forAssistant,
                          String previousAssistantValue,
                          String previousChatProviderValue)
   {
      final com.google.gwt.user.client.Command dismissProgress =
         globalDisplay_.showProgress(constants_.positAiInstallingMessage());

      installManager_.installUpdate(new PositAiInstallManager.InstallCallback()
      {
         @Override
         public void onInstallStarted()
         {
            // Progress dialog is already showing
         }

         @Override
         public void onInstallProgress(String status)
         {
            // Progress dialog shows a generic message; no additional status updates
         }

         @Override
         public void onInstallComplete()
         {
            dismissProgress.execute();
            globalDisplay_.showMessage(
               GlobalDisplay.MSG_INFO,
               constants_.positAiInstallCompleteTitle(),
               constants_.positAiInstallCompleteMessage(),
               (Operation) () -> {
                  // Refresh the assistant status if this was for the completions pref
                  if (forAssistant)
                  {
                     positAiRefreshed_ = false;
                     refresh(UserPrefsAccessor.ASSISTANT_POSIT);
                  }
               });
         }

         @Override
         public void onInstallFailed(String errorMessage)
         {
            dismissProgress.execute();

            globalDisplay_.showErrorMessage(
               constants_.positAiInstallFailedTitle(),
               constants_.positAiInstallFailedMessage(errorMessage),
               (Operation) () -> {
                  // Revert the preference since installation failed
                  revertPositAiPreference(forAssistant, previousAssistantValue, previousChatProviderValue);
               });
         }
      });
   }

   /**
    * Reverts the Posit AI preference to its previous value.
    */
   private void revertPositAiPreference(boolean forAssistant,
                                        String previousAssistantValue,
                                        String previousChatProviderValue)
   {
      if (forAssistant)
      {
         // Revert assistant preference to previous value
         String revertTo = previousAssistantValue != null
            ? previousAssistantValue
            : UserPrefsAccessor.ASSISTANT_NONE;

         if (revertTo.equals(selAssistant_.getValue()))
            return;

         selAssistant_.setValue(revertTo);
         prefs_.assistant().setGlobalValue(revertTo);
         positAiRefreshed_ = false;

         // Write the reverted preference
         prefs_.writeUserPrefs((completed) -> {});

         // Trigger the change handler to update the UI
         selAssistant_.getListBox().fireEvent(new ChangeEvent() {});
      }
      else
      {
         // Revert chat provider preference to previous value
         String revertTo = previousChatProviderValue != null
            ?  previousChatProviderValue
            : UserPrefsAccessor.CHAT_PROVIDER_NONE;
         
         if (revertTo.equals(selChatProvider_.getValue()))
            return;

         selChatProvider_.setValue(revertTo);
         prefs_.chatProvider().setGlobalValue(revertTo);

         // Write the reverted preference
         prefs_.writeUserPrefs((completed) -> {});
      }
   }

   private void reset()
   {
      assistantStartupError_ = null;
      hideButtons();
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

   @Override
   protected void initialize(UserPrefs prefs)
   {
      // Migration: if rstudio_assistant is "none" but copilot_enabled is true, auto-migrate to "copilot"
      String assistant = prefs.assistant().getGlobalValue();
      if (assistant.equals(UserPrefsAccessor.ASSISTANT_NONE) &&
          prefs.copilotEnabled().getGlobalValue())
      {
         prefs.assistant().setGlobalValue(UserPrefsAccessor.ASSISTANT_COPILOT);
         selAssistant_.setValue(UserPrefsAccessor.ASSISTANT_COPILOT);
      }

      // Reset to "none" if user has Posit AI selected but PAI is no longer enabled
      if (assistant.equals(UserPrefsAccessor.ASSISTANT_POSIT) &&
          !paiUtil_.isPaiEnabled())
      {
         prefs.assistant().setGlobalValue(UserPrefsAccessor.ASSISTANT_NONE);
         selAssistant_.setValue(UserPrefsAccessor.ASSISTANT_NONE);
      }

      initialCopilotWorkspaceEnabled_ = prefs.copilotProjectWorkspace().getGlobalValue();
      projectServer_.readProjectOptions(new ServerRequestCallback<RProjectOptions>()
      {
         @Override
         public void onResponseReceived(RProjectOptions options)
         {
            projectOptions_ = options;
            init();
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            init();
         }
      });
   }
   
   private void init()
   {
      initDisplay();
      initModel();

      // Check if project has a specific assistant configured (overrides global setting)
      String projectAssistant = getProjectAssistant();
      if (projectAssistant != null)
      {
         // Project has overridden the assistant selection
         projectAssistantOverride_ = projectAssistant;

         // Use appropriate message for disabled vs configured
         if (projectAssistant.equals(UserPrefsAccessor.ASSISTANT_NONE))
            lblProjectOverride_.setText(constants_.codeAssistantDisabledInProject());
         else
            lblProjectOverride_.setText(constants_.assistantConfiguredInProject(
               Assistant.getDisplayName(projectAssistant)));

         projectOverridePanel_.setVisible(true);

         // Disable the selector and set it to match project's assistant
         selAssistant_.setEnabled(false);
         selAssistant_.setValue(projectAssistant);

         // Trigger the change handler to update the displayed panel
         selAssistant_.getListBox().fireEvent(new ChangeEvent() {});
      }
   }

   /**
    * Returns the project-specific assistant if one is configured, or null if
    * the project uses the default (global) setting.
    */
   private String getProjectAssistant()
   {
      if (projectOptions_ == null)
         return null;

      String projectAssistant = projectOptions_.getAssistantOptions().assistant;

      // "default", null, or empty means use global setting
      if (projectAssistant == null ||
          projectAssistant.isEmpty() ||
          projectAssistant.equals("default"))
      {
         return null;
      }

      return projectAssistant;
   }

   /**
    * Called when project options are changed (e.g., from the Project Options dialog).
    * Updates the UI to reflect any changes to the project's assistant setting.
    */
   private void onProjectOptionsChanged(RProjectOptions options)
   {
      // Update our cached project options
      projectOptions_ = options;

      // Re-check if there's a project override
      String projectAssistant = getProjectAssistant();

      if (projectAssistant != null)
      {
         // Project has a specific assistant configured
         projectAssistantOverride_ = projectAssistant;

         // Use appropriate message for disabled vs configured
         if (projectAssistant.equals(UserPrefsAccessor.ASSISTANT_NONE))
            lblProjectOverride_.setText(constants_.codeAssistantDisabledInProject());
         else
            lblProjectOverride_.setText(constants_.assistantConfiguredInProject(
               Assistant.getDisplayName(projectAssistant)));

         projectOverridePanel_.setVisible(true);
         selAssistant_.setEnabled(false);
         selAssistant_.setValue(projectAssistant);

         // Reset refresh flags so status gets refreshed for the new assistant
         copilotRefreshed_ = false;
         positAiRefreshed_ = false;

         // Trigger the change handler to update the displayed panel
         selAssistant_.getListBox().fireEvent(new ChangeEvent() {});
      }
      else
      {
         // Project is using global default
         projectAssistantOverride_ = null;
         projectOverridePanel_.setVisible(false);
         selAssistant_.setEnabled(true);

         // Restore selector to global preference value
         selAssistant_.setValue(prefs_.assistant().getGlobalValue());

         // Reset refresh flags
         copilotRefreshed_ = false;
         positAiRefreshed_ = false;

         // Trigger the change handler to update the displayed panel
         selAssistant_.getListBox().fireEvent(new ChangeEvent() {});
      }
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
   
   public interface Styles extends CssResource
   {
      String button();
      String assistantStatusLabel();
      String copilotTosLabel();
      String refreshSpinner();
   }

   public interface Resources extends ClientBundle
   {
      @Source("AssistantPreferencesPane.css")
      Styles styles();
   }

   public static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }
   
   // State
   private String assistantStartupError_;
   private boolean initialCopilotWorkspaceEnabled_;
   private HandlerRegistration assistantRuntimeStatusHandler_;
   private HandlerRegistration projectOptionsChangedHandler_;
   private boolean assistantStarted_ = false; // did Copilot get started while the dialog was open?
   private boolean copilotRefreshed_ = false; // has Copilot status been refreshed for this pane instance?
   private boolean positAiRefreshed_ = false; // has Posit AI status been refreshed for this pane instance?
   private RProjectOptions projectOptions_;
   private String projectAssistantOverride_; // non-null when project has overridden assistant

   // Assistant panels (created in initDisplay)
   private VerticalPanel nonePanel_;
   private VerticalPanel positAiPanel_;
   private VerticalPanel copilotPanel_;
   private VerticalPanel copilotTosPanel_;
   private VerticalPanel commonSettingsPanel_;
   private VerticalPanel copilotOtherPanel_;
   private HorizontalPanel statusPanel_;
   private HorizontalPanel projectOverridePanel_;

   // UI
   private final SelectWidget selAssistant_;
   private final SimplePanel assistantDetailsPanel_;
   private final Label lblAssistantStatus_;
   private final Image imgRefreshSpinner_;
   private final CheckBox cbAssistantShowMessages_;
   private final CheckBox cbAssistantNesEnabled_;
   private final CheckBox cbAssistantNesAutoshow_;
   private final List<SmallButton> statusButtons_;
   private final SmallButton btnShowError_;
   private final SmallButton btnSignIn_;
   private final SmallButton btnSignOut_;
   private final SmallButton btnActivate_;
   private final SmallButton btnRefresh_;
   private final SmallButton btnDiagnostics_;
   private final SmallButton btnProjectOptions_;
   private final SmallButton btnInstall_;
   private final NumericValueWidget nvwAssistantCompletionsDelay_;
   private final SelectWidget selAssistantTabKeyBehavior_;
   private final SelectWidget selAssistantCompletionsTrigger_;
   private final SelectWidget selChatProvider_;
   private final HelpLink linkCopilotTos_;
   private final Label lblCopilotTos_;
   private final Label lblProjectOverride_;

   // Injected
   private final EventBus events_;
   private final Session session_;
   private final UserPrefs prefs_;
   private final Commands commands_;
   private final Assistant assistant_;
   private final AssistantServerOperations server_;
   private final ProjectsServerOperations projectServer_;
   private final GlobalDisplay globalDisplay_;
   private final PaiUtil paiUtil_;
   private final ChatServerOperations chatServer_;
   private final PositAiInstallManager installManager_;
   
   private static final UserPrefsAccessorConstants prefsConstants_ = GWT.create(UserPrefsAccessorConstants.class);
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
   
}
