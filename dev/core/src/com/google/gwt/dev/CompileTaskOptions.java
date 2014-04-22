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

import com.google.gwt.dev.util.arg.OptionFinalProperties;
import com.google.gwt.dev.util.arg.OptionLibraryPaths;
import com.google.gwt.dev.util.arg.OptionLink;
import com.google.gwt.dev.util.arg.OptionLogLevel;
import com.google.gwt.dev.util.arg.OptionModuleName;
import com.google.gwt.dev.util.arg.OptionOutputLibraryPath;
import com.google.gwt.dev.util.arg.OptionWorkDir;

/**
 * A common set of options for all compile tasks.
 */
public interface CompileTaskOptions extends OptionModuleName, OptionLogLevel, OptionWorkDir,
    OptionOutputLibraryPath, OptionLibraryPaths, OptionFinalProperties, OptionLink {
}
