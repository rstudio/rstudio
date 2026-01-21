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
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DialogBuilder;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.dialog.WebDialogBuilderFactory;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
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
                                 ProjectsServerOperations projectServer)
   {
      events_ = events;
      session_ = session;
      prefs_ = prefs;
      commands_ = commands;
      assistant_ = assistant;
      server_ = server;
      projectServer_ = projectServer;

      // Create assistant selector - conditionally include Posit AI option
      boolean paiEnabled = PaiUtil.isPaiEnabled(session_.getSessionInfo(), prefs_);
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

      lblCopilotStatus_ = new Label(constants_.copilotLoadingMessage());
      lblCopilotStatus_.addStyleName(RES.styles().copilotStatusLabel());
      
      statusButtons_ = new ArrayList<SmallButton>();
      
      btnShowError_ = new SmallButton(constants_.copilotShowErrorLabel());
      btnShowError_.addStyleName(RES.styles().button());
      statusButtons_.add(btnShowError_);
      
      btnSignIn_ = new SmallButton(constants_.copilotSignInLabel());
      btnSignIn_.addStyleName(RES.styles().button());
      statusButtons_.add(btnSignIn_);
      
      btnSignOut_ = new SmallButton(constants_.copilotSignOutLabel());
      btnSignOut_.addStyleName(RES.styles().button());
      statusButtons_.add(btnSignOut_);
      
      btnActivate_ = new SmallButton(constants_.copilotActivateLabel());
      Roles.getLinkRole().set(btnActivate_.getElement());
      btnActivate_.getElement().setPropertyString("href", "https://github.com/settings/copilot");
      btnActivate_.addStyleName(RES.styles().button());
      statusButtons_.add(btnActivate_);
      
      btnRefresh_ = new SmallButton(constants_.copilotRefreshLabel());
      btnRefresh_.addStyleName(RES.styles().button());
      statusButtons_.add(btnRefresh_);
      
      btnDiagnostics_ = new SmallButton(constants_.copilotDiagnosticsLabel());
      btnDiagnostics_.addStyleName(RES.styles().button());
      statusButtons_.add(btnDiagnostics_);
      
      btnProjectOptions_ = new SmallButton(constants_.copilotProjectOptionsLabel());
      btnProjectOptions_.addStyleName(RES.styles().button());
      statusButtons_.add(btnProjectOptions_);
      
      cbAssistantShowMessages_ = checkboxPref(prefs_.copilotShowMessages(), true);
      selAssistantTabKeyBehavior_ = new SelectWidget(
            prefsConstants_.copilotTabKeyBehaviorTitle(),
            new String[] {
                  prefsConstants_.copilotTabKeyBehaviorEnum_suggestion(),
                  prefsConstants_.copilotTabKeyBehaviorEnum_completions()
            },
            new String[] {
                  UserPrefsAccessor.COPILOT_TAB_KEY_BEHAVIOR_SUGGESTION,
                  UserPrefsAccessor.COPILOT_TAB_KEY_BEHAVIOR_COMPLETIONS
            },
            false,
            true,
            false);
      
      selAssistantTabKeyBehavior_.setValue(prefs_.copilotTabKeyBehavior().getGlobalValue());
      
      selAssistantCompletionsTrigger_ = new SelectWidget(
            prefsConstants_.copilotCompletionsTriggerTitle(),
            new String[] {
                  prefsConstants_.copilotCompletionsTriggerEnum_auto(),
                  prefsConstants_.copilotCompletionsTriggerEnum_manual()
            },
            new String[] {
                  UserPrefsAccessor.COPILOT_COMPLETIONS_TRIGGER_AUTO,
                  UserPrefsAccessor.COPILOT_COMPLETIONS_TRIGGER_MANUAL
            },
            false,
            true,
            false);
      
      selAssistantCompletionsTrigger_.setValue(prefs_.copilotCompletionsTrigger().getGlobalValue());
 
      nvwAssistantCompletionsDelay_ = numericPref(
            constants_.copilotCompletionsDelayLabel(),
            10,
            5000,
            prefs_.copilotCompletionsDelay());

      cbAssistantNesEnabled_ = checkboxPref(prefs_.copilotNesEnabled(), true);
      cbAssistantNesAutoshow_ = checkboxPref(prefs_.copilotNesAutoshow(), true);

      linkCopilotTos_ = new HelpLink(
            constants_.copilotTermsOfServiceLinkLabel(),
            "github-copilot-terms-of-service",
            false);
      
      lblCopilotTos_ = new Label(constants_.copilotTermsOfServiceLabel());
      lblCopilotTos_.addStyleName(RES.styles().copilotTosLabel());

      copilotStatusHandler_ = events_.addHandler(AssistantRuntimeStatusChangedEvent.TYPE, (event) -> {
         copilotStarted_ = event.getStatus() == AssistantRuntimeStatusChangedEvent.RUNNING;
      });
   }
   
   @Override
   public void onUnload()
   {
      if (copilotStatusHandler_ != null)
      {
         copilotStatusHandler_.removeHandler();
         copilotStatusHandler_ = null;
      }
      super.onUnload();
   }
   
   private void initDisplay()
   {
      add(headerLabel(constants_.assistantDisplayName()));

      // Add assistant selector
      add(selAssistant_);

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
            if (value.equals(UserPrefsAccessor.ASSISTANT_NONE))
            {
               assistantDetailsPanel_.setWidget(nonePanel_);
               copilotTosPanel_.setVisible(false);
               disableCopilot(UserPrefsAccessor.ASSISTANT_NONE);
            }
            else if (value.equals(UserPrefsAccessor.ASSISTANT_POSIT))
            {
               // Move status panel and common settings panel to Posit AI panel
               positAiPanel_.insert(spaced(statusPanel_), 0);
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
                           if (isInstalled)
                           {
                              refresh(UserPrefsAccessor.ASSISTANT_POSIT);
                           }
                           else
                           {
                              lblCopilotStatus_.setText(constants_.copilotAgentNotEnabled());
                           }
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                           lblCopilotStatus_.setText(constants_.copilotStartupError());
                        }
                     });
               }
            }
            else if (value.equals(UserPrefsAccessor.ASSISTANT_COPILOT))
            {
               // Move status panel, common settings panel and Copilot-specific "Other" panel to Copilot panel
               if (session_.getSessionInfo().getCopilotEnabled())
               {
                  copilotPanel_.insert(spaced(statusPanel_), 0);
               }
               copilotPanel_.add(commonSettingsPanel_);
               copilotPanel_.add(copilotOtherPanel_);
               assistantDetailsPanel_.setWidget(copilotPanel_);
               copilotTosPanel_.setVisible(true);

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
                              lblCopilotStatus_.setText(constants_.copilotAgentNotEnabled());
                           }
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                           lblCopilotStatus_.setText(constants_.copilotStartupError());
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
      panel.add(lblCopilotStatus_);
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
            if (value == UserPrefsAccessor.COPILOT_COMPLETIONS_TRIGGER_AUTO)
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
                  constants_.copilotStatusDialogCaption(),
                  copilotStartupError_,
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
            indicator.onProgress(constants_.copilotDiagnosticReportProgressLabel());
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
   }

   private void refresh(String assistantType)
   {
      reset();

      // Use overloaded method to pass assistantType if provided
      ServerRequestCallback<AssistantStatusResponse> callback = new ServerRequestCallback<AssistantStatusResponse>()
      {
         @Override
         public void onResponseReceived(AssistantStatusResponse response)
         {
            hideButtons();

            if (response == null)
            {
               lblCopilotStatus_.setText(constants_.copilotUnexpectedError());
            }
            else if (response.result == null)
            {
               if (response.error != null && response.error.getCode() == AssistantConstants.ErrorCodes.AGENT_NOT_INITIALIZED)
               {
                  // Copilot still starting up, so wait a second and refresh again
                  SingleShotTimer.fire(1000, () -> {
                     refresh(assistantType);
                  });
               }
               else if (response.error != null && response.error.getCode() != AssistantConstants.ErrorCodes.AGENT_SHUT_DOWN)
               {
                  lblCopilotStatus_.setText(constants_.copilotStartupError());
                  if (!StringUtil.isNullOrEmpty(response.output))
                  {
                     copilotStartupError_ = response.output;
                     showButtons(btnShowError_);
                  }
               }
               else if (AssistantResponseTypes.AssistantAgentNotRunningReason.isError(response.reason))
               {
                  int reason = (int) response.reason.valueOf();
                  lblCopilotStatus_.setText(AssistantResponseTypes.AssistantAgentNotRunningReason.reasonToString(reason));
                  showButtons(btnRefresh_, btnDiagnostics_);
               }
               else if (projectOptions_ != null && projectOptions_.getAssistantOptions().copilot_enabled == RProjectConfig.NO_VALUE)
               {
                  lblCopilotStatus_.setText(constants_.copilotDisabledInProject());
                  showButtons(btnProjectOptions_);
               }
               else if (prefs_.copilotEnabled().getValue())
               {
                  lblCopilotStatus_.setText(constants_.copilotAgentNotRunning());
                  showButtons(btnSignIn_, btnRefresh_, btnDiagnostics_);
               }
               else
               {
                  lblCopilotStatus_.setText(constants_.copilotAgentNotEnabled());
                  showButtons(btnSignIn_, btnRefresh_, btnDiagnostics_);
               }
            }
            else if (response.result.status == AssistantConstants.STATUS_OK ||
                     response.result.status == AssistantConstants.STATUS_ALREADY_SIGNED_IN)
            {
               showButtons(btnSignOut_, btnRefresh_, btnDiagnostics_);
               lblCopilotStatus_.setText(constants_.copilotSignedInAsLabel(response.result.user));
            }
            else if (response.result.status == AssistantConstants.STATUS_NOT_AUTHORIZED)
            {
               showButtons(btnActivate_, btnSignOut_, btnRefresh_, btnDiagnostics_);
               lblCopilotStatus_.setText(constants_.copilotAccountNotActivated(response.result.user));
            }
            else if (response.result.status == AssistantConstants.STATUS_NOT_SIGNED_IN)
            {
               showButtons(btnSignIn_, btnRefresh_, btnDiagnostics_);
               lblCopilotStatus_.setText(constants_.copilotNotSignedIn());
            }
            else
            {
               String message = constants_.copilotUnknownResponse(JSON.stringify(response));
               lblCopilotStatus_.setText(message);
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

   private void reset()
   {
      copilotStartupError_ = null;
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

      // Only refresh Copilot status if Copilot is the selected assistant
      if (selAssistant_.getValue().equals(UserPrefsAccessor.ASSISTANT_COPILOT))
      {
         refresh(selAssistant_.getValue());
         copilotRefreshed_ = true;
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
      String copilotStatusLabel();
      String copilotTosLabel();
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
   private String copilotStartupError_;
   private boolean initialCopilotWorkspaceEnabled_;
   private HandlerRegistration copilotStatusHandler_;
   private boolean copilotStarted_ = false; // did Copilot get started while the dialog was open?
   private boolean copilotRefreshed_ = false; // has Copilot status been refreshed for this pane instance?
   private boolean positAiRefreshed_ = false; // has Posit AI status been refreshed for this pane instance?
   private RProjectOptions projectOptions_;

   // Assistant panels (created in initDisplay)
   private VerticalPanel nonePanel_;
   private VerticalPanel positAiPanel_;
   private VerticalPanel copilotPanel_;
   private VerticalPanel copilotTosPanel_;
   private VerticalPanel commonSettingsPanel_;
   private VerticalPanel copilotOtherPanel_;
   private HorizontalPanel statusPanel_;

   // UI
   private final SelectWidget selAssistant_;
   private final SimplePanel assistantDetailsPanel_;
   private final Label lblCopilotStatus_;
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
   private final NumericValueWidget nvwAssistantCompletionsDelay_;
   private final SelectWidget selAssistantTabKeyBehavior_;
   private final SelectWidget selAssistantCompletionsTrigger_;
   private final HelpLink linkCopilotTos_;
   private final Label lblCopilotTos_;
   
   // Injected
   private final EventBus events_;
   private final Session session_;
   private final UserPrefs prefs_;
   private final Commands commands_;
   private final Assistant assistant_;
   private final AssistantServerOperations server_;
   private final ProjectsServerOperations projectServer_;
   
   private static final UserPrefsAccessorConstants prefsConstants_ = GWT.create(UserPrefsAccessorConstants.class);
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
   
}
