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
import com.google.gwt.requestfactory.shared.RequestFactory.WriteOperation;

/**
 * Abstract base class for an event announcing changes to a {@link Record}.
 * 
 * @param <R> the type of the record
 * @param <H> the type of event handler
 */
// TODO Should this provide a collection of changed values rather than fire for
// each one?
public abstract class RecordChangedEvent<R extends Record, H extends EventHandler>
    extends GwtEvent<H> {
  R record;
  WriteOperation writeOperation;

  public RecordChangedEvent(R record, WriteOperation writeOperation) {
    this.record = record;
    this.writeOperation = writeOperation;
  }

  public R getRecord() {
    return record;
  }

  public WriteOperation writeOperation() {
    return writeOperation;
  }
}
