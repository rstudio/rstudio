/*
 * Copyright 2012 Google Inc.
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
package com.google.web.bindery.autobean.benchmarks;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.benchmarks.client.RangeField;
import com.google.web.bindery.autobean.shared.ValueCodex;

import java.util.HashSet;
import java.util.Set;

/**
 * Benchmarks {@link ValueCodex#findType} (by mean of
 * {@link ValueCodex#canDecode(Class)} and {@link ValueCodex#encode(Object)}) to
 * measure the impact of a change made to it.
 * 
 * @see http://gwt-code-reviews.appspot.com/1601805/
 */
public class ValueCodexBenchmark extends Benchmark {

  enum MyEnum {
    /** */
    FOO,
    /** */
    BAR {
      @SuppressWarnings("unused")
      private void dummy() {
      }
    }
  }

  final Object[] values = {
      MyEnum.FOO, MyEnum.BAR, Boolean.TRUE, Integer.valueOf(42), "string", new java.util.Date(),
      new java.sql.Date(new java.util.Date().getTime())};

  final Set<Class<?>> allValueTypes;
  {
    allValueTypes = new HashSet<Class<?>>(ValueCodex.getAllValueTypes());
    allValueTypes.add(MyEnum.class);
    allValueTypes.add(MyEnum.BAR.getClass());
  }

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.autobean.AutoBeanBenchmarks";
  }

  // Required by JUnit
  public void testEncode() {
  }

  public void testEncode(@RangeField("values") Object value) {
    final int times = 100;
    for (int i = 0; i < times; i++) {
      ValueCodex.encode(value);
    }
  }

  // Required by JUnit
  public void testCanDecode() {
  }

  public void testCanDecode(@RangeField("allValueTypes") Class<?> valueType) {
    final int times = 100;
    for (int i = 0; i < times; i++) {
      ValueCodex.canDecode(valueType);
    }
  }
}
