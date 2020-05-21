/*
 * ApplicationThemes.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.application;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.application.events.ComputeThemeColorsEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ThemeChangedEvent;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.model.ThemeServerOperations;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ApplicationThemes implements ThemeChangedEvent.Handler,
                                          ComputeThemeColorsEvent.Handler
{
   @Inject
   public ApplicationThemes(Provider<UserPrefs> pUserPrefs,
                            Provider<UserState> pUserState,
                            Provider<AceThemes> pAceThemes,
                            EventBus events,
                            ThemeServerOperations server)
   {
      userPrefs_ = pUserPrefs;
      userState_ = pUserState;
      pAceThemes_ = pAceThemes;
      events_ = events;
      server_ = server;
      
      events_.addHandler(ThemeChangedEvent.TYPE, this);
      events_.addHandler(ComputeThemeColorsEvent.TYPE, this);
   }

   public void initializeThemes(Element root)
   {
      // Save reference to root element
      root_ = root;
      
      // Bind theme change handlers to the uiPrefs and immediately fire a theme changed event to
      // set the initial theme.
      userState_.get().theme().bind(theme ->events_.fireEvent(new ThemeChangedEvent()));
      events_.fireEvent(new ThemeChangedEvent());
      
      // Ensure creation of the Ace themes instance
      pAceThemes_.get();
   }

   @Override
   public void onComputeThemeColors(ComputeThemeColorsEvent event)
   {
      // Establish defaults
      String foreground = "#000000";
      String background = "#FFFFFF";

      // We're very exception sensitive here since this code runs in a
      // WaitForMethod (and therefore can hold the server while we wait for it
      // to return).
      try
      {
         // Create a sampler element to read runtime styles; hide it and position
         // it offscreen to ensure it doesn't flicker.
         Element sampler = Document.get().createElement("div");
         sampler.addClassName("ace_editor ace_content");
         sampler.getStyle().setVisibility(Visibility.HIDDEN);
         sampler.getStyle().setPosition(Position.ABSOLUTE);
         sampler.getStyle().setTop(-10000, Unit.PX);
         sampler.getStyle().setLeft(-10000, Unit.PX);

         // Append the sampler element to the root panel, read its content, and
         // remove it.
         root_.appendChild(sampler);
         Style style = DomUtils.getComputedStyles(sampler);
         foreground = style.getColor();
         background = style.getBackgroundColor();
         root_.removeChild(sampler);
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      
      // Send the server the computed colors
      server_.setComputedThemeColors(foreground, background, new VoidServerRequestCallback());
   }

   @Override
   public void onThemeChanged(ThemeChangedEvent event)
   {
      RStudioThemes.initializeThemes(userPrefs_.get(),
                                     userState_.get(),
                                     Document.get(),
                                     root_);
   }
   
   private final Provider<UserPrefs> userPrefs_;
   private final Provider<UserState> userState_;
   private final Provider<AceThemes> pAceThemes_;
   private final EventBus events_;
   private final ThemeServerOperations server_;
   private Element root_;
}
