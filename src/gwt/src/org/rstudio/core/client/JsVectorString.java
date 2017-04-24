/*
 * JsVectorString.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import com.google.gwt.core.client.JavaScriptObject;

public class JsVectorString extends JavaScriptObject
{
   protected JsVectorString()
   {
   }
   
   public static final native JsVectorString createVector()
   /*-{
      return [];
   }-*/;
   
   public static final native JsVectorString concat(JsVectorString lhs, JsVectorString rhs)
   /*-{
      return lhs.concat(rhs);
   }-*/;
   
   public static final native JsVectorString ofLength(int n)
   /*-{
      var result = [];
      result.length = n;
      return result;
   }-*/;
   
   public final native JsVectorString concat(JsVectorString other)
   /*-{
      return [].concat.call(this, other);
   }-*/;
   
   public final boolean contains(String value)
   {
      return indexOf(value) != -1;
   }
   
   public final native void fill(String value, int start, int end)
   /*-{
      var i = start;
      while (i < end) {
         this[i] = value;
         i++;
      }
   }-*/;
   
   public final void fill(String value)
   {
      fill(value, 0, length());
   }
   
   public final String get(int index)
   {
      return get(index, defaultValue());
   }
   
   public final native String get(int index, String defaultValue)
   /*-{
      return this[index] || defaultValue;
   }-*/;
   
   public final native int indexOf(String value)
   /*-{
      for (var i = 0, n = this.length; i < n; i++)
         if (this[i] === value)
           return i;
      return -1;
   }-*/;
   
   public final native boolean isEmpty()
   /*-{
      return this.length == 0;
   }-*/;
   
   public final native boolean isSet(int index)
   /*-{
      return typeof this[index] !== "undefined";
   }-*/;
   
   public final native void insert(int index, JsVectorString values)
   /*-{
      [].splice.apply(this, [index, 0].concat(values));
   }-*/;
   
   public final native void insert(int index, String value)
   /*-{
      this.splice(index, 0, value);
   }-*/;
   
   public final native String join(String delimiter)
   /*-{
      return this.join(delimiter);
   }-*/;
   
   public final String join()
   {
      return join(",");
   }
   
   public final native int length()
   /*-{
      return this.length || 0;
   }-*/;
   
   public final String peek()
   {
      return peek(defaultValue());
   }
   
   private native final String peek(String defaultValue)
   /*-{
      return this[this.length - 1] || defaultValue;
   }-*/;
   
   public final String pop()
   {
      return pop(defaultValue());
   }
   
   private final native String pop(String defaultValue)
   /*-{
      return this.pop() || defaultValue;
   }-*/;
   
   public final native void push(String object)
   /*-{
      this.push(object);
   }-*/;
   
   public final native void push(JsVectorString object)
   /*-{
      [].push.apply(this, object);
   }-*/;
   
   public final native void remove(int index, int count)
   /*-{
      return this.splice(index, count);
   }-*/;
   
   public final void remove(int index)
   {
      remove(index, 1);
   }
   
   public final native void reverse()
   /*-{
      this.reverse();
   }-*/;
   
   public final String shift()
   {
      return shift(defaultValue());
   }
   
   public final int size()
   {
      return length();
   }
   
   private final native String shift(String defaultValue)
   /*-{
      return this.shift() || defaultValue;
   }-*/;
   
   public final native void splice(int start, int deleteCount, JsVectorString vector)
   /*-{
      this.splice(start, deleteCount, vector);
   }-*/;
   
   public final native void set(int index, String value)
   /*-{
      this[index] = value;
   }-*/;
   
   public final native void unset(int index)
   /*-{
      this[index] = undefined;
   }-*/;
   
   public final native int unshift(String object)
   /*-{
      return this.unshift(object);
   }-*/;
   
   public final native int unshift(JsVectorString vector)
   /*-{
      return [].unshift.apply(this, vector);
   }-*/;
   
   private final native String defaultValue()
   /*-{
      return null;
   }-*/;
}
