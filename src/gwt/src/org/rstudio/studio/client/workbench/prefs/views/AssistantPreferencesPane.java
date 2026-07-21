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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.DialogOptions;
import org.rstudio.core.client.JSON;
import org.rstudio.core.client.SingleShotTimer;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DialogBuilder;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.Spinner;
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.FontStyle;
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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;


public class AssistantPreferencesPane extends PreferencesPane
{
   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      // Update preferences
      String selectedAssistant = selAssistant_.getValue();
      prefs.assistant().setGlobalValue(selectedAssistant);
      prefs.chatProvider().setGlobalValue(selChatProvider_.getValue());
      prefs.assistantTabKeyBehavior().setGlobalValue(selAssistantTabKeyBehavior_.getValue());
      prefs.assistantCompletionsTrigger().setGlobalValue(selAssistantCompletionsTrigger_.getValue());

      // Invert the collapse checkbox to get autoshow value
      prefs.assistantNesAutoshow().setGlobalValue(!cbAssistantNesCollapse_.getValue());

      // Also sync (deprecated) Copilot settings for now
      prefs.copilotEnabled().setGlobalValue(
            selectedAssistant.equals(UserPrefsAccessor.ASSISTANT_COPILOT));
      prefs.copilotTabKeyBehavior().setGlobalValue(selAssistantTabKeyBehavior_.getValue());
      prefs.copilotCompletionsTrigger().setGlobalValue(selAssistantCompletionsTrigger_.getValue());

      // validate() only checks the completions-delay field while it is shown,
      // but it may be hidden (manual trigger) or detached (another assistant
      // selected) at apply time. Clamp it now, before super.onApply() below
      // persists the field's value, so no out-of-range delay can be saved.
      clampCompletionsDelay();

      RestartRequirement restartRequirement = super.onApply(prefs);

      // The system-CA checkbox value is applied by the base class (checkboxPref).
      // The agents read NODE_OPTIONS only at launch, so a change requires an R
      // session restart to take effect.
      if (cbAssistantUseSystemCa_.getValue() != initialUseSystemCa_)
      {
         initialUseSystemCa_ = cbAssistantUseSystemCa_.getValue();
         restartRequirement.setSessionRestartRequired(true);
      }

