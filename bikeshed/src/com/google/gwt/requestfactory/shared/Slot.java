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

import com.google.gwt.valuestore.shared.Property;

/**
 * A pointer to a property value of an Entity.
 *
 * @param <E> Entity
 * @param <V> Value
 */
public class Slot<E extends Entity<E>, V> {
  private final E entity;
  private final Property<E, V> property;

  public Slot(E entity, Property<E, V> property) {
    assert null != entity;
    assert null != property;
    this.entity = entity;
    this.property = property;
  }

  // cast is okay b/c of class comparison
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Slot<E, V> other = (Slot<E, V>) obj;
    if (!entity.getId().equals(other.entity.getId())) {
      return false;
    }
    if (!property.equals(other.property)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + entity.getId().hashCode();
    result = prime * result + property.hashCode();
    return result;
  }

  E getEntity() {
    return entity;
  }

  Property<E, V> getProperty() {
    return property;
  }
}
