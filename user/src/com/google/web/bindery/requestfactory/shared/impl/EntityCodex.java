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
package com.google.web.bindery.requestfactory.shared.impl;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.ValueCodex;
import com.google.web.bindery.autobean.shared.impl.StringQuoter;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analogous to {@link ValueCodex}, but for object types.
 */
public class EntityCodex {
  /**
   * Abstracts the process by which EntityProxies are created.
   */
  public interface EntitySource {
    /**
     * Expects an encoded
     * {@link com.google.web.bindery.requestfactory.shared.messages.IdMessage}.
     */
    <Q extends BaseProxy> AutoBean<Q> getBeanForPayload(Splittable serializedIdMessage);

    /**
     * Should return an encoded
     * {@link com.google.web.bindery.requestfactory.shared.messages.IdMessage}.
     */
    Splittable getSerializedProxyId(SimpleProxyId<?> stableId);

    boolean isEntityType(Class<?> clazz);

    boolean isValueType(Class<?> clazz);
  }

  /**
   * Collection support is limited to value types and resolving ids.
   */
  public static Object decode(EntitySource source, Class<?> type, Class<?> elementType,
      Splittable split) {
    if (split == null || split == Splittable.NULL) {
      return null;
    }

    // Collection support
    if (elementType != null) {
      Collection<Object> collection = null;
      if (List.class.equals(type)) {
        collection = new ArrayList<Object>();
      } else if (Set.class.equals(type)) {
        collection = new HashSet<Object>();
      } else {
        throw new UnsupportedOperationException();
      }

      // Decode values
      if (ValueCodex.canDecode(elementType)) {
        for (int i = 0, j = split.size(); i < j; i++) {
          if (split.isNull(i)) {
            collection.add(null);
          } else {
            Object element = ValueCodex.decode(elementType, split.get(i));
            collection.add(element);
          }
        }
      } else {
        for (int i = 0, j = split.size(); i < j; i++) {
          if (split.isNull(i)) {
            collection.add(null);
          } else {
            Object element = decode(source, elementType, null, split.get(i));
            collection.add(element);
          }
        }
      }
      return collection;
    }

    if (source.isEntityType(type) || source.isValueType(type) || EntityProxyId.class.equals(type)) {
      return source.getBeanForPayload(split).as();
    }

    // Fall back to values
    return ValueCodex.decode(type, split);
  }

  /**
   * Collection support is limited to value types and resolving ids.
   */
  public static Object decode(EntitySource source, Class<?> type, Class<?> elementType,
      String jsonPayload) {
    Splittable split = StringQuoter.split(jsonPayload);
    return decode(source, type, elementType, split);
  }

  /**
   * Map decoding follows behaviour of AutoBeanCodexImpl.MapCoder
   */
  public static Object decode(EntitySource source,
      Class<?> type, Class<?> keyType, Class<?> valueType, Splittable split) {
    if (split == null || split == Splittable.NULL) {
      return null;
    }

    if (!Map.class.equals(type)) {
      throw new UnsupportedOperationException();
    }

    Map<Object, Object> map = new HashMap<Object, Object>();
    if (ValueCodex.canDecode(keyType) || !split.isIndexed()) {
      List<String> keys = split.getPropertyKeys();
      for (String propertyKey : keys) {
        Object key = (keyType == String.class) ?
            propertyKey : ValueCodex.decode(keyType, StringQuoter.split(propertyKey));
        if (split.isNull(propertyKey)) {
          map.put(key, null);
        } else {
          Splittable valueSplit = split.get(propertyKey);
          Object value = null;
          if (ValueCodex.canDecode(valueType)) {
            value = ValueCodex.decode(valueType, valueSplit);
          } else {
            value = decode(source, valueType, null, valueSplit);
          }
          map.put(key, value);
        }
      }
    } else {
       if (split.size() != 2) {
         throw new UnsupportedOperationException();
       }
       List<?> keys = (List<?>) decode(source, List.class, keyType, split.get(0));
       List<?> values = (List<?>) decode(source, List.class, valueType, split.get(1));
       if (keys.size() != values.size()) {
         throw new UnsupportedOperationException();
       }

       for (int i = 0, size = keys.size(); i < size; i++) {
         map.put(keys.get(i), values.get(i));
       }
    }

    return map;
  }

  /**
   * Create a wire-format representation of an object.
   */
  public static Splittable encode(EntitySource source, Object value) {
    if (value == null) {
      return Splittable.NULL;
    }

    if (value instanceof Poser<?>) {
      value = ((Poser<?>) value).getPosedValue();
    }

    if (value instanceof Iterable<?>) {
      StringBuffer toReturn = new StringBuffer();
      toReturn.append('[');
      boolean first = true;
      for (Object val : ((Iterable<?>) value)) {
        if (!first) {
          toReturn.append(',');
        } else {
          first = false;
        }
        if (val == null) {
          toReturn.append("null");
        } else {
          toReturn.append(encode(source, val).getPayload());
        }
      }
      toReturn.append(']');
      return StringQuoter.split(toReturn.toString());
    }

    // Map encoding follows behaviour of AutoBeanCodexImpl.MapCoder
    if (value instanceof Map<?, ?>) {
      Map<?, ?> map = (Map<?, ?>) value;
      StringBuilder sb = new StringBuilder();

      if (map.containsKey(null)) {
        throw new IllegalArgumentException("null Map keys are not supported");
      }

      boolean isSimpleMap = (map.isEmpty() || ValueCodex.canDecode(map.keySet().iterator().next().getClass()));
      if (isSimpleMap) {
        boolean first = true;
        sb.append("{");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          Object mapKey = entry.getKey();
          if (mapKey == null) {
            // A null key in a simple map is meaningless
            continue;
          }
          Object mapValue = entry.getValue();

          if (first) {
            first = false;
          } else {
            sb.append(",");
          }

          final String encodedKey = (mapKey.getClass() == String.class) ?
              (String) mapKey : encode(source, mapKey).getPayload();
          sb.append(StringQuoter.quote(encodedKey));
          sb.append(":");
          if (mapValue == null) {
            // Null values must be preserved
            sb.append("null");
          } else {
            sb.append(encode(source, mapValue).getPayload());
          }
        }
        sb.append("}");
      } else {
        List<Object> keys = new ArrayList<Object>(map.size());
        List<Object> values = new ArrayList<Object>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          keys.add(entry.getKey());
          values.add(entry.getValue());
        }
        sb.append("[");
        sb.append(encode(source, keys).getPayload());
        sb.append(",");
        sb.append(encode(source, values).getPayload());
        sb.append("]");
      }

      return StringQuoter.split(sb.toString());
    }

    if (value instanceof BaseProxy) {
      AutoBean<BaseProxy> autoBean = AutoBeanUtils.getAutoBean((BaseProxy) value);
      value = BaseProxyCategory.stableId(autoBean);
    }

    if (value instanceof SimpleProxyId<?>) {
      return source.getSerializedProxyId((SimpleProxyId<?>) value);
    }

    return ValueCodex.encode(value);
  }
}
