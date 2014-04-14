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
package com.google.gwt.dev.util.collect;

import org.apache.commons.collections.map.AbstractTestIterableMap;

import java.util.Map;

/**
 * Test for {@link HashMap}.
 */
public class HashMapTest extends AbstractTestIterableMap {
  public HashMapTest(String testName) {
    super(testName);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map makeEmptyMap() {
    return new HashMap();
  }

  @Override
  public void testFailFastEntrySet() {
    // Does not throw ConcurrentModificationException.
  }

  @Override
  public void testFailFastKeySet() {
    // Does not throw ConcurrentModificationException.
  }

  @Override
  public void testFailFastValues() {
    // Does not throw ConcurrentModificationException.
  }

  @Override
  protected boolean skipSerializedCanonicalTests() {
    return true;
  }
}
