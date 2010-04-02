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
package com.google.gwt.dev.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A counter which records bunch of statistics for frequently occuring 
 * operations. Calculates: count, total time, average time, maximum time 
 * & slow operations. 
 */
public class PerfCounter {
  private static class OperationStats {
    private long count = 0;
    private boolean isCounter = false;
    private long maxTimeNanos = 0;
    private long slowCount = 0;
    private long totalSlowTimeNanos = 0;
    private long totalTimeNanos = 0;
    
    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      
      if (!isCounter) {
        result.append(count);
        result.append("/");
        result.append(totalTimeNanos / 1000000000.0);
        result.append("/");
        result.append(totalTimeNanos / 1000000000.0 / count);
        result.append("/");
        result.append(maxTimeNanos / 1000000000.0);
        result.append("/");
        result.append(slowCount);
        result.append("/");
        result.append(totalSlowTimeNanos * 1.0 / totalTimeNanos);
        result.append(" ");
        result.append(
            "(count/total_time/avg_time/max_time/slow_count/slow_ratio)");
      } else {
        result.append(count);
        result.append(" (count)");
      }
      
      return result.toString();
    }
  }

  /**
   * Flag for enabling performance logging.
   */
  private static boolean enabled = 
    Boolean.parseBoolean(System.getProperty("gwt.perfcounters"));
  
  private static final ThreadLocal<Map<String, Long>> operationsStartTime = 
    new ThreadLocal<Map<String,Long>>() {
    @Override
    protected Map<String, Long> initialValue() {
      return new HashMap<String, Long>();
    }
  };
  
  private static final Map<String, OperationStats> operationStats = 
    new HashMap<String, OperationStats>();
  
  /**
   * Record the end of the operation. 
   */
  public static void end(String operation) {
    if (!enabled) {
      return;
    }
    end(operation, 1 * 1000000000 /* 1 sec */);
  }
  
  /**
   * Record the end of the operation. 
   */
  public static void end(String operation, long slowThresholdNano) {
    if (!enabled) {
      return;
    }
    long finishTime = System.nanoTime();
    
    Map<String, Long> startTimes = operationsStartTime.get();
    
    Long startTime;
    
    synchronized (startTimes) {
      startTime = startTimes.remove(operation);
    }
    
    Preconditions.checkNotNull(startTime);
    
    synchronized (operationStats) {
      OperationStats stats = getStats(operation);
      
      stats.count++;
      long elapsedTime = finishTime - startTime.longValue();
      stats.totalTimeNanos += elapsedTime;
      stats.maxTimeNanos = Math.max(stats.maxTimeNanos, elapsedTime);
      if (elapsedTime > slowThresholdNano) {
        stats.slowCount++;
        stats.totalSlowTimeNanos += elapsedTime;
      }
    }
  }

  /**
   * Increment counter. 
   */
  public static void inc(String operation) {
    synchronized (operationStats) {
      OperationStats stats = getStats(operation);
      stats.count++;
      stats.isCounter = true;
    }
  }

  public static void print() {
    if (!enabled) {
      return;
    }
    System.out.println("------------- Perf Counters -------------");
    synchronized (operationStats) {
      List<String> keys = new ArrayList<String>(operationStats.keySet());
      Collections.sort(keys);
      for (String operation : keys) {
        System.out.println(operation + ": " + operationStats.get(operation));
      }
    }
    System.out.println("-----------------------------------------");
  }
  
  /**
   * Start operation.
   */
  public static void start(String operation) {
    if (!enabled) {
      return;
    }
    Map<String, Long> startTimes = operationsStartTime.get();
    
    synchronized (startTimes) {
      Preconditions.checkState(!startTimes.containsKey(operation));
      long startTime = System.nanoTime();
      startTimes.put(operation, new Long(startTime));
    }
  }

  private static OperationStats getStats(String operation) {
    OperationStats stats = operationStats.get(operation);
    if (stats == null) {
      stats = new OperationStats();
      operationStats.put(operation, stats);
    }
    return stats;
  }
}
