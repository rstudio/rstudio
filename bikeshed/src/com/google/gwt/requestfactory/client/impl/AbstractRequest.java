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

import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.valuestore.shared.Property;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract implementation of {@link RequestFactory.RequestObject}.
 * 
 * @param <T> return type
 * @param <R> type of this request object
 */
public abstract class AbstractRequest<T, R extends AbstractRequest<T, R>>
    implements RequestFactory.RequestObject<T> {

  protected final RequestFactoryJsonImpl requestFactory;
  protected Receiver<T> receiver;

  private final Set<Property<?>> properties = new HashSet<Property<?>>();

  public AbstractRequest(RequestFactoryJsonImpl requestFactory) {
    this.requestFactory = requestFactory;
  }

  public void fire() {
    assert null != receiver : "to(Receiver) was not called";
    requestFactory.fire(this);
  }

  public R forProperties(Collection<Property<?>> properties) {
    this.properties.addAll(properties);
    return getThis();
  }

  public R forProperty(Property<?> property) {
    this.properties.add(property);
    return getThis();
  }

  /**
   * @return the properties
   */
  public Set<Property<?>> getProperties() {
    return Collections.unmodifiableSet(properties);
  }

  public R to(Receiver<T> target) {
    this.receiver = target;
    return getThis();
  }

  /**
   * Subclasses must override to return {@code this}, to allow builder-style
   * methods to do the same.
   */
  protected abstract R getThis();

}