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
 * Abstract implementation of {@link AbstractJsonRequestObject} for special find
 * methods that return single instances of {@link EntityProxy}.
 */
public abstract class FindRequestObjectImpl extends
    AbstractJsonObjectRequest<EntityProxy, FindRequestObjectImpl> {

  public FindRequestObjectImpl(RequestFactoryJsonImpl factory, EntityProxyId proxyId) {
    super(((EntityProxyIdImpl) proxyId).schema, factory);
  }

  // This declaration works around a javac generics bug
  @Override
  public FindRequestObjectImpl with(String... propertyRef) {
    return super.with(propertyRef);
  }

  @Override
  protected FindRequestObjectImpl getThis() {
    return this;
  }
}
