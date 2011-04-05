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
package com.google.web.bindery.requestfactory.shared;

/**
 * Serializes graphs of EntityProxy objects. A ProxySerializer is associated
 * with an instance of a {@link ProxyStore} when it is created via
 * {@link RequestFactory#getSerializer(ProxyStore)}.
 * <p>
 * The {@link EntityProxy#stableId()} of non-persisted (i.e. newly
 * {@link RequestContext#create(Class) created}) {@link EntityProxy} instances
 * are not stable.
 * <p>
 * To create a self-contained message that encapsulates a proxy:
 * 
 * <pre>
 * RequestFactory myFactory = ...;
 * MyFooProxy someProxy = ...;
 * 
 * DefaultProxyStore store = new DefaultProxyStore();
 * ProxySerializer ser = myFactory.getSerializer(store);
 * // More than one proxy could be serialized
 * String key = ser.serialize(someProxy);
 * // Create the flattened representation
 * String payload = store.encode();
 * </pre>
 * 
 * To recreate the object:
 * 
 * <pre>
 * ProxyStore store = new DefaultProxyStore(payload);
 * ProxySerializer ser = myFactory.getSerializer(store);
 * MyFooProxy someProxy = ser.deserialize(MyFooProxy.class, key);
 * </pre>
 * 
 * If two objects refer to different EntityProxy instances that have the same
 * stableId(), the last mutable proxy encountered will be preferred, otherwise
 * the first immutable proxy will be used.
 * 
 * @see DefaultProxyStore
 */
public interface ProxySerializer {
  /**
   * Recreate a proxy instance that was previously passed to
   * {@link #serialize(BaseProxy)}.
   * 
   * @param <T> the type of proxy object to create
   * @param proxyType the type of proxy object to create
   * @param key a value previously returned from {@link #serialize(BaseProxy)}
   * @return a new, immutable instance of the proxy or {@code null} if the data
   *         needed to deserialize the proxy is not present in the ProxyStore
   */
  <T extends BaseProxy> T deserialize(Class<T> proxyType, String key);

  /**
   * Recreate a {@link EntityProxy} instance that was previously passed to
   * {@link #serialize(BaseProxy)}.
   * 
   * @param <T> the type of proxy object to create
   * @param id the {@link EntityProxyId} of the desired entity
   * @return a new, immutable instance of the proxy or {@code null} if the data
   *         needed to deserialize the proxy is not present in the ProxyStore
   */
  <T extends EntityProxy> T deserialize(EntityProxyId<T> id);

  /**
   * Store a proxy into the backing store.
   * 
   * @param proxy the proxy to store
   * @return a key value that can be passed to
   *         {@link #deserialize(Class, String)}
   */
  String serialize(BaseProxy proxy);
}
