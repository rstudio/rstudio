/*
 * AceThemes.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.themes;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LinkElement;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.resources.StaticDataResource;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import java.util.ArrayList;
import java.util.HashMap;

@Singleton
public class AceThemes
{
   @Inject
   public AceThemes(AceThemeResources res,
                    final Provider<UIPrefs> prefs,
                    EventBus eventBus)
   {
      themes_ = new ArrayList<String>();
      themesByName_ = new HashMap<String, String>();

      addTheme("TextMate", res.textmate());
      addTheme("Eclipse", res.eclipse());
      addTheme("Tomorrow", res.tomorrow());
      addTheme("Cobalt", res.cobalt());
      addTheme("Idle Fingers", res.idle_fingers());
      addTheme("Twilight", res.twilight());
      addTheme("Tomorrow Night", res.tomorrow_night());
      addTheme("Tomorrow Night Blue", res.tomorrow_night_blue());
      addTheme("Tomorrow Night Bright", res.tomorrow_night_bright());
      addTheme("Tomorrow Night 80's", res.tomorrow_night_eighties());
      
      addTheme("Solarized", res.solarized());
      addTheme("Solarized Dark", res.solarizedDark());
    
      
      
     

      prefs.get().theme().bind(new CommandWithArg<String>()
      {
         public void execute(String theme)
         {
            applyTheme(theme);
         }
      });
   }

   public String[] getThemeNames()
   {
      return themes_.toArray(new String[themes_.size()]);
   }

   public String getThemeUrl(String name)
   {
      String url = themesByName_.get(name);
      return url != null ? url : themesByName_.get(defaultThemeName_);
   }

   private void addTheme(String name, StaticDataResource resource)
   {
      themes_.add(name);
      themesByName_.put(name, resource.getSafeUri().asString());
   }

   private void applyTheme(String themeName)
   {
      if (currentStyleEl_ != null)
         currentStyleEl_.removeFromParent();

      currentStyleEl_ = Document.get().createLinkElement();
      currentStyleEl_.setType("text/css");
      currentStyleEl_.setRel("stylesheet");
      currentStyleEl_.setHref(getThemeUrl(themeName));
      Document.get().getBody().appendChild(
            currentStyleEl_);
   }

   public String getEffectiveThemeName(String themeName)
   {
      return themesByName_.containsKey(themeName)
             ? themeName
             : defaultThemeName_;
   }

   private final ArrayList<String> themes_;
   private final HashMap<String, String> themesByName_;
   private final String defaultThemeName_ = "TextMate";

   private LinkElement currentStyleEl_;
}
