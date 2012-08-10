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

import com.google.gwt.validation.client.impl.metadata.BeanMetadata;
import com.google.gwt.validation.client.impl.metadata.ValidationGroupsMetadata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 */
public final class PropertyDescriptorImpl implements PropertyDescriptor {

  private boolean cascaded;
  private Set<ConstraintDescriptorImpl<?>> descriptors;
  private Class<?> elementClass;
  private String name;
  private ValidationGroupsMetadata validationGroupsMetadata;
  private BeanMetadata parentBeanMetadata;

  public PropertyDescriptorImpl(String name, Class<?> elementClass,
      boolean cascaded, BeanMetadata parentBeanMetadata,
      ConstraintDescriptorImpl<?>... descriptors) {
    this(name, elementClass, cascaded, parentBeanMetadata, null, descriptors);
  }

  public PropertyDescriptorImpl(String name, Class<?> elementClass,
      boolean cascaded, BeanMetadata parentBeanMetadata,
      ValidationGroupsMetadata validationGroupsMetadata,
      ConstraintDescriptorImpl<?>... descriptors) {
    super();

    this.elementClass = elementClass;
    this.cascaded = cascaded;
    this.name = name;
    this.validationGroupsMetadata = validationGroupsMetadata;
    this.parentBeanMetadata = parentBeanMetadata;
    this.descriptors = new HashSet<ConstraintDescriptorImpl<?>>(
        Arrays.asList(descriptors));
  }

  @Override
  public ConstraintFinder findConstraints() {
    return new ConstraintFinderImpl(parentBeanMetadata, validationGroupsMetadata, descriptors);
  }

  @Override
  public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
    return findConstraints().getConstraintDescriptors();
  }

  @Override
  public Class<?> getElementClass() {
    return elementClass;
  }

  @Override
  public String getPropertyName() {
    return name;
  }

  @Override
  public boolean hasConstraints() {
    return !descriptors.isEmpty();
  }

  @Override
  public boolean isCascaded() {
    return cascaded;
  }

  public void setValidationGroupsMetadata(ValidationGroupsMetadata validationGroupsMetadata) {
    // TODO(idol) Find some way to pass this via the constructor rather than after creation
    this.validationGroupsMetadata = validationGroupsMetadata;
  }

  public PropertyDescriptorImpl shallowCopy() {
    ConstraintDescriptorImpl<?>[] desc = new ConstraintDescriptorImpl<?>[descriptors.size()];
    descriptors.toArray(desc);
    return new PropertyDescriptorImpl( // 
        name, // 
        elementClass, // 
        cascaded, // 
        parentBeanMetadata, // 
        validationGroupsMetadata, // 
        desc);
  }
}
