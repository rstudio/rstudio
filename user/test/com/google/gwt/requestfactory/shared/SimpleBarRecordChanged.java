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

import com.google.gwt.event.shared.EventHandler;

/**
 * Test implementation of {@link com.google.gwt.requestfactory.shared.RecordChangedEvent} for
 * {@link com.google.gwt.requestfactory.shared.SimpleFooRecord}.
 */
public class SimpleBarRecordChanged extends
    RecordChangedEvent<SimpleBarRecord, SimpleBarRecordChanged.Handler> {

/**
 *  Test Handler for SimpleFooChanged event.
 */
  public interface Handler extends EventHandler {
    void onSimpleBarRecordChanged(SimpleBarRecordChanged record);
  }

  public static final Type<Handler> TYPE = new Type<Handler>();

  public SimpleBarRecordChanged(SimpleBarRecord record,
      WriteOperation writeOperation) {
    super(record, writeOperation);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onSimpleBarRecordChanged(this);
  }
}
