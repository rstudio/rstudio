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
   public static final String CHROME = "Chrome";
   public static final String CLOUDS = "Clouds";
   public static final String CLOUDS_MIDNIGHT = "Clouds Midnight";
   public static final String COBALT = "Cobalt";
   public static final String CRIMSON_EDITOR = "Crimson Editor";
   public static final String DAWN = "Dawn";
   public static final String DREAMWEAVER = "Dreamweaver";
   public static final String ECLIPSE = "Eclipse";
   public static final String IDLE_FINGERS = "Idle Fingers";
   public static final String KR_THEME = "KR Theme";
   public static final String MERBIVORE = "Merbivore";
   public static final String MERBIVORE_SOFT = "Merbivore Soft";
   public static final String MONO_INDUSTRIAL = "Mono Industrial";
   public static final String MONOKAI = "Monokai";
   public static final String NEON = "Neon";
   public static final String PASTEL_ON_DARK = "Pastel on Dark";
   public static final String SOLARIZED_DARK = "Solarized Dark";
   public static final String SOLARIZED_LIGHT = "Solarized Light";
   public static final String TEXTMATE = "TextMate";
   public static final String TOMORROW = "Tomorrow";
   public static final String TOMORROW_NIGHT = "Tomorrow Night";
   public static final String TOMORROW_NIGHT_80S = "Tomorrow Night 80's";
   public static final String TOMORROW_NIGHT_BLUE = "Tomorrow Night Blue";
   public static final String TOMORROW_NIGHT_BRIGHT = "Tomorrow Night Bright";
   public static final String TWILIGHT = "Twilight";
   public static final String VIBRANT_INK = "Vibrant Ink";

   @Inject
   public AceThemes(AceThemeResources res,
                    final Provider<UIPrefs> prefs,
                    EventBus eventBus)
   {
      themes_ = new ArrayList<String>();
      themesByName_ = new HashMap<String, String>();

      addTheme(CHROME, res.chrome());
      addTheme(CLOUDS, res.clouds());
      addTheme(CLOUDS_MIDNIGHT, res.clouds_midnight());
      addTheme(COBALT, res.cobalt());
      addTheme(CRIMSON_EDITOR, res.crimson_editor());
      addTheme(DAWN, res.dawn());
      addTheme(DREAMWEAVER, res.dreamweaver());
      addTheme(ECLIPSE, res.eclipse());
      addTheme(IDLE_FINGERS, res.idle_fingers());
      addTheme(KR_THEME, res.kr_theme());
      addTheme(MERBIVORE, res.merbivore());
      addTheme(MERBIVORE_SOFT, res.merbivore_soft());
      addTheme(MONO_INDUSTRIAL, res.mono_industrial());
      addTheme(MONOKAI, res.monokai());
      addTheme(NEON, res.neon());
      addTheme(PASTEL_ON_DARK, res.pastel_on_dark());
      addTheme(SOLARIZED_DARK, res.solarized_dark());
      addTheme(SOLARIZED_LIGHT, res.solarized_light());
      addTheme(TEXTMATE, res.textmate());
      addTheme(TOMORROW, res.tomorrow());
      addTheme(TOMORROW_NIGHT, res.tomorrow_night());
      addTheme(TOMORROW_NIGHT_80S, res.tomorrow_night_eighties());
      addTheme(TOMORROW_NIGHT_BLUE, res.tomorrow_night_blue());
      addTheme(TOMORROW_NIGHT_BRIGHT, res.tomorrow_night_bright());
      addTheme(TWILIGHT, res.twilight());
      addTheme(VIBRANT_INK, res.vibrant_ink());


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
