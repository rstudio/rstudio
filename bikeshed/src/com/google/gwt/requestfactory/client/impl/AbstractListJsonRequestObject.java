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
import com.google.gwt.requestfactory.shared.EntityListRequest;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.user.client.ui.TakesValueList;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.impl.RecordJsoImpl;
import com.google.gwt.valuestore.shared.impl.RecordSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract implementation of {@link RequestFactory.RequestObject} for methods
 * returning lists of entities (as opposed to lists of primitives or enums).
 *
 * @param <T> the type of entities returned
 * @param <R> this request type
 */
public abstract class AbstractListJsonRequestObject<T extends Record, R extends AbstractListJsonRequestObject<T, R>>
    implements RequestFactory.RequestObject, EntityListRequest<T> {

  private final RecordSchema<? extends T> schema;
  private final RequestFactoryJsonImpl requestFactory;
  private final Set<Property<?>> properties = new HashSet<Property<?>>();

  private TakesValueList<T> target;

  public AbstractListJsonRequestObject(RecordSchema<? extends T> schema,
      RequestFactoryJsonImpl requestService) {
    this.requestFactory = requestService;
    this.schema = schema;
  }

  public void fire() {
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

  public void handleResponseText(String text) {
    JsArray<RecordJsoImpl> valueJsos = RecordJsoImpl.arrayFromJson(text);
    List<T> valueList = new ArrayList<T>(valueJsos.length());
    for (int i = 0; i < valueJsos.length(); i++) {
      RecordJsoImpl jso = valueJsos.get(i);
      jso.setSchema(schema);
      valueList.add(schema.create(jso));
    }

    requestFactory.getValueStore().setRecords(valueJsos);
    target.setValueList(valueList);
  }

  public R to(TakesValueList<T> target) {
    this.target = target;
    return getThis();
  }

  /**
   * Subclasses must override to return {@code this}, to allow builder-style
   * methods to do the same.
   */
  protected abstract R getThis();
}
