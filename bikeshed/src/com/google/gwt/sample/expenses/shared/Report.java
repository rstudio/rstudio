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
package com.google.gwt.sample.expenses.shared;

import com.google.gwt.requestfactory.shared.Entity;
import com.google.gwt.requestfactory.shared.Slot;
import com.google.gwt.valuestore.shared.Property;

import java.util.Date;

/**
 * "Generated" proxy of {@link com.google.gwt.sample.expenses.domain.Report
 * domain.Report}.
 */
public class Report implements Entity<Report> {

  public static final Property<Report, String> ID = new Property<Report, String>(
      Report.class, String.class, "ID");

  public static final Property<Report, Integer> VERSION = new Property<Report, Integer>(
      Report.class, Integer.class, "VERSION");

  public static final Property<Report, Date> CREATED = new Property<Report, Date>(
      Report.class, Date.class, "CREATED");
  
  public static final Property<Report, String> PURPOSE = new Property<Report, String>(
      Report.class, String.class, "PURPOSE");

  private final String id;
  private final Integer version;

  Report(String id, Integer version) {
    this.id = id;
    this.version = version;
  }

  public String getId() {
    return id;
  }

  public Integer getVersion() {
    return version;
  }

  public <V> Slot<Report, V> slot(Property<Report, V> property) {
    return new Slot<Report, V>(this, property);
  }
}
