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
package com.google.gwt.requestfactory.shared;

import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Implemented by RequestObjects for service methods that return lists of
 * records.
 * 
 * @param <R> The type held by the returned list
 */
public interface RecordListRequest<R extends Record> extends
    RequestFactory.RequestObject<List<R>> {

  RecordListRequest<R> forProperties(Collection<Property<?>> properties);

  RecordListRequest<R> forProperty(Property<?> property);
  
  RecordListRequest<R> to(Receiver<List<R>> target);
}
