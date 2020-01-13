/*
 * AriaLiveStatusWidget.java
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.a11y.A11y;

/**
 * A visually hidden widget for performing aria-live status announcements.
 */
public class AriaLiveStatusWidget extends Widget
{
   public AriaLiveStatusWidget()
   {
      setElement(Document.get().createDivElement());
      A11y.setVisuallyHidden(getElement());
      Roles.getStatusRole().set(getElement());
   }

   public void announce(String message, int speakDelayMs)
   {
      if (speakDelayMs < 0)
      {
         clearMessage();
         return;
      }

      resultsMessage_ = message;
      if (updateReaderTimer_.isRunning())
         updateReaderTimer_.cancel();
      if (clearReaderTimer_.isRunning())
         clearReaderTimer_.cancel();
      updateReaderTimer_.schedule(speakDelayMs);
   }

   public void clearMessage()
   {
      if (updateReaderTimer_.isRunning())
         updateReaderTimer_.cancel();
      if (clearReaderTimer_.isRunning())
         clearReaderTimer_.cancel();
      getElement().setInnerText("");
   }

   /**
    * Timer for reporting the results via aria-live (to avoid interrupting typing)
    */
   private Timer updateReaderTimer_ = new Timer()
   {
      @Override
      public void run()
      {
         getElement().setInnerText(resultsMessage_);
         if (clearReaderTimer_.isRunning())
            clearReaderTimer_.cancel();
         clearReaderTimer_.schedule(4000);
      }
   };

   /**
    * Timer for clearing the previous message if nothing new arrives
    */
   private Timer clearReaderTimer_ = new Timer()
   {
     @Override
     public void run()
     {
        getElement().setInnerText("");
     }
   };

   private String resultsMessage_;
}
