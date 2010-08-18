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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.valuestore.shared.EnumProperty;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.PropertyReference;
import com.google.gwt.valuestore.shared.Record;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * <p> <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span> </p> JSO implementation of {@link Record}, used to back subclasses of
 * {@link RecordImpl}.
 */
public class RecordJsoImpl extends JavaScriptObject implements Record {

  /**
   * JSO to hold result and related objects.
   */
  public static class JsonResults extends JavaScriptObject {

    protected JsonResults() {
    }

    public final native JavaScriptObject getJavascriptResult() /*-{
      return this.result;
    }-*/;

    public final native JsArray<RecordJsoImpl> getListResult() /*-{
      return this.result;
    }-*/;

    public final native RecordJsoImpl getRecordResult() /*-{
      return this.result;
    }-*/;

    public final native JavaScriptObject getRelated() /*-{
      return this.related;
    }-*/;
  }

  public static native JsArray<RecordJsoImpl> arrayFromJson(String json) /*-{
    return eval(json);
  }-*/;

  public static RecordJsoImpl create(Long id, Integer version,
      final RecordSchema<?> schema) {
    RecordJsoImpl copy = create();
    copy.setSchema(schema);
    copy.set(Record.id, id);
    copy.set(Record.version, version);
    return copy;
  }

  public static RecordJsoImpl emptyCopy(RecordJsoImpl jso) {
    Long id = jso.get(Record.id);
    Integer version = jso.get(Record.version);
    final RecordSchema<?> schema = jso.getSchema();

    return create(id, version, schema);
  }

  public static native RecordJsoImpl fromJson(String json) /*-{
    // TODO: clean this
    eval("xyz=" + json);
    return xyz;
  }-*/;

  public static native JsonResults fromResults(String json) /*-{
    // TODO: clean this
    eval("xyz=" + json);
    return xyz;
  }-*/;

  /* Made protected, for testing */

  protected static native RecordJsoImpl create() /*-{
    return {};
  }-*/;

  protected RecordJsoImpl() {
  }

  public final native void delete(String name)/*-{
    delete this[name];
  }-*/;

  @SuppressWarnings("unchecked")
  public final <V> V get(Property<V> property) {
    // TODO lax for the moment b/c client code can't yet reasonably make the
    // request
    // assert isDefined(property.getName()) :
    // "Cannot ask for a property before setting it: "
    // + property.getName();
    if (isNullOrUndefined(property.getName())) {
      return null;
    }
    try {
      if (Boolean.class.equals(property.getType())) {
        return (V) Boolean.valueOf(getBoolean(property.getName()));
      }
      if (Character.class.equals(property.getType())) {
        return (V) Character.valueOf(String.valueOf(get(property.getName())).charAt(0));
      }
      if (Byte.class.equals(property.getType())) {
        return (V) Byte.valueOf((byte) getInt(property.getName()));
      }
      if (Short.class.equals(property.getType())) {
        return (V) Short.valueOf((short) getInt(property.getName()));
      }
      if (Float.class.equals(property.getType())) {
        return (V) Float.valueOf((float) getDouble(property.getName()));
      }
      if (BigInteger.class.equals(property.getType())) {
        return (V) new BigDecimal((String) get(property.getName())).toBigInteger();
      }
      if (BigDecimal.class.equals(property.getType())) {
        return (V) new BigDecimal((String) get(property.getName()));
      }
      if (Integer.class.equals(property.getType())) {
        return (V) Integer.valueOf(getInt(property.getName()));
      }
      if (Long.class.equals(property.getType())) {
        return (V) Long.valueOf((String) get(property.getName()));
      }
      if (Double.class.equals(property.getType())) {
        if (!isDefined(property.getName())) {
          return (V) new Double(0.0);
        }
        return (V) Double.valueOf(getDouble(property.getName()));
      }
      if (Date.class.equals(property.getType())) {
        double millis = new Date().getTime();
        if (isDefined(property.getName())) {
          millis = Double.parseDouble((String) get(property.getName()));
        }
        if (GWT.isScript()) {
          return (V) dateForDouble(millis);
        } else {
          // In dev mode, we're using real JRE dates
          return (V) new Date((long) millis);
        }
      }
    } catch (final Exception ex) {
      throw new IllegalStateException(
          "Property  " + property.getName() + " has invalid " + " value "
              + get(property.getName()) + " for type " + property.getType());
    }

    if (property instanceof EnumProperty) {
      EnumProperty<V> eProperty = (EnumProperty<V>) property;
      Enum[] values = (Enum[]) eProperty.getValues();
      int ordinal = getInt(property.getName());
      for (Enum value : values) {
        if (ordinal == value.ordinal()) {
          return (V) value;
        }
      }
    }

    if (String.class == property.getType()) {
      return (V) get(property.getName());
    }
    // at this point, we checked all the known primitive/value types we support
    // TODO: handle embedded types, List, Set, Map

    // else, it must be a record type
    String relatedId = (String) get(property.getName());
    if (relatedId == null) {
      return null;
    } else {
      // TODO: should cache this or modify JSO field in place
      String schemaAndId[] = relatedId.split("-");
      return (V) getValueStore().getRecordBySchemaAndId(
          getRequestFactory().getSchema(schemaAndId[0]),
          Long.valueOf(schemaAndId[1]));
    }
  }