      return restartRequirement;
   }

   @Override
   public boolean validate()
   {
      // The completions-delay field is only shown -- and only relevant -- when
      // an assistant is selected and completions are triggered automatically.
      // Validate it only while it is displayed, so a hidden value cannot block
      // the dialog with an error pointing at a field the user cannot see.
      if (nvwAssistantCompletionsDelay_.isAttached() &&
          nvwAssistantCompletionsDelay_.isVisible() &&
          !nvwAssistantCompletionsDelay_.validate())
      {
         return false;
      }

      // The update-check interval is shown only for Posit Assistant. Validate it
      // only while it is displayed, so a hidden value cannot block the dialog.
      // Its validate() rejects non-digit input (including a typed or pasted
      // negative) with a ^\d+$ check, so an invalid interval cannot be saved.
      if (nvwAssistantUpdateCheckInterval_.isAttached() &&
          !nvwAssistantUpdateCheckInterval_.validate())
      {
         return false;
      }

      return true;
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
      installManager_ = new PositAiInstallManager();

      // Create assistant selector - conditionally include each provider based
      // on whether it is available in this build and enabled by the administrator.
      boolean paiEnabled = paiUtil_.isPositAssistantEnabled();
      boolean copilotEnabled = session_.getSessionInfo().getCopilotEnabled();
      List<String> assistantLabelList = new ArrayList<>();
      List<String> assistantValueList = new ArrayList<>();
      assistantLabelList.add(prefsConstants_.assistantEnum_none());
      assistantValueList.add(UserPrefsAccessor.ASSISTANT_NONE);
      if (paiEnabled)
      {
         assistantLabelList.add(prefsConstants_.assistantEnum_posit());
         assistantValueList.add(UserPrefsAccessor.ASSISTANT_POSIT);
      }
      if (copilotEnabled)
      {
         assistantLabelList.add(prefsConstants_.assistantEnum_copilot());
         assistantValueList.add(UserPrefsAccessor.ASSISTANT_COPILOT);
      }
      String[] assistantLabels = assistantLabelList.toArray(new String[0]);
      String[] assistantValues = assistantValueList.toArray(new String[0]);
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

      imgRefreshSpinner_ = new Spinner();
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

      btnInstall_ = new SmallButton(constants_.positAssistantInstallButton());
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
      cbAssistantToolbarButtonVisible_ = checkboxPref(prefs_.assistantToolbarButtonVisible(), true);
      cbAssistantUseSystemCa_ = checkboxPref(prefs_.assistantUseSystemCa(), true);
      nvwAssistantUpdateCheckInterval_ = numericPref(
            prefsConstants_.positAssistantUpdateCheckIntervalHoursTitle(),
            constants_.assistantUpdateCheckIntervalTooltip(),
            NumericValueWidget.ZeroMinimum,
            NumericValueWidget.NoMaximum,
            prefs_.positAssistantUpdateCheckIntervalHours());
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
            COMPLETIONS_DELAY_MIN_MS,
            COMPLETIONS_DELAY_MAX_MS,
            prefs_.assistantCompletionsDelay());

      cbAssistantNesEnabled_ = checkboxPref(prefs_.assistantNesEnabled(), true);
      cbAssistantNesCollapse_ = new CheckBox(constants_.assistantNesCollapseLabel());
      lessSpaced(cbAssistantNesCollapse_);
      cbAssistantNesCollapse_.setValue(!prefs_.assistantNesAutoshow().getGlobalValue());
      cbAssistantNesCollapse_.setTitle(constants_.assistantNesCollapseDescription());

      // Create chat provider selector - conditionally include Posit Assistant option
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

      // Add change handler for chat provider to check for Posit Assistant installation
      selChatProvider_.addChangeHandler((event) ->
      {
         String value = selChatProvider_.getValue();
         if (value.equals(UserPrefsAccessor.CHAT_PROVIDER_POSIT))
         {
            // Check for install/update/unsupported status
            checkPositAssistantInstallation(/* forAssistant= */ false);
         }
      });

      // The toolbar button and update-check interval apply only to Posit
      // Assistant; show them only when it is available. The system certificate
      // store option applies to every AI agent (Copilot included), so it stays
      // visible whenever this pane is shown.
      boolean paiEnabled = paiUtil_.isPositAssistantEnabled();
      if (paiEnabled)
         add(cbAssistantToolbarButtonVisible_);
      add(cbAssistantUseSystemCa_);
      if (paiEnabled)
         add(nvwAssistantUpdateCheckInterval_);

      // Code suggestions section
      add(spacedBefore(headerLabel(constants_.assistantSuggestionsHeader())));

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

      // Create Quick Reference panel (always displayed last)
      quickReferencePanel_ = createQuickReferencePanel();

      // Create the three panels
      nonePanel_ = createNonePanel();
      positAiPanel_ = createPositAiPanel();
      copilotPanel_ = createCopilotPanel();

      // Add container for dynamic content
      add(assistantDetailsPanel_);

      // Create Copilot Terms of Service panel at the bottom (absolute positioning)
      copilotTosPanel_ = new VerticalPanel();
      copilotTosPanel_.add(spaced(spacedBefore(lblCopilotTos_)));
      copilotTosPanel_.add(spaced(linkCopilotTos_));
      add(copilotTosPanel_);

      // Set up panel swapping based on assistant selection. A null event means a
      // programmatic refresh (panel load, project-options change, revert); a
      // non-null event means a genuine user selection, which may trigger an
      // install/version check. Callers that refresh programmatically must invoke
      // assistantChangedHandler_.onChange(null) rather than firing a synthetic
      // ChangeEvent, so an unrelated refresh is not mistaken for a user action.
      assistantChangedHandler_ = new ChangeHandler()
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

               // Reset both flags so each panel re-queries its status when next shown
               copilotRefreshed_ = false;
               positAiRefreshed_ = false;
            }
            else if (value.equals(UserPrefsAccessor.ASSISTANT_POSIT))
            {
               // Move status panel, project override panel, and common settings panel to Posit AI panel
               positAiPanel_.insert(spaced(statusPanel_), 0);
               positAiPanel_.insert(spaced(projectOverridePanel_), 1);
               positAiPanel_.add(commonSettingsPanel_);
               positAiPanel_.add(quickReferencePanel_);
               assistantDetailsPanel_.setWidget(positAiPanel_);
               copilotTosPanel_.setVisible(false);
               disableCopilot(UserPrefsAccessor.ASSISTANT_POSIT);

               // Reset the Copilot flag so it re-queries its status when next shown
               copilotRefreshed_ = false;

               // Refresh Posit Assistant status when panel is shown
               if (!positAiRefreshed_)
               {
                  positAiRefreshed_ = true;

                  // Clear the shared status UI now so the previous assistant's
                  // account is not shown during the async install/status checks
                  reset();

                  // Check if Posit Assistant is installed
                  server_.assistantVerifyInstalled(
                     UserPrefsAccessor.ASSISTANT_POSIT,
                     new ServerRequestCallback<Boolean>()
                     {
                        @Override
                        public void onResponseReceived(Boolean isInstalled)
                        {
                           // Ignore the result if the user switched away while
                           // the verify call was in flight
                           if (isStaleStatusResult(UserPrefsAccessor.ASSISTANT_POSIT))
                              return;

                           if (event == null)
                           {
                              // Panel just loaded, not a user action -- just refresh
                              refresh(UserPrefsAccessor.ASSISTANT_POSIT);
                           }
                           else
                           {
                              // User changed the selection -- check for
                              // install, update, or unsupported status
                              checkPositAssistantInstallation(/* forAssistant= */ true);
                           }
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           if (isStaleStatusResult(UserPrefsAccessor.ASSISTANT_POSIT))
                              return;

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
               copilotPanel_.add(quickReferencePanel_);
               assistantDetailsPanel_.setWidget(copilotPanel_);
               copilotTosPanel_.setVisible(true);
               positAiRefreshed_ = false;

               // Refresh Copilot status when panel is shown
               if (!copilotRefreshed_)
               {
                  copilotRefreshed_ = true;

                  // Clear the shared status UI now so the previous assistant's
                  // account is not shown during the async install/status checks
                  reset();

                  // Check if Copilot is installed (passing assistantType so backend knows
                  // which language server to check, even if preference isn't saved yet)
                  server_.assistantVerifyInstalled(
                     UserPrefsAccessor.ASSISTANT_COPILOT,
                     new ServerRequestCallback<Boolean>()
                     {
                        @Override
                        public void onResponseReceived(Boolean isInstalled)
                        {
                           // Ignore the result if the user switched away while
                           // the verify call was in flight
                           if (isStaleStatusResult(UserPrefsAccessor.ASSISTANT_COPILOT))
                              return;

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
                           if (isStaleStatusResult(UserPrefsAccessor.ASSISTANT_COPILOT))
                              return;

                           Debug.logError(error);
                           lblAssistantStatus_.setText(constants_.assistantStartupError());
                        }
                     });
               }
            }
         }
      };

      selAssistant_.addChangeHandler(assistantChangedHandler_);
      assistantChangedHandler_.onChange(null); // Initialize

      wrapWithPanel("assistant_prefs");
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

      // Suggestions section
      panel.add(selAssistantCompletionsTrigger_);
      panel.add(nvwAssistantCompletionsDelay_);
      panel.add(cbAssistantNesEnabled_);
      panel.add(cbAssistantNesCollapse_);

      // Match the initial visibility the trigger selector's change handler
      // maintains; the change handler does not fire until the value changes.
      updateCompletionsDelayVisibility();

      return panel;
   }

   /**
    * Shows the completions-delay field only when completions are triggered
    * automatically; the delay is not used for manual completions.
    */
   private void updateCompletionsDelayVisibility()
   {
      boolean isAuto = selAssistantCompletionsTrigger_.getValue().equals(
            UserPrefsAccessor.ASSISTANT_COMPLETIONS_TRIGGER_AUTO);
      nvwAssistantCompletionsDelay_.setVisible(isAuto);

      // Keep the hidden field's value in range so re-showing it (switching back
      // to automatic) never surfaces a stale out-of-range entry. The persisted
      // value is guarded separately, in onApply(), for any path that hides or
      // detaches the field.
      if (!isAuto)
         clampCompletionsDelay();
   }

   /**
    * Clamps the completions-delay field to its supported range, falling back to
    * the saved value when the field does not contain a parseable int (empty or
    * a digit string that overflows int).
    */
   private void clampCompletionsDelay()
   {
      int delay;
      try
      {
         delay = Integer.parseInt(nvwAssistantCompletionsDelay_.getValue().trim());
      }
      catch (NumberFormatException e)
      {
         delay = prefs_.assistantCompletionsDelay().getGlobalValue();
      }

      delay = Math.max(COMPLETIONS_DELAY_MIN_MS, Math.min(COMPLETIONS_DELAY_MAX_MS, delay));
      nvwAssistantCompletionsDelay_.setValue(delay + "");
   }

   private VerticalPanel createQuickReferencePanel()
   {
      VerticalPanel panel = new VerticalPanel();

      // Quick Reference section
      panel.add(spacedBefore(headerLabel(constants_.assistantQuickReferenceHeader())));

      LayoutGrid shortcutGrid = new LayoutGrid(2, 2);
      shortcutGrid.setCellPadding(4);
      shortcutGrid.setCellSpacing(0);

      String acceptShortcut = getStyledShortcut(commands_.assistantAcceptNextEditSuggestion());
      String dismissShortcut = getStyledShortcut(commands_.assistantDismissNextEditSuggestion());

      HTML acceptShortcutHtml = new HTML(acceptShortcut);
      acceptShortcutHtml.addStyleName(RES.styles().keyboardShortcut());
      HTML dismissShortcutHtml = new HTML(dismissShortcut);
      dismissShortcutHtml.addStyleName(RES.styles().keyboardShortcut());

      shortcutGrid.setWidget(0, 0, acceptShortcutHtml);
      shortcutGrid.setText(0, 1, constants_.assistantSuggestionsRequestAcceptHint());
      shortcutGrid.setWidget(1, 0, dismissShortcutHtml);
      shortcutGrid.setText(1, 1, constants_.assistantSuggestionsDismissHint());

      panel.add(shortcutGrid);

      return panel;
   }

   private String getStyledShortcut(AppCommand command)
   {
      String shortcut = command.getShortcutPrettyHtml();
      if (shortcut != null && shortcut.endsWith(";"))
      {
         shortcut = shortcut.substring(0, shortcut.length() - 1) +
                    "<span style=\"font-family:monospace;position:relative;top:-1px;\">;</span>";
      }
      return shortcut;
   }

   private void initModel()
   {
      selAssistantCompletionsTrigger_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateCompletionsDelayVisibility();
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
            checkPositAssistantInstallation(/* forAssistant= */ true);
         }
      });
   }

   /**
    * Returns true when an async status result for the given assistant type
    * should be ignored because the user has since selected a different
    * assistant. The status label and buttons are shared across panels, so a
    * late callback for a no-longer-selected assistant must not overwrite the
    * current panel's status.
    */
   private boolean isStaleStatusResult(String assistantType)
   {
      return !assistantType.equals(selAssistant_.getValue());
   }

   private void refresh(String assistantType)
   {
      // Resolve to the current preference when no explicit type was provided
      final String type = assistantType.isEmpty() ? prefs_.assistant().getGlobalValue() : assistantType;

      // A queued or late refresh for a no-longer-selected assistant must not
      // touch the shared status UI: it would clear the current panel and, since
      // the callback below early-returns, leave the spinner running. This guards
      // invocation paths such as the retry timer and the sign-in/out callbacks.
      if (isStaleStatusResult(type))
         return;

      // Clear any prior status, then show the spinner for this request
      reset();
      imgRefreshSpinner_.setVisible(true);

      ServerRequestCallback<AssistantStatusResponse> callback = new ServerRequestCallback<AssistantStatusResponse>()
      {
         @Override
         public void onResponseReceived(AssistantStatusResponse response)
         {
            // Ignore a result for an assistant the user has since switched
            // away from; the now-current panel owns the shared status UI
            if (isStaleStatusResult(type))
               return;

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
                     refresh(type);
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
                  lblAssistantStatus_.setText(AssistantResponseTypes.AssistantAgentNotRunningReason.reasonToString(reason, Assistant.getDisplayName(type)));

                  // Show Install button for Posit Assistant when not installed
                  if (reason == AssistantResponseTypes.AssistantAgentNotRunningReason.NotInstalled &&
                      type.equals(UserPrefsAccessor.ASSISTANT_POSIT))
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
                  lblAssistantStatus_.setText(constants_.assistantDisabledInProject(Assistant.getDisplayName(type)));
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
            if (isStaleStatusResult(type))
               return;

            imgRefreshSpinner_.setVisible(false);
            Debug.logError(error);
            hideButtons();
            lblAssistantStatus_.setText(constants_.assistantUnexpectedError());
            showButtons(btnRefresh_, btnDiagnostics_);
         }
      };

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
      }
   }

   /**
    * Checks if Posit Assistant needs to be installed and prompts the user to install it.
    *
    * @param forAssistant True if this check is for the assistant (completions) preference,
    *                     false if it's for the chat provider preference.
    */
   private void checkPositAssistantInstallation(boolean forAssistant)
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
            // Posit Assistant is already installed and up-to-date
            if (forAssistant)
            {
               refresh(UserPrefsAccessor.ASSISTANT_POSIT);
            }
         }

         @Override
         public void onUpdateAvailable(String currentVersion, String newVersion,
                                       boolean isInitialInstall, boolean isDowngrade,
                                       boolean additionalProvidersAvailable)
         {
            // additionalProvidersAvailable only varies the chat pane's not-installed
            // view; the preferences install prompt does not show that description.
            showInstallUpdatePrompt(newVersion, isInitialInstall, isDowngrade,
               forAssistant, previousAssistantValue, previousChatProviderValue);
         }

         @Override
         public void onIncompatibleVersion()
         {
            // No compatible version available - show error and revert
            globalDisplay_.showErrorMessage(
               constants_.positAssistantIncompatibleTitle(),
               constants_.positAssistantIncompatibleMessage(),
               (Operation) () -> {
                  revertPositAiPreference(forAssistant, previousAssistantValue, previousChatProviderValue);
               });
         }

         @Override
         public void onUnsupportedVersionUpgradeRequired(
             String currentVersion, String newVersion, boolean isDowngrade)
         {
            // Unsupported version with a required install. Can still be a downgrade
            // if the installed copy is unsupported (e.g. protocol mismatch) and the
            // recommended package is older than what's installed.
            showInstallUpdatePrompt(newVersion, false, isDowngrade, forAssistant,
               previousAssistantValue, previousChatProviderValue);
         }

         @Override
         public void onUnsupportedVersionNoUpdate(String currentVersion)
         {
            // Unsupported version with no update - show error and revert
            globalDisplay_.showErrorMessage(
               constants_.positAssistantUnsupportedVersionTitle(),
               constants_.positAssistantUnsupportedVersionMessage(),
               (Operation) () -> {
                  revertPositAiPreference(forAssistant, previousAssistantValue, previousChatProviderValue);
               });
         }

         @Override
         public void onUnsupportedProtocol()
         {
            // Protocol unsupported - RStudio itself needs updating
            globalDisplay_.showErrorMessage(
               constants_.positAssistantUnsupportedProtocolTitle(),
               constants_.positAssistantUnsupportedProtocolMessage(),
               (Operation) () -> {
                  revertPositAiPreference(forAssistant, previousAssistantValue, previousChatProviderValue);
               });
         }

         @Override
         public void onManifestUnavailable(String errorMessage)
         {
            // Manifest unavailable - can't verify compatibility
            globalDisplay_.showErrorMessage(
               constants_.positAssistantManifestUnavailableTitle(),
               constants_.positAssistantManifestUnavailableMessage(),
               (Operation) () -> {
                  revertPositAiPreference(forAssistant, previousAssistantValue, previousChatProviderValue);
               });
         }

         @Override
         public void onCheckFailed(String errorMessage)
         {
            // Check failed - this often happens when calling from Preferences pane
            // before the preference is saved. Since we know Posit Assistant isn't installed
            // (we got here because assistantVerifyInstalled returned false, or user
            // just selected Posit Assistant), offer to install without version info.
            showInstallUpdatePrompt(null, true, false, forAssistant,
               previousAssistantValue, previousChatProviderValue);
         }
      });
   }

   /**
    * Shows the install/update prompt dialog.
    */
   private void showInstallUpdatePrompt(String newVersion, boolean isInitialInstall,
                                        boolean isDowngrade,
                                        boolean forAssistant,
                                        String previousAssistantValue,
                                        String previousChatProviderValue)
   {
      String title;
      String message;
      String yesLabel;
      if (isInitialInstall)
      {
         title = constants_.positAssistantInstallTitle();
         message = (newVersion != null) ?
            constants_.positAssistantInstallMessage(newVersion) :
            constants_.positAssistantInstallMessageNoVersion();
         yesLabel = constants_.positAssistantInstallButton();
      }
      else if (isDowngrade)
      {
         title = constants_.positAssistantDowngradeTitle();
         message = constants_.positAssistantDowngradeMessage(newVersion);
         yesLabel = constants_.positAssistantInstallVersionButton(newVersion);
      }
      else
      {
         title = constants_.positAssistantUpdateTitle();
         message = constants_.positAssistantUpdateMessage(newVersion);
         yesLabel = constants_.positAssistantUpdateButton();
      }

      globalDisplay_.showYesNoMessage(
         GlobalDisplay.MSG_QUESTION,
         title,
         message,
         false,  // includeCancel
         (Operation) () -> {
            // User chose to install/update
            performPositAssistantInstall(forAssistant, previousAssistantValue, previousChatProviderValue);
         },
         (Operation) () -> {
            // User declined - revert the preference
            revertPositAiPreference(forAssistant, previousAssistantValue, previousChatProviderValue);
         },
         null,  // cancelOperation - not used since includeCancel is false
         yesLabel,
         constants_.positAssistantCancelButton(),
         true);  // yesIsDefault
   }

   /**
    * Performs the Posit Assistant installation with progress dialog.
    */
   private void performPositAssistantInstall(boolean forAssistant,
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
         globalDisplay_.showProgress(constants_.positAssistantInstallingMessage());

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
               constants_.positAssistantInstallCompleteTitle(),
               constants_.positAssistantInstallCompleteMessage(),
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
               constants_.positAssistantInstallFailedTitle(),
               constants_.positAssistantInstallFailedMessage(errorMessage),
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
         assistantChangedHandler_.onChange(null);
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
      // Clear the shared status UI so a previous assistant's account info and
      // loading spinner are not shown while the new status is being fetched
      imgRefreshSpinner_.setVisible(false);
      lblAssistantStatus_.setText("");
      hideButtons();
   }
   
   @Override
   public ImageResource getIcon()
   {
      if (useDarkDialogTheme())
         return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconAssistantDark2x());
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
      initialUseSystemCa_ = prefs.assistantUseSystemCa().getGlobalValue();

      // Migration: if rstudio_assistant is "none" but the legacy copilot_enabled
      // pref is set, auto-migrate to "copilot" -- but only when Copilot is
      // actually available in this build and enabled by the administrator.
      String assistant = prefs.assistant().getGlobalValue();
      boolean copilotEnabled = session_.getSessionInfo().getCopilotEnabled();
      if (assistant.equals(UserPrefsAccessor.ASSISTANT_NONE) &&
          prefs.copilotEnabled().getGlobalValue() &&
          copilotEnabled)
      {
         prefs.assistant().setGlobalValue(UserPrefsAccessor.ASSISTANT_COPILOT);
         selAssistant_.setValue(UserPrefsAccessor.ASSISTANT_COPILOT);
      }

      // Reset to "none" if the selected assistant is no longer available in this
      // build or enabled by the administrator.
      if (assistant.equals(UserPrefsAccessor.ASSISTANT_POSIT) &&
          !paiUtil_.isPositAssistantEnabled())
      {
         prefs.assistant().setGlobalValue(UserPrefsAccessor.ASSISTANT_NONE);
         selAssistant_.setValue(UserPrefsAccessor.ASSISTANT_NONE);
      }
      else if (assistant.equals(UserPrefsAccessor.ASSISTANT_COPILOT) &&
               !copilotEnabled)
      {
         prefs.assistant().setGlobalValue(UserPrefsAccessor.ASSISTANT_NONE);
         selAssistant_.setValue(UserPrefsAccessor.ASSISTANT_NONE);
      }

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
         assistantChangedHandler_.onChange(null);
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
         assistantChangedHandler_.onChange(null);
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
         assistantChangedHandler_.onChange(null);
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
      String keyboardShortcut();
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
   private boolean initialUseSystemCa_; // snapshot from initialize(); guards the session-restart trigger in onApply
   private HandlerRegistration assistantRuntimeStatusHandler_;
   private HandlerRegistration projectOptionsChangedHandler_;
   private boolean assistantStarted_ = false; // did Copilot get started while the dialog was open?
   private boolean copilotRefreshed_ = false; // has Copilot status been refreshed for this pane instance?
   private boolean positAiRefreshed_ = false; // has Posit Assistant status been refreshed for this pane instance?
   private ChangeHandler assistantChangedHandler_; // swaps the displayed assistant panel; created in initDisplay
   private RProjectOptions projectOptions_;
   private String projectAssistantOverride_; // non-null when project has overridden assistant

   // Assistant panels (created in initDisplay)
   private VerticalPanel nonePanel_;
   private VerticalPanel positAiPanel_;
   private VerticalPanel copilotPanel_;
   private VerticalPanel copilotTosPanel_;
   private VerticalPanel commonSettingsPanel_;
   private VerticalPanel copilotOtherPanel_;
   private VerticalPanel quickReferencePanel_;
   private HorizontalPanel statusPanel_;
   private HorizontalPanel projectOverridePanel_;

   // UI
   private final SelectWidget selAssistant_;
   private final SimplePanel assistantDetailsPanel_;
   private final Label lblAssistantStatus_;
   private final Spinner imgRefreshSpinner_;
   private final CheckBox cbAssistantShowMessages_;
   private final CheckBox cbAssistantToolbarButtonVisible_;
   private final CheckBox cbAssistantUseSystemCa_;
   private final CheckBox cbAssistantNesEnabled_;
   private final CheckBox cbAssistantNesCollapse_;
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
   private final NumericValueWidget nvwAssistantUpdateCheckInterval_;
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
   
   private boolean useDarkDialogTheme()
   {
      Element container = Document.get().getElementById("rstudio_container");
      return prefs_.useDarkThemeModalDialogs().getValue() &&
             container != null &&
             container.hasClassName("rstudio-themes-dark");
   }

   // Supported range for the automatic completions delay, in milliseconds.
   private static final int COMPLETIONS_DELAY_MIN_MS = 10;
   private static final int COMPLETIONS_DELAY_MAX_MS = 5000;

   private static final UserPrefsAccessorConstants prefsConstants_ = GWT.create(UserPrefsAccessorConstants.class);
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);

}
