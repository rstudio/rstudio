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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.valuestore.shared.PrimitiveProperty;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.ValueRef;
import com.google.gwt.valuestore.shared.Values;
import com.google.gwt.valuestore.shared.ValuesChangedEvent;
import com.google.gwt.valuestore.shared.ValuesKey;

import java.util.Date;

/**
 * JSO implementation of {@link Values}.
 * 
 * @param <K> value type
 */
public final class ValuesImpl<K extends ValuesKey<K>> extends JavaScriptObject
    implements Values<K> {

  public static native <K extends ValuesKey<K>> JsArray<ValuesImpl<K>> arrayFromJson(
      String json) /*-{
    return eval(json);
  }-*/;

  public static <K extends ValuesKey<K>> ValuesImpl<K> emptyCopy(Values<K> from) {
    ValuesImpl<K> copy = create().cast();
    final K key = from.getKey();
    copy.setKey(key);
    copy.set(key.getId(), from.get(key.getId()));
    copy.set(key.getVersion(), from.get(key.getVersion()));
    return copy;
  }

  public static native <K extends ValuesKey<K>> ValuesImpl<K> fromJson(
      String json) /*-{
    return eval(json);
  }-*/;;

  private static native ValuesImpl<?> create() /*-{
    return {};
  }-*/;

  protected ValuesImpl() {
  }

  public ValuesChangedEvent<K, ?> createChangeEvent() {
    return getKey().createChangeEvent(this);
  }

  public native void delete(String name)/*-{
    delete this[name];
  }-*/;

  @SuppressWarnings("unchecked")
  public <V, P extends Property<K, V>> V get(P property) {

    if (property instanceof PrimitiveProperty) {
      PrimitiveProperty<K, V> prim = (PrimitiveProperty<K, V>) property;

      if (Integer.class.equals(prim.getValueType())) {
        return (V) Integer.valueOf(getInt(prim.getName()));
      }
      if (Date.class.equals(prim.getValueType())) {
        double millis = getDouble(prim.getName());
        if (GWT.isScript()) {
          return (V) dateForDouble(millis);
        } else {
          // In dev mode, we're using real JRE dates
          return (V) new Date((long) millis);
        }
      }
    }

    return nativeGet(property);
  }

  public String getId() {
    return get(getKey().getId());
  }

  public native K getKey() /*-{
    return this['__key'];
  }-*/;

  public <V> ValueRef<K, V> getRef(Property<K, V> property) {
    return new ValueRef<K, V>(this, property);
  }

  /**
   * @param property
   * @return
   */
  public native boolean isDefined(String name)/*-{
    return this[name] !== undefined;
  }-*/;

  public boolean isEmpty() {
    final Property<K, String> id = getKey().getId();
    final Property<K, Integer> version = getKey().getVersion();

    for (Property<K, ?> property : getKey().all()) {
      if ((property != id) && (property != version)
          && (isDefined(property.getName()))) {
        return false;
      }
    }
    return true;
  }

  public boolean merge(ValuesImpl<?> from) {
    assert getKey() == from.getKey();

    boolean changed = false;

    ValuesImpl<K> newValues = from.cast();

    for (Property<K, ?> property : getKey().all()) {
      if (newValues.isDefined(property.getName())) {
        changed |= copyPropertyIfDifferent(property.getName(), newValues);
      }
    }

    return changed;
  }

  public <V> void set(Property<K, V> property, V value) {
    if (value instanceof String) {
      setString(property.getName(), (String) value);
      return;
    } 
    if (value instanceof Integer) {
      setInt(property.getName(), (Integer) value);
      return;
    } 
    throw new UnsupportedOperationException("Can't yet set properties of type " + value.getClass().getName());
  }

  public native void setKey(K key) /*-{
    this['__key'] = key;
  }-*/;;

  /**
   * Return JSON representation using org.json library.
   * 
   * @return returned string.
   */
  public native String toJson() /*-{
    var replacer = function(key, value) {
      if (key == '__key') {
        return;
      }
      return value;
    }
    return JSON.stringify(this, replacer);
  }-*/;

  private native boolean copyPropertyIfDifferent(String name, ValuesImpl<K> from) /*-{
    if (this[name] == from[name]) {
      return false;
    }
    this[name] = from[name];
    return true;
  }-*/;

  private native Date dateForDouble(double millis) /*-{
    return @java.util.Date::createFrom(D)(millis);
  }-*/;

  private native double getDouble(String name) /*-{
    return this[name];
  }-*/;

  private native int getInt(String name) /*-{
    return this[name];
  }-*/;

  private native <V, P extends Property<K, V>> V nativeGet(P property) /*-{
    return this[property.@com.google.gwt.valuestore.shared.Property::getName()()];
  }-*/;

  private native void setInt(String name, int value) /*-{
    this[name] = value;
  }-*/;

  private native void setString(String name, String value) /*-{
    this[name] = value;
  }-*/;
}
