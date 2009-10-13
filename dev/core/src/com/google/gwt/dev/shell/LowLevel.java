/*
 * Copyright 2006 Google Inc.
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

import java.lang.reflect.Field;

/**
 * Various cross-platform low-level helper methods.
 */
public class LowLevel {

  public static final String PACKAGE_PATH = LowLevel.class.getPackage().getName().replace(
      '.', '/').concat("/");

  private static boolean sInitialized = false;

  /**
   * Clobbers a field on an object to which we do not have access.
   */
  public static void clobberFieldObjectValue(Class<?> victimClass,
      Object victimObject, String fieldName, Object value) {
    Throwable rethrow = null;
    try {
      Field victimField = victimClass.getDeclaredField(fieldName);
      victimField.setAccessible(true);
      victimField.set(victimObject, value);
      return;
    } catch (IllegalArgumentException e) {
      rethrow = e;
    } catch (SecurityException e) {
      rethrow = e;
    } catch (IllegalAccessException e) {
      rethrow = e;
    } catch (NoSuchFieldException e) {
      rethrow = e;
    }
    throw new RuntimeException("Unable to clobber field '" + fieldName
        + "' from class " + victimClass.getName(), rethrow);
  }

  /**
   * Clobbers a field on an object to which we do not have access.
   */
  public static void clobberFieldObjectValue(Object victim, String fieldName,
      Object value) {
    if (victim != null) {
      clobberFieldObjectValue(victim.getClass(), victim, fieldName, value);
    } else {
      throw new NullPointerException("victim must not be null");
    }
  }

  /**
   * Deletes a global ref on the specified object, invalidating its handle.
   */
  public static void deleteGlobalRefInt(int globalRef) {
    _deleteGlobalRefInt(globalRef);
  }

  /**
   * Gets an environment variable. This is to replace the deprecated
   * {@link System#getenv(java.lang.String)}.
   */
  public static String getEnv(String key) {
    return _getEnv(key);
  }

  public static synchronized void init() {
    if (!sInitialized) {
      // TODO(jat): load native code for IE proxy handling?
      sInitialized = true;
    }
  }

  /**
   * Creates a global ref on the specified object, returning its int handle.
   */
  public static int newGlobalRefInt(Object o) {
    return _newGlobalRefInt(o);
  }

  /**
   * Converts a global ref handle from {@link #newGlobalRefInt(Object)} into an
   * Object. Use at your own risk.
   */
  public static Object objFromGlobalRefInt(int globalRef) {
    return _objFromGlobalRefInt(globalRef);
  }

  /**
   * Snatches a field from an object to which we do not have access.
   */
  @SuppressWarnings("unchecked")
  public static <T> T snatchFieldObjectValue(Class<?> victimClass,
      Object victimObject, String fieldName) {
    Throwable rethrow = null;
    try {
      Field victimField = victimClass.getDeclaredField(fieldName);
      victimField.setAccessible(true);
      return (T) victimField.get(victimObject);
    } catch (IllegalArgumentException e) {
      rethrow = e;
    } catch (SecurityException e) {
      rethrow = e;
    } catch (IllegalAccessException e) {
      rethrow = e;
    } catch (NoSuchFieldException e) {
      rethrow = e;
    }
    throw new RuntimeException("Unable to snatch field '" + fieldName
        + "' from class " + victimClass.getName(), rethrow);
  }

  /**
   * Snatches a field from an object to which we do not have access.
   */
  public static Object snatchFieldObjectValue(Object victim, String fieldName) {
    if (victim != null) {
      return snatchFieldObjectValue(victim.getClass(), victim, fieldName);
    } else {
      throw new NullPointerException("victim must not be null");
    }
  }

  // CHECKSTYLE_NAMING_OFF
  private static native void _deleteGlobalRefInt(int globalRef);

  private static native String _getEnv(String key);

  private static native int _newGlobalRefInt(Object o);

  private static native Object _objFromGlobalRefInt(int globalRef);

  // CHECKSTYLE_NAMING_ON

  /**
   * This class is not instantiable.
   */
  private LowLevel() {
  }

}
