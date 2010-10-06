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
package com.google.gwt.sample.expenses.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.RequestFactory;

/**
 * A place in the app that deals with a specific {@link RequestFactory} proxy.
 */
public class ProxyPlace extends Place {
  /**
   * The things you do with a record, each of which is a different bookmarkable
   * location in the scaffold app.
   */
  public enum Operation {
    CREATE, EDIT, DETAILS
  }

  /**
   * Tokenizer.
   */
  @Prefix("r")
  public static class Tokenizer implements PlaceTokenizer<ProxyPlace> {
    /**
     * 
     */
    private static final String SEPARATOR = "!";
    private final RequestFactory requests;

    public Tokenizer(RequestFactory requests) {
      this.requests = requests;
    }

    public ProxyPlace getPlace(String token) {
      String bits[] = token.split(SEPARATOR);
      Operation operation = Operation.valueOf(bits[1]);
      if (Operation.CREATE == operation) {
        return new ProxyPlace(requests.getProxyClass(bits[0]));
      }
      return new ProxyPlace(requests.getProxyId(bits[0]), operation);
    }

    public String getToken(ProxyPlace place) {
      if (Operation.CREATE == place.getOperation()) {
        return requests.getHistoryToken(place.getProxyClass()) + SEPARATOR
            + place.getOperation();
      }
      return requests.getHistoryToken(place.getProxyId()) + SEPARATOR
          + place.getOperation();
    }
  }

  private final EntityProxyId<?> proxyId;
  private final Class<? extends EntityProxy> proxyClass;
  private final Operation operation;

  public ProxyPlace(Class<? extends EntityProxy> proxyClass) {
    this.operation = Operation.CREATE;
    this.proxyId = null;
    this.proxyClass = proxyClass;
  }

  public ProxyPlace(EntityProxyId<?> record) {
    this(record, Operation.DETAILS);
  }

  public ProxyPlace(EntityProxyId<?> proxyId, Operation operation) {
    this.operation = operation;
    this.proxyId = proxyId;
    this.proxyClass = null;
    assert Operation.CREATE != operation;
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
    ProxyPlace other = (ProxyPlace) obj;
    if (operation != other.operation) {
      return false;
    }
    if (proxyClass == null) {
      if (other.proxyClass != null) {
        return false;
      }
    } else if (!proxyClass.equals(other.proxyClass)) {
      return false;
    }
    if (proxyId == null) {
      if (other.proxyId != null) {
        return false;
      }
    } else if (!proxyId.equals(other.proxyId)) {
      return false;
    }
    return true;
  }

  public Operation getOperation() {
    return operation;
  }

  public Class<? extends EntityProxy> getProxyClass() {
    return proxyId != null ? proxyId.getProxyClass() : proxyClass;
  }

  /**
   * @return the proxyId, or null if the operation is {@link Operation#CREATE}
   */
  public EntityProxyId<?> getProxyId() {
    return proxyId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((operation == null) ? 0 : operation.hashCode());
    result = prime * result
        + ((proxyClass == null) ? 0 : proxyClass.hashCode());
    result = prime * result + ((proxyId == null) ? 0 : proxyId.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return "ProxyPlace [operation=" + operation + ", proxy=" + proxyId
        + ", proxyClass=" + proxyClass + "]";
  }
}
