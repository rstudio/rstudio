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

import com.google.gwt.sample.dynatablerf.domain.Schedule;
import com.google.web.bindery.requestfactory.shared.Locator;

/**
 * This class serves as an example of implementing a Locator to allow
 * RequestFactory to work with entities that don't conform to its expectations of
 * static find*() methods, and getId() and getVersion() methods. In a production
 * application such a Locator might be the bridge to your dependency injection
 * framework, or a data access object.
 * <p>
 * There is a reference to this class in a {@literal @}Service annotation in
 * {@link com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory}
 */
public class ScheduleLocator extends Locator<Schedule, String> {
  
  public static ScheduleSource getThreadLocalObject() {
    return SchoolCalendarService.SCHEDULE_SOURCE.get();
  }

  @Override
  public Schedule create(Class<? extends Schedule> clazz) {
    return getThreadLocalObject().create(clazz);
  }

  @Override
  public Schedule find(Class<? extends Schedule> clazz, String id) {
    return getThreadLocalObject().find(clazz, id);
  }

  @Override
  public Class<Schedule> getDomainType() {
    return getThreadLocalObject().getDomainType();
  }

  @Override
  public String getId(Schedule domainObject) {
    return getThreadLocalObject().getId(domainObject);
  }

  @Override
  public Class<String> getIdType() {
    return getThreadLocalObject().getIdType();
  }

  @Override
  public Object getVersion(Schedule domainObject) {
    return getThreadLocalObject().getVersion(domainObject);
  }

  public void persist(Schedule domainObject) {
    getThreadLocalObject().persist(domainObject);
  }
}
