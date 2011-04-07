/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.autobean.client.impl;

import com.google.gwt.autobean.shared.Splittable;
import com.google.gwt.autobean.shared.impl.HasSplittable;
import com.google.gwt.autobean.shared.impl.StringQuoter;
import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the EntityCodex.Splittable interface using a raw JavaScriptObject.
 * <p>
 * A string value represented by a JsoSplittable can't use the string object
 * directly, since {@code String.prototype} is overridden, so instead a
 * temporary wrapper object is used to encapsulate the string data.
 */
@GwtScriptOnly
public final class JsoSplittable extends JavaScriptObject implements Splittable, HasSplittable {
  public static native JsoSplittable create() /*-{
    return {};
  }-*/;

  public static Splittable create(boolean value) {
    return create0(value);
  }

  public static Splittable create(double value) {
    return create0(value);
  }

  public static Splittable create(String value) {
    return create0(value);
  }

  public static native JsoSplittable createIndexed() /*-{
    return [];
  }-*/;

  public static native JsoSplittable nullValue() /*-{
    return null;
  }-*/;

  private static native Splittable create0(boolean object) /*-{
    return object;
  }-*/;

  private static native Splittable create0(double object) /*-{
    return object;
  }-*/;

  private static native Splittable create0(String object) /*-{
    return {
      __s : object
    };
  }-*/;

  private static native boolean stringifyFastSupported() /*-{
    return $wnd.JSON && $wnd.JSON.stringify;
  }-*/;

  protected JsoSplittable() {
  }

  public native boolean asBoolean() /*-{
    return this == true;
  }-*/;;

  public native double asNumber() /*-{
    return Number(this);
  }-*/;

  public void assign(Splittable parent, int index) {
    if (isString()) {
      assign0(parent, index, asString());
    } else {
      assign0(parent, index, this);
    }
  }

  public void assign(Splittable parent, String index) {
    if (isString()) {
      assign0(parent, index, asString());
    } else {
      assign0(parent, index, this);
    }
  }

  public native String asString() /*-{
    return this.__s;
  }-*/;

  public Splittable deepCopy() {
    return StringQuoter.split(getPayload());
  }

  public JsoSplittable get(int index) {
    return getRaw(index);
  }

  public JsoSplittable get(String key) {
    return getRaw(key);
  }

  public String getPayload() {
    if (isString()) {
      return JsonUtils.escapeValue(asString());
    }
    if (stringifyFastSupported()) {
      return stringifyFast();
    }
    return stringifySlow();
  }

  public List<String> getPropertyKeys() {
    List<String> toReturn = new ArrayList<String>();
    getPropertyKeys0(toReturn);
    return Collections.unmodifiableList(toReturn);
  }

  public native Object getReified(String key) /*-{
    return this.__reified && this.__reified[':' + key];
  }-*/;

  public Splittable getSplittable() {
    return this;
  }

  public native boolean isBoolean() /*-{
    return typeof (this) == 'boolean' || this instanceof Boolean;
  }-*/;

  public native boolean isFunction() /*-{
    return typeof this == 'function';
  }-*/;

  public native boolean isIndexed() /*-{
    return this instanceof Array;
  }-*/;

  public boolean isKeyed() {
    return this != NULL && !isString() && !isIndexed() && !isFunction();
  }

  public native boolean isNull(int index) /*-{
    return this[index] == null;
  }-*/;

  public native boolean isNull(String key) /*-{
    return this[key] == null;
  }-*/;

  public native boolean isNumber() /*-{
    return typeof (this) == 'number' || this instanceof Number;
  }-*/;

  public native boolean isReified(String key) /*-{
    return !!(this.__reified && this.__reified.hasOwnProperty(':' + key));
  }-*/;

  public native boolean isString() /*-{
    return this && this.__s != null;
  }-*/;

  public native boolean isUndefined(String key) /*-{
    return this[key] === undefined;
  }-*/;

  public native void setReified(String key, Object object) /*-{
    // Use a function object so native JSON.stringify will ignore
    (this.__reified || (this.__reified = function() {
    }))[':' + key] = object;
  }-*/;

  public native void setSize(int size) /*-{
    this.length = size;
  }-*/;

  public native int size() /*-{
    return this.length;
  }-*/;

  private native void assign0(Splittable parent, int index, Splittable value) /*-{
    parent[index] = value;
  }-*/;

  private native void assign0(Splittable parent, int index, String value) /*-{
    parent[index] = value;
  }-*/;

  private native void assign0(Splittable parent, String index, Splittable value) /*-{
    parent[index] = value;
  }-*/;

  private native void assign0(Splittable parent, String index, String value) /*-{
    parent[index] = value;
  }-*/;

  private native void getPropertyKeys0(List<String> list) /*-{
    for (key in this) {
      if (this.hasOwnProperty(key)) {
        list.@java.util.List::add(Ljava/lang/Object;)(key);
      }
    }
  }-*/;

  private native JsoSplittable getRaw(int index) /*-{
    _ = this[index];
    if (_ == null) {
      return null;
    }
    if (typeof _ == 'string' || _ instanceof String) {
      return @com.google.gwt.autobean.client.impl.JsoSplittable::create(Ljava/lang/String;)(_);
    }
    return Object(_);
  }-*/;

  private native JsoSplittable getRaw(String index) /*-{
    _ = this[index];
    if (_ == null) {
      return null;
    }
    if (typeof _ == 'string' || _ instanceof String) {
      return @com.google.gwt.autobean.client.impl.JsoSplittable::create(Ljava/lang/String;)(_);
    }
    return Object(_);
  }-*/;

  private native String stringifyFast() /*-{
    return $wnd.JSON.stringify(this);
  }-*/;

  private String stringifySlow() {
    StringBuilder sb = new StringBuilder();
    stringifySlow(sb);
    return sb.toString();
  }

  private void stringifySlow(StringBuilder sb) {
    if (this == NULL) {
      sb.append("null");
      return;
    }
    if (isBoolean()) {
      sb.append(asBoolean());
      return;
    }
    if (isNumber()) {
      sb.append(asNumber());
      return;
    }
    if (isString()) {
      sb.append(JsonUtils.escapeValue(asString()));
      return;
    }
    if (isIndexed()) {
      sb.append("[");
      for (int i = 0, j = size(); i < j; i++) {
        if (i > 0) {
          sb.append(",");
        }
        get(i).stringifySlow(sb);
      }
      sb.append("]");
      return;
    }

    sb.append("{");
    boolean needsComma = false;
    for (String key : getPropertyKeys()) {
      if (needsComma) {
        sb.append(",");
      } else {
        needsComma = true;
      }
      JsoSplittable value = get(key);
      if (!value.isFunction()) {
        sb.append(JsonUtils.escapeValue(key));
        sb.append(":");
        value.stringifySlow(sb);
      }
    }
    sb.append("}");
  }
}
