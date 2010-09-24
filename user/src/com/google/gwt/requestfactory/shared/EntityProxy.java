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
package com.google.gwt.requestfactory.shared;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * A proxy for a server-side domain object.
 */
public interface EntityProxy {
  /**
   * Returns the {@link EntityProxyId} that identifies a particular instance of
   * the type proxied by the receiver.
   * <p>
   * An id returned by a proxy newly created by {@link RequestFactory#create}
   * {@link #equals(Object)} those returned later by a proxies to the persisted
   * object.
   * <p>
   * Subtypes should override to declare they return a stable id of their own
   * type, to allow type safe use of the request objects returned by
   * {@link RequestFactory#find(EntityProxyId)}.
   */
  EntityProxyId<?> stableId();
}
