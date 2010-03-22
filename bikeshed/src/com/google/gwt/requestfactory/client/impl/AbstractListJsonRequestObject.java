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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.requestfactory.shared.EntityKey;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestFactory.Service;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.valuestore.client.ValuesImpl;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.ValueStore;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract implementation of {@link RequestFactory.RequestObject} for methods
 * returning lists of entities.
 * 
 * @param <T> the type of entities returned
 * @param <R> this request type
 */
public abstract class AbstractListJsonRequestObject<T extends EntityKey<T>, R extends AbstractListJsonRequestObject<T, R>>
    implements RequestFactory.RequestObject {

  private final T key;
  private final Service requestService;
  @SuppressWarnings("unused")
  // That's next
  private final ValueStore valueStore;

  private final Set<Property<T, ?>> properties = new HashSet<Property<T, ?>>();

  private HasValueList<Values<T>> watcher;

  public AbstractListJsonRequestObject(T key, ValueStore valueStore,
      RequestFactory.Service requestService) {
    this.requestService = requestService;
    this.valueStore = valueStore;
    this.key = key;
  }

  public void fire() {
    requestService.fire(this);
  }

  public R forProperties(Collection<Property<T, ?>> properties) {
    for (Property<T, ?> property : properties) {
      forProperty(property);
    }
    return getThis();
  }

  public R forProperty(Property<T, ?> property) {
    properties.add(property);
    return getThis();
  }

  /**
   * @return the properties
   */
  public Set<Property<T, ?>> getProperties() {
    return properties;
  }

  public void handleResponseText(String text) {
    // DeltaValueStore deltaStore = valueStore.edit();
    JsArray<ValuesImpl<T>> valueArray = ValuesImpl.arrayFromJson(text);
    List<Values<T>> valueList = new ArrayList<Values<T>>(valueArray.length());
    for (int i = 0; i < valueArray.length(); i++) {
      ValuesImpl<T> values = valueArray.get(i);
      values.setPropertyHolder(key);
      // deltaStore.setValue(propertyHolder, properties, values);
      valueList.add(values);
    }

    // valueStore.subscribe(watcher, valueList, properties);
    // deltaStore.commit();
    watcher.setValueList(valueList);
  }

  public R to(HasValueList<Values<T>> watcher) {
    this.watcher = watcher;
    return getThis();
  }

  /**
   * Subclasses must override to return {@code this}, to allow builder-style
   * methods to do the same.
   */
  protected abstract R getThis();
}
