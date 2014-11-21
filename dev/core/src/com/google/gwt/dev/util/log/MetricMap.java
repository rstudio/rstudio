/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.util.log;

import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSortedMap;

import java.util.Map;

/**
 * Holds mappings from metrics to their values. Thread-safe.
 */
public class MetricMap implements CanUpdateMetrics {
  private final Map<String, Long> map = new HashMap<String, Long>();

  /**
   * Each MetricMap lives in one or more TreeLoggers.
   * See {@link AbstractTreeLogger#resetMetricMap()}.
   */
  MetricMap() {
  }

  public synchronized void setAmount(MetricName name, long amount) {
    map.put(name.key, amount);
  }

  public synchronized ImmutableSortedMap<String, Long> getSnapshot() {
    return ImmutableSortedMap.copyOf(map);
  }
}
