/*
 * DiffUtils.java
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
package org.rstudio.core.client.diff;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.diff.JsDiff.Delta;

import jsinterop.base.JsArrayLike;

public class DiffUtils
{
   /**
    * Computes the deltas between two strings, for display of edit suggestion
    * previews. Concatenating the non-added delta values reproduces 'original';
    * concatenating the non-removed delta values reproduces 'replacement'.
    *
    * @param original The original text.
    * @param replacement The replacement text.
    * @param wordDiff Whether to compute a word-level diff rather than a
    *   character-level diff.
    */
   public static List<Delta> computeDeltas(String original,
                                           String replacement,
                                           boolean wordDiff)
   {
      if (!wordDiff)
         return toList(JsDiff.diffChars(original, replacement));

      JsArrayLike<Delta> deltas = JsDiff.diffWordsWithSpace(original, replacement);

      // Refine replaced token pairs where one side wholly contains the other
      // (pure extensions, truncations, and whitespace-run changes). A
      // character-level diff of such a pair yields a single clean insertion
      // or deletion, so e.g. 'foo' -> 'foobar' still renders as an insertion
      // of 'bar' rather than a replacement of the whole word.
      List<Delta> result = new ArrayList<>();
      for (int i = 0, n = deltas.getLength(); i < n; i++)
      {
         Delta delta = deltas.getAt(i);

         if (delta.removed && i + 1 < n)
         {
            Delta next = deltas.getAt(i + 1);
            if (next.added && isContained(delta.value, next.value))
            {
               result.addAll(toList(JsDiff.diffChars(delta.value, next.value)));
               i++;
               continue;
            }
         }

         result.add(delta);
      }

      return result;
   }

   private static boolean isContained(String lhs, String rhs)
   {
      return lhs.startsWith(rhs) || lhs.endsWith(rhs) ||
             rhs.startsWith(lhs) || rhs.endsWith(lhs);
   }

   private static List<Delta> toList(JsArrayLike<Delta> deltas)
   {
      List<Delta> result = new ArrayList<>();
      for (int i = 0, n = deltas.getLength(); i < n; i++)
         result.add(deltas.getAt(i));
      return result;
   }
}
