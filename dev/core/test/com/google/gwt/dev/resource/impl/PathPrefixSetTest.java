/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.resource.impl;

import junit.framework.TestCase;

import java.util.Locale;

/**
 * Tests the trie and filtering behavior of path prefix set.
 */
public class PathPrefixSetTest extends TestCase {

  public void testEmptyPrefixSet() {
    PathPrefixSet pps = new PathPrefixSet();
    assertNull(pps.includesResource("com/google/gwt/user/client/Command.java"));
  }

  public void testNonOverlappingPrefixesEmptyFilter() {
    /*
     * Test with null filters to ensure nothing gets filtered out.
     */
    PathPrefixSet pps = new PathPrefixSet();
    PathPrefix pp1 = new PathPrefix("com/google/gwt/user/client/", null);
    PathPrefix pp2 = new PathPrefix("com/google/gwt/i18n/client/", null);
    PathPrefix pp3 = new PathPrefix("com/google/gwt/dom/client/", null);
    pps.add(pp1);
    pps.add(pp2);
    pps.add(pp3);

    assertTrue(pps.includesDirectory("com/"));
    assertTrue(pps.includesDirectory("com/google/"));
    assertTrue(pps.includesDirectory("com/google/gwt/"));
    assertTrue(pps.includesDirectory("com/google/gwt/user/"));
    assertTrue(pps.includesDirectory("com/google/gwt/user/client/"));
    assertTrue(pps.includesDirectory("com/google/gwt/user/client/ui/"));

    assertFalse(pps.includesDirectory("org/"));
    assertFalse(pps.includesDirectory("org/example/"));
    assertFalse(pps.includesDirectory("com/google/gwt/user/server/"));
    assertFalse(pps.includesDirectory("com/google/gwt/xml/client/"));

    assertEquals(pp1,
        pps.includesResource("com/google/gwt/user/client/Command.java"));
    assertEquals(pp1,
        pps.includesResource("com/google/gwt/user/client/Timer.java"));
    assertEquals(pp2,
        pps.includesResource("com/google/gwt/i18n/client/Messages.java"));
    assertEquals(pp3,
        pps.includesResource("com/google/gwt/dom/client/DivElement.java"));

    assertNull(pps.includesResource("com/google/gwt/user/rebind/rpc/ServiceInterfaceProxyGenerator.java"));
    assertNull(pps.includesResource("com/google/gwt/sample/hello/client/Hello.java"));
    assertNull(pps.includesResource("com/google/gwt/user/public/clear.cache.gif"));
  }

  public void testNonOverlappingPrefixesNonEmptyFilter() {
    /*
     * Test with a real filter to ensure it does have an effect.
     */
    PathPrefixSet pps = new PathPrefixSet();
    ResourceFilter allowsGifs = new ResourceFilter() {
      public boolean allows(String path) {
        return path.toLowerCase(Locale.ENGLISH).endsWith(".gif");
      }
    };

    PathPrefix pp1 = new PathPrefix("com/google/gwt/user/public/", allowsGifs);
    PathPrefix pp2 = new PathPrefix("com/google/gwt/sample/mail/public/",
        allowsGifs);
    pps.add(pp1);
    pps.add(pp2);

    // Correct prefix, and filter should allow .
    assertEquals(pp1,
        pps.includesResource("com/google/gwt/user/public/clear.cache.gif"));
    assertEquals(pp2,
        pps.includesResource("com/google/gwt/sample/mail/public/inboxIcon.gif"));

    // Correct prefix, but filter should exclude.
    assertNull(pps.includesResource("com/google/gwt/user/public/README.txt"));
    assertNull(pps.includesResource("com/google/gwt/sample/mail/public/README.txt"));

    // Wrong prefix, and filter would have excluded.
    assertNull(pps.includesResource("com/google/gwt/user/client/Command.java"));
    assertNull(pps.includesResource("com/google/gwt/user/rebind/rpc/ServiceInterfaceProxyGenerator.java"));

    // Wrong prefix, but filter would have allowed it.
    assertNull(pps.includesResource("com/google/gwt/i18n/public/flags.gif"));
  }

