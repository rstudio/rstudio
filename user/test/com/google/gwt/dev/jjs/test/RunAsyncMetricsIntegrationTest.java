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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A simple test that the
 * {@link GWT#runAsync(com.google.gwt.core.client.RunAsyncCallback) runAsync}
 * lightweight metrics make it all the way out to the JavaScript LWM system. A
 * number of more detailed tests are in
 * {@link com.google.gwt.core.client.impl.AsyncFragmentLoaderTest}.
 */
public class RunAsyncMetricsIntegrationTest extends GWTTestCase {
  private static final class LightweightMetricsEvent extends JavaScriptObject {
    protected LightweightMetricsEvent() {
    }

    public native String getEvtGroup() /*-{
      return this.evtGroup;
    }-*/;

    public native int getFragment() /*-{
      return this.fragment;
    }-*/;

    public native int getMillis() /*-{
      return this.millis;
    }-*/;

    public native String getModuleName() /*-{
      return this.moduleName;
    }-*/;

    public native int getSize() /*-{
      return this.size;
    }-*/;

    public native String getSubSystem() /*-{
      return this.subSystem;
    }-*/;

    public native String getType() /*-{
      return this.type;
    }-*/;
  }

  private static class LightweightMetricsObserver {
    public final Queue<LightweightMetricsEvent> events = new LinkedList<LightweightMetricsEvent>();
    @SuppressWarnings("unused")
    private JavaScriptObject previousObserver;

    /**
     * Install this observer and start watching events.
     */
    public native void install() /*-{
      var self = this;
      this.@com.google.gwt.dev.jjs.test.RunAsyncMetricsIntegrationTest.LightweightMetricsObserver::previousObserver
        = $stats;
      $stats = function(evt) {
        self.@com.google.gwt.dev.jjs.test.RunAsyncMetricsIntegrationTest.LightweightMetricsObserver::recordEvent(Lcom/google/gwt/dev/jjs/test/RunAsyncMetricsIntegrationTest$LightweightMetricsEvent;)(evt);
      }
    }-*/;

    /**
     * No more to do; uninstall this observer.
     */
    public native void uninstall() /*-{
      $stats = this.@com.google.gwt.dev.jjs.test.RunAsyncMetricsIntegrationTest.LightweightMetricsObserver::previousObserver
    }-*/;

    @SuppressWarnings("unused")
    private void recordEvent(LightweightMetricsEvent event) {
      if (event.getSubSystem().equals("runAsync")) {
        events.add(event);
      }
    }
  }

  private static final int TIMEOUT = 10000;

  private final LightweightMetricsObserver lwmObserver = new LightweightMetricsObserver();

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.RunAsyncMetricsIntegrationTest";
  }

  @Override
  public void gwtSetUp() {
    lwmObserver.events.clear();
    lwmObserver.install();
  }

  @Override
  public void gwtTearDown() {
    lwmObserver.uninstall();
  }

  public void testMetricsSignalled() {
    if (!GWT.isScript()) {
      // There are no runAsync lightweight metrics in hosted mode
      return;
    }
    delayTestFinish(TIMEOUT);
    GWT.runAsync(new RunAsyncCallback() {
      public void onFailure(Throwable reason) {
        fail();
      }

      public void onSuccess() {
        DeferredCommand.addCommand(new Command() {

          public void execute() {
            checkMetrics();
            finishTest();
          }

        });
      }
    });
  }

  /**
   * This should be called after a runAsync has been called and completed. It
   * checks that all lightweight metrics have occurred and are well formatted.
   */
  private void checkMetrics() {
    int lastMillis;

    {
      LightweightMetricsEvent event = lwmObserver.events.remove();
      assertEquals(getJunitModuleName(), event.getModuleName());
      assertEquals("leftoversDownload", event.getEvtGroup());
      assertEquals("begin", event.getType());
      assertEquals(2, event.getFragment());
      assertTrue(event.getMillis() != 0);
      lastMillis = event.getMillis();
    }
    {
      LightweightMetricsEvent event = lwmObserver.events.remove();
      assertEquals(getJunitModuleName(), event.getModuleName());
      assertEquals("leftoversDownload", event.getEvtGroup());
      assertEquals("end", event.getType());
      assertEquals(2, event.getFragment());
      assertTrue(event.getMillis() >= lastMillis);
      lastMillis = event.getMillis();
    }
    {
      LightweightMetricsEvent event = lwmObserver.events.remove();
      assertEquals(getJunitModuleName(), event.getModuleName());
      assertEquals("download1", event.getEvtGroup());
      assertEquals("begin", event.getType());
      assertEquals(1, event.getFragment());
      assertTrue(event.getMillis() >= lastMillis);
      lastMillis = event.getMillis();
    }
    {
      LightweightMetricsEvent event = lwmObserver.events.remove();
      assertEquals(getJunitModuleName(), event.getModuleName());
      assertEquals("download1", event.getEvtGroup());
      assertEquals("end", event.getType());
      assertEquals(1, event.getFragment());
      assertTrue(event.getMillis() >= lastMillis);
      lastMillis = event.getMillis();
    }
    {
      LightweightMetricsEvent event = lwmObserver.events.remove();
      assertEquals(getJunitModuleName(), event.getModuleName());
      assertEquals("runCallbacks1", event.getEvtGroup());
      assertEquals("begin", event.getType());
      assertTrue(event.getMillis() >= lastMillis);
      lastMillis = event.getMillis();
    }
    {
      LightweightMetricsEvent event = lwmObserver.events.remove();
      assertEquals(getJunitModuleName(), event.getModuleName());
      assertEquals("runCallbacks1", event.getEvtGroup());
      assertEquals("end", event.getType());
      assertTrue(event.getMillis() >= lastMillis);
      lastMillis = event.getMillis();
    }

    assertTrue(lwmObserver.events.isEmpty());
  }

  private String getJunitModuleName() {
    return getModuleName() + ".JUnit";
  }
}
