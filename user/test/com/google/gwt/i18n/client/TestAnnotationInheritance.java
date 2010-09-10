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
 * Verifies that class-level annotations on superinterface are still used
 * by this interface.  In this case, we verify that this interface's keys
 * are based on the MD5 hash of the default value.
 */
public interface TestAnnotationInheritance extends CommonInterfaceAnnotations {

  @Messages.DefaultMessage("bar")
  String bar();

  @Messages.DefaultMessage("bar")
  @Key("37B51D194A7513E45B56F6524F2D51F2")
  SafeHtml barAsSafeHtml();
}
