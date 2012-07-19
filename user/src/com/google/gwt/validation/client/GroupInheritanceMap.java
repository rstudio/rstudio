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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

import javax.validation.groups.Default;


/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Contains all the information known about the inheritance information for validation groups.
 */
public class GroupInheritanceMap {
  protected Map<Class<?>, Set<Class<?>>> groupInheritanceMap;

  /**
   * Creates an instance populated only with the {@link Default} group.
   */
  public GroupInheritanceMap() {
    groupInheritanceMap = new HashMap<Class<?>, Set<Class<?>>>();
    addGroup(Default.class);
  }

  /**
   * Adds a group to the inheritance map with no parents.
   * @param group The validation group to add.
   */
  public void addGroup(Class<?> group) {
    addGroup(group, new HashSet<Class<?>>(0));
  }

  /**
   * Adds a group and its parents to the inheritance map.
   * @param group The root validation group.
   * @param parents A set of validation groups which {@code group} extends. Must not be null.
   * @throws IllegalArgumentException If {@code parents} is null.
   */
  public void addGroup(Class<?> group, Set<Class<?>> parents) throws IllegalArgumentException {
    if (parents == null) {
      throw new IllegalArgumentException("The set of parents must not be null. Use an empty set" +
          " for a group that has no parents.");
    }
    groupInheritanceMap.put(group, parents);
  }

  /**
   * Checks if a given group has been added to the map.
   */
  public boolean containsGroup(Class<?> group) {
    return groupInheritanceMap.containsKey(group);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof GroupInheritanceMap)) {
      return false;
    }
    GroupInheritanceMap otherObj = (GroupInheritanceMap)other;
    return groupInheritanceMap.equals(otherObj.groupInheritanceMap);
  }

  /**
   * Finds all of the validation groups extended by an intial set of groups.
   * @param baseGroups The initial set of groups to find parents of. These groups must have been 
   * added to the inheritance map already.
   * @return A unified set of groups and their parents.
   * @throws IllegalArgumentException If an initial group has not been added to the map before
   * calling this method.
   */
  public Set<Class<?>> findAllExtendedGroups(Collection<Class<?>> baseGroups)
      throws IllegalArgumentException {
    Set<Class<?>> found = new HashSet<Class<?>>();
    Stack<Class<?>> remaining = new Stack<Class<?>>();
    // initialize
    for (Class<?> group : baseGroups) {
      if (!groupInheritanceMap.containsKey(group)) {
        throw new IllegalArgumentException("The collection of groups contains a group which" +
            " was not added to the map. Be sure to call addGroup() for all groups first.");
      }
      remaining.push(group);
    }
    // traverse
    Class<?> current;
    Set<Class<?>> superInterfaces;
    while (!remaining.isEmpty()) {
      current = remaining.pop();
      found.add(current);
      superInterfaces = groupInheritanceMap.get(current);
      for (Class<?> parent : superInterfaces) {
        if (!found.contains(parent)) {
          remaining.push(parent);
        }
      }
    }
    return found;
  }

  /**
   * Recursively gets all of the groups in the map (children and parents alike) in one flat set.
   */
  public Set<Class<?>> getAllGroups() {
    Set<Class<?>> allGroups = new HashSet<Class<?>>();
    for (Map.Entry<Class<?>, Set<Class<?>>> entry : groupInheritanceMap.entrySet()) {
      allGroups.add(entry.getKey());
      allGroups.addAll(entry.getValue());
    }
    return allGroups;
  }

  /**
   * If the group has been added to the map then its parent groups (of one level above) are
   * retrieved. Otherwise null is returned.
   * 
   * @see #containsGroup(Class)
   * @see #findAllExtendedGroups(Collection)
   */
  public Set<Class<?>> getParentsOfGroup(Class<?> group) {
    return groupInheritanceMap.get(group);
  }

  /**
   * Returns all of the groups added to the map (but not their parents).
   */
  public Set<Class<?>> getRootGroups() {
    return groupInheritanceMap.keySet();
  }

  @Override
  public int hashCode() {
    return groupInheritanceMap.hashCode();
  }

  public boolean isEmpty() {
    return groupInheritanceMap.isEmpty();
  }

  @Override
  public String toString() {
    return groupInheritanceMap.toString();
  }
}
