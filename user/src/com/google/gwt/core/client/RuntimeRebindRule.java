/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.client;

/**
 * A base for rule classes that can judge conditions and create object instances as part of the
 * runtime rebind rule framework.<br />
 *
 * Instances are registered with the RuntimeRebinder as part of module bootstrapping and are queried
 * and creation invoked to service GWT.create() invocations.<br />
 *
 * Subclasses are dynamically generated during compilation to match replacement, generator output
 * and fallback rebind rules.
 */
public abstract class RuntimeRebindRule {

  /**
   * Returns a newly created instance of the requested type or some replacement.
   */
  public abstract Object createInstance();

  /**
   * Returns whether the requested type name along with the current browser environment satisfies
   * the condition embedded in this rule.
   */
  public abstract boolean matches(String requestTypeName);
}
