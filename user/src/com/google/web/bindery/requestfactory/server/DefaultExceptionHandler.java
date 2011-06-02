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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.requestfactory.shared.ServerFailure;

/**
 * Default implementation for handling exceptions thrown while processing a
 * request. Suppresses stack traces and the exception class name.
 */
public class DefaultExceptionHandler implements ExceptionHandler {
  public ServerFailure createServerFailure(Throwable throwable) {
    return new ServerFailure(
        "Server Error: " + (throwable == null ? null : throwable.getMessage()), null, null, true);
  }
}