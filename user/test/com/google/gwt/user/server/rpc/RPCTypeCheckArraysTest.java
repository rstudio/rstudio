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

package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializedTypeViolationException;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Array test cases for the type checking code found in server-side RPC
 * deserialization.
 */
@SuppressWarnings("rawtypes")
public class RPCTypeCheckArraysTest extends TestCase {

  /*
   * Test that primitive arrays cannot be replaced by non-array types
   *   - int for int[]: testPrimitiveSpoofingArray
   *   - String for String[]: testPrimitiveSpoofingArray
   *   - HashSet for int[]: testHashSetSpoofingArray
   *   - HashSet for String[]: testHashSetSpoofingArray
   *   - HashSet for AClass[]: testHashSetSpoofingClassArray
   * 
   * Test that non-array-type cannot be replaced by array types
   *   - int[] for int: testArraysSpoofingObjects
   *   - String[] for String: testArraytestArraysSpoofingObjects
   *   - String[] for AClass: testArraysSpoofingObjects
   *
   * Test generics parameterized by arrays:
   *   - HashMap<String, int[]>: testArrayAsGeneric
   *   - HashMap<String, Integer[]>: testArrayAsGeneric
   *   - HashMap<String, AClass[]>: testArrayAsGeneric
   *
   * Test arrays of generics:
   *   - List<Integer>[]: testArrayOfGeneric
   *   - List[]: testArrayOfGeneric
   *   - List<? extends HashSet<Integer>>[]: testArrayOfGeneric
   */  
  
  
  /**
   * A class for testing spoofing of arrays.
   */
  public static class ArraysParamTestClass implements RemoteService {
    @SuppressWarnings("unused")
    public static void testArrays(int[] intArg, String[] stringArg) { }

    @SuppressWarnings("unused")
    public static void testAClassArray(RPCTypeCheckTest.AClass[] arg1) { }
    
    @SuppressWarnings("unused")
    public static void testGenericsArrays(
        HashMap<String, int[]> arg1,
        HashMap<String, Integer[]> arg2,
        HashMap<String, RPCTypeCheckTest.AClass[]> arg3) { }
    
    @SuppressWarnings("unused")
    public static void testArrayOfGeneric(List<Integer>[] arg1) { }
    
    @SuppressWarnings("unused")
    public static void testArrayOfGenericRaw(List[] arg1) { }
    
    @SuppressWarnings("unused")
    public static void testArrayOfGenericWildcard(List<? extends HashSet<Integer>>[] arg1) { }
  }

