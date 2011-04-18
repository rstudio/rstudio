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
package com.google.web.bindery.autobean.shared.impl;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor.ParameterizationVisitor;
import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.ValueCodex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Contains the implementation details of AutoBeanCodex. This type was factored
 * out of AutoBeanCodex so that various implementation details can be accessed
 * without polluting a public API.
 */
public class AutoBeanCodexImpl {

  /**
   * Describes a means of encoding or decoding a particular type of data to or
   * from a wire format representation. Any given instance of a Coder should be
   * stateless; any state required for operation must be maintained in an
   * {@link EncodeState}.
   */
  public interface Coder {
    Object decode(EncodeState state, Splittable data);

    void encode(EncodeState state, Object value);

    Splittable extractSplittable(EncodeState state, Object value);
  }

  /**
   * Contains transient state for Coder operation.
   */
  public static class EncodeState {
    /**
     * Constructs a state object used for decoding payloads.
     */
    public static EncodeState forDecode(AutoBeanFactory factory) {
      return new EncodeState(factory, null);
    }

    /**
     * Constructs a state object used for encoding payloads.
     */
    public static EncodeState forEncode(AutoBeanFactory factory, StringBuilder sb) {
      return new EncodeState(factory, sb);
    }

    /**
     * Constructs a "stateless" state for testing Coders that do not require
     * AutoBean implementation details.
     */
    public static EncodeState forTesting() {
      return new EncodeState(null, null);
    }

    final EnumMap enumMap;
    final AutoBeanFactory factory;
    final StringBuilder sb;
    final Stack<AutoBean<?>> seen;

    private EncodeState(AutoBeanFactory factory, StringBuilder sb) {
      this.factory = factory;
      enumMap = factory instanceof EnumMap ? (EnumMap) factory : null;
      this.sb = sb;
      this.seen = sb == null ? null : new Stack<AutoBean<?>>();
    }
  }

  /**
   * Dynamically creates a Coder that is capable of operating on a particular
   * parameterization of a datastructure (e.g. {@code Map<String, List<String>>}
   * ).
   */
  static class CoderCreator extends ParameterizationVisitor {
    private Stack<Coder> stack = new Stack<Coder>();

    @Override
    public void endVisitType(Class<?> type) {
      if (List.class.equals(type) || Set.class.equals(type)) {
        stack.push(collectionCoder(type, stack.pop()));
      } else if (Map.class.equals(type)) {
        // Note that the parameters are passed in reverse order
        stack.push(mapCoder(stack.pop(), stack.pop()));
      } else if (Splittable.class.equals(type)) {
        stack.push(splittableCoder());
      } else if (type.getEnumConstants() != null) {
        @SuppressWarnings(value = {"unchecked"})
        Class<Enum<?>> enumType = (Class<Enum<?>>) type;
        stack.push(enumCoder(enumType));
      } else if (ValueCodex.canDecode(type)) {
        stack.push(valueCoder(type));
      } else {
        stack.push(objectCoder(type));
      }
    }

    public Coder getCoder() {
      assert stack.size() == 1 : "Incorrect size: " + stack.size();
      return stack.pop();
    }
  }

  /**
   * Constructs one of the lightweight collection types.
   */
  static class CollectionCoder implements Coder {
    private final Coder elementDecoder;
    private final Class<?> type;

    public CollectionCoder(Class<?> type, Coder elementDecoder) {
      this.elementDecoder = elementDecoder;
      this.type = type;
    }

    public Object decode(EncodeState state, Splittable data) {
      Collection<Object> collection;
      if (List.class.equals(type)) {
        collection = new SplittableList<Object>(data, elementDecoder, state);
      } else if (Set.class.equals(type)) {
        collection = new SplittableSet<Object>(data, elementDecoder, state);
      } else {
        // Should not reach here
        throw new RuntimeException(type.getName());
      }
      return collection;
    }

    public void encode(EncodeState state, Object value) {
      if (value == null) {
        state.sb.append("null");
        return;
      }

      Iterator<?> it = ((Collection<?>) value).iterator();
      state.sb.append("[");
      if (it.hasNext()) {
        elementDecoder.encode(state, it.next());
        while (it.hasNext()) {
          state.sb.append(",");
          elementDecoder.encode(state, it.next());
        }
      }
      state.sb.append("]");
    }

