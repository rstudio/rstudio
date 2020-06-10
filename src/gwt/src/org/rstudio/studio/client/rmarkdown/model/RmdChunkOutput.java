/*
 * RmdChunkOutput.java
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

import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class RmdChunkOutput extends JavaScriptObject
{
   protected RmdChunkOutput()
   { }
   
   public final native String getChunkId() /*-{
      return this.chunk_id;
   }-*/;

   public final native String getDocId() /*-{
      return this.doc_id;
   }-*/;

   public final native JsArray<RmdChunkOutputUnit> getUnits() /*-{
      return this.chunk_outputs || [];
   }-*/;
   
   public final native RmdChunkOutputUnit getUnit() /*-{
      return this.chunk_output;
   }-*/;
   
   public final native String getRequestId() /*-{
      return this.request_id;
   }-*/;
   
   public final int getType() 
   {
      if (getUnit() == null)
         return TYPE_MULTIPLE_UNIT;
      return TYPE_SINGLE_UNIT;
   }
   
   public final boolean isEmpty() 
   {
      return (getUnits() == null || getUnits().length() == 0) &&
             getUnit() == null;
   }
   
   public final boolean isReplay()
   {
      return !StringUtil.isNullOrEmpty(getRequestId());
   }
   
   public static final int TYPE_SINGLE_UNIT   = 0;
   public static final int TYPE_MULTIPLE_UNIT = 1;
   
   public static final int EXEC_CHUNK_BODY    = 0;
   public static final int EXEC_CHUNK_SECTION = 1;
}
