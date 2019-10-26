/*
 * LocalReplaceProgress.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.find.model;

import com.google.gwt.core.client.JavaScriptObject;

public class LocalReplaceProgress extends JavaScriptObject
{
   /**
    * Summarize currently running replace
    * 
    * @param units The total number of progress units completed
    * @param max The total number of progress units remaining
    * @param elapsed The time that has elapsed on the server
    * @param received The time that the client last received an update
    */
   public static native LocalReplaceProgress create(int units, int max,
                                                    int elapsed, int received) /*-{
      return ({
         units: units,
         max: max,
         elapsed: elapsed,
         received: received
      });
   }-*/;
   
   protected LocalReplaceProgress() {}

   public native final int getUnits() /*-{
      return units;
   }-*/;
   
   public native final int max() /*-{
      return max;
   }-*/;
   
   public native final int elapsed() /*-{
      return elapsed;
   }-*/;
   
   public native final int received() /*-{
      return received;
   }-*/;

   /*
   public double percent() {
      return ((double)units() / (double) max()) * 100;
   };
   */
}