    public Splittable extractSplittable(EncodeState state, Object value) {
      return tryExtractSplittable(value);
    }
  }

  /**
   * Produces enums.
   * 
   * @param <E>
   */
  static class EnumCoder<E extends Enum<?>> implements Coder {
    private final Class<E> type;

    public EnumCoder(Class<E> type) {
      this.type = type;
    }

    public Object decode(EncodeState state, Splittable data) {
      return state.enumMap.getEnum(type, data.asString());
    }

    public void encode(EncodeState state, Object value) {
      if (value == null) {
        state.sb.append("null");
        return;
      }
      state.sb.append(StringQuoter.quote(state.enumMap.getToken((Enum<?>) value)));
    }

    public Splittable extractSplittable(EncodeState state, Object value) {
      return StringQuoter.split(StringQuoter.quote(state.enumMap.getToken((Enum<?>) value)));
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

  /**
   * Constructs one of the lightweight Map types, depending on the key type.
   */
  static class MapCoder implements Coder {
    private final Coder keyDecoder;
    private final Coder valueDecoder;

    /**
     * Parameters in reversed order to accommodate stack-based setup.
     */
    public MapCoder(Coder valueDecoder, Coder keyDecoder) {
      this.keyDecoder = keyDecoder;
      this.valueDecoder = valueDecoder;
    }

    public Object decode(EncodeState state, Splittable data) {
      Map<Object, Object> toReturn;
      if (data.isIndexed()) {
        assert data.size() == 2 : "Wrong data size: " + data.size();
        toReturn = new SplittableComplexMap<Object, Object>(data, keyDecoder, valueDecoder, state);
      } else {
        toReturn = new SplittableSimpleMap<Object, Object>(data, keyDecoder, valueDecoder, state);
      }
      return toReturn;
    }

    public void encode(EncodeState state, Object value) {
      if (value == null) {
        state.sb.append("null");
        return;
      }

      Map<?, ?> map = (Map<?, ?>) value;
      boolean isSimpleMap = keyDecoder instanceof ValueCoder;
      if (isSimpleMap) {
        boolean first = true;
        state.sb.append("{");
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
            state.sb.append(",");
          }

          keyDecoder.encode(state, mapKey);
          state.sb.append(":");
          if (mapValue == null) {
            // Null values must be preserved
            state.sb.append("null");
          } else {
            valueDecoder.encode(state, mapValue);
          }
        }
        state.sb.append("}");
      } else {
        List<Object> keys = new ArrayList<Object>(map.size());
        List<Object> values = new ArrayList<Object>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          keys.add(entry.getKey());
          values.add(entry.getValue());
        }
        state.sb.append("[");
        collectionCoder(List.class, keyDecoder).encode(state, keys);
        state.sb.append(",");
        collectionCoder(List.class, valueDecoder).encode(state, values);
        state.sb.append("]");
      }
    }

