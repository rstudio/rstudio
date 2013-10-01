/*
 * Copyright 2013 Google Inc.
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

/**
 * Contains most global read-only compiler state and makes it easily accessible to the far flung
 * reaches of the compiler call graph without the constant accumulation of more and more function
 * parameters.<br />
 *
 * This initial implementation starts with just the global options but will be extended to include
 * the top level module definition, various oracles, the compilation unit cache and eventually input
 * libraries for separate compilation.
 */
public class CompilerContext {

  /**
   * CompilerContext builder.
   */
  public static class Builder {
    private PrecompileTaskOptions options;

    public CompilerContext build() {
      CompilerContext compilerContext = new CompilerContext();
      compilerContext.options = options;
      return compilerContext;
    }

    public Builder options(PrecompileTaskOptions options) {
      this.options = options;
      return this;
    }
  }

  // TODO(stalcup): split this into module parsing, precompilation, compilation, and linking option
  // sets.
  private PrecompileTaskOptions options = new PrecompileTaskOptionsImpl();

  public PrecompileTaskOptions getOptions() {
    return options;
  }

  public void setOptions(PrecompileTaskOptions options) {
    this.options = options;
  }
}
