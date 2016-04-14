/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.dev.util.arg;

import com.google.gwt.util.tools.ArgHandlerString;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Handles the -bindAddress command line flag.
 */
public class ArgHandlerBindAddress extends ArgHandlerString {

  private static final String BIND_ADDRESS_TAG = "-bindAddress";
  public static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";

  private final OptionBindAddress options;

  public ArgHandlerBindAddress(OptionBindAddress options) {
    this.options = options;
  }

  @Override
  public String[] getDefaultArgs() {
    return new String[]{BIND_ADDRESS_TAG, DEFAULT_BIND_ADDRESS};
  }

  @Override
  public String getPurpose() {
    return "Specifies the bind address for the code server and web server " + "(defaults to "
        + DEFAULT_BIND_ADDRESS + ")";
  }

  @Override
  public String getTag() {
    return BIND_ADDRESS_TAG;
  }

  @Override
  public String[] getTagArgs() {
    return new String[]{"host-name-or-address"};
  }

  @Override
  public boolean setString(String value) {
    try {
      InetAddress address = InetAddress.getByName(value);
      options.setBindAddress(value);
      if (address.isAnyLocalAddress()) {
        // replace a wildcard address with our machine's local address
        // this isn't fully accurate, as there is no guarantee we will get
        // the right one on a multihomed host
        options.setConnectAddress(InetAddress.getLocalHost().getHostAddress());
      } else {
        options.setConnectAddress(value);
      }
      return true;
    } catch (UnknownHostException e) {
      System.err.println("-bindAddress host \"" + value + "\" unknown");
      return false;
    }
  }
}
