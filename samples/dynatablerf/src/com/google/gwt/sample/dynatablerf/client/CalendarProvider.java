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

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.sample.dynatablerf.client.events.DataAvailableEvent;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.valuestore.shared.SyncResult;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A data provider that bridges the provides row level updates from the data
 * available through a <@link SchoolCalendarService>.
 */
public class CalendarProvider {
  private static final Logger log = Logger.getLogger(CalendarProvider.class.getName());

  private final HandlerManager eventBus = new HandlerManager(this);

  private int lastMaxRows = -1;

  private List<PersonProxy> lastPeople;

  private int lastStartRow = -1;

  private final DynaTableRequestFactory requests;

  public CalendarProvider(DynaTableRequestFactory requests) {
    this.requests = requests;
  }

  public HandlerRegistration addRowDataHandler(
      DataAvailableEvent.Handler handler) {
    return eventBus.addHandler(DataAvailableEvent.TYPE, handler);
  }

  public void updateRowData(final int startRow, final int maxRows) {
    // Check the simple cache first.
    if (startRow == lastStartRow) {
      if (maxRows == lastMaxRows) {
        // Use the cached batch.
        pushResults(startRow, lastPeople);
        return;
      }
    }

    requests.schoolCalendarRequest().getPeople(startRow, maxRows).fire(
        new Receiver<List<PersonProxy>>() {
          // TODO onError call RowDataAcceptor#fail, not yet provided by RF
          public void onSuccess(List<PersonProxy> response,
              Set<SyncResult> syncResults) {
            lastStartRow = startRow;
            lastMaxRows = maxRows;
            lastPeople = response;
            pushResults(startRow, response);

            if (response.size() > 0) {
              demoPersist(response.get(0));
            }
          }

        });
  }

  private void demoPersist(PersonProxy someone) {
    /*
     * Create a request to call someone's persist method.
     */
    RequestObject<Void> request = requests.personRequest().persist(someone);

    someone = request.edit(someone);
    someone.setName("Ray Ryan");
    someone.setDescription("Was here");

    request.fire(new Receiver<Void>() {
      public void onSuccess(Void isNull, /* syncResults going away very soon */
      Set<SyncResult> syncResults) {
        /*
         * A PersonProxyChanged should have fired. By M4, subtypes like
         * PersonProxyChanged should go away and be replaced by a more general
         * ProxyUpdateEvent
         */
        log.info("The persist call worked, did you see an update event?");
      }

      /*
       * Coming soon
       * 
       * void onViolation(Set<ConstraintViolation> violations); void onError(
       * ... tbd ... );
       * 
       * But likely this will come first, not sure who is dealing with
       * serializing ConstraintViolation. Sorry.
       * 
       * void onViolation(Set<SyncResult> syncResults) { ... }
       */

    });
  }

  private void pushResults(int startRow, List<PersonProxy> people) {
    eventBus.fireEvent(new DataAvailableEvent(startRow, people));
  }
}
