/*
 * WorkbenchMetrics.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;

public class WorkbenchMetrics extends JavaScriptObject
{
   protected WorkbenchMetrics()
   {
   }
   
   public static final native WorkbenchMetrics create(int consoleWidth,
                                                      int graphicsWidth,
                                                      int graphicsHeight) /*-{
      var clientMetrics = new Object();
      clientMetrics.consoleWidth = consoleWidth ;
      clientMetrics.graphicsWidth = graphicsWidth ;
      clientMetrics.graphicsHeight = graphicsHeight ;
      return clientMetrics ;
   }-*/;
   
   public final native int getConsoleWidth() /*-{
      return this.consoleWidth;
   }-*/;

   public final native int getGraphicsWidth() /*-{
      return this.graphicsWidth ;
   }-*/;

   public final native int getGraphicsHeight() /*-{
      return this.graphicsHeight;
   }-*/;
   
   public final boolean equalTo(WorkbenchMetrics other)
   {
      return (getConsoleWidth() == other.getConsoleWidth() &&
              getGraphicsWidth() == other.getGraphicsWidth() && 
              getGraphicsHeight() == other.getGraphicsHeight());
   }
}
