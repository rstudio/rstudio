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
package com.google.gwt.requestfactory.shared.impl;

import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.messages.IdUtil;

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
  public static final int NEVER_EPHEMERAL = -1;

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
   * Construct an ephemeral id. May be called only from
   * {@link IdFactory#getId()}.
   */
  SimpleEntityProxyId(Class<P> proxyClass, int clientId) {
    assert proxyClass != null;
    this.clientId = clientId;
    this.proxyClass = proxyClass;
    hashCode = clientId;
  }

  /**
   * Construct a stable id. May only be called from {@link IdFactory#getId()}
   */
  SimpleEntityProxyId(Class<P> proxyClass, String serverId) {
    assert proxyClass != null;
    assert serverId != null && !serverId.contains("@")
        && !"null".equals(serverId);
    setServerId(serverId);
    clientId = NEVER_EPHEMERAL;
    hashCode = serverId.hashCode();
    this.proxyClass = proxyClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleEntityProxyId<?>)) {
      return false;
    }
    SimpleEntityProxyId<?> other = (SimpleEntityProxyId<?>) o;
    if (!proxyClass.equals(other.proxyClass)) {
      return false;
    }

    if (clientId != NEVER_EPHEMERAL && clientId == other.clientId) {
      /*
       * Unexpected: It should be the case that locally-created ids are never
       * aliased and will be caught by the first if statement.
       */
      return true;
    }

    if (serverId != null && serverId.equals(other.serverId)) {
      return true;
    }
    return false;
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

  @Override
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
    assert !"null".equals(serverId);
    this.serverId = serverId;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    if (isEphemeral()) {
      return IdUtil.ephemeralId(clientId, proxyClass.getName());
    } else {
      return IdUtil.persistedId(serverId, proxyClass.getName());
    }
  }

  /**
   * Returns <code>true</code> if the id was created as an ephemeral id.
   */
  public boolean wasEphemeral() {
    return clientId != NEVER_EPHEMERAL;
  }
}
