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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;

/**
 * JSO implementation of {@link Values}.
 * @param <T> value type
 */
public final class ValuesImpl<T> extends JavaScriptObject implements Values<T> {

  public static native <T> JsArray<ValuesImpl<T>> arrayFromJson(String json) /*-{
    return eval(json);
  }-*/;

  public static native <T> ValuesImpl<T> fromJson(String json) /*-{
    return eval(json);
  }-*/;

  protected ValuesImpl() {
  }

  public native <V, P extends Property<T, V>> V get(P property) /*-{
    return this[property.@com.google.gwt.valuestore.shared.Property::getName()()];
  }-*/;

  public native T getPropertyHolder() /*-{
    return this.propertyHolder;
  }-*/;
  
  public native void setPropertyHolder(T propertyHolder) /*-{
    this.propertyHolder = propertyHolder;
  }-*/;
}
