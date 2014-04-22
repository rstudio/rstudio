/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.url;

import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * A factory that constructs URL Stream Handlers and extends Jar protocol handling by making it
 * possible to close all InputStreams for a given Zip file.
 * <p>
 * This closing functionality makes it possible to run untrusted plugin code (for example
 * Generators) and still guarantee that there will be no accidental attempts to read from old and
 * deleted Zip files using stale InputStreams.
 */
public class CloseableJarHandlerFactory implements URLStreamHandlerFactory {

  /**
   * Takes over the URL connection opening system so that Zip file connections can be gathered and
   * then closed before Zip files are replaced on disk. This avoids a JVM crash on Linux that can
   * occur if a third party plugin (ie. Generator) fails to close its InputStreams.
   */
  private static CloseableJarHandlerFactory closeableJarHandlerFactory;

  /**
   * Closes all InputStreams that were created by the URL system that point at the given Jar file.
   */
  public static void closeStreams(String jarFilePath) throws IOException {
    closeableJarHandlerFactory.closeableJarHandler.closeStreams(jarFilePath);
  }

  /**
   * Ensures that the standard UrlStreamHandlerFactory has been replaced.
   */
  public static synchronized void installOverride() {
    if (closeableJarHandlerFactory == null) {
      closeableJarHandlerFactory = new CloseableJarHandlerFactory();
      URL.setURLStreamHandlerFactory(closeableJarHandlerFactory);
    }
  }

  private static URLStreamHandler createHandlerForProtocol(String protocol) {
    String className = String.format("sun.net.www.protocol.%s.Handler", protocol);
    try {
      return (URLStreamHandler) Class.forName(className).newInstance();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Couldn't find handler for protocol: " + protocol);
    } catch (InstantiationException e) {
      throw new RuntimeException("Handler isn't not instantiable for protocol: " + protocol);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Handler isn't not accessible for protocol: " + protocol);
    }
  }

  private CloseableJarHandler closeableJarHandler = new CloseableJarHandler();

  private CloseableJarHandlerFactory() {
    // Force access through the public static functions.
  }

  @Override
  public URLStreamHandler createURLStreamHandler(String protocol) {
    if (protocol.equals("jar")) {
      // Override the url stream handler for Jar files.
      return closeableJarHandler;
    } else {
      // Regular url stream handling for other protocols.
      return createHandlerForProtocol(protocol);
    }
  }
}
