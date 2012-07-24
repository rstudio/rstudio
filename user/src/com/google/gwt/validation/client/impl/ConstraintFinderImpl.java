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
import com.google.gwt.validation.client.GroupInheritanceMap;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
  private GroupInheritanceMap groupInheritanceMap;
  private List<Class<?>> groups;
  private Set<ConstraintOrigin> definedInSet;
  private Set<ElementType> elementTypes;

  public ConstraintFinderImpl(GroupInheritanceMap groupInheritanceMap, 
      Set<ConstraintDescriptorImpl<?>> constraintDescriptors) {
    this.groupInheritanceMap = groupInheritanceMap;
    this.constraintDescriptors = constraintDescriptors;
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
    if (groupInheritanceMap == null) {
      // sanity check - this could be null if the caller does not set a group inheritance map first
      throw new IllegalStateException("ConstraintFinderImpl not initialized properly. A " +
          "GroupInheritanceMap is required by GWT to properly find all constraint descriptors.");
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
    this.groups = new ArrayList<Class<?>>(groups.length);
    Collections.addAll(this.groups, groups);
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
      // TODO(idol) The group sequence ordering will play a part here
      Set<Class<?>> extendedGroups = groupInheritanceMap.findAllExtendedGroups(groups);
      for (Class<?> group : extendedGroups) {
        addMatchingDescriptorsForGroup(group, matchingDescriptors);
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
