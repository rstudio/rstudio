package com.google.gwt.dev.shell;

import junit.framework.TestCase;
import com.google.gwt.dev.shell.CheckForUpdates.GwtVersion;

public class CheckForUpdatesTest extends TestCase {

  /*
   * Test GwtVersion comparisons
   */
  public final void testVersionComparison() {
    GwtVersion v1 = new GwtVersion(null);
    assertEquals(0, v1.compareTo(v1));

    v1 = new GwtVersion("1");
    assertEquals(0, v1.compareTo(v1));
    GwtVersion v2 = new GwtVersion("2");
    assertTrue(v1.compareTo(v2) < 0);
    assertTrue(v2.compareTo(v1) > 0);

    v1 = new GwtVersion("1.2.3");
    v2 = new GwtVersion(null);
    assertTrue(v1.compareTo(v2) > 0);
    assertTrue(v2.compareTo(v1) < 0);

    v1 = new GwtVersion("1.2.3");
    v2 = new GwtVersion("2.0.0");
    assertTrue(v1.compareTo(v2) < 0);
    assertTrue(v2.compareTo(v1) > 0);

    v1 = new GwtVersion("1.2.99");
    v2 = new GwtVersion("2.0.0");
    assertTrue(v1.compareTo(v2) < 0);
    assertTrue(v2.compareTo(v1) > 0);

    v1 = new GwtVersion("1.2.99");
    v2 = new GwtVersion("1.3.0");
    assertTrue(v1.compareTo(v2) < 0);
    assertTrue(v2.compareTo(v1) > 0);

    v1 = new GwtVersion("001.002.099");
    v2 = new GwtVersion("1.2.99");
    assertEquals(0, v1.compareTo(v2));

    // TODO: more tests
//    assertFalse(CheckForUpdates.isServerVersionNewer("2", "1.2.3"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3", "2"));
//
//    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3", "2.3.4.5"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3.4", "2.3.4"));
//
//    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3", "1.2.3"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("1000.2000.3000",
//      "1000.2000.3000"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("001.002.003", "1.2.3"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3", "001.002.003"));
//
//    assertTrue(CheckForUpdates.isServerVersionNewer("1.2.3", "2.2.3"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("2.2.3", "1.2.3"));
//
//    assertTrue(CheckForUpdates.isServerVersionNewer("1.2.3", "1.3.3"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("1.3.3", "1.2.3"));
//
//    assertTrue(CheckForUpdates.isServerVersionNewer("1.2.3", "1.2.4"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.4", "1.2.3"));
//
//    assertTrue(CheckForUpdates.isServerVersionNewer("1000.2000.3000",
//      "1000.2000.4000"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("1000.2000.4000",
//      "1000.2000.3000"));
//
//    assertTrue(CheckForUpdates.isServerVersionNewer("1000.2000.3000",
//      "1000.2000.3001"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("1000.2000.3001",
//      "1000.2000.3000"));
//
//    assertTrue(CheckForUpdates.isServerVersionNewer("0.2.3", "1.1.3"));
//    assertFalse(CheckForUpdates.isServerVersionNewer("1.1.3", "0.2.3"));
  }
}
