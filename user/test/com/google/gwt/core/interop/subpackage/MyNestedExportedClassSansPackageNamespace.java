/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.core.interop.subpackage;

import jsinterop.annotations.JsType;

/**
 * An exported class that is also not subject to the package namespace specified in its parent
 * package.
 */
@JsType // Does *not* uses the namespace from the parent package
public class MyNestedExportedClassSansPackageNamespace {
  public final static int WOO = 1001;
}
