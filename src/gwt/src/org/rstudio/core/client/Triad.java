package org.rstudio.core.client;

public class Triad<TFirst, TSecond, TThird>
{
   public Triad(TFirst first, TSecond second, TThird third)
   {
      this.first = first;
      this.second = second;
      this.third = third;
   }

   public TFirst first;
   public TSecond second;
   public TThird third;
}
