/*
 * WorkbenchMetrics.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
                                                      int graphicsHeight,
                                                      boolean isRetina) /*-{
      var clientMetrics = new Object();
      clientMetrics.consoleWidth = consoleWidth ;
      clientMetrics.graphicsWidth = graphicsWidth ;
      clientMetrics.graphicsHeight = graphicsHeight ;
      clientMetrics.isRetina = isRetina;
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
   
   public final native int isRetina() /*-{
      return this.isRetina;
   }-*/;
   
   public final boolean equalTo(WorkbenchMetrics other)
   {  
      return (other != null &&
              getConsoleWidth() == other.getConsoleWidth() &&
              getGraphicsWidth() == other.getGraphicsWidth() && 
              getGraphicsHeight() == other.getGraphicsHeight() &&
              isRetina() == other.isRetina());
   }
   
   // are the metrics "close enough"to the previous ones such that we don't 
   // need to send an update? graphics always have to be equal but if the 
   // console width has gotten 1 or 2 characters wider then we can 
   // suppress sending. note we do this to avoid chatty set_workbench_metrics
   // calls (failing to increase the width of the console merely results 
   // in some extra whitespace at the right margin)
   public final boolean closeEnoughToPrevious(WorkbenchMetrics previous)
   {  
      // if previous is null then we're not close enough
      if (previous == null)
         return false;
      
      // new width offset
      int newWidthOffset = getConsoleWidth() - previous.getConsoleWidth();
      
      // if it's 0, 1, or 2 larger then it's close enough
      boolean consoleCloseEnough = newWidthOffset >= 0 && newWidthOffset <= 2;
         
      return (consoleCloseEnough &&
              (getGraphicsWidth() == previous.getGraphicsWidth()) && 
              (getGraphicsHeight() == previous.getGraphicsHeight()));
   }
}
