/*
 * Copyright 2016 Google Inc.
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
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/SecurityException.html">the
 * official Java API doc</a> for details.
 * 
 * This exception is never thrown by GWT or GWT's libraries, as GWT does not have a SecurityManager.
 * It is provided in GWT only for compatibility with user code that explicitly catches it.
 */
public class SecurityException extends RuntimeException {
  private SecurityException() {
  }
}
