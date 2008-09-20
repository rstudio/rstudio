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
package com.google.gwt.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A Svn interface task, because the initial solution of <exec> and
 * <propertyregex> is unhappy in ant 1.6.5, and while that's old, it's not
 * quite "too old" for us to care.
 */
public class SvnInfo extends Task {

  // URL line from svn info output, selecting the very last term of the URL as
  // the branch specifier
  private static final String URL_REGEX = "\\s*URL:\\s*https?://.*/([^/]*)\\s*";
  
  
  public SvnInfo() {
    super();
  }

  @Override
  public void execute() throws BuildException {
    String result;
  
    if (outprop == null) {
      throw new BuildException(
          "<svninfo> task requires an outputproperty attribute");
    }
    if (workdir == null) {
      workdir = ".";
    }
    File workDirFile = new File(workdir);
    if (!workDirFile.isDirectory()) {
      throw new BuildException(workdir + " is not a directory");
    }

    String branch = getSvnBranch(workDirFile);
    String revision = getSvnVersion(workDirFile);
    getProject().setNewProperty(outprop, branch + "@" + revision);
    if (fileprop != null) {
      getProject().setNewProperty(fileprop, branch + "-" 
          + revision.replaceAll(":", "-"));
    }
  }

  /**
   * Establishes the property containing the SVN output string, branch@rev.
   * @param propname  Name of a property
   */
  public void setOutputProperty(String propname) {
    outprop = propname;
  }

  /**
   * Establishes the property containing the SVN output string, branch@rev.
   * @param propname  Name of a property
   */
  public void setOutputFileProperty(String propname) {
    fileprop = propname;
  }

  /**
   * Establishes the directory used as the SVN workspace to fetch version
   * information about
   * @param srcdir  workspace directory name
   */
  public void setDirectory(String srcdir) {
    workdir = srcdir;
  }

  private String getSvnBranch(File workdir) {
    String branchName = null;

    LineNumberReader svnout = runCommand(workdir, "svn", "info");
    try {
      String line = svnout.readLine();
      
      Pattern urlRegex = Pattern.compile(URL_REGEX);
      while (line != null) {
        Matcher m = urlRegex.matcher(line);
        
        if (m.matches()) {
          branchName = m.group(1);
          if (branchName == null || "".equals(branchName)) {
            throw new BuildException("svn info didn't get branch from URL line " 
                + line);
          }
          break;
        }
        line = svnout.readLine();
      }
    } catch (IOException e) {
      throw new BuildException("<svninfo> cannot read svn info's output stream", 
          e);
    }
    return branchName;
  }

  private String getSvnVersion(File workdir) {
    String line = null;

    LineNumberReader svnout = runCommand(workdir, "svnversion");
    try {
      line = svnout.readLine();
    } catch (IOException e) {
      throw new BuildException("<svninfo> cannot read svnversion's output stream",
          e);
    }
    if (line == null || "".equals(line)) {
      throw new BuildException("svnversion didn't give any answer");
    }
    return line;
  }

  private LineNumberReader runCommand (File workdir, String... cmd) {
    String cmdString = "";
    for (String arg : cmd) {
      cmdString = cmdString + arg + " ";
    }
    cmdString = cmdString.substring(0, cmdString.length() - 1);
    
    ProcessBuilder svnPb = new ProcessBuilder(cmd);
    svnPb.directory(workdir);
    Process svnproc;
    try {
      svnproc = svnPb.start();
    } catch (IOException e) {
      throw new BuildException("cannot launch command " + cmdString, e);
    }
    
    LineNumberReader svnerr = 
      new LineNumberReader(new InputStreamReader(svnproc.getErrorStream()));
    try {
      String line = svnerr.readLine();
      String errorText = "";
      if (line != null) {
        while (line != null) {
          errorText = errorText + "  " + line + "\n";
          line = svnerr.readLine();
        }
        throw new BuildException(cmdString + " returned error output:\n" 
            + errorText);
      }
    } catch (IOException e) {
      throw new BuildException("cannot read error stream from " + cmdString, 
          e);
    }
    
    LineNumberReader svnout = 
      new LineNumberReader(new InputStreamReader(svnproc.getInputStream()));    
    return svnout;
  }
  
  private String fileprop;
  private String outprop;
  private String workdir;
}
