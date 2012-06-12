/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptyKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptyList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptySet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEmptyValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEnum;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeEnumMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashMapKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeHashMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeIdentityHashMapKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeIdentityHashMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashMapKey;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashMapValue;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedHashSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeLinkedList;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeSingleton;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeMap;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeTreeSet;
import com.google.gwt.user.client.rpc.TestSetFactory.MarkerTypeVector;
import com.google.gwt.user.client.rpc.core.java.util.LinkedHashMap_CustomFieldSerializer;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Boolean(false)));
        
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Byte((byte) 0)));
        
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Character('0')));
        
        finishTest();
      }
    });
  }
  
  public void testDateArray() {
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Date()));
        
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Double(0.0)));
        
        finishTest();
      }
    });
  }

  public void testEmptyEnumMap() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> expected =
        TestSetFactory.createEmptyEnumMap();
    delayTestFinishForRpc();
    service.echoEmptyEnumMap(expected, new AsyncCallback<EnumMap<MarkerTypeEnum,
      MarkerTypeEnumMapValue>>() {
        public void onFailure(Throwable caught) {
          TestSetValidator.rethrowException(caught);
        }

        public void onSuccess(EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> result) {
          assertNotNull(result);
          assertTrue(TestSetValidator.isValid(expected, result));
          finishTest();
        }
    });
  }
  
  public void testEmptyList() {
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TestSetFactory.createEmptyList(),
        new AsyncCallback<List<MarkerTypeEmptyList>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(List<MarkerTypeEmptyList> result) {
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
        new AsyncCallback<Map<MarkerTypeEmptyKey, MarkerTypeEmptyValue>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Map<MarkerTypeEmptyKey, MarkerTypeEmptyValue> result) {
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
        new AsyncCallback<Set<MarkerTypeEmptySet>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Set<MarkerTypeEmptySet> result) {
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, TestSetFactory.MarkerTypeEnum.C));
        
        finishTest();
      }
    });
  }

  public void testEnumMapEnumKey() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> expected =
        TestSetFactory.createEnumMapEnumKey();
    delayTestFinishForRpc();
    service.echoEnumKey(expected,
        new AsyncCallback<EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidEnumKey(expected, result));
            finishTest();
          }
        });
  }

  public void testEnumMap() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> expected =
        TestSetFactory.createEnumMap();
    delayTestFinishForRpc();
    service.echo(expected,
        new AsyncCallback<EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(EnumMap<MarkerTypeEnum, MarkerTypeEnumMapValue> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValid(expected, result));
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Float(0.0)));
        
        finishTest();
      }
    });
  }

  public void testHashMap() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final HashMap<MarkerTypeHashMapKey, MarkerTypeHashMapValue> expected = TestSetFactory.createHashMap();
    delayTestFinishForRpc();
    service.echo(expected,
        new AsyncCallback<HashMap<MarkerTypeHashMapKey, MarkerTypeHashMapValue>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(HashMap<MarkerTypeHashMapKey, MarkerTypeHashMapValue> result) {
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

  public void testIdentityHashMapEnumKey() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final IdentityHashMap<MarkerTypeEnum, MarkerTypeIdentityHashMapValue> expected =
        TestSetFactory.createIdentityHashMapEnumKey();
    delayTestFinishForRpc();
    service.echoEnumKey(expected,
        new AsyncCallback<IdentityHashMap<MarkerTypeEnum, MarkerTypeIdentityHashMapValue>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(IdentityHashMap<MarkerTypeEnum, MarkerTypeIdentityHashMapValue> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidEnumKey(expected, result));
            finishTest();
          }
        });
  }

  public void testIdentityHashMap() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final IdentityHashMap<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue> expected =
        TestSetFactory.createIdentityHashMap();
    delayTestFinishForRpc();
    service
        .echo(
            expected,
            new AsyncCallback<IdentityHashMap<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue>>() {
              public void onFailure(Throwable caught) {
                TestSetValidator.rethrowException(caught);
              }

              public void onSuccess(
                  IdentityHashMap<MarkerTypeIdentityHashMapKey, MarkerTypeIdentityHashMapValue> result) {
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Integer(0)));
        
        finishTest();
      }
    });
  }

  public void testLinkedHashMap() {
    CollectionsTestServiceAsync service = getServiceAsync();

    final LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue> expected =
        TestSetFactory.createLinkedHashMap();
    assertFalse(LinkedHashMap_CustomFieldSerializer.getAccessOrderNoReflection(expected));

    delayTestFinishForRpc();
    service.echo(expected,
        new AsyncCallback<LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(
              LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue> result) {
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

    final LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue> expected =
        TestSetFactory.createLRULinkedHashMap();
    assertTrue(LinkedHashMap_CustomFieldSerializer.getAccessOrderNoReflection(expected));

    delayTestFinishForRpc();
    service.echo(expected,
        new AsyncCallback<LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(
              LinkedHashMap<MarkerTypeLinkedHashMapKey, MarkerTypeLinkedHashMapValue> actual) {
            assertNotNull(actual);
            expected.get("SerializableSet");
            actual.get("SerializableSet");
            assertTrue(TestSetValidator.isValid(expected, actual));
            finishTest();
          }
        });
  }

  public void testLinkedHashSet() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final LinkedHashSet<MarkerTypeLinkedHashSet> expected = TestSetFactory.createLinkedHashSet();
    delayTestFinishForRpc();
    service.echo(expected,
        new AsyncCallback<LinkedHashSet<MarkerTypeLinkedHashSet>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(
              LinkedHashSet<MarkerTypeLinkedHashSet> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValid(expected, result));
            finishTest();
          }
        });
  }

  public void testLinkedList() {
    CollectionsTestServiceAsync service = getServiceAsync();
    final LinkedList<MarkerTypeLinkedList> expected = TestSetFactory.createLinkedList();
    delayTestFinishForRpc();
    service.echo(expected, new AsyncCallback<LinkedList<MarkerTypeLinkedList>>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(LinkedList<MarkerTypeLinkedList> result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValid(expected, result));
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Long(0L)));
        
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Short((short) 0)));
         
        finishTest();
      }
    });
  }

  public void testSingletonList() {
    CollectionsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echoSingletonList(TestSetFactory.createSingletonList(),
        new AsyncCallback<List<MarkerTypeSingleton>>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(List<MarkerTypeSingleton> result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidSingletonList(result));
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new java.sql.Date(0L)));
         
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Time(0L)));
         
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new Timestamp(0L)));
         
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new String("")));
         
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
        
        // ensure result preserves meta-data for array store type checking
        assertTrue(TestSetValidator.checkObjectArrayElementAssignment(
            result, 0, new String[4]));
         
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
      collectionsTestService =
          (CollectionsTestServiceAsync) GWT.create(CollectionsTestService.class);
    }
    return collectionsTestService;
  }
}
