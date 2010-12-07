/*
 * DuplicateHelper.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

import java.util.*;

public class DuplicateHelper
{
   /**
    * Provides information about duplicates in the list that was tested.
    */
   public static class DuplicationInfo<T>
   {
      public DuplicationInfo(Comparator<T> comparator)
      {
         comparator_ = comparator;
      }

      /**
       * For a given value, return how many times it appears in the original
       * value list (or 0 if never).
       */
      public int occurrences(T value)
      {
         for (Pair<T, Integer> count : valueCounts_)
            if (0 == comparator_.compare(value, count.a))
               return count.b;
         return 0;
      }

      /**
       * Returns a list, each element of which is a list of indices of elements
       * in the original value list whose values are duplicates.
       *
       * For example:
       *
       * a = ["foo", "bar", "bar", "bar", "foo"]
       * dupeInfo = detectDupes(a)
       * dupeInfo.dupes()  ==> [ [0,4], [1,2,3] ]
       */
      public ArrayList<ArrayList<Integer>> dupes()
      {
         return dupes_;
      }

      void addDupeInfo(T value, ArrayList<Integer> indices)
      {
         valueCounts_.add(new Pair<T, Integer>(value, indices.size()));
         if (indices.size() > 1)
            dupes_.add(indices);
      }

      private final Comparator<T> comparator_;
      private ArrayList<ArrayList<Integer>> dupes_ =
            new ArrayList<ArrayList<Integer>>();
      private ArrayList<Pair<T, Integer>> valueCounts_ =
            new ArrayList<Pair<T, Integer>>();
   }

   private static class Pair<A,B>
   {
      private Pair(A a, B b)
      {
         this.a = a;
         this.b = b;
      }

      public final A a;
      public final B b;
   }

   /**
    * Detect duplicates and calculate frequency information in the given
    * list, according to the given comparator's definition of equality.
    * The comparator must correctly support not only equality but also
    * comparisons, since the duplicate detection algorithm relies on sorting.
    */
   public static <T> DuplicationInfo<T> detectDupes(List<T> list,
                                                    final Comparator<T> comparator)
   {
      ArrayList<Pair<Integer, T>> sorted = new ArrayList<Pair<Integer, T>>();
      for (int i = 0; i < list.size(); i++)
      {
         sorted.add(new Pair<Integer, T>(i, list.get(i)));
      }

      // Sort our copy of the list, so dupes are right next to each other
      Collections.sort(sorted, new Comparator<Pair<Integer, T>>()
      {
         public int compare(Pair<Integer, T> left,
                            Pair<Integer, T> right)
         {
            return comparator.compare(left.b, right.b);
         }
      });

      DuplicationInfo<T> dupeInfo = new DuplicationInfo<T>(comparator);
      ArrayList<Integer> currentDupes = new ArrayList<Integer>();
      T lastSeenValue = null;
      for (Pair<Integer, T> value : sorted)
      {
         if (lastSeenValue == null ||
             comparator.compare(lastSeenValue, value.b) != 0)
         {
            // This value isn't the same as the previous one. If we've got
            // dupes in our list, then add them to the results. Then start
            // a new list.
            if (currentDupes.size() > 0)
               dupeInfo.addDupeInfo(lastSeenValue, currentDupes);
            currentDupes = new ArrayList<Integer>();
         }

         // Add ourselves to the current list
         currentDupes.add(value.a);
         lastSeenValue = value.b;
      }

      if (currentDupes.size() > 0)
         dupeInfo.addDupeInfo(lastSeenValue, currentDupes);

      return dupeInfo;
   }
}
