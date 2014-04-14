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
package com.google.gwt.dev.js;

/**
 * An interface for producing fresh names.
 *
 * Objects that implement this generator retain some global state about scopes/programs to be
 * able to produce fresh names.
 *
 * The names produced by a name generator might be arbitrary as long as they are fresh. Specific
 * FreshNameGenerators might be related to global renamers to produce fresh names according to
 * some consistent pattern. E.g. after a program is obfuscated one might need a FreshNameGenerator
 * that produces fresh obfuscated names to be introduced to the ast.
 */
public interface FreshNameGenerator {
  /**
   * returns a fresh (unused) name.
   */
  String getFreshName();
}
