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
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.copilot.model.CopilotConstants;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotStatusResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotStatusChangedEvent;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
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
      // Map "none" (used as default placeholder in UI) to "default" for storage
      String selectedAssistant = selAssistant_.getValue();
      if (selectedAssistant.equals(UserPrefsAccessor.RSTUDIO_ASSISTANT_NONE))
         assistantOptions.assistant = "default";
      else
         assistantOptions.assistant = selectedAssistant;

      RestartRequirement requirement = new RestartRequirement();

      // Check if assistant changed
      String originalAssistant = options_.getAssistantOptions().assistant;
      if (originalAssistant == null)
         originalAssistant = "default";
      if (!originalAssistant.equals(assistantOptions.assistant))
         requirement.setSessionRestartRequired(true);

      return requirement;
   }

   @Inject
   public ProjectAssistantPreferencesPane(EventBus events,
                                        Session session,
                                        UserPrefs prefs,
                                        AriaLiveService ariaLive,
                                        Copilot copilot,
                                        CopilotServerOperations server,
                                        ProjectsServerOperations projectServer)
   {
      events_ = events;
      session_ = session;
      prefs_ = prefs;
      copilot_ = copilot;
      server_ = server;
      projectServer_ = projectServer;

      // Create assistant selector - conditionally include Posit AI option
      // Note: Use "(Default)" instead of "(None)" since project settings inherit from global
      boolean paiEnabled = PaiUtil.isPaiEnabled(session_.getSessionInfo(), prefs_);
      String[] assistantLabels;
      String[] assistantValues;
      if (paiEnabled)
      {
         assistantLabels = new String[] {
               constants_.defaultInParentheses(),
               prefsConstants_.rstudioAssistantEnum_posit_ai(),
               prefsConstants_.rstudioAssistantEnum_copilot()
         };
         assistantValues = new String[] {
               UserPrefsAccessor.RSTUDIO_ASSISTANT_NONE,
               UserPrefsAccessor.RSTUDIO_ASSISTANT_POSIT_AI,
               UserPrefsAccessor.RSTUDIO_ASSISTANT_COPILOT
         };
      }
      else
      {
         assistantLabels = new String[] {
               constants_.defaultInParentheses(),
               prefsConstants_.rstudioAssistantEnum_copilot()
         };
         assistantValues = new String[] {
               UserPrefsAccessor.RSTUDIO_ASSISTANT_NONE,
               UserPrefsAccessor.RSTUDIO_ASSISTANT_COPILOT
         };
      }
      selAssistant_ = new SelectWidget(
            constants_.assistantSelectLabel(),
            assistantLabels,
            assistantValues,
            false,
            true,
            false);
      selAssistant_.setValue(prefs_.rstudioAssistant().getGlobalValue());

      // Container for dynamic assistant-specific content
      assistantDetailsPanel_ = new SimplePanel();

      lblCopilotStatus_ = new Label(constants_.copilotLoadingMessage());
      
      statusButtons_ = new ArrayList<SmallButton>();
      
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
   
   private void initDisplay(RProjectOptions options)
   {
      add(headerLabel(constants_.assistantDisplayName()));

      // Add assistant selector
      add(selAssistant_);

      // Create the three panels
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

      // Show what global assistant is currently active
      String globalAssistant = prefs_.rstudioAssistant().getGlobalValue();
      String globalAssistantName;
      if (globalAssistant.equals(UserPrefsAccessor.RSTUDIO_ASSISTANT_COPILOT))
         globalAssistantName = prefsConstants_.rstudioAssistantEnum_copilot();
      else if (globalAssistant.equals(UserPrefsAccessor.RSTUDIO_ASSISTANT_POSIT_AI))
         globalAssistantName = prefsConstants_.rstudioAssistantEnum_posit_ai();
      else
         globalAssistantName = prefsConstants_.rstudioAssistantEnum_none();

      Label lblInfo = new Label(constants_.projectAssistantDefaultInfo(globalAssistantName));
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

   private VerticalPanel createCopilotPanel(RProjectOptions options)
   {
      VerticalPanel panel = new VerticalPanel();

      if (session_.getSessionInfo().getCopilotEnabled())
      {
         HorizontalPanel statusPanel = new HorizontalPanel();
         statusPanel.add(lblCopilotStatus_);
         for (SmallButton button : statusButtons_)
            statusPanel.add(button);
         panel.add(spaced(statusPanel));
      }
      else
      {
         panel.add(new Label(constants_.copilotDisabledByAdmin()));
      }

      return panel;
   }
   
   private void initModel()
   {
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
   }
   
   private void refresh()
   {
      hideButtons();
      
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
               }
               else if (CopilotResponseTypes.CopilotAgentNotRunningReason.isError(response.reason))
               {
                  int reason = (int) response.reason.valueOf();
                  lblCopilotStatus_.setText(CopilotResponseTypes.CopilotAgentNotRunningReason.reasonToString(reason));
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

   @Override
   protected void initialize(RProjectOptions options)
   {
      options_ = options;

      // Load assistant selection from project options
      // Map "default" (or null/empty) to "none" which represents "(Default)" in UI
      String projectAssistant = options.getAssistantOptions().assistant;
      if (projectAssistant == null || projectAssistant.isEmpty() || projectAssistant.equals("default"))
         selAssistant_.setValue(UserPrefsAccessor.RSTUDIO_ASSISTANT_NONE);
      else
         selAssistant_.setValue(projectAssistant);

      initDisplay(options);
      initModel();

      // Only refresh Copilot status if Copilot is the selected assistant
      if (selAssistant_.getValue().equals(UserPrefsAccessor.RSTUDIO_ASSISTANT_COPILOT))
      {
         refresh();
         copilotRefreshed_ = true;
      }
   }
   
   @Override
   public ImageResource getIcon()
   {
      // TODO: Replace with proper Copilot icon
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconCodeEditing2x());
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
   private HandlerRegistration copilotStatusHandler_;
   private boolean copilotStarted_ = false; // did Copilot get started while the dialog was open?
   private boolean copilotRefreshed_ = false; // has Copilot status been refreshed for this pane instance?

   // Assistant panels (created in initDisplay)
   private VerticalPanel nonePanel_;
   private VerticalPanel positAiPanel_;
   private VerticalPanel copilotPanel_;
   private VerticalPanel copilotTosPanel_;

   // UI
   private final SelectWidget selAssistant_;
   private final SimplePanel assistantDetailsPanel_;
   private final Label lblCopilotStatus_;
   private final List<SmallButton> statusButtons_;
   private final SmallButton btnSignIn_;
   private final SmallButton btnSignOut_;
   private final SmallButton btnActivate_;
   private final SmallButton btnRefresh_;
   private final SmallButton btnDiagnostics_;
   private final HelpLink linkCopilotTos_;
   private final Label lblCopilotTos_;
   
   // Injected
   private final EventBus events_;
   private final Session session_;
   private final UserPrefs prefs_;
   private final Copilot copilot_;
   private final CopilotServerOperations server_;
   private final ProjectsServerOperations projectServer_;
   
   private static final AssistantPreferencesPane.Resources RES = AssistantPreferencesPane.RES;
   private static final UserPrefsAccessorConstants prefsConstants_ = GWT.create(UserPrefsAccessorConstants.class);
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
   
   
}
