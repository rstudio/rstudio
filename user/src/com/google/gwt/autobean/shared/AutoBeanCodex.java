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
package com.google.gwt.autobean.shared;

import com.google.gwt.autobean.shared.impl.EnumMap;
import com.google.gwt.autobean.shared.impl.LazySplittable;
import com.google.gwt.autobean.shared.impl.StringQuoter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Utility methods for encoding an AutoBean graph into a JSON-compatible string.
 * This codex intentionally does not preserve object identity, nor does it
 * encode cycles, but it will detect them.
 */
public class AutoBeanCodex {
  static class Decoder extends AutoBeanVisitor {
    private final Stack<AutoBean<?>> beanStack = new Stack<AutoBean<?>>();
    private final Stack<Splittable> dataStack = new Stack<Splittable>();
    private AutoBean<?> bean;
    private Splittable data;
    private final AutoBeanFactory factory;

    public Decoder(AutoBeanFactory factory) {
      this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    public <T> AutoBean<T> decode(Splittable data, Class<T> type) {
      push(data, type);
      bean.accept(this);
      return (AutoBean<T>) pop();
    }

    @Override
    public boolean visitCollectionProperty(String propertyName,
        AutoBean<Collection<?>> value, CollectionPropertyContext ctx) {
      if (data.isNull(propertyName)) {
        return false;
      }

      Collection<Object> collection;
      if (List.class.equals(ctx.getType())) {
        collection = new ArrayList<Object>();
      } else if (Set.class.equals(ctx.getType())) {
        collection = new HashSet<Object>();
      } else {
        throw new UnsupportedOperationException("Only List and Set supported");
      }

      boolean isValue = ValueCodex.canDecode(ctx.getElementType());
      boolean isEncoded = Splittable.class.equals(ctx.getElementType());
      Splittable listData = data.get(propertyName);
      for (int i = 0, j = listData.size(); i < j; i++) {
        if (listData.isNull(i)) {
          collection.add(null);
        } else {
          if (isValue) {
            collection.add(decodeValue(ctx.getElementType(), listData.get(i)));
          } else if (isEncoded) {
            collection.add(listData.get(i));
          } else {
            collection.add(decode(listData.get(i), ctx.getElementType()).as());
          }
        }
      }
      ctx.set(collection);
      return false;
    }

    @Override
    public boolean visitMapProperty(String propertyName,
        AutoBean<Map<?, ?>> value, MapPropertyContext ctx) {
      if (data.isNull(propertyName)) {
        return false;
      }

      Map<?, ?> map;
      if (ValueCodex.canDecode(ctx.getKeyType())) {
        map = decodeValueKeyMap(data.get(propertyName), ctx.getKeyType(),
            ctx.getValueType());
      } else {
        map = decodeObjectKeyMap(data.get(propertyName), ctx.getKeyType(),
            ctx.getValueType());
      }
      ctx.set(map);
      return false;
    }

    @Override
    public boolean visitReferenceProperty(String propertyName,
        AutoBean<?> value, PropertyContext ctx) {
      if (data.isNull(propertyName)) {
        return false;
      }

      if (Splittable.class.equals(ctx.getType())) {
        ctx.set(data.get(propertyName));
        return false;
      }

      push(data.get(propertyName), ctx.getType());
      bean.accept(this);
      ctx.set(pop().as());
      return false;
    }

    @Override
    public boolean visitValueProperty(String propertyName, Object value,
        PropertyContext ctx) {
      if (!data.isNull(propertyName)) {
        Object object;
        Splittable propertyValue = data.get(propertyName);
        Class<?> type = ctx.getType();
        object = decodeValue(type, propertyValue);
        ctx.set(object);
      }
      return false;
    }

    private Map<?, ?> decodeObjectKeyMap(Splittable map, Class<?> keyType,
        Class<?> valueType) {
      boolean isEncodedKey = Splittable.class.equals(keyType);
      boolean isEncodedValue = Splittable.class.equals(valueType);
      boolean isValueValue = Splittable.class.equals(valueType);

      Splittable keyList = map.get(0);
      Splittable valueList = map.get(1);
      assert keyList.size() == valueList.size();

      Map<Object, Object> toReturn = new HashMap<Object, Object>(keyList.size());
      for (int i = 0, j = keyList.size(); i < j; i++) {
        Object key;
        if (isEncodedKey) {
          key = keyList.get(i);
        } else {
          key = decode(keyList.get(i), keyType).as();
        }

        Object value;
        if (valueList.isNull(i)) {
          value = null;
        } else if (isEncodedValue) {
          value = keyList.get(i);
        } else if (isValueValue) {
          value = decodeValue(valueType, keyList.get(i));
        } else {
          value = decode(valueList.get(i), valueType).as();
        }

        toReturn.put(key, value);
      }
      return toReturn;
    }

    private Object decodeValue(Class<?> type, Splittable propertyValue) {
      return decodeValue(type, propertyValue.asString());
    }

    private Object decodeValue(Class<?> type, String propertyValue) {
      Object object;
      if (type.isEnum() && bean.getFactory() instanceof EnumMap) {
        // The generics kind of get in the way here
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<Enum> enumType = (Class<Enum>) type;
        @SuppressWarnings("unchecked")
        Enum<?> e = ((EnumMap) bean.getFactory()).getEnum(enumType,
            propertyValue);
        object = e;
      } else {
        object = ValueCodex.decode(type, propertyValue);
      }
      return object;
    }

    private Map<?, ?> decodeValueKeyMap(Splittable map, Class<?> keyType,
        Class<?> valueType) {
      Map<Object, Object> toReturn = new HashMap<Object, Object>();

      boolean isEncodedValue = Splittable.class.equals(valueType);
      boolean isValueValue = ValueCodex.canDecode(valueType);
      for (String encodedKey : map.getPropertyKeys()) {
        Object key = decodeValue(keyType, encodedKey);
        Object value;
        if (map.isNull(encodedKey)) {
          value = null;
        } else if (isEncodedValue) {
          value = map.get(encodedKey);
        } else if (isValueValue) {
          value = decodeValue(valueType, map.get(encodedKey));
        } else {
          value = decode(map.get(encodedKey), valueType).as();
        }
        toReturn.put(key, value);
      }

      return toReturn;
    }

    private AutoBean<?> pop() {
      dataStack.pop();
      if (dataStack.isEmpty()) {
        data = null;
      } else {
        data = dataStack.peek();
      }
      AutoBean<?> toReturn = beanStack.pop();
      if (beanStack.isEmpty()) {
        bean = null;
      } else {
        bean = beanStack.peek();
      }
      return toReturn;
    }

    private void push(Splittable data, Class<?> type) {
      this.data = data;
      bean = factory.create(type);
      if (bean == null) {
        throw new IllegalArgumentException(
            "The AutoBeanFactory cannot create a " + type.getName());
      }
      dataStack.push(data);
      beanStack.push(bean);
    }
  }

