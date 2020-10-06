/*
 * ChunkRowExecState.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.user.client.Timer;

public abstract class ChunkRowExecState
{
   public ChunkRowExecState(int state)
   {
      state_ = state;
   }

   // Public methods ----------------------------------------------------------

   public int getState()
   {
      return state_;
   }
   
   public void setState(int state)
   {
      // ignore if already at this state
      if (state_ == state)
         return;
      
      // if the moving to the error state, clean other states
      if (state == LINE_ERROR)
         removeClazz();
      
      // if this is the error state, there's no transition to the resting state
      if (state_ == LINE_ERROR && state == LINE_RESTING)
         return;
      
      state_ = state;
      resetTimer();
      if (state_ == LINE_RESTING)
      {
         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               addClazz(state_);
               scheduleDismiss();
            }
         };
         timer_.schedule(LINGER_MS);
      }
      else
      {
         addClazz(state_);
      }
   }
   
   public void detach()
   {
      resetTimer();
      removeClazz();
   }

   // Abstract methods --------------------------------------------------------

   protected abstract void addClazz(int state);
   
   protected abstract void removeClazz();
   
   // Private methods ---------------------------------------------------------
   
   private void scheduleDismiss()
   {
      resetTimer();
      timer_ = new Timer()
      {
         @Override
         public void run()
         {
            detach();
         }
      };
      timer_.schedule(FADE_MS);
   }
   
   private void resetTimer()
   {
      if (timer_ != null && timer_.isRunning())
      {
         timer_.cancel();
         timer_ = null;
      }
   }

   protected int state_;

   private Timer timer_;
   
   private final static int LINGER_MS = 250;
   private final static int FADE_MS   = 400;

   public final static int LINE_QUEUED   = 0;
   public final static int LINE_EXECUTED = 1;
   public final static int LINE_RESTING  = 2;
   public final static int LINE_ERROR    = 3;
   public final static int LINE_NONE     = 4;
}
