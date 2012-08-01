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

import com.google.gwt.validation.client.ValidationGroupsMetadata;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Abstract BeanDescriptor for use by generated {@link GwtBeanDescriptor}.
 * <p>
 * Subclasses are expected to call setDescriptorMap from the constructor.
 * 
 * @param <T> the bean Type
 */
public final class GwtBeanDescriptorImpl<T> implements GwtBeanDescriptor<T> {

  /**
   * Builder for {@link GwtBeanDescriptor}s.
   *
   * @param <T> the bean Type
   */
  public static final class Builder<T> {

    private final Class<T> clazz;
    private final Map<String, PropertyDescriptorImpl> descriptorMap =
        new HashMap<String, PropertyDescriptorImpl>();
    private final Set<ConstraintDescriptorImpl<? extends Annotation>> constraints =
        new HashSet<ConstraintDescriptorImpl<? extends Annotation>>();
    private boolean isConstrained;
    private BeanMetadata beanMetadata;

    private Builder(Class<T> clazz) {
      this.clazz = clazz;
    }

    public Builder<T> add(
        ConstraintDescriptorImpl<? extends Annotation> constraintDescriptor) {
      constraints.add(constraintDescriptor);
      return this;
    }

    public GwtBeanDescriptorImpl<T> build() {
      return new GwtBeanDescriptorImpl<T>(clazz, isConstrained, descriptorMap, beanMetadata,
          constraints);
    }

    public Builder<T> put(String key, PropertyDescriptorImpl value) {
      descriptorMap.put(key, value.shallowCopy());
      return this;
    }

    public Builder<T> setBeanMetadata(BeanMetadata beanMetadata) {
      this.beanMetadata = beanMetadata;
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
  private final Set<ConstraintDescriptorImpl<?>> constraints = new HashSet<ConstraintDescriptorImpl<?>>();

  private final Map<String, PropertyDescriptorImpl> descriptorMap = new HashMap<String, PropertyDescriptorImpl>();
  private final boolean isBeanConstrained;

  private final BeanMetadata beanMetadata;
  
  private ValidationGroupsMetadata validationGroupsMetadata;

  private GwtBeanDescriptorImpl(Class<T> clazz, boolean isConstrained,
      Map<String, PropertyDescriptorImpl> descriptorMap, BeanMetadata beanMetadata,
      Set<ConstraintDescriptorImpl<?>> constraints) {
    this.clazz = clazz;
    this.isBeanConstrained = isConstrained;
    this.beanMetadata = beanMetadata;
    this.descriptorMap.putAll(descriptorMap);
    this.constraints.addAll(constraints);
  }

  @Override
  public ConstraintFinder findConstraints() {
    return new ConstraintFinderImpl(beanMetadata, validationGroupsMetadata, constraints);
  }

  @Override
  public Set<PropertyDescriptor> getConstrainedProperties() {
    Collection<PropertyDescriptorImpl> props = descriptorMap.values();
    for (PropertyDescriptorImpl prop : props) {
      prop.setValidationGroupsMetadata(validationGroupsMetadata);
    }
    return new HashSet<PropertyDescriptor>(props);
  }

  @Override
  public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
    return findConstraints().getConstraintDescriptors();
  }

  @Override
  public PropertyDescriptor getConstraintsForProperty(String propertyName) {
    PropertyDescriptorImpl propDesc = descriptorMap.get(propertyName);
    if (propDesc != null) {
      propDesc.setValidationGroupsMetadata(validationGroupsMetadata);
    }
    return propDesc;
  }

  @Override
  public Class<?> getElementClass() {
    return clazz;
  }

  @Override
  public boolean hasConstraints() {
    return !constraints.isEmpty();
  }

  @Override
  public boolean isBeanConstrained() {
    return isBeanConstrained;
  }

  @Override
  public void setValidationGroupsMetadata(ValidationGroupsMetadata validationGroupsMetadata) {
    // TODO(idol) Find some way to pass this via the constructor rather than after creation
    this.validationGroupsMetadata = validationGroupsMetadata;
  }
}