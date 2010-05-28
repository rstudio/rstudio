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
import com.google.gwt.requestfactory.shared.RecordListRequest;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.impl.RecordJsoImpl;
import com.google.gwt.valuestore.shared.impl.RecordSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementation of
 * {@link com.google.gwt.requestfactory.shared.RequestFactory.RequestObject
 * RequestFactory.RequestObject} for requests that return lists of
 * {@link Record}.
 * 
 * @param <T> the type of entities returned
 * @param <R> this request type
 */
public abstract class //
AbstractJsonListRequest<T extends Record, R extends AbstractJsonListRequest<T, R>> //
    extends AbstractRequest<List<T>, R> implements RecordListRequest<T> {
  protected final RecordSchema<? extends T> schema;

  public AbstractJsonListRequest(RecordSchema<? extends T> schema,
      RequestFactoryJsonImpl requestService) {
    super(requestService);
    this.schema = schema;
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
    receiver.onSuccess(valueList);
  }
}
