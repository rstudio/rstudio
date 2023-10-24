/*
 * CopilotPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JSON;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.HelpLink;
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
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessorConstants;
import org.rstudio.studio.client.workbench.prefs.views.events.CopilotEnabledEvent;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
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
      
      RestartRequirement requirement = super.onApply(prefs);
      if (initialCopilotIndexingEnabled_ != prefs.copilotIndexingEnabled().getGlobalValue())
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
      
      btnProjectOptions_ = new SmallButton("Project Options...");
      btnProjectOptions_.addStyleName(RES.styles().button());
      statusButtons_.add(btnProjectOptions_);
      
      cbCopilotEnabled_ = checkboxPref(prefs_.copilotEnabled(), true);
      cbCopilotIndexingEnabled_ = checkboxPref(prefs_.copilotIndexingEnabled(), true);
      
      selCopilotTabKeyBehavior_ = new SelectWidget(
            constants.copilotTabKeyBehaviorTitle(),
            new String[] {
                  constants.copilotTabKeyBehaviorEnum_suggestion(),
                  constants.copilotTabKeyBehaviorEnum_completions()
            },
            new String[] {
                  UserPrefsAccessor.COPILOT_TAB_KEY_BEHAVIOR_SUGGESTION,
                  UserPrefsAccessor.COPILOT_TAB_KEY_BEHAVIOR_COMPLETIONS
            },
            false,
            true,
            false);
      
      selCopilotTabKeyBehavior_.setValue(prefs_.copilotTabKeyBehavior().getGlobalValue());
      
      previewBlurb_ = new HTML(
            "<p>This feature is in preview. If you'd like to provide feedback or report an issue, please " +
            "<a target=\"_blank\" href=\"https://github.com/rstudio/rstudio/issues\">file an issue</a> " +
            "on the RStudio GitHub repository.</p>");
      previewBlurb_.addStyleName(RES.styles().copilotPreviewBlurb());
      
      linkCopilotTos_ = new HelpLink(
            "GitHub Copilot: Terms of Service",
            "github-copilot-terms-of-service",
            false);
      
      lblCopilotTos_ = new Label(
            "By using GitHub Copilot, you agree to abide by their terms of service.");
      lblCopilotTos_.addStyleName(RES.styles().copilotTosLabel());
   }
   
   private void initDisplay()
   {
      add(headerLabel("GitHub Copilot (Preview)"));
      
      if (session_.getSessionInfo().getCopilotEnabled())
      {
         add(cbCopilotEnabled_);

         HorizontalPanel statusPanel = new HorizontalPanel();
         statusPanel.add(lblCopilotStatus_);
         for (SmallButton button : statusButtons_)
            statusPanel.add(button);
         add(spaced(statusPanel));
         
         add(headerLabel("Copilot Indexing"));
         add(spaced(cbCopilotIndexingEnabled_));

         add(spacedBefore(headerLabel("Copilot Completions")));
         // add(checkboxPref(prefs_.copilotAllowAutomaticCompletions()));
         // add(selCopilotTabKeyBehavior_);
         add(numericPref("Show code suggestions after keyboard idle (ms):", 10, 5000, prefs_.copilotCompletionsDelay()));
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
      cbCopilotEnabled_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            boolean enabled = event.getValue();
            
            if (enabled)
            {
               copilot_.ensureAgentInstalled(new CommandWithArg<Boolean>()
               {
                  @Override
                  public void execute(Boolean isInstalled)
                  {
                     if (isInstalled)
                     {
                        // Eagerly change the preference here, so that we can
                        // respond to changes in the agent status.
                        prefs_.copilotEnabled().setGlobalValue(true);
                        prefs_.writeUserPrefs((completed) ->
                        {
                           refresh();
                        });
                     }
                     else
                     {
                        // Installation of the Copilot agent failed;
                        // revert the checkbox state.
                        cbCopilotEnabled_.setValue(false);
                     }
                  }
               });
            }
            else
            {
               // Eagerly change the preference here, so that we can
               // respond to changes in the agent status.
               prefs_.copilotEnabled().setGlobalValue(false);
               prefs_.writeUserPrefs((completed) ->
               {
                  refresh();
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
               if (projectOptions_ != null && projectOptions_.getCopilotOptions().copilot_enabled == RProjectConfig.NO_VALUE)
               {
                  lblCopilotStatus_.setText("GitHub Copilot has been disabled in this project.");
                  showButtons(btnProjectOptions_);
               }
               else if (prefs_.copilotEnabled().getValue())
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
               showButtons(btnSignOut_, btnRefresh_);
               lblCopilotStatus_.setText("You are currently signed in as: " + response.result.user);
            }
            else if (response.result.status == CopilotConstants.STATUS_NOT_AUTHORIZED)
            {
               showButtons(btnActivate_, btnSignOut_, btnRefresh_);
               lblCopilotStatus_.setText(
                     "You are currently signed in as " + response.result.user + ", but " +
                     "you haven't yet activated your GitHub Copilot account.");
            }
            else if (response.result.status == CopilotConstants.STATUS_NOT_SIGNED_IN)
            {
               showButtons(btnSignIn_, btnRefresh_);
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

   private void reset()
   {
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
      return "Copilot";
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      initialCopilotIndexingEnabled_ = prefs.copilotIndexingEnabled().getGlobalValue();
      
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
      String copilotTosLabel();
      String copilotPreviewBlurb();
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
   private boolean initialCopilotIndexingEnabled_;
   private RProjectOptions projectOptions_;
 
   // UI
   private final Label lblCopilotStatus_;
   private final CheckBox cbCopilotEnabled_;
   private final CheckBox cbCopilotIndexingEnabled_;
   private final List<SmallButton> statusButtons_;
   private final SmallButton btnSignIn_;
   private final SmallButton btnSignOut_;
   private final SmallButton btnActivate_;
   private final SmallButton btnRefresh_;
   private final SmallButton btnProjectOptions_;
   private final SelectWidget selCopilotTabKeyBehavior_;
   private final HelpLink linkCopilotTos_;
   private final Label lblCopilotTos_;
   private final HTML previewBlurb_;
   
   // Injected
   private final EventBus events_;
   private final Session session_;
   private final UserPrefs prefs_;
   private final Commands commands_;
   private final Copilot copilot_;
   private final CopilotServerOperations server_;
   private final ProjectsServerOperations projectServer_;
   
   private static final UserPrefsAccessorConstants constants = GWT.create(UserPrefsAccessorConstants.class);
   
}
