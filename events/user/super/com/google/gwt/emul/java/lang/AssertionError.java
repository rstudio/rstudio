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
package java.lang;

/**
 * Represents an error caused by an assertion failure.
 */
public class AssertionError extends Error {

  public AssertionError() {
  }

  public AssertionError(boolean message) {
    this(String.valueOf(message));
  }

  public AssertionError(char message) {
    this(String.valueOf(message));
  }

  public AssertionError(double message) {
    this(String.valueOf(message));
  }

  public AssertionError(float message) {
    this(String.valueOf(message));
  }

  public AssertionError(int message) {
    this(String.valueOf(message));
  }

  public AssertionError(long message) {
    this(String.valueOf(message));
  }

  public AssertionError(Object message) {
    super(String.valueOf(message), message instanceof Throwable
      ? (Throwable) message : null);
  }

  private AssertionError(String message) {
    super(message);
  }
}
