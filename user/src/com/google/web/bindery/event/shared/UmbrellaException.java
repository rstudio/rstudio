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
package com.google.web.bindery.event.shared;

import java.util.Collections;
import java.util.Set;

/**
 * A {@link RuntimeException} that collects a {@link Set} of child
 * {@link Throwable}s together. Typically thrown after a loop, with all of the
 * exceptions thrown during that loop, but delayed so that the loop finishes
 * executing.
 */
public class UmbrellaException extends RuntimeException {

  private static final String MSG =
      "One or more exceptions caught, see full set in UmbrellaException#getCauses";
  /**
   * The causes of the exception.
   */
  private Set<Throwable> causes;

  public UmbrellaException(Set<Throwable> causes) {
    super(MSG, causes.size() == 0 ? null : causes.toArray(new Throwable[0])[0]);
    this.causes = causes;
  }

  /**
   * Required for GWT RPC serialization.
   */
  protected UmbrellaException() {
    // Can't delegate to the other constructor or GWT RPC gets cranky
    super(MSG);
    this.causes = Collections.<Throwable> emptySet();
  }

  /**
   * Get the set of exceptions that caused the failure.
   * 
   * @return the set of causes
   */
  public Set<Throwable> getCauses() {
    return causes;
  }
}
