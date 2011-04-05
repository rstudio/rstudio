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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <b>Experimental API, subject to change</b> Provides the method name for a
 * JSON-RPC invocation.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRpcWireName {
  String value();

  String version() default "";
}