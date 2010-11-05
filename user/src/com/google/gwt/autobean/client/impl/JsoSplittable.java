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
import com.google.gwt.autobean.shared.impl.StringQuoter;
import com.google.gwt.core.client.JavaScriptObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the EntityCodex.Splittable interface
 */
public final class JsoSplittable extends JavaScriptObject implements Splittable {
  /**
   * This type is only used in DevMode because we can't treat Strings as JSOs.
   */
  public static class StringSplittable implements Splittable {
    private final String value;

    public StringSplittable(String value) {
      this.value = value;
    }

    public String asString() {
      return value;
    }

    public Splittable get(int index) {
      throw new UnsupportedOperationException();
    }

    public Splittable get(String key) {
      throw new UnsupportedOperationException();
    }

    public String getPayload() {
      return StringQuoter.quote(value);
    }

    public List<String> getPropertyKeys() {
      return Collections.emptyList();
    }

    public boolean isNull(int index) {
      throw new UnsupportedOperationException();
    }

    public boolean isNull(String key) {
      throw new UnsupportedOperationException();
    }

    public int size() {
      return 0;
    }
  }

  public static Splittable create(Object object) {
    if (object instanceof String) {
      return new StringSplittable((String) object);
    }
    return create0(object);
  }

  private static native Splittable create0(Object object) /*-{
    return object;
  }-*/;

  protected JsoSplittable() {
  }

  public native String asString() /*-{
    return String(this);
  }-*/;

  public Splittable get(int index) {
    return create(get0(index));
  }

  public Splittable get(String key) {
    return create(get0(key));
  }

  public String getPayload() {
    throw new UnsupportedOperationException(
        "Cannot convert JsoSplittable to payload");
  }

  public List<String> getPropertyKeys() {
    List<String> toReturn = new ArrayList<String>();
    getPropertyKeys0(toReturn);
    return Collections.unmodifiableList(toReturn);
  }

  public native boolean isNull(int index) /*-{
    return this[index] == null;
  }-*/;

  public native boolean isNull(String key) /*-{
    return this[key] == null;
  }-*/;

  public native int size() /*-{
    return this.length;
  }-*/;

  private native Object get0(int index) /*-{
    return Object(this[index]);
  }-*/;

  private native Object get0(String key) /*-{
    return Object(this[key]);
  }-*/;

  private native void getPropertyKeys0(List<String> list) /*-{
    for (key in this) {
      if (this.hasOwnProperty(key)) {
        list.@java.util.List::add(Ljava/lang/Object;)(key);
      }
    }
  }-*/;
}
