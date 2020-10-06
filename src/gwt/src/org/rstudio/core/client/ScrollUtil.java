/*
 * ScrollUtil.java
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
package org.rstudio.core.client;

import org.rstudio.core.client.widget.RStudioFrame;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;

public class ScrollUtil
{
   public static void setScrollPositionOnLoad(final RStudioFrame frame, 
                                              final int scrollPosition)
   {
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
         @Override
         public boolean execute()
         {
            // don't wait indefinitely for the document to load
            retries_++;
            if (retries_ > MAX_SCROLL_RETRIES)
               return false;

            // wait for a document to become available in the frame
            if (frame.getIFrame() == null)
               return true;
            
            if (frame.getIFrame().getContentDocument() == null)
               return true;

            // wait for the document to finish loading
            Document doc = frame.getIFrame().getContentDocument();
            String readyState = getDocumentReadyState(doc);
            if (readyState == null)
               return true;
            
            if (!readyState.equals("complete"))
               return true;
            
            // wait for a real document to load (about:blank may be intermediate)
            if (doc.getScrollTop() > 0)
               return true;

            if (doc.getURL() == URIConstants.ABOUT_BLANK)
               return true;
            
            // restore scroll position
            if (scrollPosition > 0)
               doc.setScrollTop(scrollPosition);

            return false;
         }
         
         private int retries_ = 0;
      }, SCROLL_RETRY_MS);
   }

   private final native static String getDocumentReadyState(Document doc) /*-{
      return doc.readyState || null;
   }-*/;
   
   private final static int SCROLL_RETRY_MS = 50;
   private final static int MAX_SCROLL_RETRIES = 200;  // 10s
}
