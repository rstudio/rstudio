// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.silvercomet.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * An overlay type that structures a runner's data.
 *
 * @author knorton@google.com (Kelly Norton)
 */
public final class Runner extends JavaScriptObject {
  protected Runner() {
  }

  /**
   * The runner's age.
   */
  public native int age() /*-{
    return this[2];
  }-*/;

  /**
   * The number on the runner's bib (race number).
   */
  public native int bibNumber() /*-{
    return this[5];
  }-*/;

  /**
   * The runner's finishing time based on the RFID tag in their bib. -1 is
   * returned if the runner did not record a valid time with their bib.
   */
  public native int bibTime() /*-{
    return this[8];
  }-*/;

  /**
   * The city the runner entered in their registration.
   */
  public native String city() /*-{
    return this[4];
  }-*/;

  /**
   * Male or Female.
   */
  public native int gender() /*-{
    return this[3];
  }-*/;

  /**
   * The runner's finishing time based on the time from the gun fire to the time
   * they passed the finish line.
   */
  public native int gunTime() /*-{
    return this[6];
  }-*/;

  /**
   * The runner's full name.
   */
  public native String name() /*-{
    return this[1];
  }-*/;

  /**
   * The runner's pace in seconds / mile.
   */
  public native int pace() /*-{
    return this[7];
  }-*/;

  /**
   * The runner's finishing place (1 based).
   */
  public native int place() /*-{
    return this[0];
  }-*/;

  /**
   * Allows updating of the runner's place to accomodate recordering by
   * {@link #bibTime()} instead of {@link #gunTime()}.
   */
  public native void setPlace(int place) /*-{
    this[0] = place;
  }-*/;

  /**
   * Normalized finishing time. This returns the {@link #bibTime()} if it's
   * valid, {@link #gunTime()} otherwise.
   */
  public int time() {
    return bibTime() != -1 ? bibTime() : gunTime();
  }
}
