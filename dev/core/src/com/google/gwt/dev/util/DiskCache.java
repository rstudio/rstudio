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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A nifty class that lets you squirrel away data on the file system. Write
 * once, read many times. Instance of this are thread-safe by way of internal
 * synchronization.
 * 
 * Note that in the current implementation, the backing temp file will get
 * arbitrarily large as you continue adding things to it. There is no internal
 * GC or compaction.
 */
public class DiskCache {
  /**
   * For future thought: if we used Object tokens instead of longs, we could
   * actually track references and do GC/compaction on the underlying file.
   * 
   * I considered using memory mapping, but I didn't see any obvious way to make
   * the map larger after the fact, which kind of defeats the infinite-append
   * design. At any rate, I measured the current performance of this design to
   * be so fast relative to what I'm using it for, I didn't pursue this further.
   */

  private static class Shutdown implements Runnable {
    public void run() {
      for (WeakReference<DiskCache> ref : shutdownList) {
        try {
          DiskCache diskCache = ref.get();
          if (diskCache != null) {
            diskCache.close();
          }
        } catch (Throwable e) {
        }
      }
    }
  }

  private static List<WeakReference<DiskCache>> shutdownList;

  private boolean atEnd = true;
  private RandomAccessFile file;

  public DiskCache() {
    try {
      File temp = File.createTempFile("gwt", "byte-cache");
      temp.deleteOnExit();
      file = new RandomAccessFile(temp, "rw");
      file.setLength(0);
      if (shutdownList == null) {
        shutdownList = new ArrayList<WeakReference<DiskCache>>();
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
      }
      shutdownList.add(new WeakReference<DiskCache>(this));
    } catch (IOException e) {
      throw new RuntimeException("Unable to initialize byte cache", e);
    }
  }

  /**
   * Read some bytes off disk.
   * 
   * @param token a handle previously returned from
   *          {@link #writeByteArray(byte[])}
   * @return the bytes that were written
   */
  public synchronized byte[] readByteArray(long token) {
    try {
      atEnd = false;
      file.seek(token);
      int length = file.readInt();
      byte[] result = new byte[length];
      file.readFully(result);
      return result;
    } catch (IOException e) {
      throw new RuntimeException("Unable to read from byte cache", e);
    }
  }

  public <T> T readObject(long token, Class<T> type) {
    try {
      byte[] bytes = readByteArray(token);
      ByteArrayInputStream in = new ByteArrayInputStream(bytes);
      return Util.readStreamAsObject(in, type);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
          "Unexpected exception deserializing from disk cache", e);
    } catch (IOException e) {
      throw new RuntimeException(
          "Unexpected exception deserializing from disk cache", e);
    }
  }

  /**
   * Read a String from disk.
   * 
   * @param token a handle previously returned from {@link #writeString(String)}
   * @return the String that was written
   */
  public String readString(long token) {
    return Util.toString(readByteArray(token));
  }

  /**
   * Write a byte array to disk.
   * 
   * @return a handle to retrieve it later
   */
  public synchronized long writeByteArray(byte[] bytes) {
    try {
      if (!atEnd) {
        file.seek(file.length());
      }
      long position = file.getFilePointer();
      file.writeInt(bytes.length);
      file.write(bytes);
      return position;
    } catch (IOException e) {
      throw new RuntimeException("Unable to write to byte cache", e);
    }
  }

  public long writeObject(Object object) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Util.writeObjectToStream(out, object);
      return writeByteArray(out.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException("Unexpected IOException on in-memory stream",
          e);
    }
  }

  /**
   * Write a String to disk.
   * 
   * @return a handle to retrieve it later
   */
  public long writeString(String str) {
    return writeByteArray(Util.getBytes(str));
  }

  /**
   * Reads bytes of data back from disk and writes them into the specified
   * output stream.
   */
  public synchronized void writeTo(long token, OutputStream out) {
    byte[] buf = Util.takeThreadLocalBuf();
    try {
      atEnd = false;
      file.seek(token);
      int length = file.readInt();
      int bufLen = buf.length;
      while (length > bufLen) {
        int read = file.read(buf, 0, bufLen);
        length -= read;
        out.write(buf, 0, read);
      }
      while (length > 0) {
        int read = file.read(buf, 0, length);
        length -= read;
        out.write(buf, 0, read);
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to read from byte cache", e);
    } finally {
      Util.releaseThreadLocalBuf(buf);
    }
  }

  @Override
  protected synchronized void finalize() throws Throwable {
    close();
  }

  private void close() throws Throwable {
    if (file != null) {
      file.setLength(0);
      file.close();
      file = null;
    }
  }
}