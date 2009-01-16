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
package com.google.gwt.dev;

import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerModuleName;
import com.google.gwt.dev.util.arg.ArgHandlerTreeLoggerFlag;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirRequired;

abstract class CompileArgProcessor extends ArgProcessorBase {
  public CompileArgProcessor(CompileTaskOptions options) {
    registerHandler(new ArgHandlerLogLevel(options));
    registerHandler(new ArgHandlerTreeLoggerFlag(options));
    registerHandler(new ArgHandlerWorkDirRequired(options));
    registerHandler(new ArgHandlerModuleName(options) {
      @Override
      public String getPurpose() {
        return super.getPurpose() + " to compile";
      }
    });
  }
}
