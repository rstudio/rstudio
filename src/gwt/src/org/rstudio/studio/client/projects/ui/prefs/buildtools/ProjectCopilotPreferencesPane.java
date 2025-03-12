/*
 * ProjectCopilotPreferencesPane.java
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JSON;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectCopilotOptions;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesPane;
import org.rstudio.studio.client.projects.ui.prefs.YesNoAskDefault;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.copilot.model.CopilotConstants;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotStatusResponse;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessorConstants;
import org.rstudio.studio.client.workbench.prefs.views.CopilotPreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.events.CopilotEnabledEvent;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ProjectCopilotPreferencesPane extends ProjectPreferencesPane
{
   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      RProjectCopilotOptions copilotOptions = options.getCopilotOptions();
      copilotOptions.copilot_enabled = copilotEnabled_.getValue();
      copilotOptions.copilot_indexing_enabled = copilotIndexingEnabled_.getValue();
      
      RestartRequirement requirement = new RestartRequirement();
      if (options_.getCopilotOptions().copilot_indexing_enabled != copilotIndexingEnabled_.getValue())
         requirement.setSessionRestartRequired(true);
      return requirement;
   }
   
   @Inject
   public ProjectCopilotPreferencesPane(EventBus events,
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
      
      LayoutGrid grid = new LayoutGrid(2, 2);
      grid.addStyleName(RESOURCES.styles().workspaceGrid());

      copilotEnabled_ = new YesNoAskDefault(false);
      grid.setWidget(0, 0, new FormLabel(prefsConstants_.copilotEnabledTitle(), copilotEnabled_));
      grid.setWidget(0, 1, copilotEnabled_);
      
      copilotIndexingEnabled_ = new YesNoAskDefault(false);
      grid.setWidget(1, 0, new FormLabel(prefsConstants_.copilotIndexingEnabledTitle(), copilotIndexingEnabled_));
      grid.setWidget(1, 1, copilotIndexingEnabled_);
      
      linkCopilotTos_ = new HelpLink(
            constants_.copilotTermsOfServiceLinkLabel(),
            "github-copilot-terms-of-service",
            false);
      
      lblCopilotTos_ = new Label(constants_.copilotTermsOfServiceLabel());
      lblCopilotTos_.addStyleName(RES.styles().copilotTosLabel());
   }
   
   private void initDisplay(RProjectOptions options)
   {
      add(headerLabel(constants_.copilotDisplayName()));
      
      if (session_.getSessionInfo().getCopilotEnabled())
      {
         HorizontalPanel statusPanel = new HorizontalPanel();
         statusPanel.add(lblCopilotStatus_);
         for (SmallButton button : statusButtons_)
            statusPanel.add(button);
         add(spaced(statusPanel));
         
         LayoutGrid grid = new LayoutGrid(2, 2);
    
         copilotEnabled_.setValue(options.getCopilotOptions().copilot_enabled);
         grid.setWidget(0, 0, new FormLabel(prefsConstants_.copilotEnabledTitle(), copilotEnabled_));
         grid.setWidget(0, 1, copilotEnabled_);
         
         copilotIndexingEnabled_.setValue(options.getCopilotOptions().copilot_indexing_enabled);
         grid.setWidget(1, 0, new FormLabel(prefsConstants_.copilotIndexingEnabledTitle(), copilotIndexingEnabled_));
         grid.setWidget(1, 1, copilotIndexingEnabled_);
         
         add(grid);
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
      copilotEnabled_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            int copilotEnabled = copilotEnabled_.getValue();
            if (copilotEnabled == YesNoAskDefault.YES_VALUE)
            {
               options_.getCopilotOptions().copilot_enabled = copilotEnabled;
               projectServer_.writeProjectOptions(options_, new ServerRequestCallback<Void>()
               {
                  @Override
                  public void onResponseReceived(Void response)
                  {
                     events_.fireEvent(new CopilotEnabledEvent(true, true));
                     refresh();
                  }
                  
                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                     
                  }
               });
            }
            else
            {
               options_.getCopilotOptions().copilot_enabled = copilotEnabled;
               projectServer_.writeProjectOptions(options_, new ServerRequestCallback<Void>()
               {
                  @Override
                  public void onResponseReceived(Void response)
                  {
                     refresh();
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);

                  }
               });
            }
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
               if (response.error != null)
               {
                  lblCopilotStatus_.setText(constants_.copilotStartupError());
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
      
      initDisplay(options);
      initModel();
      refresh();
   }
   
   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(CopilotPreferencesPane.RES.iconCopilotLight2x());
   }

   @Override
   public String getName()
   {
      return constants_.copilotPaneName();
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
   
   // UI
   private final YesNoAskDefault copilotEnabled_;
   private final YesNoAskDefault copilotIndexingEnabled_;
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
   
   private static final CopilotPreferencesPane.Resources RES = CopilotPreferencesPane.RES;
   private static final UserPrefsAccessorConstants prefsConstants_ = GWT.create(UserPrefsAccessorConstants.class);
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
   
   
}
