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
package com.google.gwt.sample.dynatablerf.console;

import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.requestfactory.server.testing.RequestFactoryMagic;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.sample.dynatablerf.shared.AddressProxy;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * A proof-of-concept to demonstrate how RequestFactory can be used from
 * non-client code.
 */
public class Console {
  public static void main(String[] args) {
    String url = "http://localhost:8888/gwtRequest";
    if (args.length == 1) {
      url = args[0];
    }
    try {
      new Console(new URI(url)).exec();
      System.exit(0);
    } catch (URISyntaxException e) {
      System.err.println("Could not parse argument");
    }
    System.exit(1);
  }

  private final DynaTableRequestFactory rf;

  private Console(URI uri) {
    /*
     * Instantiation of the RequestFactory interface uses the
     * RequestFactoryMagic class instead of GWT.create().
     */
    this.rf = RequestFactoryMagic.create(DynaTableRequestFactory.class);
    // Initialization follows the same pattern as client code
    rf.initialize(new SimpleEventBus(), new HttpClientTransport(uri));
  }

  /**
   * Making a request from non-GWT code is similar. The implementation of the
   * demonstration HttpClientTransport issues the requests synchronously. A
   * different transport system might use asynchronous callbacks.
   */
  private void exec() {
    rf.schoolCalendarRequest().getPeople(0, 100,
        Arrays.asList(true, true, true, true, true, true, true)).with("address").fire(
        new Receiver<List<PersonProxy>>() {
          @Override
          public void onSuccess(List<PersonProxy> response) {
            // Print each record to the console
            for (PersonProxy person : response) {
              AddressProxy address = person.getAddress();
              String addressBlob = address.getStreet() + " "
                  + address.getCity() + " " + address.getState() + " "
                  + address.getZip();
              System.out.printf("%-40s%40s\n%80s\n\n", person.getName(),
                  person.getDescription(), addressBlob);
            }
          }
        });
  }
}
