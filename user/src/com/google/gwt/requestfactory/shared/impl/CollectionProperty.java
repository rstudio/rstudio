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
package com.google.gwt.requestfactory.shared.impl;

import java.util.Collection;

/**
 * <p> <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span> </p> Defines a property of a {@link com.google.gwt.requestfactory.shared.EntityProxy} that contains a
 * one-to-many set of related values.
 *
 * @param <C> the type of the Container, must be List or Set
 * @param <E> the type of the element the container contains
 */
public class CollectionProperty<C extends Collection, E> extends Property<C> {

  private Class<E> leafType;

  public CollectionProperty(String name, String displayName, Class<C> colType,
      Class<E> type) {
    super(name, displayName, colType);
    this.leafType = type;
  }

  public CollectionProperty(String name, Class<C> colType, Class<E> type) {
    super(name, colType);
    this.leafType = type;
  }

  public Class<E> getLeafType() {
    return leafType;
  }
}
