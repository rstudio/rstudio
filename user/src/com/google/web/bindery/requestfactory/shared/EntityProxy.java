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
 * A proxy for a server-side domain object.
 */
@ProxyFor(Object.class)
public interface EntityProxy extends BaseProxy {
  /**
   * Returns the {@link EntityProxyId} that identifies a particular instance of
   * the type proxied by the receiver.
   * <p>
   * An id returned by a proxy newly created by {@link RequestContext#create}
   * {@link Object#equals(Object) equals} those returned later by proxies to the
   * persisted object.
   * <p>
   * Subtypes should override to declare they return a stable id of their own
   * type, to allow type safe use of the request objects returned by
   * {@link RequestFactory#find(EntityProxyId)}.
   * 
   * @return an {@link EntityProxyId} instance
   */
  EntityProxyId<?> stableId();
}
