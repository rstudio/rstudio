/*
 * XRef.java
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
package org.rstudio.core.client;

import com.google.gwt.core.client.JavaScriptObject;

public class XRef extends JavaScriptObject
{
   protected XRef()
   {
   }

   public static final native XRef create()
   /*-{
      return {};
   }-*/;

   public final native boolean hasXRefString()
   /*-{
      return !!this.type && !!this.id;
   }-*/;

   public final native String getXRefString()
   /*-{
      return this.type + ":" + this.id;
   }-*/;

   public final native String getFile()  /*-{ return this["file"];  }-*/;
   public final native String getType()  /*-{ return this["type"];  }-*/;
   public final native String getId()    /*-{ return this["id"];    }-*/;
   public final native String getTitle() /*-{ return this["title"]; }-*/;
}
