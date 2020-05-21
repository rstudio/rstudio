/*
 * EnvironmentClientState.java
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

package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class EnvironmentClientState extends JavaScriptObject
{
   protected EnvironmentClientState()
   {
   }

   public static final native EnvironmentClientState create(
           int scrollPosition,
           String[] expandedObjects, 
           int sortColumn,
           boolean ascendingSort) /*-{
       var options = new Object();
       options.scroll_position = scrollPosition;
       options.expanded_objects = expandedObjects;
       options.sort_column = sortColumn;
       options.ascending_sort = ascendingSort;
       return options;
   }-*/;

   public final native int getScrollPosition() /*-{
      return this.scroll_position;
   }-*/;

   public final native JsArrayString getExpandedObjects() /*-{
      return this.expanded_objects ? this.expanded_objects : [];
   }-*/;
   
   public final native int getSortColumn() /*-{
      return this.sort_column;
   }-*/;

   public final native boolean getAscendingSort() /*-{
      return this.ascending_sort;
   }-*/;
}
