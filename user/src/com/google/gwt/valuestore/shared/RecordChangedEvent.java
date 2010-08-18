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
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract base class for an event announcing changes to a {@link Record}.
 * <p>
 * Note that this event includes an unpopulated copy of the changed record
 * &mdash; all properties are undefined except it's id. That is, the event
 * includes only enough information for receivers to issue requests to get
 * themselves fresh copies of the record.
 * <p>
 * TODO: rather than an empty record, consider using a string token 
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

  /**
   * @return an unpopulated copy of the changed record &mdash; all properties
   *         are undefined except it's id
   */
  public R getRecord() {
    return record;
  }

  public WriteOperation getWriteOperation() {
    return writeOperation;
  }
}
