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
package com.google.gwt.valuestore.shared;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Abstract base class for an event announcing changes to a {@link Values}
 * record with a particular {@link ValuesKey}.
 *
 * @param <K> the type of the key
 * @param <H> the type of event handler
 */
// TODO This name is too close to ValueChangeEvent, very confusing
// TODO Should this provide a collection of changed values rather than fire for each one?
public abstract class ValuesChangedEvent<K extends ValuesKey<K>, H extends EventHandler>
    extends GwtEvent<H> {
  Values<K> values;

  public ValuesChangedEvent(Values<K> values) {
    this.values = values;
  }

  public Values<K> getValues() {
    return values;
  }
}
