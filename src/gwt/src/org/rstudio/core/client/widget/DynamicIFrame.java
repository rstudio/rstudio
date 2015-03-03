/*
 * DynamicIFrame.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Frame;

import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;

public abstract class DynamicIFrame extends Frame
{
   public DynamicIFrame()
   {
      // wait for the window object to become available
      final Scheduler.RepeatingCommand loadFrame = new Scheduler.RepeatingCommand()
      {
         @Override
         public boolean execute()
         {
            if (getIFrame() == null || getWindow() == null || getDocument() == null)
               return true;
            onFrameLoaded();
            return false;
         }
      };

      // defer an attempt to pull the window object; if it isn't successful, try
      // every 50ms
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            if (loadFrame.execute()) 
            {
               Scheduler.get().scheduleFixedDelay(loadFrame, 50);
            }
         }
      });
   }

   protected abstract void onFrameLoaded();
   

   protected IFrameElementEx getIFrame()
   {
      return getElement().cast();
   }

   protected WindowEx getWindow()
   {
      return getIFrame().getContentWindow();
   }

   protected final Document getDocument()
   {
      return getWindow().getDocument();
   }
}
