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

import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;

import java.util.Collection;

/**
 * Implemented by RequestObjects for service methods that return list
 * properties.
 * 
 * @param <E> The type held by the returned list
 */
public interface EntityListRequest<E> extends RequestFactory.RequestObject {
  EntityListRequest<E> forProperties(Collection<Property<E, ?>> properties);
  
  EntityListRequest<E> forProperty(Property<E, ?> property);

  EntityListRequest<E> to(HasValueList<Values<E>> watcher);
}
