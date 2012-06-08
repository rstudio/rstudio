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
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializedTypeViolationException;
import com.google.gwt.user.client.rpc.TestSetFactory.ReverseSorter;
import com.google.gwt.user.server.rpc.RPCTypeCheckCollectionsTest.TestHashSet;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Test cases for the type checking code found in server-side RPC
 * deserialization.
 */
public class RPCTypeCheckTest extends TestCase {

  /*
   * Test that collection classes correctly report when they are not the
   * expected type, when the other type is a class.
   *   - ArrayList<Integer>: testArrayListSpoofingClass
   *   - ArraysAsList<Integer>: testArraysAsListSpoofingClass
   *   - EmptyList<Integer>: testEmptyListSpoofingClass
   *   - EmptyMap<String, Integer>: testEmptyMapSpoofingClass
   *   - EmptySet<Integer>: testEmptySetSpoofingClass
   *   - HashMap<String, Integer>: testHashMapSpoofingClass
   *   - HashSet<Integer>: testHashSetSpoofingClass
   *   - IdentityHashMap<AClass, Integer>: testIdentityHashMapSpoofingClass
   *   - LinkedHashMap<String, Integer>: testLinkedHashMapSpoofingClass
   *   - LinkedHashSet<Integer>: testHashSetSpoofingClass
   *   - LinkedList<Integer>: testLinkedListSpoofingClass
   *   - SingletonList<Integer>: testSingletonListSpoofingClass
   *   - TreeMap<String, Integer>: testTreeMapSpoofingClass
   *   - TreeSet<Integer>: testTreeSetSpoofingClass
   *   - Vector<Integer>: testVectorSpoofingClass
   * 
   * Test that primitive classes cannot be interchanged, or can safely. In
   * general, these do not pose much danger as the serialization for basic types
   * all perform a strictly finite amount of work and catch invalid 
   * representations.
   *   - Integer for String, and vice versa: testPrimitiveSpoofing
   *   - int for String, and String for int: testValueSpoofing
   *   
   * Test collections in fields of classes:
   *   - BClass contains a List<Integer>: testClassField
   *   - CClass contains a BClass: testClassField
   *   - KClass<X> contains a DClass<X, Integer> and List<X>: testGenericFields
   *   
   * Test generic classes that use their generic types in various ways
   *   - DClass<X, Y> extends LinkedList<X>: testGenericClasses
   *   - EClass<X, Y> { HashMap<X, Y> ... }: testGenericClasses
   *   - FClass<X, Y> { List<X> ... List<Y> ... }: testGenericClasses
   *   - GClass<X, Y> { DClass<X, Y>, ... }: testNestedGenericClasses
   *   - JClass<X, Y> { fields of HashMap }: testGenericFields
   *      
   * 
   * Wildcard types
   *   - (IClass<? extends T> arg1) with
   *      <T extends Set<? super GEClass<Integer, String>>>: testComplexGenerics
   *   - LClass<T implements List<X> & MClass<Y>>: testMethodClassGenerics 
   */  
  
  /**
   * Test custom class.
   */
  public static class AClass extends Object implements IsSerializable {
    int a = 9876;

    AClass() {
    }

    AClass(int aVal) {
      a = aVal;
    }

    int getA() {
      return a;
    }

    void setA(int newA) {
      a = newA;
    }
  }

  /**
   * Test custom class containing a parameterized collection.
   */
  public static class BClass extends Object implements IsSerializable {
    List<Integer> aList = new LinkedList<Integer>();

    BClass() {
    }

    BClass(int aVal) {
      aList.add(aVal);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setList(List newValue) {
      aList = newValue;
    }
  }

  /**
   * Test custom class containing a raw collection.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static class BClassRaw extends Object implements IsSerializable {
    List aList = new LinkedList();

    BClassRaw() {
    }

    BClassRaw(int aVal) {
      aList.add(aVal);
    }

    public void setList(List newValue) {
      aList = newValue;
    }
  }

  /**
   * Test custom class containing a subclass containing a parameterized
   * collection.
   */
  public static class CClass extends Object implements IsSerializable {
    BClass b = new BClass(12345);

    CClass() {
    }

    public void setBClass(BClass newValue) {
      b = newValue;
    }
  }

  /**
   * Test custom class containing a subclass containing a raw
   * collection.
   */
  public static class CClassRaw extends Object implements IsSerializable {
    BClassRaw b = new BClassRaw(12345);

    CClassRaw() {
    }

    public void setBClass(BClassRaw newValue) {
      b = newValue;
    }
  }

