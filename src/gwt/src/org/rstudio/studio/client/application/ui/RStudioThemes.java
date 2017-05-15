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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;

public class RStudioThemes
{
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
      
      if (themeName == "default" || themeName == "dark-grey" || themeName == "alternate") {         
         document.getBody().addClassName("rstudio-themes-flat");
         
         if (themeName.contains("dark")) {
            element.addClassName("rstudio-themes-dark");
            
            if (usesScrollbars()) {
               document.getBody().addClassName("rstudio-themes-scrollbars");
            }
         }
         element.addClassName("rstudio-themes-" + themeName);
      }
   }

   public static boolean isFlat(UIPrefs prefs) {
      return prefs.getFlatTheme().getValue() != "classic"; 
   }
   
   private static boolean usesScrollbars() {
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
   
   private static Boolean usesScrollbars_ = null;
}
