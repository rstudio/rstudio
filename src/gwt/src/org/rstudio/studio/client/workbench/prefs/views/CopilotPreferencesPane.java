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
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.inject.Inject;


public class CopilotPreferencesPane extends PreferencesPane
{
   @Inject
   public CopilotPreferencesPane(Copilot copilot,
                                 UserPrefs prefs,
                                 AriaLiveService ariaLive)
   {
      copilot_ = copilot;
      prefs_ = prefs;
      
      cbCopilotEnabled_ = checkboxPref(prefs_.copilotEnabled(), true);
   }
   
   private void initDisplay()
   {
      add(headerLabel("GitHub Copilot"));
      add(cbCopilotEnabled_);
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
                     if (!isInstalled)
                     {
                        cbCopilotEnabled_.setValue(false);
                     }
                  }
               });
            }
         }
      });
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
   }
   
   public interface Styles extends CssResource
   {
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
   
   private final Copilot copilot_;
   private final UserPrefs prefs_;
   private final CheckBox cbCopilotEnabled_;

   
}