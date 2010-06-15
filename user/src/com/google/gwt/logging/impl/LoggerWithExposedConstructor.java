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

package com.google.gwt.logging.impl;

import java.util.logging.Logger;

/**
 * Since the Impl classes are in a different package than the classes they
 * implement, they cannot use the protected Logger constructor. This subclass
 * of Logger, which just explses the constructor, gets around that, and it's
 * protected constructor is only available in the impl package, so clients
 * cannot use it.
 */
public class LoggerWithExposedConstructor extends Logger {
  protected LoggerWithExposedConstructor(String name) {
    super(name, null);
  }
}
