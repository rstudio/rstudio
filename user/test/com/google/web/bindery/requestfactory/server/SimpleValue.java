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
package com.google.web.bindery.requestfactory.server;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.Null;

/**
 * A domain object that is used to demonstrate value-object behaviors.
 */
public class SimpleValue {
  private Date date;
  private int number;
  private SimpleFoo simpleFoo;
  /**
   * Constraint violation testing.
   */
  @Null
  private String shouldBeNull;
  private List<SimpleValue> simpleValue;
  private String string;

  public Date getDate() {
    return date;
  }

  public int getNumber() {
    return number;
  }

  public String getShouldBeNull() {
    return shouldBeNull;
  }

  public SimpleFoo getSimpleFoo() {
    return simpleFoo;
  }

  public List<SimpleValue> getSimpleValue() {
    return simpleValue;
  }

  public String getString() {
    return string;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public void setShouldBeNull(String value) {
    this.shouldBeNull = value;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public void setSimpleFoo(SimpleFoo simpleFoo) {
    this.simpleFoo = simpleFoo;
  }

  public void setSimpleValue(List<SimpleValue> simpleValue) {
    this.simpleValue = simpleValue;
  }

  public void setString(String string) {
    this.string = string;
  }
}
