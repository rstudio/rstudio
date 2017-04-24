/*
 * JsVector.java
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

public class JsVector<T> extends JavaScriptObject
{
   protected JsVector()
   {
   }
   
   public static final native <T> JsVector<T> createVector()
   /*-{
      return [];
   }-*/;
   
   public static final native <T> JsVector<T> concat(JsVector<T> lhs, JsVector<T> rhs)
   /*-{
      return lhs.concat(rhs);
   }-*/;
   
   public static final native <T> JsVector<T> ofLength(int n)
   /*-{
      var result = [];
      result.length = n;
      return result;
   }-*/;
   
   public final native JsVector<T> concat(JsVector<T> other)
   /*-{
      return [].concat.call(this, other);
   }-*/;
   
   public final boolean contains(T value)
   {
      return indexOf(value) != -1;
   }
   
   public final native void fill(T value, int start, int end)
   /*-{
      var i = start;
      while (i < end) {
         this[i] = value;
         i++;
      }
   }-*/;
   
   public final void fill(T value)
   {
      fill(value, 0, length());
   }
   
   public final T get(int index)
   {
      return get(index, defaultValue());
   }
   
   public final native T get(int index, T defaultValue)
   /*-{
      return this[index] || defaultValue;
   }-*/;
   
   public final native int indexOf(T value)
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
   
   public final native void insert(int index, JsVector<T> values)
   /*-{
      [].splice.apply(this, [index, 0].concat(values));
   }-*/;
   
   public final native void insert(int index, T value)
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
   
   public final T peek()
   {
      return peek(defaultValue());
   }
   
   private native final T peek(T defaultValue)
   /*-{
      return this[this.length - 1] || defaultValue;
   }-*/;
   
   public final T pop()
   {
      return pop(defaultValue());
   }
   
   private final native T pop(T defaultValue)
   /*-{
      return this.pop() || defaultValue;
   }-*/;
   
   public final native void push(T object)
   /*-{
      this.push(object);
   }-*/;
   
   public final native void push(JsVector<T> object)
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
   
   public final T shift()
   {
      return shift(defaultValue());
   }
   
   public final int size()
   {
      return length();
   }
   
   private final native T shift(T defaultValue)
   /*-{
      return this.shift() || defaultValue;
   }-*/;
   
   public final native void splice(int start, int deleteCount, JsVector<T> vector)
   /*-{
      this.splice(start, deleteCount, vector);
   }-*/;
   
   public final native void set(int index, T value)
   /*-{
      this[index] = value;
   }-*/;
   
   public final native void unset(int index)
   /*-{
      this[index] = undefined;
   }-*/;
   
   public final native int unshift(T object)
   /*-{
      return this.unshift(object);
   }-*/;
   
   public final native int unshift(JsVector<T> vector)
   /*-{
      return [].unshift.apply(this, vector);
   }-*/;
   
   private final native T defaultValue()
   /*-{
      return null;
   }-*/;
}
