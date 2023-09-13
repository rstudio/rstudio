/*
 * ProjectCopilotPreferencesPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ProjectCopilotPreferencesPane extends ProjectPreferencesPane
{
   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setCopilotEnabled(copilotEnabled_.getValue());
      config.setCopilotIndexingEnabled(copilotIndexingEnabled_.getValue());
      return new RestartRequirement();
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
      
      lblCopilotStatus_ = new Label("(Loading...)");
      
      statusButtons_ = new ArrayList<SmallButton>();
      
      btnSignIn_ = new SmallButton("Sign In");
      btnSignIn_.addStyleName(RES.styles().button());
      statusButtons_.add(btnSignIn_);
      
      btnSignOut_ = new SmallButton("Sign Out");
      btnSignOut_.addStyleName(RES.styles().button());
      statusButtons_.add(btnSignOut_);
      
      btnActivate_ = new SmallButton("Activate");
      Roles.getLinkRole().set(btnActivate_.getElement());
      btnActivate_.getElement().setPropertyString("href", "https://github.com/settings/copilot");
      btnActivate_.addStyleName(RES.styles().button());
      statusButtons_.add(btnActivate_);
      
      btnRefresh_ = new SmallButton("Refresh");
      btnRefresh_.addStyleName(RES.styles().button());
      statusButtons_.add(btnRefresh_);
      
      LayoutGrid grid = new LayoutGrid(2, 2);
      grid.addStyleName(RESOURCES.styles().workspaceGrid());

      copilotEnabled_ = new YesNoAskDefault(false);
      grid.setWidget(0, 0, new FormLabel(constants.copilotEnabledTitle(), copilotEnabled_));
      grid.setWidget(0, 1, copilotEnabled_);
      
      copilotIndexingEnabled_ = new YesNoAskDefault(false);
      grid.setWidget(1, 0, new FormLabel(constants.copilotIndexingEnabledTitle(), copilotIndexingEnabled_));
      grid.setWidget(1, 1, copilotIndexingEnabled_);
      
      previewBlurb_ = new HTML(
            "<p>This feature is in preview. If you'd like to provide feedback or report an issue, please " +
            "<a target=\"_blank\" href=\"https://github.com/rstudio/rstudio/issues\">file an issue</a> " +
            "on the public RStudio GitHub repository.</p>");
      previewBlurb_.addStyleName(RES.styles().copilotPreviewBlurb());
      
      linkCopilotTos_ = new HelpLink(
            "GitHub Copilot: Terms of Service",
            "github-copilot-terms-of-service",
            false);
      
      lblCopilotTos_ = new Label(
            "By using GitHub Copilot, you agree to abide by their terms of service.");
      lblCopilotTos_.addStyleName(RES.styles().copilotTosLabel());
   }
   
   private void initDisplay(RProjectOptions options)
   {
      add(headerLabel("GitHub Copilot (Preview)"));
      
      if (session_.getSessionInfo().getCopilotEnabled())
      {
         HorizontalPanel statusPanel = new HorizontalPanel();
         statusPanel.add(lblCopilotStatus_);
         for (SmallButton button : statusButtons_)
            statusPanel.add(button);
         add(spaced(statusPanel));
         
         LayoutGrid grid = new LayoutGrid(2, 2);
    
         copilotEnabled_.setValue(options.getConfig().getCopilotEnabled());
         grid.setWidget(0, 0, new FormLabel(constants.copilotEnabledTitle(), copilotEnabled_));
         grid.setWidget(0, 1, copilotEnabled_);
         
         copilotIndexingEnabled_.setValue(options.getConfig().getCopilotIndexingEnabled());
         grid.setWidget(1, 0, new FormLabel(constants.copilotIndexingEnabledTitle(), copilotIndexingEnabled_));
         grid.setWidget(1, 1, copilotIndexingEnabled_);
         
         add(grid);
      }
      else
      {
         add(new Label("GitHub Copilot integration has been disabled by the administrator."));
      }
      
      VerticalPanel bottomPanel = new VerticalPanel();
      bottomPanel.getElement().getStyle().setBottom(0, Unit.PX);
      bottomPanel.getElement().getStyle().setPosition(Position.ABSOLUTE);
      bottomPanel.add(spaced(previewBlurb_));
      bottomPanel.add(spaced(lblCopilotTos_));
      bottomPanel.add(spaced(linkCopilotTos_));
      add(bottomPanel);
   }
   
   private void initModel()
   {
      final int initialCopilotEnabled = copilotEnabled_.getValue();
      copilotEnabled_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            int copilotEnabled = copilotEnabled_.getValue();
            if (copilotEnabled == YesNoAskDefault.YES_VALUE)
            {
               copilot_.ensureAgentInstalled(new CommandWithArg<Boolean>()
               {
                  @Override
                  public void execute(Boolean isInstalled)
                  {
                     if (isInstalled)
                     {
                        options_.getConfig().setCopilotEnabled(copilotEnabled);
                        projectServer_.writeProjectConfig(options_.getConfig(), new ServerRequestCallback<Void>()
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
                        copilotEnabled_.setValue(initialCopilotEnabled);
                     }
                  }
               });
            }
            else
            {
               options_.getConfig().setCopilotEnabled(copilotEnabled);
               projectServer_.writeProjectConfig(options_.getConfig(), new ServerRequestCallback<Void>()
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
               if (prefs_.copilotEnabled().getValue())
               {
                  lblCopilotStatus_.setText("The GitHub Copilot agent is not currently running.");
               }
               else
               {
                  lblCopilotStatus_.setText("The GitHub Copilot agent has not been enabled.");
               }
            }
            else if (response.result.status == CopilotConstants.STATUS_OK ||
                     response.result.status == CopilotConstants.STATUS_ALREADY_SIGNED_IN)
            {
               showButtons(btnSignOut_);
               lblCopilotStatus_.setText("You are currently signed in as: " + response.result.user);
            }
            else if (response.result.status == CopilotConstants.STATUS_NOT_AUTHORIZED)
            {
               showButtons(btnActivate_, btnRefresh_);
               lblCopilotStatus_.setText(
                     "You are currently signed in as " + response.result.user + ", but " +
                     "you haven't yet activated your GitHub Copilot account.");
            }
            else if (response.result.status == CopilotConstants.STATUS_NOT_SIGNED_IN)
            {
               showButtons(btnSignIn_);
               lblCopilotStatus_.setText("You are not currently signed in.");
            }
            else
            {
               String message =
                     "RStudio received a Copilot response that it does not understand.\n" +
                     JSON.stringify(response);
               
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
      return "Copilot";
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
   private final HelpLink linkCopilotTos_;
   private final Label lblCopilotTos_;
   private final HTML previewBlurb_;
   
   // Injected
   private final EventBus events_;
   private final Session session_;
   private final UserPrefs prefs_;
   private final Copilot copilot_;
   private final CopilotServerOperations server_;
   private final ProjectsServerOperations projectServer_;
   
   private static final CopilotPreferencesPane.Resources RES = CopilotPreferencesPane.RES;
   private static final UserPrefsAccessorConstants constants = GWT.create(UserPrefsAccessorConstants.class);
   
   
}
