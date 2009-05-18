/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.junit.remote;

import com.google.gwt.util.tools.ArgHandler;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * Used to process arguments and start up instances of BrowserManagerServer.
 * Some of this logic used to be in BrowserManagerServer.main() and was moved
 * here so we could inherit argument parsing from the ToolBase class.
 */
class BrowserManagerServerLauncher extends ToolBase {

  private final class ArgHandlerPort extends ArgHandlerString {

    @Override
    public String getPurpose() {
      return "Controls the port for the RMI invocation (defaults to "
          + Registry.REGISTRY_PORT + ")";
    }

    @Override
    public String getTag() {
      return "-port";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"port-number"};
    }

    @Override
    public boolean isRequired() {
      return false;
    }

    @Override
    public boolean setString(String value) {
      try {
        portArg = Integer.parseInt(value);
      } catch (NumberFormatException e) {
        logger.severe("The -port argument must be an integer value.  Got: "
            + value);
        return false;
      }
      return true;
    }
  }

  /**
   * Handles the list of registration ids / machine names passed on the command
   * line.
   */
  private class ArgHandlerRegistration extends ArgHandler {
    @Override
    public String getPurpose() {
      return "Specify two arguments: a registration id used for the "
          + "RMI call and the browser launch command";
    }

    @Override
    public String getTag() {
      return null;
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"registration-id", "path-to-browser-executable"};
    }

    @Override
    public int handle(String[] args, int startIndex) {
      // Consume 2 arguments
      if (args.length >= startIndex + 2) {
        BMSEntry entry = new BMSEntry(args[startIndex], args[startIndex + 1]);
        bmsList.add(entry);
        return 1;
      }
      return -1;
    }

    @Override
    public boolean isRequired() {
      return true;
    }
  }

  private static class BMSEntry {
    final String browserPath;
    final String registrationKey;

    BMSEntry(String registrationKeyIn, String browserPathIn) {
      registrationKey = registrationKeyIn;
      browserPath = browserPathIn;
    }
  }

  private static final Logger logger = Logger.getLogger(BrowserManagerServerLauncher.class.getName());

  private static final String USAGE = ""
      + "Manages local browser windows for a remote client using RMI.\n" + "\n"
      + "Pass in an even number of args, at least 2. The first argument\n"
      + "is a short registration name, and the second argument is the\n"
      + "executable to run when that name is used; for example,\n" + "\n"
      + "\tie6 \"C:\\Program Files\\Internet Explorer\\IEXPLORE.EXE\"\n" + "\n"
      + "would register Internet Explorer to \"rmi://localhost/ie6\".\n"
      + "The third and fourth arguments make another pair, and so on.\n";

  private List<BMSEntry> bmsList = new ArrayList<BMSEntry>();
  private int portArg = Registry.REGISTRY_PORT;
  private boolean serializeArg = false;

  /**
   * Creates a new BrowserServerLauncher and registers argument handling
   * instances.
   */
  BrowserManagerServerLauncher() {
    registerHandler(new ArgHandlerPort());
    registerHandler(new ArgHandlerRegistration());
    registerHandler(new ArgHandlerFlag() {

      @Override
      public String getPurpose() {
        return "Queue up requests to a single server so that only a single "
            + "test runs at a time (Usefule for a simple Firefox setup.)\n";
      }

      @Override
      public String getTag() {
        return "-serialize";
      }

      @Override
      public boolean setFlag() {
        serializeArg = true;
        return true;
      }

    });
  }

  public boolean doProcessArgs(String[] args) {
    if (args.length == 0) {
      System.err.println(USAGE);
      return false;
    }
    return processArgs(args);
  }

  /**
   * This method should be invoked after argument parsing completes.
   */
  public void run() {
    Registry rmiRegistry = null;

    try {
      // Create an RMI registry so we don't need an external process.
      // Uses the default RMI port if no port is specified with the -port arg.
      rmiRegistry = LocateRegistry.createRegistry(portArg);
    } catch (RemoteException e) {
      logger.log(Level.SEVERE, "Couldn't bind RMI Registry to port " + portArg,
          e);
      System.exit(1);
    }

    logger.log(Level.ALL, "RMI registry ready on port " + portArg + ".");

    // Startup each of the registered servers on this machine.
    for (BMSEntry entry : bmsList) {
      BrowserManagerServer server = null;
      try {
        server = new BrowserManagerServer(entry.browserPath, serializeArg);
      } catch (RemoteException re) {
        logger.log(Level.SEVERE, entry.registrationKey
            + ": Error starting new BrowserManagerServer.", re);
        System.exit(2);
      }

      try {
        rmiRegistry.rebind(entry.registrationKey, server);
      } catch (RemoteException re) {
        logger.log(Level.SEVERE, entry.registrationKey + " server: " + server
            + " port: " + portArg + " Error on rebind to "
            + entry.registrationKey, re);
        System.exit(3);
      }
      logger.log(Level.INFO, "Server: " + entry.registrationKey
          + " started and awaiting connections.");
    }

    logger.log(Level.INFO, "All servers started.");
  }

  @Override
  protected String getDescription() {
    return USAGE;
  }

}
