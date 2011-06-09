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
package com.google.web.bindery.requestfactory.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Tests the use of non-trivial EntityProxy and ValueProxy key types.
 */
public class ComplexKeysTest extends GWTTestCase {
  /**
   * The factory being tested.
   */
  protected interface Factory extends RequestFactory {
    Context context();
  }

  @Service(ContextImpl.class)
  interface Context extends RequestContext {
    Request<DomainWithEntityKeyProxy> createEntity(String key);

    Request<DomainWithValueKeyProxy> createValue(String key);
  }

  static class ContextImpl {
    public static DomainWithEntityKey createEntity(String key) {
      return new DomainWithEntityKey(new EntityKey(key));
    }

    public static DomainWithValueKey createValue(String key) {
      return new DomainWithValueKey(new ValueKey(key));
    }
  }

  static class DomainWithEntityKey {

    public static DomainWithEntityKey findDomainWithEntityKey(EntityKey key) {
      return new DomainWithEntityKey(key);
    }

    private final EntityKey key;

    public DomainWithEntityKey(EntityKey key) {
      if (key == null) {
        throw new IllegalArgumentException("Key key");
      }
      this.key = key;
    }

    public EntityKey getId() {
      return key;
    }

    public Integer getVersion() {
      return 0;
    }
  }

  @ProxyFor(DomainWithEntityKey.class)
  interface DomainWithEntityKeyProxy extends EntityProxy {
    EntityKeyProxy getId();

    EntityProxyId<DomainWithEntityKeyProxy> stableId();
  }

  static class DomainWithValueKey {
    public static DomainWithValueKey create(String key) {
      return new DomainWithValueKey(new ValueKey(key));
    }

    public static DomainWithValueKey findDomainWithValueKey(ValueKey key) {
      return new DomainWithValueKey(key);
    }

    private final ValueKey key;

    public DomainWithValueKey(ValueKey key) {
      if (key == null) {
        throw new IllegalArgumentException("Key key");
      }
      this.key = key;
    }

    public ValueKey getId() {
      return key;
    }

    public Integer getVersion() {
      return 0;
    }
  }

  @ProxyFor(DomainWithValueKey.class)
  interface DomainWithValueKeyProxy extends EntityProxy {
    ValueKeyProxy getId();

    EntityProxyId<DomainWithValueKeyProxy> stableId();
  }

  static class EntityKey {
    public static EntityKey findEntityKey(String key) {
      return new EntityKey(key);
    }

    private final String key;

    public EntityKey(String key) {
      assertEquals("key", key);
      this.key = key;
    }

    public String getId() {
      return key;
    }

    public Integer getVersion() {
      return 0;
    }
  }

  @ProxyFor(EntityKey.class)
  interface EntityKeyProxy extends EntityProxy {
  }

  static class ValueKey {
    private String key;

    public ValueKey() {
    }

    public ValueKey(String key) {
      setKey(key);
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      assertEquals("key", key);
      this.key = key;
    }
  }

  @ProxyFor(ValueKey.class)
  interface ValueKeyProxy extends ValueProxy {
    String getKey();
  }

  private static final int TEST_DELAY = 5000;
  private Factory factory;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  public void testEntityKey() {
    delayTestFinish(TEST_DELAY);
    context().createEntity("key").fire(
        new Receiver<DomainWithEntityKeyProxy>() {
          @Override
          public void onSuccess(final DomainWithEntityKeyProxy response) {
            factory.find(response.stableId()).fire(
                new Receiver<DomainWithEntityKeyProxy>() {
                  @Override
                  public void onSuccess(DomainWithEntityKeyProxy found) {
                    assertEquals(response.stableId(), found.stableId());
                    finishTest();
                  }
                });
          }
        });
  }

  public void testValueKey() {
    delayTestFinish(TEST_DELAY);
    context().createValue("key").fire(new Receiver<DomainWithValueKeyProxy>() {
      @Override
      public void onSuccess(final DomainWithValueKeyProxy response) {
        factory.find(response.stableId()).fire(
            new Receiver<DomainWithValueKeyProxy>() {
              @Override
              public void onSuccess(DomainWithValueKeyProxy found) {
                assertEquals(response.stableId(), found.stableId());
                finishTest();
              }
            });
      }
    });
  }

  protected Factory createFactory() {
    Factory toReturn = GWT.create(Factory.class);
    toReturn.initialize(new SimpleEventBus());
    return toReturn;
  }

  @Override
  protected void gwtSetUp() {
    factory = createFactory();
  }

  private Context context() {
    return factory.context();
  }
}
