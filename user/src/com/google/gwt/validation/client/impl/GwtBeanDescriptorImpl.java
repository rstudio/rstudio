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
public final class GwtBeanDescriptorImpl<T> implements GwtBeanDescriptor<T> {

  /**
   * Builder for {@link GwtBeanDescriptors}.
   * 
   * @param <T> the bean Type
   */
  public static final class Builder<T> {

    private final Class<T> clazz;
    private final Map<String, PropertyDescriptor> descriptorMap = new HashMap<String, PropertyDescriptor>();
    private boolean isConstrained;

    private Builder(Class<T> clazz) {
      this.clazz = clazz;
    }

    public GwtBeanDescriptorImpl<T> build() {
      return new GwtBeanDescriptorImpl<T>(clazz, isConstrained, descriptorMap);
    }

    public Builder<T> put(String key, PropertyDescriptor value) {
      descriptorMap.put(key, value);
      return this;
    }

    public Builder<T> setConstrained(boolean isConstrained) {
      this.isConstrained = isConstrained;
      return this;
    }
  }

  public static <T> Builder<T> builder(Class<T> clazz) {
    return new Builder<T>(clazz);
  }

  private final Class<T> clazz;
  private final Set<ConstraintDescriptor<?>> constraints = new HashSet<ConstraintDescriptor<?>>();

  private final Map<String, PropertyDescriptor> descriptorMap = new HashMap<String, PropertyDescriptor>();
  private final boolean isBeanConstrained;

  private GwtBeanDescriptorImpl(Class<T> clazz, boolean isConstrained,
      Map<String, PropertyDescriptor> descriptorMap) {
    super();
    this.clazz = clazz;
    this.isBeanConstrained = isConstrained;
    this.descriptorMap.putAll(descriptorMap);
  }

  public ConstraintFinder findConstraints() {
    // TODO(nchalko) implement
    return null;
  }

  public Set<PropertyDescriptor> getConstrainedProperties() {
    return new HashSet<PropertyDescriptor>(descriptorMap.values());
  }

  public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
    // Copy for safety
    return new HashSet<ConstraintDescriptor<?>>(constraints);
  }

  public PropertyDescriptor getConstraintsForProperty(String propertyName) {
    return descriptorMap.get(propertyName);
  }

  public Class<?> getElementClass() {
    return clazz;
  }

  public boolean hasConstraints() {
    return !constraints.isEmpty();
  }

  public boolean isBeanConstrained() {
    return isBeanConstrained;
  }

  protected void setDescriptorMap(Map<String, PropertyDescriptor> map) {
    descriptorMap.clear();
    descriptorMap.putAll(map);
    constraints.clear();
    for (PropertyDescriptor p : descriptorMap.values()) {
      if (p.hasConstraints()) {
        constraints.addAll(p.getConstraintDescriptors());
      }
    }
  }
}