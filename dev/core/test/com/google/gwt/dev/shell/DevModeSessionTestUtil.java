// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.gwt.dev.shell;

/**
 * Provides public method for convenience in unit tests. Allows access of
 * package-private member
 * {@link DevModeSession#setSessionForCurrentThread(DevModeSession)}.
 * 
 * @author jhumphries@google.com (Joshua Humphries)
 */
public class DevModeSessionTestUtil {
  public static DevModeSession createSession(String moduleName, String userAgent,
      boolean setForCurrentThread) {
    DevModeSession session = new DevModeSession(moduleName, userAgent);
    if (setForCurrentThread) {
      DevModeSession.setSessionForCurrentThread(session);
    }
    return session;
  }
}
