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
package com.google.web.bindery.requestfactory.shared.impl.posers;

import com.google.web.bindery.requestfactory.shared.impl.Poser;

import java.util.Date;

/**
 * A sometimes-mutable implementation of {@link Date}.
 */
@SuppressWarnings("deprecation")
public class DatePoser extends Date implements Poser<Date> {
  private boolean frozen;

  public DatePoser(Date copy) {
    super(copy.getTime());
    setFrozen(true);
  }

  public Date getPosedValue() {
    return new Date(getTime());
  }

  public boolean isFrozen() {
    return frozen;
  }

  @Override
  public void setDate(int date) {
    checkFrozen();
    super.setDate(date);
  }

  public void setFrozen(boolean frozen) {
    this.frozen = frozen;
  }

  @Override
  public void setHours(int hours) {
    checkFrozen();
    super.setHours(hours);
  }

  @Override
  public void setMinutes(int minutes) {
    checkFrozen();
    super.setMinutes(minutes);
  }

  @Override
  public void setMonth(int month) {
    checkFrozen();
    super.setMonth(month);
  }

  @Override
  public void setSeconds(int seconds) {
    checkFrozen();
    super.setSeconds(seconds);
  }

  @Override
  public void setTime(long time) {
    checkFrozen();
    super.setTime(time);
  }

  @Override
  public void setYear(int year) {
    checkFrozen();
    super.setYear(year);
  }

  private void checkFrozen() {
    if (frozen) {
      throw new IllegalStateException("The Date has been frozen");
    }
  }
}
