/*
 * RStudioThemedFrame.java
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

package org.rstudio.core.client.widget;

import java.util.Map;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.theme.ThemeColorExtractor;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ThemeChangedEvent;
import org.rstudio.studio.client.application.events.ThemeColorsComputedEvent;
import org.rstudio.studio.client.application.ui.RStudioThemes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.inject.Inject;

public class RStudioThemedFrame extends RStudioFrame
                                implements ThemeChangedEvent.Handler,
                                           ThemeColorsComputedEvent.Handler
{
   public RStudioThemedFrame(String title)
   {
      this(title, null, null, null, false);
   }

   public RStudioThemedFrame(
      String title,
      String url,
      String customStyle,
      String urlStyle,
      boolean removeBodyStyle)
   {
      this(title,
           url,
           customStyle,
           urlStyle,
           removeBodyStyle,
           true);
   }

   public RStudioThemedFrame(
      String title,
      String url,
      String customStyle,
      String urlStyle,
      boolean removeBodyStyle,
      boolean enableThemes)
   {
      this(title,
           url,
           false,
           null,
           customStyle,
           urlStyle,
           removeBodyStyle,
           enableThemes);
   }

   public RStudioThemedFrame(
      String title,
      String url,
      boolean sandbox,
      String sandboxAllow,
      String customStyle,
      String urlStyle,
      boolean removeBodyStyle,
      boolean enableThemes)
   {
      super(title, url, sandbox, sandboxAllow);
      
      customStyle_ = customStyle;
      urlStyle_ = urlStyle;
      removeBodyStyle_ = removeBodyStyle;
      enableThemes_ = enableThemes;
      
      RStudioGinjector.INSTANCE.injectMembers(this);

      setAceThemeAndCustomStyle(customStyle_, urlStyle_, removeBodyStyle_);
   }

   @Inject
   public void initialize(EventBus events)
   {
      events_ = events;

      events_.addHandler(ThemeChangedEvent.TYPE, this);
      events_.addHandler(ThemeColorsComputedEvent.TYPE, this);
   }

   @Override
   public void onThemeChanged(ThemeChangedEvent event)
   {
      applyThemesIfEnabled();
   }

   @Override
   public void onThemeColorsComputed(ThemeColorsComputedEvent event)
   {
      injectThemeVariables();
      ensureLoadHandlerInitialized();
   }

   /**
    * Sets a custom action to be executed when the frame loads.
    * This action will be executed INSTEAD of the default theming behavior.
    * The action is cleared after execution (one-shot).
    *
    * @param action The action to execute on load, or null to clear
    */
   public void setOnLoadAction(Runnable action)
   {
      pendingLoadAction_ = action;
      ensureLoadHandlerInitialized();
   }

   /**
    * Clears any pending load action without executing it.
    */
   public void clearOnLoadAction()
   {
      pendingLoadAction_ = null;
   }

   /**
    * Ensures the single load handler is initialized.
    * This should be called whenever we need load handling.
    */
   private void ensureLoadHandlerInitialized()
   {
      if (!loadHandlerInitialized_)
      {
         loadHandlerInitialized_ = true;
         addLoadHandler(new LoadHandler()
         {
            @Override
            public void onLoad(LoadEvent event)
            {
               onFrameLoaded();
            }
         });
      }
   }

   /**
    * Called when the frame loads. Executes any pending action,
    * or falls back to default theming behavior.
    */
   private void onFrameLoaded()
   {
      if (pendingLoadAction_ != null)
      {
         // Execute and clear the pending action (one-shot)
         Runnable action = pendingLoadAction_;
         pendingLoadAction_ = null;
         action.run();
      }
      else
      {
         // Default behavior: apply themes
         applyThemesIfEnabled();
      }
   }

   /**
    * Applies themes immediately if enabled.
    */
   private void applyThemesIfEnabled()
   {
      if (enableThemes_)
      {
         addThemesStyle(customStyle_, urlStyle_, removeBodyStyle_);
      }
   }

   private void addThemesStyle(String customStyle, String urlStyle, boolean removeBodyStyle)
   {
      if (getWindow() != null && getWindow().getDocument() != null)
      {
         Document document = getWindow().getDocument();
         if (!isEligibleForCustomStyles(document))
            return;
         
         if (customStyle == null)
            customStyle = "";
         
         customStyle += RES.styles().getText();
         StyleElement style = document.createStyleElement();
         style.setInnerHTML(customStyle);
         document.getHead().appendChild(style);
         
         if (urlStyle != null)
         {
            LinkElement styleLink = document.createLinkElement();
            styleLink.setHref(urlStyle);
            styleLink.setRel("stylesheet");
            document.getHead().appendChild(styleLink);
         }
         
         RStudioGinjector.INSTANCE.getAceThemes().applyTheme(document);

         // Inject CSS variables after theme is applied
         injectThemeVariables();

         BodyElement body = document.getBody();
         if (body != null)
         {
            if (removeBodyStyle)
               body.removeAttribute("style");
            
            RStudioThemes.initializeThemes(
               RStudioGinjector.INSTANCE.getUserPrefs(),
               RStudioGinjector.INSTANCE.getUserState(),
               document, document.getBody());
            
            body.addClassName("ace_editor_theme");
            
            // Add OS tag to the frame so that it can apply OS-specific CSS if
            // needed.
            body.addClassName(BrowseCap.operatingSystem());
            
            // Add flat theme class. None of our own CSS should use this, but many
            // third party themes developed against earlier versions of RStudio
            // (2021.09 and older) use it extensively in selectors.
            body.addClassName("rstudio-themes-flat");
         }
      }
   }

   private void setAceThemeAndCustomStyle(
      final String customStyle,
      final String urlStyle,
      final boolean removeBodyStyle)
   {
      if (!enableThemes_)
         return;

      // Apply themes immediately (for already-loaded content)
      addThemesStyle(customStyle, urlStyle, removeBodyStyle);

      // Ensure the single load handler is set up for future loads
      ensureLoadHandlerInitialized();
   }
   
   /**
    * Inject CSS variables representing theme colors into the iframe document.
    */
   public void injectThemeVariables()
   {
      try
      {
         // Check iframe is ready (use getWindow/getDocument, NOT getIFrame/getContentDocument)
         if (getWindow() == null)
            return;

         Document document = null;
         try
         {
            document = getWindow().getDocument();
         }
         catch (Exception e)
         {
            // Cross-origin iframe - can't inject, skip silently
            return;
         }

         if (document == null)
            return;

         // Extract colors (cache is managed inside ThemeColorExtractor)
         Map<String, String> colors = ThemeColorExtractor.extractEssentialColors();

         // Get iframe's html element
         Element htmlElement = document.getDocumentElement();
         if (htmlElement == null)
            return;

         // Inject each variable
         for (Map.Entry<String, String> entry : colors.entrySet())
         {
            injectCSSVariable(htmlElement, entry.getKey(), entry.getValue());
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
         // Fail gracefully - iframe will use fallback CSS values
      }
   }

   /**
    * Native method to inject a CSS custom property into an element.
    */
   private native void injectCSSVariable(Element htmlElement,
                                        String property,
                                        String value) /*-{
      htmlElement.style.setProperty(property, value);
   }-*/;

   private static final native boolean isEligibleForCustomStyles(Document document)
   /*-{
      // We disable custom styling for most vignettes, as we cannot guarantee
      // the vignette will remain legible after attempting to re-style with
      // a dark theme.

      // If the document contains an 'article', avoid custom styling.
      var articles = document.getElementsByTagName("article");
      if (articles.length !== 0)
         return false;

      // If the document uses hljs, avoid custom styling.
      // https://github.com/rstudio/rstudio/issues/11022
      var hljs = document.defaultView.hljs;
      if (hljs != null)
         return false;

      return true;
   }-*/;
   
   // Resources ----
   public interface Resources extends ClientBundle
   {
      @Source("RStudioThemedFrame.css")
      Styles styles();
   }

   public interface Styles extends CssResource
   {
   }

   private static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

   // Private members ----

   private EventBus events_;
   private String customStyle_;
   private String urlStyle_;
   private boolean removeBodyStyle_;
   private boolean enableThemes_;

   // Single load handler state
   private boolean loadHandlerInitialized_ = false;
   private Runnable pendingLoadAction_ = null;
}
