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

import junit.framework.TestCase;

/**
 * Eponymous unit test.
 */
public class EntityProxyIdImplTest extends TestCase {

  static class Schema1 extends ProxySchema<ProxyImpl> {
    public Schema1() {
      super("schemey");
    }

    @Override
    public ProxyImpl create(ProxyJsoImpl jso, boolean isFuture) {
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
    EntityProxyIdImpl<?> newKey1 = new EntityProxyIdImpl<EntityProxy>("test", new Schema1(),
        RequestFactoryJsonImpl.IS_FUTURE, null);

    EntityProxyIdImpl<?> anotherNewKey1 = new EntityProxyIdImpl<EntityProxy>(newKey1.encodedId, newKey1.schema,
        newKey1.isFuture, null);
    assertTrue(newKey1.equals(anotherNewKey1));
    assertTrue(newKey1.hashCode() == anotherNewKey1.hashCode());

    EntityProxyIdImpl<?> newKey2 = new EntityProxyIdImpl<EntityProxy>((String) newKey1.encodedId + 1, newKey1.schema,
        newKey1.isFuture, null);
    assertFalse(newKey1.equals(newKey2));
    assertFalse(newKey1.hashCode() == newKey2.hashCode());

    EntityProxyIdImpl<?> newKey1NoSchema = new EntityProxyIdImpl<EntityProxy>(newKey1.encodedId,
        new Schema2(), newKey1.isFuture, null);
    assertFalse(newKey1.equals(newKey1NoSchema));
    assertFalse(newKey1.hashCode() == newKey1NoSchema.hashCode());

    EntityProxyIdImpl<?> oldKey1 = new EntityProxyIdImpl<EntityProxy>(newKey1.encodedId, newKey1.schema,
        !newKey1.isFuture, null);
    assertFalse(newKey1.equals(oldKey1));
    assertFalse(newKey1.hashCode() == oldKey1.hashCode());
  }
  
  public void testEqualsWithFuture() {
    EntityProxyIdImpl<?> newKey1 = new EntityProxyIdImpl<EntityProxy>("test", new Schema1(),
        RequestFactoryJsonImpl.IS_FUTURE, null);

    EntityProxyIdImpl<?> persistedNewKey1 = new EntityProxyIdImpl<EntityProxy>("test2",
        newKey1.schema, RequestFactoryJsonImpl.NOT_FUTURE, newKey1.encodedId);
    assertTrue(persistedNewKey1.equals(persistedNewKey1));
    assertTrue(newKey1.equals(persistedNewKey1));
    assertTrue(persistedNewKey1.equals(newKey1));
    assertTrue(newKey1.hashCode() == persistedNewKey1.hashCode());

    EntityProxyIdImpl<?> anotherPersistedNewKey1 = new EntityProxyIdImpl<EntityProxy>("test3",
        newKey1.schema, RequestFactoryJsonImpl.NOT_FUTURE,
        (String) newKey1.encodedId + 1);
    assertTrue(anotherPersistedNewKey1.equals(anotherPersistedNewKey1));
    assertFalse(persistedNewKey1.equals(anotherPersistedNewKey1));
    assertFalse(anotherPersistedNewKey1.equals(persistedNewKey1));
    assertFalse(newKey1.hashCode() == anotherPersistedNewKey1.hashCode());
  }
}
