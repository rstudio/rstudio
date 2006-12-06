/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.sample.dynatable.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Composite;

public class SchoolCalendarWidget extends Composite {

  public class CalendarProvider implements DynaTableDataProvider {

    public CalendarProvider() {
      // Initialize the service.
      //
      calService = (SchoolCalendarServiceAsync) GWT.create(SchoolCalendarService.class);

      // By default, we assume we'll make RPCs to a servlet, but see
      // updateRowData(). There is special support for canned RPC responses.
      // (Which is a totally demo hack, by the way :-)
      // 
      ServiceDefTarget target = (ServiceDefTarget) calService;
      
      // Use a module-relative URLs to ensure that this client code can find 
      // its way home, even when the URL changes (as might happen when you 
      // deploy this as a webapp under an external servlet container). 
      String moduleRelativeURL = GWT.getModuleBaseURL() + "calendar";
      target.setServiceEntryPoint(moduleRelativeURL);
    }

    public void updateRowData(final int startRow, final int maxRows,
        final RowDataAcceptor acceptor) {
      // Check the simple cache first.
      //
      if (startRow == lastStartRow) {
        if (maxRows == lastMaxRows) {
          // Use the cached batch.
          //
          pushResults(acceptor, startRow, lastPeople);
          return;
        }
      }

      // Decide where to get the data.
      // This is very unusual. Normally, you'd set the service entry point
      // only once. But since this demo runs without servlet support, we
      // play a game with URLs to fetch canned RPC responses from static files.
      // So, as you might guess, this is really fragile: changing the number
      // of visible rows in the table will cause different batch sizes, and
      // the URL naming trick below will *definitely* break.
      //
      if (USE_STATIC_RPC_ANSWERS) {
        ServiceDefTarget target = (ServiceDefTarget) calService;
        String staticResponseURL = GWT.getModuleBaseURL();
        staticResponseURL += "calendar" + startRow + ".txt";
        target.setServiceEntryPoint(staticResponseURL);
      }

      // Actually fetch the data remotely.
      //
      calService.getPeople(startRow, maxRows, new AsyncCallback() {
        public void onFailure(Throwable caught) {
          acceptor.failed(caught);
        }

        public void onSuccess(Object result) {
          Person[] people = (Person[]) result;
          lastStartRow = startRow;
          lastMaxRows = maxRows;
          lastPeople = people;
          pushResults(acceptor, startRow, people);
        }

      });

    }

    private void pushResults(RowDataAcceptor acceptor, int startRow,
        Person[] people) {
      String[][] rows = new String[people.length][];
      for (int i = 0, n = rows.length; i < n; i++) {
        Person person = people[i];
        rows[i] = new String[3];
        rows[i][0] = person.getName();
        rows[i][1] = person.getDescription();
        rows[i][2] = person.getSchedule(daysFilter);
      }
      acceptor.accept(startRow, rows);
    }

    private final SchoolCalendarServiceAsync calService;
    private int lastMaxRows = -1;
    private Person[] lastPeople;
    private int lastStartRow = -1;
  }

  private final static boolean USE_STATIC_RPC_ANSWERS = true;

  public SchoolCalendarWidget(int visibleRows) {
    String[] columns = new String[]{"Name", "Description", "Schedule"};
    String[] styles = new String[]{"name", "desc", "sched"};
    dynaTable = new DynaTableWidget(calProvider, columns, styles, visibleRows);
    initWidget(dynaTable);
  }

  protected boolean getDayIncluded(int day) {
    return daysFilter[day];
  }

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
      pendingRefresh = new Command() {
        public void execute() {
          pendingRefresh = null;
          dynaTable.refresh();
        }
      };
      DeferredCommand.add(pendingRefresh);
    }
  }

  private final CalendarProvider calProvider = new CalendarProvider();
  private final boolean[] daysFilter = new boolean[]{
    true, true, true, true, true, true, true};
  private final DynaTableWidget dynaTable;
  private Command pendingRefresh;
}
