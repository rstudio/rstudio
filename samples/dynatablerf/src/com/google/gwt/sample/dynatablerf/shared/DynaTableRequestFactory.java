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

import com.google.gwt.requestfactory.shared.Instance;
import com.google.gwt.requestfactory.shared.LoggingRequest;
import com.google.gwt.requestfactory.shared.ProxyListRequest;
import com.google.gwt.requestfactory.shared.ProxyRequest;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.Service;
import com.google.gwt.sample.dynatablerf.domain.Address;
import com.google.gwt.sample.dynatablerf.domain.Person;
import com.google.gwt.sample.dynatablerf.server.SchoolCalendarService;

/**
 * Request factory for the DynaTable sample. Instantiate via
 * {@link com.google.gwt.core.client.GWT#create}.
 */
public interface DynaTableRequestFactory extends RequestFactory {

  /**
   * Source of request objects for the Address class.
   */
  @Service(Address.class)
  interface AddressRequest {
    ProxyRequest<AddressProxy> findAddress(String id);

    @Instance
    Request<Void> persist(AddressProxy person);
  }

  /**
   * Source of request objects for the Person class.
   */
  @Service(Person.class)
  interface PersonRequest {
    ProxyRequest<PersonProxy> findPerson(String id);

    @Instance
    Request<Void> persist(PersonProxy person);
  }

  /**
   * Source of request objects for the SchoolCalendarService.
   */
  @Service(SchoolCalendarService.class)
  interface SchoolCalendarRequest {
    // TODO(amitmanjhi, cromwellian) Request<List<PersonProxy>>
    ProxyListRequest<PersonProxy> getPeople(int startIndex, int maxCount);
  }

  AddressRequest addressRequest();

  LoggingRequest loggingRequest();

  PersonRequest personRequest();

  SchoolCalendarRequest schoolCalendarRequest();
}
