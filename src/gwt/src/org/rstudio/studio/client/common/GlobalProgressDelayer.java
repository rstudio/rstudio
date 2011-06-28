package org.rstudio.studio.client.common;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

public class GlobalProgressDelayer
{
   public GlobalProgressDelayer(final GlobalDisplay globalDisplay,
                                final String progressMessage)
   {
      final int DELAY_MILLIS = 250;

      timer_ = new Timer()
      {
         @Override
         public void run()
         {
            dismiss_ = globalDisplay.showProgress(progressMessage);
         }
      };
      timer_.schedule(DELAY_MILLIS);
   }

   public void dismiss()
   {
      timer_.cancel();
      if (dismiss_ != null)
         dismiss_.execute();
   }

   private final Timer timer_;
   private Command dismiss_;
}
