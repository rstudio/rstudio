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
package com.google.gwt.sample.dynatablerf.client;

import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;

import java.util.List;

/**
   * A data provider that bridges the provides row level updates from the data
   * available through a <@link SchoolCalendarService>.
   */
  public class CalendarProvider implements DynaTableDataProvider {

    private int lastMaxRows = -1;

    private PersonProxy[] lastPeople;

    private int lastStartRow = -1;

    private final DynaTableRequestFactory requests;

    public CalendarProvider(DynaTableRequestFactory requests) {
      // Initialize the service.
      //
      this.requests =  requests;
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

      // Fetch the data remotely.
      //

      requests.schoolCalendarRequest().getPeople(startRow, maxRows).to(new Receiver<List<PersonProxy>>() {
        
        // TODO onError call RowDataAcceptor#fail, not yet provided by RF
        
        public void onSuccess(List<PersonProxy> response) {
          lastStartRow = startRow;
          lastMaxRows = maxRows;
          lastPeople = response.toArray(new PersonProxy[response.size()]);
          PersonProxy[] result = response.toArray(new PersonProxy[response.size()]);
          pushResults(acceptor, startRow, result);
        }
      }).fire();
    }
    
    private void pushResults(RowDataAcceptor acceptor, int startRow,
        PersonProxy[] people) {
      String[][] rows = new String[people.length][];
      for (int i = 0, n = rows.length; i < n; i++) {
        PersonProxy person = people[i];
        rows[i] = new String[3];
        rows[i][0] = person.getName();
        rows[i][1] = person.getDescription();
        rows[i][2] = person.getSchedule();
        // TODO bring back filtering
//        rows[i][2] = person.getSchedule(daysFilter);
      }
      acceptor.accept(startRow, rows);
    }
  }