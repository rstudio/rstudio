/*
 * CodeBrowserContents.java
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

import java.util.HashMap;

import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.JavaScriptObject;

public class CodeBrowserContents extends JavaScriptObject
{
   protected CodeBrowserContents()
   {
   }

   public static final native CodeBrowserContents create(String context) /*-{
      var contents = new Object();
      contents.context = context;
      return contents;
   }-*/;
   
   
   public native final String getContext() /*-{
      return this.context;
   }-*/;
   
   public final boolean equalTo(CodeBrowserContents other)
   {
      return getContext() == other.getContext();
   }
   
   public final void fillProperties(HashMap<String, String> properties)
   {
      properties.put("context", getContext());
   }

   public final void fillProperties(JsObject properties)
   {
      properties.setString("context", getContext());
   }
}
