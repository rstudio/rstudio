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
 * Nothing fancy.
 * 
 * @param <P> the type of EntityProxy object the id describes
 */
public class SimpleEntityProxyId<P extends EntityProxy> implements
    EntityProxyId<P> {
  /**
   * A placeholder value for {@link #clientId} to indicate the id was not
   * created locally.
   */
  private static final int NEVER_EPHEMERAL = -1;

  /**
   * The client-side id is ephemeral, and is valid only during the lifetime of a
   * module. Any use of the client-side id except to send to the server as a
   * bookkeeping exercise is wrong.
   */
  private final int clientId;

  /**
   * The hashcode of the id must remain stable, even if the server id is later
   * assigned.
   */
  private final int hashCode;

  private final Class<P> proxyClass;

  /**
   * The serverId is totally opaque to the client. It's probably a
   * base64-encoded string, but it could be digits of pi. Any code that does
   * anything other than send the contents of this field back to the server is
   * wrong.
   */
  private String serverId;

  /**
   * Construct a stable id. May only be called from
   * {@link AbstractRequestFactory#getId}.
   */
  SimpleEntityProxyId(Class<P> proxyClass, String serverId) {
    setServerId(serverId);
    clientId = NEVER_EPHEMERAL;
    hashCode = serverId.hashCode();
    this.proxyClass = proxyClass;
  }

  /**
   * Construct an ephemeral id. May be called only from
   * {@link AbstractRequestFactory#allocateId}.
   */
  SimpleEntityProxyId(Class<P> proxyClass, int clientId) {
    this.clientId = clientId;
    this.proxyClass = proxyClass;
    hashCode = clientId;
  }

  public boolean equals(Object o) {
    if (!(o instanceof SimpleEntityProxyId<?>)) {
      return false;
    }
    SimpleEntityProxyId<?> other = (SimpleEntityProxyId<?>) o;
    if (!proxyClass.equals(other.proxyClass)) {
      return false;
    }
    return (clientId != NEVER_EPHEMERAL && clientId == other.clientId)
        || serverId.equals(other.serverId);
  }

  public int getClientId() {
    return clientId;
  }

  public Class<P> getProxyClass() {
    return proxyClass;
  }

  public String getServerId() {
    return serverId;
  }

  public int hashCode() {
    return hashCode;
  }

  public boolean isEphemeral() {
    return serverId == null;
  }

  /**
   * Allows the server id token to be set. This method may be called exactly
   * once over the lifetime of an id.
   */
  public void setServerId(String serverId) {
    if (this.serverId != null) {
      throw new IllegalStateException();
    }
    this.serverId = serverId;
  }
}
