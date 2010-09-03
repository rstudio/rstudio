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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

/**
 *
 */
public class PropertyDescriptorImpl implements PropertyDescriptor {

  private boolean cascaded;
  private Set<ConstraintDescriptor<?>> descriptors = new HashSet<ConstraintDescriptor<?>>();
  private Class<?> elementClass;
  private String name;

  /**
   * @param name
   * @param elementClass
   * @param cascaded
   * @param descriptors
   */
  public PropertyDescriptorImpl(String name, Class<?> elementClass,
      boolean cascaded, ConstraintDescriptor<?>... descriptors) {
    super();

    this.elementClass = elementClass;
    this.cascaded = cascaded;
    this.name = name;
    this.descriptors = new HashSet<ConstraintDescriptor<?>>(
        Arrays.asList(descriptors));
  }

  public ConstraintFinder findConstraints() {
    return null;
  }

  public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
    return Collections.unmodifiableSet(descriptors);
  }

  public Class<?> getElementClass() {
    return elementClass;
  }

  public String getPropertyName() {
    return name;
  }

  public boolean hasConstraints() {
    return !descriptors.isEmpty();
  }

  public boolean isCascaded() {
    return cascaded;
  }
}
