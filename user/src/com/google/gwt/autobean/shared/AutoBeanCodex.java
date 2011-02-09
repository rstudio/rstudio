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

import com.google.gwt.autobean.shared.AutoBeanVisitor.ParameterizationVisitor;
import com.google.gwt.autobean.shared.impl.EnumMap;
import com.google.gwt.autobean.shared.impl.LazySplittable;
import com.google.gwt.autobean.shared.impl.StringQuoter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

  /**
   * Describes a means of encoding or decoding a particular type of data to or
   * from a wire format representation.
   */
  interface Coder {
    Object decode(Splittable data);

    void encode(StringBuilder sb, Object value);
  }

  /**
   * Creates a Coder that is capable of operating on a particular
   * parameterization of a datastructure (e.g. {@code Map<String, List<String>>}
   * ).
   */
  class CoderCreator extends ParameterizationVisitor {
    private Stack<Coder> stack = new Stack<Coder>();

    @Override
    public void endVisitType(Class<?> type) {
      if (List.class.equals(type) || Set.class.equals(type)) {
        stack.push(new CollectionCoder(type, stack.pop()));
      } else if (Map.class.equals(type)) {
        // Note that the parameters are passed in reverse order
        stack.push(new MapCoder(stack.pop(), stack.pop()));
      } else if (Splittable.class.equals(type)) {
        stack.push(new SplittableDecoder());
      } else if (type.getEnumConstants() != null) {
        @SuppressWarnings(value = {"rawtypes", "unchecked"})
        EnumCoder decoder = new EnumCoder(type);
        stack.push(decoder);
      } else if (ValueCodex.canDecode(type)) {
        stack.push(new ValueCoder(type));
      } else {
        stack.push(new ObjectCoder(type));
      }
    }

    public Coder getCoder() {
      assert stack.size() == 1 : "Incorrect size: " + stack.size();
      return stack.pop();
    }
  }

  class CollectionCoder implements Coder {
    private final Coder elementDecoder;
    private final Class<?> type;

    public CollectionCoder(Class<?> type, Coder elementDecoder) {
      this.elementDecoder = elementDecoder;
      this.type = type;
    }

    public Object decode(Splittable data) {
      Collection<Object> collection;
      if (List.class.equals(type)) {
        collection = new ArrayList<Object>();
      } else if (Set.class.equals(type)) {
        collection = new HashSet<Object>();
      } else {
        // Should not reach here
        throw new RuntimeException(type.getName());
      }
      for (int i = 0, j = data.size(); i < j; i++) {
        Object element = data.isNull(i) ? null
            : elementDecoder.decode(data.get(i));
        collection.add(element);
      }
      return collection;
    }

    public void encode(StringBuilder sb, Object value) {
      if (value == null) {
        sb.append("null");
        return;
      }

      Iterator<?> it = ((Collection<?>) value).iterator();
      sb.append("[");
      if (it.hasNext()) {
        elementDecoder.encode(sb, it.next());
        while (it.hasNext()) {
          sb.append(",");
          elementDecoder.encode(sb, it.next());
        }
      }
      sb.append("]");
    }
  }

  class EnumCoder<E extends Enum<E>> implements Coder {
    private final Class<E> type;

    public EnumCoder(Class<E> type) {
      this.type = type;
    }

    public Object decode(Splittable data) {
      return enumMap.getEnum(type, data.asString());
    }

    public void encode(StringBuilder sb, Object value) {
      if (value == null) {
        sb.append("null");
      }
      sb.append(StringQuoter.quote(enumMap.getToken((Enum<?>) value)));
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

  class MapCoder implements Coder {
    private final Coder keyDecoder;
    private final Coder valueDecoder;

    /**
     * Parameters in reversed order to accommodate stack-based setup.
     */
    public MapCoder(Coder valueDecoder, Coder keyDecoder) {
      this.keyDecoder = keyDecoder;
      this.valueDecoder = valueDecoder;
    }

    public Object decode(Splittable data) {
      Map<Object, Object> toReturn = new HashMap<Object, Object>();
      if (data.isIndexed()) {
        assert data.size() == 2 : "Wrong data size: " + data.size();
        Splittable keys = data.get(0);
        Splittable values = data.get(1);
        for (int i = 0, j = keys.size(); i < j; i++) {
          Object key = keys.isNull(i) ? null : keyDecoder.decode(keys.get(i));
          Object value = values.isNull(i) ? null
              : valueDecoder.decode(values.get(i));
          toReturn.put(key, value);
        }
      } else {
        ValueCoder keyValueDecoder = (ValueCoder) keyDecoder;
        for (String rawKey : data.getPropertyKeys()) {
          Object key = keyValueDecoder.decode(rawKey);
          Object value = data.isNull(rawKey) ? null
              : valueDecoder.decode(data.get(rawKey));
          toReturn.put(key, value);
        }
      }
      return toReturn;
    }

    public void encode(StringBuilder sb, Object value) {
      if (value == null) {
        sb.append("null");
        return;
      }

      Map<?, ?> map = (Map<?, ?>) value;
      boolean isSimpleMap = keyDecoder instanceof ValueCoder;
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
          if (mapValue == null) {
            // A null value can be ignored
            continue;
          }

          if (first) {
            first = false;
          } else {
            sb.append(",");
          }

          keyDecoder.encode(sb, mapKey);
          sb.append(":");
          valueDecoder.encode(sb, mapValue);
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
        new CollectionCoder(List.class, keyDecoder).encode(sb, keys);
        sb.append(",");
        new CollectionCoder(List.class, valueDecoder).encode(sb, values);
        sb.append("]");
      }
    }
  }

  class ObjectCoder implements Coder {
    private final Class<?> type;

    public ObjectCoder(Class<?> type) {
      this.type = type;
    }

    public Object decode(Splittable data) {
      AutoBean<?> bean = doDecode(type, data);
      return bean == null ? null : bean.as();
    }

    public void encode(StringBuilder sb, Object value) {
      if (value == null) {
        sb.append("null");
        return;
      }
      doEncode(sb, AutoBeanUtils.getAutoBean(value));
    }
  }

  /**
   * Extracts properties from a bean and turns them into JSON text.
   */
  class PropertyGetter extends AutoBeanVisitor {
    private boolean first = true;
    private final StringBuilder sb;

    public PropertyGetter(StringBuilder sb) {
      this.sb = sb;
    }

    @Override
    public void endVisit(AutoBean<?> bean, Context ctx) {
      sb.append("}");
      seen.pop();
    }

    @Override
    public boolean visit(AutoBean<?> bean, Context ctx) {
      if (seen.contains(bean)) {
        throw new HaltException(new UnsupportedOperationException(
            "Cycles not supported"));
      }
      seen.push(bean);
      sb.append("{");
      return true;
    }

    @Override
    public boolean visitReferenceProperty(String propertyName,
        AutoBean<?> value, PropertyContext ctx) {
      if (value != null) {
        encodeProperty(propertyName, value.as(), ctx);
      }
      return false;
    }

    @Override
    public boolean visitValueProperty(String propertyName, Object value,
        PropertyContext ctx) {
      if (value != null
          && !value.equals(ValueCodex.getUninitializedFieldValue(ctx.getType()))) {
        encodeProperty(propertyName, value, ctx);
      }
      return false;
    }

    private void encodeProperty(String propertyName, Object value,
        PropertyContext ctx) {
      CoderCreator pd = new CoderCreator();
      ctx.accept(pd);
      Coder decoder = pd.getCoder();
      if (first) {
        first = false;
      } else {
        sb.append(",");
      }
      sb.append(StringQuoter.quote(propertyName));
      sb.append(":");
      decoder.encode(sb, value);
    }
  }

  /**
   * Populates beans with data extracted from an evaluated JSON payload.
   */
  class PropertySetter extends AutoBeanVisitor {
    private Splittable data;

    public void decodeInto(Splittable data, AutoBean<?> bean) {
      this.data = data;
      bean.accept(this);
    }

    @Override
    public boolean visitReferenceProperty(String propertyName,
        AutoBean<?> value, PropertyContext ctx) {
      decodeProperty(propertyName, ctx);
      return false;
    }

    @Override
    public boolean visitValueProperty(String propertyName, Object value,
        PropertyContext ctx) {
      decodeProperty(propertyName, ctx);
      return false;
    }

    protected void decodeProperty(String propertyName, PropertyContext ctx) {
      if (!data.isNull(propertyName)) {
        CoderCreator pd = new CoderCreator();
        ctx.accept(pd);
        Coder decoder = pd.getCoder();
        Object propertyValue = decoder.decode(data.get(propertyName));
        ctx.set(propertyValue);
      }
    }
  }

  class SplittableDecoder implements Coder {
    public Object decode(Splittable data) {
      return data;
    }

    public void encode(StringBuilder sb, Object value) {
      if (value == null) {
        sb.append("null");
        return;
      }
      sb.append(((Splittable) value).getPayload());
    }
  }

  class ValueCoder implements Coder {
    private final Class<?> type;

    public ValueCoder(Class<?> type) {
      assert type.getEnumConstants() == null : "Should use EnumTypeCodex";
      this.type = type;
    }

    public Object decode(Splittable propertyValue) {
      return decode(propertyValue.asString());
    }

    public Object decode(String propertyValue) {
      return ValueCodex.decode(type, propertyValue);
    }

    public void encode(StringBuilder sb, Object value) {
      sb.append(ValueCodex.encode(value).getPayload());
    }
  }

  public static <T> AutoBean<T> decode(AutoBeanFactory factory, Class<T> clazz,
      Splittable data) {
    return new AutoBeanCodex(factory).doDecode(clazz, data);
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
    new AutoBeanCodex(bean.getFactory()).doEncode(sb, bean);
    return new LazySplittable(sb.toString());
  }

  private final EnumMap enumMap;
  private final AutoBeanFactory factory;
  private final Stack<AutoBean<?>> seen = new Stack<AutoBean<?>>();

  private AutoBeanCodex(AutoBeanFactory factory) {
    this.factory = factory;
    this.enumMap = factory instanceof EnumMap ? (EnumMap) factory : null;
  }

  <T> AutoBean<T> doDecode(Class<T> clazz, Splittable data) {
    AutoBean<T> toReturn = factory.create(clazz);
    if (toReturn == null) {
      throw new IllegalArgumentException(clazz.getName());
    }
    doDecodeInto(data, toReturn);
    return toReturn;
  }

  void doDecodeInto(Splittable data, AutoBean<?> bean) {
    new PropertySetter().decodeInto(data, bean);
  }

  void doEncode(StringBuilder sb, AutoBean<?> bean) {
    PropertyGetter e = new PropertyGetter(sb);
    try {
      bean.accept(e);
    } catch (HaltException ex) {
      throw ex.getCause();
    }
  }
}
