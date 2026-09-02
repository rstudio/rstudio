/*
 * StartupTiming.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

/**
 * Records client startup milestones as User Timing marks ("rstudio:&lt;name&gt;").
 * The marks show up in the browser's Performance panel, and RStudio Desktop
 * harvests them into the RSTUDIO_STARTUP_TIMING file alongside its own and
 * the R session's checkpoints (see tasks/startup-timing.ts).
 */
public class StartupTiming
{
   public static native void mark(String name) /*-{
      if ($wnd.performance && $wnd.performance.mark)
         $wnd.performance.mark("rstudio:" + name);
   }-*/;

   /**
    * Records a mark once the browser has painted the frame following the
    * current one; used to approximate when a just-attached UI is visible.
    */
   public static native void markAfterPaint(String name) /*-{
      if (!$wnd.requestAnimationFrame)
         return;
      $wnd.requestAnimationFrame(function() {
         $wnd.setTimeout(function() {
            @org.rstudio.core.client.StartupTiming::mark(Ljava/lang/String;)(name);
         }, 0);
      });
   }-*/;
}
