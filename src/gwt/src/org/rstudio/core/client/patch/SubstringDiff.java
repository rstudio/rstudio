/*
 * SubstringDiff.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.patch;

public class SubstringDiff
{
   public SubstringDiff(String origVal, String newVal)
   {
      // Figure out how many characters at the beginning of the two strings
      // are identical.
      int headLimit = Math.min(origVal.length(), newVal.length());
      int head;
      for (head = 0;
            head < headLimit && origVal.charAt(head) == newVal.charAt(head);
            head++)
      {}

      // Figure out how many characters at the end of the two strings are
      // identical, but don't go past the range we established in the above
      // step (i.e., anything already in the head can't be part of the tail).
      int tailDelta = newVal.length() - origVal.length();
      int tailLimit = Math.max(head, head - tailDelta);
      int tail;
      for (tail = origVal.length();
            tail > tailLimit && origVal.charAt(tail-1) == newVal.charAt(tail+tailDelta-1);
            tail--)
      {}

      // Now we have a chunk of newVal that is unique (it may simply be "")
      // and offset_/length_ show what region within oldDoc it replaces. 
      replacement_ = newVal.substring(head, tail + tailDelta);
      offset_ = head;
      length_ = tail - head;
   }

   public String getReplacement()
   {
      return replacement_;
   }

   public int getOffset()
   {
      return offset_;
   }

   public int getLength()
   {
      return length_;
   }

   public String patch(String original)
   {
      if (isEmpty())
         return original;
      
      return original.substring(0, offset_)
            + replacement_
            + original.substring(offset_ + length_);
   }

   /**
    * @return True iff there was no difference between the strings.
    */
   public boolean isEmpty()
   {
      return length_ == 0 && replacement_.length() == 0;
   }

   private final int offset_;
   private final int length_;
   private final String replacement_;



   /*
   public static void test()
   {
      test("", "", 0, 0, "");
      test("a", "a", 1, 0, "");
      test("ab", "ab", 2, 0, "");
      test("ab", "a", 1, 1, "");
      test("abc", "ac", 1, 1, "");
      test("abc", "adc", 1, 1, "d");
      test("abc", "bc", 0, 1, "");
      test("bc", "abc", 0, 0, "a");
      test("a\nb\nc", "a\nc", 2, 2, "");
   }
   static void test(String old, String neu, int offset, int len, String repl)
   {
      SubstringDiff diff = new SubstringDiff(old, neu);
      assert diff.getOffset() == offset
            && diff.getLength() == len
            && diff.getReplacement().equals(repl) :
            "\"" + old + "\" - \"" + neu + "\" => " +
            "\"" + diff.getReplacement() + "\" [" + diff.getOffset() + ", " + diff.getLength() + "]";
      assert diff.patch(old).equals(neu);
   }
   */
}
