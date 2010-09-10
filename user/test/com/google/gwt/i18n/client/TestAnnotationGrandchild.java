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
package com.google.gwt.i18n.client;

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Verifies that class-level annotations on grandparent interface are still honored,
 * to make sure multiple levels of inheritance are handled.
 */
public interface TestAnnotationGrandchild extends TestAnnotationInheritance {

  @Messages.DefaultMessage("baz")
  String baz();

  @Messages.DefaultMessage("baz")
  @Key("73FEFFA4B7F6BB68E44CF984C85F6E88")
  SafeHtml bazAsSafeHtml();
}
