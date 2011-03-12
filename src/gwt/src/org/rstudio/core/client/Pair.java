package org.rstudio.core.client;

public class Pair<TFirst, TSecond>
{
   public Pair(TFirst first, TSecond second)
   {
      this.first = first;
      this.second = second;
   }

   public TFirst first;
   public TSecond second;
}
