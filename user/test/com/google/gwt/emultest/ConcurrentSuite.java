/*
 * Copyright 2017 Google Inc.
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
package com.google.gwt.emultest;

import com.google.gwt.emultest.java.util.concurrent.ConcurrentHashMapTest;
import com.google.gwt.emultest.java.util.concurrent.TimeUnitTest;
import com.google.gwt.emultest.java.util.concurrent.atomic.AtomicIntegerTest;
import com.google.gwt.emultest.java.util.concurrent.atomic.AtomicLongTest;
import com.google.gwt.emultest.java.util.concurrent.atomic.AtomicReferenceArrayTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/** Test JRE emulation of java.util.concurrent. */
@RunWith(Suite.class)
@SuiteClasses({
  ConcurrentHashMapTest.class,
  TimeUnitTest.class,
  AtomicIntegerTest.class,
  AtomicLongTest.class,
  AtomicReferenceArrayTest.class,
})
public class ConcurrentSuite { }
