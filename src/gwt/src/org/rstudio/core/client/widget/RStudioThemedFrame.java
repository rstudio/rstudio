/*
 * RStudioThemedFrame.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

package org.rstudio.core.client.widget;

import org.rstudio.core.client.theme.ThemeColors;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ThemeChangedEvent;
import org.rstudio.studio.client.application.ui.RStudioThemes;

import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.inject.Inject;

public class RStudioThemedFrame extends RStudioFrame
                                implements ThemeChangedEvent.Handler
{
   public RStudioThemedFrame()
   {
      this(null, null, null, false);
   }

   public RStudioThemedFrame(
      String url,
      String customStyle,
      String urlStyle,
      boolean removeBodyStyle)
   {
      this(url, false, null, customStyle, urlStyle, removeBodyStyle);
   }

   public RStudioThemedFrame(
      String url,
      boolean sandbox,
      String sandboxAllow,
      String customStyle,
      String urlStyle,
      boolean removeBodyStyle)
   {
      super(url, sandbox, sandboxAllow);
      
      customStyle_ = customStyle;
      urlStyle_ = urlStyle;
      removeBodyStyle_ = removeBodyStyle;
      
      RStudioGinjector.INSTANCE.injectMembers(this);

      setAceThemeAndCustomStyle(customStyle_, urlStyle_, removeBodyStyle_);
   }

   @Inject
   public void initialize(EventBus events)
   {
      events_ = events;

      events_.addHandler(ThemeChangedEvent.TYPE, this);
   }

   @Override
   public void onThemeChanged(ThemeChangedEvent event)
   {
      setAceThemeAndCustomStyle(customStyle_, urlStyle_, removeBodyStyle_);
   }
   
   private void addThemesStyle(String customStyle, String urlStyle, boolean removeBodyStyle)
   {
      if (getWindow() != null && getWindow().getDocument() != null)
      {
         Document document = getWindow().getDocument();
         
         if (customStyle == null) customStyle = "";
         
         customStyle += "\n" +
         ".rstudio-themes-flat.rstudio-themes-dark.rstudio-themes-scrollbars::-webkit-scrollbar,\n" +
         ".rstudio-themes-flat.rstudio-themes-dark.rstudio-themes-scrollbars ::-webkit-scrollbar {\n" +
         "   background: #FFF;\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-dark.rstudio-themes-scrollbars::-webkit-scrollbar-thumb,\n" +
         ".rstudio-themes-flat.rstudio-themes-dark.rstudio-themes-scrollbars ::-webkit-scrollbar-thumb {\n" +
         "   -webkit-border-radius: 10px;\n" +
         "   background: " + ThemeColors.darkGreyBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-dark.rstudio-themes-scrollbars::-webkit-scrollbar-track,\n" + 
         ".rstudio-themes-flat.rstudio-themes-dark.rstudio-themes-scrollbars ::-webkit-scrollbar-track,\n" + 
         ".rstudio-themes-flat.rstudio-themes-dark.rstudio-themes-scrollbars::-webkit-scrollbar-corner,\n" +
         ".rstudio-themes-flat.rstudio-themes-dark.rstudio-themes-scrollbars ::-webkit-scrollbar-corner {\n" +
         "   background: " + ThemeColors.darkGreyMostInactiveBackground + ";\n" +
         "}\n" + 
         ".rstudio-themes-flat.rstudio-themes-dark.rstudio-themes-scrollbars::-webkit-scrollbar-thumb,\n" +
         ".rstudio-themes-flat.rstudio-themes-dark.rstudio-themes-scrollbars ::-webkit-scrollbar-thumb{\n" +
         "   border: solid 3px " + ThemeColors.darkGreyMostInactiveBackground + ";" +
         "}\n";
         
         StyleElement style = document.createStyleElement();
         style.setInnerHTML(customStyle);
         document.getHead().appendChild(style);
         
         if (urlStyle != null) {
            LinkElement styleLink = document.createLinkElement();
            styleLink.setHref(urlStyle);
            styleLink.setRel("stylesheet");
            document.getHead().appendChild(styleLink);
         }
         
         RStudioGinjector.INSTANCE.getAceThemes().applyTheme(document);
         
         BodyElement body = document.getBody();
         if (body != null)
         {
            if (removeBodyStyle) body.removeAttribute("style");
            
            RStudioThemes.initializeThemes(
              RStudioGinjector.INSTANCE.getUIPrefs(),
              document, document.getBody());
            
            body.addClassName("ace_editor_theme");
         }
      }
   }

   private void setAceThemeAndCustomStyle(
      final String customStyle,
      final String urlStyle,
      final boolean removeBodyStyle)
   {
      addThemesStyle(customStyle, urlStyle, removeBodyStyle);
      
      this.addLoadHandler(new LoadHandler()
      {      
         @Override
         public void onLoad(LoadEvent arg0)
         {
            addThemesStyle(customStyle, urlStyle, removeBodyStyle);
         }
      });
   }

   private EventBus events_;
   private String customStyle_;
   private String urlStyle_;
   private boolean removeBodyStyle_;
}
