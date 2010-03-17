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

import com.google.gwt.requestfactory.shared.FieldRef;
import com.google.gwt.requestfactory.shared.LongString;
import com.google.gwt.requestfactory.shared.ServerType;
import com.google.gwt.valuestore.shared.Property;

import java.util.Date;

/**
 * "Generated" proxy of
 * {@link com.google.gwt.sample.expenses.server.domain.Report domain.Report}.
 */
@ServerType(com.google.gwt.sample.expenses.server.domain.Report.class)
public class ReportRef implements ExpensesEntity<ReportRef> {

  @LongString
  public static final Property<ReportRef, String> ID = new Property<ReportRef, String>(
      ReportRef.class, String.class, "ID");

  public static final Property<ReportRef, Integer> VERSION = new Property<ReportRef, Integer>(
      ReportRef.class, Integer.class, "VERSION");

  public static final Property<ReportRef, Date> CREATED = new Property<ReportRef, Date>(
      ReportRef.class, Date.class, "CREATED");

  public static final Property<ReportRef, String> PURPOSE = new Property<ReportRef, String>(
      ReportRef.class, String.class, "PURPOSE");

  private final String id;
  private final Integer version;

  public ReportRef(String id, Integer version) {
    this.id = id;
    this.version = version;
  }

  public <T> T accept(ExpensesEntityFilter<T> filter) {
    return filter.filter(this);
  }

  public void accept(ExpensesEntityVisitor visitor) {
    visitor.visit(this);
  }

  public <V> FieldRef<ReportRef, V> getFieldRef(Property<ReportRef, V> property) {
    return new FieldRef<ReportRef, V>(this, property);
  }

  public String getId() {
    return id;
  }

  public Integer getVersion() {
    return version;
  }
}
