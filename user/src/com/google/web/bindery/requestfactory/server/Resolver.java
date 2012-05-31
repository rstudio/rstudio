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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor;
import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.ValueCodex;
import com.google.web.bindery.autobean.vm.impl.TypeUtils;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.impl.Constants;
import com.google.web.bindery.requestfactory.shared.impl.SimpleProxyId;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
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
      return rawType.equals(other.rawType) && elementType.equals(other.elementType);
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
   * Copies values and references from a domain object to a client object. This
   * type does not descend into referenced objects.
   */
  private class PropertyResolver extends AutoBeanVisitor {
    private final Object domainEntity;
    private final boolean isOwnerValueProxy;
    private final boolean needsSimpleValues;
    private final Set<String> propertyRefs;

    private PropertyResolver(Resolution resolution) {
      ResolutionKey key = resolution.getResolutionKey();
      this.domainEntity = key.getDomainObject();
      this.isOwnerValueProxy = state.isValueType(TypeUtils.ensureBaseType(key.requestedType));
      this.needsSimpleValues = resolution.needsSimpleValues();
      this.propertyRefs = resolution.takeWork();
    }

    @Override
    public boolean visitReferenceProperty(String propertyName, AutoBean<?> value,
        PropertyContext ctx) {
      /*
       * Send the property if the enclosing type is a ValueProxy, if the owner
       * requested the property, or if the property is a list of values.
       */
      Class<?> elementType =
          ctx instanceof CollectionPropertyContext ? ((CollectionPropertyContext) ctx)
              .getElementType() : null;
      boolean shouldSend =
          isOwnerValueProxy || matchesPropertyRef(propertyRefs, propertyName)
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
      Resolution resolution = resolveClientValue(domainValue, type);
      addPathsToResolution(resolution, propertyName, propertyRefs);
      ctx.set(resolution.getClientObject());
      return false;
    }

    @Override
    public boolean visitValueProperty(String propertyName, Object value, PropertyContext ctx) {
      /*
       * Only call the getter for simple values once since they're not
       * explicitly enumerated.
       */
      if (needsSimpleValues) {
        // Limit unrequested value properties?
        value = service.getProperty(domainEntity, propertyName);
        ctx.set(value);
      }
      return false;
    }
  }

  /**
   * Tracks the state of resolving a single client object.
   */
  private static class Resolution {
    /**
     * There's no Collections shortcut for this.
     */
    private static final SortedSet<String> EMPTY = Collections
        .unmodifiableSortedSet(new TreeSet<String>());

    /**
     * The client object.
     */
    private final Object clientObject;

    /**
     * A one-shot flag for {@link #hasWork()} to ensure that simple properties
     * will be resolved, even when there's no requested property set.
     */
    private boolean needsSimpleValues;
    private SortedSet<String> toResolve = EMPTY;
    private final SortedSet<String> resolved = new TreeSet<String>();
    private final ResolutionKey key;

    public Resolution(Object simpleValue) {
      assert !(simpleValue instanceof Resolution);
      this.clientObject = simpleValue;
      this.key = null;
    }

    public Resolution(ResolutionKey key, BaseProxy clientObject) {
      this.clientObject = clientObject;
      this.key = key;
      needsSimpleValues = true;
    }

    /**
     * Removes the prefix from each requested path and enqueues paths that have
     * not been previously resolved for the next batch of work.
     */
    public void addPaths(String prefix, Collection<String> requestedPaths) {
      if (clientObject == null) {
        // No point trying to follow paths past a null value
        return;
      }
      
      // Identity comparison intentional
      if (toResolve == EMPTY) {
        toResolve = new TreeSet<String>();
      }
      prefix = prefix.isEmpty() ? prefix : (prefix + ".");
      int prefixLength = prefix.length();
      for (String path : requestedPaths) {
        if (path.startsWith(prefix)) {
          toResolve.add(path.substring(prefixLength));
        } else if (path.startsWith("*.")) {
          toResolve.add(path.substring("*.".length()));
        }
      }
      toResolve.removeAll(resolved);
      if (toResolve.isEmpty()) {
        toResolve = EMPTY;
      }
    }

    public Object getClientObject() {
      return clientObject;
    }

    public ResolutionKey getResolutionKey() {
      return key;
    }

    public boolean hasWork() {
      return needsSimpleValues || !toResolve.isEmpty();
    }

    public boolean needsSimpleValues() {
      return needsSimpleValues;
    }

    /**
     * Returns client-object-relative reference paths that should be further
     * resolved.
     */
    public SortedSet<String> takeWork() {
      needsSimpleValues = false;
      SortedSet<String> toReturn = toResolve;
      resolved.addAll(toReturn);
      toResolve = EMPTY;
      return toReturn;
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
      this.hashCode = System.identityHashCode(domainObject) * 13 + requestedType.hashCode() * 7;
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

    public Object getDomainObject() {
      return domainObject;
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
    /* 
     * Match all fields for a wildcard
     * 
     * Also, remove list index suffixes. Not actually used, was in anticipation
     * of OGNL type schemes. That said, Editor will slip in such things.
     */
    return propertyRefs.contains("*")
        || propertyRefs.contains(newPrefix.replaceAll("\\[\\d+\\]", ""));
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
   * Expand the property references in an InvocationMessage into a
   * fully-expanded list of properties. For example, <code>[foo.bar.baz]</code>
   * will be converted into <code>[foo, foo.bar, foo.bar.baz]</code>.
   */
  private static Set<String> expandPropertyRefs(Set<String> refs) {
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
   * Maps proxy instances to the Resolution objects.
   *<p>
   * FIXME: The proxies are later mutated, which is not an issue as this is an
   * IdentityHashMap, but still feels weird. We should try to find a way to
   * put immutable objects as keys in this map.
   */
  private IdentityHashMap<BaseProxy, Resolution> clientObjectsToResolutions =
      new IdentityHashMap<BaseProxy, Resolution>();
  /**
   * Maps domain values to client values. This map prevents cycles in the object
   * graph from causing infinite recursion.
   */
  private final Map<ResolutionKey, Resolution> resolved = new HashMap<ResolutionKey, Resolution>();
  private final ServiceLayer service;
  private final RequestState state;
  /**
   * Contains Resolutions with path references that have not yet been resolved.
   */
  private Set<Resolution> toProcess = new LinkedHashSet<Resolution>();
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
  public Object resolveClientValue(Object domainValue, Type assignableTo, Set<String> propertyRefs) {
    Resolution toReturn = resolveClientValue(domainValue, assignableTo);
    if (toReturn == null) {
      return null;
    }
    addPathsToResolution(toReturn, "", expandPropertyRefs(propertyRefs));
    while (!toProcess.isEmpty()) {
      List<Resolution> working = new ArrayList<Resolution>(toProcess);
      toProcess.clear();
      for (Resolution resolution : working) {
        if (resolution.hasWork()) {
          AutoBean<BaseProxy> bean =
              AutoBeanUtils.getAutoBean((BaseProxy) resolution.getClientObject());
          bean.accept(new PropertyResolver(resolution));
        }
      }
    }
    return toReturn.getClientObject();
  }

  /**
   * Convert a client-side value into a domain value.
   * 
   * @param maybeEntityProxy the client object to resolve
   * @param detectDeadEntities if <code>true</code> this method will throw a
   *          ReportableException containing a {@link DeadEntityException} if an
   *          EntityProxy cannot be resolved
   */
  public Object resolveDomainValue(Object maybeEntityProxy, boolean detectDeadEntities) {
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
      if (maybeEntityProxy instanceof List<?>) {
        accumulator = new ArrayList<Object>();
      } else if (maybeEntityProxy instanceof Set<?>) {
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
   * Calls {@link Resolution#addPaths(String, Collection)}, enqueuing
   * {@code key} if {@link Resolution#hasWork()} returns {@code true}. This
   * method will also expand paths on the members of Collections.
   */
  private void addPathsToResolution(Resolution resolution, String prefix, Set<String> propertyRefs) {
    if (propertyRefs.isEmpty()) {
      // No work to do
      return;
    }
    if (resolution.getResolutionKey() != null) {
      // Working on a proxied type
      assert resolution.getClientObject() instanceof BaseProxy : "Expecting BaseProxy, found "
          + resolution.getClientObject().getClass().getCanonicalName();
      resolution.addPaths(prefix, propertyRefs);
      if (resolution.hasWork()) {
        toProcess.add(resolution);
      }
      return;
    }
    if (resolution.getClientObject() instanceof Collection) {
      // Pass the paths onto the Resolutions for the contained elements
      Collection<?> collection = (Collection<?>) resolution.getClientObject();
      for (Object obj : collection) {
        Resolution subResolution = clientObjectsToResolutions.get(obj);
        // subResolution will be null for List<Integer>, etc.
        if (subResolution != null) {
          addPathsToResolution(subResolution, prefix, propertyRefs);
        }
      }
      return;
    }
    assert false : "Should not add paths to client type "
        + resolution.getClientObject().getClass().getCanonicalName();
  }

  /**
   * Creates a resolution for a simple value.
   */
  private Resolution makeResolution(Object domainValue) {
    assert !state.isEntityType(domainValue.getClass())
        && !state.isValueType(domainValue.getClass()) : "Not a simple value type";
    return new Resolution(domainValue);
  }

  /**
   * Create or reuse a Resolution for a proxy object.
   */
  private Resolution makeResolution(ResolutionKey key, BaseProxy clientObject) {
    Resolution resolution = resolved.get(key);
    if (resolution == null) {
      resolution = new Resolution(key, clientObject);
      clientObjectsToResolutions.put(clientObject, resolution);
      toProcess.add(resolution);
      resolved.put(key, resolution);
    }
    return resolution;
  }

  /**
   * Creates a proxy instance held by a Resolution for a given domain type.
   */
  private <T extends BaseProxy> Resolution resolveClientProxy(Object domainEntity,
      Class<T> proxyType, ResolutionKey key) {
    if (domainEntity == null) {
      return null;
    }

    SimpleProxyId<? extends BaseProxy> id = state.getStableId(domainEntity);

    boolean isEntityProxy = state.isEntityType(proxyType);
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
          id = state.getIdFactory().allocateSyntheticId(proxyType, ++syntheticId);
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
    bean.setTag(Constants.IN_RESPONSE, true);
    if (domainVersion != null) {
      Splittable flatVersion = state.flatten(domainVersion);
      bean.setTag(Constants.VERSION_PROPERTY_B64, SimpleRequestProcessor.toBase64(flatVersion
          .getPayload()));
    }

    T clientObject = bean.as();
    return makeResolution(key, clientObject);
  }

  /**
   * Creates a Resolution object that holds a client value that represents the
   * given domain value. The resolved client value will be assignable to
   * {@code clientType}.
   */
  private Resolution resolveClientValue(Object domainValue, Type clientType) {
    if (domainValue == null) {
      return new Resolution(null);
    }

    boolean anyType = clientType == null;
    if (anyType) {
      clientType = Object.class;
    }

    Class<?> assignableTo = TypeUtils.ensureBaseType(clientType);
    ResolutionKey key = new ResolutionKey(domainValue, clientType);

    Resolution previous = resolved.get(key);
    if (previous != null && assignableTo.isInstance(previous.getClientObject())) {
      return previous;
    }

    Class<?> returnClass = service.resolveClientType(domainValue.getClass(), assignableTo, true);

    if (anyType) {
      assignableTo = returnClass;
    }

    // Pass simple values through
    if (ValueCodex.canDecode(returnClass)) {
      return makeResolution(domainValue);
    }

    // Convert entities to EntityProxies or EntityProxyIds
    boolean isProxy = BaseProxy.class.isAssignableFrom(returnClass);
    boolean isId = EntityProxyId.class.isAssignableFrom(returnClass);
    if (isProxy || isId) {
      Class<? extends BaseProxy> proxyClass = returnClass.asSubclass(BaseProxy.class);
      return resolveClientProxy(domainValue, proxyClass, key);
    }

    // Convert collections
    if (Collection.class.isAssignableFrom(returnClass)) {
      Collection<Object> accumulator;
      if (List.class.isAssignableFrom(returnClass)) {
        accumulator = new ArrayList<Object>();
      } else if (Set.class.isAssignableFrom(returnClass)) {
        accumulator = new HashSet<Object>();
      } else {
        throw new ReportableException("Unsupported collection type" + returnClass.getName());
      }

      Type elementType = TypeUtils.getSingleParameterization(Collection.class, clientType);
      for (Object o : (Collection<?>) domainValue) {
        accumulator.add(resolveClientValue(o, elementType).getClientObject());
      }
      return makeResolution(accumulator);
    }

    throw new ReportableException("Unsupported domain type " + returnClass.getCanonicalName());
  }
}