    public Splittable extractSplittable(EncodeState state, Object value) {
      return tryExtractSplittable(value);
    }
  }

  /**
   * Recurses into {@link AutoBeanCodexImpl}.
   */
  static class ObjectCoder implements Coder {
    private final Class<?> type;

    public ObjectCoder(Class<?> type) {
      this.type = type;
    }

    public Object decode(EncodeState state, Splittable data) {
      AutoBean<?> bean = doDecode(state, type, data);
      return bean == null ? null : bean.as();
    }

    public void encode(EncodeState state, Object value) {
      if (value == null) {
        state.sb.append("null");
        return;
      }
      doEncode(state, AutoBeanUtils.getAutoBean(value));
    }

    public Splittable extractSplittable(EncodeState state, Object value) {
      return tryExtractSplittable(value);
    }
  }

  static class PropertyCoderCreator extends AutoBeanVisitor {
    private AutoBean<?> bean;

    @Override
    public boolean visit(AutoBean<?> bean, Context ctx) {
      this.bean = bean;
      return true;
    }

    @Override
    public boolean visitReferenceProperty(String propertyName, AutoBean<?> value,
        PropertyContext ctx) {
      maybeCreateCoder(propertyName, ctx);
      return false;
    }

    @Override
    public boolean visitValueProperty(String propertyName, Object value, PropertyContext ctx) {
      maybeCreateCoder(propertyName, ctx);
      return false;
    }

    private void maybeCreateCoder(String propertyName, PropertyContext ctx) {
      CoderCreator creator = new CoderCreator();
      ctx.accept(creator);
      coderFor.put(key(bean, propertyName), creator.getCoder());
    }
  }

  /**
   * Extracts properties from a bean and turns them into JSON text.
   */
  static class PropertyGetter extends AutoBeanVisitor {
    private boolean first = true;
    private final EncodeState state;

    public PropertyGetter(EncodeState state) {
      this.state = state;
    }

    @Override
    public void endVisit(AutoBean<?> bean, Context ctx) {
      state.sb.append("}");
      state.seen.pop();
    }

    @Override
    public boolean visit(AutoBean<?> bean, Context ctx) {
      if (state.seen.contains(bean)) {
        throw new HaltException(new UnsupportedOperationException("Cycles not supported"));
      }
      state.seen.push(bean);
      state.sb.append("{");
      return true;
    }

    @Override
    public boolean visitReferenceProperty(String propertyName, AutoBean<?> value,
        PropertyContext ctx) {
      if (value != null) {
        encodeProperty(propertyName, value.as(), ctx);
      }
      return false;
    }

    @Override
    public boolean visitValueProperty(String propertyName, Object value, PropertyContext ctx) {
      if (value != null && !value.equals(ValueCodex.getUninitializedFieldValue(ctx.getType()))) {
        encodeProperty(propertyName, value, ctx);
      }
      return false;
    }

    private void encodeProperty(String propertyName, Object value, PropertyContext ctx) {
      CoderCreator pd = new CoderCreator();
      ctx.accept(pd);
      Coder decoder = pd.getCoder();
      if (first) {
        first = false;
      } else {
        state.sb.append(",");
      }
      state.sb.append(StringQuoter.quote(propertyName));
      state.sb.append(":");
      decoder.encode(state, value);
    }
  }

  /**
   * Populates beans with data extracted from an evaluated JSON payload.
   */
  static class PropertySetter extends AutoBeanVisitor {
    private Splittable data;
    private EncodeState state;

    public void decodeInto(EncodeState state, Splittable data, AutoBean<?> bean) {
      this.data = data;
      this.state = state;
      bean.accept(this);
    }

    @Override
    public boolean visitReferenceProperty(String propertyName, AutoBean<?> value,
        PropertyContext ctx) {
      decodeProperty(propertyName, ctx);
      return false;
    }

    @Override
    public boolean visitValueProperty(String propertyName, Object value, PropertyContext ctx) {
      decodeProperty(propertyName, ctx);
      return false;
    }

    protected void decodeProperty(String propertyName, PropertyContext ctx) {
      if (!data.isNull(propertyName)) {
        CoderCreator pd = new CoderCreator();
        ctx.accept(pd);
        Coder decoder = pd.getCoder();
        Object propertyValue = decoder.decode(state, data.get(propertyName));
        ctx.set(propertyValue);
      }
    }
  }

  /**
   * A passthrough Coder.
   */
  static class SplittableCoder implements Coder {
    static final Coder INSTANCE = new SplittableCoder();

    public Object decode(EncodeState state, Splittable data) {
      return data;
    }

    public void encode(EncodeState state, Object value) {
      if (value == null) {
        state.sb.append("null");
        return;
      }
      state.sb.append(((Splittable) value).getPayload());
    }

    public Splittable extractSplittable(EncodeState state, Object value) {
      return (Splittable) value;
    }
  }

  /**
   * Delegates to ValueCodex.
   */
  static class ValueCoder implements Coder {
    private final Class<?> type;

    public ValueCoder(Class<?> type) {
      assert type.getEnumConstants() == null : "Should use EnumTypeCodex";
      this.type = type;
    }

    public Object decode(EncodeState state, Splittable propertyValue) {
      if (propertyValue == null || propertyValue == Splittable.NULL) {
        return ValueCodex.getUninitializedFieldValue(type);
      }
      return ValueCodex.decode(type, propertyValue);
    }

    public void encode(EncodeState state, Object value) {
      state.sb.append(ValueCodex.encode(type, value).getPayload());
    }

    public Splittable extractSplittable(EncodeState state, Object value) {
      return ValueCodex.encode(type, value);
    }
  }

  /**
   * A map of AutoBean interface+property names to the Coder for that property.
   */
  private static final Map<String, Coder> coderFor = new HashMap<String, Coder>();
  /**
   * A map of types to a Coder that handles the type.
   */
  private static final Map<Class<?>, Coder> coders = new HashMap<Class<?>, Coder>();

  public static Coder collectionCoder(Class<?> type, Coder elementCoder) {
    return new CollectionCoder(type, elementCoder);
  }

  public static Coder doCoderFor(AutoBean<?> bean, String propertyName) {
    String key = key(bean, propertyName);
    Coder toReturn = coderFor.get(key);
    if (toReturn == null) {
      bean.accept(new PropertyCoderCreator());
      toReturn = coderFor.get(key);
      if (toReturn == null) {
        throw new IllegalArgumentException(propertyName);
      }
    }
    return toReturn;
  }

  public static <T> AutoBean<T> doDecode(EncodeState state, Class<T> clazz, Splittable data) {
    /*
     * If we decode the same Splittable twice, re-use the ProxyAutoBean to
     * maintain referential integrity. If we didn't do this, either facade would
     * update the same backing data, yet not be the same object via ==
     * comparison.
     */
    @SuppressWarnings("unchecked")
    AutoBean<T> toReturn = (AutoBean<T>) data.getReified(AutoBeanCodexImpl.class.getName());
    if (toReturn != null) {
      return toReturn;
    }
    toReturn = state.factory.create(clazz);
    data.setReified(AutoBeanCodexImpl.class.getName(), toReturn);
    if (toReturn == null) {
      throw new IllegalArgumentException(clazz.getName());
    }
    ((AbstractAutoBean<T>) toReturn).setData(data);
    return toReturn;
  }

  public static void doDecodeInto(EncodeState state, Splittable data, AutoBean<?> bean) {
    new PropertySetter().decodeInto(state, data, bean);
  }

  public static void doEncode(EncodeState state, AutoBean<?> bean) {
    PropertyGetter e = new PropertyGetter(state);
    try {
      bean.accept(e);
    } catch (HaltException ex) {
      throw ex.getCause();
    }
  }

  public static <E extends Enum<?>> Coder enumCoder(Class<E> type) {
    Coder toReturn = coders.get(type);
    if (toReturn == null) {
      toReturn = new EnumCoder<E>(type);
      coders.put(type, toReturn);
    }
    return toReturn;
  }

  public static Coder mapCoder(Coder valueCoder, Coder keyCoder) {
    return new MapCoder(valueCoder, keyCoder);
  }

  public static Coder objectCoder(Class<?> type) {
    Coder toReturn = coders.get(type);
    if (toReturn == null) {
      toReturn = new ObjectCoder(type);
      coders.put(type, toReturn);
    }
    return toReturn;
  }

  public static Coder splittableCoder() {
    return SplittableCoder.INSTANCE;
  }

  public static Coder valueCoder(Class<?> type) {
    Coder toReturn = coders.get(type);
    if (toReturn == null) {
      toReturn = new ValueCoder(type);
      coders.put(type, toReturn);
    }
    return toReturn;
  }

  static Splittable tryExtractSplittable(Object value) {
    AutoBean<?> bean = AutoBeanUtils.getAutoBean(value);
    if (bean != null) {
      value = bean;
    }
    if (bean instanceof HasSplittable) {
      return ((HasSplittable) bean).getSplittable();
    }
    return null;
  }

  private static String key(AutoBean<?> bean, String propertyName) {
    return bean.getType().getName() + ":" + propertyName;
  }
}
