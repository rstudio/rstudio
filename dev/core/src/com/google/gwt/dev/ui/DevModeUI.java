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
package com.google.gwt.dev.ui;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.ModuleHandle;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines the interaction between DevelopmentMode and the UI, so that
 * alternate UIs can be implemented.
 * <p>
 * Sequence of calls:
 * <ul>
 * <li>{@link #initialize(Type)}
 * <li>{@link #getTopLogger()}
 * <li>possibly {@link #getWebServerLogger(String, byte[])}
 * <li>{@link #setStartupUrls(Map)}
 * <li>{@link #moduleLoadComplete(boolean)} or
 * <li>zero or more {@link #getModuleLogger}
 * </ul>
 * {@link #setCallback(com.google.gwt.dev.ui.UiEvent.Type, UiCallback)} may be
 * interspersed among these calls after {@link #initialize(Type)} is called.
 */
public abstract class DevModeUI {

  /**
   * Map of callbacks.
   */
  private final Map<UiEvent.Type<? extends UiCallback>, UiCallback> callbacks
      = new HashMap<UiEvent.Type<? extends UiCallback>, UiCallback>();

  /**
   * A lazily-initialized console logger - see {@link #getConsoleLogger()}.
   */
  private PrintWriterTreeLogger consoleLogger = null;

  /**
   * Log level for all logging in this UI.
   */
  private Type logLevel;

  /**
   * Show that a module is loaded in the UI.
   *
   * <p>Note that the {@link CloseModuleEvent} should already have a callback
   * registered when this is called if needed -- the UI is not required to
   * change the UI if it is registered later.
   *
   * @param userAgent full user agent name
   * @param remoteSocket name of remote socket endpoint in host:port format
   * @param url URL of top-level window
   * @param tabKey stable browser tab identifier, or the empty string if no
   *     such identifier is available
   * @param moduleName the name of the module loaded
   * @param sessionKey a unique session key
   * @param agentTag short-form user agent identifier, suitable for use in
   *     a label for this connection
   * @param agentIcon icon to use for the user agent (fits inside 24x24) or
   *     null if unavailable
   * @param logLevel logging detail requested
   * @return a handle to the module
   */
  public abstract ModuleHandle getModuleLogger(String userAgent,
      String remoteSocket, String url, String tabKey, String moduleName,
      String sessionKey, String agentTag, byte[] agentIcon, Type logLevel);

  /**
   * Create a top-level logger for messages which are not associated with the
   * web server or any module.  Defaults to logging to stdout.
   *
   * @return TreeLogger instance to use
   */
  public TreeLogger getTopLogger() {
    return getConsoleLogger();
  }

  /**
   * Create the web server portion of the UI if not already created, and
   * return its TreeLogger instance.
   *
   * <p>Note that the {@link RestartServerEvent} should already have a callback
   * registered when this is called -- the UI is not required to change the
   * UI if it is registered later.
   *
   * @param serverName short name of the web server or null if only the icon
   *     should be used
   * @param serverIcon byte array containing an icon (fitting into 24x24) to
   *     use for the server, or null if only the name should be used
   * @return TreeLogger instance
   */
  public abstract TreeLogger getWebServerLogger(String serverName,
      byte[] serverIcon);

  /**
   * Initialize the UI - must be called exactly once and before any other method
   * on this class.
   *
   * <p>Subclasses should call super.initialize(logLevel).
   *
   * @param logLevel log level for all logging
   */
  public void initialize(Type logLevel) {
    this.logLevel = logLevel;
  }

  /**
   * Indicates that all modules have been loaded (loading the XML, not
   * completing onModuleLoad), and that URLs previously specified in
   * {@link #setStartupUrls(Map)} may be launched if successful.
   *
   * @param success true if all modules were successfully loaded
   */
  public void moduleLoadComplete(boolean success) {
    // do nothing by default
  }

  /**
   * Sets the callback for a given event type..
   *
   * @param <C> callback type
   * @param type UI event type token
   * @param callback event callback, or null to clear the callback
   */
  public final <C extends UiCallback> void setCallback(UiEvent.Type<C> type,
      C callback) {
    assert type != null;
    callbacks.put(type, callback);
  }

  /**
   * Set the URLs that should be available to start.
   *
   * @param urls map of URLs -- the key is the name supplied with -startupUrls,
   *     and the value is the mapped URL with all parameters included
   */
  public void setStartupUrls(Map<String, URL> urls) {
    // do nothing by default
  }

  /**
   * Show in the UI that the web server, identified by the logger returned from
   * {@link #getWebServerLogger(String, byte[])}, is operating in a secure
   * fashion.
   *
   * @param serverLogger
   */
  public void setWebServerSecure(TreeLogger serverLogger) {
    // do nothing by default
  }

  /**
   * Call callback for a given event.
   *
   * @param eventType type of event
   * @return the UiCallback for this event or null if none
   */
  @SuppressWarnings("unchecked")
  protected final <C extends UiCallback> C getCallback(
      UiEvent.Type<?> eventType) {
    return (C) callbacks.get(eventType);
  }

  /**
   * @return a console-based logger.
   */
  protected final TreeLogger getConsoleLogger() {
    if (consoleLogger == null) {
      consoleLogger = new PrintWriterTreeLogger();
      consoleLogger.setMaxDetail(getLogLevel());
    }
    return consoleLogger;
  }

  /**
   * @return the log level for all logging.
   */
  protected final Type getLogLevel() {
    return logLevel;
  }

  /**
   * Returns true if a callback has been registered for an event.
   *
   * @param eventType type of event
   * @return true if a callback has been registered for event
   */
  protected final boolean hasCallback(
      UiEvent.Type<? extends UiCallback> eventType) {
    return callbacks.get(eventType) != null;
  }
}
