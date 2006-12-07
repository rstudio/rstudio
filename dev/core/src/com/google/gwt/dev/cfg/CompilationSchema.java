/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.dev.util.xml.Schema;

/**
 * The XML schema class to parse XML for deferred binding conditions.
 */
public class CompilationSchema extends Schema {

  private final class BodySchema extends Schema {
    protected final String __generated_type_hash_1_class = null;

    protected final String __generated_type_hash_2_hash = null;

    protected final String __rebind_decision_1_in = null;
    protected final String __rebind_decision_2_out = null;

    protected Schema __generated_type_hash_begin(String type, String hash) {
      compilation.recordGeneratedTypeHash(type, hash);
      return null;
    }

    protected Schema __rebind_decision_begin(String in, String out) {
      compilation.recordDecision(in, out);
      return null;
    }
  }

  private final Compilation compilation;

  public CompilationSchema(Compilation compilation) {
    this.compilation = compilation;
  }

  protected Schema __cache_entry_begin() {
    return new BodySchema();
  }
}
