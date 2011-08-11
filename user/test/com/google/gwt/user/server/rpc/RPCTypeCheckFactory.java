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

import static com.google.gwt.user.client.rpc.impl.AbstractSerializationStream.RPC_SEPARATOR_CHAR;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStream;
import com.google.gwt.user.server.rpc.RPCTypeCheckCollectionsTest.TestHashSet;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Generates RPC messages for use in testing server message handling.
 * 
 * This class is designed to make it easy to generate arbitrary messages, in the
 * sense that the elements in the string do not need to match the method's
 * expected elements in any way.
 * 
 */
public class RPCTypeCheckFactory {
  /**
   * A Serialization policy that allows anything to be serialized.
   */
  public static class TestSerializationPolicy extends SerializationPolicy {
    @Override
    public boolean shouldDeserializeFields(Class<?> clazz) {
      return true;
    }

    @Override
    public boolean shouldSerializeFields(Class<?> clazz) {
      return true;
    }

    @SuppressWarnings("unused")
    @Override
    public void validateDeserialize(Class<?> clazz) throws SerializationException {
    }

    @SuppressWarnings("unused")
    @Override
    public void validateSerialize(Class<?> clazz) throws SerializationException {
    }
  }

  private static final TestSerializationPolicy testSerializationPolicy =
      new TestSerializationPolicy();

  /**
   * Generates a HashMap containing HashSet for testing.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static HashMap<HashSet, Integer> generateTestHashMap() {
    HashMap<HashSet, Integer> result = new HashMap<HashSet, Integer>();
    HashSet entry1 = new HashSet();
    entry1.add(0);
    result.put(entry1, 12345);

    return result;
  }

  /**
   * Generates a HashSet of HashSet for testing.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static HashSet generateTestHashSet() {
    HashSet result = new HashSet();
    HashSet entry1 = new HashSet();
    HashSet entry2 = new HashSet();
    entry1.add(12345);
    entry1.add(67890);
    result.add(entry1);
    result.add(entry2);

    return result;
  }
  /**
   * Generates a HashSet that contains a HashSet of HashSets for testing.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static HashSet generateTestHashSetHashSet() {
    HashSet result = new HashSet();
    HashSet entry0 = new HashSet();
    HashSet entry1 = new HashSet();
    HashSet entry2 = new HashSet();
    entry1.add(12345);
    entry2.add(67890);
    entry0.add(entry1);
    entry0.add(entry2);
    result.add(entry0);

    return result;
  }

  /**
   * Generates a IdentityHashMap that contains a HashSet for testing.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static IdentityHashMap<HashSet, Integer> generateTestIdentityHashMap() {
    IdentityHashMap<HashSet, Integer> result = new IdentityHashMap<HashSet, Integer>();
    HashSet entry1 = new HashSet();
    HashSet entry2 = new HashSet();
    entry1.add(12345);
    entry2.add(67890);
    result.put(entry1, 12345);
    result.put(entry2, 67890);

    return result;
  }
  /**
   * Generates a LinkedHashMap that contains a HashSet for testing.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static LinkedHashMap<HashSet, Integer> generateTestLinkedHashMap() {
    LinkedHashMap<HashSet, Integer> result = new LinkedHashMap<HashSet, Integer>();
    HashSet entry1 = new HashSet();
    HashSet entry2 = new HashSet();
    entry1.add(12345);
    entry2.add(67890);
    result.put(entry1, 12345);
    result.put(entry2, 67890);

    return result;
  }

  /**
   * Generates a LinkedHashSet that takes a very long time to deserialize.
   * 
   * @return A LinkedHashSet that cannot be used but requires computation of an
   *         exponential number of hash codes.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static TestHashSet generateTestLinkedHashSet() {
    TestHashSet result = new TestHashSet();
    HashSet entry1 = new HashSet();
    HashSet entry2 = new HashSet();
    entry1.add(12345);
    entry2.add(67890);
    result.add(entry1);
    result.add(entry2);

    return result;
  }

  private static String generateSerializedClassString(Class<?> instanceType) {
    if (instanceType == int.class) {
      return "I";
    }

    return instanceType.getName() + "/"
        + SerializabilityUtil.getSerializationSignature(instanceType, testSerializationPolicy);
  }

  private String headerString = "";

  private String bodyString = "";

  private LinkedHashMap<String, Integer> stringTable;

  private IdentityHashMap<Object, Integer> encodedObjectTable;

  /**
   * Create a new Factory that will generate an RPC string to invoke the given
   * method found in the given class.
   * 
   * @throws NoSuchMethodException when the method requested does not exist on
   *           the class provided.
   */
  public RPCTypeCheckFactory(Class<?> testMethodClass, String testMethodName)
      throws NoSuchMethodException {
    headerString +=
        Integer.toString(AbstractSerializationStream.SERIALIZATION_STREAM_VERSION)
            + RPC_SEPARATOR_CHAR + // version
            "0" + RPC_SEPARATOR_CHAR; // flags

    stringTable = new LinkedHashMap<String, Integer>(20);
    writeStringFromTable("moduleBaseURL");
    writeStringFromTable("whitelistHashcode");

    encodedObjectTable = new IdentityHashMap<Object, Integer>(20);

    writeMethodSignature(testMethodClass, testMethodName);
  }

