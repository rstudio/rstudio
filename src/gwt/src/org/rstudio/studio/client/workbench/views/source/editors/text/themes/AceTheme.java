/*
 * AceTheme.java
 *
 * Copyright (C) 2018 by RStudio, Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;

/**
 * Represents an editor theme.
 */
public class AceTheme extends JavaScriptObject
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
   
   public native final String getName()
   /*-{
      return this.name;
   }-*/;
   
   public native final String getUrl()
   /*-{
      if ((this.url !== null) &&
          (this.url !== undefined) &&
          (this.url !== "") &&
          (this.url.charAt(0) === '/'))
         return this.url.substr(1);
      else
         return this.url;
   }-*/;
   
   public native final Boolean isDark()
   /*-{
      return this.isDark;
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
}
