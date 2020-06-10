/*
 * AriaLiveStatusWidget.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;

/**
 * A visually hidden widget for performing aria-live status announcements. Can also
 * perform alerts, but those should be very rare; most alerts should be visible to both
 * sighted users and those using screen readers.
 */
public class AriaLiveStatusWidget extends Widget
                                  implements AriaLiveStatusReporter
{
   public AriaLiveStatusWidget()
   {
      setElement(Document.get().createDivElement());
      A11y.setVisuallyHidden(getElement());

      statusElement_ = Document.get().createDivElement();
      Roles.getStatusRole().set(statusElement_);

      alertElement_ = Document.get().createDivElement();
      Roles.getAlertRole().set(alertElement_);
      
      getElement().appendChild(statusElement_);
      getElement().appendChild(alertElement_);
   }

   public void reportStatus(String message, int speakDelayMs, Severity severity)
   {
      if (speakDelayMs < 0)
      {
         clearTimers();
         clearMessage();
         return;
      }

      resultsMessage_ = message;
      severity_ = severity;
      clearTimers();
      updateReaderTimer_.schedule(speakDelayMs);
   }

   public void clearTimers()
   {
      if (updateReaderTimer_.isRunning())
         updateReaderTimer_.cancel();
      if (clearReaderTimer_.isRunning())
         clearReaderTimer_.cancel();
   }
   
   public void clearMessage()
   {
      statusElement_.setInnerText("");
      alertElement_.setInnerText("");
   }

   /**
    * Timer for reporting the results via aria-live (to avoid interrupting typing)
    */
   private Timer updateReaderTimer_ = new Timer()
   {
      @Override
      public void run()
      {
         if (severity_ == Severity.STATUS)
            statusElement_.setInnerText(resultsMessage_);
         else
            alertElement_.setInnerText(resultsMessage_);
         if (clearReaderTimer_.isRunning())
            clearReaderTimer_.cancel();
         clearReaderTimer_.schedule(CLEAR_PRIOR_MESSAGE_DELAY);
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
        clearMessage();
     }
   };

   private static final int CLEAR_PRIOR_MESSAGE_DELAY = 4000;
   private String resultsMessage_;
   private Element statusElement_;
   private Element alertElement_;
   private Severity severity_;
   
}
