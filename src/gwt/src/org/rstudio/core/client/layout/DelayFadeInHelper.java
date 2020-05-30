/*
 * DelayFadeInHelper.java
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
package org.rstudio.core.client.layout;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

public class DelayFadeInHelper
{
   public DelayFadeInHelper(Widget widget)
   {
      this(widget, 250);
   }

   public DelayFadeInHelper(Widget widget, int ms)
   {
      this(widget, ms, null);
   }

   /**
    * @param widget widget to fadein
    * @param ms animation duration
    * @param callback optional callback to invoke after fadein complete
    */
   public DelayFadeInHelper(Widget widget, int ms, Command callback)
   {
      widget_ = widget;
      animationMs_ = ms;
      callback_ = callback;
   }

   public void beginShow()
   {
      hide();

      final Object nonce = new Object();
      nonce_ = nonce;
      new Timer()
      {
         @Override
         public void run()
         {
            if (nonce_ == nonce)
            {
               animation_ = new FadeInAnimation(widget_, 1, callback_);
               animation_.run(animationMs_);
            }
         }
      }.schedule(750);
   }

   public void hide()
   {
      stopPending();
      widget_.setVisible(false);
      // jcheng: The next line shouldn't be necessary since we just set visible
      // to false, but there was an annoying bug where it seemed the Stop
      // button's visibility was being set to true when the Compile PDF panel is
      // introduced. For some reason opacity is not affected, so this fixes it.
      widget_.getElement().getStyle().setOpacity(0.0);
   }

   private void stopPending()
   {
      nonce_ = null;
      if (animation_ != null)
      {
         animation_.cancel();
         animation_ = null;
      }
   }

   private Object nonce_;
   private FadeInAnimation animation_;

   private final Widget widget_;
   private final int animationMs_;
   private final Command callback_;
}
