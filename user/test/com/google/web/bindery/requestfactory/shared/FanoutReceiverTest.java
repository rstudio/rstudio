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
package com.google.web.bindery.requestfactory.shared;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.event.shared.UmbrellaException;

import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * A simple unit test of FanoutReceiver.
 */
public class FanoutReceiverTest extends GWTTestCase {
  private static class CountingReceiver extends Receiver<Integer> {
    private boolean explode;
    private int onConstraintViolation;
    private int onFailure;
    private int onSuccess;
    private int onViolation;

    public void check() {
      assertEquals(1, onConstraintViolation);
      assertEquals(1, onFailure);
      assertEquals(1, onSuccess);
      assertEquals(1, onViolation);
    }

    private void maybeExplode() {
      if (explode) {
        throw new RuntimeException(MESSAGE);
      }
    }

    @Override
    public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
      maybeExplode();
      onConstraintViolation++;
    }

    @Override
    public void onFailure(ServerFailure error) {
      maybeExplode();
      onFailure++;
    }

    @Override
    public void onSuccess(Integer response) {
      maybeExplode();
      assertEquals(EXPECTED_VALUE, response.intValue());
      onSuccess++;
    }

    @Deprecated
    @Override
    public void onViolation(Set<Violation> errors) {
      maybeExplode();
      onViolation++;
    }

    public void setExplode(boolean explode) {
      this.explode = explode;
    }
  }

  private static final int EXPECTED_VALUE = 42;
  private static final String MESSAGE = "It didn't work!";

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  @SuppressWarnings("deprecation")
  public void test() {
    CountingReceiver c1 = new CountingReceiver();
    CountingReceiver c2 = new CountingReceiver();
    FanoutReceiver<Integer> fan = new FanoutReceiver<Integer>();
    fan.add(c1);
    fan.add(c2);
    fan.onConstraintViolation(null);
    fan.onFailure(null);
    fan.onSuccess(EXPECTED_VALUE);
    fan.onViolation(null);
    c1.check();
    c2.check();
  }

  public void testAddNull() {
    try {
      new FanoutReceiver<Void>().add(null);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @SuppressWarnings("deprecation")
  public void testExceptions() {
    CountingReceiver c1 = new CountingReceiver();
    c1.setExplode(true);
    CountingReceiver c2 = new CountingReceiver();
    FanoutReceiver<Integer> fan = new FanoutReceiver<Integer>();
    fan.add(c1);
    fan.add(c2);
    try {
      fan.onConstraintViolation(null);
      fail("Expected UmbrellaException");
    } catch (UmbrellaException ex) {
      assertEquals(1, ex.getCauses().size());
      assertEquals(MESSAGE, ex.getCause().getMessage());
    }
    try {
      fan.onFailure(null);
      fail("Expected UmbrellaException");
    } catch (UmbrellaException ex) {
      assertEquals(1, ex.getCauses().size());
      assertEquals(MESSAGE, ex.getCause().getMessage());
    }
    try {
      fan.onSuccess(EXPECTED_VALUE);
      fail("Expected UmbrellaException");
    } catch (UmbrellaException ex) {
      assertEquals(1, ex.getCauses().size());
      assertEquals(MESSAGE, ex.getCause().getMessage());
    }
    try {
      fan.onViolation(null);
      fail("Expected UmbrellaException");
    } catch (UmbrellaException ex) {
      assertEquals(1, ex.getCauses().size());
      assertEquals(MESSAGE, ex.getCause().getMessage());
    }
    // Make sure that c2 stil gets called
    c2.check();
  }
}
