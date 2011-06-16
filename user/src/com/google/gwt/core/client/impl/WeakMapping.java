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
package com.google.gwt.core.client.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class associating a (String, Object) map with arbitrary source objects
 * (except for Strings). This implementation is used in Development Mode.
 */
public class WeakMapping {

  /*
   * This implementation is used in Development Mode only. It uses a HashMap to
   * associate the (key, value) maps with source object instances. The object
   * instances are wrapped in IdentityWeakReference objects in order to both
   * allow the underlying objects to be garbage-collected and to apply
   * IdentityHashMap semantics so that distinct objects that happen to compare
   * as equals() still get to have distinct maps associated with them.
   */

  /**
   * A WeakReference implementing equals() and hashCode(). The hash code of the
   * reference is permanently set to the identity hash code of the referent at
   * construction time.
   */
  static class IdentityWeakReference extends WeakReference<Object> {

    /**
     * The identity hash code of the referent, cached during construction.
     */
    private final int hashCode;

    public IdentityWeakReference(Object referent, ReferenceQueue<Object> queue) {
      super(referent, queue);
      hashCode = System.identityHashCode(referent);
    }

    @Override
    public boolean equals(Object other) {
      /*
       * Identical objects are always equal.
       */
      if (this == other) {
        return true;
      }

      /*
       * We can only be equal to another IdentityWeakReference.
       */
      if (!(other instanceof IdentityWeakReference)) {
        return false;
      }

      /*
       * Check equality of the underlying referents. If either referent is no
       * longer present, equals() will return false (note that the case of
       * identical IdentityWeakReference objects has already been defined to
       * return true above).
       */
      Object referent = get();
      if (referent == null) {
        return false;
      }
      return referent == ((IdentityWeakReference) other).get();
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }

  private static class ManagedWeakReference<T> extends WeakReference<T> {
    public ManagedWeakReference(T object) {
      super(object);
    }
  }

  /**
   * Provides synchronization around {@link #queue} so at most one thread is
   * cleaning at any given time.
   */
  private static final Lock cleanupLock = new ReentrantLock();

  /**
   * A Map from Objects to <String,Object> maps. Hashing is based on object
   * identity. Weak references are used to allow otherwise unreferenced Objects
   * to be garbage collected.
   */
  private static final ConcurrentHashMap<IdentityWeakReference, Map<String, Object>> map =
      new ConcurrentHashMap<IdentityWeakReference, Map<String, Object>>();

  /**
   * A ReferenceQueue used to clean up the map as its keys are
   * garbage-collected.
   */
  private static ReferenceQueue<Object> queue = new ReferenceQueue<Object>();

  /**
   * Returns the Object associated with the given key in the (key, value)
   * mapping associated with the given Object instance.
   * 
   * @param instance the source Object.
   * @param key a String key.
   * @return an Object associated with that key on the given instance, or null.
   */
  public static Object get(Object instance, String key) {
    cleanup();
    Object ref = new IdentityWeakReference(instance, queue);
    Map<String, Object> m = map.get(ref);
    if (m == null) {
      return null;
    }
    Object toReturn = m.get(key);
    if (toReturn instanceof ManagedWeakReference) {
      toReturn = ((ManagedWeakReference<?>) toReturn).get();
    }
    return toReturn;
  }

  /**
   * Associates a value with a given key in the (key, value) mapping associated
   * with the given Object instance. Note that the key space is module-wide, so
   * some care should be taken to choose sufficiently unique identifiers.
   * 
   * <p>
   * Due to restrictions of the Production Mode implementation, the instance
   * argument must not be a String.
   * 
   * @param instance the source Object, which must not be a String.
   * @param key a String key.
   * @param value the Object to associate with the key on the given source
   *          Object.
   * @throws IllegalArgumentException if instance is a String.
   */
  public static void set(Object instance, String key, Object value) {
    cleanup();
    if (instance instanceof String) {
      throw new IllegalArgumentException("Cannot use Strings with WeakMapping");
    }

    IdentityWeakReference ref = new IdentityWeakReference(instance, queue);
    Map<String, Object> m = map.get(ref);
    if (m == null) {
      m = new ConcurrentHashMap<String, Object>();
      map.putIfAbsent(ref, m);
      m = map.get(ref);
    }
    if (value == null) {
      m.remove(key);
    } else {
      m.put(key, value);
    }
  }

  /**
   * Like {@link #set(Object, String, Object)}, but doesn't guarantee that
   * {@code value} can be retrieved.
   */
  public static void setWeak(Object instance, String key, Object value) {
    set(instance, key, new ManagedWeakReference<Object>(value));
  }

  /**
   * Remove garbage-collected keys from the map. The (key, value) maps
   * associated with those keys will then become unreferenced themselves and
   * will be eligible for future garbage collection.
   */
  private static void cleanup() {
    if (cleanupLock.tryLock()) {
      Reference<? extends Object> ref;
      while ((ref = queue.poll()) != null) {
        /**
         * Note that we can still remove ref from the map even though its
         * referent has been nulled out since we only need == equality to do so.
         */
        map.remove(ref);
      }
      cleanupLock.unlock();
    }
  }
}
