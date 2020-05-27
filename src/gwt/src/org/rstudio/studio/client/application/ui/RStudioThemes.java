/*
 * RStudioThemes.java
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

package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;

import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

public class RStudioThemes
{
   public static void initializeThemes(UserPrefs userPrefs,
                                       UserState userState,
                                       Document document,
                                       Element element)
   {
      String themeName = getThemeFromUserPrefs(userPrefs, userState);
      initializeThemes(themeName, document, element);
   }

   public static void initializeThemes(Document document,
                                       Element element)
   {
      if (activeTheme_ != null)
      {
         initializeThemes(activeTheme_, document, element);
      }
   }
   
   public static void initializeThemes(String themeName,
                                       Document document,
                                       Element element)
   {
      element.removeClassName("rstudio-themes-dark");
      element.removeClassName("rstudio-themes-default");
      element.removeClassName("rstudio-themes-dark-grey");
      element.removeClassName("rstudio-themes-alternate");
      element.removeClassName("rstudio-themes-scrollbars");

      document.getBody().removeClassName("rstudio-themes-flat");
      
      document.getBody().removeClassName("rstudio-themes-dark-menus");
      document.getBody().removeClassName("rstudio-themes-dark-menus-disabled");
      
      document.getBody().removeClassName("rstudio-themes-light-menus");
      document.getBody().removeClassName("rstudio-themes-light-menus-disabled");
      
      if (themeName == "default" || themeName == "dark-grey" || themeName == "alternate")
      {
         document.getBody().addClassName("rstudio-themes-flat");
         
         if (themeName.contains("dark"))
         {
            document.getBody().addClassName("rstudio-themes-dark-menus");
            element.addClassName("rstudio-themes-dark");
         }
         else
         {
            document.getBody().addClassName("rstudio-themes-light-menus");
         }

         if (usesScrollbars())
         {
            element.addClassName("rstudio-themes-scrollbars");
         }
            
         element.addClassName("rstudio-themes-" + themeName);
         element.setId("rstudio_container");
      }
      
      activeTheme_ = themeName;
   }

   public static boolean isFlat(UserPrefs prefs)
   {
      return prefs.globalTheme().getValue() != UserPrefs.GLOBAL_THEME_CLASSIC;
   }
   
   public static boolean isFlat()
   {
      return Document.get().getBody().hasClassName("rstudio-themes-flat");
   }
   
   public static boolean isEditorDark()
   {
      return Document.get().getBody().hasClassName("editor_dark");
   }

   public static String suggestThemeFromAceTheme(AceTheme aceTheme, String rstudioTheme)
   {
      if (StringUtil.equals(rstudioTheme, "classic") || (aceTheme == null))
         return rstudioTheme;
      
      return aceTheme.isDark() ? "dark-grey" : rstudioTheme;
   }

   public static String getThemeFromUserPrefs(UserPrefs prefs, UserState state)
   {
      return suggestThemeFromAceTheme(
        state.theme().getGlobalValue().cast(),
        prefs.globalTheme().getGlobalValue()
      );
   }
   
   public static boolean usesScrollbars()
   {
      if (usesScrollbars_ != null) return usesScrollbars_;
      
      if (!BrowseCap.isMacintosh()) {
         usesScrollbars_ = true;
      }
      else {
         Element parent = Document.get().createElement("div");
         parent.getStyle().setWidth(100, Unit.PX);
         parent.getStyle().setHeight(100, Unit.PX);
         parent.getStyle().setOverflow(Overflow.AUTO);
         parent.getStyle().setVisibility(Visibility.HIDDEN);
         parent.getStyle().setPosition(Position.FIXED);
         parent.getStyle().setLeft(-300, Unit.PX);
         parent.getStyle().setTop(-300, Unit.PX);
   
         Element content = Document.get().createElement("div");
         content.getStyle().setWidth(100, Unit.PX);
         content.getStyle().setHeight(200, Unit.PX);
   
         parent.appendChild(content);
         Document.get().getBody().appendChild(parent);
   
         boolean hasScrollbars = parent.getOffsetWidth() - parent.getClientWidth() > 0;
         parent.removeFromParent();
         
         usesScrollbars_ = hasScrollbars;
      }
      
      return usesScrollbars_;
   }
   
   public static void disableDarkMenus()
   {
      BodyElement body = Document.get().getBody();
      if (body.hasClassName("rstudio-themes-dark-menus")) {
         body.removeClassName("rstudio-themes-dark-menus");
         body.addClassName("rstudio-themes-dark-menus-disabled");
      }
   }
   
   public static void enableDarkMenus()
   {
      BodyElement body = Document.get().getBody();
      if (body.hasClassName("rstudio-themes-dark-menus-disabled")) {
         body.removeClassName("rstudio-themes-dark-menus-disabled");
         body.addClassName("rstudio-themes-dark-menus");
      }
   }
   
   private static Boolean usesScrollbars_ = null;
   private static String activeTheme_ = null;
}
