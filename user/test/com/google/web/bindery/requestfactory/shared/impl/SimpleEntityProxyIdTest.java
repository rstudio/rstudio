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
package com.google.web.bindery.requestfactory.shared.impl;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.SimpleBarProxy;
import com.google.web.bindery.requestfactory.shared.SimpleFooProxy;

import junit.framework.TestCase;

/**
 * Tests {@link SimpleEntityProxyId}.
 */
public class SimpleEntityProxyIdTest extends TestCase {

  public void testEquality() {
    SimpleEntityProxyId<EntityProxy> client1 = id(EntityProxy.class, 1);
    // equal to self
    assertTrue(isStable(client1, client1));
    // equal to identical client id
    assertTrue(isStable(client1, id(EntityProxy.class, 1)));

    // Persist and check again
    client1.setServerId("server1");
    // equal to self
    assertTrue(isStable(client1, client1));
    // equal to identical client id
    assertTrue(isStable(client1, id(EntityProxy.class, 1)));

    SimpleEntityProxyId<EntityProxy> server1 = id(EntityProxy.class, "server1");
    assertTrue(isStable(server1, id(EntityProxy.class, "server1")));

    /*
     * Compare a server-only id the persisted client id, this should be false
     * since the hashcodes would vary.
     */
    assertFalse(isStable(client1, server1));
  }

  public void testInequality() {
    assertFalse(isStable(id(EntityProxy.class, 1), id(EntityProxy.class, 2)));

    assertFalse(isStable(id(EntityProxy.class, "server1"),
        id(EntityProxy.class, "server2")));

    // Same client-side id, but different types
    assertFalse(isStable(id(SimpleFooProxy.class, 1),
        id(SimpleBarProxy.class, 1)));

    // Same server id, but different types
    assertFalse(isStable(id(SimpleFooProxy.class, "server1"),
        id(SimpleBarProxy.class, "server1")));
  }

  private <T extends EntityProxy> SimpleEntityProxyId<T> id(Class<T> clazz,
      int clientId) {
    return new SimpleEntityProxyId<T>(clazz, clientId);
  }

  private <T extends EntityProxy> SimpleEntityProxyId<T> id(Class<T> clazz,
      String serverId) {
    return new SimpleEntityProxyId<T>(clazz, serverId);
  }

  /**
   * Assert that the id behaves with the stable sematics that are desired for
   * client code.
   */
  private boolean isStable(SimpleEntityProxyId<?> a, SimpleEntityProxyId<?> b) {
    return a.equals(b) && b.equals(a) && a.hashCode() == b.hashCode();
  }
}
