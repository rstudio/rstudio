/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AnonymousClassInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.Circle;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableSubclass;

/**
 * TODO: document me.
 */
public class InheritanceTest extends TypeSerializerWorkAround {
  // private static final int TEST_DELAY = Integer.MAX_VALUE;
  private static final int TEST_DELAY = 5000;

  public String getModuleName() {
    return "com.google.gwt.user.RPCSuite";
  }

  /**
   * Test that anonymous classes are not serializable
   */
  public void testAnonymousClasses() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    try {
      service.echo(new AnonymousClassInterface() {
        public void foo() {
          // TODO Auto-generated method stub
        }
      }, new AsyncCallback() {
        public void onFailure(Throwable caught) {
          finishTest();
        }

        public void onSuccess(Object result) {
          fail("Anonymous inner classes should not be serializable");
        }
      });
    } catch (RuntimeException ex) {
      workAroundTypeSerializerBug(ex);
    }
  }

  /**
   * Tests that a shadowed field is properly serialized.
   * 
   * Checks for <a href="bug
   * http://code.google.com/p/google-web-toolkit/issues/detail?id=161">BUG 161</a>
   */
  public void testFieldShadowing() {
    delayTestFinish(TEST_DELAY);

    try {
      InheritanceTestServiceAsync service = getServiceAsync();
      service.echo(InheritanceTestSetFactory.createCircle(),
          new AsyncCallback() {
            public void onFailure(Throwable caught) {
              fail("Unexpected failure");
            }

            public void onSuccess(Object result) {
              Circle circle = (Circle) result;
              assertNotNull(circle.getName());
              finishTest();
            }
          });
    } catch (RuntimeException ex) {
      workAroundTypeSerializerBug(ex);
    }
  }

  /**
   * Test that non-static inner classes are not serializable
   */
  public void testNonStaticInnerClass() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestSetFactory factory = new InheritanceTestSetFactory();
    InheritanceTestServiceAsync service = getServiceAsync();
    try {
      service.echo(factory.new NonStaticInnerClass(), new AsyncCallback() {
        public void onFailure(Throwable caught) {
          finishTest();
        }

        public void onSuccess(Object result) {
          fail("Non-static inner classes should not be serializable");
        }
      });
    } catch (RuntimeException ex) {
      workAroundTypeSerializerBug(ex);
    }
  }

  public void testReturnOfUnserializableClassFromServer() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.getUnserializableClass(new AsyncCallback() {
      public void onFailure(Throwable caught) {
        finishTest();
      }

      public void onSuccess(Object result) {
        fail("Returning an unserializable class from the server should fail");
      }
    });
  }

  /**
   * Test that a valid serializable class can be serialized
   */
  public void testSerializableClass() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(InheritanceTestSetFactory.createSerializableClass(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            fail(caught.toString());
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(InheritanceTestSetValidator.isValid((SerializableClass) result));
            finishTest();
          }
        });
  }

  /**
   * Test that IsSerializable is inherited, also test static inner classes
   */
  public void testSerializableSubclass() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(InheritanceTestSetFactory.createSerializableSubclass(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            fail(caught.toString());
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(InheritanceTestSetValidator.isValid((SerializableSubclass) result));
            finishTest();
          }
        });
  }

  // test that transient fields do not prevent serializability
  public void testTransientFieldExclusion() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(
        InheritanceTestSetFactory.createSerializableClassWithTransientField(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            fail(caught.toString());
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(InheritanceTestSetValidator.isValid((SerializableClassWithTransientField) result));
            finishTest();
          }
        });
  }

  /**
   * Test that unserializable fields prevent a class from being serializable
   * also tests unserializable subclasses
   */
  public void testUnserializableClassField() {
    delayTestFinish(TEST_DELAY);

    try {
      InheritanceTestServiceAsync service = getServiceAsync();
      service.echo(
          InheritanceTestSetFactory.createSerializableClassWithUnserializableClassField(),
          new AsyncCallback() {
            public void onFailure(Throwable caught) {
              finishTest();
            }

            public void onSuccess(Object result) {
              fail("Class SerializableClassWithUnserializableClassField should not be serializable");
            }
          });
    } catch (RuntimeException ex) {
      workAroundTypeSerializerBug(ex);
    }
  }

  /**
   * Test that unserializable fields prevent a class from being serializable,
   * also tests unserializable subclasses
   */
  public void testUnserializableObjectField() {
    delayTestFinish(TEST_DELAY);

    try {
      InheritanceTestServiceAsync service = getServiceAsync();
      service.echo(
          InheritanceTestSetFactory.createSerializableClassWithUnserializableObjectField(),
          new AsyncCallback() {
            public void onFailure(Throwable caught) {
              finishTest();
            }

            public void onSuccess(Object result) {
              fail("Class SerializableClassWithUnserializableObjectField should not be serializable");
            }
          });
    } catch (RuntimeException ex) {
      workAroundTypeSerializerBug(ex);
    }
  }

  private InheritanceTestServiceAsync getServiceAsync() {
    if (inheritanceTestService == null) {
      inheritanceTestService = (InheritanceTestServiceAsync) GWT.create(InheritanceTestService.class);
      ((ServiceDefTarget) inheritanceTestService).setServiceEntryPoint(GWT.getModuleBaseURL()
          + "inheritance");
    }
    return inheritanceTestService;
  }

  private InheritanceTestServiceAsync inheritanceTestService;
}
