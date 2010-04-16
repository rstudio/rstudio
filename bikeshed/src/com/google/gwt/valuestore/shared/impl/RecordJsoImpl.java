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
package com.google.gwt.valuestore.shared.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.PropertyReference;
import com.google.gwt.valuestore.shared.Record;

import java.util.Date;

/**
 * JSO implementation of {@link Record}, used to back subclasses of
 * {@link RecordImpl}.
 */
public class RecordJsoImpl extends JavaScriptObject implements Record {
  public static native JsArray<RecordJsoImpl> arrayFromJson(String json) /*-{
    return eval(json);
  }-*/;

  public static RecordJsoImpl emptyCopy(RecordImpl from) {
    RecordJsoImpl copy = create();
    final RecordSchema<?> schema = from.getSchema();
    copy.setSchema(schema);
    copy.set(Record.id, from.get(Record.id));
    copy.set(Record.version, from.get(Record.version));
    return copy;
  }

  private static native RecordJsoImpl create() /*-{
    return {};
  }-*/;

  protected RecordJsoImpl() {
  }

  public final native void delete(String name)/*-{
    delete this[name];
  }-*/;

  @SuppressWarnings("unchecked")
  public final <V> V get(Property<V> property) {
    assert isDefined(property.getName()) : "Cannot ask for a property before setting it: "
        + property.getName();

    if (Integer.class.equals(property.getType())) {
      return (V) Integer.valueOf(getInt(property.getName()));
    }
    if (Date.class.equals(property.getType())) {
      double millis = getDouble(property.getName());
      if (GWT.isScript()) {
        return (V) dateForDouble(millis);
      } else {
        // In dev mode, we're using real JRE dates
        return (V) new Date((long) millis);
      }
    }

    return get(property.getName());
  }

  public final native <T> T get(String propertyName) /*-{
    return this[propertyName];
  }-*/;

  public final String getId() {
    return this.get(id);
  }

  public final <V> PropertyReference<V> getRef(Property<V> property) {
    return new PropertyReference<V>(this, property);
  }

  public final native RecordSchema<?> getSchema() /*-{
    return this['__key'];
  }-*/;

  public final String getVersion() {
    return this.get(version);
  }

  /**
   * @param name
   */
  public final native boolean isDefined(String name)/*-{
    return this[name] !== undefined;
  }-*/;

  public final boolean isEmpty() {
    for (Property<?> property : getSchema().allProperties()) {
      if ((property != Record.id) && (property != Record.version)
          && (isDefined(property.getName()))) {
        return false;
      }
    }
    return true;
  }

  public final boolean merge(RecordJsoImpl from) {
    assert getSchema() == from.getSchema();

    boolean changed = false;
    for (Property<?> property : getSchema().allProperties()) {
      if (from.isDefined(property.getName())) {
        changed |= copyPropertyIfDifferent(property.getName(), from);
      }
    }

    return changed;
  }

  public final <V> void set(Property<V> property, V value) {
    if (value instanceof String) {
      setString(property.getName(), (String) value);
      return;
    }
    if (value instanceof Integer) {
      setInt(property.getName(), (Integer) value);
      return;
    }
    throw new UnsupportedOperationException("Can't yet set properties of type "
        + value.getClass().getName());
  }

  public final native void setSchema(RecordSchema<?> schema) /*-{
    this['__key'] = schema;
  }-*/;

  /**
   * Return JSON representation using org.json library.
   * 
   * @return returned string.
   */
  public final native String toJson() /*-{
    var replacer = function(key, value) {
      if (key == '__key') {
        return;
      }
      return value;
    }
    return JSON.stringify(this, replacer);
  }-*/;

  private native boolean copyPropertyIfDifferent(String name, RecordJsoImpl from) /*-{
    if (this[name] == from[name]) {
      return false;
    }
    this[name] = from[name];
    return true;
  }-*/;

  private native Date dateForDouble(double millis) /*-{
    return @java.util.Date::createFrom(D)(millis);
  }-*/;;

  private native double getDouble(String name) /*-{
    return this[name];
  }-*/;

  private native int getInt(String name) /*-{
    return this[name];
  }-*/;

  private native void setInt(String name, int value) /*-{
    this[name] = value;
  }-*/;

  private native void setString(String name, String value) /*-{
    this[name] = value;
  }-*/;
}
