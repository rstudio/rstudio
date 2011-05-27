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

import com.google.web.bindery.event.shared.UmbrellaException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * A FanoutReceiver will forward its callbacks to zero or more other Receivers.
 * Any exceptions thrown by the queued Receivers will be re-thrown as an
 * {@link UmbrellaException} after all Receivers have been invoked.
 * 
 * @param <T> the type of data being received
 */
public class FanoutReceiver<T> extends Receiver<T> {
  private List<Receiver<? super T>> toCall;
  private Set<Throwable> toThrow;

  /**
   * Register a receiver to be called by the fanout.
   * 
   * @throws IllegalArgumentException if {@code receiver} is {@code null}
   */
  public void add(Receiver<? super T> receiver) {
    if (receiver == null) {
      throw new IllegalArgumentException();
    }
    if (toCall == null) {
      toCall = new ArrayList<Receiver<? super T>>();
    }
    toCall.add(receiver);
  }

  @Override
  public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
    try {
      if (toCall != null) {
        for (Receiver<? super T> r : toCall) {
          try {
            r.onConstraintViolation(violations);
          } catch (Throwable t) {
            onUncaughtThrowable(t);
          }
        }
      }
    } finally {
      finish();
    }
  }

  @Override
  public void onFailure(ServerFailure error) {
    try {
      if (toCall != null) {
        for (Receiver<? super T> r : toCall) {
          try {
            r.onFailure(error);
          } catch (Throwable t) {
            onUncaughtThrowable(t);
          }
        }
      }
    } finally {
      finish();
    }
  }

  @Override
  public void onSuccess(T response) {
    try {
      if (toCall != null) {
        for (Receiver<? super T> r : toCall) {
          try {
            r.onSuccess(response);
          } catch (Throwable t) {
            onUncaughtThrowable(t);
          }
        }
      }
    } finally {
      finish();
    }
  }

  @Deprecated
  @Override
  public void onViolation(Set<Violation> errors) {
    try {
      if (toCall != null) {
        for (Receiver<? super T> r : toCall) {
          try {
            r.onViolation(errors);
          } catch (Throwable t) {
            onUncaughtThrowable(t);
          }
        }
      }
    } finally {
      finish();
    }
  }

  /**
   * Called after all Receivers have been executed.
   */
  protected void finish() {
    if (toThrow != null) {
      // Reset if the user wants to re-fire the Request
      Set<Throwable> causes = toThrow;
      toThrow = null;
      throw new UmbrellaException(causes);
    }
  }

  /**
   * Subclasses may override this method to alter how the FanoutReceiver
   * collects exceptions that escape from the queued Receivers.
   */
  protected void onUncaughtThrowable(Throwable t) {
    if (toThrow == null) {
      toThrow = new LinkedHashSet<Throwable>();
    }
    toThrow.add(t);
  }
}