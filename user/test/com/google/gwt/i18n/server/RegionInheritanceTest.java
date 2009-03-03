/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.i18n.server;

import junit.framework.TestCase;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test of RegionInheritance.
 */
public class RegionInheritanceTest extends TestCase {

  /**
   * Test method for {@link com.google.gwt.i18n.server.RegionInheritance#findCommonParent(java.lang.String, java.lang.String)}.
   */
  public void testFindCommonParent() {
    assertEquals("001", RegionInheritance.findCommonParent("US", "KZ"));
    assertEquals("019", RegionInheritance.findCommonParent("US", "MX"));
    assertEquals("021", RegionInheritance.findCommonParent("US", "CA"));
    assertEquals("419", RegionInheritance.findCommonParent("BR", "MX"));
    assertEquals("005", RegionInheritance.findCommonParent("BR", "AR"));
    assertNull(RegionInheritance.findCommonParent("BR", null));
    assertNull(RegionInheritance.findCommonParent(null, "BR"));
    assertNull(RegionInheritance.findCommonParent(null, null));
    assertNull(RegionInheritance.findCommonParent("US", "XQZ"));
  }

  /**
   * Test method for {@link com.google.gwt.i18n.server.RegionInheritance#getAllAncestors(java.lang.String)}.
   */
  public void testGetAllAncestors() {
    List<String> ancestors = RegionInheritance.getAllAncestors("US");
    assertEquals(3, ancestors.size());
    assertTrue("Should have contained 001", ancestors.contains("001"));
    assertTrue("Should have contained 021", ancestors.contains("021"));
    assertTrue("Should have contained 019", ancestors.contains("019"));
  }

  /**
   * Test method for {@link com.google.gwt.i18n.server.RegionInheritance#getImmediateParents(java.lang.String)}.
   */
  public void testGetImmediateParents() {
    // TODO(jat): adjust if the inheritance data is modified to allow multiple
    // parents.
    Set<String> parents = RegionInheritance.getImmediateParents("US");
    assertEquals(1, parents.size());
    assertEquals("021", parents.iterator().next());
    parents = RegionInheritance.getImmediateParents("BO");
    assertEquals(1, parents.size());
    assertEquals("005", parents.iterator().next());
    parents = RegionInheritance.getImmediateParents("005");
    assertEquals(1, parents.size());
    assertEquals("419", parents.iterator().next());
  }

  /**
   * Test method for {@link com.google.gwt.i18n.server.RegionInheritance#isParentOf(java.lang.String, java.lang.String)}.
   */
  public void testIsParentOf() {
    assertTrue(RegionInheritance.isParentOf(null, null));
    assertFalse(RegionInheritance.isParentOf(null, "US"));
    assertFalse(RegionInheritance.isParentOf("US", null));
    assertTrue(RegionInheritance.isParentOf("US", "US"));
    assertTrue(RegionInheritance.isParentOf("019", "US"));
    assertFalse(RegionInheritance.isParentOf("419", "US"));
    assertTrue(RegionInheritance.isParentOf("419", "MX"));
    assertTrue(RegionInheritance.isParentOf("001", "US"));
    assertFalse(RegionInheritance.isParentOf("US", "001"));
  }

  /**
   * Verifies some basic assumptions about the map.
   */
  public void testMap() {
    Map<String, String> map = RegionInheritance.getInheritanceMap();
    Set<String> regions = map.keySet();
    for (String region : regions) {
      if (region.length() == 2) {
        if (!Character.isLetter(region.charAt(0))
            || !Character.isLetter(region.charAt(1))) {
          fail("2-character region names should be letters");
        }
      } else if (region.length() == 3) {
        if (!Character.isDigit(region.charAt(0))
            || !Character.isDigit(region.charAt(1))
            || !Character.isDigit(region.charAt(2))) {
          fail("3-character region names should be numeric");
        }
      } else {
        fail("Regions in parent map should be 2 letters or 3 digits");
      }
      checkUltimateParent(map, region, "001");
    }
  }

  private void checkUltimateParent(Map<String, String> map, String region,
      String match) {
    String origRegion = region;
    while (region != null) {
      if (region.equals(match)) {
        return;
      }
      region = map.get(region);
    }
    fail("Ultimate parent of " + origRegion + " not " + match);
  }
}
