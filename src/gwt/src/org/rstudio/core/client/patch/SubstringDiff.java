/*
 * SubstringDiff.java
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
package org.rstudio.core.client.patch;

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;

public class SubstringDiff
{
   public SubstringDiff(String origVal, String newVal)
   {
      origVal_ = origVal;
      newVal_ = newVal;
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
   
   public TextChange[] asTextChanges() 
   {
      ArrayList<TextChange> changes = new ArrayList<TextChange>();
      if (valid_)
      {
         if (offset_ > 0)
            changes.add(new TextChange(TextChange.Type.Equal, origVal_.substring(0, offset_)));
         
         if (length_ > 0)
            changes.add(new TextChange(TextChange.Type.Delete, origVal_.substring(offset_, offset_ + length_)));
         
         if (replacement_.length() > 0)
            changes.add(new TextChange(TextChange.Type.Insert, replacement_));
         
         if (offset_ + length_ < origVal_.length())
            changes.add(new TextChange(TextChange.Type.Equal, origVal_.substring(offset_ + length_)));
      }
      else
      {
         if (origVal_.length() > 0)
            changes.add(new TextChange(TextChange.Type.Delete, origVal_));
         if (newVal_.length() > 0)
            changes.add(new TextChange(TextChange.Type.Insert, newVal_));
      }
      return changes.toArray(new TextChange[] {});      
   }

   
   private static final native JsObject diffImpl(String origVal, String newVal)
   /*-{
   
      // Convert to UTF-8 byte array.
      var o = new $wnd.TextEncoder("utf-8").encode(origVal);
      var n = new $wnd.TextEncoder("utf-8").encode(newVal);
      
      var olen = o.length;
      var nlen = n.length;
      
      // Figure out how many characters at the beginning of the two strings
      // are identical.
      var headLimit = Math.min(olen, nlen);
      var head;
      for (head = 0;
           head < headLimit && o[head] === n[head];
           head++)
      {
      }

      // Figure out how many characters at the end of the two strings are
      // identical, but don't go past the range we established in the above
      // step (i.e., anything already in the head can't be part of the tail).
      var tailDelta = nlen - olen;
      var tailLimit = Math.max(head, head - tailDelta);
      
      var tail;
      for (tail = olen;
           tail > tailLimit && o[tail - 1] === n[tail + tailDelta - 1];
           tail--)
      {
      }
      
      // Early check for case with no diff.
      if (olen === nlen && head === tail) {
         return {
            "replacement": "",
            "offset": 0,
            "length": 0
         }
      }
      
      // Move head and tail to ensure we align on starts of UTF-8 characters.
      // UTF-8 continuation bytes match the byte sequence 10xxxxxx;
      // that is, are values in the range [128, 192). So we want to ensure
      // head + tail land on bytes not containing those values.
      while (head > 0)
      {
         var ch = o[head] || 0;
         if (ch < 128 || ch >= 192)
            break;
         head--;
      }
      
      while (tail < olen)
      {
         var ch = o[tail] || 0;
         if (ch < 128 || ch >= 192)
            break;
         tail++;
      }
      
      // Extract the modified slice of data, and decode it back to a string.
      // NOTE: Internet Explorer does not support slice on Uint8Array objects.
      var slice = n.subarray(head, tail + tailDelta);
      var replacement = new $wnd.TextDecoder().decode(slice);
      
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

   private final String origVal_;
   private final String newVal_;

   private int offset_;
   private int length_;
   private String replacement_;
   private boolean valid_;

}
