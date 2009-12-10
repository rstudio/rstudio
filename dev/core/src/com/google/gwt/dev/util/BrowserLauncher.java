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
package com.google.gwt.dev.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Provides a platform and JDK-independent method of launching a browser
 * given a URI.
 * 
 * <p>Portions derived from public-domain code at
 * <pre>http://www.centerkey.com/java/browser/</pre>
 */
public class BrowserLauncher {

  /**
   * A browser launcher that uses JDK 1.6 Desktop.browse support.
   */
  private static class Jdk16Launcher extends ReflectiveLauncher {
    
    /**
     * Create a Jdk16Launcher if supported.
     * 
     * @throws UnsupportedOperationException if not supported
     */
    public Jdk16Launcher() throws UnsupportedOperationException {
      try {
        Class<?> desktopClass = Class.forName("java.awt.Desktop");
        browseMethod = desktopClass.getMethod("browse", URI.class);
        Method factory = desktopClass.getMethod("getDesktop");
        browseObject = factory.invoke(null);
        return;
      } catch (ClassNotFoundException e) {
        // not on JDK 1.6, try other methods
      } catch (NoSuchMethodException e) {
        // not on JDK 1.6, try other methods
      } catch (SecurityException e) {
        // ignore, try other methods
      } catch (IllegalArgumentException e) {
        // ignore, try other methods
      } catch (IllegalAccessException e) {
        // ignore, try other methods
      } catch (InvocationTargetException e) {
        // ignore, try other methods
      }
      throw new UnsupportedOperationException("no JDK 1.6 Desktop.browse");
    }

    @Override
    protected Object convertUrl(String url) throws URISyntaxException, MalformedURLException {
      return new URL(url).toURI();  
    }
  }

  private interface Launcher {
    void browse(String url) throws IOException, URISyntaxException;      
  }

  /**
   * Launch the default browser on Mac via FileManager openURL.
   */
  private static class MacLauncher extends ReflectiveLauncher {
    
    public MacLauncher() throws UnsupportedOperationException {
      Throwable caught = null;
      try {
        Class<?> fileManager = Class.forName("com.apple.eio.FileManager");
        browseMethod = fileManager.getMethod("openURL", String.class);
        browseObject = null;
        return;
      } catch (SecurityException e) {
        caught = e;
      } catch (ClassNotFoundException e) {
        caught = e;
      } catch (NoSuchMethodException e) {
        caught = e;
      }
      throw new UnsupportedOperationException("Can't get openURL", caught);
    }
  }

  /**
   * Interface for launching a URL in a browser, which uses reflection.
   * 
   * <p>Subclass must set browseObject and browseMethod appropriately.
   */
  private abstract static class ReflectiveLauncher implements Launcher {

    protected Object browseObject;
    protected Method browseMethod;

    public void browse(String url) throws IOException, URISyntaxException {
      Object arg = convertUrl(url);
      Throwable caught = null;
      try {
        browseMethod.invoke(browseObject, arg);
        return;
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof IOException) {
          throw (IOException) cause;
        }
        caught = e;
      } catch (IllegalAccessException e) {
        caught = e;
      }
      throw new RuntimeException("Unexpected exception", caught);
    }

    /**
     * Convert the URL into another form if required.  The default
     * implementation simply returns the unmodified string.
     * 
     * @param url URL in string form
     * @return the URL in the form needed for browseMethod
     * @throws URISyntaxException
     * @throws MalformedURLException 
     */
    protected Object convertUrl(String url) throws URISyntaxException,
        MalformedURLException {
      return url;
    }
  }

  /**
   * Launch a browser by searching for a browser executable on the path.
   */
  private static class UnixExecBrowserLauncher implements Launcher {
    
    private static final String[] browsers = {
      "firefox", "opera", "konqueror", "chrome", "chromium", "epiphany",
      "seamonkey", "mozilla", "netscape", "galeon", "kazehakase",
    };
    
    private String browserExecutable;

    /**
     * Creates a launcher by searching for a suitable browser executable.
     * Assumes the presence of the "which" command.
     * 
     * @throws UnsupportedOperationException if no suitable browser can be
     *           found.
     */
    public UnixExecBrowserLauncher() throws UnsupportedOperationException {
      for (String browser : browsers) {
        try {
          Process process = Runtime.getRuntime().exec(new String[] { "which",
              browser});
          if (process.waitFor() == 0) {
            browserExecutable = browser;
            return;
          }
        } catch (IOException e) {
          // ignore, try next one
        } catch (InterruptedException e) {
          // ignore, try next one
        }
      }
      throw new UnsupportedOperationException("no suitable browser found");
    }

    public void browse(String url) throws IOException {
      Runtime.getRuntime().exec(new String[] { browserExecutable, url });
      // TODO(jat): do we need to wait for it to exit and check exit status?
      // That would be best for Firefox, but bad for some of the other browsers.
    }
  }
  
  /**
   * Launch the default browser on Windows via the URL protocol handler.
   */
  private static class WindowsLauncher implements Launcher {

    public void browse(String url) throws IOException {
      Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
      // TODO(jat): do we need to wait for it to exit and check exit status?
    }
  }
  
  private static Launcher launcher;

  /**
   * Browse to a given URI.
   * 
   * @param url
   * @throws IOException
   * @throws URISyntaxException 
   */
  public static void browse(String url) throws IOException, URISyntaxException {
    if (launcher == null) {
      findLauncher();
    }
    launcher.browse(url);
  }

  /**
   * Main method so this can be run from the command line for testing.
   * 
   * @param args URL to launch
   * @throws URISyntaxException 
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException,
      URISyntaxException {
    if (args.length == 0) {
      System.err.println("Usage: BrowserLauncher url...");
      System.exit(1);
    }
    for (String url : args) {
      browse(url);
    }
  }
  
  /**
   * Initialize launcher to an appropriate one for the current platform/JDK.
   */
  private static void findLauncher() {
    try {
      launcher = new Jdk16Launcher();
      return;
    } catch (UnsupportedOperationException e) {
      // ignore and try other methods
    }
    String osName = System.getProperty("os.name");
    if (osName.startsWith("Mac OS")) {
      launcher = new MacLauncher();
    } else if (osName.startsWith("Windows")) {
      launcher = new WindowsLauncher();
    } else {
      launcher = new UnixExecBrowserLauncher();
      // let UnsupportedOperationException escape to caller
    }
  }
}
