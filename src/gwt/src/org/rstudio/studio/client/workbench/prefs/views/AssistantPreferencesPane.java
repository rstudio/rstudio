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
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.copilot.model.CopilotConstants;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes;
import org.rstudio.studio.client.workbench.copilot.model.CopilotStatusChangedEvent;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotStatusResponse;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessorConstants;
import org.rstudio.studio.client.workbench.prefs.views.events.CopilotEnabledEvent;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.FontStyle;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
      prefs.rstudioAssistant().setGlobalValue(selectedAssistant);
      prefs.copilotEnabled().setGlobalValue(
            selectedAssistant.equals(UserPrefsAccessor.RSTUDIO_ASSISTANT_COPILOT));

      prefs.copilotTabKeyBehavior().setGlobalValue(selCopilotTabKeyBehavior_.getValue());
      prefs.copilotCompletionsTrigger().setGlobalValue(selCopilotCompletionsTrigger_.getValue());

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
                                 Copilot copilot,
                                 CopilotServerOperations server,
                                 ProjectsServerOperations projectServer)
   {
      events_ = events;
      session_ = session;
      prefs_ = prefs;
      commands_ = commands;
      copilot_ = copilot;
      server_ = server;
      projectServer_ = projectServer;

      // Create assistant selector
      selAssistant_ = new SelectWidget(
            constants_.assistantSelectLabel(),
            new String[] {
                  prefsConstants_.rstudioAssistantEnum_none(),
                  prefsConstants_.rstudioAssistantEnum_posit_ai(),
                  prefsConstants_.rstudioAssistantEnum_copilot()
            },
            new String[] {
                  UserPrefsAccessor.RSTUDIO_ASSISTANT_NONE,
                  UserPrefsAccessor.RSTUDIO_ASSISTANT_POSIT_AI,
                  UserPrefsAccessor.RSTUDIO_ASSISTANT_COPILOT
            },
            false,
            true,
            false);
      selAssistant_.setValue(prefs_.rstudioAssistant().getGlobalValue());

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
      
      cbCopilotShowMessages_ = checkboxPref(prefs_.copilotShowMessages(), true);
      cbCopilotProjectWorkspace_ = checkboxPref(prefs_.copilotProjectWorkspace(), true);
      selCopilotTabKeyBehavior_ = new SelectWidget(
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
      
      selCopilotTabKeyBehavior_.setValue(prefs_.copilotTabKeyBehavior().getGlobalValue());
      
      selCopilotCompletionsTrigger_ = new SelectWidget(
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
      
      selCopilotCompletionsTrigger_.setValue(prefs_.copilotCompletionsTrigger().getGlobalValue());
 
      nvwCopilotCompletionsDelay_ = numericPref(
            constants_.copilotCompletionsDelayLabel(),
            10,
            5000,
            prefs_.copilotCompletionsDelay());

      cbCopilotNesEnabled_ = checkboxPref(prefs_.copilotNesEnabled(), true);
      cbCopilotNesAutoshow_ = checkboxPref(prefs_.copilotNesAutoshow(), true);

      linkCopilotTos_ = new HelpLink(
            constants_.copilotTermsOfServiceLinkLabel(),
            "github-copilot-terms-of-service",
            false);
      
      lblCopilotTos_ = new Label(constants_.copilotTermsOfServiceLabel());
      lblCopilotTos_.addStyleName(RES.styles().copilotTosLabel());

      copilotStatusHandler_ = events_.addHandler(CopilotStatusChangedEvent.TYPE, (event) -> {
         copilotStarted_ = event.getStatus() == CopilotStatusChangedEvent.RUNNING;
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
            if (value.equals(UserPrefsAccessor.RSTUDIO_ASSISTANT_NONE))
            {
               assistantDetailsPanel_.setWidget(nonePanel_);
               copilotTosPanel_.setVisible(false);
            }
            else if (value.equals(UserPrefsAccessor.RSTUDIO_ASSISTANT_POSIT_AI))
            {
               assistantDetailsPanel_.setWidget(positAiPanel_);
               copilotTosPanel_.setVisible(false);
            }
            else if (value.equals(UserPrefsAccessor.RSTUDIO_ASSISTANT_COPILOT))
            {
               assistantDetailsPanel_.setWidget(copilotPanel_);
               copilotTosPanel_.setVisible(true);
               // Refresh Copilot status when panel is shown
               if (!copilotRefreshed_)
               {
                  refresh();
                  copilotRefreshed_ = true;
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

   private VerticalPanel createPositAiPanel()
   {
      VerticalPanel panel = new VerticalPanel();
      Label lblPlaceholder = new Label(constants_.positAiPlaceholder());
      panel.add(spaced(lblPlaceholder));
      return panel;
   }

   private VerticalPanel createCopilotPanel()
   {
      VerticalPanel panel = new VerticalPanel();

      if (session_.getSessionInfo().getCopilotEnabled())
      {
         HorizontalPanel statusPanel = new HorizontalPanel();
         statusPanel.add(lblCopilotStatus_);
         for (SmallButton button : statusButtons_)
            statusPanel.add(button);
         panel.add(spaced(statusPanel));

         panel.add(spacedBefore(headerLabel(constants_.copilotCompletionsHeader())));
         panel.add(selCopilotCompletionsTrigger_);
         panel.add(nvwCopilotCompletionsDelay_);

         panel.add(spacedBefore(headerLabel(constants_.copilotSuggestionsHeader())));
         panel.add(cbCopilotNesEnabled_);
         panel.add(cbCopilotNesAutoshow_);

         String modifier = BrowseCap.isMacintosh() ? "Cmd" : "Ctrl";
         Label lblNesShortcutHint = new Label(constants_.copilotSuggestionsShortcutHint(modifier));
         lblNesShortcutHint.getElement().getStyle().setFontStyle(FontStyle.ITALIC);
         panel.add(spaced(lblNesShortcutHint));

         panel.add(spacedBefore(headerLabel(constants_.otherCaption())));
         panel.add(cbCopilotShowMessages_);
         panel.add(cbCopilotProjectWorkspace_);
      }
      else
      {
         panel.add(new Label(constants_.copilotDisabledByAdmin()));
      }

      return panel;
   }
   
   private void initModel()
   {
      selCopilotCompletionsTrigger_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            String value = selCopilotCompletionsTrigger_.getValue();
            if (value == UserPrefsAccessor.COPILOT_COMPLETIONS_TRIGGER_AUTO)
            {
               nvwCopilotCompletionsDelay_.setVisible(true);
            }
            else
            {
               nvwCopilotCompletionsDelay_.setVisible(false);
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
            copilot_.onCopilotSignIn((response) -> refresh());
         }
      });
      
      btnSignOut_.addClickHandler(new ClickHandler()
      {
         
         @Override
         public void onClick(ClickEvent event)
         {
            copilot_.onCopilotSignOut((response) -> refresh());
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
            refresh();
         }
      });
      
      btnDiagnostics_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ProgressIndicator indicator = getProgressIndicator();
            indicator.onProgress(constants_.copilotDiagnosticReportProgressLabel());
            copilot_.onCopilotDiagnostics(() ->
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
      
      events_.addHandler(CopilotEnabledEvent.TYPE, (event) ->
      {
         refresh();
      });
   }
   
   private void refresh()
   {
      reset();
      
      server_.copilotStatus(new ServerRequestCallback<CopilotStatusResponse>()
      {
         @Override
         public void onResponseReceived(CopilotStatusResponse response)
         {
            hideButtons();
            
            if (response == null)
            {
               lblCopilotStatus_.setText(constants_.copilotUnexpectedError());
            }
            else if (response.result == null)
            {
               if (response.error != null && response.error.getCode() == CopilotConstants.ErrorCodes.AGENT_NOT_INITIALIZED)
               {
                  // Copilot still starting up, so wait a second and refresh again
                  SingleShotTimer.fire(1000, () -> {
                     refresh();
                  });
               }
               else if (response.error != null && response.error.getCode() != CopilotConstants.ErrorCodes.AGENT_SHUT_DOWN)
               {
                  lblCopilotStatus_.setText(constants_.copilotStartupError());
                  if (!StringUtil.isNullOrEmpty(response.output))
                  {
                     copilotStartupError_ = response.output;
                     showButtons(btnShowError_);
                  }
               }
               else if (CopilotResponseTypes.CopilotAgentNotRunningReason.isError(response.reason))
               {
                  int reason = (int) response.reason.valueOf();
                  lblCopilotStatus_.setText(CopilotResponseTypes.CopilotAgentNotRunningReason.reasonToString(reason));
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
            else if (response.result.status == CopilotConstants.STATUS_OK ||
                     response.result.status == CopilotConstants.STATUS_ALREADY_SIGNED_IN)
            {
               showButtons(btnSignOut_, btnRefresh_, btnDiagnostics_);
               lblCopilotStatus_.setText(constants_.copilotSignedInAsLabel(response.result.user));
            }
            else if (response.result.status == CopilotConstants.STATUS_NOT_AUTHORIZED)
            {
               showButtons(btnActivate_, btnSignOut_, btnRefresh_, btnDiagnostics_);
               lblCopilotStatus_.setText(constants_.copilotAccountNotActivated(response.result.user));
            }
            else if (response.result.status == CopilotConstants.STATUS_NOT_SIGNED_IN)
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
      });
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
      String assistant = prefs.rstudioAssistant().getGlobalValue();
      if (assistant.equals(UserPrefsAccessor.RSTUDIO_ASSISTANT_NONE) &&
          prefs.copilotEnabled().getGlobalValue())
      {
         prefs.rstudioAssistant().setGlobalValue(UserPrefsAccessor.RSTUDIO_ASSISTANT_COPILOT);
         selAssistant_.setValue(UserPrefsAccessor.RSTUDIO_ASSISTANT_COPILOT);
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
      if (selAssistant_.getValue().equals(UserPrefsAccessor.RSTUDIO_ASSISTANT_COPILOT))
      {
         refresh();
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
   private RProjectOptions projectOptions_;

   // Assistant panels (created in initDisplay)
   private VerticalPanel nonePanel_;
   private VerticalPanel positAiPanel_;
   private VerticalPanel copilotPanel_;
   private VerticalPanel copilotTosPanel_;

   // UI
   private final SelectWidget selAssistant_;
   private final SimplePanel assistantDetailsPanel_;
   private final Label lblCopilotStatus_;
   private final CheckBox cbCopilotShowMessages_;
   private final CheckBox cbCopilotProjectWorkspace_;
   private final CheckBox cbCopilotNesEnabled_;
   private final CheckBox cbCopilotNesAutoshow_;
   private final List<SmallButton> statusButtons_;
   private final SmallButton btnShowError_;
   private final SmallButton btnSignIn_;
   private final SmallButton btnSignOut_;
   private final SmallButton btnActivate_;
   private final SmallButton btnRefresh_;
   private final SmallButton btnDiagnostics_;
   private final SmallButton btnProjectOptions_;
   private final NumericValueWidget nvwCopilotCompletionsDelay_;
   private final SelectWidget selCopilotTabKeyBehavior_;
   private final SelectWidget selCopilotCompletionsTrigger_;
   private final HelpLink linkCopilotTos_;
   private final Label lblCopilotTos_;
   
   // Injected
   private final EventBus events_;
   private final Session session_;
   private final UserPrefs prefs_;
   private final Commands commands_;
   private final Copilot copilot_;
   private final CopilotServerOperations server_;
   private final ProjectsServerOperations projectServer_;
   
   private static final UserPrefsAccessorConstants prefsConstants_ = GWT.create(UserPrefsAccessorConstants.class);
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
   
}
