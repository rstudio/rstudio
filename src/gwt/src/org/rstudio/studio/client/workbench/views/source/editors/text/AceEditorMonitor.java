/*
 * AceEditorMonitor.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.dom.WindowEx;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.AttachEvent;

public class AceEditorMonitor
{
   public AceEditorMonitor(AceEditor editor)
   {
      editor_ = editor;
      init();
   }
   
   private void init()
   {
      editor_.addAttachHandler((AttachEvent event) ->
      {
         if (event.isAttached())
         {
            beginMonitoring();
         }
         else
         {
            endMonitoring();
         }
      });
   }
   
   private boolean monitor()
   {
      if (!monitoring_ || editor_ == null)
         return false;

      // check if the device pixel ratio has changed;
      // if not, then nothing to do (but continue monitoring)
      double devicePixelRatio = WindowEx.get().getDevicePixelRatio();
      if (devicePixelRatio == devicePixelRatio_)
         return true;

      // the dpi has changed; synchronize the scroll speed
      editor_.getWidget().syncScrollSpeed();
      return true;
   }
   
   private void beginMonitoring()
   {
      if (monitoring_)
         return;
      
      monitoring_ = true;
      Scheduler.get().scheduleFixedDelay(this::monitor, 1000);
   }
   
   private void endMonitoring()
   {
      monitoring_ = false;
      editor_ = null;
   }
 
   private AceEditor editor_;
   private boolean monitoring_ = false;
   private double devicePixelRatio_ = 0.0;
   
}