  private static String generateArrayGenericsA() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, "testGenericsArrays");
      
      HashMap<String, HashSet> arg1 = new HashMap<String, HashSet>();
      arg1.put("foo", RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(arg1);
      
      HashMap<String, Integer[]> arg2 = new HashMap<String, Integer[]>();
      Integer[] entry1 = new Integer[] {12345, 67890};
      arg2.put("bar", entry1);
      strFactory.write(arg2);

      HashMap<String, RPCTypeCheckTest.AClass[]> arg3 =
        new HashMap<String, RPCTypeCheckTest.AClass[]>();
      RPCTypeCheckTest.AClass[] entry2 = new RPCTypeCheckTest.AClass[2];
      entry2[0] = new RPCTypeCheckTest.AClass(234);
      entry2[1] = new RPCTypeCheckTest.AClass(567);
      arg3.put("baz", entry2);
      strFactory.write(arg3);

      return strFactory.toString();
      
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateArrayGenericsB() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, "testGenericsArrays");
      
      HashMap<String, int[]> arg1 = new HashMap<String, int[]>();
      int[] entry1 = new int[] { 234, 567 };
      arg1.put("foo", entry1);
      strFactory.write(arg1);
      
      HashMap<String, HashSet[]> arg2 = new HashMap<String, HashSet[]>();
      HashSet[] entry2 = new HashSet[] { RPCTypeCheckFactory.generateTestHashSet() };
      arg2.put("bar", entry2);
      strFactory.write(arg2);

      HashMap<String, RPCTypeCheckTest.AClass[]> arg3 =
        new HashMap<String, RPCTypeCheckTest.AClass[]>();
      RPCTypeCheckTest.AClass[] entry3 = new RPCTypeCheckTest.AClass[2];
      entry3[0] = new RPCTypeCheckTest.AClass(234);
      entry3[1] = new RPCTypeCheckTest.AClass(567);
      arg3.put("baz", entry3);
      strFactory.write(arg3);

      return strFactory.toString();
      
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateArrayGenericsC() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, "testGenericsArrays");
      
      HashMap<String, int[]> arg1 = new HashMap<String, int[]>();
      int[] entry1 = new int[] { 234, 567 };
      arg1.put("foo", entry1);
      strFactory.write(arg1);
      
      HashMap<String, Integer[]> arg2 = new HashMap<String, Integer[]>();
      Integer[] entry2 = new Integer[] {12345, 67890};
      arg2.put("bar", entry2);
      strFactory.write(arg2);

      HashMap<String, HashSet[]> arg3 = new HashMap<String, HashSet[]>();
      HashSet[] entry3 = new HashSet[] { RPCTypeCheckFactory.generateTestHashSet() };
      arg3.put("baz", entry3);
      strFactory.write(arg3);

      return strFactory.toString();
      
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateArrayOfGenericsA(String methodName) {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, methodName);
      
      HashMap<String, HashSet> arg1 = new HashMap<String, HashSet>();
      arg1.put("foo", RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(arg1);
      
      return strFactory.toString();
      
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateArrayOfGenericsB(String methodName) {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, methodName);
      
      LinkedList[] arg1 = new LinkedList[1];
      LinkedList<LinkedHashSet> list = new LinkedList<LinkedHashSet>();
      list.add(RPCTypeCheckFactory.generateTestLinkedHashSet());
      arg1[0] = list;
      strFactory.write(arg1);
      
      return strFactory.toString();
      
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static String generateArrayOfGenericsC(String methodName) {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, methodName);
      
      LinkedList<HashSet<Integer>>[] arg1 = new LinkedList[1];
      LinkedList<HashSet<Integer>> list = new LinkedList<HashSet<Integer>>();
      HashSet<Integer> listElmt = new HashSet<Integer>();
      listElmt.add(12345);
      listElmt.add(67890);
      list.add(listElmt);
      arg1[0] = list;
      strFactory.write(arg1);
      
      return strFactory.toString();
      
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateHashSetSpoofingClassArray() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, "testAClassArray");

      HashSet<Integer> hashSet = new HashSet<Integer>();
      hashSet.add(12345);
      hashSet.add(67890);
      strFactory.write(hashSet);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateHashSetSpoofingIntArray() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, "testArrays");

      HashSet<Integer> hashSet = new HashSet<Integer>();
      hashSet.add(12345);
      hashSet.add(67890);
      strFactory.write(hashSet);

      String[] stringArray = new String[]{"foo", "bar"};
      strFactory.write(stringArray);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateHashSetSpoofingStringArray() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, "testArrays");

      int[] intArray = new int[]{12345, 67890};
      strFactory.write(intArray);

      HashSet<Integer> hashSet = new HashSet<Integer>();
      hashSet.add(12345);
      hashSet.add(67890);
      strFactory.write(hashSet);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateIntArraySpoofingInt() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(RPCTypeCheckTest.PrimitiveParamTestClass.class, "testIntString");

      int[] iArray = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      strFactory.write(iArray);

      strFactory.write("foo");

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateIntSpoofingIntArray() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, "testArrays");

      int i = 12345;
      strFactory.write(i);

      String[] stringArray = new String[]{"foo", "bar"};
      strFactory.write(stringArray);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }
  
  private static String generateStringArraySpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(RPCTypeCheckTest.ClassesParamTestClass.class, "testAClass");

      String a = "a";
      String[] stringArray = new String[]{a, a, a, a, a, a, a, a, a, a, a, a, a, a};
      strFactory.write(stringArray);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateStringArraySpoofingString() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(RPCTypeCheckTest.PrimitiveParamTestClass.class, "testIntString");

      int i = 12345;
      strFactory.write(i);

      String a = "a";
      String[] stringArray = new String[]{a, a, a, a, a, a, a, a, a, a, a, a, a, a};
      strFactory.write(stringArray);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateStringSpoofingStringArray() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ArraysParamTestClass.class, "testArrays");

      int[] arg1 = new int[]{12345, 67890};
      strFactory.write(arg1);

      String arg2 = "bar";
      strFactory.write(arg2);

      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  /**
   * This checks that arrays as generic types are checked correctly.
   */
  public void testArrayGenerics() {
    try {
      RPC.decodeRequest(generateArrayGenericsA());
      fail("Expected IncompatibleRemoteServiceException from testArrayGenerics (1)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*int.*"));
    }
    try {
      RPC.decodeRequest(generateArrayGenericsB());
      fail("Expected IncompatibleRemoteServiceException from testArrayGenerics (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateArrayGenericsC());
      fail("Expected IncompatibleRemoteServiceException from testArrayGenerics (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*AClass.*"));
    }
  }

  /**
   * This checks that arrays of generic types are checked correctly.
   */
  public void testArrayOfGenerics() {
    try {
      RPC.decodeRequest(generateArrayOfGenericsC("testArrayOfGenericRaw"));
      // Expect to pass
    } catch (Exception e) {
      fail("Unexpected exception from testArrayOfGenerics (0): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateArrayOfGenericsA("testArrayOfGeneric"));
      fail("Expected IncompatibleRemoteServiceException from testArrayOfGenerics (1)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashMap.*List.*"));
    }
    try {
      RPC.decodeRequest(generateArrayOfGenericsB("testArrayOfGeneric"));
      fail("Expected IncompatibleRemoteServiceException from testArrayOfGenerics (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateArrayOfGenericsA("testArrayOfGenericWildcard"));
      fail("Expected IncompatibleRemoteServiceException from testArrayOfGenerics (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashMap.*List.*"));
    }
    try {
      RPC.decodeRequest(generateArrayOfGenericsC("testArrayOfGenericWildcard"));
    } catch (Exception e) {
      fail("Unexpected Exception from testArrayOfGenerics (4): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateArrayOfGenericsB("testArrayOfGenericWildcard"));
      fail("Expected IncompatibleRemoteServiceException from testArrayOfGenerics (5)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
  }

  /**
   * This checks that arrays cannot be used in place of primitives.
   */
  public void testArraysSpoofingObjects() {
    try {
      RPC.decodeRequest(generateIntArraySpoofingInt());
      fail("Expected ArrayIndexOutOfBoundsException from testArraysSpoofingPrimitives (1)");
    } catch (Exception e) {
      // Expected to get here
      assertTrue(e instanceof ArrayIndexOutOfBoundsException);
    }
    try {
      RPCRequest result = RPC.decodeRequest(generateStringArraySpoofingString());
      assertTrue(result.getParameters()[1].toString().matches(".*\\[Ljava.lang.String.*"));
    } catch (Exception e) {
      fail("Unexpected Exception from testArraysSpoofingPrimitives (2)");
    }
    try {
      RPC.decodeRequest(generateStringArraySpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testArraysSpoofingPrimitives (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*\\[Ljava.lang.String.*AClass.*"));
    }
  }

  /**
   * This checks situations in which an RPC message is modified to replace
   * arguments of a primitive type with another primitive type.
   */
  public void testHashSetSpoofingArray() {
    try {
      RPC.decodeRequest(generateHashSetSpoofingIntArray());
      fail("Expected IncompatibleRemoteServiceException from testHashSetSpoofingArray (1)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here. When the int array is processed, it reads the
      // HashSet class and fails to know what it is, because it is not the
      // way an int would be serialized in this context.
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*\\[I.*"));
    }
    try {
      RPC.decodeRequest(generateHashSetSpoofingStringArray());
      fail("Expected IncompatibleRemoteServiceException from testHashSetSpoofingArray (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
  }

  /**
   * This checks situations in which an RPC message is modified to replace an
   * array of objects with a HashSet (custom serialized container type).
   */
  public void testHashSetSpoofingClassArray() {
    try {
      RPC.decodeRequest(generateHashSetSpoofingClassArray());
      fail("Expected IncompatibleRemoteServiceException from testHashSetSpoofingClassArray");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here.
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*AClass.*"));
    }
  }

  /**
   * This checks situations in which an RPC message is modified to replace
   * arguments of a primitive type with another primitive type.
   */
  public void testPrimitiveSpoofingArray() {
    try {
      RPC.decodeRequest(generateIntSpoofingIntArray());
      fail("Expected IncompatibleRemoteServiceException from testPrimitiveSpoofingArray (1)");
    } catch (Exception e) {
      // Expected to get here. When the int array value is processed, it reads
      // the
      // integer value and tries to look it up as a class string, only to run
      // out
      // of bounds in the string table.
      assertTrue(e instanceof ArrayIndexOutOfBoundsException);
    }
    try {
      RPC.decodeRequest(generateStringSpoofingStringArray());
      fail("Expected IncompatibleRemoteServiceException from testPrimitiveSpoofingArray (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializationException.class, e.getCause().getClass());
    }
  }
}
