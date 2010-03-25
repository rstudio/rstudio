/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.junit;

/**
 * An enum to indicate the Platform where a test should run. In general, it
 * should be able to represent a large matrix such as: Browser * Os_combo *
 * {hosted, web} * {htmlUnit, actual_browser}.
 * <p>
 * For HtmlUnit, we distinguish among three categories of failures:
 * <li>{@link HtmlUnitBug}: Gwt tests that are failing due to a bug in HtmlUnit.
 * Ideally, these must be accompanied by a bug report on the HtmlUnit issue
 * tracker.
 * <li>{@link HtmlUnitLayout}: Gwt tests that test layout. HtmlUnit does not use
 * a layout engine, though some simple layout tests do pass with HtmlUnit.
 * <li>{@link HtmlUnitUnknown}: Gwt tests whose failures have not been
 * investigated yet.
 *
 */
public enum Platform {
  Devel, HtmlUnitBug, HtmlUnitLayout, HtmlUnitUnknown, Prod,
}