  static class Encoder extends AutoBeanVisitor {
    private EnumMap enumMap;
    private Set<AutoBean<?>> seen = new HashSet<AutoBean<?>>();
    private Stack<StringBuilder> stack = new Stack<StringBuilder>();
    private StringBuilder sb;

    public Encoder(AutoBeanFactory factory) {
      if (factory instanceof EnumMap) {
        enumMap = (EnumMap) factory;
      }
    }

    @Override
    public void endVisit(AutoBean<?> bean, Context ctx) {
      if (sb.length() == 0) {
        // No properties
        sb.append("{");
      } else {
        sb.setCharAt(0, '{');
      }
      sb.append("}");
    }

    @Override
    public void endVisitReferenceProperty(String propertyName,
        AutoBean<?> value, PropertyContext ctx) {
      StringBuilder popped = pop();
      if (popped.length() > 0) {
        sb.append(",\"").append(propertyName).append("\":").append(
            popped.toString());
      }
    }

    @Override
    public boolean visitCollectionProperty(String propertyName,
        AutoBean<Collection<?>> value, CollectionPropertyContext ctx) {
      push(new StringBuilder());

      if (value == null) {
        return false;
      }

      Collection<?> collection = value.as();
      if (collection.isEmpty()) {
        sb.append("[]");
        return false;
      }

      if (ValueCodex.canDecode(ctx.getElementType())) {
        for (Object element : collection) {
          sb.append(",").append(
              encodeValue(ctx.getElementType(), element).getPayload());
        }
      } else {
        boolean isEncoded = Splittable.class.equals(ctx.getElementType());
        for (Object element : collection) {
          sb.append(",");
          if (element == null) {
            sb.append("null");
          } else if (isEncoded) {
            sb.append(((Splittable) element).getPayload());
          } else {
            encodeToStringBuilder(sb, element);
          }
        }
      }
      sb.setCharAt(0, '[');
      sb.append("]");
      return false;
    }

