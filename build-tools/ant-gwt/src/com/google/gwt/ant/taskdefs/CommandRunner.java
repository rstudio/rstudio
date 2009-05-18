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
package com.google.gwt.ant.taskdefs;

import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * Utility class to run external commands.
 */
class CommandRunner {

  /**
   * Returns the output of running a command as a string. Will fail if the
   * invoked process returns a non-zero status code and
   * <code>checkStatusCode</code> is <code>true</code>.
   */
  public static String getCommandOutput(boolean checkStatusCode, File workDir,
      String... cmd) {
    Process process = runCommandIgnoringErr(workDir, cmd);
    StringBuilder output = new StringBuilder();
    LineNumberReader lnr = new LineNumberReader(new InputStreamReader(
        process.getInputStream()));
    try {
      for (String line = lnr.readLine(); line != null; line = lnr.readLine()) {
        output.append(line);
        output.append('\n');
      }
      int statusCode = process.waitFor();
      if (checkStatusCode && statusCode != 0) {
        throw new BuildException("Non-zero status code result (" + statusCode
            + ") running command: " + makeCmdString(cmd));
      }
      return output.toString();
    } catch (IOException e) {
      throw new BuildException("Unable to read command: " + makeCmdString(cmd),
          e);
    } catch (InterruptedException e) {
      throw new BuildException("Interrupted waiting for command: "
          + makeCmdString(cmd), e);
    } finally {
      if (lnr != null) {
        try {
          lnr.close();
        } catch (IOException e) {
          // do nothing
        }
      }
    }
  }

  /**
   * Returns the output of running a command as a string. Will fail if the
   * invoked process returns a non-zero status code.
   */
  public static String getCommandOutput(File workDir, String... cmd) {
    return getCommandOutput(true, workDir, cmd);
  }

  /**
   * Runs the specified command and returns the {@link Process}. The caller
   * must handle both the output and error streams to avoid blocking the
   * underlying process.
   */
  public static Process runCommand(File workDir, String... cmd) {
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(workDir);
    try {
      return pb.start();
    } catch (IOException e) {
      throw new BuildException("Unable to launch command: "
          + makeCmdString(cmd), e);
    }
  }

  /**
   * Runs the specified command and returns the {@link Process}. The resulting
   * process's error stream will be continually drained in a daemon thread to
   * prevent the underlying process from blocking. The caller must handle both
   * output stream to avoid blocking the underlying process.
   */
  public static Process runCommandIgnoringErr(File workDir, String... cmd) {
    final Process process = runCommand(workDir, cmd);
    // Consume error output on another thread to avoid blocking.
    Thread errThread = new Thread(new Runnable() {
      public void run() {
        InputStream errorStream = process.getErrorStream();
        try {
          byte[] buf = new byte[8192];
          int read;
          do {
            read = errorStream.read(buf);
          } while (read >= 0);
        } catch (IOException e) {
        } finally {
          try {
            errorStream.close();
          } catch (IOException e) {
          }
        }
      }
    });
    errThread.setDaemon(true);
    errThread.start();
    return process;
  }

  /**
   * Turns a command array into a printable string.
   */
  static String makeCmdString(String... cmd) {
    StringBuilder sb = new StringBuilder();
    for (String arg : cmd) {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append(arg);
    }
    String cmdString = sb.toString();
    return cmdString;
  }
}
