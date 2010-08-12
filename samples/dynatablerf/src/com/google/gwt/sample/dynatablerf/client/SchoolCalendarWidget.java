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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Composite;

/**
 * A Composite widget that abstracts a DynaTableWidget and a data provider.
 */
public class SchoolCalendarWidget extends Composite {

  private final boolean[] daysFilter = new boolean[] {
      true, true, true, true, true, true, true};

  private final DynaTableWidget dynaTable;

  private ScheduledCommand pendingRefresh;

  public SchoolCalendarWidget(DynaTableWidget dynaTable) {
    this.dynaTable = dynaTable;
    initWidget(dynaTable);
  }

  protected boolean getDayIncluded(int day) {
    return daysFilter[day];
  }

  @Override
  protected void onLoad() {
    dynaTable.refresh();
  }

  protected void setDayIncluded(int day, boolean included) {
    if (daysFilter[day] == included) {
      // No change.
      //
      return;
    }

    daysFilter[day] = included;
    if (pendingRefresh == null) {
      pendingRefresh = new ScheduledCommand() {
        public void execute() {
          pendingRefresh = null;
          dynaTable.refresh();
        }
      };
      Scheduler.get().scheduleDeferred(pendingRefresh);
    }
  }
}
