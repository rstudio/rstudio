/*
 * ContentItem.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ContentItem extends JavaScriptObject
{
   protected ContentItem()
   {
   }

   public static final native ContentItem create(String title,
                                                 String contentUrl) /*-{
      var contentItem = new Object();
      contentItem.title = title;
      contentItem.contentUrl = contentUrl;
      return contentItem;
   }-*/;
   
   
   public native final String getTitle() /*-{
      return this.title;
   }-*/;
   
   public native final String getContentUrl() /*-{
      return this.contentUrl;
   }-*/;
}
