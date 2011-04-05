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
package com.google.web.bindery.requestfactory.shared.impl;

import com.google.web.bindery.requestfactory.shared.BaseProxy;

/**
 * The base implementation of id objects in the RequestFactory system. This type
 * exists to allow ValueProxies to be implemented in the same manner as an
 * EntityProxy as far as metadata maintenance is concerned. There is a specific
 * subtype {@link SimpleEntityProxyId} which implements the requisite public
 * interface for EntityProxy types.
 * 
 * @param <P> the type of BaseProxy object the id describes
 */
public class SimpleProxyId<P extends BaseProxy> {
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
   * The encodedAddress is totally opaque to the client. It's probably a
   * base64-encoded string, but it could be digits of pi. Any code that does
   * anything other than send the contents of this field back to the server is
   * wrong.
   */
  private String encodedAddress;

  /**
   * The hashcode of the id must remain stable, even if the server id is later
   * assigned.
   */
  private final int hashCode;

  /**
   * The EntityProxy type.
   */
  private final Class<P> proxyClass;

  /**
   * A flag to indicate that the id is synthetic, that it is not valid beyond
   * the duration of the request.
   */
  private int syntheticId;

  /**
   * Construct an ephemeral id. May be called only from
   * {@link IdFactory#createId()}.
   */
  SimpleProxyId(Class<P> proxyClass, int clientId) {
    assert proxyClass != null;
    this.clientId = clientId;
    this.proxyClass = proxyClass;
    hashCode = clientId;
  }

  /**
   * Construct a stable id. May only be called from {@link IdFactory#createId()}
   */
  SimpleProxyId(Class<P> proxyClass, String encodedAddress) {
    assert proxyClass != null;
    assert encodedAddress != null;
    setServerId(encodedAddress);
    clientId = NEVER_EPHEMERAL;
    hashCode = encodedAddress.hashCode();
    this.proxyClass = proxyClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleProxyId<?>)) {
      return false;
    }
    SimpleProxyId<?> other = (SimpleProxyId<?>) o;
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

    if (encodedAddress != null && encodedAddress.equals(other.encodedAddress)) {
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

  /**
   * TODO: Rename to getAddress().
   */
  public String getServerId() {
    return encodedAddress;
  }

  /**
   * A flag to indicate that the id is synthetic, that it is not valid beyond
   * the duration of the request.
   */
  public int getSyntheticId() {
    return syntheticId;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public boolean isEphemeral() {
    return encodedAddress == null;
  }

  public boolean isSynthetic() {
    return syntheticId > 0;
  }

  /**
   * Allows the server address token to be set. This method may be called
   * exactly once over the lifetime of an id.
   */
  public void setServerId(String encodedAddress) {
    if (this.encodedAddress != null) {
      throw new IllegalStateException();
    }
    assert !"null".equals(encodedAddress);
    this.encodedAddress = encodedAddress;
  }

  public void setSyntheticId(int syntheticId) {
    this.syntheticId = syntheticId;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    if (isEphemeral()) {
      return IdUtil.ephemeralId(clientId, proxyClass.getName());
    } else if (isSynthetic()) {
      return IdUtil.syntheticId(syntheticId, proxyClass.getName());
    } else {
      return IdUtil.persistedId(encodedAddress, proxyClass.getName());
    }
  }

  /**
   * Returns <code>true</code> if the id was created as an ephemeral id.
   */
  public boolean wasEphemeral() {
    return clientId != NEVER_EPHEMERAL;
  }
}
