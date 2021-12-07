/*
 * QuartoPreferencesPane.java
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class QuartoPreferencesPane extends PreferencesPane
{
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
   public static final String NAME = constants_.name();
   
   @Inject
   public QuartoPreferencesPane(PreferencesDialogResources res, Session session)
   {
      res_ = res;
      session_ = session;
      PreferencesDialogBaseResources baseRes = PreferencesDialogBaseResources.INSTANCE;
      
      add(headerLabel(constants_.name()));
      
      add(new Label(constants_.quartoPreviewLabel(), true));
      
      chkEnableQuarto_ = new CheckBox(constants_.enableQuartoPreviewCheckboxLabel());
      add(spacedBefore(chkEnableQuarto_));
      lblQuartoVersion_ = new Label("", true);
      lblQuartoVersion_.setVisible(false);
      lblQuartoVersion_.addStyleName(res_.styles().checkBoxAligned());
      add(spacedBefore(lblQuartoVersion_));      
      add(lblQuartoPath_ = new Label());
      lblQuartoPath_.addStyleName(baseRes.styles().infoLabel());
      lblQuartoPath_.addStyleName(res_.styles().checkBoxAligned());

      lblQuartoPath_.setVisible(false);
      
      HelpLink helpLink = new HelpLink(constants_.helpLinkCaption(), "https://quarto.org", false, false);
      nudgeRight(helpLink);
      helpLink.addStyleName(res_.styles().newSection());
      add(helpLink);
      
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      QuartoConfig config = session_.getSessionInfo().getQuartoConfig();
      
      if (prefs.quartoEnabled().getValue().equals(UserPrefs.QUARTO_ENABLED_ENABLED))
      {
         chkEnableQuarto_.setValue(true);
      }
      else if (prefs.quartoEnabled().getValue().equals(UserPrefs.QUARTO_ENABLED_DISABLED))
      {
         chkEnableQuarto_.setValue(false);
      }
      else // auto
      {
         chkEnableQuarto_.setValue(
           !config.user_installed.isEmpty()
         );
      }
      
     
      // let user know what version of quarto we are using (only 
      // show version info for non-embedded versions)
      lblQuartoVersion_.setText("Quarto v" + config.version);
      lblQuartoVersion_.setVisible(chkEnableQuarto_.getValue() && 
            !config.user_installed.isEmpty());
      lblQuartoPath_.setText(config.user_installed);
      lblQuartoPath_.setVisible(lblQuartoVersion_.isVisible());
      
   
      // only write quarto pref if the user interacts with it
      chkEnableQuarto_.addValueChangeHandler(event -> {
         boolean showVersion = !config.user_installed.isEmpty() && event.getValue();
         lblQuartoVersion_.setVisible(showVersion);
         lblQuartoPath_.setVisible(showVersion);
         writeEnableQuarto_ = event.getValue()  
            ? UserPrefs.QUARTO_ENABLED_ENABLED 
            : UserPrefs.QUARTO_ENABLED_DISABLED;
      });
   }
   
   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      if (writeEnableQuarto_ != null)
      {
         prefs.quartoEnabled().setGlobalValue(writeEnableQuarto_);
         return new RestartRequirement(true, true, true);
      }
      else
      {
         return new RestartRequirement();
      }
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconQuarto2x());
   }

   @Override
   public String getName()
   {
      return NAME;
   }
   
   
   private final Session session_;
   private final PreferencesDialogResources res_;
   
   private final CheckBox chkEnableQuarto_;
   private final Label lblQuartoVersion_;
   private final Label lblQuartoPath_;
   private String writeEnableQuarto_ = null;

}
