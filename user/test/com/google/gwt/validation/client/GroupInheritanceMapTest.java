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

import junit.framework.TestCase;

import java.util.Set;
import java.util.HashSet;

import javax.validation.groups.Default;

/**
 * Test case for {@link GroupInheritanceMap}.
 */
public class GroupInheritanceMapTest extends TestCase {
  GroupInheritanceMap groupInheritanceMap = new GroupInheritanceMap();

  private void addSomeTestGroups() {
    Set<Class<?>> part1Parents = new HashSet<Class<?>>();
    part1Parents.add(MiniPart.class);
    groupInheritanceMap.addGroup(Part1.class, part1Parents);
    groupInheritanceMap.addGroup(Part2.class);
    Set<Class<?>> bigParents = new HashSet<Class<?>>();
    bigParents.add(Part1.class);
    bigParents.add(Part2.class);
    groupInheritanceMap.addGroup(Big.class, bigParents);
    Set<Class<?>> miniPartParents = new HashSet<Class<?>>();
    miniPartParents.add(SuperSmall.class);
    groupInheritanceMap.addGroup(MiniPart.class, miniPartParents);
    groupInheritanceMap.addGroup(SuperSmall.class);
  }

  public void testDefaultGroupExists() {
    assertTrue(groupInheritanceMap.containsGroup(Default.class));
  }

  public void testAddingNullThrowsException() {
    // should throw exception when parents is null
    try {
      groupInheritanceMap.addGroup(Big.class, null);
      fail("Expected an " + IllegalArgumentException.class);
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }
  
  public void testFindAllExtendedGroups() {
    // should get all of the groups and all of their parents recursively
    addSomeTestGroups();
    Set<Class<?>> baseGroups = new HashSet<Class<?>>();
    baseGroups.add(Part1.class);
    baseGroups.add(Part2.class);
    Set<Class<?>> desired = new HashSet<Class<?>>();
    desired.add(Part1.class);
    desired.add(Part2.class);
    desired.add(MiniPart.class);
    desired.add(SuperSmall.class);
    assertEquals(desired, groupInheritanceMap.findAllExtendedGroups(baseGroups));
  }

  public void testFindingExtendedGroupsThrowsExceptionWhenUnknown() {
    // should throw exception when the group has not been added to the map
    assertFalse(groupInheritanceMap.containsGroup(MiniPart.class));
    try {
      Set<Class<?>> miniPart = new HashSet<Class<?>>();
      miniPart.add(MiniPart.class);
      groupInheritanceMap.findAllExtendedGroups(miniPart);
      fail("Expected an " + IllegalArgumentException.class);
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  public void testGetAllGroups() {
    // should return all groups and their parents recursively
    addSomeTestGroups();
    Set<Class<?>> desired = new HashSet<Class<?>>();
    desired.add(Default.class);
    desired.add(Part1.class);
    desired.add(Part2.class);
    desired.add(MiniPart.class);
    desired.add(SuperSmall.class);
    desired.add(Big.class);
    assertEquals(desired, groupInheritanceMap.getAllGroups());
  }

  private interface Part1 extends MiniPart {
  }
  
  private interface MiniPart extends SuperSmall {
  }
  
  private interface SuperSmall {
  }
  
  private interface Part2 {
  }
  
  private interface Big extends Part1, Part2 {
  }
}
