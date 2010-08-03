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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The entry point class which performs the initial loading of the DynaTableRf
 * application.
 */
public class DynaTableRf implements EntryPoint {

  interface Binder extends UiBinder<Widget, DynaTableRf> {
  }

  @UiField(provided = true)
  SchoolCalendarWidget calendar;

  HandlerManager eventBus = new HandlerManager(null);

  @UiField(provided = true)
  DayFilterWidget filter;

  public void onModuleLoad() {

    DynaTableRequestFactory requests = GWT.create(DynaTableRequestFactory.class);
    requests.init(eventBus);

    CalendarProvider provider = new CalendarProvider(requests);

    calendar = new SchoolCalendarWidget(provider, 15);
    filter = new DayFilterWidget(eventBus);

    RootLayoutPanel.get().add(
        GWT.<Binder> create(Binder.class).createAndBindUi(this));
  }
}
