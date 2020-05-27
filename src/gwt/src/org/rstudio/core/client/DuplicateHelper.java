/*
 * DuplicateHelper.java
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
package org.rstudio.core.client;

import org.rstudio.core.client.files.FileSystemItem;
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
            if (0 == comparator_.compare(value, count.first))
               return count.second;
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

   private static class CaseInsensitiveStringComparator implements Comparator<String>
   {
      public int compare(String s1, String s2)
      {
         return s1.compareToIgnoreCase(s2);
      }
   }

   public static <T> int dedupeSortedList(ArrayList<T> list)
   {
      int removedCount = 0;

      for (int i = list.size() - 1; i > 0; i--)
      {
         T x = list.get(i-1);
         T y = list.get(i);
         if (((x == null) == (y == null)) &&
             ((x == null) || x.equals(y)))
         {
            list.remove(i);
            removedCount++;
         }
      }

      return removedCount;
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
            return comparator.compare(left.second, right.second);
         }
      });

      DuplicationInfo<T> dupeInfo = new DuplicationInfo<T>(comparator);
      ArrayList<Integer> currentDupes = new ArrayList<Integer>();
      T lastSeenValue = null;
      for (Pair<Integer, T> value : sorted)
      {
         if (lastSeenValue == null ||
             comparator.compare(lastSeenValue, value.second) != 0)
         {
            // This value isn't the same as the previous one. If we've got
            // dupes in our list, then add them to the results. Then start
            // a new list.
            if (currentDupes.size() > 0)
               dupeInfo.addDupeInfo(lastSeenValue, currentDupes);
            currentDupes = new ArrayList<Integer>();
         }

         // Add ourselves to the current list
         currentDupes.add(value.first);
         lastSeenValue = value.second;
      }

      if (currentDupes.size() > 0)
         dupeInfo.addDupeInfo(lastSeenValue, currentDupes);

      return dupeInfo;
   }

   /**
    * Use Mac OS X style prettifying of paths. Display the filename,
    * and if there are multiple entries with the same filename, append
    * a disambiguating folder to those filenames.
    */
   public static ArrayList<String> getPathLabels(ArrayList<String> paths,
                                                 boolean includeExtension)
   {
      ArrayList<String> labels = new ArrayList<String>();
      for (String entry : paths)
      {
         if (includeExtension)
            labels.add(FileSystemItem.getNameFromPath(entry));
         else
            labels.add(FileSystemItem.createFile(entry).getStem());
      }

      DuplicationInfo<String> dupeInfo = DuplicateHelper.detectDupes(
            labels, new CaseInsensitiveStringComparator());

      for (ArrayList<Integer> dupeList : dupeInfo.dupes())
      {
         fixupDupes(paths, dupeList, labels);
      }

      dupeInfo = DuplicateHelper.detectDupes(
            labels, new CaseInsensitiveStringComparator());

      // There are edge cases where we may still end up with dupes at this
      // point. In that case, just disambiguate using the full path.
      // Example:
      // ~/foo/tmp/README
      // ~/bar/tmp/README
      // ~/foo/README
      // ~/bar/README
      for (ArrayList<Integer> dupeList : dupeInfo.dupes())
      {
         for (Integer index : dupeList)
         {
            FileSystemItem fsi = FileSystemItem.createFile(
                  paths.get(index));
            String name = includeExtension ? fsi.getName() : fsi.getStem();
            labels.set(index, disambiguate(name,
                                           fsi.getParentPathString()));
         }
      }


      return labels;
   }

   private static void fixupDupes(ArrayList<String> fullPaths,
                           ArrayList<Integer> indices,
                           ArrayList<String> labels)
   {
      ArrayList<ArrayList<String>> pathElementListList =
            new ArrayList<ArrayList<String>>();

      for (Integer index : indices)
         pathElementListList.add(toPathElements(fullPaths.get(index)));

      while (indices.size() > 0)
      {
         ArrayList<String> lastPathElements = new ArrayList<String>();

         for (int i = 0; i < pathElementListList.size(); i++)
         {
            ArrayList<String> pathElementList = pathElementListList.get(i);

            if (pathElementList.isEmpty())
            {
               int trueIndex = indices.get(i);
               String path = FileSystemItem.createFile(fullPaths.get(trueIndex))
                     .getParentPathString();
               labels.set(trueIndex,
                          disambiguate(labels.get(trueIndex), path));

               indices.remove(i);
               pathElementListList.remove(i);
               i--;
            }
            else
            {
               lastPathElements.add(
                     pathElementList.remove(pathElementList.size() - 1));
            }
         }


         DuplicationInfo<String> dupeInfo = DuplicateHelper.detectDupes(
               lastPathElements,
               new CaseInsensitiveStringComparator());

         for (int i = 0; i < lastPathElements.size(); i++)
         {
            if (1 == dupeInfo.occurrences(lastPathElements.get(i)))
            {
               int trueIndex = indices.get(i);
               labels.set(trueIndex, disambiguate(labels.get(trueIndex),
                                          lastPathElements.get(i)));

               indices.remove(i);
               pathElementListList.remove(i);
               lastPathElements.remove(i);
               i--;
            }
         }

         assert indices.size() == pathElementListList.size();
      }
   }

   private static String disambiguate(String filename, String disambiguatingPath)
   {
      return filename + " \u2014 " + disambiguatingPath;
   }

   private static ArrayList<String> toPathElements(String path)
   {
      FileSystemItem fsi = FileSystemItem.createFile(path);
      return new ArrayList<String>(
            Arrays.asList(fsi.getParentPathString().split("/")));
   }
}
