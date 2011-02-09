package org.rstudio.core.client;

public class IntervalTracker
{
   public IntervalTracker(long intervalMillis, boolean startElapsed)
   {
      threshold_ = intervalMillis;
      if (!startElapsed)
         reset();
   }

   public void reset()
   {
      lastTime_ = System.currentTimeMillis();
   }

   public boolean hasElapsed()
   {
      return lastTime_ != null
             && System.currentTimeMillis() - lastTime_ < threshold_;
   }

   private Long lastTime_;
   private final long threshold_;
}
