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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

/**
 * TODO: document me.
 */
public class CollectionsTest extends GWTTestCase {
  private static final int TEST_DELAY = 10000;

  private CollectionsTestServiceAsync collectionsTestService;

  public void _testDateArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final Date[] expected = TestSetFactory.createDateArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (Date[]) result));
        finishTest();
      }
    });
  }

  public void disabledTestLongArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final Long[] expected = TestSetFactory.createLongArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (Long[]) result));
        finishTest();
      }
    });
  }

  public void disabledTestPrimitiveLongArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final long[] expected = TestSetFactory.createPrimitiveLongArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (long[]) result));
        finishTest();
      }
    });
  }

  public String getModuleName() {
    return "com.google.gwt.user.RPCSuite";
  }

  public void testArrayList() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    service.echo(TestSetFactory.createArrayList(), new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValid((ArrayList) result));
        finishTest();
      }
    });
  }

  public void testBooleanArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final Boolean[] expected = TestSetFactory.createBooleanArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (Boolean[]) result));
        finishTest();
      }
    });
  }

  public void testByteArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final Byte[] expected = TestSetFactory.createByteArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (Byte[]) result));
        finishTest();
      }
    });
  }

  public void testCharArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final Character[] expected = TestSetFactory.createCharArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (Character[]) result));
        finishTest();
      }
    });
  }

  public void testDoubleArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final Double[] expected = TestSetFactory.createDoubleArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (Double[]) result));
        finishTest();
      }
    });
  }

  /**
   * This method checks that attempting to return
   * {@link java.util.Arrays#asList(Object[])} from the server will result in an
   * InvocationException on the client.
   */
  public void testFailureWhenReturningArraysAsList() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final List expected = new ArrayList();
    for (byte i = 0; i < 10; ++i) {
      expected.add(new Byte(i));
    }

    service.getArraysAsList(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        assertTrue(caught.getClass().getName()
            + " should have been an InvocationException",
            caught instanceof InvocationException);
        finishTest();
      }

      public void onSuccess(Object result) {
        fail("Expected an InvocationException");
      }
    });
  }

  public void testFloatArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final Float[] expected = TestSetFactory.createFloatArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (Float[]) result));
        finishTest();
      }
    });
  }

  public void testHashMap() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final HashMap expected = TestSetFactory.createHashMap();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValid(expected, (HashMap) result));
        finishTest();
      }
    });
  }

  public void testHashSet() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final HashSet expected = TestSetFactory.createHashSet();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValid(expected, (HashSet) result));
        finishTest();
      }
    });
  }

  public void testIntegerArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final Integer[] expected = TestSetFactory.createIntegerArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (Integer[]) result));
        finishTest();
      }
    });
  }

  public void testPrimitiveBooleanArray() {
    delayTestFinish(TEST_DELAY);

    final boolean[] expected = TestSetFactory.createPrimitiveBooleanArray();
    CollectionsTestServiceAsync service = getServiceAsync();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertTrue(TestSetValidator.equals(expected, (boolean[]) result));
        finishTest();
      }
    });
  }

  public void testPrimitiveByteArray() {
    delayTestFinish(TEST_DELAY);

    final byte[] expected = TestSetFactory.createPrimitiveByteArray();
    CollectionsTestServiceAsync service = getServiceAsync();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertTrue(TestSetValidator.equals(expected, (byte[]) result));
        finishTest();
      }
    });
  }

  public void testPrimitiveCharArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final char[] expected = TestSetFactory.createPrimitiveCharArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (char[]) result));
        finishTest();
      }
    });
  }

  public void testPrimitiveDoubleArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final double[] expected = TestSetFactory.createPrimitiveDoubleArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (double[]) result));
        finishTest();
      }
    });
  }

  public void testPrimitiveFloatArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final float[] expected = TestSetFactory.createPrimitiveFloatArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (float[]) result));
        finishTest();
      }
    });
  }

  public void testPrimitiveIntegerArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final int[] expected = TestSetFactory.createPrimitiveIntegerArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (int[]) result));
        finishTest();
      }
    });
  }

  public void testPrimitiveShortArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final short[] expected = TestSetFactory.createPrimitiveShortArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (short[]) result));
        finishTest();
      }
    });
  }

  public void testShortArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final Short[] expected = TestSetFactory.createShortArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (Short[]) result));
        finishTest();
      }
    });
  }

  public void testStringArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final String[] expected = TestSetFactory.createStringArray();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, (String[]) result));
        finishTest();
      }
    });
  }

  public void testStringArrayArray() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final String[][] expected = new String[][] {
        new String[] {"hello"}, new String[] {"bye"}};
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        finishTest();
      }
    });
  }

  public void testVector() {
    delayTestFinish(TEST_DELAY);

    CollectionsTestServiceAsync service = getServiceAsync();
    final Vector expected = TestSetFactory.createVector();
    service.echo(expected, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValid(expected, (Vector) result));
        finishTest();
      }
    });
  }

  private CollectionsTestServiceAsync getServiceAsync() {
    if (collectionsTestService == null) {
      collectionsTestService = (CollectionsTestServiceAsync) GWT.create(CollectionsTestService.class);
      ((ServiceDefTarget) collectionsTestService).setServiceEntryPoint(GWT.getModuleBaseURL()
          + "collections");
    }
    return collectionsTestService;
  }
}
