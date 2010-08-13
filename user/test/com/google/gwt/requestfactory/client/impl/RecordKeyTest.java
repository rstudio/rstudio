/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.requestfactory.client.impl;

import junit.framework.TestCase;

/**
 * Tests for {@link RecordJsoImpl}.
 */
public class RecordKeyTest extends TestCase {

  public void testEquals() {
    RecordKey newKey1 = new RecordKey(1L, SimpleFooRecordImpl.SCHEMA,
        RequestFactoryJsonImpl.IS_FUTURE);
    RecordKey newKey2 = new RecordKey(newKey1.id + 1, newKey1.schema,
        newKey1.isFuture);
    RecordKey oldKey1 = new RecordKey(newKey1.id, newKey1.schema,
        !newKey1.isFuture);
    RecordKey anotherNewKey1 = new RecordKey(newKey1.id, newKey1.schema,
        newKey1.isFuture);
    
    assertTrue(newKey1.equals(anotherNewKey1));
    assertTrue(newKey1.hashCode() == anotherNewKey1.hashCode());
    
    assertFalse(newKey1.equals(newKey2));
    assertFalse(newKey1.hashCode() == newKey2.hashCode());
    
    assertFalse(newKey1.equals(oldKey1));
    assertFalse(newKey1.hashCode() == newKey2.hashCode());
  }
}
