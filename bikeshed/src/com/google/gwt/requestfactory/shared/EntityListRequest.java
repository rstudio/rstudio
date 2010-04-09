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

import com.google.gwt.user.client.ui.TakesValueList;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;
import com.google.gwt.valuestore.shared.ValuesKey;

import java.util.Collection;

/**
 * Implemented by RequestObjects for service methods that return lists of
 * entities.
 * <p>
 * TODO Really should be for lists of anything.
 *
 * @param <K> The type held by the returned list
 */
public interface EntityListRequest<K extends ValuesKey<K>> extends
    RequestFactory.RequestObject {

  EntityListRequest<K> forProperties(Collection<Property<K, ?>> properties);

  EntityListRequest<K> forProperty(Property<K, ?> property);

  EntityListRequest<K> to(TakesValueList<Values<K>> target);
}
