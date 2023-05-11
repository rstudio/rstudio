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
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

public class AceEditorMonitor
{
   public AceEditorMonitor(AceEditor editor)
   {
      editor_ = editor;
      init();
   }
   
   private void init()
   {
      editor_.addAttachHandler((AttachEvent event) -> {
         
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
      if (!monitoring_)
         return false;

      UserPrefs prefs = RStudioGinjector.INSTANCE.getUserPrefs();

      int scrollMultiplier = prefs.editorScrollMultiplier().getValue().intValue();
      // calculate speed ratio, lower scroll speed = higher ratio = faster scroll
      double ratio = WindowEx.get().getDevicePixelRatio() * (100.0 / scrollMultiplier);
      if (devicePixelRatio_ != ratio && editor_ != null)
      {
         devicePixelRatio_ = ratio;
         editor_.setScrollSpeed(ACE_EDITOR_DEFAULT_SCROLL_SPEED / ratio);
      }

      return true;
   }
   
   private void beginMonitoring()
   {
      if (monitoring_)
         return;
      
      monitoring_ = true;
      Scheduler.get().scheduleFixedDelay(() -> {
         return monitor();
      }, 1000);
   }
   
   private void endMonitoring()
   {
      monitoring_ = false;
      editor_ = null;
   }
 
   private boolean monitoring_ = false;
   private AceEditor editor_;
   
   private double devicePixelRatio_ = 1.0;
   
   private static final double ACE_EDITOR_DEFAULT_SCROLL_SPEED = 2.0;
   
}
