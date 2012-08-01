/*
 * Copyright 2012 Google Inc.
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

import com.google.gwt.validation.client.ConstraintOrigin;
import com.google.gwt.validation.client.Group;
import com.google.gwt.validation.client.GroupChain;
import com.google.gwt.validation.client.GroupChainGenerator;
import com.google.gwt.validation.client.ValidationGroupsMetadata;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.groups.Default;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.Scope;
import javax.validation.metadata.ElementDescriptor.ConstraintFinder;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 */
public final class ConstraintFinderImpl implements ConstraintFinder {
  private Set<ConstraintDescriptorImpl<?>> constraintDescriptors;
  private ValidationGroupsMetadata validationGroupsMetadata;
  private List<Class<?>> groups;
  private Set<ConstraintOrigin> definedInSet;
  private Set<ElementType> elementTypes;
  private BeanMetadata beanMetadata;

  public ConstraintFinderImpl(BeanMetadata beanMetadata,
      ValidationGroupsMetadata validationGroupsMetadata, 
      Set<ConstraintDescriptorImpl<?>> constraintDescriptors) {
    this.validationGroupsMetadata = validationGroupsMetadata;
    this.constraintDescriptors = constraintDescriptors;
    this.beanMetadata = beanMetadata;
    elementTypes = new HashSet<ElementType>();
    elementTypes.add(ElementType.TYPE);
    elementTypes.add(ElementType.METHOD);
    elementTypes.add(ElementType.FIELD);
    definedInSet = new HashSet<ConstraintOrigin>();
    definedInSet.add(ConstraintOrigin.DEFINED_LOCALLY);
    definedInSet.add(ConstraintOrigin.DEFINED_IN_HIERARCHY);
    groups = Collections.emptyList();
  }

  @Override
  public ConstraintFinder declaredOn(ElementType... types) {
    elementTypes.clear();
    elementTypes.addAll(Arrays.asList(types));
    return this;
  }

  @Override
  public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
    if (validationGroupsMetadata == null) {
      // sanity check - this could be null if the caller does not set group metadata first
      throw new IllegalStateException("ConstraintFinderImpl not initialized properly. A " +
          "ValidationGroupsMetadata object is required by GWT to properly find all constraint " +
          "descriptors.");
    }
    Set<ConstraintDescriptor<?>> matchingDescriptors = new HashSet<ConstraintDescriptor<?>>();
    findMatchingDescriptors(matchingDescriptors);
    return Collections.unmodifiableSet(matchingDescriptors);
  }

  @Override
  public boolean hasConstraints() {
    return !getConstraintDescriptors().isEmpty();
  }

  @Override
  public ConstraintFinder lookingAt(Scope scope) {
    if (scope.equals(Scope.LOCAL_ELEMENT)) {
      definedInSet.remove(ConstraintOrigin.DEFINED_IN_HIERARCHY);
    }
    return this;
  }

  @Override
  public ConstraintFinder unorderedAndMatchingGroups(Class<?>... groups) {
    this.groups = new ArrayList<Class<?>>();
    for (Class<?> clazz : groups) {
      if (Default.class.equals(clazz) && beanMetadata.defaultGroupSequenceIsRedefined()) {
        this.groups.addAll(beanMetadata.getDefaultGroupSequence());
      }
      else {
        this.groups.add(clazz);
      }
    }
    return this;
  }

  private void addMatchingDescriptorsForGroup(Class<?> group,
      Set<ConstraintDescriptor<?>> matchingDescriptors) {
    for (ConstraintDescriptorImpl<?> descriptor : constraintDescriptors) {
      if (definedInSet.contains(descriptor.getDefinedOn())
          && elementTypes.contains(descriptor.getElementType())
          && descriptor.getGroups().contains(group)) {
        matchingDescriptors.add(descriptor);
      }
    }
  }

  private void findMatchingDescriptors(Set<ConstraintDescriptor<?>> matchingDescriptors) {
    if (!groups.isEmpty()) {
      GroupChain groupChain =
          new GroupChainGenerator(validationGroupsMetadata).getGroupChainFor(groups);
      Iterator<Group> groupIterator = groupChain.getGroupIterator();
      while (groupIterator.hasNext()) {
        Group g = groupIterator.next();
        addMatchingDescriptorsForGroup(g.getGroup(), matchingDescriptors);
      }
    }
    else {
      for (ConstraintDescriptorImpl<?> descriptor : constraintDescriptors) {
        if (definedInSet.contains(descriptor.getDefinedOn()) &&
            elementTypes.contains(descriptor.getElementType())) {
          matchingDescriptors.add(descriptor);
        }
      }
    }
  }

}
