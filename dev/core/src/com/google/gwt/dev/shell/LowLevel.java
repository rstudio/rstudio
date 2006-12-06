// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import com.google.gwt.util.tools.Utility;

import org.eclipse.swt.graphics.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

/**
 * Various cross-platform low-level helper methods.
 */
public class LowLevel {

  /**
   * Not instantiable
   */
  private LowLevel() {
  }

  public static final String PACKAGE_PATH = LowLevel.class.getPackage()
    .getName().replace('.', '/').concat("/");

  private static boolean sInitialized = false;

  public static synchronized void init() {
    if (!sInitialized) {
      String libName = "gwt-ll";
      try {
        String installPath = Utility.getInstallPath();
        try {
          // try to make absolute
          installPath = new File(installPath).getCanonicalPath();
        } catch (IOException e) {
          // ignore problems, failures will occur when the libs try to load
        }
        System.load(installPath + '/' + System.mapLibraryName(libName));
      } catch (UnsatisfiedLinkError e) {
        StringBuffer sb = new StringBuffer();
        sb.append("Unable to load required native library '" + libName + "'");
        sb.append("\n\tYour GWT installation may be corrupt");
        throw new UnsatisfiedLinkError(sb.toString());
      }
      sInitialized = true;
    }
  }

  /**
   * Loads an image from the classpath.
   */
  public static Image loadImage(String name) {
    ClassLoader cl = LowLevel.class.getClassLoader();
    InputStream is = cl.getResourceAsStream(LowLevel.PACKAGE_PATH + name);
    if (is != null) {
      try {
        Image image = new Image(null, is);
        return image;
      } finally {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    } else {
      // Bad image.
      //
      return new Image(null, 1, 1);
    }
  }

  /**
   * Clobbers a field on an object to which we do not have access.
   */
  public static void clobberFieldObjectValue(Class victimClass,
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
    if (victim != null)
      clobberFieldObjectValue(victim.getClass(), victim, fieldName, value);
    else
      throw new NullPointerException("victim must not be null");
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
  public static Object snatchFieldObjectValue(Class victimClass,
      Object victimObject, String fieldName) {
    Throwable rethrow = null;
    try {
      Field victimField = victimClass.getDeclaredField(fieldName);
      victimField.setAccessible(true);
      return victimField.get(victimObject);
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
    if (victim != null)
      return snatchFieldObjectValue(victim.getClass(), victim, fieldName);
    else
      throw new NullPointerException("victim must not be null");
  }

  private static native void _deleteGlobalRefInt(int globalRef);

  private static native String _getEnv(String key);

  private static native int _newGlobalRefInt(Object o);

  private static native Object _objFromGlobalRefInt(int globalRef);

}
