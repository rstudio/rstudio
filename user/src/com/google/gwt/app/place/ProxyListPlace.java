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
package com.google.gwt.app.place;

import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.RequestFactory;

/**
 * A place in the app that deals with lists of {@link RequestFactory}
 * proxy objects.
 */
public class ProxyListPlace extends Place {

  /**
   * Tokenizer.
   */
  @Prefix("l")
  public static class Tokenizer implements PlaceTokenizer<ProxyListPlace> {
    private final RequestFactory requests;

    public Tokenizer(RequestFactory requests) {
      this.requests = requests;
    }

    public ProxyListPlace getPlace(String token) {
      return new ProxyListPlace(requests.getClass(token));
    }

    public String getToken(ProxyListPlace place) {
      return requests.getToken(place.getProxyClass());
    }
  }

  private final Class<? extends EntityProxy> proxyType;

  public ProxyListPlace(Class<? extends EntityProxy> proxyType) {
    this.proxyType = proxyType;
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
    ProxyListPlace other = (ProxyListPlace) obj;
    if (!proxyType.equals(other.proxyType)) {
      return false;
    }
    return true;
  }

  public Class<? extends EntityProxy> getProxyClass() {
    return proxyType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + proxyType.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ProxyListPlace [proxyType=" + proxyType + "]";
  }
}
