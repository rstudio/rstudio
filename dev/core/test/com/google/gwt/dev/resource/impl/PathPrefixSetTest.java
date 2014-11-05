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

  public void doTestEmptyPrefixSet(boolean mergePathPrefixes) {
    PathPrefixSet pps = new PathPrefixSet(mergePathPrefixes);
    assertNull(pps.includesResource("com/google/gwt/user/client/Command.java"));
  }

  public void doTestExcludes_DenyOverridesAllow(boolean mergePathPrefixes) {
    PathPrefixSet pathPrefixSet = new PathPrefixSet(mergePathPrefixes);

    String[] excludesDeny = new String[] {"a/b/FILTERMEOUT"};
    String[] excludesAllow = null;
    PathPrefix pathPrefixExcludesDeny =
        new PathPrefix("FooModule", "a/b/", null, false, excludesDeny);
    PathPrefix pathPrefixExcludesAllow =
        new PathPrefix("BarModule", "a/b/", null, false, excludesAllow);

    pathPrefixSet.add(pathPrefixExcludesDeny);
    pathPrefixSet.add(pathPrefixExcludesAllow);

    assertNull(pathPrefixSet.includesResource("a/b/FILTERMEOUT"));
  }

  public void doTestFilter_AllowOverridesDeny(boolean mergePathPrefixes) {
    PathPrefixSet pathPrefixSet = new PathPrefixSet(mergePathPrefixes);

    // "Includes" and "Skips" xml entries together become Filters.
    ResourceFilter filterDeny = new ResourceFilter() {
      @Override
      public boolean allows(String path) {
        return !path.endsWith("FILTERMEOUT");
      }
    };
    ResourceFilter filterAllow = null;
    PathPrefix pathPrefixFilterDeny = new PathPrefix("a/b/", filterDeny);
    PathPrefix pathPrefixFilterAllow = new PathPrefix("a/b/", filterAllow);

    pathPrefixSet.add(pathPrefixFilterDeny);
    pathPrefixSet.add(pathPrefixFilterAllow);

    assertNotNull(pathPrefixSet.includesResource("a/b/DONT_FILTERMEOUT"));
  }

  public void doTestMostSpecificFilterWins(boolean mergePathPrefixes) {
    PathPrefixSet pathPrefixSet = new PathPrefixSet(mergePathPrefixes);

    PathPrefix pathPrefixGeneralFilterDeny = new PathPrefix("a/b/", new ResourceFilter() {
        @Override
      public boolean allows(String path) {
        return !path.endsWith("FILTERMEOUT");
      }
    });
    PathPrefix pathPrefixSpecificFilterAllow = new PathPrefix("a/b/c/", null);

    pathPrefixSet.add(pathPrefixGeneralFilterDeny);
    pathPrefixSet.add(pathPrefixSpecificFilterAllow);

    assertNull(pathPrefixSet.includesResource("a/b/FILTERMEOUT"));
    assertEquals(pathPrefixSpecificFilterAllow, pathPrefixSet.includesResource(
        "a/b/c/DONT_FILTERMEOUT").getPathPrefix());
  }

  public void doTestNonOverlappingPrefixesEmptyFilter(boolean mergePathPrefixes) {
    /*
     * Test with null filters to ensure nothing gets filtered out.
     */
    PathPrefixSet pps = new PathPrefixSet(mergePathPrefixes);
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

    assertEquals(pp1, pps.includesResource(
        "com/google/gwt/user/client/Command.java").getPathPrefix());
    assertEquals(pp1, pps.includesResource(
        "com/google/gwt/user/client/Timer.java").getPathPrefix());
    assertEquals(pp2, pps.includesResource(
        "com/google/gwt/i18n/client/Messages.java").getPathPrefix());
    assertEquals(pp3, pps.includesResource(
        "com/google/gwt/dom/client/DivElement.java").getPathPrefix());

    assertNull(
        pps.includesResource("com/google/gwt/user/rebind/rpc/ServiceInterfaceProxyGenerator.java"));
    assertNull(pps.includesResource("com/google/gwt/sample/hello/client/Hello.java"));
    assertNull(pps.includesResource("com/google/gwt/user/public/clear.cache.gif"));
  }

  public void doTestNonOverlappingPrefixesNonEmptyFilter(boolean mergePathPrefixes) {
    /*
     * Test with a real filter to ensure it does have an effect.
     */
    PathPrefixSet pps = new PathPrefixSet(mergePathPrefixes);
    ResourceFilter allowsGifs = new ResourceFilter() {
      @Override
      public boolean allows(String path) {
        return path.toLowerCase(Locale.ROOT).endsWith(".gif");
      }
    };

    PathPrefix pp1 = new PathPrefix("com/google/gwt/user/public/", allowsGifs);
    PathPrefix pp2 = new PathPrefix("com/google/gwt/sample/mail/public/",
        allowsGifs);
    pps.add(pp1);
    pps.add(pp2);

    // Correct prefix, and filter should allow .
    assertEquals(pp1, pps.includesResource(
        "com/google/gwt/user/public/clear.cache.gif").getPathPrefix());
    assertEquals(pp2, pps.includesResource(
        "com/google/gwt/sample/mail/public/inboxIcon.gif").getPathPrefix());

    // Correct prefix, but filter should exclude.
    assertNull(pps.includesResource("com/google/gwt/user/public/README.txt"));
    assertNull(pps.includesResource("com/google/gwt/sample/mail/public/README.txt"));

    // Wrong prefix, and filter would have excluded.
    assertNull(pps.includesResource("com/google/gwt/user/client/Command.java"));
    assertNull(pps.includesResource("com/google/gwt/user/rebind/rpc/ServiceInterfaceProxyGenerator.java"));

    // Wrong prefix, but filter would have allowed it.
    assertNull(pps.includesResource("com/google/gwt/i18n/public/flags.gif"));
  }

  public void doTestOverlappingPrefixesEmptyFilter(boolean mergePathPrefixes) {
    /*
     * Without a filter.
     */
    PathPrefixSet pps = new PathPrefixSet(mergePathPrefixes);
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

    assertEquals(pp3, pps.includesResource("W.java").getPathPrefix());
    assertEquals(pp5, pps.includesResource("a/X.java").getPathPrefix());
    assertEquals(pp1, pps.includesResource("a/b/Y.java").getPathPrefix());
    assertEquals(pp4, pps.includesResource("a/b/c/Z.java").getPathPrefix());
    assertEquals(pp4, pps.includesResource("a/b/c/d/V.java").getPathPrefix());
  }

  public void doTestOverlappingPrefixesNonEmptyFilter(boolean mergePathPrefixes) {
    /*
     * Ensure the right filter applies.
     */
    PathPrefixSet pps = new PathPrefixSet(mergePathPrefixes);
    PathPrefix pp1 = new PathPrefix("", null);
    PathPrefix pp2 = new PathPrefix("a/", null);
    PathPrefix pp3 = new PathPrefix("a/b/", new ResourceFilter() {
      @Override
      public boolean allows(String path) {
        // Disallow anything ending with "FILTERMEOUT".
        return !path.endsWith("FILTERMEOUT");
      }
    });
    PathPrefix pp4 = new PathPrefix("a/b/c/", null);
    PathPrefix pp5 = new PathPrefix("a/", new ResourceFilter() {
      @Override
      public boolean allows(String path) {
        return !path.endsWith("X.java");
      }
    });
    pps.add(pp1);
    pps.add(pp2);
    pps.add(pp3);
    pps.add(pp4);
    pps.add(pp5);

    assertEquals(pp1, pps.includesResource("W.java").getPathPrefix());

    // see TODO in the implementation note for PathPrefixSet.java
    // assertEquals(pp2, pps.includesResource("a/X.java"));
    assertEquals(pp5, pps.includesResource("a/Y.java").getPathPrefix());
    assertEquals(pp3, pps.includesResource("a/b/Y.java").getPathPrefix());
    // This should be gone, since it is found in b.
    assertNull(pps.includesResource("a/b/FILTERMEOUT"));
    /*
     * This should not be gone, because it is using c's (null) filter instead of
     * b's. The logic here is that the prefix including c is more specific and
     * seemed to want c's resources to be included.
     */
    assertEquals(pp4,
        pps.includesResource("a/b/c/DONT_FILTERMEOUT").getPathPrefix());
    assertEquals(pp4, pps.includesResource("a/b/c/Z.java").getPathPrefix());
    assertEquals(pp4, pps.includesResource("a/b/c/d/V.java").getPathPrefix());
  }

  /**
   * In essence, this tests support for the default package in Java.
   */
  public void doTestZeroLengthPrefixEmptyFilter(boolean mergePathPrefixes) {
    /*
     * Without a filter.
     */
    PathPrefixSet pps = new PathPrefixSet(mergePathPrefixes);
    PathPrefix pp1 = new PathPrefix("", null);
    pps.add(pp1);

    assertEquals(pp1, pps.includesResource("W.java").getPathPrefix());
    assertEquals(pp1, pps.includesResource("a/X.java").getPathPrefix());
    assertEquals(pp1, pps.includesResource("a/b/Y.java").getPathPrefix());
    assertEquals(pp1, pps.includesResource("a/b/c/Z.java").getPathPrefix());
  }

  public void doTestZeroLengthPrefixNonEmptyFilter(boolean mergePathPrefixes) {
    /*
     * With a filter.
     */
    PathPrefixSet pps = new PathPrefixSet(mergePathPrefixes);
    PathPrefix pp1 = new PathPrefix("", new ResourceFilter() {
      @Override
      public boolean allows(String path) {
        return path.endsWith("Y.java");
      }
    });
    pps.add(pp1);

    assertNull(pps.includesResource("W.java"));
    assertNull(pps.includesResource("a/X.java"));
    assertEquals(pp1, pps.includesResource("a/b/Y.java").getPathPrefix());
    assertNull(pps.includesResource("a/b/c/Z.java"));
  }

  public void testMalformedPrefix() {
    try {
      new PathPrefix("com/google/foo//", null);
      // Can't use fail() because of the AssertionError catch.
      throw new RuntimeException(
          "PathPrefix construction should have failed because of the ending //");
    } catch (AssertionError e) {
      // expected
    }
  }

  public void testEmptyPrefixSet() {
    doTestEmptyPrefixSet(true);
    doTestEmptyPrefixSet(false);
  }

  public void testExcludes_DenyOverridesAllow() {
    doTestExcludes_DenyOverridesAllow(true);
    doTestExcludes_DenyOverridesAllow(false);
  }

  public void testFilter_AllowOverridesDeny() {
    doTestFilter_AllowOverridesDeny(true);
    doTestFilter_AllowOverridesDeny(false);
  }

  public void testMostSpecificFilterWins() {
    doTestMostSpecificFilterWins(true);
    doTestMostSpecificFilterWins(false);
  }

  public void testNonOverlappingPrefixesEmptyFilter() {
    doTestNonOverlappingPrefixesEmptyFilter(true);
    doTestNonOverlappingPrefixesEmptyFilter(false);
  }

  public void testNonOverlappingPrefixesNonEmptyFilter() {
    doTestNonOverlappingPrefixesNonEmptyFilter(true);
    doTestNonOverlappingPrefixesNonEmptyFilter(false);
  }

  public void testOverlappingPrefixesEmptyFilter() {
    doTestOverlappingPrefixesEmptyFilter(true);
    doTestOverlappingPrefixesEmptyFilter(false);
  }

  public void testOverlappingPrefixesNonEmptyFilter() {
    doTestOverlappingPrefixesNonEmptyFilter(true);
    doTestOverlappingPrefixesNonEmptyFilter(false);
  }

  public void testZeroLengthPrefixEmptyFilter() {
    doTestZeroLengthPrefixEmptyFilter(true);
    doTestZeroLengthPrefixEmptyFilter(false);
  }

  public void testZeroLengthPrefixNonEmptyFilter() {
    doTestZeroLengthPrefixNonEmptyFilter(true);
    doTestZeroLengthPrefixNonEmptyFilter(false);
  }
}
