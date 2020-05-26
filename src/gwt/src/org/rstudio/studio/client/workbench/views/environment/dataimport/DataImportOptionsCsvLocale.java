/*
 * DataImportOptionsCsvLocale.java
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import com.google.gwt.core.client.JavaScriptObject;

public class DataImportOptionsCsvLocale extends JavaScriptObject
{
   protected DataImportOptionsCsvLocale()
   {
   }
   
   public static native DataImportOptionsCsvLocale createLocaleNative(
      String dateName,
      String dateFormat,
      String timeFormat,
      String decimalMark,
      String groupingMark,
      String tz,
      String encoding,
      boolean asciify) /*-{
      return {
         dateName: dateName,
         dateFormat: dateFormat,
         timeFormat: timeFormat,
         decimalMark: decimalMark,
         groupingMark: groupingMark,
         tz: tz,
         encoding: encoding,
         asciify: asciify
      }
   }-*/;
  
   public final native String getDateName() /*-{
      return this.dateName;
   }-*/;
   
   public final native String getDateFormat() /*-{
      return this.dateFormat;
   }-*/;

   public final native String getTimeFormat() /*-{
      return this.timeFormat;
   }-*/;

   public final native String getDecimalMark() /*-{
      return this.decimalMark;
   }-*/;

   public final native String getGroupingMark() /*-{
      return this.groupingMark;
   }-*/;
   
   public final native String getTZ() /*-{
      return this.tz;
   }-*/;

   public final native String getEncoding() /*-{
      return this.encoding;
   }-*/;

   public final native boolean getAsciify() /*-{
      return this.asciify;
   }-*/;

   public static DataImportOptionsCsvLocale createLocale(
      String dateName,
      String dateFormat,
      String timeFormat,
      String decimalMark,
      String groupingMark,
      String tz,
      String encoding,
      boolean asciify)
   {
      return createLocaleNative(
         dateName,
         dateFormat,
         timeFormat,
         decimalMark,
         groupingMark,
         tz,
         encoding,
         asciify);
   }
}
