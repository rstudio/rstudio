/*
 * Copyright 2008 Google Inc.
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
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeArrayList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeArraysAsList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmpty;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeVector;
import com.google.gwt.user.client.rpc.core.java.util.LinkedHashMap_CustomFieldSerializer;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Tests collections across RPC.
 */
public class CollectionsTest extends RpcTestBase {

  private CollectionsTestServiceAsync collectionsTestService;

  /**
   * TODO: Why is this disabled???
   */
  public void disabledTestDateArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Date[] expected = TestSetFactory.createDateArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Date[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Date[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  /**
   * This tests sending payloads that must be segmented to avoid problems with
   * IE6/7. This test is disabled since it sometimes fails on Safari, possibly
   * due to SSW.
   */
  public void disabledTestVeryLargeArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final int[] expected = TestSetFactory.createVeryLargeArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<int[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(int[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testArrayList() {
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TestSetFactory.createArrayList(),
        new AsyncCallback<ArrayList<MarkerTypeArrayList>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(ArrayList<MarkerTypeArrayList> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValid(result));
            finishTest();
          }
        });
  }
  
  public void testArrayListVoid() {
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echoArrayListVoid(TestSetFactory.createArrayListVoid(),
        new AsyncCallback<ArrayList<Void>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(ArrayList<Void> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidArrayListVoid(result));
            finishTest();
          }
        });
  }

  public void testArraysAsList() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final List<MarkerTypeArraysAsList> expected = TestSetFactory.createArraysAsList();

