/*
 * DataView.java
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
package
org.rstudio.studio.client.workbench.views.data.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;

public class DataView extends JavaScriptObject
{
   protected DataView()
   {
   }
   
   public static class ColumnInfo extends JavaScriptObject
   {
      protected ColumnInfo()
      {
      }
      
      public static final int STRING = 0;
      public static final int NUMBER = 1;
      
      public final native String getName() /*-{
         return this.name;
      }-*/;
      
      public final native int getType() /*-{
         return this.type;
      }-*/;
   }
   
   public final native String getTitle() /*-{
      return this.title;
   }-*/;

   public final native JsArray<ColumnInfo> getColumnInfo() /*-{
      return this.columnInfo;
   }-*/;
   
   public final native int getRowCount() /*-{
      return this.rowCount;
   }-*/;
   
   public final native JsArrayString getStringColumn(int index) /*-{
      return this.columns[index];
   }-*/;
   
   public final native JsArrayNumber getNumberColumn(int index) /*-{
      return this.columns[index];
   }-*/;  
}
