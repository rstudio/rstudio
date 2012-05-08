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
package com.google.gwt.core.server;

import com.google.gwt.core.server.ServerGwtBridge.ClassInstantiatorBase;
import com.google.gwt.core.server.ServerGwtBridge.Properties;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.shared.Localizable;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * Test for {@link ServerGwtBridge}.
 */
public class ServerGwtBridgeTest extends TestCase {

  /**
   * Test interface to automatically find implementation classes.
   */
  public interface Bar {
  }

  /**
   * Test implementation for {@link Bar}.
   */
  public static class BarImpl implements Bar {
  }

  /**
   * Test interface for no implementations.
   */
  public interface Boo {
  }

  /**
   * Test interface for localizations.
   */
  public interface Foo extends Localizable {
    String locale();
  }

  /**
   * Test implementation class for default localization of {@link Foo}.
   */
  public static class Foo_ implements Foo {
    @Override
    public String locale() {
      return "default";
    }
  }

  /**
   * Test implementation class for en localization of {@link Foo}.
   */
  public static class Foo_en implements Foo {
    @Override
    public String locale() {
      return "en";
    }
  }

  /**
   * Test implementation class for en_US localization of {@link Foo}.
   */
  public static class Foo_en_US implements Foo {
    @Override
    public String locale() {
      return "en_US";
    }
  }

  private final ServerGwtBridge bridge = ServerGwtBridge.getInstance();
  private final String de = "de";
  private final String defaultLocale = "default";
  private final String en = "en";
  private final String en_GB = "en_GB";
  private final String en_US = "en_US";
  private final String en_US_POSIX = "en_US_POSIX";

  public void testBazDe() {
    bridge.setGlobalProperty("locale", de);
    Baz foo = bridge.create(Baz.class);
    assertEquals("default", foo.locale());
  }

  public void testBazDefault() {
    bridge.setGlobalProperty("locale", defaultLocale);
    Baz foo = bridge.create(Baz.class);
    assertEquals("default", foo.locale());
  }

  public void testBazEn() {
    bridge.setGlobalProperty("locale", en);
    Baz foo = bridge.create(Baz.class);
    assertEquals("en", foo.locale());
  }

  public void testBazEnGb() {
    bridge.setGlobalProperty("locale", en_GB);
    Baz foo = bridge.create(Baz.class);
    assertEquals("en", foo.locale());
  }

  public void testBazEnUs() {
    bridge.setGlobalProperty("locale", en_US);
    Baz foo = bridge.create(Baz.class);
    assertEquals("en_US", foo.locale());
  }

  public void testBazEnUsPosix() {
    bridge.setGlobalProperty("locale", en_US_POSIX);
    Baz foo = bridge.create(Baz.class);
    assertEquals("en_US", foo.locale());
  }

  public void testFooDe() {
    bridge.setGlobalProperty("locale", de);
    Foo foo = bridge.create(Foo.class);
    assertEquals("default", foo.locale());
  }

  public void testFooDefault() {
    bridge.setGlobalProperty("locale", defaultLocale);
    Foo foo = bridge.create(Foo.class);
    assertEquals("default", foo.locale());
  }

  public void testFooEn() {
    bridge.setGlobalProperty("locale", en);
    Foo foo = bridge.create(Foo.class);
    assertEquals("en", foo.locale());
  }

  public void testFooEnGb() {
    bridge.setGlobalProperty("locale", en_GB);
    Foo foo = bridge.create(Foo.class);
    assertEquals("en", foo.locale());
  }

  public void testFooEnUs() {
    bridge.setGlobalProperty("locale", en_US);
    Foo foo = bridge.create(Foo.class);
    assertEquals("en_US", foo.locale());
  }

  public void testFooEnUsPosix() {
    bridge.setGlobalProperty("locale", en_US_POSIX);
    Foo foo = bridge.create(Foo.class);
    assertEquals("en_US", foo.locale());
  }

  public void testLogging() throws IOException {
    // setup the logger
    Logger logger = Logger.getLogger(ServerGwtBridge.class.getName());
    logger.setLevel(Level.INFO);
    logger.setUseParentHandlers(false);
    for (Handler handler : logger.getHandlers()) {
      logger.removeHandler(handler);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamHandler handler = new StreamHandler(baos, new Formatter() {
      private String lineSeparator = System.getProperty("line.separator", "\n");

      @Override
      public synchronized String format(LogRecord record) {
        StringBuffer buf = new StringBuffer();
        String msg = formatMessage(record);
        buf.append(record.getLevel().getName()).append(": ").append(msg).append(lineSeparator);
        if (record.getThrown() != null) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          record.getThrown().printStackTrace(pw);
          pw.close();
          buf.append(sw.toString());
        }
        return buf.toString();
      }
    });
    logger.addHandler(handler);

    // log some things
    GWT.log("msg1");
    Throwable t = new RuntimeException("foo");
    t.fillInStackTrace();
    GWT.log("msg2", t);
    handler.flush();

    // check what we logged
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new ByteArrayInputStream(baos.toByteArray())));
    assertEquals("INFO: msg1", reader.readLine());
    assertEquals("INFO: msg2", reader.readLine());
    assertEquals(RuntimeException.class.getName() + ": foo", reader.readLine());
    assertTrue(reader.readLine().startsWith("\tat " + ServerGwtBridgeTest.class.getName()));
  }

  /**
   * Check that when there are multiple instantiators for a given class, the
   * most recently registered one has priority.
   */
  public void testLastOverrides() {
    ServerGwtBridge thisBridge = new ServerGwtBridge();
    thisBridge.register(Object.class, new ClassInstantiatorBase() {
      @SuppressWarnings("unchecked")
      @Override
      public <T> T create(Class<?> baseClass, Properties properties) {
        return (T) tryCreate(BarImpl.class);
      }
    });
    Object obj = thisBridge.create(Boo.class);
    assertNotNull(obj);
    assertTrue(obj instanceof BarImpl);
  }

  public void testObjectClass() {
    bridge.setGlobalProperty("locale", defaultLocale);
    Bar bar = bridge.create(BarImpl.class);
    assertNotNull(bar);
    assertTrue(bar instanceof BarImpl);
  }

  public void testObjectInterface() {
    bridge.setGlobalProperty("locale", defaultLocale);
    Bar bar = bridge.create(BarImpl.class);
    assertNotNull(bar);
    assertTrue(bar instanceof BarImpl);
  }

  public void testObjectInterfaceNoImpl() {
    bridge.setGlobalProperty("locale", defaultLocale);
    try {
      @SuppressWarnings("unused")
      Boo boo = bridge.create(Boo.class);
      fail("expected exception");
    } catch (RuntimeException expected) {
    }
  }
}
