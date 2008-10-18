package com.google.gwt.dev.shell;

import junit.framework.TestCase;

public class CheckForUpdatesTest extends TestCase {

  /*
   * Test method for
   * 'com.google.gwt.dev.shell.CheckForUpdates.isServerVersionNewer(String,
   * String)'
   */
  public final void testIsServerVersionNewer() {
    assertFalse(CheckForUpdates.isServerVersionNewer(null, null));

    assertFalse(CheckForUpdates.isServerVersionNewer("1", "2"));
    assertFalse(CheckForUpdates.isServerVersionNewer("2", "1"));

    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3", null));
    assertFalse(CheckForUpdates.isServerVersionNewer(null, "1.2.3"));

    assertFalse(CheckForUpdates.isServerVersionNewer("2", "1.2.3"));
    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3", "2"));

    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3", "2.3.4.5"));
    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3.4", "2.3.4"));

    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3", "1.2.3"));
    assertFalse(CheckForUpdates.isServerVersionNewer("1000.2000.3000",
      "1000.2000.3000"));
    assertFalse(CheckForUpdates.isServerVersionNewer("001.002.003", "1.2.3"));
    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.3", "001.002.003"));

    assertTrue(CheckForUpdates.isServerVersionNewer("1.2.3", "2.2.3"));
    assertFalse(CheckForUpdates.isServerVersionNewer("2.2.3", "1.2.3"));

    assertTrue(CheckForUpdates.isServerVersionNewer("1.2.3", "1.3.3"));
    assertFalse(CheckForUpdates.isServerVersionNewer("1.3.3", "1.2.3"));

    assertTrue(CheckForUpdates.isServerVersionNewer("1.2.3", "1.2.4"));
    assertFalse(CheckForUpdates.isServerVersionNewer("1.2.4", "1.2.3"));

    assertTrue(CheckForUpdates.isServerVersionNewer("1000.2000.3000",
      "1000.2000.4000"));
    assertFalse(CheckForUpdates.isServerVersionNewer("1000.2000.4000",
      "1000.2000.3000"));

    assertTrue(CheckForUpdates.isServerVersionNewer("1000.2000.3000",
      "1000.2000.3001"));
    assertFalse(CheckForUpdates.isServerVersionNewer("1000.2000.3001",
      "1000.2000.3000"));

    assertTrue(CheckForUpdates.isServerVersionNewer("0.2.3", "1.1.3"));
    assertFalse(CheckForUpdates.isServerVersionNewer("1.1.3", "0.2.3"));
  }

}
