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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.validation.GroupDefinitionException;
import javax.validation.groups.Default;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * An instance of {@code GroupChain} defines the group order during one full validation call.
 * <p>
 * Modified from the Hibernate validator for use with GWT.
 */
public final class GroupChain {

  /**
   * The list of single groups to be used this validation.
   */
  private final List<Group> groupList = new ArrayList<Group>();

  /**
   * The different sequences for this validation. The map contains the list of groups mapped to
   * their sequence name.
   */
  private final Map<Class<?>, List<Group>> sequenceMap = new HashMap<Class<?>, List<Group>>();

  public void checkDefaultGroupSequenceIsExpandable(List<Class<?>> defaultGroupSequence)
      throws GroupDefinitionException {
    for (Map.Entry<Class<?>, List<Group>> entry : sequenceMap.entrySet()) {
      Class<?> sequence = entry.getKey();
      List<Group> groups = entry.getValue();
      List<Group> defaultGroupList = buildTempGroupList(defaultGroupSequence, sequence);
      int defaultGroupIndex = containsDefaultGroupAtIndex(sequence, groups);
      if (defaultGroupIndex != -1) {
        ensureDefaultGroupSequenceIsExpandable(groups, defaultGroupList, defaultGroupIndex);
      }
    }
  }

  public Collection<Group> getAllGroups() {
    return groupList;
  }

  public Iterator<Group> getGroupIterator() {
    return groupList.iterator();
  }

  public Iterator<List<Group>> getSequenceIterator() {
    return sequenceMap.values().iterator();
  }

  public void insertGroup(Group group) {
    if (!groupList.contains(group)) {
      groupList.add(group);
    }
  }

  public void insertSequence(List<Group> groups) {
    if (groups != null && groups.size() != 0) {
      if (!sequenceMap.containsValue(groups)) {
        sequenceMap.put(groups.get(0).getSequence(), groups);
      }
    }
  }

  @Override
  public String toString() {
    return "GroupChain{" +
        "groupList=" + groupList +
        ", sequenceMap=" + sequenceMap +
        "}";
  }

  private List<Group> buildTempGroupList(List<Class<?>> defaultGroupSequence, Class<?> sequence) {
    List<Group> groups = new ArrayList<Group>();
    for (Class<?> clazz : defaultGroupSequence) {
      Group g = new Group(clazz, sequence);
      groups.add(g);
    }
    return groups;
  }

  private int containsDefaultGroupAtIndex(Class<?> sequence, List<Group> groupList) {
    Group defaultGroup = new Group(Default.class, sequence);
    return groupList.indexOf(defaultGroup);
  }

  private void ensureDefaultGroupSequenceIsExpandable(List<Group> groupList,
      List<Group> defaultGroupList, int defaultGroupIndex) throws GroupDefinitionException {
    for (int i = 0; i < defaultGroupList.size(); i++) {
      Group group = defaultGroupList.get(i);
      if (group.getGroup().equals(Default.class)) {
        // we don't have to consider the default group since it is the one we want to replace
        continue;
      }
      // check whether the sequence contains group of the default group sequence
      int index = groupList.indexOf(group);
      if (index == -1) {
        continue; // if the group is not in the sequence we can continue
      }

      if ((i == 0 && index == defaultGroupIndex - 1)
          || (i == defaultGroupList.size() - 1 && index == defaultGroupIndex + 1)) {
        // if we are at the beginning or end of he defaultGroupSequence and the matches are either
        // directly before resp after we can continue as well, since we basically have two groups
        continue;
      }
      throw new GroupDefinitionException("Unable to expand default group list " + defaultGroupList +
          " into sequence " + groupList);
    }
  }
}
