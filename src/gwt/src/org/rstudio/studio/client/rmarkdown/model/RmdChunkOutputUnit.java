/*
 * RmdChunkOutputUnit.java
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
package org.rstudio.studio.client.rmarkdown.model;

import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class RmdChunkOutputUnit extends JavaScriptObject
{
   protected RmdChunkOutputUnit()
   {
   }

   public final native int getType() /*-{
      return this.output_type;
   }-*/;
   
   public final native String getString() /*-{
      return this.output_val;
   }-*/;
   
   public final native JsArray<JsArrayEx> getArray() /*-{
      return this.output_val;
   }-*/;
   
   public final native UnhandledError getUnhandledError() /*-{
      return this.output_val;
   }-*/;
   
   public final native int getOrdinal() /*-{
      return this.output_ordinal;
   }-*/;

   public final native JavaScriptObject getOutputObject() /*-{
      return this.output_val;
   }-*/;
   
   public final native JavaScriptObject getMetadata() /*-{
      return this.output_metadata || {};
   }-*/;
   
   // symmetric with server-side enumeration
   public final static int TYPE_NONE    = 0;
   public final static int TYPE_TEXT    = 1; 
   public final static int TYPE_PLOT    = 2;
   public final static int TYPE_HTML    = 3;
   public final static int TYPE_ERROR   = 4;
   public final static int TYPE_ORDINAL = 5;
   public final static int TYPE_DATA    = 6;
}
