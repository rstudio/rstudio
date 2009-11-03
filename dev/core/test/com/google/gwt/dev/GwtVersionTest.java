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
package com.google.gwt.dev;

import junit.framework.TestCase;

/**
 * Tests GwtVersion.
 */
public class GwtVersionTest extends TestCase {

  /**
   * Check for compatibility between compareTo, equals, and hashCode.
   */
  public void testCompareEqualsHashCode() {
    checkCompareEqualsHashCode("0.0.0", "0.0.0");
    checkCompareEqualsHashCode("0.0.0", "0");
    checkCompareEqualsHashCode("1.2.3", "001.002.003");
    checkCompareEqualsHashCode("1.2.3", "001.002.004");
    checkCompareEqualsHashCode("1.2.4", "001.002.003");
    checkCompareEqualsHashCode("1.2.4-ms1", "1.2.4-ms1");
    checkCompareEqualsHashCode("1.2.4-ms2", "1.2.4-ms2");
    checkCompareEqualsHashCode("1.2.4-ms2", "1.2.4-rc1");
  }
  
  /**
   * Test that GwtVersion.compareTo produced expected results.
   */
  public void testCompareTo() {
    GwtVersion v1 = new GwtVersion("0.0.0");
    assertEquals(0, v1.compareTo(v1));
    GwtVersion v2 = new GwtVersion("0.0.0");
    assertEquals(0, v1.compareTo(v2));
    assertEquals(0, v2.compareTo(v1));
    v2 = new GwtVersion("0.0.0b");
    assertTrue(v1.compareTo(v2) > 0);
    assertTrue(v2.compareTo(v1) < 0);
    v1 = new GwtVersion("0.0.0c");
    v2 = new GwtVersion("0.0.0b");
    assertEquals(0, v1.compareTo(v2));
    assertEquals(0, v2.compareTo(v1));
    v1 = new GwtVersion("1.9.41");
    v2 = new GwtVersion("1.11.12");
    assertTrue(v1.compareTo(v2) < 0);
    assertTrue(v2.compareTo(v1) > 0);
    v1 = new GwtVersion("1.9.41");
    v2 = new GwtVersion("1.4.12");
    assertTrue(v1.compareTo(v2) > 0);
    assertTrue(v2.compareTo(v1) < 0);
    v1 = new GwtVersion("1.9.41");
    v2 = new GwtVersion("2.0.0-ms1");
    assertTrue(v1.compareTo(v2) < 0);
    assertTrue(v2.compareTo(v1) > 0);
    v1 = new GwtVersion("2.0.0-ms2");
    v2 = new GwtVersion("2.0.0-rc1");
    assertEquals(0, v1.compareTo(v2));
    assertEquals(0, v2.compareTo(v1));
    v1 = new GwtVersion("001.002.099");
    v2 = new GwtVersion("1.2.99");
    assertEquals(0, v1.compareTo(v2));
    assertEquals(0, v2.compareTo(v1));
  }

  /**
   * Test that GwtVersion.compareTo produced expected results.
   */
  public void testEquals() {
    GwtVersion v1 = new GwtVersion("0.0.0");
    assertEquals(v1, v1);
    GwtVersion v2 = new GwtVersion("0.0.0");
    assertEquals(v1, v2);
    assertEquals(v2, v1);
    v2 = new GwtVersion("");
    assertEquals(v1, v2);
    assertEquals(v2, v1);
    v2 = new GwtVersion("1.2.3");
    assertFalse(v1.equals(v2));
    assertFalse(v2.equals(v1));
  }

  /**
   * Test that various versions are properly detected as to whether or not they
   * are "no-nag" versions. 
   */
  public void testIsNoNagVersion() {
    GwtVersion version = new GwtVersion("0.0.0");
    assertFalse(version.isNoNagVersion());
    version = new GwtVersion("0.0.999");
    assertTrue(version.isNoNagVersion());
    version = new GwtVersion("2.0.999");
    assertTrue(version.isNoNagVersion());
    version = new GwtVersion("2.0.999-rc1");
    assertTrue(version.isNoNagVersion());
    version = new GwtVersion("2.0.999-ms2");
    assertTrue(version.isNoNagVersion());
    version = new GwtVersion("0.999.0");
    assertFalse(version.isNoNagVersion());
    version = new GwtVersion("2.999.0-rc1");
    assertFalse(version.isNoNagVersion());
  }

