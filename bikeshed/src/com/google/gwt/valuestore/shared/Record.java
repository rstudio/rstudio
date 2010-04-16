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
package com.google.gwt.valuestore.shared;


/**
 * An entry in a {@link com.google.gwt.valuestore.shared.ValueStore ValueStore}.
 */
public interface Record {
  public static final Property<String> id = new Property<String>("id",
      String.class);
  public static final Property<Integer> version = new Property<Integer>(
      "version", Integer.class);

  /**
   * Get this record's value for the given property. Behavior is undefined if
   * the record has no such property, or if the property have never been set. It
   * is unusual to call this method directly. Rather it is expected to be called
   * by bean-style getter methods provided by implementing classes.
   * 
   * @param <V> the type of the property's value
   * @param property the property to fetch
   * @return the value
   */
  <V> V get(Property<V> property);

  /**
   * @return the id of this Record
   */
  String getId();

  /**
   * Get a "pointer" to value of this property in the receiver, useful
   * for making rpc requests against values that have not yet reached
   * the client. 
   * 
   * @param <V> the type of the property value
   * @param property the property referrred to 
   * @return a reference to the receiver's value of this propertyf
   */
  <V> PropertyReference<V> getRef(Property<V> property);

  /**
   * @return the version of this Record
   */
  Integer getVersion();
}
