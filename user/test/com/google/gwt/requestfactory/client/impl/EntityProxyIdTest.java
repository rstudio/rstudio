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

import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyChangedEvent;
import com.google.gwt.requestfactory.shared.WriteOperation;

import junit.framework.TestCase;

/**
 * Eponymous unit test.
 */
public class EntityProxyIdTest extends TestCase {

  static class Schema1 extends ProxySchema<ProxyImpl> {
    public Schema1() {
      super("schemey");
    }

    @Override
    public ProxyImpl create(ProxyJsoImpl jso, boolean isFuture) {
      throw new UnsupportedOperationException("Auto-generated method stub");
    }

    @Override
    public EntityProxyChangedEvent<?, ?> createChangeEvent(EntityProxy proxy,
        WriteOperation writeOperation) {
      throw new UnsupportedOperationException("Auto-generated method stub");
    }

    @Override
    public Class<? extends EntityProxy> getProxyClass() {
      throw new UnsupportedOperationException("Auto-generated method stub");
    }
  }
  
  static class Schema2 extends Schema1 {
  }

  public void testEquals() {
    EntityProxyId newKey1 = new EntityProxyId(1L, new Schema1(),
        RequestFactoryJsonImpl.IS_FUTURE);

    EntityProxyId anotherNewKey1 = new EntityProxyId(newKey1.id, newKey1.schema,
        newKey1.isFuture);
    assertTrue(newKey1.equals(anotherNewKey1));
    assertTrue(newKey1.hashCode() == anotherNewKey1.hashCode());

    EntityProxyId newKey2 = new EntityProxyId(newKey1.id + 1, newKey1.schema,
        newKey1.isFuture);
    assertFalse(newKey1.equals(newKey2));
    assertFalse(newKey1.hashCode() == newKey2.hashCode());

    EntityProxyId newKey1NoSchema = new EntityProxyId(newKey1.id,
        new Schema2(), newKey1.isFuture);
    assertFalse(newKey1.equals(newKey1NoSchema));
    assertFalse(newKey1.hashCode() == newKey1NoSchema.hashCode());

    EntityProxyId oldKey1 = new EntityProxyId(newKey1.id, newKey1.schema,
        !newKey1.isFuture);
    assertFalse(newKey1.equals(oldKey1));
    assertFalse(newKey1.hashCode() == oldKey1.hashCode());
  }
}
