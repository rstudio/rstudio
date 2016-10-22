/*
 * DataImportOptionsCsvLocale.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import com.google.gwt.core.client.JavaScriptObject;

public class DataImportOptionsCsvLocale extends JavaScriptObject
{
   protected DataImportOptionsCsvLocale()
   {
   }
  
   public final native String getDateNames() /*-{
      return this.dateNames;
   }-*/;
   
   public final native String getDateFormat() /*-{
      return this.dateFormat_;
   }-*/;

   public final native String getTimeFormat() /*-{
      return this.timeFormat_;
   }-*/;

   public final native String getDecimalMark() /*-{
      return this.decimalMark_;
   }-*/;

   public final native String getGroupingMark() /*-{
      return this.groupingMark_;
   }-*/;
   
   public final native String getTZ() /*-{
      return this.tz;
   }-*/;

   public final native String getEncoding() /*-{
      return this.encoding;
   }-*/;

   public final native String getAsciify() /*-{
      return this.asciify;
   }-*/;
}