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
package com.google.gwt.dev.cfg;

/**
 * A shorthand rule to replace the type being rebound with itself.<br />
 *
 * Useful when defining fallback rebind rules that makes sure that a GWT.create(Foo.class) will
 * attempt to instantiate a Foo if no other replacement rebinds have been declared or are currently
 * applicable.
 */
public class RuleReplaceWithFallback extends RuleReplaceWith {

  public RuleReplaceWithFallback(String typeSourceName) {
    super(typeSourceName);
  }

  @Override
  protected String generateMatchesExpression() {
    return String.format("return requestTypeClass == @%s::class;", getReplacementTypeName());
  }
}
