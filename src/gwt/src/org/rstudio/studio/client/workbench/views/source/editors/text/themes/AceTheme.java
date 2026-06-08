/*
 * AceTheme.java
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

import java.util.Map;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.workbench.prefs.model.UserStateAccessor;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;

import com.google.gwt.core.client.GWT;

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
         return create(constants_.tomorrowNight(), "theme/default/tomorrow_night.rstheme", true);
      }
      else
      {
         return create(constants_.textmateDefaultParentheses(), "theme/default/textmate.rstheme", false);
      }
   }

   /**
    * Resolve the editor theme that actually applies, following the single
    * "selected -> global -> built-in default" fallback that defines which theme
    * the IDE renders. Callers pass the preferred name (an effective/project
    * value or the pane's current selection) and the global fallback; an
    * uninstalled name drops to the next tier. Returns null only when the theme
    * list is unavailable (null or empty), which callers treat as "leave the
    * current theme in place."
    */
   public static final AceTheme resolveApplied(Map<String, AceTheme> themeList,
                                               String selectedName,
                                               String globalName)
   {
      if (themeList == null || themeList.isEmpty())
         return null;

      AceTheme theme = themeList.get(selectedName);
      if (theme == null)
         theme = themeList.get(globalName);
      if (theme == null)
         theme = themeList.get(createDefault().getName());
      return theme;
   }

   public static final native AceTheme create(String name, String url, Boolean isDark)
   /*-{
      return {
         name: name,
         url: url,
         isDark: isDark
      };
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
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}