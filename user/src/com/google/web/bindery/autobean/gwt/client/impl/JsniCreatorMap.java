/*
 * Copyright 2011 Google Inc.
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

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

/**
 * Used in prod-mode code to create instances of generated AutoBean subtypes via
 * JSNI references to their constructor methods.
 */
public final class JsniCreatorMap extends JavaScriptObject {
  public static JsniCreatorMap createMap() {
    return JavaScriptObject.createObject().cast();
  }

  /*
   * Structure is a string map of class literal names to the no-arg and one-arg
   * constructors of a generated AutoBean subtype.
   */
  protected JsniCreatorMap() {
  }

  public void add(Class<?> clazz, JsArray<JavaScriptObject> constructors) {
    assert constructors.length() == 2 : "Expecting two constructor references";
    set(clazz.getName(), constructors);
  }

  public <T> AutoBean<T> create(Class<T> clazz, AbstractAutoBeanFactory factory) {
    JsArray<JavaScriptObject> arr = get(clazz.getName());
    if (arr != null && arr.get(0) != null) {
      return invoke(arr.get(0), factory, null);
    }
    return null;
  }

  public <T> AutoBean<T> create(Class<T> clazz, AbstractAutoBeanFactory factory, Object delegate) {
    JsArray<JavaScriptObject> arr = get(clazz.getName());
    if (arr != null) {
      assert arr.get(1) != null : "No delegate-based constructor";
      return invoke(arr.get(1), factory, delegate);
    }
    return null;
  }

  private native JsArray<JavaScriptObject> get(String key) /*-{
    return this[key];
  }-*/;

  private native <T> AutoBean<T> invoke(JavaScriptObject fn, Object arg1, Object arg2)/*-{
    return fn(arg1, arg2);
  }-*/;

  private native void set(String key, JsArray<JavaScriptObject> arr) /*-{
    this[key] = arr;
  }-*/;
}
