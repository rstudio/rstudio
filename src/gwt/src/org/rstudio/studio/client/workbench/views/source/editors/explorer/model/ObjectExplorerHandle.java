/*
 * ObjectExplorerHandle.java
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
package org.rstudio.studio.client.workbench.views.source.editors.explorer.model;

import java.util.HashMap;

import com.google.gwt.core.client.JavaScriptObject;

import org.rstudio.core.client.js.JsObject;

public class ObjectExplorerHandle extends JavaScriptObject
{
   public static final String URI_PREFIX = "explorer://";
   
   protected ObjectExplorerHandle()
   {
   }
   
   public final native String getId()       /*-{ return this["id"];    }-*/;
   public final native String getName()     /*-{ return this["name"];  }-*/;
   public final native String getTitle()    /*-{ return this["title"]; }-*/;
   public final native String getLanguage() /*-{ return this["language"]; }-*/;
   
   public final String getPath()
   {
      return URI_PREFIX + getId();
   }

   public final void fillProperties(HashMap<String, String> properties)
   {
      properties.put("id", getId());
      properties.put("name", getName());
      properties.put("title", getTitle());
      properties.put("language", getLanguage());
   }

   public final void fillProperties(JsObject properties)
   {
      properties.setString("id", getId());
      properties.setString("name", getName());
      properties.setString("title", getTitle());
      properties.setString("language", getLanguage());
   }
}
