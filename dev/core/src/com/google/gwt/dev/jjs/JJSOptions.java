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
package com.google.gwt.dev.jjs;

import com.google.gwt.dev.util.arg.OptionAggressivelyOptimize;
import com.google.gwt.dev.util.arg.OptionDisableCastChecking;
import com.google.gwt.dev.util.arg.OptionDisableClassMetadata;
import com.google.gwt.dev.util.arg.OptionEnableAssertions;
import com.google.gwt.dev.util.arg.OptionEnableClosureCompiler;
import com.google.gwt.dev.util.arg.OptionFragmentCount;
import com.google.gwt.dev.util.arg.OptionFragmentsMerge;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.arg.OptionOptimizePrecompile;
import com.google.gwt.dev.util.arg.OptionRunAsyncEnabled;
import com.google.gwt.dev.util.arg.OptionScriptStyle;
import com.google.gwt.dev.util.arg.OptionSoycDetailed;
import com.google.gwt.dev.util.arg.OptionSoycEnabled;
import com.google.gwt.dev.util.arg.OptionSoycHtmlDisabled;
import com.google.gwt.dev.util.arg.OptionStrict;

/**
 * Controls options for the {@link JavaToJavaScriptCompiler}.
 */
public interface JJSOptions extends OptionOptimize, OptionAggressivelyOptimize,
    OptionDisableClassMetadata, OptionDisableCastChecking, OptionEnableAssertions,
    OptionRunAsyncEnabled, OptionScriptStyle, OptionSoycEnabled, OptionSoycDetailed,
    OptionOptimizePrecompile, OptionStrict, OptionSoycHtmlDisabled,
    OptionEnableClosureCompiler, OptionFragmentsMerge, OptionFragmentCount {

}
