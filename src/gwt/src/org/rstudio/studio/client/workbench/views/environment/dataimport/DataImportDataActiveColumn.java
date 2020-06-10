/*
 * DataImportDataActiveColumn.java
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

public class DataImportDataActiveColumn extends JavaScriptObject
{
   protected DataImportDataActiveColumn()
   {
      
   }
   
   public final native int getTop() /*-{
      return this.top;
   }-*/;
   
   public final native int getLeft() /*-{
      return this.left;
   }-*/;
   
   public final native int getWidth() /*-{
      return this.width;
   }-*/;
   
   public final native int getIndex() /*-{
      return this.index;
   }-*/;
   
   public final native String getName() /*-{
      return this.name.toString();
   }-*/;
}
