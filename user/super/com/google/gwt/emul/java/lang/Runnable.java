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
 * Encapsulates an action for later execution.
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/Runnable.html">
 * the official Java API doc</a> for details.
 *
 * <p>
 * This interface is provided only for JRE compatibility. GWT does not support
 * multithreading.
 * </p>
 */
@FunctionalInterface
public interface Runnable {
  void run();
}
