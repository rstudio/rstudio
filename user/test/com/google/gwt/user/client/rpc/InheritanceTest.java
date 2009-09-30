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
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AnonymousClassInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.Circle;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableSubclass;

/**
 * TODO: document me.
 */
public class InheritanceTest extends GWTTestCase {
  private static final int TEST_DELAY = 5000;

  private InheritanceTestServiceAsync inheritanceTestService;

  public String getModuleName() {
    return "com.google.gwt.user.RPCSuite";
  }

  /**
   * Test that anonymous classes are not serializable.
   */
  public void testAnonymousClasses() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(new AnonymousClassInterface() {
      public void foo() {
        // purposely empty
      }
    }, new AsyncCallback<Object>() {
      public void onFailure(Throwable caught) {
        finishTest();
      }

      public void onSuccess(Object result) {
        fail("Anonymous inner classes should not be serializable");
      }
    });
  }

  /**
   * Tests that a shadowed field is properly serialized.
   * 
   * Checks for <a href="bug
   * http://code.google.com/p/google-web-toolkit/issues/detail?id=161">BUG
   * 161</a>
   */
  public void testFieldShadowing() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(InheritanceTestSetFactory.createCircle(),
        new AsyncCallback<Object>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Object result) {
            Circle circle = (Circle) result;
            assertNotNull(circle.getName());
            finishTest();
          }
        });
  }

  /**
   * Tests that transient fields do not prevent serializability.
   */
  public void testJavaSerializableClass() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(new InheritanceTestSetFactory.JavaSerializableClass(3),
        new AsyncCallback<Object>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            finishTest();
          }
        });
  }

  /**
   * Test that non-static inner classes are not serializable.
   */
  public void testNonStaticInnerClass() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(InheritanceTestSetFactory.createNonStaticInnerClass(),
        new AsyncCallback<Object>() {
          public void onFailure(Throwable caught) {
            finishTest();
          }

          public void onSuccess(Object result) {
            fail("Non-static inner classes should not be serializable");
          }
        });
  }

  public void testReturnOfUnserializableClassFromServer() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.getUnserializableClass(new AsyncCallback<Object>() {
      public void onFailure(Throwable caught) {
        finishTest();
      }

      public void onSuccess(Object result) {
        fail("Returning an unserializable class from the server should fail");
      }
    });
  }

  /**
   * Test that a valid serializable class can be serialized.
   */
  public void testSerializableClass() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(InheritanceTestSetFactory.createSerializableClass(),
        new AsyncCallback<Object>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(InheritanceTestSetValidator.isValid((SerializableClass) result));
            finishTest();
          }
        });
  }

  /**
   * Test that IsSerializable is inherited, also test static inner classes.
   */
  public void testSerializableSubclass() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(InheritanceTestSetFactory.createSerializableSubclass(),
        new AsyncCallback<Object>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(InheritanceTestSetValidator.isValid((SerializableSubclass) result));
            finishTest();
          }
        });
  }

  public void testSerializationExceptionPreventsCall() {
    delayTestFinish(TEST_DELAY);
    final boolean serializationExceptionCaught[] = new boolean[1];
    new Timer() {
      @Override
      public void run() {
        assertTrue("serializationExceptionCaught was not true",
            serializationExceptionCaught[0]);
        finishTest();
      }
    }.schedule(TEST_DELAY / 2);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(new AnonymousClassInterface() {
      public void foo() {
        // purposely empty
      }
    }, new AsyncCallback<Object>() {
      public void onFailure(Throwable caught) {
        assertTrue(
            "onFailure: got something other than a SerializationException ("
                + caught.getClass().getName() + ")",
            caught instanceof SerializationException);
        serializationExceptionCaught[0] = true;
      }

      public void onSuccess(Object result) {
        fail("onSuccess: call should not have succeeded");
      }
    });
  }

  /**
   * Tests that transient fields do not prevent serializability.
   */
  public void testTransientFieldExclusion() {
    delayTestFinish(TEST_DELAY);

    InheritanceTestServiceAsync service = getServiceAsync();
    service.echo(
        InheritanceTestSetFactory.createSerializableClassWithTransientField(),
        new AsyncCallback<Object>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(InheritanceTestSetValidator.isValid((SerializableClassWithTransientField) result));
            finishTest();
          }
        });
  }

  private InheritanceTestServiceAsync getServiceAsync() {
    if (inheritanceTestService == null) {
      inheritanceTestService = (InheritanceTestServiceAsync) GWT.create(InheritanceTestServiceSubtype.class);
      ((ServiceDefTarget) inheritanceTestService).setServiceEntryPoint(GWT.getModuleBaseURL()
          + "inheritance");
    }
    return inheritanceTestService;
  }
}