    @Override
    public boolean visitMapProperty(String propertyName,
        AutoBean<Map<?, ?>> value, MapPropertyContext ctx) {
      push(new StringBuilder());

      if (value == null) {
        return false;
      }

      Map<?, ?> map = value.as();
      if (map.isEmpty()) {
        sb.append("{}");
        return false;
      }

      Class<?> keyType = ctx.getKeyType();
      Class<?> valueType = ctx.getValueType();
      boolean isEncodedKey = Splittable.class.equals(keyType);
      boolean isEncodedValue = Splittable.class.equals(valueType);
      boolean isValueKey = ValueCodex.canDecode(keyType);
      boolean isValueValue = ValueCodex.canDecode(valueType);

      if (isValueKey) {
        writeValueKeyMap(map, keyType, valueType, isEncodedValue, isValueValue);
      } else {
        writeObjectKeyMap(map, valueType, isEncodedKey, isEncodedValue,
            isValueValue);
      }

      return false;
    }

    @Override
    public boolean visitReferenceProperty(String propertyName,
        AutoBean<?> value, PropertyContext ctx) {
      push(new StringBuilder());

      if (value == null) {
        return false;
      }

      if (Splittable.class.equals(ctx.getType())) {
        sb.append(((Splittable) value.as()).getPayload());
        return false;
      }

      if (seen.contains(value)) {
        haltOnCycle();
      }

      return true;
    }

    @Override
    public boolean visitValueProperty(String propertyName, Object value,
        PropertyContext ctx) {
      // Skip primitive types whose values are uninteresting.
      Class<?> type = ctx.getType();
      Object blankValue = ValueCodex.getUninitializedFieldValue(type);
      if (value == blankValue || value != null && value.equals(blankValue)) {
        return false;
      }

      // Special handling for enums if we have an obfuscation map
      Splittable split;
      split = encodeValue(type, value);
      sb.append(",\"").append(propertyName).append("\":").append(
          split.getPayload());
      return false;
    }

    StringBuilder pop() {
      StringBuilder toReturn = stack.pop();
      sb = stack.peek();
      return toReturn;
    }

    void push(StringBuilder sb) {
      stack.push(sb);
      this.sb = sb;
    }

    private void encodeToStringBuilder(StringBuilder accumulator, Object value) {
      push(new StringBuilder());
      AutoBean<?> bean = AutoBeanUtils.getAutoBean(value);
      if (!seen.add(bean)) {
        haltOnCycle();
      }
      bean.accept(this);
      accumulator.append(pop().toString());
      seen.remove(bean);
    }

    /**
     * Encodes a value, with special handling for enums to allow the field name
     * to be overridden.
     */
    private Splittable encodeValue(Class<?> expectedType, Object value) {
      Splittable split;
      if (value instanceof Enum<?> && enumMap != null) {
        split = ValueCodex.encode(String.class,
            enumMap.getToken((Enum<?>) value));
      } else {
        split = ValueCodex.encode(expectedType, value);
      }
      return split;
    }

