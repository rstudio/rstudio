// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Vector;

public abstract class Handle {

  public static final String HANDLE_CLASS = "com.google.gwt.core.client.JavaScriptObject";

  /**
   * For a thread-safety check to make sure only one thread ever accesses it.
   */
  private static Thread theOnlyThreadAllowed;

  /**
   * A queue of Integers (IUnknown ptrs, really), ready to be released by the
   * main thread.
   */
  private static Vector toBeReleased = new Vector();
  private static Object toBeReleasedLock = new Object();

  private static Handle sImpl;
  
  protected Handle() {
    if (sImpl != null) {
      throw new RuntimeException("More than one Handle class!");
    }
    sImpl = this;
  }

  protected abstract void lockPtr(int ptr);
  protected abstract void unlockPtr(int ptr);
  
  public static Object createHandle(Class type, int ptr) {
    try {
      checkThread();
      Constructor ctor = type.getDeclaredConstructor(new Class[]{Integer.TYPE});
      ctor.setAccessible(true);

      Object handle = ctor.newInstance(new Object[]{new Integer(ptr)});
      sImpl.lockPtr(ptr);
      return handle;
    } catch (InstantiationException e) {
      throw new RuntimeException("Error creating handle", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Error creating handle", e);
    } catch (SecurityException e) {
      throw new RuntimeException("Error creating handle", e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Error creating handle", e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Error creating handle", e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Error creating handle", e);
    }
  }

  public static int getPtrFromHandle(Object handle) {
    try {
      checkThread();

      Class handleClass = handle.getClass();
      while (handleClass != null && !handleClass.getName().equals(HANDLE_CLASS)) {
        handleClass = handleClass.getSuperclass();
      }
      
      if (handleClass == null) {
        throw new RuntimeException("Error reading handle");
      }

      Field opaqueField = handleClass.getDeclaredField("opaque");
      opaqueField.setAccessible(true);
      Integer opaque = (Integer) opaqueField.get(handle);
      return opaque.intValue();
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Error reading handle", e);
    } catch (SecurityException e) {
      throw new RuntimeException("Error reading handle", e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Error reading handle", e);
    }
  }

  /**
   * The main thread should call this from time to time to release external COM
   * objects that Java is no longer referencing.
   */
  public static void releaseQueuedPtrs() {
    checkThread();
    Vector temp;
    synchronized (toBeReleasedLock) {
      temp = toBeReleased;
      toBeReleased = new Vector();
    }
    for (Iterator iter = temp.iterator(); iter.hasNext();) {
      Integer ptr = (Integer) iter.next();
      sImpl.unlockPtr(ptr.intValue());
    }
    temp.clear();
  }

  /**
   * Moves this ptr into a queue of COM objects that are ready to be released.
   */
  public static void enqueuePtr(int opaque) {
    // Add to the queue to be released by the main thread later.
    //
    Integer intOpaque = new Integer(opaque);
    synchronized (toBeReleasedLock) {
      toBeReleased.add(intOpaque);
    }
  }

  /**
   * Ensures that the current thread is actually the UI thread.
   */
  private static synchronized void checkThread() {
    if (theOnlyThreadAllowed == null) {
      theOnlyThreadAllowed = Thread.currentThread();
    } else if (theOnlyThreadAllowed != Thread.currentThread()) {
      throw new RuntimeException("This object has permanent thread affinity.");
    }
  }
}
