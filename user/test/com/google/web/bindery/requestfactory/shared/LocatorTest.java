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
 * Tests the use of Locator objects.
 */
public class LocatorTest extends GWTTestCase {
  /**
   * The locator being tested.
   */
  public static class DomainLocator extends Locator<Domain, String> {
    @Override
    public Domain create(Class<? extends Domain> clazz) {
      assertEquals(Domain.class, clazz);
      return new Domain();
    }

    @Override
    public Domain find(Class<? extends Domain> clazz, String id) {
      assertEquals(ID, id);
      return Domain.INSTANCE;
    }

    @Override
    public Class<Domain> getDomainType() {
      return Domain.class;
    }

    @Override
    public String getId(Domain domainObject) {
      return ID;
    }

    @Override
    public Class<String> getIdType() {
      return String.class;
    }

    @Override
    public Object getVersion(Domain domainObject) {
      return 0;
    }
  }

  /**
   * The factory under test.
   */
  protected interface Factory extends RequestFactory {
    Context context();
  }

  @Service(ContextImpl.class)
  interface Context extends RequestContext {
    Request<DomainProxy> getDomain();
  }

  static class ContextImpl {
    public static Domain getDomain() {
      return Domain.INSTANCE;
    }
  }

  static class Domain {
    static final Domain INSTANCE = new Domain();
  }

  @ProxyFor(value = Domain.class, locator = DomainLocator.class)
  interface DomainProxy extends EntityProxy {
    EntityProxyId<DomainProxy> stableId();
  };

  private static final String ID = "DomainId";
  private static final int TEST_DELAY = 5000;

  private Factory factory;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  public void testLocator() {
    delayTestFinish(TEST_DELAY);
    context().getDomain().fire(new Receiver<DomainProxy>() {
      @Override
      public void onSuccess(final DomainProxy response) {
        factory.find(response.stableId()).fire(new Receiver<DomainProxy>() {
          @Override
          public void onSuccess(DomainProxy found) {
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
