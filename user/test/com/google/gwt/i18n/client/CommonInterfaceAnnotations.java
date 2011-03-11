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

import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Base interface to test annotation inheritance.
 *
 * <p>This works by setting the key generator to MD5 on this interface,
 * then verifying that keys in the subinterface are looked up with
 * MD5 hashes rather than method names.
 */
// Note specifically using the old MD5KeyGenerator to make sure still have some
// coverage of it.
@GenerateKeys("com.google.gwt.i18n.rebind.keygen.MD5KeyGenerator")
public interface CommonInterfaceAnnotations extends Messages {

  @DefaultMessage("foo")
  String foo();

  @DefaultMessage("foo")
  @Key("foo")
  SafeHtml fooAsSafeHtml();
}
