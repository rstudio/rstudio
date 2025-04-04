/*
 * CopilotPreferencesPane.java
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
import org.rstudio.core.client.StringUtil;
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
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;


public class CopilotPreferencesPane extends PreferencesPane
{
   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      prefs.copilotTabKeyBehavior().setGlobalValue(selCopilotTabKeyBehavior_.getValue());
      prefs.copilotCompletionsTrigger().setGlobalValue(selCopilotCompletionsTrigger_.getValue());
      
      RestartRequirement requirement = super.onApply(prefs);
      if (initialCopilotIndexingEnabled_ != prefs.copilotIndexingEnabled().getGlobalValue())
         requirement.setSessionRestartRequired(true);
      if (initialCopilotWorkspaceEnabled_ != prefs.copilotProjectWorkspace().getGlobalValue())
         requirement.setSessionRestartRequired(true); 
      return requirement;
   }
   
   @Inject
   public CopilotPreferencesPane(EventBus events,
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
      
      cbCopilotEnabled_ = checkboxPref(prefs_.copilotEnabled(), true);
      cbCopilotIndexingEnabled_ = checkboxPref(prefs_.copilotIndexingEnabled(), true);
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
      
      linkCopilotTos_ = new HelpLink(
            constants_.copilotTermsOfServiceLinkLabel(),
            "github-copilot-terms-of-service",
            false);
      
      lblCopilotTos_ = new Label(constants_.copilotTermsOfServiceLabel());
      lblCopilotTos_.addStyleName(RES.styles().copilotTosLabel());
   }
   
   private void initDisplay()
   {
      add(headerLabel(constants_.copilotDisplayName()));
      
      if (session_.getSessionInfo().getCopilotEnabled())
      {
         add(cbCopilotEnabled_);

         HorizontalPanel statusPanel = new HorizontalPanel();
         statusPanel.add(lblCopilotStatus_);
         for (SmallButton button : statusButtons_)
            statusPanel.add(button);
         add(spaced(statusPanel));
         
         add(headerLabel(constants_.copilotIndexingHeader()));
         add(spaced(cbCopilotIndexingEnabled_));

         add(spacedBefore(headerLabel(constants_.copilotCompletionsHeader())));
         add(selCopilotCompletionsTrigger_);
         add(nvwCopilotCompletionsDelay_);

         add(spacedBefore(headerLabel(constants_.otherCaption())));
         add(cbCopilotShowMessages_);
         add(cbCopilotProjectWorkspace_);

         // add(checkboxPref(prefs_.copilotAllowAutomaticCompletions()));
         // add(selCopilotTabKeyBehavior_);
      }
      else
      {
         add(new Label(constants_.copilotDisabledByAdmin()));
      }
      
      VerticalPanel bottomPanel = new VerticalPanel();
      bottomPanel.getElement().getStyle().setBottom(0, Unit.PX);
      bottomPanel.getElement().getStyle().setPosition(Position.ABSOLUTE);
      bottomPanel.add(spaced(lblCopilotTos_));
      bottomPanel.add(spaced(linkCopilotTos_));
      add(bottomPanel);
   }
   
   private void initModel()
   {
      cbCopilotEnabled_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            boolean enabled = event.getValue();
            
            if (enabled)
            {
               // Eagerly change the preference here, so that we can
               // respond to changes in the agent status.
               prefs_.copilotEnabled().setGlobalValue(true);
               prefs_.writeUserPrefs((completed) ->
               {
                  refresh();
               });
            }
         }
      });
      
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
               if (response.error != null)
               {
                  lblCopilotStatus_.setText(constants_.copilotStartupError());
                  if (!StringUtil.isNullOrEmpty(response.output))
                  {
                     copilotStartupError_ = response.output;
                     showButtons(btnShowError_);
                  }
               }
               else if (projectOptions_ != null && projectOptions_.getCopilotOptions().copilot_enabled == RProjectConfig.NO_VALUE)
               {
                  lblCopilotStatus_.setText(constants_.copilotDisabledInProject());
                  showButtons(btnProjectOptions_);
               }
               else if (prefs_.copilotEnabled().getValue())
               {
                  lblCopilotStatus_.setText(constants_.copilotAgentNotRunning());
               }
               else
               {
                  lblCopilotStatus_.setText(constants_.copilotAgentNotEnabled());
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
      return new ImageResource2x(RES.iconCopilotLight2x());
   }

   @Override
   public String getName()
   {
      return constants_.copilotPaneName();
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      initialCopilotIndexingEnabled_ = prefs.copilotIndexingEnabled().getGlobalValue();
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
      refresh();
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
      @Source("CopilotPreferencesPane.css")
      Styles styles();
      
      @Source("iconCopilotLight_2x.png")
      ImageResource iconCopilotLight2x();
      
      @Source("iconCopilotDark_2x.png")
      ImageResource iconCopilotDark2x();
      
   }

   public static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }
   
   // State
   private String copilotStartupError_;
   private boolean initialCopilotIndexingEnabled_;
   private boolean initialCopilotWorkspaceEnabled_;
   private RProjectOptions projectOptions_;
 
   // UI
   private final Label lblCopilotStatus_;
   private final CheckBox cbCopilotEnabled_;
   private final CheckBox cbCopilotIndexingEnabled_;
   private final CheckBox cbCopilotShowMessages_;
   private final CheckBox cbCopilotProjectWorkspace_;
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
