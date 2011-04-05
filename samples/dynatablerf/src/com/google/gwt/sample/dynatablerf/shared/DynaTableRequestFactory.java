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
package com.google.gwt.sample.dynatablerf.shared;

import com.google.gwt.sample.dynatablerf.domain.Person;
import com.google.gwt.sample.dynatablerf.server.ScheduleService;
import com.google.gwt.sample.dynatablerf.server.ScheduleServiceLocator;
import com.google.gwt.sample.dynatablerf.server.SchoolCalendarService;
import com.google.web.bindery.requestfactory.shared.InstanceRequest;
import com.google.web.bindery.requestfactory.shared.LoggingRequest;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Request factory for the DynaTable sample. Instantiate via
 * {@link com.google.gwt.core.client.GWT#create}.
 */
public interface DynaTableRequestFactory extends RequestFactory {
  /**
   * Source of request objects for the Person class.
   */
  @Service(Person.class)
  interface PersonRequest extends RequestContext {
    InstanceRequest<PersonProxy, Void> persist();
  }

  /**
   * Source of request objects for the SchoolCalendarService.
   */
  @Service(SchoolCalendarService.class)
  interface SchoolCalendarRequest extends RequestContext {
    List<Boolean> ALL_DAYS = Collections.unmodifiableList(Arrays.asList(true,
        true, true, true, true, true, true));
    List<Boolean> NO_DAYS = Collections.unmodifiableList(Arrays.asList(false,
        false, false, false, false, false, false));

    Request<List<PersonProxy>> getPeople(int startIndex, int maxCount,
        List<Boolean> dayFilter);

    Request<PersonProxy> getRandomPerson();
  }

  /**
   * Source of request objects for Schedule entities.
   */
  @Service(value = ScheduleService.class, locator = ScheduleServiceLocator.class)
  interface ScheduleRequest extends RequestContext {
    Request<TimeSlotProxy> createTimeSlot(int zeroBasedDayOfWeek, int startMinutes,
        int endMinutes);
  }

  LoggingRequest loggingRequest();

  PersonRequest personRequest();
  
  ScheduleRequest scheduleRequest();

  SchoolCalendarRequest schoolCalendarRequest();
}
