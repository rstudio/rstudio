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
package com.google.gwt.validation.client.impl;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

/**
 * Abstract BeanDescriptor for use by generated {@link GwtBeanDescriptor}.
 * <p>
 * Subclasses are expected to call setDescriptorMap from the constructor.
 *
 * @param <T> the bean Type
 */
public abstract class AbstractBeanDescriptor<T> implements GwtBeanDescriptor<T> {

  private final Class<T> clazz;

  private final Set<ConstraintDescriptor<?>> constraints = new HashSet<ConstraintDescriptor<?>>();
  private final Map<String, PropertyDescriptor> descriptor = new HashMap<String, PropertyDescriptor>();

  /**
   * @param clazz
   */
  public AbstractBeanDescriptor(Class<T> clazz) {
    super();
    this.clazz = clazz;
  }

  public ConstraintFinder findConstraints() {
    // TODO(nchalko) implement
    return null;
  }

  public Set<PropertyDescriptor> getConstrainedProperties() {
    return new HashSet<PropertyDescriptor>(descriptor.values());
  }

  public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
    // Copy for safety
    return new HashSet<ConstraintDescriptor<?>>(constraints);
  }

  public PropertyDescriptor getConstraintsForProperty(String propertyName) {
    return descriptor.get(propertyName);
  }

  public Class<?> getElementClass() {
    return clazz;
  }

  public boolean hasConstraints() {
    return !constraints.isEmpty();
  }

  public boolean isBeanConstrained() {
    return true;
  }

  protected void setDescriptorMap(Map<String, PropertyDescriptor> map) {
    descriptor.clear();
    descriptor.putAll(map);
    constraints.clear();
    for (PropertyDescriptor p : descriptor.values()) {
      if (p.hasConstraints()) {
        constraints.addAll(p.getConstraintDescriptors());
      }
    }
  }
}