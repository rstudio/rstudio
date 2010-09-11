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

import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link ValueStoreJsonImpl}.
 */
public class ValueStoreJsonImplTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  public void testPutInValueStore() {
    ValueStoreJsonImpl valueStore = new ValueStoreJsonImpl();
    ProxyJsoImpl minimalJso = ProxyJsoImplTest.getMinimalJso();
    ProxyJsoImpl populatedJso = ProxyJsoImplTest.getPopulatedJso();
    ProxyJsoImpl copyPopulatedJso = ProxyJsoImplTest.getPopulatedJso();
    minimalJso.getRequestFactory().init(new SimpleEventBus());

    assertNull(valueStore.putInValueStore(minimalJso));
    assertNull(valueStore.putInValueStore(populatedJso));
    assertSame(populatedJso, valueStore.putInValueStore(copyPopulatedJso));
    assertSame(populatedJso, valueStore.putInValueStore(minimalJso));
  }
  
}