  /**
   * Convert the accumulated contents of this object to the final RPC message
   * string.
   */
  @Override
  public String toString() {
    String result = headerString;

    result += Integer.toString(stringTable.size()) + RPC_SEPARATOR_CHAR;
    for (String strEntry : stringTable.keySet()) {
      result += strEntry + RPC_SEPARATOR_CHAR;
    }

    result += bodyString;

    return result;
  }

  /**
   * Add data for an int object.
   */
  public void write(int integer) throws SerializationException {
    try {
      bodyString += Integer.toString(integer) + RPC_SEPARATOR_CHAR;
    } catch (Exception e) {
      throw new SerializationException(e);
    }
  }

  /**
   * Add data for an int array object.
   */
  public void write(int[] intArray) {
    writeStringFromTable(generateSerializedClassString(intArray.getClass()));
    bodyString += Integer.toString(intArray.length) + RPC_SEPARATOR_CHAR;
    for (int i : intArray) {
      bodyString += Integer.toString(i) + RPC_SEPARATOR_CHAR;
    }
  }

  /**
   * Add data for an {@link Integer} object.
   */
  public void write(Integer integer) throws SerializationException {
    if (getEncodedIndex(integer)) {
      return;
    }
    try {
      writeStringFromTable(generateSerializedClassString(Integer.class));
      bodyString += integer.toString() + RPC_SEPARATOR_CHAR;
    } catch (Exception e) {
      throw new SerializationException(e);
    }
  }

  /**
   * Add data for a {@link java.util.List} object.
   */
  public void write(List<?> list) throws SerializationException {
    if (getEncodedIndex(list)) {
      return;
    }
    writeStringFromTable(generateSerializedClassString(list.getClass()));
    writeListBody(list);
  }

