/*
 * Copyright 2011 Google Inc.
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

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Tests {@link DiskCacheToken}.
 */
public class DiskCacheTokenTest extends TestCase {
  private final DiskCache diskCache = new DiskCache();

  public void testSerialization() {
    byte[] buf = new byte[]{1, 5, 9, 7, 3, 4, 2};
    long t = diskCache.writeByteArray(buf);
    DiskCacheToken token1 = new DiskCacheToken(diskCache, t);

    long t2 = diskCache.writeObject(token1);
    DiskCacheToken token2 = new DiskCacheToken(diskCache, t2);

    DiskCacheToken token1again = token2.readObject(DiskCacheToken.class);
    byte[] buf2 = token1again.readByteArray();
    assertTrue("Values were not equals", Arrays.equals(buf, buf2));
  }
}