    private void haltOnCycle() {
      throw new HaltException(new UnsupportedOperationException(
          "Cycle detected"));
    }

    /**
     * Writes a map JSON literal where the keys are object types. This is
     * encoded as a list of two lists, since it's possible that two distinct
     * objects have the same encoded form.
     */
    private void writeObjectKeyMap(Map<?, ?> map, Class<?> valueType,
        boolean isEncodedKey, boolean isEncodedValue, boolean isValueValue) {
      StringBuilder keys = new StringBuilder();
      StringBuilder values = new StringBuilder();

      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (isEncodedKey) {
          keys.append(",").append(((Splittable) entry.getKey()).getPayload());
        } else {
          encodeToStringBuilder(keys.append(","), entry.getKey());
        }

        if (isEncodedValue) {
          values.append(",").append(
              ((Splittable) entry.getValue()).getPayload());
        } else if (isValueValue) {
          values.append(",").append(
              encodeValue(valueType, entry.getValue()).getPayload());
        } else {
          encodeToStringBuilder(values.append(","), entry.getValue());
        }
      }
      keys.setCharAt(0, '[');
      keys.append("]");
      values.setCharAt(0, '[');
      values.append("]");

      sb.append("[").append(keys.toString()).append(",").append(
          values.toString()).append("]");
    }

    /**
     * Writes a map JSON literal where the keys are value types.
     */
    private void writeValueKeyMap(Map<?, ?> map, Class<?> keyType,
        Class<?> valueType, boolean isEncodedValue, boolean isValueValue) {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        sb.append(",").append(encodeValue(keyType, entry.getKey()).getPayload()).append(
            ":");
        if (isEncodedValue) {
          sb.append(((Splittable) entry.getValue()).getPayload());
        } else if (isValueValue) {
          sb.append(encodeValue(valueType, entry.getValue()).getPayload());
        } else {
          encodeToStringBuilder(sb, entry.getValue());
        }
      }
      sb.setCharAt(0, '{');
      sb.append("}");
    }
  }

  /**
   * Used to stop processing.
   */
  static class HaltException extends RuntimeException {
    public HaltException(RuntimeException cause) {
      super(cause);
    }

    @Override
    public RuntimeException getCause() {
      return (RuntimeException) super.getCause();
    }
  }

  public static <T> AutoBean<T> decode(AutoBeanFactory factory, Class<T> clazz,
      Splittable data) {
    return new Decoder(factory).decode(data, clazz);
  }

  /**
   * Decode an AutoBeanCodex payload.
   * 
   * @param <T> the expected return type
   * @param factory an AutoBeanFactory capable of producing {@code AutoBean<T>}
   * @param clazz the expected return type
   * @param payload a payload string previously generated by
   *          {@link #encode(AutoBean)}
   * @return an AutoBean containing the payload contents
   */
  public static <T> AutoBean<T> decode(AutoBeanFactory factory, Class<T> clazz,
      String payload) {
    Splittable data = StringQuoter.split(payload);
    return decode(factory, clazz, data);
  }

  /**
   * Encodes an AutoBean. The actual payload contents can be retrieved through
   * {@link Splittable#getPayload()}.
   * 
   * @param bean the bean to encode
   * @return a Splittable that encodes the state of the AutoBean
   */
  public static Splittable encode(AutoBean<?> bean) {
    if (bean == null) {
      return LazySplittable.NULL;
    }

    StringBuilder sb = new StringBuilder();
    encodeForJsoPayload(sb, bean);
    return new LazySplittable(sb.toString());
  }

  // ["prop",value,"prop",value, ...]
  private static void encodeForJsoPayload(StringBuilder sb, AutoBean<?> bean) {
    Encoder e = new Encoder(bean.getFactory());
    e.push(sb);
    try {
      bean.accept(e);
    } catch (HaltException ex) {
      throw ex.getCause();
    }
  }

  private AutoBeanCodex() {
  }
}
