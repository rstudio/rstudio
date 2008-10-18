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

/**
 * Tests the trie and filtering behavior of path prefix set.
 */
public class PathPrefixSetTest extends TestCase {

  public void testEmptyPrefixSet() {
    PathPrefixSet pps = new PathPrefixSet();
    assertFalse(pps.includesResource("com/google/gwt/user/client/Command.java"));
  }

  public void testNonOverlappingPrefixes() {
    {
      /*
       * Test with null filters to ensure nothing gets filtered out.
       */
      PathPrefixSet pps = new PathPrefixSet();
      pps.add(new PathPrefix("com/google/gwt/user/client/", null));
      pps.add(new PathPrefix("com/google/gwt/i18n/client/", null));
      pps.add(new PathPrefix("com/google/gwt/dom/client/", null));

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

      assertTrue(pps.includesResource("com/google/gwt/user/client/Command.java"));
      assertTrue(pps.includesResource("com/google/gwt/user/client/Timer.java"));
      assertTrue(pps.includesResource("com/google/gwt/i18n/client/Messages.java"));
      assertTrue(pps.includesResource("com/google/gwt/dom/client/DivElement.java"));

      assertFalse(pps.includesResource("com/google/gwt/user/rebind/rpc/ServiceInterfaceProxyGenerator.java"));
      assertFalse(pps.includesResource("com/google/gwt/sample/hello/client/Hello.java"));
      assertFalse(pps.includesResource("com/google/gwt/user/public/clear.cache.gif"));
    }

    {
      /*
       * Test with a real filter to ensure it does have an effect.
       */
      PathPrefixSet pps = new PathPrefixSet();
      ResourceFilter allowsGifs = new ResourceFilter() {
        public boolean allows(String path) {
          return path.toLowerCase().endsWith(".gif");
        }
      };

      pps.add(new PathPrefix("com/google/gwt/user/public/", allowsGifs));
      pps.add(new PathPrefix("com/google/gwt/sample/mail/public/", allowsGifs));

      // Correct prefix, and filter should allow .
      assertTrue(pps.includesResource("com/google/gwt/user/public/clear.cache.gif"));
      assertTrue(pps.includesResource("com/google/gwt/sample/mail/public/inboxIcon.gif"));

      // Correct prefix, but filter should exclude.
      assertFalse(pps.includesResource("com/google/gwt/user/public/README.txt"));
      assertFalse(pps.includesResource("com/google/gwt/sample/mail/public/README.txt"));

      // Wrong prefix, and filter would have excluded.
      assertFalse(pps.includesResource("com/google/gwt/user/client/Command.java"));
      assertFalse(pps.includesResource("com/google/gwt/user/rebind/rpc/ServiceInterfaceProxyGenerator.java"));

      // Wrong prefix, but filter would have allowed it.
      assertFalse(pps.includesResource("com/google/gwt/i18n/public/flags.gif"));
    }
  }

  public void testOverlappingPrefixes() {
    {
      /*
       * Without a filter.
       */
      PathPrefixSet pps = new PathPrefixSet();
      pps.add(new PathPrefix("", null));
      pps.add(new PathPrefix("a/", null));
      pps.add(new PathPrefix("a/b/", null));
      pps.add(new PathPrefix("a/b/c/", null));

      assertTrue(pps.includesResource("W.java"));
      assertTrue(pps.includesResource("a/X.java"));
      assertTrue(pps.includesResource("a/b/Y.java"));
      assertTrue(pps.includesResource("a/b/c/Z.java"));
      assertTrue(pps.includesResource("a/b/c/d/V.java"));
    }

    {
      /*
       * Ensure the right filter applies.
       */
      PathPrefixSet pps = new PathPrefixSet();
      pps.add(new PathPrefix("", null));
      pps.add(new PathPrefix("a/", null));
      pps.add(new PathPrefix("a/b/", new ResourceFilter() {
        public boolean allows(String path) {
          // Disallow anything ending with "FILTERMEOUT".
          return !path.endsWith("FILTERMEOUT");
        }
      }));
      pps.add(new PathPrefix("a/b/c/", null));

      assertTrue(pps.includesResource("W.java"));
      assertTrue(pps.includesResource("a/X.java"));
      assertTrue(pps.includesResource("a/b/Y.java"));
      // This should be gone, since it is found in b.
      assertFalse(pps.includesResource("a/b/FILTERMEOUT"));
      /*
       * This should not be gone, because it is using c's (null) filter instead
       * of b's. The logic here is that the prefix including c is more specific
       * and seemed to want c's resources to be included.
       */
      assertTrue(pps.includesResource("a/b/c/DONT_FILTERMEOUT"));
      assertTrue(pps.includesResource("a/b/c/Z.java"));
      assertTrue(pps.includesResource("a/b/c/d/V.java"));
    }
  }

  /**
   * In essense, this tests support for the default package in Java.
   */
  public void testZeroLengthPrefix() {
    {
      /*
       * Without a filter.
       */
      PathPrefixSet pps = new PathPrefixSet();
      pps.add(new PathPrefix("", null));

      assertTrue(pps.includesResource("W.java"));
      assertTrue(pps.includesResource("a/X.java"));
      assertTrue(pps.includesResource("a/b/Y.java"));
      assertTrue(pps.includesResource("a/b/c/Z.java"));
    }

    {
      /*
       * With a filter.
       */
      PathPrefixSet pps = new PathPrefixSet();
      pps.add(new PathPrefix("", new ResourceFilter() {
        public boolean allows(String path) {
          return path.endsWith("Y.java");
        }
      }));

      assertFalse(pps.includesResource("W.java"));
      assertFalse(pps.includesResource("a/X.java"));
      assertTrue(pps.includesResource("a/b/Y.java"));
      assertFalse(pps.includesResource("a/b/c/Z.java"));

    }
  }
}
