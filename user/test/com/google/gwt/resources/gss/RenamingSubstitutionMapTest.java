/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.resources.gss;

import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * Test for {@link com.google.gwt.resources.gss.RenamingSubstitutionMap}.
 */
public class RenamingSubstitutionMapTest extends TestCase {

  public void testComputeReplacementMapWithPrefixedClasses() {
    Map<String, Map<String, String>> replacementWithPrefix = new HashMap<String, Map<String,
        String>>();
    replacementWithPrefix.put("", ImmutableMap.of("class1", "obfuscated1", "class2",
        "obfuscated2"));
    replacementWithPrefix.put("prefix1-", ImmutableMap.of("class3", "obfuscated3", "class4",
        "obfuscated4"));

    RenamingSubstitutionMap substitutionMap = new RenamingSubstitutionMap(replacementWithPrefix);

    assertEquals("obfuscated1", substitutionMap.get("class1"));
    assertEquals("obfuscated2", substitutionMap.get("class2"));
    assertEquals("obfuscated3", substitutionMap.get("prefix1-class3"));
    assertEquals("obfuscated4", substitutionMap.get("prefix1-class4"));
    assertTrue(substitutionMap.getExternalClassCandidates().isEmpty());
  }

  public void testComputeReplacementMapWithMissingCLass() {
    Map<String, Map<String, String>> replacementWithPrefix = new HashMap<String, Map<String,
        String>>();
    replacementWithPrefix.put("", ImmutableMap.of("class1", "obfuscated1"));

    RenamingSubstitutionMap substitutionMap = new RenamingSubstitutionMap(replacementWithPrefix);

    assertEquals("obfuscated1", substitutionMap.get("class1"));
    assertEquals("otherClass", substitutionMap.get("otherClass"));

    assertFalse(substitutionMap.getExternalClassCandidates().isEmpty());
    assertTrue(substitutionMap.getExternalClassCandidates().contains("otherClass"));
  }
}
