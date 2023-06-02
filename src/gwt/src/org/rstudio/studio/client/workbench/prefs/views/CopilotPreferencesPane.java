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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JSON;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.copilot.model.CopilotConstants;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotStatusResponse;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

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
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;


public class CopilotPreferencesPane extends PreferencesPane
{
   @Inject
   public CopilotPreferencesPane(UserPrefs prefs,
                                 AriaLiveService ariaLive,
                                 Copilot copilot,
                                 CopilotServerOperations server)
   {
      prefs_ = prefs;
      copilot_ = copilot;
      server_ = server;
      
      cbCopilotEnabled_ = checkboxPref(prefs_.copilotEnabled(), true);
      lblCopilotStatus_ = new Label("(Loading...)");
      
      btnSignIn_ = new SmallButton("Sign In");
      btnSignIn_.addStyleName(RES.styles().button());
      
      btnSignOut_ = new SmallButton("Sign Out");
      btnSignOut_.addStyleName(RES.styles().button());
      
      linkCopilotTos_ = new HelpLink(
            "GitHub Copilot: Terms of Service",
            "github-copilot-terms-of-service");
      
      lblCopilotTos_ = new Label(
            "By using GitHub Copilot, you agree to abide by the terms of service.");
      lblCopilotTos_.addStyleName(RES.styles().copilotTosLabel());
   }
   
   private void initDisplay()
   {
      add(headerLabel("GitHub Copilot"));
      add(cbCopilotEnabled_);
      
      HorizontalPanel statusPanel = new HorizontalPanel();
      statusPanel.add(lblCopilotStatus_);
      statusPanel.add(btnSignIn_);
      statusPanel.add(btnSignOut_);
      add(spaced(statusPanel));
      
      add(headerLabel("Copilot Completions"));
      add(numericPref("Show code suggestions after keyboard idle (ms):", 10, 2000, prefs_.copilotCompletionsDelay()));
      
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
   }
   
   private void refresh()
   {
      reset();
      
      server_.copilotStatus(new ServerRequestCallback<CopilotStatusResponse>()
      {
         @Override
         public void onResponseReceived(CopilotStatusResponse response)
         {
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
               btnSignOut_.setEnabled(true);
               btnSignOut_.setVisible(true);
               lblCopilotStatus_.setText("You are currently signed in as: " + response.result.user);
            }
            else if (response.result.status == CopilotConstants.STATUS_NOT_SIGNED_IN)
            {
               btnSignIn_.setEnabled(true);
               btnSignIn_.setVisible(true);
               lblCopilotStatus_.setText("You are not currently signed in.");
            }
            else
            {
               // TODO
               lblCopilotStatus_.setText(JSON.stringify(response));
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
      btnSignIn_.setEnabled(false);
      btnSignIn_.setVisible(false);
      
      btnSignOut_.setEnabled(false);
      btnSignOut_.setVisible(false);
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
      initDisplay();
      initModel();
      refresh();
   }
   
   public interface Styles extends CssResource
   {
      String button();
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

   private static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }
   
   private final UserPrefs prefs_;
   private final Copilot copilot_;
   private final CopilotServerOperations server_;
   private final CheckBox cbCopilotEnabled_;
   private final Label lblCopilotStatus_;
   private final SmallButton btnSignIn_;
   private final SmallButton btnSignOut_;
   private final HelpLink linkCopilotTos_;
   private final Label lblCopilotTos_;

   
}