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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

  /**
   * Builder for {@link GroupInheritanceMap}
   */
  public static class Builder {
    Map<Class<?>, Set<Class<?>>> mapping;

    private Builder() {
      mapping = new HashMap<Class<?>, Set<Class<?>>>();
      addGroup(Default.class);
    }

    /**
     * Adds a group to the inheritance map. May optionally include parents of the group.
     * @param group The validation group to add.
     * @param parents A list of validation groups which {@code group} extends. Can be empty if the
     * group contains no parents.
     */
    public Builder addGroup(Class<?> group, Class<?>... parents) {
      mapping.put(group, new HashSet<Class<?>>(Arrays.asList(parents)));
      return this;
    }
    
    public GroupInheritanceMap build() {
      return new GroupInheritanceMap(mapping);
    }
  }

  /**
   * Creates a builder populated only with the {@link Default} group.
   */
  public static Builder builder() {
    return new Builder();
  }

  private final Map<Class<?>, Set<Class<?>>> mapping;

  private GroupInheritanceMap(Map<Class<?>, Set<Class<?>>> mapping) {
    this.mapping = Collections.unmodifiableMap(mapping);
  }

  /**
   * Checks if a given group has been added to the map.
   */
  public boolean containsGroup(Class<?> group) {
    return mapping.containsKey(group);
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
    return mapping.equals(otherObj.mapping);
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
      if (!mapping.containsKey(group)) {
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
      superInterfaces = mapping.get(current);
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
    for (Map.Entry<Class<?>, Set<Class<?>>> entry : mapping.entrySet()) {
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
    return mapping.get(group);
  }

  /**
   * Returns all of the groups added to the map (but not their parents).
   */
  public Set<Class<?>> getRootGroups() {
    return mapping.keySet();
  }

  @Override
  public int hashCode() {
    return mapping.hashCode();
  }

  public boolean isEmpty() {
    return mapping.isEmpty();
  }

  @Override
  public String toString() {
    return mapping.toString();
  }
}
