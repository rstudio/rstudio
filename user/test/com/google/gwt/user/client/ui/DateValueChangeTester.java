/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

import junit.framework.Assert;

import java.util.Date;
import java.util.LinkedList;


/**
 * Handy tool for testing classes that implement {@link HasValue} of
 * {@link Date}.
 */
public class DateValueChangeTester extends Assert {
  static class Handler implements ValueChangeHandler<Date> {
    private LinkedList<Date> received = new LinkedList<Date>();

    @Override
    public void onValueChange(ValueChangeEvent<Date> event) {
      received.addLast(event.getValue());
    }

    private void assertSingleEvent(Date date) {
      Date receivedEvent = received.removeFirst();
      assertEquals(date, receivedEvent);
      assertNotSame(date, receivedEvent);
      assertNoEvents();
    }

    private void assertNoEvents() {
      assertTrue(received.isEmpty());
    }
  }

  private final HasValue<Date> subject;

  /**
   * The HasValue<Date> to be tested. It should have been freshly created before
   * handing it to this tester.
   */
  public DateValueChangeTester(HasValue<Date> subject) {
    this.subject = subject;
  }

  /**
   * Asserts that the default value is null, checks that value change events do
   * and don't fire when appropriate, and that getValue() always returns what
   * was handed to getValue().
   */
  @SuppressWarnings("deprecation")
  public void run() {

    // Negative test cases.
    assertNull(subject.getValue());

    DateValueChangeTester.Handler h = new Handler();
    subject.addValueChangeHandler(h);

    subject.setValue(null);
    assertNull(subject.getValue());
    h.assertNoEvents();

    Date able = new Date(1999, 5, 15);
    subject.setValue(able);
    assertEquals(able, subject.getValue());
    h.assertNoEvents();

    subject.setValue(able);
    h.assertNoEvents();

    Date baker = new Date(1965, 12, 7);
    subject.setValue(baker);
    h.assertNoEvents();

    // Positive test cases.

    // Value has not changed, so should not fire a change event even though
    // fire event is true.
    fire(baker);
    h.assertNoEvents();
    fire(able);
    h.assertSingleEvent(able);

    fire(baker);
    h.assertSingleEvent(baker);

    // Value has changed, but boolean is false.
    subject.setValue(baker, false);
    h.assertNoEvents();

    Date highResolutionDate = new Date(); // Date 'now' includes milliseconds
    fire(highResolutionDate);
    normalizeTime(highResolutionDate);
    h.assertSingleEvent(highResolutionDate);
  }

  protected void normalizeTime(Date highResolutionDate) {
    // no-op by default but may be overridden to decrease precision of the time w.r.t format
  }

  protected void fire(Date d) {
    subject.setValue(d, true);
  }
}
