/*
 * DataPreviewResult.java
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
package org.rstudio.studio.client.workbench.views.environment.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import org.rstudio.core.client.js.JsObject;

public class DataPreviewResult extends JavaScriptObject
{
   protected DataPreviewResult()
   {
   }

   public final native String getInputLines() /*-{
      return this.inputLines[0];
   }-*/;

   public final native JsArray<JsObject> getOutput() /*-{
      return this.output;
   }-*/;

   public final native String getEncoding() /*-{
      return this.encoding[0];
   }-*/;

   public final native JsArrayString getOutputNames() /*-{
      return this.outputNames;
   }-*/;

   public final native boolean hasHeader() /*-{
      return this.header[0];
   }-*/;

   public final native String getSeparator() /*-{
      return this.separator[0];
   }-*/;

   public final native String getDecimal() /*-{
      return this.decimal[0];
   }-*/;

   public final native String getQuote() /*-{
      return this.quote[0];
   }-*/;
   
   public final native String getComment() /*-{
      return this.comment[0];
   }-*/;
   
   public final native boolean getDefaultStringsAsFactors() /*-{
      return this.defaultStringsAsFactors[0];
   }-*/;
}
