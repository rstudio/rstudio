/*
 * SubstringDiff.java
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
package org.rstudio.core.client.patch;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;

public class SubstringDiff
{
   public SubstringDiff(String origVal, String newVal)
   {
      try
      {
         JsObject diff = diffImpl(origVal, newVal);
         replacement_  = diff.getString("replacement");
         offset_       = diff.getInteger("offset");
         length_       = diff.getInteger("length");
         valid_        = true;
      }
      catch (Exception e)
      {
         Debug.logException(e);
         replacement_ = "";
         offset_ = 0;
         length_ = 0;
         valid_ = false;
      }
   }
   
   private static final native JsObject diffImpl(String origVal, String newVal)
   /*-{
   
      // Convert to UTF-8 byte array.
      var o = new TextEncoder("utf-8").encode(origVal);
      var n = new TextEncoder("utf-8").encode(newVal);
      
      // Figure out how many characters at the beginning of the two strings
      // are identical.
      var headLimit = Math.min(o.length, n.length);
      var head;
      for (head = 0;
           head < headLimit && o[head] === n[head];
           head++)
      {
      }

      // Figure out how many characters at the end of the two strings are
      // identical, but don't go past the range we established in the above
      // step (i.e., anything already in the head can't be part of the tail).
      var tailDelta = n.length - o.length;
      var tailLimit = Math.max(head, head - tailDelta);
      
      var tail;
      for (tail = o.length;
           tail > tailLimit && o[tail - 1] === n[tail + tailDelta - 1];
           tail--)
      {
      }
      
      // Extract the modified slice of data, and decode it back to a string.
      var slice = n.slice(head, tail + tailDelta);
      var replacement = new TextDecoder().decode(slice);
      
      return {
         "replacement": replacement,
         "offset": head,
         "length": tail - head
      };
      
   
   }-*/;

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
   
   public boolean isEmpty()
   {
      return length_ == 0 && replacement_.length() == 0;
   }
   
   public boolean isValid()
   {
      return valid_;
   }

   private int offset_;
   private int length_;
   private String replacement_;
   private boolean valid_;

}
