/*
 * AceThemes.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.themes;

import com.google.gwt.core.client.GWT;
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
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.model.ThemeServerOperations;

import java.util.HashMap;
import java.util.function.Consumer;

@Singleton
public class AceThemes
{
   @Inject
   public AceThemes(ThemeServerOperations themeServerOperations,
                    final Provider<UserState> state,
                    final Provider<UserPrefs> prefs,
                    EventBus events)
   {
      themeServerOperations_ = themeServerOperations;
      events_ = events;
      state_ = state;
      prefs_ = prefs;
      themes_ = new HashMap<>();

      state.get().theme().bind(theme -> applyTheme((AceTheme)theme.cast()));
   }
   
   private void applyTheme(Document document, final AceTheme theme)
   {
      final String linkId = "rstudio-acethemes-linkelement";

      // Build the URL.
      StringBuilder themeUrl = new StringBuilder();
      themeUrl.append(GWT.getHostPageBaseURL())
         .append(theme.getUrl())
         .append("?dark=")
         .append(theme.isDark() ? "1" : "0")
         .append("&refresh=1");
      
      LinkElement currentStyleEl = document.createLinkElement();
      currentStyleEl.setType("text/css");
      currentStyleEl.setRel("stylesheet");
      currentStyleEl.setId(linkId);
      currentStyleEl.setHref(themeUrl.toString());
      
      // In server mode, augment the theme with a font if we have one
      if (!Desktop.isDesktop() && prefs_.get().serverEditorFontEnabled().getValue())
      {
         String font = prefs_.get().serverEditorFont().getValue();
         if (!StringUtil.isNullOrEmpty(font))
         {
            final String fontId = "rstudio-fontelement";
            LinkElement fontEl = document.createLinkElement();
            fontEl.setType("text/css");
            fontEl.setRel("stylesheet");
            fontEl.setId(fontId);
            fontEl.setHref(
                  GWT.getHostPageBaseURL() + 
                  "fonts/css/" + 
                  font + ".css");
            Element oldFontEl = document.getElementById(fontId);
            if (null != oldFontEl)
            {
              document.getBody().replaceChild(fontEl, oldFontEl);
            }
            else
            {
               document.getBody().appendChild(fontEl);
            }
         }
      }
   
      Element oldStyleEl = document.getElementById(linkId);
      if (null != oldStyleEl)
      {
        document.getBody().replaceChild(currentStyleEl, oldStyleEl);
      }
      else
      {
         document.getBody().appendChild(currentStyleEl);
      }
      
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
            if (Desktop.hasDesktopFrame())
            {
               // find 'rstudio_container' element (note that this may not exist
               // in some satellite windows; e.g. the Git window)
               Element el = Document.get().getElementById("rstudio_container");
               if (el == null)
                  return;
               
               Style style = DomUtils.getComputedStyles(el);
               String color = style.getBackgroundColor();
               RGBColor parsed = RGBColor.fromCss(color);
               
               JsArrayInteger colors = JsArrayInteger.createArray(3).cast();
               colors.set(0, parsed.red());
               colors.set(1, parsed.green());
               colors.set(2, parsed.blue());
               Desktop.getFrame().setBackgroundColor(colors);
               Desktop.getFrame().syncToEditorTheme(theme.isDark());

               el = DomUtils.getElementsByClassName("rstheme_toolbarWrapper")[0];
               style = DomUtils.getComputedStyles(el);
               color = style.getBackgroundColor();
               parsed = RGBColor.fromCss(color);

               Desktop.getFrame().changeTitleBarColor(parsed.red(), parsed.green(), parsed.blue());
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
      applyTheme(document, (AceTheme)state_.get().theme().getValue());
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
            themes_.clear();
            int len = jsonThemeArray.length();
            for (int i = 0; i < len; ++i)
            {
               AceTheme theme = jsonThemeArray.get(i);
               themes_.put(theme.getName(), theme);
            }
            
            if (len == 0)
               Debug.logWarning("Server was unable to find any installed themes.");
            themeConsumer.accept(themes_);
         }
      });
   }
   
   public void addTheme(
      String themeLocation,
      Consumer<String> stringConsumer,
      Consumer<String> errorMessageConsumer)
   {
      themeServerOperations_.addTheme(new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String result)
         {
            stringConsumer.accept(result);
         }
         
         @Override
         public void onError(ServerError error)
         {
            errorMessageConsumer.accept(error.getUserMessage());
         }
      }, themeLocation);
   }
   
   public void removeTheme(
      String themeName,
      Consumer<String> errorMessageConsumer,
      Operation afterOperation)
   {
      if (!themes_.containsKey(themeName))
      {
         errorMessageConsumer.accept("The specified theme does not exist");
      }
      else if (themes_.get(themeName).isDefaultTheme())
      {
         errorMessageConsumer.accept("The specified theme is a default RStudio theme and cannot be removed.");
      }
      else
      {
         themeServerOperations_.removeTheme(
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onResponseReceived(Void response)
               {
                  themes_.remove(themeName);
                  afterOperation.execute();
               }
               
               @Override
               public void onError(ServerError error)
               {
                  errorMessageConsumer.accept(error.getUserMessage());
               }
            },
            themeName);
      }
   }
   
   // This function can be used to get the name of a theme without adding it to RStudio. It is not
   // used to get the name of an existing theme.
   public void getThemeName(
      String themeLocation,
      Consumer<String> stringConsumer,
      Consumer<String> errorMessageConsumer)
   {
      themeServerOperations_.getThemeName(
         new ServerRequestCallback<String>()
         {
            @Override
            public void onResponseReceived(String result)
            {
               stringConsumer.accept(result);
            }
            
            @Override
            public void onError(ServerError error)
            {
               errorMessageConsumer.accept(error.getUserMessage());
            }
         },
         themeLocation);
   }

   private ThemeServerOperations themeServerOperations_;
   private final EventBus events_;
   private final Provider<UserState> state_;
   private final Provider<UserPrefs> prefs_;
   private HashMap<String, AceTheme> themes_;
}
