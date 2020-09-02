/*
 * AceTheme.java
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.workbench.prefs.model.UserStateAccessor;

/**
 * Represents an editor theme.
 */
public class AceTheme extends UserStateAccessor.Theme
{
   protected AceTheme() {}
   
   public static final AceTheme createDefault()
   {
      return createDefault(false);
   }
   
   public static final AceTheme createDefault(boolean isDark)
   {
      if (isDark)
      {
         return create("Tomorrow Night", "theme/default/tomorrow_night.rstheme", true);
      }
      else
      {
         return create("Textmate (default)", "theme/default/textmate.rstheme", false);
      }
   }
   
   public static final native AceTheme create(String name, String url, Boolean isDark)
   /*-{
      var theme = new Object();
      theme.name = name;
      theme.url = url;
      theme.isDark = isDark;
      return theme;
   }-*/;
   
   public native final Boolean isDark()
   /*-{
      return this.isDark;
   }-*/;
   
   public native final Boolean isSolarizedLight() /*-{
      return this.url.indexOf('solarized_light.rstheme') !== -1;
   }-*/;
   
   public final Boolean isDefaultTheme()
   {
      return Pattern.create("^theme/default/.+?\\.rstheme$").test(getUrl());
   }
   
   public final Boolean isLocalCustomTheme()
   {
      return Pattern.create("^theme/custom/local/.+?\\.rstheme$").test(getUrl());
   }
   
   public final Boolean isGlobalCustomTheme()
   {
      return Pattern.create("^theme/custom/global/.+?\\.rstheme$").test(getUrl());
   }
   
   public final String getFileStem()
   {
      return FileSystemItem.createFile(this.getUrl()).getStem();
   }
   
   public final Boolean isEqualTo(AceTheme other)
   {
      return StringUtil.equalsIgnoreCase(other.getName(), this.getName());
   }

   public final static String getThemeErrorClass(AceTheme theme)
   {    
      if (theme == null || createDefault().isEqualTo(theme))
         return " ace_constant";
      else  
         return " ace_constant ace_language";
   }
   
}
