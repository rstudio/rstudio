/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.util;

import com.google.gwt.util.tools.Utility;

import junit.framework.TestCase;

import java.net.URL;

/** Pure junit test of Utility functionality*/
public class UtilityTest extends TestCase {

  /** Tests that URLAsChars correctly processes unicode*/
  public void testUnicode() {
    URL r = this.getClass().getResource("unicodeTest.txt");
    assertNotNull(r);

    char[] x = Util.readURLAsChars(r);
    assertEquals(2,x.length);
    char a = '\u4F60';
    assertEquals(x[0],a);
    assertEquals(x[1],'\u597D');
  }


  public void testVersionNumberComparisons() {
    assertTrue(Utility.versionCompare("1.4.3.22", "1.04.3.22") == 0);
    assertTrue(Utility.versionCompare("1.4.3.22.1", "1.4.3.22") > 0);
    assertTrue(Utility.versionCompare("1.4.3.22.1", "1.4.3.32") < 0);
    assertTrue(Utility.versionCompare("1.4.3.22", "1.4.3.22.1") < 0);
    assertTrue(Utility.versionCompare("1.4.3.22.1", "1.4.3.22.2") < 0);

    assertTrue(Utility.versionCompare("1.4.3.22.1_b4", "1.4.3.22_b2") > 0);
    assertTrue(Utility.versionCompare("1.4.3.22_b11", "01.04.3.22_b1") > 0);

    try {
      Utility.versionCompare("1.4.3.22.1.dodo", "1.4.3.22.1");
      fail("Should have trown a IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
    try {
      Utility.versionCompare("1.4.3.22.1", "1.4.3.22.1.dodo");
      fail("Should have trown a IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
  }
}
