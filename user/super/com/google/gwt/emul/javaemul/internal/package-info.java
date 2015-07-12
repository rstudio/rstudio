/*
 * Copyright 2015 Google Inc.
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

/**
 * Contains internal classes used for GWT's JRE implementation.
 *
 * <p>The classes in here are not meant to be used by GWT users at all and can change at any time.
 * Classes that go in here play an important supporting role in GWT's JRE implementation. They
 * should also be mostly transpilable with other Java to JavaScript compilers. This means that they
 * can not refer to any GWT specific implementations (no classes outside of the JRE).
 */
package javaemul.internal;