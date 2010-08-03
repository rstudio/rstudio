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
package com.google.gwt.sample.dynatablerf.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Indicates that new data is available to the application.
 */
public class DataAvailableEvent extends GwtEvent<DataAvailableEvent.Handler> {
  /**
   * Handles {@link DataAvailableEvent}.
   */
  public interface Handler extends EventHandler {
    void onRowData(DataAvailableEvent event);
  }

  public static final Type<Handler> TYPE = new Type<Handler>();
  private final List<PersonProxy> people;
  private final int startRow;

  public DataAvailableEvent(int startRow, List<PersonProxy> people) {
    this.startRow = startRow;
    this.people = Collections.unmodifiableList(new ArrayList<PersonProxy>(
        people));
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public List<PersonProxy> getPeople() {
    return people;
  }

  public int getStartRow() {
    return startRow;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRowData(this);
  }
}