  /**
   * Add data for a {@link java.util.Map} object.
   */
  public void write(Map<?, ?> map) throws SerializationException {
    if (getEncodedIndex(map)) {
      return;
    }
    writeStringFromTable(generateSerializedClassString(map.getClass()));
    if (map instanceof LinkedHashMap) {
      // Set the iteration order for the linked hash map, always insertion
      // order.
      bodyString += "0" + RPC_SEPARATOR_CHAR;
    }
    if (map instanceof TreeMap) {
      // Set the iteration order for the linked hash map, always insertion
      // order.
      Comparator<?> comp = ((TreeMap<?, ?>) map).comparator();
      if (comp == null) {
        bodyString += "0" + RPC_SEPARATOR_CHAR;
      } else {
        writeStringFromTable(generateSerializedClassString(comp.getClass()));
      }
    }
    bodyString += Integer.toString(map.size()) + RPC_SEPARATOR_CHAR;
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      writeCollectionElement(entry.getKey());
      writeCollectionElement(entry.getValue());
    }
  }

  /**
   * Add data for an {@link Object} where no more specific write method exists.
   */
  public void write(Object object) throws SerializationException {
    if (getEncodedIndex(object)) {
      return;
    }

    try {
      Class<?> clazz = object.getClass();
      writeStringFromTable(generateSerializedClassString(clazz));

      writeClass(object, clazz);
    } catch (Exception e) {
      throw new SerializationException(e);
    }
  }

  /**
   * Add data for a {@link Object} array.
   */
  public void write(Object[] objectArray) throws SerializationException {
    if (getEncodedIndex(objectArray)) {
      return;
    }
    writeStringFromTable(generateSerializedClassString(objectArray.getClass()));
    bodyString += Integer.toString(objectArray.length) + RPC_SEPARATOR_CHAR;
    for (Object element : objectArray) {
      writeCollectionElement(element);
    }
  }

  /**
   * Add data for a {@link java.util.Set} object.
   */
  public void write(Set<?> set) throws SerializationException {
    if (getEncodedIndex(set)) {
      return;
    }
    writeStringFromTable(generateSerializedClassString(set.getClass()));
    if (set instanceof TreeSet) {
      writeStringFromTable(generateSerializedClassString(((TreeSet<?>) set).comparator().getClass()));
    }
    bodyString += Integer.toString(set.size()) + RPC_SEPARATOR_CHAR;
    for (Object element : set) {
      writeCollectionElement(element);
    }
  }

  /**
   * Add data for a {@link String} object.
   */
  public void write(String string) {
    writeStringFromTable(string);
  }

  /**
   * Add data for a {@link java.util.Vector} object.
   */
  public void write(Vector<?> vector) throws SerializationException {
    if (getEncodedIndex(vector)) {
      return;
    }
    writeStringFromTable(generateSerializedClassString(vector.getClass()));
    bodyString += Integer.toString(vector.size()) + RPC_SEPARATOR_CHAR;
    for (Object element : vector) {
      writeCollectionElement(element);
    }
  }

  /**
   * Add data for an empty list object (as returned by
   * {@link java.util.Collections}).
   */
  public void writeEmptyList() {
    List<?> emptyList = java.util.Collections.emptyList();
    writeStringFromTable(generateSerializedClassString(emptyList.getClass()));
  }

  /**
   * Add data for an empty map object (as returned by
   * {@link java.util.Collections}).
   */
  public void writeEmptyMap() {
    Map<?, ?> emptyMap = java.util.Collections.emptyMap();
    writeStringFromTable(generateSerializedClassString(emptyMap.getClass()));
  }

  /**
   * Add data for an empty set object (as returned by
   * {@link java.util.Collections}).
   */
  public void writeEmptySet() {
    Set<?> emptySet = java.util.Collections.emptySet();
    writeStringFromTable(generateSerializedClassString(emptySet.getClass()));
  }

  private boolean getEncodedIndex(Object obj) {
    Integer foundIndex = encodedObjectTable.get(obj);
    if (foundIndex == null) {
      foundIndex = encodedObjectTable.size();
      encodedObjectTable.put(obj, foundIndex);
      return false;
    }

    bodyString += Integer.toString(-(foundIndex.intValue() + 1)) + RPC_SEPARATOR_CHAR;

    return true;
  }

  private void writeClass(Object object, Class<?> clazz) throws SerializationException {
    Field[] fields = clazz.getDeclaredFields();
    try {
      for (Field field : fields) {
        Type type = field.getGenericType();
        if (type == int.class) {
          write(field.getInt(object));
        } else {
          Object value = field.get(object);
          writeCollectionElement(value);
        }
      }

      Class<?> superClass = clazz.getSuperclass();
      if (superClass == Object.class) {
        return;
      } else if (superClass == LinkedList.class) {
        writeListBody((List<?>) object);
        return;
      }

      writeClass(object, superClass);

    } catch (Exception e) {
      throw new SerializationException(e);
    }
  }

  /**
   * @param element
   * @throws SerializationException
   */
  @SuppressWarnings("cast")
  private void writeCollectionElement(Object element) throws SerializationException {
    if (element == null) {
      bodyString += "0" + RPC_SEPARATOR_CHAR;
    } else if (element instanceof Map) {
      write((Map<?, ?>) element);
    } else if (element instanceof HashSet) {
      write((HashSet<?>) element);
    } else if (element instanceof int[]) {
      write((int[]) element);
    } else if (element instanceof Integer) {
      write((Integer) element);
    } else if (element instanceof Long) {
      writeLong((Long) element);
    } else if (element instanceof Float) {
      writeFloat((Float) element);
    } else if (element instanceof Short) {
      writeShort((Short) element);
    } else if (element instanceof Double) {
      writeDouble((Double) element);
    } else if (element instanceof RPCTypeCheckTest.DClass) {
      write((Object) element);
    } else if (element instanceof String) {
      writeStringInCollection((String) element);
    } else if (element instanceof List) {
      write((List<?>) element);
    } else if (element instanceof Object[]) {
      write((Object[]) element);
    } else {
      write(element);
    }
  }

  private void writeDouble(Double d) {
    writeStringFromTable(generateSerializedClassString(Double.class));
    writeStringFromTable(d.toString());
  }

  private void writeFloat(Float f) {
    writeStringFromTable(generateSerializedClassString(Float.class));
    writeStringFromTable(f.toString());
  }

  private void writeListBody(List<?> list) throws SerializationException {
    if (list.getClass().toString().matches(".*Arrays\\$ArrayList.*")) {
      write(list.toArray());
      return;
    }
    if (!list.getClass().toString().matches(".*Collections\\$SingletonList.*")) {
      bodyString += Integer.toString(list.size()) + RPC_SEPARATOR_CHAR;
    }
    for (Object element : list) {
      writeCollectionElement(element);
    }
  }

  private void writeLong(Long l) {
    writeStringFromTable(generateSerializedClassString(Long.class));
    writeStringFromTable(l.toString());
  }

  private void writeMethodSignature(Class<?> testMethodClass, String testMethodName)
      throws NoSuchMethodException {
    writeStringFromTable(testMethodClass.getName());
    writeStringFromTable(testMethodName);

    Method[] testMethods = testMethodClass.getDeclaredMethods();
    Method targetMethod = null;
    for (Method method : testMethods) {
      if (method.getName().equals(testMethodName)) {
        targetMethod = method;
        break;
      }
    }
    if (targetMethod == null) {
      throw new NoSuchMethodException("Could not find " + testMethodName + " in "
          + testMethodClass.getName());
    }

    Class<?>[] parameterClasses = targetMethod.getParameterTypes();
    bodyString += Integer.toString(parameterClasses.length) + RPC_SEPARATOR_CHAR;
    for (Class<?> parameter : parameterClasses) {
      writeStringFromTable(generateSerializedClassString(parameter));
    }
  }

  private void writeShort(Short s) {
    writeStringFromTable(generateSerializedClassString(Short.class));
    writeStringFromTable(s.toString());
  }

  private void writeStringFromTable(String string) {
    Integer index = stringTable.get(string);
    if (index == null) {
      index = stringTable.size() + 1;
      stringTable.put(string, index);
    }
    bodyString += index.toString() + RPC_SEPARATOR_CHAR;
  }

  private void writeStringInCollection(String string) {
    writeStringFromTable(generateSerializedClassString(string.getClass()));
    writeStringFromTable(string);
  }

}
