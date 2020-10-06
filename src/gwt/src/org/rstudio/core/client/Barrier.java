/*
 * Barrier.java
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

import com.google.gwt.event.shared.HandlerManager;
import org.rstudio.core.client.events.BarrierReleasedEvent;

/**
 * Use this class to track when multiple independent operations are all
 * completed.
 *
 * Each operation should call acquire() when it begins to get a token,
 * then release() on the token when the operation is complete. When all
 * outstanding tokens are released, then BarrierReleasedEvent is fired.
 *
 * As a safety precaution, all but the first call to release() on a
 * given token is a no-op. 
 */
public class Barrier
{
   public class Token
   {
      public Token()
      {
      }

      /**
       * Call when the operation is completed (or errors out, or is aborted,
       * etc.)
       */
      public void release()
      {
         if (!released_)
         {
            released_ = true;
            Barrier.this.release();
         }
      }

      private boolean released_;
   }

   /**
    * Call at the beginning of an operation.
    */
   public Token acquire()
   {
      count_++;
      return new Token();
   }

   public void addBarrierReleasedHandler(BarrierReleasedEvent.Handler handler)
   {
      handlers_.addHandler(BarrierReleasedEvent.TYPE, handler);
   }


   private void release()
   {
      if (--count_ == 0)
      {
         handlers_.fireEvent(new BarrierReleasedEvent());
      }
   }

   private int count_;
   private HandlerManager handlers_ = new HandlerManager(this);
}
