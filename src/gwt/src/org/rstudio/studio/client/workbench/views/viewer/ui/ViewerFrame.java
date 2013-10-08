package org.rstudio.studio.client.workbench.views.viewer.ui;

import org.rstudio.core.client.dom.IFrameElementEx;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.ui.Frame;

/*
 * ViewerFrame.java
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

public class ViewerFrame extends Frame
{
   public void navigate(final String url)
   {
      // allow subsequent calls to nativate override previous calls
      targetUrl_ = url;
      
      RepeatingCommand navigateCommand = new RepeatingCommand() {
         @Override
         public boolean execute()
         {
            IFrameElementEx iFrame = getIFrameEx();
            if (iFrame != null && iFrame.getContentWindow() != null)
            {
               if (targetUrl_.equals(getUrl()))
               {
                  iFrame.getContentWindow().reload();
               }
               else
               {
                  iFrame.getContentWindow().replaceLocationHref(targetUrl_);
                  setUrl(targetUrl_);
               }
               return false;
            }
            else
            {
               return true;
            }
         }
      };

      if (navigateCommand.execute())
         Scheduler.get().scheduleFixedDelay(navigateCommand, 100);      
   }
   
   private IFrameElementEx getIFrameEx()
   {
      return getElement().cast();
   }
   
   private String targetUrl_;
}