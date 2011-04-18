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
package com.google.web.bindery.autobean.gwt.client.impl;

import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.impl.HasSplittable;
import com.google.web.bindery.autobean.shared.impl.StringQuoter;
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
  private static boolean stringifyFastTested;
  private static boolean stringifyFastResult;

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
    return Boolean(object);
  }-*/;

  private static native Splittable create0(double object) /*-{
    return Number(object);
  }-*/;

  private static native Splittable create0(String object) /*-{
    return {
      __s : object
    };
  }-*/;

  private static native boolean isUnwrappedString(JavaScriptObject obj) /*-{
    return Object.prototype.toString.call(obj) == '[object String]';
  }-*/;

  private static boolean stringifyFastSupported() {
    if (stringifyFastTested) {
      return stringifyFastResult;
    }
    stringifyFastTested = true;
    return stringifyFastResult = stringifyFastSupported0();
  }

  /**
   * Test that the JSON api is available and that it does not add function
   * objects to the output. The test for function objects is for old versions of
   * Safari.
   */
  private static native boolean stringifyFastSupported0() /*-{
    return $wnd.JSON && $wnd.JSON.stringify && $wnd.JSON.stringify({
      b : function() {
      }
    }) == '{}';
  }-*/;

  protected JsoSplittable() {
  };

  public native boolean asBoolean() /*-{
    return this == true;
  }-*/;

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
    return Object.prototype.toString.call(this) == '[object Boolean]';
  }-*/;

  public native boolean isFunction() /*-{
    return Object.prototype.toString.call(this) == '[object Function]';
  }-*/;

  public native boolean isIndexed() /*-{
    return Object.prototype.toString.call(this) == '[object Array]';
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
    return Object.prototype.toString.call(this) == '[object Number]';
  }-*/;

  public native boolean isReified(String key) /*-{
    return !!(this.__reified && this.__reified.hasOwnProperty(':' + key));
  }-*/;

  /**
   * Returns whether or not the current object is a string-carrier.
   */
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
    if (@com.google.web.bindery.autobean.gwt.client.impl.JsoSplittable::isUnwrappedString(*)(_)) {
      return @com.google.web.bindery.autobean.gwt.client.impl.JsoSplittable::create(Ljava/lang/String;)(_);
    }
    return Object(_);
  }-*/;

  private native JsoSplittable getRaw(String index) /*-{
    _ = this[index];
    if (_ == null) {
      return null;
    }
    if (@com.google.web.bindery.autobean.gwt.client.impl.JsoSplittable::isUnwrappedString(*)(_)) {
      return @com.google.web.bindery.autobean.gwt.client.impl.JsoSplittable::create(Ljava/lang/String;)(_);
    }
    return Object(_);
  }-*/;

  /**
   * The test for {@code $H} removes the key in the emitted JSON, however making
   * a similar test for {@code __reified} causes the key to be emitted with an
   * explicit {@code null} value.
   */
  private native String stringifyFast() /*-{
    return $wnd.JSON.stringify(this, function(key, value) {
      if (key == "$H") {
        return;
      }
      return value;
    });
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
        if ("$H".equals(key)) {
          // Ignore hashcode
          continue;
        }
        sb.append(JsonUtils.escapeValue(key));
        sb.append(":");
        value.stringifySlow(sb);
      }
    }
    sb.append("}");
  }
}
