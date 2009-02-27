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

import junit.framework.TestCase;

import java.util.Date;

/**
 * Handy tool for testing classes that implement {@link HasValue} of
 * {@link Date}.
 */
public class DateValueChangeTester {
  static class Handler implements ValueChangeHandler<Date> {
    Date received = null;

    public void onValueChange(ValueChangeEvent<Date> event) {
      received = event.getValue();
    }
  }

  protected final HasValue<Date> subject;

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
    TestCase.assertNull(subject.getValue());

    DateValueChangeTester.Handler h = new Handler();
    subject.addValueChangeHandler(h);

    subject.setValue(null);
    TestCase.assertNull(subject.getValue());
    TestCase.assertNull(h.received);

    Date able = new Date(1999, 5, 15);
    subject.setValue(able);
    TestCase.assertEquals(able, subject.getValue());
    TestCase.assertNull(h.received);

    subject.setValue(able);
    TestCase.assertNull(h.received);

    Date baker = new Date(1965, 12, 7);
    subject.setValue(baker);
    TestCase.assertNull(h.received);

    // Positive test cases.

    // Value has not changed, so should not fire a change event even though
    // fire event is true.
    fire(baker);
    TestCase.assertNull(h.received);
    fire(able);
    TestCase.assertEquals(able, h.received);
    TestCase.assertNotSame(able, h.received);

    fire(baker);
    TestCase.assertEquals(baker, h.received);
    TestCase.assertNotSame(baker, h.received);
    
    h.received = null;
    // Value has changed, but boolean is false.
    subject.setValue(baker, false);
    TestCase.assertNull(h.received);
  }

  protected void fire(Date d) {
    subject.setValue(d, true);
  }

}
