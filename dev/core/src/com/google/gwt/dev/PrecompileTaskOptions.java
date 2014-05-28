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
package com.google.gwt.dev;

import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.util.arg.OptionDisableUpdateCheck;
import com.google.gwt.dev.util.arg.OptionEnableGeneratingOnShards;
import com.google.gwt.dev.util.arg.OptionGenDir;
import com.google.gwt.dev.util.arg.OptionMaxPermsPerPrecompile;
import com.google.gwt.dev.util.arg.OptionMissingDepsFile;
import com.google.gwt.dev.util.arg.OptionSaveSource;
import com.google.gwt.dev.util.arg.OptionSourceMapFilePrefix;
import com.google.gwt.dev.util.arg.OptionValidateOnly;
import com.google.gwt.dev.util.arg.OptionWarnMissingDeps;
import com.google.gwt.dev.util.arg.OptionWarnOverlappingSource;

/**
 * The set of options for the Precompiler.
 */
public interface PrecompileTaskOptions extends JJSOptions, CompileTaskOptions, OptionGenDir,
    OptionSaveSource, OptionSourceMapFilePrefix, OptionValidateOnly, OptionDisableUpdateCheck,
    OptionEnableGeneratingOnShards, OptionMaxPermsPerPrecompile, OptionMissingDepsFile,
    OptionWarnOverlappingSource, OptionWarnMissingDeps, PrecompilationResult {
}
