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
package com.google.gwt.requestfactory.client.impl.messages;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.HashMap;
import java.util.HashSet;

/**
 * JSO that holds an entity reference being returned from the server.
 */
public class ReturnRecord extends JavaScriptObject {

  static native void fillKeys(JavaScriptObject jso, HashSet<String> s) /*-{
    for (key in jso) {
      if (jso.hasOwnProperty(key)) {
        s.@java.util.HashSet::add(Ljava/lang/Object;)(key);
      }
    }
  }-*/;

  protected ReturnRecord() {
  }

  public final native void fillViolations(HashMap<String, String> s) /*-{
    for (key in this.violations) {
      if (this.violations.hasOwnProperty(key)) {
        s.@java.util.HashMap::put(Ljava/lang/Object;Ljava/lang/Object;)(key, this.violations[key]);
      }
    }
  }-*/;

  public final native Object get(String propertyName) /*-{
    return Object(this[propertyName]);
  }-*/;

  public final String getEncodedId() {
    String parts[] = getSchemaAndId().split("@");
    return parts[1];
  }

  public final native String getFutureId()/*-{
    return this[@com.google.gwt.requestfactory.shared.impl.Constants::ENCODED_FUTUREID_PROPERTY];
  }-*/;

  public final String getSchema() {
    String parts[] = getSchemaAndId().split("@");
    return parts[0];
  }

  public final native String getSchemaAndId() /*-{
    return this[@com.google.gwt.requestfactory.shared.impl.Constants::ENCODED_ID_PROPERTY];
  }-*/;

  public final native String getSimpleId()/*-{
    return this[@com.google.gwt.requestfactory.shared.impl.Constants::ENCODED_ID_PROPERTY];
  }-*/;

  public final native int getVersion()/*-{
    return this[@com.google.gwt.requestfactory.shared.impl.Constants::ENCODED_VERSION_PROPERTY] || 0;
  }-*/;

  public final native boolean hasFutureId()/*-{
    return @com.google.gwt.requestfactory.shared.impl.Constants::ENCODED_FUTUREID_PROPERTY in this;
  }-*/;

  public final native boolean hasId()/*-{
    return @com.google.gwt.requestfactory.shared.impl.Constants::ENCODED_ID_PROPERTY in this;
  }-*/;

  public final native boolean hasProperty(String property) /*-{
    return this.hasOwnProperty(property);
  }-*/;

  public final native boolean hasViolations()/*-{
    return @com.google.gwt.requestfactory.shared.impl.Constants::VIOLATIONS_TOKEN in this;
  }-*/;

  /**
   * Returns <code>true</code> if the property explicitly set to null.
   */
  public final native boolean isNull(String property) /*-{
    return this[property] === null;
  }-*/;

}