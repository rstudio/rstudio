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
package com.google.gwt.sample.dynatablerf.server;

import com.google.gwt.sample.dynatablerf.domain.Address;

import java.util.Random;

/**
 * Utility class for creating random addresses.
 */
class AddressFuzzer {
  private static final String[] CITY_NAMES = new String[] {
      "Mountain View", "Palo Alto", "Sunnyvale", "Los Gatos", "Santa Clara",
      "San Jose", "Cupertino", "Saratoga", "Gilroy", "Milpitas"};

  private static final String[] STATE_NAMES = new String[] {
      "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado",
      "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho",
      "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana",
      "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota",
      "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada",
      "New Hampshire", "New Jersey", "New Mexico", "New York",
      "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon",
      "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
      "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington",
      "West Virginia", "Wisconsin", "Wyoming"};

  private static final String[] STREET_NAMES = new String[] {
      "Amphitheatre Pkwy", "Charleston Rd", "Garcia Ave", "Stierlin Ct",
      "Alta Ave", "Huff Ave", "Joaquin Rd", "Plymouth St", "Crittenden Ln",
      "Salado Dr"};

  public static void fuzz(Random r, Address a) {
    a.setCity(CITY_NAMES[r.nextInt(CITY_NAMES.length)]);
    a.setStreet(r.nextInt(4096) + " "
        + STREET_NAMES[r.nextInt(STREET_NAMES.length)]);
    a.setState(STATE_NAMES[r.nextInt(STATE_NAMES.length)]);
    StringBuilder zip = new StringBuilder();
    zip.append(String.format("%05d", r.nextInt(99999)));
    if (r.nextBoolean()) {
      zip.append(String.format("-%04d", r.nextInt(9999)));
    }
    a.setZip(zip.toString());
  }
}
