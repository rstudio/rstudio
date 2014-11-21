package org.rstudio.studio.client.workbench.views.source.editors.text.r;

import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionToolTip;

import com.google.gwt.user.client.Window;

public class RCompletionToolTip extends CppCompletionToolTip
{
   public RCompletionToolTip()
   {
   }

   public void resolvePositionRelativeTo(final int left, final int top)
   {
      // some constants
      final int H_PAD = 12;
      final int V_PAD = -3;
      final int H_BUFFER = 100;
      final int MIN_WIDTH = 300;

      // do we have enough room to the right? if not then
      int roomRight = Window.getClientWidth() - left;
      int maxWidth = Math.min(roomRight - H_BUFFER, 500);
      final boolean showLeft = maxWidth < MIN_WIDTH;
      if (showLeft)
         maxWidth = left - H_BUFFER;

      setMaxWidth(maxWidth);
      setPopupPositionAndShow(new PositionCallback(){

         @Override
         public void setPosition(int offsetWidth,
                                 int offsetHeight)
         {
            // if we are showing left then adjust
            int adjustedLeft = left;
            if (showLeft)
            {
               adjustedLeft = getAbsoluteLeft() -
                     offsetWidth - H_PAD;
            }

            setPopupPosition(adjustedLeft, top - getOffsetHeight());
         }
      });

   }

}
