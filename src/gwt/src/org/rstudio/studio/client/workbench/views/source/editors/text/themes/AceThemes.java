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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.core.client.ColorUtil.RGBColor;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.model.ThemeServerOperations;

import java.util.HashMap;
import java.util.function.Consumer;

@Singleton
public class AceThemes
{
   @Inject
   public AceThemes(ThemeServerOperations themeServerOperations,
                    final Provider<UIPrefs> prefs,
                    EventBus events)
   {
      themeServerOperations_ = themeServerOperations;
      events_ = events;
      prefs_ = prefs;

      prefs.get().theme().bind(theme -> applyTheme(theme));
   }
   
   private void applyTheme(Document document, final AceTheme theme)
   {
      Element oldStyleEl = document.getElementById(linkId_);
      if (oldStyleEl != null)
         oldStyleEl.removeFromParent();
      
      LinkElement currentStyleEl = document.createLinkElement();
      currentStyleEl.setType("text/css");
      currentStyleEl.setRel("stylesheet");
      currentStyleEl.setId(linkId_);
      currentStyleEl.setHref(theme.getUrl() + "?dark=" + (theme.isDark() ? "1" : "0"));
      document.getBody().appendChild(currentStyleEl);
      
      if(theme.isDark())
         document.getBody().addClassName("editor_dark");
      else
         document.getBody().removeClassName("editor_dark");
         
      
      // Deferred so that the browser can render the styles.
      new Timer()
      {
         @Override
         public void run()
         {
            events_.fireEvent(new EditorThemeChangedEvent(theme));
            
            // synchronize the effective background color with the desktop
            if (Desktop.isDesktop())
            {
               Element el = Document.get().getElementById("rstudio_container");
               Style style = DomUtils.getComputedStyles(el);
               String color = style.getBackgroundColor();
               RGBColor parsed = RGBColor.fromCss(color);
               
               JsArrayInteger colors = JsArrayInteger.createArray(3).cast();
               colors.set(0, parsed.red());
               colors.set(1, parsed.green());
               colors.set(2, parsed.blue());
               Desktop.getFrame().setBackgroundColor(colors);
            }
         }
      }.schedule(100);
   }

   private void applyTheme(final AceTheme theme)
   {
      applyTheme(Document.get(), theme);
   }

   public void applyTheme(Document document)
   {
      applyTheme(document, prefs_.get().theme().getValue());
   }
   
   public void getThemes(
      Consumer<HashMap<String, AceTheme>> themeConsumer,
      ProgressIndicator indicator)
   {
      themeServerOperations_.getThemes(
         new DelayedProgressRequestCallback<JsArray<AceTheme>>(indicator)
      {
         @Override
         public void onSuccess(JsArray<AceTheme> jsonThemeArray)
         {
            HashMap<String, AceTheme> themes = new HashMap<>();
            int len = jsonThemeArray.length();
            for (int i = 0; i < len; ++i)
            {
               AceTheme theme = jsonThemeArray.get(i);
               themes.put(theme.getName(), theme);
            }
            
            Debug.logWarning("Server was unable to find any installed themes.");
            themeConsumer.accept(themes);
         }
      });
   }

   private ThemeServerOperations themeServerOperations_;
   private final EventBus events_;
   private final Provider<UIPrefs> prefs_;
   private final String linkId_ = "rstudio-acethemes-linkelement";
}
