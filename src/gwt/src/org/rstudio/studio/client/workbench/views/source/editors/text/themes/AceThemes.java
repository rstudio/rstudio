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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.resources.StaticDataResource;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeChangedEvent;

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
   public static final String MATERIAL = "Material";
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
                    EventBus events)
   {
      themes_ = new ArrayList<String>();
      themesByName_ = new HashMap<String, String>();
      darkThemes_ = new HashMap<String, Boolean>();
      events_ = events;
      prefs_ = prefs;
      
      addTheme(AMBIANCE, res.ambiance(), true);
      addTheme(CHAOS, res.chaos(), true);
      addTheme(CHROME, res.chrome(), false);
      addTheme(CLOUDS_MIDNIGHT, res.clouds_midnight(), true);
      addTheme(CLOUDS, res.clouds(), false);
      addTheme(COBALT, res.cobalt(), true);
      addTheme(CRIMSON_EDITOR, res.crimson_editor(), false);
      addTheme(DAWN, res.dawn(), false);
      addTheme(DREAMWEAVER, res.dreamweaver(), false);
      addTheme(ECLIPSE, res.eclipse(), false);
      addTheme(IDLE_FINGERS, res.idle_fingers(), true);
      addTheme(KATZENMILCH, res.katzenmilch(), false);
      addTheme(KR_THEME, res.kr_theme(), true);
      addTheme(MATERIAL, res.material(), true);
      addTheme(MERBIVORE_SOFT, res.merbivore_soft(), true);
      addTheme(MERBIVORE, res.merbivore(), true);
      addTheme(MONO_INDUSTRIAL, res.mono_industrial(), true);
      addTheme(MONOKAI, res.monokai(), true);
      addTheme(PASTEL_ON_DARK, res.pastel_on_dark(), true);
      addTheme(SOLARIZED_DARK, res.solarized_dark(), true);
      addTheme(SOLARIZED_LIGHT, res.solarized_light(), false);
      addTheme(TEXTMATE, res.textmate(), false);
      addTheme(TOMORROW_NIGHT_BLUE, res.tomorrow_night_blue(), true);
      addTheme(TOMORROW_NIGHT_BRIGHT, res.tomorrow_night_bright(), true);
      addTheme(TOMORROW_NIGHT_EIGHTIES, res.tomorrow_night_eighties(), true);
      addTheme(TOMORROW_NIGHT, res.tomorrow_night(), true);
      addTheme(TOMORROW, res.tomorrow(), false);
      addTheme(TWILIGHT, res.twilight(), true);
      addTheme(VIBRANT_INK, res.vibrant_ink(), true);
      addTheme(XCODE, res.xcode(), false);

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

   private void addTheme(String name,
                         StaticDataResource resource,
                         boolean isDark)
   {
      themes_.add(name);
      themesByName_.put(name, resource.getSafeUri().asString());
      if (isDark)
         darkThemes_.put(name, true);
   }
   
   public boolean isDark(String themeName)
   {
      if (themeName == null)
         return false;
      else
         return darkThemes_.containsKey(themeName);
   }
   
   private void applyTheme(Document document, final String themeName)
   {
      Element oldStyleEl = document.getElementById(linkId_);
      if (oldStyleEl != null)
         oldStyleEl.removeFromParent();
      
      LinkElement currentStyleEl = document.createLinkElement();
      currentStyleEl.setType("text/css");
      currentStyleEl.setRel("stylesheet");
      currentStyleEl.setId(linkId_);
      currentStyleEl.setHref(getThemeUrl(themeName));
      document.getBody().appendChild(currentStyleEl);
      
      addDarkClassIfNecessary(themeName);
      
      // Deferred so that the browser can render the styles.
      new Timer()
      {
         @Override
         public void run()
         {
            events_.fireEvent(new EditorThemeChangedEvent(themeName));
         }
      }.schedule(100);
   }

   private void applyTheme(final String themeName)
   {
      applyTheme(Document.get(), themeName);
   }

   public void applyTheme(Document document)
   {
      applyTheme(document, prefs_.get().theme().getValue());
   }
   
   public void addDarkClassIfNecessary(String themeName)
   {
      if (isDark(themeName))
         Document.get().getBody().addClassName("editor_dark");
      else
         Document.get().getBody().removeClassName("editor_dark");
   }

   public String getEffectiveThemeName(String themeName)
   {
      return themesByName_.containsKey(themeName)
             ? themeName
             : defaultThemeName_;
   }

   private final EventBus events_;
   private final ArrayList<String> themes_;
   private final HashMap<String, String> themesByName_;
   private final HashMap<String, Boolean> darkThemes_;
   private final String defaultThemeName_ = TEXTMATE;
   private final Provider<UIPrefs> prefs_;
   private final String linkId_ = "rstudio-acethemes-linkelement";
}