    delayTestFinishForRpc();
    service.echoArraysAsList(expected,
        new AsyncCallback<List<MarkerTypeArraysAsList>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(List<MarkerTypeArraysAsList> result) {
            assertNotNull(result);
            assertEquals(expected, result);
            finishTest();
          }
        });
  }

  public void testBooleanArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Boolean[] expected = TestSetFactory.createBooleanArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Boolean[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Boolean[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testByteArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Byte[] expected = TestSetFactory.createByteArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Byte[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Byte[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testCharArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Character[] expected = TestSetFactory.createCharArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Character[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Character[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testDoubleArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Double[] expected = TestSetFactory.createDoubleArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Double[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Double[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testEmptyList() {
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TestSetFactory.createEmptyList(),
        new AsyncCallback<List<MarkerTypeEmpty>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(List<MarkerTypeEmpty> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValid(result));
            finishTest();
          }
        });
  }

  public void testEmptyMap() {
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TestSetFactory.createEmptyMap(),
        new AsyncCallback<Map<MarkerTypeEmpty, MarkerTypeEmpty>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Map<MarkerTypeEmpty, MarkerTypeEmpty> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValid(result));
            finishTest();
          }
        });
  }

  public void testEmptySet() {
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TestSetFactory.createEmptySet(),
        new AsyncCallback<Set<MarkerTypeEmpty>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Set<MarkerTypeEmpty> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValid(result));
            finishTest();
          }
        });
  }

  public void testEnumArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Enum<?>[] expected = TestSetFactory.createEnumArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Enum<?>[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Enum<?>[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testFloatArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Float[] expected = TestSetFactory.createFloatArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Float[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Float[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testHashMap() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final HashMap<String, MarkerTypeHashMap> expected = TestSetFactory.createHashMap();
    delayTestFinishForRpc();
    service.echo(expected,
        new AsyncCallback<HashMap<String, MarkerTypeHashMap>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(HashMap<String, MarkerTypeHashMap> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValid(expected, result));
            finishTest();
          }
        });
  }

  public void testHashSet() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final HashSet<MarkerTypeHashSet> expected = TestSetFactory.createHashSet();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<HashSet<MarkerTypeHashSet>>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(HashSet<MarkerTypeHashSet> result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValid(expected, result));
        finishTest();
      }
    });
  }

  public void testIntegerArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Integer[] expected = TestSetFactory.createIntegerArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Integer[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Integer[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testLinkedHashMap() {
    CollectionsTestServiceAsync service = getServiceAsync();

    final LinkedHashMap<String, MarkerTypeLinkedHashMap> expected = TestSetFactory.createLinkedHashMap();
    assertFalse(LinkedHashMap_CustomFieldSerializer.getAccessOrderNoReflection(expected));

    delayTestFinishForRpc();
    service.echo(expected,
        new AsyncCallback<LinkedHashMap<String, MarkerTypeLinkedHashMap>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(
              LinkedHashMap<String, MarkerTypeLinkedHashMap> result) {
            assertNotNull(result);
            expected.get("SerializableSet");
            result.get("SerializableSet");
            assertTrue(TestSetValidator.isValid(expected, result));
            finishTest();
          }
        });
  }

  public void testLinkedHashMapLRU() {
    CollectionsTestServiceAsync service = getServiceAsync();

    final LinkedHashMap<String, MarkerTypeLinkedHashMap> expected = TestSetFactory.createLRULinkedHashMap();
    assertTrue(LinkedHashMap_CustomFieldSerializer.getAccessOrderNoReflection(expected));

    delayTestFinishForRpc();
    service.echo(expected,
        new AsyncCallback<LinkedHashMap<String, MarkerTypeLinkedHashMap>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(
              LinkedHashMap<String, MarkerTypeLinkedHashMap> actual) {
            assertNotNull(actual);
            expected.get("SerializableSet");
            actual.get("SerializableSet");
            assertTrue(TestSetValidator.isValid(expected, actual));
            finishTest();
          }
        });
  }

  public void testLongArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Long[] expected = TestSetFactory.createLongArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Long[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Long[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testPrimitiveBooleanArray() {
    final boolean[] expected = TestSetFactory.createPrimitiveBooleanArray();
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<boolean[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(boolean[] result) {
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testPrimitiveByteArray() {
    final byte[] expected = TestSetFactory.createPrimitiveByteArray();
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<byte[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(byte[] result) {
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testPrimitiveCharArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final char[] expected = TestSetFactory.createPrimitiveCharArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<char[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(char[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testPrimitiveDoubleArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final double[] expected = TestSetFactory.createPrimitiveDoubleArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<double[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(double[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testPrimitiveFloatArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final float[] expected = TestSetFactory.createPrimitiveFloatArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<float[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(float[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testPrimitiveIntegerArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final int[] expected = TestSetFactory.createPrimitiveIntegerArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<int[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(int[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testPrimitiveLongArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final long[] expected = TestSetFactory.createPrimitiveLongArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<long[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(long[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testPrimitiveShortArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final short[] expected = TestSetFactory.createPrimitiveShortArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<short[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(short[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testShortArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Short[] expected = TestSetFactory.createShortArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Short[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Short[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testSqlDateArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final java.sql.Date[] expected = TestSetFactory.createSqlDateArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<java.sql.Date[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(java.sql.Date[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testSqlTimeArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Time[] expected = TestSetFactory.createSqlTimeArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Time[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Time[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testSqlTimestampArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Timestamp[] expected = TestSetFactory.createSqlTimestampArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Timestamp[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Timestamp[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testStringArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final String[] expected = TestSetFactory.createStringArray();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<String[]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(String[] result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.equals(expected, result));
        finishTest();
      }
    });
  }

  public void testStringArrayArray() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final String[][] expected = new String[][] {
        new String[] {"hello"}, new String[] {"bye"}};
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<String[][]>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(String[][] result) {
        assertNotNull(result);
        finishTest();
      }
    });
  }

  public void testTreeMap() {
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    for (boolean option : new boolean[] {true, false}) {
      final TreeMap<String, MarkerTypeTreeMap> expected = TestSetFactory.createTreeMap(option);
      service.echo(expected, option,
          new AsyncCallback<TreeMap<String, MarkerTypeTreeMap>>() {
            public void onFailure(Throwable caught) {
              TestSetValidator.rethrowException(caught);
            }

            public void onSuccess(TreeMap<String, MarkerTypeTreeMap> result) {
              assertNotNull(result);
              assertTrue(TestSetValidator.isValid(expected, result));
              finishTest();
            }
          });
    }
  }

  public void testTreeSet() {
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    for (boolean option : new boolean[] {true, false}) {
      final TreeSet<MarkerTypeTreeSet> expected = TestSetFactory.createTreeSet(option);
      service.echo(expected, option,
          new AsyncCallback<TreeSet<MarkerTypeTreeSet>>() {
            public void onFailure(Throwable caught) {
              TestSetValidator.rethrowException(caught);
            }

            public void onSuccess(TreeSet<MarkerTypeTreeSet> result) {
              assertNotNull(result);
              assertTrue(TestSetValidator.isValid(expected, result));
              finishTest();
            }
          });
    }
  }

  public void testVector() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final Vector<MarkerTypeVector> expected = TestSetFactory.createVector();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<Vector<MarkerTypeVector>>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Vector<MarkerTypeVector> result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValid(expected, result));
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
