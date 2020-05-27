/*
 * SlideNavigationItem.java
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
package org.rstudio.studio.client.common.presentation.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SlideNavigationItem extends JavaScriptObject
{
   protected SlideNavigationItem()
   {
   }
   
   public static final native SlideNavigationItem create(String title,
                                                         int indent,
                                                         int index,
                                                         int line) /*-{
        var item = new Object();
        item.title = title;
        item.indent = indent;
        item.index = index;
        item.line = line;
        return item;
   }-*/;
      
   public final native String getTitle() /*-{
      return this.title;
   }-*/;
 
   public final native int getIndent() /*-{
      return this.indent;
   }-*/;
   
   public final native int getIndex() /*-{
      return this.index;
   }-*/;
   
   public final native int getLine() /*-{
      return this.line;
   }-*/;
}
