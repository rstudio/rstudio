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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

/**
 * Utility methods for dealing with VM memory.
 */
public class Memory {

  /**
   * Number of times to call System.gc() before measuring the memory
   * usage/dumping the heap. This value was arrived at through trial-and-error
   * on the Sun JVM; memory usage seems to stabilize after 3 to 4 runs. We think
   * the reason it requires multiple runs is due to the generational aspect of
   * garbage collection.
   */
  private static final int NUM_GC_COLLECTIONS = 4;

  /**
   * Set this system property to a filename suffix to dump heaps into.
   */
  private static final String PROPERTY_DUMP_HEAP = "gwt.memory.dumpHeap";

  /**
   * Set this system property to dump memory usage at various points.
   */
  private static final String PROPERTY_DUMP_MEMORY = "gwt.memory.usage";

  /**
   * Time to start measuring since the last memory measurement/dump, or
   * application startup.
   */
  private static long startTime;

  public static void initialize() {
    if (System.getProperty(PROPERTY_DUMP_MEMORY) != null) {
      System.out.println("Will print mem usage");
    }
    if (System.getProperty(PROPERTY_DUMP_HEAP) != null) {
      System.out.println("Will dump heap into: *-"
          + System.getProperty(PROPERTY_DUMP_HEAP));
    }
    startTime = System.currentTimeMillis();
  }

  public static void main(String[] args) {
    initialize();
    System.setProperty(PROPERTY_DUMP_MEMORY, "");
    maybeDumpMemory("test");
  }

  public static void maybeDumpMemory(String info) {
    long elapsed = System.currentTimeMillis() - startTime;
    if (System.getProperty(PROPERTY_DUMP_MEMORY) != null) {
      for (int i = 0; i < NUM_GC_COLLECTIONS; ++i) {
        System.gc();
      }
      long heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
      long nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
      String infoString = String.format("[%.18s]", info);
      System.out.println(String.format(
          "%-20s %10d heap, %10d nonheap, %10d total, %10.2fs", infoString,
          heap, nonHeap, heap + nonHeap, (double) elapsed / 1000));
    }
    String dumpFile = System.getProperty(PROPERTY_DUMP_HEAP);
    if (dumpFile != null) {
      String procName = ManagementFactory.getRuntimeMXBean().getName();
      dumpFile = info + "-" + procName + dumpFile;
      new File(dumpFile).delete();
      try {
        Class<?> beanClass = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
        Object bean = ManagementFactory.newPlatformMXBeanProxy(
            ManagementFactory.getPlatformMBeanServer(),
            "com.sun.management:type=HotSpotDiagnostic", beanClass);
        Method dumpHeapMethod = beanClass.getDeclaredMethod("dumpHeap",
            String.class, Boolean.TYPE);
        dumpHeapMethod.invoke(bean, dumpFile, true);
        System.out.println("(" + info + ") dumped heap into: " + dumpFile);
      } catch (Throwable e) {
        System.err.println("Unable to dump heap");
        e.printStackTrace();
      }
    }
    // Reset for next call
    startTime = System.currentTimeMillis();
  }
}
