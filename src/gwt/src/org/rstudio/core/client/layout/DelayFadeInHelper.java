/*
 * DelayFadeInHelper.java
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
package org.rstudio.core.client.layout;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

public class DelayFadeInHelper
{
   public DelayFadeInHelper(Widget widget)
   {
      widget_ = widget;
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
               animation_ = new FadeInAnimation(
                     widget_, 1, null);
               animation_.run(250);
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
}
