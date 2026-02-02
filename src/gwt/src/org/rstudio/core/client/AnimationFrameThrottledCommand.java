/*
 * AnimationFrameThrottledCommand.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.core.client;

import com.google.gwt.animation.client.AnimationScheduler;

/**
 * Throttles execution of an action to the browser's animation frame rate
 * (~60fps or display refresh rate). Multiple calls to nudge() within a
 * single frame will be coalesced into one call to performAction().
 *
 * This is useful for throttling expensive visual updates triggered by
 * high-frequency events like mouse moves or resize operations.
 */
public abstract class AnimationFrameThrottledCommand
{
   /**
    * Implement this method to perform the throttled action.
    */
   protected abstract void performAction();

   /**
    * Request that performAction() be called on the next animation frame.
    * Multiple calls to nudge() before the next frame will be coalesced.
    */
   public final void nudge()
   {
      if (pending_)
         return;

      pending_ = true;
      AnimationScheduler.get().requestAnimationFrame(timestamp -> {
         pending_ = false;
         performAction();
      });
   }

   /**
    * Returns whether an animation frame is currently pending.
    */
   public final boolean isPending()
   {
      return pending_;
   }

   private boolean pending_ = false;
}
