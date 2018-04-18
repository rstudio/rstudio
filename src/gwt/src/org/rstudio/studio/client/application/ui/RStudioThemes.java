/*
 * RStudioThemes.java
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

package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class RStudioThemes
{
   public static void initializeThemes(UIPrefs uiPrefs,
                                       Document document,
                                       Element element)
   {
      String themeName = getThemeFromUiPrefs(uiPrefs);
      initializeThemes(themeName, document, element);
   }

   public static void initializeThemes(String themeName,
                                       Document document,
                                       Element element)
   {
      document.getBody().removeClassName("rstudio-themes-flat");
      element.removeClassName("rstudio-themes-dark");
      element.removeClassName("rstudio-themes-default");
      element.removeClassName("rstudio-themes-dark-grey");
      element.removeClassName("rstudio-themes-alternate");
      element.removeClassName("rstudio-themes-scrollbars");

      document.getBody().removeClassName("rstudio-themes-dark-menus");
      document.getBody().removeClassName("rstudio-themes-dark-menus-disabled");
      
      if (themeName == "default" || themeName == "dark-grey" || themeName == "alternate") {         
         document.getBody().addClassName("rstudio-themes-flat");
         
         if (themeName.contains("dark")) {
            document.getBody().addClassName("rstudio-themes-dark-menus");
            element.addClassName("rstudio-themes-dark");
         }

         if (usesScrollbars()) {
            element.addClassName("rstudio-themes-scrollbars");
         }
            
         element.addClassName("rstudio-themes-" + themeName);
         element.setId("rstudio_container");
      }
   }

   public static boolean isFlat(UIPrefs prefs) {
      return prefs.getFlatTheme().getValue() != "classic"; 
   }
   
   public static boolean isFlat() {
      return Document.get().getBody().hasClassName("rstudio-themes-flat");
   }
   
   public static boolean isEditorDark() {
      return Document.get().getBody().hasClassName("editor_dark");
   }

   public static String suggestThemeFromAceTheme(String aceTheme, String rstudioTheme) {
      if (rstudioTheme == "classic") return rstudioTheme;

      RegExp keyReg = RegExp.compile(
         "ambiance|chaos|clouds midnight|cobalt|dracula|idle fingers|kr theme|" +
         "material|merbivore soft|merbivore|mono industrial|monokai|" +
         "pastel on dark|solarized dark|tomorrow night blue|tomorrow night bright|" +
         "tomorrow night 80s|tomorrow night|twilight|vibrant ink", "i");
      
      MatchResult result = keyReg.exec(aceTheme);
      return result != null ? "dark-grey" : rstudioTheme;
   }

   public static String getThemeFromUiPrefs(UIPrefs prefs) {
      return suggestThemeFromAceTheme(
        prefs.theme().getGlobalValue(),
        prefs.getFlatTheme().getGlobalValue()
      );
   }
   
   public static boolean usesScrollbars() {
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

   private native static String getEditorThemeProperty(String theme, String property) /*-{
      var colors = {
         "Ambiance"              : { background : "#202020", color : "#E6E1DC" },
         "Chaos"                 : { background : "#161616", color : "#E6E1DC" },
         "Chrome"                : { background : "#FFFFFF", color : "black"   },
         "Clouds Midnight"       : { background : "#191919", color : "#929292" },
         "Clouds"                : { background : "#FFFFFF", color : "#000000" },
         "Cobalt"                : { background : "#002240", color : "#FFFFFF" },
         "Crimson Editor"        : { background : "#FFFFFF", color : "rgb(64, 64, 64)" },
         "Dawn"                  : { background : "#F9F9F9", color : "#080808" },
         "Dracula"               : { background : "#282a36", color : "#f8f8f2" },
         "Dreamwaver"            : { background : "#FFFFFF", color : "black" },
         "Eclipse"               : { background : "#FFFFFF", color : "black" },
         "Idle Fingers"          : { background : "#323232", color : "#FFFFFF" },
         "Katzenmilch"           : { background : "#f3f2f3", color : "rgba(15, 0, 9, 1.0)" },
         "Kr Theme"              : { background : "#0B0A09", color : "#FCFFE0" },
         "Material"              : { background : "#263238", color : "#C5C8C6" },
         "Merbivore Soft"        : { background : "#1C1C1C", color : "#E6E1DC" },
         "Merbivore"             : { background : "#161616", color : "#E6E1DC" },
         "Mono Industrial"       : { background : "#222C28", color : "#FFFFFF" },
         "Monokai"               : { background : "#272822", color : "#F8F8F2" },
         "Pastel On Dark"        : { background : "#2C2828", color : "#8F938F" },
         "Solarized Dark"        : { background : "#002B36", color : "#93A1A1" },
         "Solarized Light"       : { background : "#FDF6E3", color : "#586E75" },
         "TextMate"              : { background : "#FFFFFF", color : "black"   },
         "Tomorrow Night Blue"   : { background : "#002451", color : "#FFFFFF" },
         "Tomorrow Night Bright" : { background : "#000000", color : "#DEDEDE" },
         "Tomorrow Night 80s"    : { background : "#2D2D2D", color : "#CCCCCC" },
         "Tomorrow Night"        : { background : "#1D1F21", color : "#C5C8C6" },
         "Tomorrow"              : { background : "#FFFFFF", color : "#4D4D4C" },
         "Twilight"              : { background : "#141414", color : "#F8F8F8" },
         "Vibrant Ink"           : { background : "#0F0F0F", color : "#FFFFFF" },
         "Xcode"                 : { background : "#FFFFFF", color : "#000000" }
      };

      return colors[theme][property];
   }-*/;

   public static String getBackground(String theme)
   {
      return getEditorThemeProperty(theme, "background");
   }

   public static String getForeground(String theme)
   {
      return getEditorThemeProperty(theme, "color");
   }
   
   private static Boolean usesScrollbars_ = null;
}
