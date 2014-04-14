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

import org.apache.commons.collections.set.AbstractTestSet;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Test for {@link HashMap}.
 */
public class IdentityHashSetTest extends AbstractTestSet {
  private static final Float FLOAT_6 = 6.0f;
  private static final Double DOUBLE_5 = 5.0;

  public IdentityHashSetTest(String testName) {
    super(testName);
  }

  @Override
  public boolean areEqualElementsDistinguishable() {
    return true;
  }

  /**
   * Must use stable identities.
   */
  @Override
  public Object[] getFullNonNullElements() {
    return new Object[] {
        "", "One", 2, "Three", 4, "One", DOUBLE_5, FLOAT_6, "Seven", "Eight",
        "Nine", 10, (short) 11, 12L, "Thirteen", "14", "15", (byte) 16};
  }

  /**
   * Must use stable identities.
   */
  @Override
  public Object[] getOtherNonNullElements() {
    return new Object[] {
        0, 0f, 0.0, "Zero", (short) 0, (byte) 0, 0L, '\u0000', "0"};
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection makeConfirmedCollection() {
    final java.util.IdentityHashMap map = new java.util.IdentityHashMap();
    return new AbstractSet() {
      @Override
      public boolean add(Object e) {
        return map.put(e, e) == null;
      }

      @Override
      public Iterator iterator() {
        return map.keySet().iterator();
      }

      @Override
      public int size() {
        return map.size();
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set makeEmptySet() {
    return new IdentityHashSet();
  }

  @Override
  protected boolean skipSerializedCanonicalTests() {
    return true;
  }

  /**
   * This can't possible work due to non-stable identities.
   */
  @Override
  public void testSerializeDeserializeThenCompare() throws Exception {
  }
}
