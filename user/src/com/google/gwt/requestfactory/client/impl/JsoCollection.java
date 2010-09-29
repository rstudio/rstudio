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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.requestfactory.shared.impl.Property;

/**
 * Interface for injecting dependencies into JsoList and JsoSet.
 */
public interface JsoCollection {

  /**
   * Inject dependencies needed by jso collections to commit mutations to
   * {@link DeltaValueStoreJsonImpl}.
   *
   * @param property the Property corresponding to the contained type
   * @param proxy the Proxy on which this collection resides
   */
  void setDependencies(Property<?> property, ProxyImpl proxy);

  /**
   * Returns the JavaScriptObject (usually a raw JS Array) backing this
   * collection.
   */
  JavaScriptObject asJso();
}
