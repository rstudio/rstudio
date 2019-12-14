/*
 * LiveRegionWidget.java
 *
 * Copyright (C) 2019 by RStudio, Inc.
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

import com.google.gwt.aria.client.LiveValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.a11y.A11y;

/**
 * A visually hidden panel for performing aria-live region announcements.
 */
public class LiveRegionWidget extends Widget
{
   public static boolean ASSERTIVE = true;
   public static boolean POLITE = false;

   public LiveRegionWidget()
   {
      commonInit();
   }

   public LiveRegionWidget(boolean assertive)
   {
      commonInit();
      setAssertive(assertive);
   }

   private void commonInit()
   {
      setElement(Document.get().createDivElement());
      A11y.setVisuallyHidden(getElement());
      A11y.setStatusRole(getElement(), false /* non-assertive by default */);
   }

   public void setAssertive(boolean assertive)
   {
      Roles.getAlertRole().setAriaLiveProperty(getElement(),
            assertive ? LiveValue.ASSERTIVE : LiveValue.POLITE);
   }

   public void announce(String message, int speakDelayMs)
   {
      if (speakDelayMs < 0)
      {
         if (updateReaderTimer_.isRunning())
            updateReaderTimer_.cancel();
         return;
      }

      resultsMessage_ = message;
      if (updateReaderTimer_.isRunning())
         updateReaderTimer_.cancel();
      updateReaderTimer_.schedule(speakDelayMs);
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
      }
   };

   private String resultsMessage_;
}

