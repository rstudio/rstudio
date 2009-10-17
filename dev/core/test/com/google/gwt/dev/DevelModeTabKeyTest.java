/**
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
 * Tests for DevelModeTabKey.
 */
public class DevelModeTabKeyTest extends TestCase {
  
  public void testConstructor() {
    DevelModeTabKey key = new DevelModeTabKey("ua1",
        "http://example.org/foo.html", "tabkey1", "host:9999");
    assertEquals("ua1", key.getUserAgent());
    assertEquals("http://example.org/foo.html", key.getUrl());
    assertEquals("tabkey1", key.getTabKey());
    assertEquals("host:9999", key.getRemoteSocket());
    key = new DevelModeTabKey("ua1",
        "http://example.org/foo.html?param=value#hash", "tabkey1", "host:9999");
    assertEquals("http://example.org/foo.html", key.getUrl());
    
    // check acceptance/rejection of nulls
    try {
      key = new DevelModeTabKey(null,
          "http://example.org/foo.html", "tabkey1", "host:9999");
      fail("Expected exception on null userAgent");
    } catch (IllegalArgumentException expected) {
    }
    key = new DevelModeTabKey("ua1",
        null, "tabkey1", "host:9999");
    assertEquals("", key.getUrl());
    key = new DevelModeTabKey("ua1",
        "http://example.org/foo.html", null, "host:9999");
    assertEquals("", key.getTabKey());
    try {
      key = new DevelModeTabKey("ua1",
          "http://example.org/foo.html", "tabkey1", null);
      fail("Expected exception on null remoteHost");
    } catch (IllegalArgumentException expected) {
    }
  }
  
  public void testEquals() {
    DevelModeTabKey key1 = new DevelModeTabKey("ua1",
        "http://example.org/foo.html", "tabkey1", "host:9999");
    DevelModeTabKey key2 = new DevelModeTabKey("ua1",
        "http://example.org/foo.html", "tabkey1", "host:9999");
    assertEquals(key1, key2);
    
    // query parameters and the history token don't matter
    key2 = new DevelModeTabKey("ua1",
        "http://example.org/foo.html?param=value#hash", "tabkey1", "host:9999");
    assertEquals(key1, key2);

    // various mismatches
    key2 = new DevelModeTabKey("ua2",
        "http://example.org/foo.html?param=value#hash", "tabkey1", "host:9999");
    assertFalse(key1.equals(key2));
    key2 = new DevelModeTabKey("ua1",
        "http://example.org:80/foo.html", "tabkey1", "host:9999");
    assertFalse(key1.equals(key2));
    key2 = new DevelModeTabKey("ua1",
        "http://example.org/foo.html", "tabkey2", "host:9999");
    assertFalse(key1.equals(key2));
    key2 = new DevelModeTabKey("ua1",
        "http://example.org:80/foo.html", "tabkey1", "host:9998");
    assertFalse(key1.equals(key2));
  }
}
