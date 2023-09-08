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
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesPane;
import org.rstudio.studio.client.projects.ui.prefs.YesNoAskDefault;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessorConstants;
import org.rstudio.studio.client.workbench.prefs.views.CopilotPreferencesPane;

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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ProjectCopilotPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectCopilotPreferencesPane(Session session,
                                        UserPrefs prefs,
                                        AriaLiveService ariaLive,
                                        Copilot copilot,
                                        CopilotServerOperations server)
   {
      session_ = session;
      prefs_ = prefs;
      copilot_ = copilot;
      server_ = server;
      
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
      
      linkCopilotTos_ = new HelpLink(
            "GitHub Copilot: Terms of Service",
            "github-copilot-terms-of-service",
            false);
      
      lblCopilotTos_ = new Label(
            "By using GitHub Copilot, you agree to abide by the terms of service.");
      lblCopilotTos_.addStyleName(RES.styles().copilotTosLabel());
   }
   
   private void initDisplay()
   {
      add(headerLabel("GitHub Copilot (Preview)"));
      
      if (session_.getSessionInfo().getCopilotEnabled())
      {
         LayoutGrid grid = new LayoutGrid(2, 2);
         
         grid.setWidget(0, 0, new FormLabel(constants.copilotEnabledTitle(), copilotEnabled_));
         grid.setWidget(0, 1, copilotEnabled_);
         
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
               copilot_.ensureAgentInstalled(new CommandWithArg<Boolean>()
               {
                  @Override
                  public void execute(Boolean isInstalled)
                  {
                     if (isInstalled)
                     {
                        // TODO: how to synchronize the preferences eagerly here?
                        //  prefs_.copilotEnabled().setProjectValue(true);
                        //  prefs_.writeUserPrefs((completed) ->
                        //  {
                        //     refresh();
                        //  });
                     }
                     else
                     {
                        copilotEnabled_.setValue(copilotEnabled);
                     }
                  }
               });
            }
            else
            {
               // TODO: how to synchronize the preference eagerly here?
               // prefs_.copilotEnabled().setProjectValue(false);
               // prefs_.writeUserPrefs((completed) ->
               // {
               //    refresh();
               // });
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
      // TODO
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      options_ = options;
      
      initDisplay();
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

   @Override
   public RestartRequirement onApply(RProjectOptions prefs)
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   public static final CopilotPreferencesPane.Resources RES = CopilotPreferencesPane.RES;
   
   private RProjectOptions options_;
   
   private final Session session_;
   private final UserPrefs prefs_;
   private final Copilot copilot_;
   private final CopilotServerOperations server_;
   private final Label lblCopilotStatus_;
   private final List<SmallButton> statusButtons_;
   private final SmallButton btnSignIn_;
   private final SmallButton btnSignOut_;
   private final SmallButton btnActivate_;
   private final SmallButton btnRefresh_;
   private final HelpLink linkCopilotTos_;
   private final Label lblCopilotTos_;
   
   private final YesNoAskDefault copilotEnabled_;
   private final YesNoAskDefault copilotIndexingEnabled_;
   
   private static final UserPrefsAccessorConstants constants = GWT.create(UserPrefsAccessorConstants.class);
   
   
}