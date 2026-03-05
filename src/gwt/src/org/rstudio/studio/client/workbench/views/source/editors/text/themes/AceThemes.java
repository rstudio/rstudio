/*
 * AceThemes.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.themes;

import java.util.HashMap;
import java.util.function.Consumer;

import org.rstudio.core.client.ColorUtil.RGBColor;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.FontDetector;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.ComputeThemeColorsEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.model.ThemeServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

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
   
   public AceTheme getCurrentTheme()
   {
      return currentTheme_;
   }
   
   private void applyTheme(Document document, final AceTheme theme)
   {
      if (Document.get() == document)
      {
         currentTheme_ = theme;
      }
      
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
      augmentThemeWithFont(document);

      // Clear any load handler on the previous element before registering
      // a new one, so there is never more than one active handler.
      Element oldStyleEl = document.getElementById(linkId);
      if (null != oldStyleEl)
      {
         clearCssLoadHandler(LinkElement.as(oldStyleEl));
      }

      // Register a load handler so that theme colors are recomputed
      // once the CSS has been loaded and applied. Only for the main
      // document; iframes and satellite windows also call applyTheme(),
      // but we only need one recomputation per theme change.
      //
      // The handler is registered before DOM insertion so that we
      // don't miss a synchronous onload for cached stylesheets.
      if (Document.get() == document)
      {
         setCssLoadHandlers(currentStyleEl,
            () -> onThemeCssLoaded(),
            () -> onThemeCssError());
      }

      if (null != oldStyleEl)
      {
         document.getBody().replaceChild(currentStyleEl, oldStyleEl);
      }
      else
      {
         document.getBody().appendChild(currentStyleEl);
      }

      if (theme.isDark())
      {
         document.getBody().removeClassName("editor_light");
         document.getBody().addClassName("editor_dark");
      }
      else
      {
         document.getBody().removeClassName("editor_dark");
         document.getBody().addClassName("editor_light");
      }

      // NOTE: EditorThemeChangedEvent and desktop color synchronization
      // are fired from onThemeCssLoaded() once the stylesheet has been
      // loaded and applied.
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
         errorMessageConsumer.accept(constants_.specifiedThemeDoesNotExist());
      }
      else if (themes_.get(themeName).isDefaultTheme())
      {
         errorMessageConsumer.accept(constants_.specifiedDefaultThemeCannotBeRemoved());
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

   private void augmentThemeWithFont(Document document)
   {
      // Skip for desktop builds; fonts are applied via alternate mechanism
      if (Desktop.isDesktop())
         return;
      
      // Check whether server fonts are actually enabled.
      boolean enabled = prefs_.get().serverEditorFontEnabled().getValue();
      if (!enabled)
         return;

      // Check the selected font. Note that the name is misleading; these can
      // be either be fonts provided by the browser or installed on the user's
      // system, _or_ fonts installed by an administrator. We need to disambiguate.
      String fontName = prefs_.get().serverEditorFont().getValue();
      if (StringUtil.isNullOrEmpty(fontName))
         return;

      // Remove a pre-existing font element, if any. Clear any pending
      // load handlers first so stale callbacks don't fire after removal.
      Element fontStylesEl = DomUtils.querySelector(document.getBody(), "#" + RSTUDIO_FONTELEMENT_ID);
      if (fontStylesEl != null)
      {
         if (LinkElement.is(fontStylesEl))
            clearCssLoadHandler(LinkElement.as(fontStylesEl));
         fontStylesEl.removeFromParent();
      }

      // Apply the font CSS. Use alternate code paths depending on whether this
      // appears to be a client-side font, versus a server font.
      if (FontDetector.isFontSupported(fontName))
      {
         applyBrowserFont(document, fontName);
      }
      else
      {
         applyServerFont(document, fontName);
      }
   }

   private void applyBrowserFont(Document document, String fontName)
   {
      String fontCss = getFontCss(fontName);
      StyleElement fontStyles = document.createStyleElement();
      fontStyles.setId(RSTUDIO_FONTELEMENT_ID);
      fontStyles.setPropertyString("textContent", fontCss);
      document.getHead().appendChild(fontStyles);
   }

   private void applyServerFont(Document document, String fontName)
   {
      LinkElement fontEl = document.createLinkElement();
      fontEl.setType("text/css");
      fontEl.setRel("stylesheet");
      fontEl.setId(RSTUDIO_FONTELEMENT_ID);
      fontEl.setHref(GWT.getHostPageBaseURL() + "fonts/css/" + fontName + ".css");

      // Register the load handler before DOM insertion so that we
      // don't miss a synchronous onload for cached stylesheets.
      // Only for the main document, matching the theme CSS pattern.
      if (Document.get() == document)
      {
         setCssLoadHandlers(fontEl,
            () -> onFontCssLoaded(),
            () -> onFontCssError());
      }

      document.getBody().appendChild(fontEl);
   }

   public static String getFontCss(String fontName)
   {
      return RES.fontsCss().getText().replaceAll("#!font#", fontName);
   }

   public interface Resources extends ClientBundle
   {
      @Source("resources/fonts.css")
      TextResource fontsCss();
   }
   
   private native void setCssLoadHandlers(LinkElement link, Runnable onLoad, Runnable onError) /*-{
      link.onload = $entry(function() {
         onLoad.@java.lang.Runnable::run()();
      });
      link.onerror = $entry(function() {
         onError.@java.lang.Runnable::run()();
      });
   }-*/;

   private native void clearCssLoadHandler(LinkElement link) /*-{
      link.onload = null;
      link.onerror = null;
   }-*/;

   private void onThemeCssLoaded()
   {
      // Synchronize the effective background color with the desktop frame.
      try
      {
         syncDesktopThemeColors();
      }
      catch (Exception e)
      {
         Debug.logWarning("Failed to sync desktop theme colors: " + e.getMessage());
      }

      fireThemeEvents();
   }

   private void syncDesktopThemeColors()
   {
      if (!Desktop.hasDesktopFrame())
         return;

      if (currentTheme_ == null)
         return;

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
      Desktop.getFrame().syncToEditorTheme(currentTheme_.isDark());

      Element[] toolbarEls = DomUtils.getElementsByClassName("rstheme_toolbarWrapper");
      if (toolbarEls.length == 0)
         return;

      el = toolbarEls[0];
      style = DomUtils.getComputedStyles(el);
      color = style.getBackgroundColor();
      parsed = RGBColor.fromCss(color);

      Desktop.getFrame().changeTitleBarColor(parsed.red(), parsed.green(), parsed.blue());
   }

   private void onThemeCssError()
   {
      Debug.logWarning("Failed to load Ace theme CSS: " +
         (currentTheme_ != null ? currentTheme_.getUrl() : "<unknown>"));

      // Fire events even on error so that downstream consumers
      // (iframe theme variables, client state, desktop frame, etc.)
      // get default/fallback values rather than remaining permanently stale.
      fireThemeEvents();
   }

   private void fireThemeEvents()
   {
      if (currentTheme_ != null)
      {
         events_.fireEvent(new EditorThemeChangedEvent(currentTheme_));
      }
      events_.fireEvent(new ComputeThemeColorsEvent());
   }

   private void onFontCssLoaded()
   {
      // Nothing to do -- font CSS doesn't affect theme colors, and the
      // theme CSS load handler already fires the necessary events.
      //
      // The error handler below fires ComputeThemeColorsEvent because a
      // font load failure may affect sampled style values, warranting a
      // recomputation as a precaution.
   }

   private void onFontCssError()
   {
      String fontName = prefs_.get().serverEditorFont().getValue();
      Debug.logWarning("Failed to load editor font CSS: " +
         (!StringUtil.isNullOrEmpty(fontName) ? fontName : "<unknown>"));

      events_.fireEvent(new ComputeThemeColorsEvent());
   }

   private AceTheme currentTheme_;

   private ThemeServerOperations themeServerOperations_;
   private final EventBus events_;
   private final Provider<UserState> state_;
   private final Provider<UserPrefs> prefs_;
   private HashMap<String, AceTheme> themes_;

   private static final String RSTUDIO_FONTELEMENT_ID = "rstudio-fontelement";
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
   public static final Resources RES = GWT.create(Resources.class);
   
}