  public final native <T> T get(String propertyName) /*-{
    return this[propertyName] || null;
  }-*/;

  public final Long getId() {
    return this.get(id);
  }

  public final <V> PropertyReference<V> getRef(Property<V> property) {
    return new PropertyReference<V>(this, property);
  }
  
  // TODO: HACK! Need to look up token to schema for relatins
  public final native RequestFactoryJsonImpl getRequestFactory() /*-{
    return this['__rf'];
  }-*/;

  public final native RecordSchema<?> getSchema() /*-{
    return this['__key'];
  }-*/;
  
  // TODO: HACK! Make VS public and stash it in the record for relation lookups
  public final native ValueStoreJsonImpl getValueStore() /*-{
     return this['__vs'];  
  }-*/;

  public final Integer getVersion() {
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
      if ((property != Record.id) && (property != Record.version) && (isDefined(
          property.getName()))) {
        return false;
      }
    }
    return true;
  }

  /**
   * @param name
   */
  public final native boolean isNullOrUndefined(String name)/*-{
    return this[name] == null;
  }-*/;

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
     if (value instanceof Character) {
      setString(property.getName(), String.valueOf(value));
      return;
    }

    if (value instanceof Long || value instanceof BigDecimal
        || value instanceof BigInteger) {
      setString(property.getName(), String.valueOf(value));
      return;
    }

    if (value instanceof Integer || value instanceof Short
        || value instanceof Byte) {
      setInt(property.getName(), ((Number) value).intValue());
      return;
    }

    if (value instanceof Date) {
      long millis = ((Date) value).getTime();
      setString(property.getName(), String.valueOf(millis));
      return;
    }
    if (value instanceof Double || value instanceof Float) {
      setDouble(property.getName(), ((Number) value).doubleValue());
      return;
    }

    if (value instanceof Enum<?>) {
      setInt(property.getName(), ((Enum<?>) value).ordinal());
      return;
    }

    if (value instanceof Boolean) {
      setBoolean(property.getName(), ((Boolean) value).booleanValue());
    }

    if (value instanceof Record) {
      setString(property.getName(),
          getRequestFactory().getSchema(value.getClass().getName()).getToken().getName()
              + "-" + String.valueOf(((Record) value).getId()));  
    }

    throw new UnsupportedOperationException(
        "Can't yet set properties of type " + value.getClass().getName());
  }

  // TODO: HACK! Need to look up token to schema for relatins
  public final native void setRequestFactory(RequestFactoryJsonImpl requestFactory) /*-{
    this['__rf'] = requestFactory;
  }-*/;

  public final native void setSchema(RecordSchema<?> schema) /*-{
    this['__key'] = schema;
  }-*/;

  // TODO: HACK! Make VS public and stash it in the record for relation lookups

  public final native void setValueStore(ValueStoreJsonImpl valueStoreJson) /*-{
    this['__vs']=valueStoreJson;
  }-*/;

  /**
   * Return JSON representation using org.json library.
   *
   * @return returned string.
   */
  public final native String toJson() /*-{
    // Safari 4.0.5 appears not to honor the replacer argument, so we can't do this:

    //    var replacer = function(key, value) {
    //      if (key == '__key') {
    //        return;
    //      }
    //      return value;
    //    }
    // return $wnd.JSON.stringify(this, replacer);

    var key = this.__key;
    delete this.__key;
    // TODO verify that the stringify() from json2.js works on IE
    var rtn = $wnd.JSON.stringify(this);
    this.__key = key;
    return rtn;
  }-*/;

  /**
   * Return JSON representation of just id and version fields, using org.json
   * library.
   *
   * @return returned string.
   */
  public final native String toJsonIdVersion() /*-{
    // Safari 4.0.5 appears not to honor the replacer argument, so we can't do this:
    //    var replacer = function(key, value) {
    //      if (key == 'id' || key == 'version') {
    //        return value;
    //      }
    //      return;
    //    }
    //    return $wnd.JSON.stringify(this, replacer);
    var object = { id: this.id, version: this.version };
    return $wnd.JSON.stringify(object);
  }-*/;

  private native boolean copyPropertyIfDifferent(String name,
      RecordJsoImpl from) /*-{
    if (this[name] == from[name]) {
      return false;
    }
    this[name] = from[name];
    return true;
  }-*/;

  private native Date dateForDouble(double millis) /*-{
    return @java.util.Date::createFrom(D)(millis);
  }-*/;

  private native boolean getBoolean(String name) /*-{
    return this[name];
  }-*/;

  private native double getDouble(String name) /*-{
    return this[name];
  }-*/;

  private native int getInt(String name) /*-{
    return this[name];
  }-*/;

  private native void setBoolean(String name, boolean value) /*-{
    this[name] = value;
  }-*/;

  private native void setDouble(String name, double value) /*-{
    this[name] = value;
  }-*/;

  private native void setInt(String name, int value) /*-{
    this[name] = value;
  }-*/;

  private native void setString(String name, String value) /*-{
    this[name] = value;
  }-*/;
}