  public void testOverlappingPrefixesEmptyFilter() {
    /*
     * Without a filter.
     */
    PathPrefixSet pps = new PathPrefixSet();
    PathPrefix pp1 = new PathPrefix("a/b/", null);
    PathPrefix pp2 = new PathPrefix("a/", null);
    PathPrefix pp3 = new PathPrefix("", null);
    PathPrefix pp4 = new PathPrefix("a/b/c/", null);
    PathPrefix pp5 = new PathPrefix("a/", null);
    pps.add(pp1);
    pps.add(pp2);
    pps.add(pp3);
    pps.add(pp4);
    // pp5 now overrides pp2
    pps.add(pp5);

    assertEquals(pp3, pps.includesResource("W.java"));
    assertEquals(pp5, pps.includesResource("a/X.java"));
    assertEquals(pp1, pps.includesResource("a/b/Y.java"));
    assertEquals(pp4, pps.includesResource("a/b/c/Z.java"));
    assertEquals(pp4, pps.includesResource("a/b/c/d/V.java"));
  }

  public void testOverlappingPrefixesNonEmptyFilter() {
    /*
     * Ensure the right filter applies.
     */
    PathPrefixSet pps = new PathPrefixSet();
    PathPrefix pp1 = new PathPrefix("", null);
    PathPrefix pp2 = new PathPrefix("a/", null);
    PathPrefix pp3 = new PathPrefix("a/b/", new ResourceFilter() {
      public boolean allows(String path) {
        // Disallow anything ending with "FILTERMEOUT".
        return !path.endsWith("FILTERMEOUT");
      }
    });
    PathPrefix pp4 = new PathPrefix("a/b/c/", null);
    PathPrefix pp5 = new PathPrefix("a/", new ResourceFilter() {
      public boolean allows(String path) {
        return !path.endsWith("X.java");
      }
    });
    pps.add(pp1);
    pps.add(pp2);
    pps.add(pp3);
    pps.add(pp4);
    pps.add(pp5);

    assertEquals(pp1, pps.includesResource("W.java"));

    // see TODO in the implementation note for PathPrefixSet.java
    // assertEquals(pp2, pps.includesResource("a/X.java"));
    assertEquals(pp5, pps.includesResource("a/Y.java"));
    assertEquals(pp3, pps.includesResource("a/b/Y.java"));
    // This should be gone, since it is found in b.
    assertNull(pps.includesResource("a/b/FILTERMEOUT"));
    /*
     * This should not be gone, because it is using c's (null) filter instead of
     * b's. The logic here is that the prefix including c is more specific and
     * seemed to want c's resources to be included.
     */
    assertEquals(pp4, pps.includesResource("a/b/c/DONT_FILTERMEOUT"));
    assertEquals(pp4, pps.includesResource("a/b/c/Z.java"));
    assertEquals(pp4, pps.includesResource("a/b/c/d/V.java"));
  }

  /**
   * In essence, this tests support for the default package in Java.
   */
  public void testZeroLengthPrefixEmptyFilter() {
    /*
     * Without a filter.
     */
    PathPrefixSet pps = new PathPrefixSet();
    PathPrefix pp1 = new PathPrefix("", null);
    pps.add(pp1);

    assertEquals(pp1, pps.includesResource("W.java"));
    assertEquals(pp1, pps.includesResource("a/X.java"));
    assertEquals(pp1, pps.includesResource("a/b/Y.java"));
    assertEquals(pp1, pps.includesResource("a/b/c/Z.java"));
  }

  public void testZeroLengthPrefixNonEmptyFilter() {
    /*
     * With a filter.
     */
    PathPrefixSet pps = new PathPrefixSet();
    PathPrefix pp1 = new PathPrefix("", new ResourceFilter() {
      public boolean allows(String path) {
        return path.endsWith("Y.java");
      }
    });
    pps.add(pp1);

    assertNull(pps.includesResource("W.java"));
    assertNull(pps.includesResource("a/X.java"));
    assertEquals(pp1, pps.includesResource("a/b/Y.java"));
    assertNull(pps.includesResource("a/b/c/Z.java"));
  }
}
