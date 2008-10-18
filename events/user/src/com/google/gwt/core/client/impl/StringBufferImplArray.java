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
package com.google.gwt.core.client.impl;

/**
 * A {@link StringBufferImpl} that uses an array and an explicit length for
 * appending strings. Note that the length of the array is stored as a property
 * of the underlying JavaScriptObject because making it a field on this object
 * causes difficulty with inlining. This is the best implementation on IE, and
 * generally second best on all other browsers, making it the best default when
 * the user agent is unknown.
 */
public class StringBufferImplArray extends StringBufferImplArrayBase {
}
