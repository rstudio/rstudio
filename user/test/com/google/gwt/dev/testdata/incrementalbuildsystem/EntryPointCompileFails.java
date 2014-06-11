/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.testdata.incrementalbuildsystem;

import com.google.gwt.core.client.EntryPoint;

/**
 * An entry point test class that should immediately fail to compile because it references a type
 * that fails to compile.
 */
public class EntryPointCompileFails implements EntryPoint {

  public ImmediateCompileFails immediateCompileFails;

  @Override
  public void onModuleLoad() {
    immediateCompileFails = new ImmediateCompileFails();
  }
}
