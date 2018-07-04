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

/**
 * Represents an editor theme.
 */
public class AceTheme extends JavaScriptObject
{
   protected AceTheme() {}
   
   public static final AceTheme createDefault()
   {
      return create("Textmate (default)", "/theme/default/textmate.rstheme", false);
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
      return this.url;
   }-*/;
   
   public native final Boolean isDark()
   /*-{
      return this.isDark;
   }-*/;
   
}
