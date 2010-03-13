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
package com.google.gwt.valuestore.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;

import java.util.Date;

/**
 * JSO implementation of {@link Values}.
 * 
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

  @SuppressWarnings("unchecked")
  public <V, P extends Property<T, V>> V get(P property) {

    if (Integer.class.equals(property.getValueType())) {
      return (V) Integer.valueOf(getInt(property.getName()));
    }
    if (Date.class.equals(property.getValueType())) {
      double millis = getDouble(property.getName());
      // TODO (rjrjr) bring this back when Date gets JSO friendly again
//      if (GWT.isScript()) {
//        return (V) initDate(new Date(), millis);
//      } else {
        // In dev mode, we're using real JRE dates
        return (V) new Date((long) millis);
//      }
    }

    return nativeGet(property);
  }

  public native T getPropertyHolder() /*-{
    return this.propertyHolder;
  }-*/;

  public native void setPropertyHolder(T propertyHolder) /*-{
    this.propertyHolder = propertyHolder;
  }-*/;

  public native void setString(Property<T, String> property, String value) /*-{
    this[property.@com.google.gwt.valuestore.shared.Property::getName()()] = value;
  }-*/;

  /**
   * @return
   */
  public native String toJson() /*-{
    var output = "";
    for (property in this) { 
      if (property != 'propertyHolder') {
        output += '"' + property + '": ' + '"' + this[property] + '"' + '; ';
      }
    }
    return output;
  }-*/;

  private native double getDouble(String name) /*-{
    return this[name];
  }-*/;

//  private native Date initDate(Date date, double millis) /*-{
//    date.@java.util.Date::init(D)(millis);
//    return date;
//  }-*/;

  private native int getInt(String name) /*-{
    return this[name];
  }-*/;

  private native <V, P extends Property<T, V>> V nativeGet(P property) /*-{
    return this[property.@com.google.gwt.valuestore.shared.Property::getName()()];
  }-*/;
}