  /**
   * Verify that bogus version numbers don't fail.
   */
  public void testParseBad() {
    checkAllZerosVersion("", null);
    checkAllZerosVersion("bogus", null); // we skip leading garbage
    checkAllZerosVersion("0.x.x", "x.x");
    GwtVersion version = new GwtVersion("1.x.x");
    int[] components = version.getComponents();
    assertEquals(3, components.length);
    assertEquals(1, components[0]);
    assertEquals(0, components[1]);
    assertEquals(0, components[2]);
    assertEquals("x.x", version.getSuffix());
    version = new GwtVersion("1.2.x");
    components = version.getComponents();
    assertEquals(3, components.length);
    assertEquals(1, components[0]);
    assertEquals(2, components[1]);
    assertEquals(0, components[2]);
    assertEquals("x", version.getSuffix());
    version = new GwtVersion("1.2.3x");
    components = version.getComponents();
    assertEquals(3, components.length);
    assertEquals(1, components[0]);
    assertEquals(2, components[1]);
    assertEquals(3, components[2]);
    assertEquals("x", version.getSuffix());
  }

  /**
   * Tests parsing various version numbers.
   */
  public void testParseBasic() {
    GwtVersion version = new GwtVersion("1.2.3");
    int[] components = version.getComponents();
    assertEquals(3, components.length);
    assertEquals(1, components[0]);
    assertEquals(2, components[1]);
    assertEquals(3, components[2]);
    assertNull(version.getSuffix());
    version = new GwtVersion("1.2.3-ms1");
    components = version.getComponents();
    assertEquals(3, components.length);
    assertEquals(1, components[0]);
    assertEquals(2, components[1]);
    assertEquals(3, components[2]);
    assertEquals("-ms1", version.getSuffix());
    version = new GwtVersion("1.2.3-rc2");
    components = version.getComponents();
    assertEquals(3, components.length);
    assertEquals(1, components[0]);
    assertEquals(2, components[1]);
    assertEquals(3, components[2]);
    assertEquals("-rc2", version.getSuffix());
    version = new GwtVersion("1.2.3-RC1");
    components = version.getComponents();
    assertEquals(3, components.length);
    assertEquals(1, components[0]);
    assertEquals(2, components[1]);
    assertEquals(3, components[2]);
    assertEquals("-RC1", version.getSuffix());
  }

  /**
   * Tests various ways you can get a version number of 0.0.0.
   */
  public void testParseZeros() {
    checkAllZerosVersion("0.0.0", null);
    checkAllZerosVersion("0.0", null);
    checkAllZerosVersion("0", null);
    checkAllZerosVersion("", null);
    checkAllZerosVersion(null, null);
    checkAllZerosVersion("foo0.0.0", null);
    checkAllZerosVersion("foo0.0.0 bar", " bar");
  }
 
  /**
   * Test that GwtVersion.toString() returns expected results.
   */
  public void testToString() {
    String versionString = "0.0.0";
    GwtVersion version = new GwtVersion(versionString);
    assertEquals(versionString, version.toString());
    versionString = "0.0.0a";
    version = new GwtVersion(versionString);
    assertEquals(versionString, version.toString());
    versionString = "foo 0.0.0a";
    version = new GwtVersion(versionString);
    assertEquals("0.0.0a", version.toString());
    versionString = "1.2.3";
    version = new GwtVersion(versionString);
    assertEquals(versionString, version.toString());
    versionString = "1.2.3-rc1";
    version = new GwtVersion(versionString);
    assertEquals(versionString, version.toString());
    versionString = "1.2.3-ms2";
    version = new GwtVersion(versionString);
    assertEquals(versionString, version.toString());
  }

  /**
   * Verify that the version string is treated equivalently to 0.0.0.
   * 
   * @param versionString version number in string form
   * @param expectedSuffix expected suffix of the version
   */
  private void checkAllZerosVersion(String versionString,
      String expectedSuffix) {
    GwtVersion version = new GwtVersion(versionString);
    int[] components = version.getComponents();
    assertEquals(3, components.length);
    assertEquals(0, components[0]);
    assertEquals(0, components[1]);
    assertEquals(0, components[2]);
    assertEquals(expectedSuffix, version.getSuffix());
  }

  /**
   * Check that compareTo, equals, and hashCode are compatible for a pair of
   * versions.
   * 
   * @param v1String string format version number to test
   * @param v2String string format version number to test
   */
  private void checkCompareEqualsHashCode(String v1String, String v2String) {
    GwtVersion v1 = new GwtVersion(v1String);
    GwtVersion v2 = new GwtVersion(v2String);
    int h1 = v1.hashCode();
    int h2 = v2.hashCode();
    int c12 = v1.compareTo(v2);
    int c21 = v2.compareTo(v1);
    boolean e12 = v1.equals(v2);
    boolean e21 = v2.equals(v1);
    assertEquals("equals not symmetric", e12, e21);
    assertEquals("compareTo not symmetric", c12, -c21);
    assertEquals("compareTo/equals don't match", e12, c12 == 0);
    assertEquals("hashCode/equals don't match", e12, h1 == h2);
  }
}
