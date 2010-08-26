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
package com.google.gwt.requestfactory.shared;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * A proxy for a server-side domain object.
 */
public interface Record {
  Property<Long> id = new Property<Long>("id", Long.class);
                                                                                                                                            
  Property<Integer> version = new Property<Integer>("version", Integer.class);

  /**
   * Get this record's value for the given property. Behavior is undefined if
   * the record has no such property, or if the property has never been set. It
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
  Long getId();

  /**
   * Get a "pointer" to value of this property in the receiver, useful for
   * making rpc requests against values that have not yet reached the client.
   * 
   * @param <V> the type of the property value
   * @param property the property referred to
   * @return a reference to the receiver's value of this property
   */
  <V> PropertyReference<V> getRef(Property<V> property);

  /**
   * @return the version of this Record
   */
  Integer getVersion();
}
