/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.sample.dynatablerf.server;

import com.google.gwt.sample.dynatablerf.domain.TimeSlot;

/**
 * Service object for Schedule entities, used to demonstrate the use of non-static
 * service objects with RequestFactory. RequestFactory finds this service via the
 * {@link ScheduleServiceLocator}.
 */
public class ScheduleService {

  public TimeSlot createTimeSlot(int zeroBasedDayOfWeek, int startMinutes, int endMinutes) {
    return new TimeSlot(zeroBasedDayOfWeek, startMinutes, endMinutes);
  }
  
}
