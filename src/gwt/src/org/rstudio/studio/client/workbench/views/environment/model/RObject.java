/*
 * RObject.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class RObject extends JavaScriptObject
{
   protected RObject()
   {
   }
   
   public final native String getName() /*-{
      return this.name;
   }-*/;

   public final native String getType() /*-{
      return this.type;
   }-*/;
   
   public final native JsArrayString getClazz() /*-{
      return this.clazz || [];
   }-*/;
   
   public final native boolean isData() /*-{
      return this.is_data;
   }-*/;

   public final native String getValue() /*-{
      return this.value ? this.value : "NO_VALUE";
   }-*/;

   public final native String getDescription() /*-{
       return this.description ? this.description : "";
   }-*/;

   public final native JsArrayString getContents() /*-{
      return this.contents;
   }-*/;

   public final native int getLength() /*-{
      return this.length;
   }-*/;
   
   public final native int getSize() /*-{
      return this.size;
   }-*/;
   
   public final native boolean getContentsDeferred() /*-{
      return this.contents_deferred;
   }-*/;
   
   public final native void setDeferredContents(JsArrayString contents) /*-{
      this.contents_deferred = false;
      this.contents = contents;
   }-*/;
}

