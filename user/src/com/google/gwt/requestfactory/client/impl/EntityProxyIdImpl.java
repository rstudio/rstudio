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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * The key used to store
 * {@link com.google.gwt.requestfactory.shared.EntityProxy Proxy}. This is
 * stable across creates and updates on the client.
 * <p>
 * At all times, the id is not null. If the isFuture flag is true, the
 * corresponding proxy has not been persisted and the id field is a futureId. If
 * the isFuture flag is false, the corresponding proxy has been persisted and
 * the id is the data-store id. The futureId is non-null if the entity was
 * created on this client.
 */
final class EntityProxyIdImpl<P extends EntityProxy> implements EntityProxyId<P> {

  private static int hashCode(ProxySchema<?> proxySchema, boolean hasFutureId, Object finalId) {
    final int prime = 31;
    int result = hasFutureId ? 0 : 1;
    result = prime * result + finalId.hashCode();
    result = prime * result + proxySchema.hashCode();
    return result;
  }

  final ProxySchema<?> schema;
  // TODO(rjrjr) These should be strings, but the future map implications are a bit daunting
  final Object encodedId;
  final Object futureId;

  final boolean isFuture;

  protected EntityProxyIdImpl(Object encodedId, ProxySchema<?> schema,
      boolean isFuture, Object futureId) {
    assert encodedId != null;
    assert schema != null;
    if (isFuture) {
      assert futureId == null;
    }

    this.encodedId = encodedId;
    this.schema = schema;
    this.isFuture = isFuture;
    this.futureId = futureId;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    @SuppressWarnings("unchecked")
    EntityProxyIdImpl<P> other = (EntityProxyIdImpl<P>) obj;
    if (!schema.equals(other.schema)) {
      return false;
    }
    if (isFuture == other.isFuture && encodedId.equals(other.encodedId)) {
      return true;
    }
    // one of the isFuture is false. check its futureId
    if (!isFuture && other.encodedId.equals(futureId)) {
      return true;
    }
    if (!other.isFuture && encodedId.equals(other.futureId)) {
      return true;
    }
    return false;
  }

  /*
   * This hashcode is complicated.
   */
  @Override
  public int hashCode() {
    if (futureId == null && !isFuture) {
      // does not have a futureId.
      return hashCode(schema, false, encodedId); 
    }
    // has futureId
    return hashCode(schema, true, isFuture ? encodedId : futureId);
  }

  @Override
  public String toString() {
    return "[EntityProxyId schema: " + schema.getClass().getName() + " id: "
        + encodedId + " isFuture: " + (isFuture ? "true" : "false")
        + " futureId: " + futureId + "]";
  }
}
