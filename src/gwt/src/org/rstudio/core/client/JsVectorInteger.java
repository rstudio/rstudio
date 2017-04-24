/*
 * JsVectorInteger.java
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

public class JsVectorInteger extends JavaScriptObject
{
   protected JsVectorInteger()
   {
   }
   
   public static final native JsVectorInteger createVector()
   /*-{
      return [];
   }-*/;
   
   public static final native JsVectorInteger concat(JsVectorInteger lhs, JsVectorInteger rhs)
   /*-{
      return lhs.concat(rhs);
   }-*/;
   
   public static final native JsVectorInteger ofLength(int n)
   /*-{
      var result = [];
      result.length = n;
      return result;
   }-*/;
   
   public final native JsVectorInteger concat(JsVectorInteger other)
   /*-{
      return [].concat.call(this, other);
   }-*/;
   
   public final boolean contains(int value)
   {
      return indexOf(value) != -1;
   }
   
   public final native void fill(int value, int start, int end)
   /*-{
      var i = start;
      while (i < end) {
         this[i] = value;
         i++;
      }
   }-*/;
   
   public final void fill(int value)
   {
      fill(value, 0, length());
   }
   
   public final int get(int index)
   {
      return get(index, defaultValue());
   }
   
   public final native int get(int index, int defaultValue)
   /*-{
      return this[index] || defaultValue;
   }-*/;
   
   public final native int indexOf(int value)
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
   
   public final native void insert(int index, JsVectorInteger values)
   /*-{
      [].splice.apply(this, [index, 0].concat(values));
   }-*/;
   
   public final native void insert(int index, int value)
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
   
   public final int peek()
   {
      return peek(defaultValue());
   }
   
   private native final int peek(int defaultValue)
   /*-{
      return this[this.length - 1] || defaultValue;
   }-*/;
   
   public final int pop()
   {
      return pop(defaultValue());
   }
   
   private final native int pop(int defaultValue)
   /*-{
      return this.pop() || defaultValue;
   }-*/;
   
   public final native void push(int object)
   /*-{
      this.push(object);
   }-*/;
   
   public final native void push(JsVectorInteger object)
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
   
   public final int shift()
   {
      return shift(defaultValue());
   }
   
   public final int size()
   {
      return length();
   }
   
   private final native int shift(int defaultValue)
   /*-{
      return this.shift() || defaultValue;
   }-*/;
   
   public final native void splice(int start, int deleteCount, JsVectorInteger vector)
   /*-{
      this.splice(start, deleteCount, vector);
   }-*/;
   
   public final native void set(int index, int value)
   /*-{
      this[index] = value;
   }-*/;
   
   public final native void unset(int index)
   /*-{
      this[index] = undefined;
   }-*/;
   
   public final native int unshift(int object)
   /*-{
      return this.unshift(object);
   }-*/;
   
   public final native int unshift(JsVectorInteger vector)
   /*-{
      return [].unshift.apply(this, vector);
   }-*/;
   
   private final native int defaultValue()
   /*-{
      return 0;
   }-*/;
}
