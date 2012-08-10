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
package com.google.gwt.validation.client.impl.metadata;

import junit.framework.TestCase;

import java.util.Set;
import java.util.HashSet;

import javax.validation.groups.Default;

/**
 * Test case for {@link ValidationGroupsMetadata}.
 */
public class ValidationGroupsMetadataTest extends TestCase {

  private ValidationGroupsMetadata createWithTestGroups() {
    return ValidationGroupsMetadata.builder()
        .addGroup(Part1.class, MiniPart.class)
        .addGroup(Part2.class)
        .addGroup(Big.class, Part1.class, Part2.class)
        .addGroup(MiniPart.class, SuperSmall.class)
        .addGroup(SuperSmall.class)
        .build();
  }

  public void testDefaultGroupExists() {
    assertTrue(ValidationGroupsMetadata.builder().build().containsGroup(Default.class));
  }
  
  public void testFindAllExtendedGroups() {
    // should get all of the groups and all of their parents recursively
    ValidationGroupsMetadata groupsMetadata = createWithTestGroups();
    Set<Class<?>> baseGroups = new HashSet<Class<?>>();
    baseGroups.add(Part1.class);
    baseGroups.add(Part2.class);
    Set<Class<?>> desired = new HashSet<Class<?>>();
    desired.add(Part1.class);
    desired.add(Part2.class);
    desired.add(MiniPart.class);
    desired.add(SuperSmall.class);
    assertEquals(desired, groupsMetadata.findAllExtendedGroups(baseGroups));
  }

  public void testFindingExtendedGroupsThrowsExceptionWhenUnknown() {
    // should throw exception when the group has not been added to the map
    ValidationGroupsMetadata groupsMetadata = ValidationGroupsMetadata.builder().build();
    assertFalse(groupsMetadata.containsGroup(MiniPart.class));
    try {
      Set<Class<?>> miniPart = new HashSet<Class<?>>();
      miniPart.add(MiniPart.class);
      groupsMetadata.findAllExtendedGroups(miniPart);
      fail("Expected an " + IllegalArgumentException.class);
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  public void testGetAllGroupsAndSequences() {
    // should return all groups and their parents recursively as well as sequence groups
    ValidationGroupsMetadata groupsMetadata = createWithTestGroups();
    Set<Class<?>> desired = new HashSet<Class<?>>();
    desired.add(Default.class);
    desired.add(Part1.class);
    desired.add(Part2.class);
    desired.add(MiniPart.class);
    desired.add(SuperSmall.class);
    desired.add(Big.class);
    assertEquals(desired, groupsMetadata.getAllGroupsAndSequences());
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
