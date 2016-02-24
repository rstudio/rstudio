/*
 * DocumentIdleBackgroundTask.java
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
package org.rstudio.core.client;

import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

public class DocumentIdleBackgroundTask
{
   public static abstract class BackgroundTask
   {
      // Return 'true' to continue execution, 'false' to end execution
      public abstract boolean continueExecution(Position pos, boolean isMouse);
      
      // Return 'false' to signal a stop / failure to start
      public boolean onStart() { return true; }
      public void onStop() { return; }
   }
   
   public DocumentIdleBackgroundTask(DocDisplay docDisplay,
                                     BackgroundTask task)
   {
      this(docDisplay, 500, 500, task);
   }
   
   public DocumentIdleBackgroundTask(DocDisplay docDisplay,
                                     long pollDelayMs,
                                     long idleThresholdMs,
                                     BackgroundTask task)
   {
      docDisplay_ = docDisplay;
      pollDelayMs_ = pollDelayMs;
      idleThresholdMs_ = idleThresholdMs;
      task_ = task;
      
      docDisplay_.addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            start();
         }
      });
      
      docDisplay_.addBlurHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            stop();
            if (handler_ != null)
            {
               handler_.removeHandler();
               handler_ = null;
            }
         }
      });
   }
   
   public void start()
   {
      assert !isRunning_ : "Background process already running!";
      
      isRunning_ = true;
      stopRequested_ = false;
      
      handler_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent event)
         {
            int eventType = event.getTypeInt();
            lastEventWasMouseMove_ = eventType == Event.ONMOUSEMOVE;
            
            if (lastEventWasMouseMove_)
            {
               lastMouseMoveTime_ = System.currentTimeMillis();
               lastMouseCoords_ = ScreenCoordinates.create(
                     event.getNativeEvent().getClientX(),
                     event.getNativeEvent().getClientY());
            }
         }
      });
      
      if (!task_.onStart())
         return;
      
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         @Override
         public boolean execute()
         {
            if (stopRequested_)
               return stopExecution();
            
            long currentTime = System.currentTimeMillis();
            long lastModifiedTime = docDisplay_.getLastModifiedTime();
            long lastCursorChangedTime = docDisplay_.getLastCursorChangedTime();
            
            if ((currentTime - lastModifiedTime) < idleThresholdMs_)
               return true;
            
            if ((currentTime - lastCursorChangedTime) < idleThresholdMs_)
               return true;
            
            if ((currentTime - lastMouseMoveTime_) < idleThresholdMs_)
               return true;
            
            Position position =
                  lastEventWasMouseMove_ ?
                        docDisplay_.toDocumentPosition(lastMouseCoords_) :
                           docDisplay_.getCursorPosition();
            
            return task_.continueExecution(position, lastEventWasMouseMove_);
         }
      }, (int) pollDelayMs_);
      
   }
   
   public void stop()
   {
      stopRequested_ = true;
   }
   
   private boolean stopExecution()
   {
      task_.onStop();
      isRunning_ = false;
      stopRequested_ = false;
      return false;
   }
   
   private boolean isRunning_;
   private boolean stopRequested_;
   
   private final DocDisplay docDisplay_;
   private final long pollDelayMs_;
   private final long idleThresholdMs_;
   private final BackgroundTask task_;
   
   private long lastMouseMoveTime_;
   private ScreenCoordinates lastMouseCoords_;
   private HandlerRegistration handler_;
   private boolean lastEventWasMouseMove_;
}
