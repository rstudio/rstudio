/*
 * AceThemes.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
   public static final String AMBIANCE = "Ambiance";
   public static final String CHAOS = "Chaos";
   public static final String CHROME = "Chrome";
   public static final String CLOUDS_MIDNIGHT = "Clouds Midnight";
   public static final String CLOUDS = "Clouds";
   public static final String COBALT = "Cobalt";
   public static final String CRIMSON_EDITOR = "Crimson Editor";
   public static final String DAWN = "Dawn";
   public static final String DREAMWEAVER = "Dreamweaver";
   public static final String ECLIPSE = "Eclipse";
   public static final String IDLE_FINGERS = "Idle Fingers";
   public static final String KATZENMILCH = "Katzenmilch";
   public static final String KR_THEME = "Kr Theme";
   public static final String MERBIVORE_SOFT = "Merbivore Soft";
   public static final String MERBIVORE = "Merbivore";
   public static final String MONO_INDUSTRIAL = "Mono Industrial";
   public static final String MONOKAI = "Monokai";
   public static final String PASTEL_ON_DARK = "Pastel On Dark";
   public static final String SOLARIZED_DARK = "Solarized Dark";
   public static final String SOLARIZED_LIGHT = "Solarized Light";
   public static final String TEXTMATE = "TextMate";
   public static final String TOMORROW_NIGHT_BLUE = "Tomorrow Night Blue";
   public static final String TOMORROW_NIGHT_BRIGHT = "Tomorrow Night Bright";
   public static final String TOMORROW_NIGHT_EIGHTIES = "Tomorrow Night 80s";
   public static final String TOMORROW_NIGHT = "Tomorrow Night";
   public static final String TOMORROW = "Tomorrow";
   public static final String TWILIGHT = "Twilight";
   public static final String VIBRANT_INK = "Vibrant Ink";
   public static final String XCODE = "Xcode";
   
   @Inject
   public AceThemes(AceThemeResources res,
                    final Provider<UIPrefs> prefs,
                    EventBus eventBus)
   {
      themes_ = new ArrayList<String>();
      themesByName_ = new HashMap<String, String>();
      
      addTheme(AMBIANCE, res.ambiance());
      addTheme(CHAOS, res.chaos());
      addTheme(CHROME, res.chrome());
      addTheme(CLOUDS_MIDNIGHT, res.clouds_midnight());
      addTheme(CLOUDS, res.clouds());
      addTheme(COBALT, res.cobalt());
      addTheme(CRIMSON_EDITOR, res.crimson_editor());
      addTheme(DAWN, res.dawn());
      addTheme(DREAMWEAVER, res.dreamweaver());
      addTheme(ECLIPSE, res.eclipse());
      addTheme(IDLE_FINGERS, res.idle_fingers());
      addTheme(KATZENMILCH, res.katzenmilch());
      addTheme(KR_THEME, res.kr_theme());
      addTheme(MERBIVORE_SOFT, res.merbivore_soft());
      addTheme(MERBIVORE, res.merbivore());
      addTheme(MONO_INDUSTRIAL, res.mono_industrial());
      addTheme(MONOKAI, res.monokai());
      addTheme(PASTEL_ON_DARK, res.pastel_on_dark());
      addTheme(SOLARIZED_DARK, res.solarized_dark());
      addTheme(SOLARIZED_LIGHT, res.solarized_light());
      addTheme(TEXTMATE, res.textmate());
      addTheme(TOMORROW_NIGHT_BLUE, res.tomorrow_night_blue());
      addTheme(TOMORROW_NIGHT_BRIGHT, res.tomorrow_night_bright());
      addTheme(TOMORROW_NIGHT_EIGHTIES, res.tomorrow_night_eighties());
      addTheme(TOMORROW_NIGHT, res.tomorrow_night());
      addTheme(TOMORROW, res.tomorrow());
      addTheme(TWILIGHT, res.twilight());
      addTheme(VIBRANT_INK, res.vibrant_ink());
      addTheme(XCODE, res.xcode());

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
   private final String defaultThemeName_ = TEXTMATE;

   private LinkElement currentStyleEl_;
}