  /**
   * A class containing a method used to check type spoofing attacks on RPC
   * messages containing novel classes.
   */
  public static class ClassesParamTestClass<T extends List<Integer> & MInterface<String>>
      implements RemoteService {
    @SuppressWarnings("unused")
    public static void testAClass(AClass arg1) {
    }

    @SuppressWarnings("unused")
    public static void testAClassArray(AClass[] arg1) {
    }

    @SuppressWarnings("unused")
    public static void testBClass(BClass arg1) {
    }

    @SuppressWarnings("unused")
    public static void testBClassRaw(BClassRaw arg1) {
    }

    @SuppressWarnings("unused")
    public static void testCClass(CClass arg1) {
    }

    @SuppressWarnings("unused")
    public static void testCClassRaw(CClassRaw arg1) {
    }

    @SuppressWarnings("unused")
    public static <T extends Set<? super GEClass<Integer, String>>> void testComplexGenerics(
        IClass<? extends T> arg1) {
    }

    @SuppressWarnings({"unused", "rawtypes"})
    public static <T extends Set<? super GEClass>> void testComplexGenericsRaw(
        IClass<? extends T> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testDClass(DClass<Integer, String> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testDClassRaw(DClassRaw<String> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testEClass(EClass<String, Integer> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testFClass(FClass<Integer, String> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testFClassRaw(FClassRaw<Integer> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testGDClass(GDClass<Integer, String> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testGDClassRaw1(GDClassRaw arg1) {
    }

    @SuppressWarnings({"unused", "rawtypes"})
    public static void testGDClassRaw2(GDClass arg1) {
    }

    @SuppressWarnings("unused")
    public static void testGEClass(GEClass<Integer, String> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testHDClass(HDClass<Integer, String> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testHEClass(HEClass<Integer, String> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testJClass(JClass<Integer, String> arg1) {
    }

    @SuppressWarnings("unused")
    public static void testJClassRaw1(JClassRaw<Integer, String> arg1) {
    }

    @SuppressWarnings({"unused", "rawtypes"})
    public static void testJClassRaw2(JClass arg1) {
    }

    @SuppressWarnings({"unused"})
    public static void testKClass(KClass<String> arg1) {
    }

    @SuppressWarnings({"unused", "rawtypes"})
    public static void testKClassRaw(KClass arg1) {
    }

    @SuppressWarnings("unused")
    public void testWildcardBounds(T arg1) {
    }
  }

  /**
   * Test custom parameterized class that extends a parameterized class and
   * includes a field of a parameterized type.
   */
  public static class DClass<X, Y> extends LinkedList<X> implements IsSerializable {
    Y y;

    DClass() {
      y = null;
    }

    void setY(Y value) {
      y = value;
    }
  }

  /**
   * Test custom parameterized class that extends a parameterized class and
   * includes a field of a parameterized type, with no actual parameters (raw).
   */
  @SuppressWarnings("rawtypes")
  public static class DClassRaw<Y> extends LinkedList implements IsSerializable {
    Y y;

    DClassRaw() {
      y = null;
    }

    void setY(Y value) {
      y = value;
    }
  }

  /**
   * Test custom parameterized class that includes a field of a parameterized
   * type, with two types.
   */
  public static class EClass<X, Y> extends Object implements IsSerializable {
    HashMap<X, Y> map;

    EClass() {
      map = null;
    }

    void setMap(HashMap<X, Y> value) {
      map = value;
    }
  }

  /**
   * Test custom parameterized class that includes two fields, each using one of
   * the parameterized types.
   */
  public static class FClass<X, Y> extends Object implements IsSerializable {
    List<X> listX;
    List<Y> listY;

    FClass() {
      listX = null;
      listY = null;
    }

    void setX(List<X> value) {
      listX = value;
    }

    void setY(List<Y> value) {
      listY = value;
    }
  }

  /**
   * Test custom parameterized class that includes two fields, one using the
   * parameter type and the other raw. We wish to ensure that the raw type
   * does not feel the need to be the parameter type.
   */
  @SuppressWarnings("rawtypes")
  public static class FClassRaw<X> extends Object implements IsSerializable {
    List<X> listX;
    List listY;

    FClassRaw() {
      listX = null;
      listY = null;
    }

    void setX(List<X> value) {
      listX = value;
    }

    void setY(List value) {
      listY = value;
    }
  }

  /**
   * Test custom parameterized class that extends another custom parameterized
   * class that extends another parameterized collection.
   */
  public static class GDClass<A, B> extends DClass<A, B> {
  }

  /**
   * Test custom parameterized class that extends another custom parameterized
   * class that extends another parameterized collection, raw version.
   */
  @SuppressWarnings("rawtypes")
  public static class GDClassRaw extends DClass {
  }

  /**
   * Test custom parameterized class that extends another custom parameterized
   * class that has a parameterized collection for a field.
   */
  public static class GEClass<A, B> extends EClass<B, A> {
  }

  /**
   * Test custom parameterized class that uses another custom parameterized
   * class that extends another parameterized collection.
   */
  public static class HDClass<A, B> extends Object implements IsSerializable {
    public DClass<A, B> dClass = null;
  }

  /**
   * Test custom parameterized class that uses another custom parameterized
   * class that has a parameterized collection for a field.
   */
  public static class HEClass<A, B> extends Object implements IsSerializable {
    public EClass<B, A> eClass = null;
  }

  /**
   * Test class that just holds a single field of a generic type.
   */
  public static class IClass<T> extends Object implements IsSerializable {
    T a;

    IClass() {
      a = null;
    }

    IClass(T aVal) {
      a = aVal;
    }

    T getA() {
      return a;
    }

    void setA(T newA) {
      a = newA;
    }
  }

  /**
   * Test class that has fields of the same underlying type but different actual
   * parameters for each type.
   */
  public static class JClass<K, V> extends EClass<K, V> {
    public HashMap<Long, Double> field1 = null;
    public EClass<Short, Float> field2 = null;
    public HashMap<K, V> field3 = null;
    public HashMap<String, Integer> field4 = null;
  }

  /**
   * Test class that has fields of the same underlying type but different actual
   * parameters for each type, including raw types.
   */
  @SuppressWarnings("rawtypes")
  public static class JClassRaw<K, V> extends EClass {
    public HashMap<Long, Double> field1 = null;
    public EClass<Short, Float> field2 = null;
    public HashMap<K, V> field3 = null;
    public HashMap field4 = null;
  }

  /**
   * Test case for a generic type that extends a generic type with fewer
   * parameters, used as a field and as an array.
   * 
   * During testing, instances of DClass are used for all of the fields.
   */
  public static class KClass<X> implements IsSerializable {
    public DClass<X, Integer> field1 = null;
    public List<X> field2 = null;
    public DClass<X, Integer>[] field3 = null;
    public List<X>[] field4 = null;
  }

  /**
   * Test case for multiple wildcard bounds.
   */
  public static class LClass<X, Y> extends LinkedList<X>
      implements MInterface<Y>, IsSerializable {
    public Y field1 = null;
    public List<Y> field2 = null;
    
    @Override
    public Y echo(Y arg) {
      return arg;
    }
  }

  /**
   * An invalid test case for multiple wildcard bounds.
   * 
   * It does not implement List.
   */
  public static class LClassInvalid1<X, Y> extends IClass<X>
      implements MInterface<Y> {
    public Y field1 = null;
    public List<Y> field2 = null;
    
    @Override
    public Y echo(Y arg) {
      return arg;
    }
  }

  /**
   * An invalid test case for multiple wildcard bounds.
   * 
   * It does not implement MInterface.
   */
  public static class LClassInvalid2<X, Y> extends LinkedList<X> implements IsSerializable {
    public Y field1 = null;
    public List<Y> field2 = null;
  }

  /**
   * Test interface for multiple wildcard bounds.
   */
  public interface MInterface<X> {
    X echo(X arg);
  }

  /**
   * A class containing a method used to check type spoofing attacks on RPC
   * messages containing primitive types.
   */
  public static class PrimitiveParamTestClass implements RemoteService {
    @SuppressWarnings("unused")
    public static void testIntegerString(Integer arg1, String arg2) {
    }

    @SuppressWarnings("unused")
    public static void testIntString(int arg1, String arg2) {
    }
  }

  private static String generateArrayListSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      ArrayList<Integer> arrayList = new ArrayList<Integer>();
      arrayList.add(12345);
      arrayList.add(67890);
      strFactory.write(arrayList);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateArraysAsListSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      List<Integer> arrayAsList = Arrays.asList(12345, 67890);
      strFactory.write(arrayAsList);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateBClassSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testBClass");

      BClass arg1 = new BClass();
      LinkedList<HashSet> list = new LinkedList<HashSet>();
      list.add(RPCTypeCheckFactory.generateTestHashSet());
      arg1.setList(list);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateBClassRaw() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testBClassRaw");

      BClassRaw arg1 = new BClassRaw();
      LinkedList<String> list = new LinkedList<String>();
      list.add("foo");
      arg1.setList(list);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateCClassSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testCClass");

      CClass arg1 = new CClass();
      BClass b = new BClass();
      LinkedList<HashSet> list = new LinkedList<HashSet>();
      list.add(RPCTypeCheckFactory.generateTestHashSet());
      b.setList(list);
      arg1.setBClass(b);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateCClassRaw() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testCClassRaw");

      CClassRaw arg1 = new CClassRaw();
      BClassRaw b = new BClassRaw();
      LinkedList<Integer> list = new LinkedList<Integer>();
      list.add(9876);
      b.setList(list);
      arg1.setBClass(b);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static String generateDClassRaw() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testDClassRaw");

      DClassRaw<String> arg1 = new DClassRaw<String>();
      arg1.setY("foo");
      arg1.add(12345);
      strFactory.write((Object) arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateDClassXSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testDClass");

      DClass<HashSet, String> arg1 = new DClass<HashSet, String>();
      arg1.setY("foo");
      arg1.add(RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write((Object) arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateDClassYSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testDClass");

      DClass<Integer, HashSet> arg1 = new DClass<Integer, HashSet>();
      arg1.setY(RPCTypeCheckFactory.generateTestHashSet());
      arg1.add(12345);
      strFactory.write((Object) arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateEClassXSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testEClass");

      EClass<HashSet, Integer> arg1 = new EClass<HashSet, Integer>();
      arg1.setMap(RPCTypeCheckFactory.generateTestHashMap());
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateEClassYSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testEClass");

      EClass<String, HashSet> arg1 = new EClass<String, HashSet>();
      HashMap<String, HashSet> map = new HashMap<String, HashSet>();
      map.put("foo", RPCTypeCheckFactory.generateTestHashSet());
      arg1.setMap(map);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateEmptyListSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      strFactory.writeEmptyList();

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateEmptyMapSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      strFactory.writeEmptyMap();

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateEmptySetSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      strFactory.writeEmptySet();

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateFClassRaw() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testFClassRaw");

      FClassRaw<Integer> arg1 = new FClassRaw<Integer>();
      LinkedList<Integer> fieldX = new LinkedList<Integer>();
      fieldX.add(12345);
      LinkedList<String> fieldY = new LinkedList<String>();
      fieldY.add("foo");
      arg1.setX(fieldX);
      arg1.setY(fieldY);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateFClassXSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testFClass");

      FClass<HashSet, String> arg1 = new FClass<HashSet, String>();
      LinkedList<HashSet> fieldX = new LinkedList<HashSet>();
      fieldX.add(RPCTypeCheckFactory.generateTestHashSet());
      LinkedList<String> fieldY = new LinkedList<String>();
      fieldY.add("foo");
      arg1.setX(fieldX);
      arg1.setY(fieldY);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateFClassYSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testFClass");

      FClass<Integer, HashSet> arg1 = new FClass<Integer, HashSet>();
      LinkedList<Integer> fieldX = new LinkedList<Integer>();
      fieldX.add(12345);
      LinkedList<HashSet> fieldY = new LinkedList<HashSet>();
      fieldY.add(RPCTypeCheckFactory.generateTestHashSet());
      arg1.setX(fieldX);
      arg1.setY(fieldY);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static String generateGDClassRaw1() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testGDClassRaw1");

      GDClassRaw gClass = new GDClassRaw();
      gClass.setY("foo");
      gClass.add(12345);
      strFactory.write((Object) gClass);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateGDClassRaw2() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testGDClassRaw2");

      GDClass gClass = new GDClass();
      gClass.setY("foo");
      gClass.add(12345);
      strFactory.write((Object) gClass);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateGDClassXSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testGDClass");

      GDClass<HashSet, String> gClass = new GDClass<HashSet, String>();
      gClass.setY("foo");
      gClass.add(RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write((Object) gClass);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateGDClassYSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testGDClass");

      GDClass<Integer, HashSet> gClass = new GDClass<Integer, HashSet>();
      gClass.setY(RPCTypeCheckFactory.generateTestHashSet());
      gClass.add(12345);
      strFactory.write((Object) gClass);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"cast", "rawtypes"})
  private static String generateGEClassXSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testGEClass");

      GEClass<HashSet, String> arg1 = new GEClass<HashSet, String>();
      HashMap<String, HashSet> map = new HashMap<String, HashSet>();
      map.put("foo", RPCTypeCheckFactory.generateTestHashSet());
      arg1.setMap(map);
      strFactory.write((Object) arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes", "cast"})
  private static String generateGEClassYSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testGEClass");

      GEClass<Integer, HashSet> arg1 = new GEClass<Integer, HashSet>();
      arg1.setMap(RPCTypeCheckFactory.generateTestHashMap());
      strFactory.write((Object) arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateHashMapSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
      hashMap.put("foo", 12345);
      strFactory.write(hashMap);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateHashSetSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

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

  @SuppressWarnings({"rawtypes"})
  private static String generateHDClassXSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testHDClass");

      HDClass<HashSet, String> hClass = new HDClass<HashSet, String>();
      hClass.dClass = new DClass<HashSet, String>();
      hClass.dClass.setY("foo");
      hClass.dClass.add(RPCTypeCheckFactory.generateTestHashSet());
      strFactory.write(hClass);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateHDClassYSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testHDClass");

      HDClass<Integer, HashSet> hClass = new HDClass<Integer, HashSet>();
      hClass.dClass = new DClass<Integer, HashSet>();
      hClass.dClass.setY(RPCTypeCheckFactory.generateTestHashSet());
      hClass.dClass.add(12345);
      strFactory.write(hClass);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateHEClassXSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testHEClass");

      HEClass<HashSet, String> arg1 = new HEClass<HashSet, String>();
      arg1.eClass = new EClass<String, HashSet>();
      HashMap<String, HashSet> map = new HashMap<String, HashSet>();
      map.put("foo", RPCTypeCheckFactory.generateTestHashSet());
      arg1.eClass.setMap(map);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateHEClassYSpoofing() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testHEClass");

      HEClass<Integer, HashSet> arg1 = new HEClass<Integer, HashSet>();
      arg1.eClass = new EClass<HashSet, Integer>();
      arg1.eClass.setMap(RPCTypeCheckFactory.generateTestHashMap());
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateIClassRaw() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testComplexGenericsRaw");

      /*
       * Generate an object that is valid for IClass<? extends T> where <T extends
       * Set<? super GEClass>>.
       * 
       * EClass is a superclass of GEClass, so we can use that in the thing that
       * extends Set.
       */
      HashMap<Integer, String> eClassField = new HashMap<Integer, String>();
      eClassField.put(12345, "foo");
      EClass<Integer, String> geClass = new EClass<Integer, String>();
      geClass.setMap(eClassField);
      TestHashSet<EClass<Integer, String>> set = new TestHashSet<EClass<Integer, String>>();
      set.add(geClass);
      IClass<TestHashSet<EClass<Integer, String>>> arg1 =
          new IClass<TestHashSet<EClass<Integer, String>>>(set);

      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateIClassSpoofingA() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testComplexGenerics");

      /*
       * Generate an object that spoofs IClass<? extends T> where <T extends
       * Set<? super GEClass<Integer, String>>> at the highest level (i.e.
       * something that does not extend Set).
       * 
       * EClass is a superclass of GEClass, so we can use that in the thing that
       * extends Set. But it switched the parameter order, so we need
       * EClass<String, Integer>.
       * 
       * LinkedList does not extend Set.
       * 
       * So we need to construct this: IClass<LinkedList<EClass<String,
       * Integer>>> arg1
       */
      HashMap<String, Integer> eClassField = new HashMap<String, Integer>();
      eClassField.put("foo", 12345);
      eClassField.put("bar", 67890);
      EClass<String, Integer> geClass = new EClass<String, Integer>();
      geClass.setMap(eClassField);
      LinkedList<EClass<String, Integer>> list = new LinkedList<EClass<String, Integer>>();
      list.add(geClass);
      IClass<LinkedList<EClass<String, Integer>>> arg1 =
          new IClass<LinkedList<EClass<String, Integer>>>(list);

      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateIClassSpoofingB() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testComplexGenerics");

      /*
       * Generate an object that spoofs IClass<? extends T> where <T extends
       * Set<? super GEClass<Integer, String>>> at the second highest level
       * (i.e. something that is not a superclass of GEClass).
       * 
       * Pathological hash set is not a superclass of GEClass, so we can use
       * that.
       * 
       * So we need to construct this: IClass<TestHashSet<HashSet<Integer,
       * String>>> arg1
       */
      HashSet hashSet = RPCTypeCheckFactory.generateTestHashSetHashSet();
      IClass<HashSet> arg1 = new IClass<HashSet>(hashSet);

      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static String generateIClassSpoofingC() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testComplexGenerics");

      /*
       * Generate an object that spoofs IClass<? extends T> where <T extends
       * Set<? super GEClass<Integer, String>>> at the deepest level (i.e.
       * GEClass parameter types).
       * 
       * EClass is a superclass of GEClass, so we can use that in the thing that
       * extends Set. But it switched the parameter order, so we need
       * EClass<String, HashSet>.
       */
      HashMap<String, HashSet> eClassField = new HashMap<String, HashSet>();
      eClassField.put("foo", RPCTypeCheckFactory.generateTestHashSet());
      EClass<String, HashSet> geClass = new EClass<String, HashSet>();
      geClass.setMap(eClassField);
      TestHashSet<EClass<String, HashSet>> set = new TestHashSet<EClass<String, HashSet>>();
      set.add(geClass);
      IClass<TestHashSet<EClass<String, HashSet>>> arg1 =
          new IClass<TestHashSet<EClass<String, HashSet>>>(set);

      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateIClassValid() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testComplexGenerics");

      /*
       * Generate an object that works for IClass<? extends T> where <T extends
       * Set<? super GEClass<Integer, String>>>.
       * 
       * EClass is a superclass of GEClass, so we can use that in the thing that
       * extends Set. But it switched the parameter order, so we need
       * EClass<String, Integer>.
       * 
       * HashSet extends Set.
       * 
       * RPCTypeCheckCollectionsTest.TestHashSet extends HashSet.
       * 
       * So we need to construct this: IClass<TestHashSet<EClass<String,
       * Integer>>> arg1
       */
      HashMap<String, Integer> eClassField = new HashMap<String, Integer>();
      eClassField.put("foo", 12345);
      eClassField.put("bar", 67890);
      EClass<String, Integer> geClass = new EClass<String, Integer>();
      geClass.setMap(eClassField);
      TestHashSet<EClass<String, Integer>> set = new TestHashSet<EClass<String, Integer>>();
      set.add(geClass);
      IClass<TestHashSet<EClass<String, Integer>>> arg1 =
          new IClass<TestHashSet<EClass<String, Integer>>>(set);

      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateIdentityHashMapSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      IdentityHashMap<String, Integer> hashMap = new IdentityHashMap<String, Integer>();
      hashMap.put("foo", 12345);
      strFactory.write(hashMap);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateIntegerSpoofingString() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(PrimitiveParamTestClass.class, "testIntegerString");

      Integer i = 12345;
      strFactory.write(i);

      Integer j = 67890;
      strFactory.write(j);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateIntSpoofingString() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(PrimitiveParamTestClass.class, "testIntString");

      int i = 12345;
      strFactory.write(i);

      int j = 67890;
      strFactory.write(j);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static String generateJClassRaw1() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testJClassRaw1");

      JClassRaw<Integer, String> arg1 = new JClassRaw<Integer, String>();
      arg1.field1 = new HashMap<Long, Double>();
      arg1.field1.put(123L, 0.12345);
      arg1.field2 = new EClass<Short, Float>();
      HashMap<Short, Float> eClassMap = new HashMap<Short, Float>();
      eClassMap.put((short) 567, 0.456f);
      arg1.field2.setMap(eClassMap);
      arg1.field3 = new HashMap<Integer, String>();
      arg1.field3.put(9876, "foo");
      arg1.field4 = new HashMap<String, Integer>();
      arg1.field4.put("bar", 765);
      HashMap<Integer, String> jClassMap = new HashMap<Integer, String>();
      jClassMap.put(4321, "baz");
      arg1.setMap(jClassMap);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateJClassRaw2() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testJClassRaw2");

      JClass arg1 = new JClass();
      arg1.field1 = new HashMap<Long, Double>();
      arg1.field1.put(123L, 0.12345);
      arg1.field2 = new EClass<Short, Float>();
      HashMap<Short, Float> eClassMap = new HashMap<Short, Float>();
      eClassMap.put((short) 567, 0.456f);
      arg1.field2.setMap(eClassMap);
      arg1.field3 = new HashMap<Integer, String>();
      arg1.field3.put(9876, "foo");
      arg1.field4 = new HashMap<String, Integer>();
      arg1.field4.put("bar", 765);
      HashMap<Integer, String> jClassMap = new HashMap<Integer, String>();
      jClassMap.put(4321, "baz");
      arg1.setMap(jClassMap);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateJClassSpoofing1() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testJClass");

      JClass<Integer, String> arg1 = new JClass<Integer, String>();
      arg1.field1 = (HashMap) RPCTypeCheckFactory.generateTestHashMap();
      arg1.field2 = new EClass<Short, Float>();
      HashMap<Short, Float> eClassMap = new HashMap<Short, Float>();
      eClassMap.put((short) 567, 0.456f);
      arg1.field2.setMap(eClassMap);
      arg1.field3 = new HashMap<Integer, String>();
      arg1.field3.put(9876, "foo");
      arg1.field4 = new HashMap<String, Integer>();
      arg1.field4.put("bar", 765);
      HashMap<Integer, String> jClassMap = new HashMap<Integer, String>();
      jClassMap.put(4321, "baz");
      arg1.setMap(jClassMap);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateJClassSpoofing2() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testJClass");

      JClass<Integer, String> arg1 = new JClass<Integer, String>();
      arg1.field1 = new HashMap<Long, Double>();
      arg1.field1.put(123L, 0.12345);
      arg1.field2 = new EClass<Short, Float>();
      arg1.field2.setMap((HashMap) RPCTypeCheckFactory.generateTestHashMap());
      arg1.field3 = new HashMap<Integer, String>();
      arg1.field3.put(9876, "foo");
      arg1.field4 = new HashMap<String, Integer>();
      arg1.field4.put("bar", 765);
      HashMap<Integer, String> jClassMap = new HashMap<Integer, String>();
      jClassMap.put(4321, "baz");
      arg1.setMap(jClassMap);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateJClassSpoofing3() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testJClass");

      JClass<Integer, String> arg1 = new JClass<Integer, String>();
      arg1.field1 = new HashMap<Long, Double>();
      arg1.field1.put(123L, 0.12345);
      arg1.field2 = new EClass<Short, Float>();
      HashMap<Short, Float> eClassMap = new HashMap<Short, Float>();
      eClassMap.put((short) 567, 0.456f);
      arg1.field2.setMap(eClassMap);
      arg1.field3 = (HashMap) RPCTypeCheckFactory.generateTestHashMap();
      arg1.field4 = new HashMap<String, Integer>();
      arg1.field4.put("bar", 765);
      HashMap<Integer, String> jClassMap = new HashMap<Integer, String>();
      jClassMap.put(4321, "baz");
      arg1.setMap(jClassMap);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateJClassSpoofing4() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testJClass");

      JClass<Integer, String> arg1 = new JClass<Integer, String>();
      arg1.field1 = new HashMap<Long, Double>();
      arg1.field1.put(123L, 0.12345);
      arg1.field2 = new EClass<Short, Float>();
      HashMap<Short, Float> eClassMap = new HashMap<Short, Float>();
      eClassMap.put((short) 567, 0.456f);
      arg1.field2.setMap(eClassMap);
      arg1.field3 = new HashMap<Integer, String>();
      arg1.field3.put(9876, "foo");
      arg1.field4 = (HashMap) RPCTypeCheckFactory.generateTestHashMap();
      HashMap<Integer, String> jClassMap = new HashMap<Integer, String>();
      jClassMap.put(4321, "baz");
      arg1.setMap(jClassMap);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateJClassSpoofing5() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testJClass");

      JClass<Integer, String> arg1 = new JClass<Integer, String>();
      arg1.field1 = new HashMap<Long, Double>();
      arg1.field1.put(123L, 0.12345);
      arg1.field2 = new EClass<Short, Float>();
      HashMap<Short, Float> eClassMap = new HashMap<Short, Float>();
      eClassMap.put((short) 567, 0.456f);
      arg1.field2.setMap(eClassMap);
      arg1.field3 = new HashMap<Integer, String>();
      arg1.field3.put(9876, "foo");
      arg1.field4 = new HashMap<String, Integer>();
      arg1.field4.put("bar", 765);
      arg1.setMap((HashMap) RPCTypeCheckFactory.generateTestHashMap());
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateJClassValid() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testJClass");

      JClass<Integer, String> arg1 = new JClass<Integer, String>();
      arg1.field1 = new HashMap<Long, Double>();
      arg1.field1.put(123L, 0.12345);
      arg1.field2 = new EClass<Short, Float>();
      HashMap<Short, Float> eClassMap = new HashMap<Short, Float>();
      eClassMap.put((short) 567, 0.456f);
      arg1.field2.setMap(eClassMap);
      arg1.field3 = new HashMap<Integer, String>();
      arg1.field3.put(9876, "foo");
      arg1.field4 = new HashMap<String, Integer>();
      arg1.field4.put("bar", 765);
      HashMap<Integer, String> jClassMap = new HashMap<Integer, String>();
      jClassMap.put(4321, "baz");
      arg1.setMap(jClassMap);
      strFactory.write(arg1);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static String generateKClassValid(String methodName) {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, methodName);
    
      DClass<String, Integer> field1 = new DClass<String, Integer>();
      field1.setY(12);
      field1.add("foo");

      DClass<String, Integer> field2 = new DClass<String, Integer>();
      field2.setY(34);
      field2.add("bar");

      DClass<String, Integer>[] field3 = new DClass[1];
      field3[0] = new DClass<String, Integer>();
      field3[0].setY(56);
      field3[0].add("oof");

      DClass<String, Integer>[] field4 = new DClass[1];
      field4[0] = new DClass<String, Integer>();
      field4[0].setY(78);
      field4[0].add("rab");
      
      KClass<String> arg1 = new KClass<String>();
      arg1.field1 = field1;
      arg1.field2 = field2;
      arg1.field3 = field3;
      arg1.field4 = field4;
      strFactory.write(arg1);
      
      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateKClassInvalid1(String methodName) {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, methodName);
    
      DClass<Integer, Integer> field1 = new DClass<Integer, Integer>();
      field1.setY(12);
      field1.add(90);

      DClass<String, Integer> field2 = new DClass<String, Integer>();
      field2.setY(34);
      field2.add("bar");

      DClass<String, Integer>[] field3 = new DClass[1];
      field3[0] = new DClass<String, Integer>();
      field3[0].setY(56);
      field3[0].add("oof");

      DClass<String, Integer>[] field4 = new DClass[1];
      field4[0] = new DClass<String, Integer>();
      field4[0].setY(78);
      field4[0].add("rab");
      
      KClass arg1 = new KClass();
      arg1.field1 = field1;
      arg1.field2 = field2;
      arg1.field3 = field3;
      arg1.field4 = field4;
      strFactory.write(arg1);
      
      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateKClassInvalid2() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testKClass");
    
      DClass<String, Integer> field1 = new DClass<String, Integer>();
      field1.setY(12);
      field1.add("foo");

      DClass<Integer, Integer> field2 = new DClass<Integer, Integer>();
      field2.setY(34);
      field2.add(90);

      DClass<String, Integer>[] field3 = new DClass[1];
      field3[0] = new DClass<String, Integer>();
      field3[0].setY(56);
      field3[0].add("oof");

      DClass<String, Integer>[] field4 = new DClass[1];
      field4[0] = new DClass<String, Integer>();
      field4[0].setY(78);
      field4[0].add("rab");
      
      KClass arg1 = new KClass();
      arg1.field1 = field1;
      arg1.field2 = field2;
      arg1.field3 = field3;
      arg1.field4 = field4;
      strFactory.write(arg1);
      
      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateKClassInvalid3() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testKClass");
    
      DClass<String, Integer> field1 = new DClass<String, Integer>();
      field1.setY(12);
      field1.add("foo");

      DClass<String, Integer> field2 = new DClass<String, Integer>();
      field2.setY(34);
      field2.add("bar");

      DClass<Integer, Integer>[] field3 = new DClass[1];
      field3[0] = new DClass<Integer, Integer>();
      field3[0].setY(56);
      field3[0].add(90);

      DClass<String, Integer>[] field4 = new DClass[1];
      field4[0] = new DClass<String, Integer>();
      field4[0].setY(78);
      field4[0].add("rab");
      
      KClass arg1 = new KClass();
      arg1.field1 = field1;
      arg1.field2 = field2;
      arg1.field3 = field3;
      arg1.field4 = field4;
      strFactory.write(arg1);
      
      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateKClassInvalid4() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testKClass");
    
      DClass<String, Integer> field1 = new DClass<String, Integer>();
      field1.setY(12);
      field1.add("foo");

      DClass<String, Integer> field2 = new DClass<String, Integer>();
      field2.setY(34);
      field2.add("bar");

      DClass<String, Integer>[] field3 = new DClass[1];
      field3[0] = new DClass<String, Integer>();
      field3[0].setY(56);
      field3[0].add("oof");

      DClass<Integer, Integer>[] field4 = new DClass[1];
      field4[0] = new DClass<Integer, Integer>();
      field4[0].setY(78);
      field4[0].add(90);
      
      KClass arg1 = new KClass();
      arg1.field1 = field1;
      arg1.field2 = field2;
      arg1.field3 = field3;
      arg1.field4 = field4;
      strFactory.write(arg1);
      
      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String generateKClassInvalid5() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testKClass");
    
      DClass<String, String> field1 = new DClass<String, String>();
      field1.setY("bar");
      field1.add("foo");

      DClass<String, Integer> field2 = new DClass<String, Integer>();
      field2.setY(34);
      field2.add("bar");

      DClass<String, Integer>[] field3 = new DClass[1];
      field3[0] = new DClass<String, Integer>();
      field3[0].setY(56);
      field3[0].add("oof");

      DClass<String, Integer>[] field4 = new DClass[1];
      field4[0] = new DClass<String, Integer>();
      field4[0].setY(78);
      field4[0].add("rab");
      
      KClass arg1 = new KClass();
      arg1.field1 = field1;
      arg1.field2 = field2;
      arg1.field3 = field3;
      arg1.field4 = field4;
      strFactory.write(arg1);
      
      return strFactory.toString();

    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }
  
  private static String generateLClassInvalid1() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testWildcardBounds");
    
      LClass<String, String> arg = new LClass<String, String>();
      arg.add("foo");
      arg.field1 = "foo";
      arg.field2 = new LinkedList<String>();
      arg.field2.add("bar");
      strFactory.write((Object) arg);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLClassInvalid2() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testWildcardBounds");
    
      LClass<Integer, Integer> arg = new LClass<Integer, Integer>();
      arg.add(12345);
      arg.field1 = 67890;
      arg.field2 = new LinkedList<Integer>();
      arg.field2.add(45678);
      strFactory.write((Object) arg);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLClassInvalid3() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testWildcardBounds");
    
      LClassInvalid1<Integer, String> arg = new LClassInvalid1<Integer, String>();
      arg.setA(12345);
      arg.field1 = "foo";
      arg.field2 = new LinkedList<String>();
      arg.field2.add("bar");
      strFactory.write(arg);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLClassInvalid4() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testWildcardBounds");
    
      LClassInvalid2<Integer, String> arg = new LClassInvalid2<Integer, String>();
      arg.add(12345);
      arg.field1 = "foo";
      arg.field2 = new LinkedList<String>();
      arg.field2.add("bar");
      strFactory.write((Object) arg);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static String generateLClassInvalid5() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testWildcardBounds");
    
      LClass arg = new LClass();
      arg.add("oof");
      arg.field1 = "foo";
      arg.field2 = new LinkedList<String>();
      arg.field2.add("bar");
      strFactory.write((Object) arg);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static String generateLClassInvalid6() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testWildcardBounds");
    
      LClass arg = new LClass();
      arg.add(12345);
      arg.field1 = 67890;
      arg.field2 = new LinkedList<String>();
      arg.field2.add("bar");
      strFactory.write((Object) arg);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static String generateLClassInvalid7() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testWildcardBounds");
    
      LClass arg = new LClass();
      arg.add(12345);
      arg.field1 = "foo";
      arg.field2 = new LinkedList<Integer>();
      arg.field2.add(67890);
      strFactory.write((Object) arg);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLClassValid() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testWildcardBounds");
    
      LClass<Integer, String> arg = new LClass<Integer, String>();
      arg.add(12345);
      arg.field1 = "foo";
      arg.field2 = new LinkedList<String>();
      arg.field2.add("bar");
      strFactory.write((Object) arg);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLinkedHashMapSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      LinkedHashMap<String, Integer> hashMap = new LinkedHashMap<String, Integer>();
      hashMap.put("foo", 12345);
      strFactory.write(hashMap);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLinkedHashSetSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      LinkedHashSet<Integer> hashSet = new LinkedHashSet<Integer>();
      hashSet.add(12345);
      hashSet.add(67890);
      strFactory.write(hashSet);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateLinkedListSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      LinkedList<Integer> list = new LinkedList<Integer>();
      list.add(12345);
      list.add(67890);
      strFactory.write(list);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateSingletonListSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      Integer i = new Integer(67890);
      List<Integer> singletonList = Collections.singletonList(i);
      strFactory.write(singletonList);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateStringSpoofingInt() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(PrimitiveParamTestClass.class, "testIntString");

      String arg1 = "foo";
      strFactory.write(arg1);

      String arg2 = "bar";
      strFactory.write(arg2);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateStringSpoofingInteger() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(PrimitiveParamTestClass.class, "testIntegerString");

      String arg1 = "foo";
      strFactory.write(arg1);

      String arg2 = "bar";
      strFactory.write(arg2);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateTreeMapSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
      treeMap.put("foo", 12345);
      strFactory.write(treeMap);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateTreeSetSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      ReverseSorter<Integer> sorter = new ReverseSorter<Integer>();
      TreeSet<Integer> treeSet = new TreeSet<Integer>(sorter);
      treeSet.add(12345);
      treeSet.add(67890);
      strFactory.write(treeSet);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  private static String generateVectorSpoofingClass() {
    try {
      RPCTypeCheckFactory strFactory =
          new RPCTypeCheckFactory(ClassesParamTestClass.class, "testAClass");

      Vector<Integer> vector = new Vector<Integer>();
      vector.add(12345);
      vector.add(67890);
      strFactory.write(vector);

      return strFactory.toString();
    } catch (Exception e) {
      fail(e.getMessage());

      return null;
    }
  }

  /**
   * This checks that ArrayList correctly reports that it is an incorrect type.
   */
  public void testArrayListSpoofingClass() {
    try {
      RPC.decodeRequest(generateArrayListSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testArrayListSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*ArrayList.*AClass.*"));
    }
  }

  /**
   * This checks that a List generated by Arrays.asList correctly reports that
   * it is an incorrect type.
   */
  public void testArraysAsListSpoofingClass() {
    try {
      RPC.decodeRequest(generateArraysAsListSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testArraysAsListSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Arrays\\$ArrayList.*AClass.*"));
    }
  }

  /**
   * This checks that generic fields of classes work correctly.
   */
  public void testClassFields() {
    try {
      RPC.decodeRequest(generateBClassSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testClassFields (1)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateCClassSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testClassFields (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
  }

  /**
   * This checks that raw collections fields of classes work correctly.
   */
  public void testClassFieldsRaw() {
    try {
      RPC.decodeRequest(generateBClassRaw());
    } catch (Exception e) {
      fail("Unexpected Exception from testClassFieldsRaw (1): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateCClassRaw());
    } catch (Exception e) {
      fail("Unexpected Exception from testClassFieldsRaw (2): " + e.getMessage());
    }
  }

  /**
   * This checks that complex generic declarations are both correctly serialized
   * (no false negatives) and correctly verified.
   */
  public void testComplexGenerics() {
    try {
      RPC.decodeRequest(generateIClassValid());
    } catch (Exception e) {
      fail("Unexpected assertion from testComplexGenerics (1a): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateIClassRaw());
    } catch (Exception e) {
      fail("Unexpected assertion from testComplexGenerics (1b): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateIClassSpoofingA());
      fail("Expected IncompatibleRemoteServiceException from testComplexGenerics (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*LinkedList.*? extends T.*"));
    }
    try {
      RPC.decodeRequest(generateIClassSpoofingB());
      fail("Expected IncompatibleRemoteServiceException from testComplexGenerics (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*? super .*GEClass.*"));
    }
    try {
      RPC.decodeRequest(generateIClassSpoofingC());
      fail("Expected IncompatibleRemoteServiceException from testComplexGenerics (4)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
  }

  /**
   * This checks that a List generated by Collections.emptyList correctly
   * reports that it is an incorrect type.
   */
  public void testEmptyListSpoofingClass() {
    try {
      RPC.decodeRequest(generateEmptyListSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testEmptyListSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*List.*AClass.*"));
    }
  }

  /**
   * This checks that a Map generated by Collections.emptyMap correctly reports
   * that it is an incorrect type.
   */
  public void testEmptyMapSpoofingClass() {
    try {
      RPC.decodeRequest(generateEmptyMapSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testEmptyMapSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Map.*AClass.*"));
    }
  }

  /**
   * This checks that a Set generated by Collections.emptySet correctly reports
   * that it is an incorrect type.
   */
  public void testEmptySetSpoofingClass() {
    try {
      RPC.decodeRequest(generateEmptySetSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testEmptySetSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Set.*AClass.*"));
    }
  }

  /**
   * This checks that fields of generic classes work correctly.
   */
  public void testGenericClasses() {
    try {
      RPC.decodeRequest(generateDClassRaw());
    } catch (Exception e) {
      fail("Unexpected assertion from testGenericClasses (1-): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateDClassXSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testGenericClasses (1)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateDClassYSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testGenericClasses (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
    try {
      RPC.decodeRequest(generateEClassXSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testGenericClasses (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
    try {
      RPC.decodeRequest(generateEClassYSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testGenericClasses (4)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateFClassRaw());
    } catch (Exception e) {
      fail("Unexpected assertion from testGenericClasses (5-): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateFClassXSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testGenericClasses (5)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateFClassYSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testGenericClasses (6)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
  }

  /**
   * This tests that generic class with fields that have the same types but
   * different actual parameters are correctly handled.
   *
   * It also checks classes with fields that have more generic arguments
   * than the class provides, or that use instances with more parameters than
   * the field itself requires (i.e List<X> when used as Object).
   */
  public void testGenericFields() {
    try {
      RPC.decodeRequest(generateJClassValid());
    } catch (Exception e) {
      fail("Unexpected assertion from testGenericFields (1a): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateJClassRaw1());
    } catch (Exception e) {
      fail("Unexpected assertion from testGenericFields (1b): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateJClassRaw2());
    } catch (Exception e) {
      fail("Unexpected assertion from testGenericFields (1c): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateJClassSpoofing1());
      fail("Expected IncompatibleRemoteServiceException from testGenericFields (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Long.*"));
    }
    try {
      RPC.decodeRequest(generateJClassSpoofing2());
      fail("Expected IncompatibleRemoteServiceException from testGenericFields (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Short.*"));
    }
    try {
      RPC.decodeRequest(generateJClassSpoofing3());
      fail("Expected IncompatibleRemoteServiceException from testGenericFields (4)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateJClassSpoofing4());
      fail("Expected IncompatibleRemoteServiceException from testGenericFields (5)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
    try {
      RPC.decodeRequest(generateJClassSpoofing5());
      fail("Expected IncompatibleRemoteServiceException from testGenericFields (6)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateKClassValid("testKClass"));
    } catch (Exception e) {
      fail("Unexpected assertion from testGenericFields (7a): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateKClassValid("testKClassRaw"));
    } catch (Exception e) {
      fail("Unexpected assertion from testGenericFields (7b): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateKClassInvalid1("testKClass"));
      fail("Expected IncompatibleRemoteServiceException from testGenericFields (7c)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Integer.*String.*"));
    }
    try {
      RPC.decodeRequest(generateKClassInvalid1("testKClassRaw"));
    } catch (Exception e) {
      fail("Unexpected assertion from testGenericFields (7d): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateKClassInvalid2());
      fail("Expected IncompatibleRemoteServiceException from testGenericFields (7e)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Integer.*String.*"));
    }
    try {
      RPC.decodeRequest(generateKClassInvalid3());
      fail("Expected IncompatibleRemoteServiceException from testGenericFields (7f)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Integer.*String.*"));
    }
    try {
      RPC.decodeRequest(generateKClassInvalid4());
      fail("Expected IncompatibleRemoteServiceException from testGenericFields (7g)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Integer.*String.*"));
    }
    try {
      RPC.decodeRequest(generateKClassInvalid5());
      fail("Expected IncompatibleRemoteServiceException from testGenericFields (7h)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*String.*Integer.*"));
    }
  }

  /**
   * This checks that HashMap correctly reports that it is an incorrect type.
   */
  public void testHashMapSpoofingClass() {
    try {
      RPC.decodeRequest(generateHashMapSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testHashMapSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashMap.*AClass.*"));
    }
  }

  /**
   * This checks that HashSet correctly reports that it is an incorrect type.
   */
  public void testHashSetSpoofingClass() {
    try {
      RPC.decodeRequest(generateHashSetSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testHashSetSpoofingClass (1)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(e.getCause().getClass(), SerializedTypeViolationException.class);
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*AClass.*"));
    }
    try {
      RPC.decodeRequest(generateLinkedHashSetSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testHashSetSpoofingClass (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*LinkedHashSet.*AClass.*"));
    }
  }

  /**
   * This checks that IdentityHashMap correctly reports that it is an incorrect
   * type.
   */
  public void testIdentityHashMapSpoofingClass() {
    try {
      RPC.decodeRequest(generateIdentityHashMapSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testIdentityHashMapSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*IdentityHashMap.*AClass.*"));
    }
  }

  /**
   * This checks that LinkedHashMap correctly reports that it is an incorrect
   * type.
   */
  public void testLinkedHashMapSpoofingClass() {
    try {
      RPC.decodeRequest(generateLinkedHashMapSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testLinkedHashMapSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*LinkedHashMap.*AClass.*"));
    }
  }

  /**
   * This checks that LinkedList correctly reports that it is an incorrect type.
   */
  public void testLinkedListSpoofingClass() {
    try {
      RPC.decodeRequest(generateLinkedListSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testLinkedListSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*LinkedList.*AClass.*"));
    }
  }

  /**
   * This checks that we correctly report, or don't report, errors when we
   * cannot know the actual value of a generic but may have bounds from the
   * method class.
   * 
   * In this case,
   * ClassesParamTestClass<T extends List<Integer> & MInterface<String>>
   * is the class containing the method, and the method expects an argument of
   * type T, so the thing in the RPC method should meet the bounds on T.
   */
  public void testMethodClassGenerics() {
    try {
      RPC.decodeRequest(generateLClassValid());
    } catch (Exception e) {
      fail("Unexpected assertion from testMethodClassGenerics (1): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateLClassInvalid1());
      fail("Expected IncompatibleRemoteServiceException from testMethodClassGenerics (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*String.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateLClassInvalid2());
      fail("Expected IncompatibleRemoteServiceException from testMethodClassGenerics (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Integer.*String.*"));
    }
    try {
      RPC.decodeRequest(generateLClassInvalid3());
      fail("Expected IncompatibleRemoteServiceException from testMethodClassGenerics (4)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*LClassInvalid1.*T.*"));
    }
    try {
      RPC.decodeRequest(generateLClassInvalid4());
      fail("Expected IncompatibleRemoteServiceException from testMethodClassGenerics (5)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*LClassInvalid2.*T.*"));
    }
    try {
      RPC.decodeRequest(generateLClassInvalid5());
      fail("Expected IncompatibleRemoteServiceException from testMethodClassGenerics (6)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*String.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateLClassInvalid6());
      fail("Expected IncompatibleRemoteServiceException from testMethodClassGenerics (7)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Integer.*String.*"));
    }
    try {
      RPC.decodeRequest(generateLClassInvalid7());
      fail("Expected IncompatibleRemoteServiceException from testMethodClassGenerics (8)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Integer.*String.*"));
    }
  }

  /**
   * This checks that fields of generic classes work correctly.
   */
  public void testNestedGenericClasses() {
    try {
      RPC.decodeRequest(generateGDClassRaw1());
    } catch (Exception e) {
      fail("Unexpected Exception from testNestedGenericClasses (0a): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateGDClassRaw2());
    } catch (Exception e) {
      fail("Unexpected Exception from testNestedGenericClasses (0b): " + e.getMessage());
    }
    try {
      RPC.decodeRequest(generateGDClassXSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenericClasses (1)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateGDClassYSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenericClasses (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
    try {
      RPC.decodeRequest(generateGEClassXSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenericClasses (3)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateGEClassYSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenericClasses (4)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
    try {
      RPC.decodeRequest(generateHDClassXSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenericClasses (5)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateHDClassYSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenericClasses (6)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
    try {
      RPC.decodeRequest(generateHEClassXSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenericClasses (7)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*Integer.*"));
    }
    try {
      RPC.decodeRequest(generateHEClassYSpoofing());
      fail("Expected IncompatibleRemoteServiceException from testNestedGenericClasses (8)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*HashSet.*String.*"));
    }
  }

  /**
   * This checks situations in which an RPC message is modified to replace
   * arguments of a primitive type with another primitive type.
   */
  public void testPrimitiveSpoofingPrimitive() {
    try {
      // An integer can pretend to be a string, and the result will be the
      // Integer class serialization string appearing as the string value.
      RPC.decodeRequest(generateIntegerSpoofingString());
    } catch (IncompatibleRemoteServiceException e) {
      fail("Unexpected IncompatibleRemoteServiceException from testPrimitiveSpoofingPrimitive (1)");
    }
    try {
      RPC.decodeRequest(generateStringSpoofingInteger());
      fail("Expected IncompatibleRemoteServiceException from testPrimitiveSpoofingPrimitive (2)");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializationException.class, e.getCause().getClass());
    }
  }

  /**
   * This checks that Collections.singletonList correctly reports that it is an
   * incorrect type.
   */
  public void testSingletonListSpoofingClass() {
    try {
      RPC.decodeRequest(generateSingletonListSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testSingletonListSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*List.*AClass.*"));
    }
  }

  /**
   * This checks that TreeMap correctly reports that it is an incorrect type.
   */
  public void testTreeMapSpoofingClass() {
    try {
      RPC.decodeRequest(generateTreeMapSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testTreeMapSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*TreeMap.*AClass.*"));
    }
  }

  /**
   * This checks that TreeSet correctly reports that it is an incorrect type.
   */
  public void testTreeSetSpoofingClass() {
    try {
      RPC.decodeRequest(generateTreeSetSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testTreeSetSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*TreeSet.*AClass.*"));
    }
  }

  /**
   * This checks situations in which an RPC message is modified to replace
   * arguments of a primitive value type with another primitive type.
   */
  public void testValueSpoofing() {
    try {
      // When an int appears in place of a string, the result will be the
      // int value indexing the string table, which will result in
      // an out of bounds exception if the index is out of bounds, or
      // an incorrect string if the integer value is within range of the string
      // table.
      RPC.decodeRequest(generateIntSpoofingString());
      fail("Expected ArrayIndexOutOfBoundsException from testValueSpoofing (1)");
    } catch (ArrayIndexOutOfBoundsException e) {
      // Expected
    }
    try {
      // When a string pretends to be an int, it simply results in an incorrect
      // integer value.
      RPC.decodeRequest(generateStringSpoofingInt());
    } catch (Exception e) {
      fail("Unexpected exception: " + e.toString());
    }
  }

  /**
   * This checks that Vector correctly reports that it is an incorrect type.
   */
  public void testVectorSpoofingClass() {
    try {
      RPC.decodeRequest(generateVectorSpoofingClass());
      fail("Expected IncompatibleRemoteServiceException from testVectorSpoofingClass");
    } catch (IncompatibleRemoteServiceException e) {
      // Expected to get here
      assertEquals(SerializedTypeViolationException.class, e.getCause().getClass());
      assertTrue(e.getCause().getMessage().matches(".*Vector.*AClass.*"));
    }
  }
}
