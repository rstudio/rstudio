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
package com.google.gwt.validation.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.GroupDefinitionException;
import javax.validation.ValidationException;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Helper class used to resolve groups and sequences into a single chain of groups which can then be validated.
 * <p>
 * Modified from the Hibernate validator for use with GWT.
 */
public class GroupChainGenerator {
  private final ValidationGroupsMetadata validationGroupsMetadata;

  private final Map<Class<?>, List<Group>> resolvedSequences = new HashMap<Class<?>, List<Group>>();

  public GroupChainGenerator(ValidationGroupsMetadata validationGroupsMetadata) {
    this.validationGroupsMetadata = validationGroupsMetadata;
  }

  /**
   * Generates a chain of groups to be validated given the specified validation groups.
   *
   * @param groups The groups specified at the validation call.
   *
   * @return an instance of {@code GroupChain} defining the order in which validation has to occur.
   */
  public GroupChain getGroupChainFor(Collection<Class<?>> groups) {
    if (groups == null || groups.size() == 0) {
      throw new IllegalArgumentException("At least one group has to be specified.");
    }

    for (Class<?> clazz : groups) {
      if (!validationGroupsMetadata.containsGroup(clazz)
          && !validationGroupsMetadata.isSeqeuence(clazz)) {
        throw new ValidationException("The class " + clazz + " is not a valid group or sequence.");
      }
    }

    GroupChain chain = new GroupChain();
    for (Class<?> clazz : groups) {
      if (isGroupSequence(clazz)) {
        insertSequence(clazz, chain);
      }
      else {
        Group group = new Group(clazz);
        chain.insertGroup(group);
        insertInheritedGroups(clazz, chain);
      }
    }

    return chain;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("GroupChainGenerator");
    sb.append("{resolvedSequences=").append(resolvedSequences);
    sb.append('}');
    return sb.toString();
  }

  private void addGroups(List<Group> resolvedGroupSequence, List<Group> groups) {
    for (Group tmpGroup : groups) {
      if (resolvedGroupSequence.contains(tmpGroup)
          && resolvedGroupSequence.indexOf(tmpGroup) < resolvedGroupSequence.size() - 1) {
        throw new GroupDefinitionException("Unable to expand group sequence.");
      }
      resolvedGroupSequence.add(tmpGroup);
    }
  }

  private void addInheritedGroups(Group group, List<Group> expandedGroups) {
    Set<Class<?>> inheritedGroups = validationGroupsMetadata.getParentsOfGroup(group.getGroup());
    if (inheritedGroups != null) {
      for (Class<?> inheritedGroup : inheritedGroups) {
        if (isGroupSequence(inheritedGroup)) {
          throw new GroupDefinitionException("Sequence definitions are not allowed as composing " +
              "parts of a sequence.");
        }
        Group g = new Group(inheritedGroup, group.getSequence());
        expandedGroups.add(g);
        addInheritedGroups(g, expandedGroups);
      }
    }
  }

  private List<Group> expandInhertitedGroups(List<Group> sequence) {
    List<Group> expandedGroup = new ArrayList<Group>();
    for (Group group : sequence) {
      expandedGroup.add(group);
      addInheritedGroups(group, expandedGroup);
    }
    return expandedGroup;
  }

  /**
   * Recursively add inherited groups into the group chain.
   *
   * @param clazz The group interface
   * @param chain The group chain we are currently building.
   */
  private void insertInheritedGroups(Class<?> clazz, GroupChain chain) {
    for (Class<?> inheritedGroup : validationGroupsMetadata.getParentsOfGroup(clazz)) {
      Group group = new Group(inheritedGroup);
      chain.insertGroup(group);
      insertInheritedGroups(inheritedGroup, chain);
    }
  }

  private void insertSequence(Class<?> clazz, GroupChain chain) {
    List<Group> sequence;
    if (resolvedSequences.containsKey(clazz)) {
      sequence = resolvedSequences.get(clazz);
    } else {
      sequence = resolveSequence(clazz, new ArrayList<Class<?>>());
      // we expand the inherited groups only after we determined whether the sequence is expandable
      sequence = expandInhertitedGroups(sequence);
    }
    chain.insertSequence(sequence);
  }

  private boolean isGroupSequence(Class<?> clazz) {
    return validationGroupsMetadata.isSeqeuence(clazz);
  }

  private List<Group> resolveSequence(Class<?> group, List<Class<?>> processedSequences) {
    if (processedSequences.contains(group)) {
      throw new GroupDefinitionException("Cyclic dependency in groups definition");
    } else {
      processedSequences.add(group);
    }
    List<Group> resolvedGroupSequence = new ArrayList<Group>();
    List<Class<?>> sequenceList = validationGroupsMetadata.getSequenceList(group);
    for (Class<?> clazz : sequenceList ) {
      if (isGroupSequence(clazz)) {
        List<Group> tmpSequence = resolveSequence(clazz, processedSequences);
        addGroups(resolvedGroupSequence, tmpSequence);
      }
      else {
        List<Group> list = new ArrayList<Group>();
        list.add(new Group(clazz, group));
        addGroups(resolvedGroupSequence, list);
      }
    }
    resolvedSequences.put(group, resolvedGroupSequence);
    return resolvedGroupSequence;
  }
}
