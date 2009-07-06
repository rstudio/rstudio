/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.rpc.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Contains serialization dispatch information for types that don't support
 * simple serialization.
 */
public final class TypeOverrides extends JavaScriptObject {
  /**
   * An individual entry, which is a wrapper around the CFS's serialize method.
   */
  public static final class SerializeFunction extends JavaScriptObject {
    protected SerializeFunction() {
    }

    public native void serialize(SerializationStreamWriter writer,
        Object instance) /*-{
      this(writer, instance);
    }-*/;
  }

  public static TypeOverrides create() {
    return JavaScriptObject.createObject().cast();
  }

  protected TypeOverrides() {
  }

  public native String[] getExtraFields(String className) /*-{
    return this['B' + className];
  }-*/;

  public native SerializeFunction getOverride(String className) /*-{
    return this['A' + className];
  }-*/;

  public native boolean hasExtraFields(String className) /*-{
    return !!this['B' + className];
  }-*/;

  public native void set(String className, SerializeFunction override) /*-{
    this['A' + className] = override;
  }-*/;

  public void set(String className, SerializeFunction override,
      String[] extraFields) {
    set(className, override);
    set(className, extraFields);
  }

  public native void set(String className, String[] extraFields) /*-{
    this['B' + className] = extraFields;
  }-*/;
}
