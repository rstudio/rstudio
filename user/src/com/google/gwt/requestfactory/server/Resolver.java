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
package com.google.gwt.requestfactory.server;

import com.google.gwt.autobean.server.impl.TypeUtils;
import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanUtils;
import com.google.gwt.autobean.shared.AutoBeanVisitor;
import com.google.gwt.autobean.shared.Splittable;
import com.google.gwt.autobean.shared.ValueCodex;
import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.impl.Constants;
import com.google.gwt.requestfactory.shared.impl.SimpleProxyId;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Responsible for converting between domain and client entities. This class has
 * a small amount of temporary state used to handle graph cycles and assignment
 * of synthetic ids.
 * 
 * @see RequestState#getResolver()
 */
class Resolver {
  /**
   * A parameterized type with a single parameter.
   */
  private static class CollectionType implements ParameterizedType {
    private final Class<?> rawType;
    private final Class<?> elementType;

    private CollectionType(Class<?> rawType, Class<?> elementType) {
      this.rawType = rawType;
      this.elementType = elementType;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof CollectionType)) {
        return false;
      }
      CollectionType other = (CollectionType) o;
      return rawType.equals(other.rawType)
          && elementType.equals(other.elementType);
    }

    public Type[] getActualTypeArguments() {
      return new Type[] {elementType};
    }

    public Type getOwnerType() {
      return null;
    }

    public Type getRawType() {
      return rawType;
    }

    @Override
    public int hashCode() {
      return rawType.hashCode() * 13 + elementType.hashCode() * 7;
    }
  }

  /**
   * Used to map the objects being resolved and its API slice to the client-side
   * value. This handles the case where a domain object is returned to the
   * client mapped to two proxies of differing types.
   */
  private static class ResolutionKey {
    private final Object domainObject;
    private final int hashCode;
    private final Type requestedType;

    public ResolutionKey(Object domainObject, Type requestedType) {
      this.domainObject = domainObject;
      this.requestedType = requestedType;
      this.hashCode = System.identityHashCode(domainObject) * 13
          + requestedType.hashCode() * 7;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ResolutionKey)) {
        return false;
      }
      ResolutionKey other = (ResolutionKey) o;
      // Object identity comparison intentional
      if (domainObject != other.domainObject) {
        return false;
      }
      if (!requestedType.equals(other.requestedType)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    /**
     * For debugging use only.
     */
    @Override
    public String toString() {
      return domainObject.toString() + " => " + requestedType.toString();
    }
  }

  /**
   * Returns the trailing {@code [n]} index value from a path.
   */
  static int index(String path) {
    int idx = path.lastIndexOf('[');
    if (idx == -1) {
      return -1;
    }
    return Integer.parseInt(path.substring(idx + 1, path.lastIndexOf(']')));
  }

  /**
   * Returns {@code true} if the given prefix is one of the requested property
   * references.
   */
  static boolean matchesPropertyRef(Set<String> propertyRefs, String newPrefix) {
    return propertyRefs.contains(newPrefix.replaceAll("\\[\\d+\\]", ""));
  }

  /**
   * Removes the trailing {@code [n]} from a path.
   */
  static String snipIndex(String path) {
    int idx = path.lastIndexOf('[');
    if (idx == -1) {
      return path;
    }
    return path.substring(0, idx);
  }

  /**
   * Maps domain values to client values. This map prevents cycles in the object
   * graph from causing infinite recursion.
   */
  private final Map<ResolutionKey, Object> resolved = new HashMap<ResolutionKey, Object>();
  private final ServiceLayer service;
  private final RequestState state;
  private int syntheticId;

  /**
   * Should only be called from {@link RequestState}.
   */
  Resolver(RequestState state) {
    this.state = state;
    this.service = state.getServiceLayer();
  }

  /**
   * Given a domain object, return a value that can be encoded by the client.
   * 
   * @param domainValue the domain object to be converted into a client-side
   *          value
   * @param assignableTo the type in the client to which the resolved value
   *          should be assignable. A value of {@code null} indicates that any
   *          resolution will suffice.
   * @param propertyRefs the property references requested by the client
   */
  public Object resolveClientValue(Object domainValue, Type assignableTo,
      Set<String> propertyRefs) {
    return resolveClientValue(domainValue, assignableTo,
        getPropertyRefs(propertyRefs), "");
  }

  /**
   * Convert a client-side value into a domain value.
   * 
   * @param maybeEntityProxy the client object to resolve
   * @param detectDeadEntities if <code>true</code> this method will throw a
   *          ReportableException containing a {@link DeadEntityException} if an
   *          EntityProxy cannot be resolved
   */
  public Object resolveDomainValue(Object maybeEntityProxy,
      boolean detectDeadEntities) {
    if (maybeEntityProxy instanceof BaseProxy) {
      AutoBean<BaseProxy> bean = AutoBeanUtils.getAutoBean((BaseProxy) maybeEntityProxy);
      Object domain = bean.getTag(Constants.DOMAIN_OBJECT);
      if (domain == null && detectDeadEntities) {
        throw new ReportableException(new DeadEntityException(
            "The requested entity is not available on the server"));
      }
      return domain;
    } else if (maybeEntityProxy instanceof Collection<?>) {
      Collection<Object> accumulator;
      if (maybeEntityProxy instanceof List) {
        accumulator = new ArrayList<Object>();
      } else if (maybeEntityProxy instanceof Set) {
        accumulator = new HashSet<Object>();
      } else {
        throw new ReportableException("Unsupported collection type "
            + maybeEntityProxy.getClass().getName());
      }
      for (Object o : (Collection<?>) maybeEntityProxy) {
        accumulator.add(resolveDomainValue(o, detectDeadEntities));
      }
      return accumulator;
    }
    return maybeEntityProxy;
  }

  /**
   * Expand the property references in an InvocationMessage into a
   * fully-expanded list of properties. For example, <code>[foo.bar.baz]</code>
   * will be converted into <code>[foo, foo.bar, foo.bar.baz]</code>.
   */
  private Set<String> getPropertyRefs(Set<String> refs) {
    if (refs == null) {
      return Collections.emptySet();
    }

    Set<String> toReturn = new TreeSet<String>();
    for (String raw : refs) {
      for (int idx = raw.length(); idx >= 0; idx = raw.lastIndexOf('.', idx - 1)) {
        toReturn.add(raw.substring(0, idx));
      }
    }
    return toReturn;
  }

  /**
   * Converts a domain entity into an EntityProxy that will be sent to the
   * client.
   */
  private <T extends BaseProxy> T resolveClientProxy(final Object domainEntity,
      Class<T> proxyType, final Set<String> propertyRefs, ResolutionKey key,
      final String prefix) {
    if (domainEntity == null) {
      return null;
    }

    SimpleProxyId<? extends BaseProxy> id = state.getStableId(domainEntity);

    boolean isEntityProxy = state.isEntityType(proxyType);
    final boolean isOwnerValueProxy = state.isValueType(proxyType);
    Object domainVersion;

    // Create the id or update an ephemeral id by calculating its address
    if (id == null || id.isEphemeral()) {
      // The address is an id or an id plus a path
      Object domainId;
      if (isEntityProxy) {
        // Compute data needed to return id to the client
        domainId = service.getId(domainEntity);
        domainVersion = service.getVersion(domainEntity);
      } else {
        domainId = null;
        domainVersion = null;
      }
      if (id == null) {
        if (domainId == null) {
          /*
           * This will happen when server code attempts to return an unpersisted
           * object to the client. In this case, we'll assign a synthetic id
           * that is valid for the duration of the response. The client is
           * expected to assign a client-local id to this object and then it
           * will behave as though it were an object newly-created by the
           * client.
           */
          id = state.getIdFactory().allocateSyntheticId(proxyType,
              ++syntheticId);
        } else {
          Splittable flatValue = state.flatten(domainId);
          id = state.getIdFactory().getId(proxyType, flatValue.getPayload(), 0);
        }
      } else if (domainId != null) {
        // Mark an ephemeral id as having been persisted
        Splittable flatValue = state.flatten(domainId);
        id.setServerId(flatValue.getPayload());
      }
    } else if (isEntityProxy) {
      // Already have the id, just pull the current version
      domainVersion = service.getVersion(domainEntity);
    } else {
      // The version of a value object is always null
      domainVersion = null;
    }

    @SuppressWarnings("unchecked")
    AutoBean<T> bean = (AutoBean<T>) state.getBeanForPayload(id, domainEntity);
    resolved.put(key, bean.as());
    bean.setTag(Constants.IN_RESPONSE, true);
    if (domainVersion != null) {
      Splittable flatVersion = state.flatten(domainVersion);
      bean.setTag(Constants.VERSION_PROPERTY_B64,
          SimpleRequestProcessor.toBase64(flatVersion.getPayload()));
    }

    bean.accept(new AutoBeanVisitor() {

      @Override
      public boolean visitReferenceProperty(String propertyName,
          AutoBean<?> value, PropertyContext ctx) {
        // Does the user care about the property?
        String newPrefix = (prefix.length() > 0 ? (prefix + ".") : "")
            + propertyName;

        /*
         * Send the property if the enclosing type is a ValueProxy, if the owner
         * requested the property, or if the property is a list of values.
         */
        Class<?> elementType = ctx instanceof CollectionPropertyContext
            ? ((CollectionPropertyContext) ctx).getElementType() : null;
        boolean shouldSend = isOwnerValueProxy
            || matchesPropertyRef(propertyRefs, newPrefix)
            || elementType != null && ValueCodex.canDecode(elementType);

        if (!shouldSend) {
          return false;
        }

        // Call the getter
        Object domainValue = service.getProperty(domainEntity, propertyName);
        if (domainValue == null) {
          return false;
        }

        // Turn the domain object into something usable on the client side
        Type type;
        if (elementType == null) {
          type = ctx.getType();
        } else {
          type = new CollectionType(ctx.getType(), elementType);
        }
        Object clientValue = resolveClientValue(domainValue, type,
            propertyRefs, newPrefix);

        ctx.set(clientValue);
        return false;
      }

      @Override
      public boolean visitValueProperty(String propertyName, Object value,
          PropertyContext ctx) {
        // Limit unrequested value properties?
        value = service.getProperty(domainEntity, propertyName);
        ctx.set(value);
        return false;
      }
    });

    return bean.as();
  }

  /**
   * Recursive-descent implementation.
   */
  private Object resolveClientValue(Object domainValue, Type returnType,
      Set<String> propertyRefs, String prefix) {
    if (domainValue == null) {
      return null;
    }

    boolean anyType = returnType == null;
    if (anyType) {
      returnType = Object.class;
    }

    Class<?> assignableTo = TypeUtils.ensureBaseType(returnType);
    ResolutionKey key = new ResolutionKey(domainValue, returnType);

    Object previous = resolved.get(key);
    if (previous != null && assignableTo.isInstance(previous)) {
      return assignableTo.cast(previous);
    }

    Class<?> returnClass = service.resolveClientType(domainValue.getClass(),
        assignableTo, true);

    if (anyType) {
      assignableTo = returnClass;
    }

    // Pass simple values through
    if (ValueCodex.canDecode(returnClass)) {
      return assignableTo.cast(domainValue);
    }

    // Convert entities to EntityProxies or EntityProxyIds
    boolean isProxy = BaseProxy.class.isAssignableFrom(returnClass);
    boolean isId = EntityProxyId.class.isAssignableFrom(returnClass);
    if (isProxy || isId) {
      Class<? extends BaseProxy> proxyClass = assignableTo.asSubclass(BaseProxy.class);
      BaseProxy entity = resolveClientProxy(domainValue, proxyClass,
          propertyRefs, key, prefix);
      if (isId) {
        return assignableTo.cast(((EntityProxy) entity).stableId());
      }
      return assignableTo.cast(entity);
    }

    // Convert collections
    if (Collection.class.isAssignableFrom(returnClass)) {
      Collection<Object> accumulator;
      if (List.class.isAssignableFrom(returnClass)) {
        accumulator = new ArrayList<Object>();
      } else if (Set.class.isAssignableFrom(returnClass)) {
        accumulator = new HashSet<Object>();
      } else {
        throw new ReportableException("Unsupported collection type"
            + returnClass.getName());
      }
      resolved.put(key, accumulator);

      Type elementType = TypeUtils.getSingleParameterization(Collection.class,
          returnType);
      for (Object o : (Collection<?>) domainValue) {
        accumulator.add(resolveClientValue(o, elementType, propertyRefs, prefix));
      }
      return assignableTo.cast(accumulator);
    }

    throw new ReportableException("Unsupported domain type "
        + returnClass.getCanonicalName());
  }
}