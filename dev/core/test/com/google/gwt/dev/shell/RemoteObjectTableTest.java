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
package com.google.gwt.dev.shell;

import com.google.gwt.dev.shell.BrowserChannel.RemoteObjectRef;

import junit.framework.TestCase;

import java.util.Set;

/**
 * Test RemoteObjectTable.
 */
public class RemoteObjectTableTest extends TestCase {

  /**
   * Mock implementation of a RemoteObjectRef.
   */
  public class MockRemoteObjectRef implements RemoteObjectRef {

    public final int refId;

    public MockRemoteObjectRef(int refId) {
      this.refId = refId;
    }

    public int getRefid() {
      return refId;
    }
  }

  public void testFree() throws InterruptedException {
    RemoteObjectTable<MockRemoteObjectRef> table = new RemoteObjectTable<
        MockRemoteObjectRef>();
    MockRemoteObjectRef ref = new MockRemoteObjectRef(1);
    table.putRemoteObjectRef(1, ref);
    Set<Integer> freed = table.getRefIdsForCleanup();
    ensureGC();
    assertEquals(0, freed.size());
    ref = null;
    ensureGC();
    freed = table.getRefIdsForCleanup();
    assertEquals(1, freed.size());
  }

  /**
   * This method attempts to ensure that a GC has happened and any weak
   * references have been cleaned up.
   * 
   * @throws InterruptedException
   */
  private void ensureGC() throws InterruptedException {
    // TODO(jat): is this sufficient across all VMs?
    System.gc();
    Thread.sleep(10);
    System.gc();
  }
}
