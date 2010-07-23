/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.sample.dynatablerf.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * The entry point class which performs the initial loading of the DynaTableRf
 * application.
 */
public class DynaTableRf implements EntryPoint {

  public void onModuleLoad() {
    // Find the slot for the calendar widget.
    //
    RootPanel slot = RootPanel.get("calendar");
    if (slot != null) {
      HandlerManager eventBus = new HandlerManager(null);
      DynaTableRequestFactory requests = GWT.create(DynaTableRequestFactory.class);
      requests.init(eventBus);
      CalendarProvider calProvider = new CalendarProvider(requests);
      SchoolCalendarWidget calendar = new SchoolCalendarWidget(calProvider, 15);
      slot.add(calendar);

      // Find the slot for the days filter widget.
      //
      slot = RootPanel.get("days");
      if (slot != null) {
        DayFilterWidget filter = new DayFilterWidget(calendar);
        slot.add(filter);
      }
    }
  }
}
