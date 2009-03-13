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
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Svn interface task, because the initial solution of <exec> and
 * <propertyregex> is unhappy in ant 1.6.5, and while that's old, it's not quite
 * "too old" for us to care.
 */
public class SvnInfo extends Task {

  /**
   * Structured svn info.
   */
  static class Info {
    /**
     * The relative path of this svn working copy within the root of the remote
     * repository. That is, if the root is "http://example.com/svn" and the
     * working copy URL is "http://example.com/svn/tags/w00t", this will be set
     * to "tags/w00t".
     */
    public final String branch;

    /**
     * The revision of this working copy. Initially set to the value parsed from
     * "svn info" given a more detailed value via "svnversion".
     */
    public String revision;

    public Info(String branch, String revision) {
      this.branch = branch;
      this.revision = revision;
    }
  }

  /**
   * A regex that matches a URL.
   */
  static final String URL_REGEX = "\\w+://\\S*";

  /**
   * A pattern that matches the URL line in svn info output.
   */
  private static final Pattern BRANCH_PATTERN = Pattern.compile("\\s*URL:\\s*("
      + URL_REGEX + ")\\s*");

  /**
   * A pattern that matches the Revision line in svn info output.
   */
  private static final Pattern REVISION_PATTERN = Pattern.compile("\\s*Revision:\\s*(\\d+)\\s*");

  /**
   * A pattern that matches the Repository Root line in svn info output.
   */
  private static final Pattern ROOT_PATTERN = Pattern.compile("\\s*Repository Root:\\s*("
      + URL_REGEX + ")\\s*");

  /**
   * Returns true if this git working copy matches the specified svn revision,
   * and also has no local modifications.
   */
  static boolean doesGitWorkingCopyMatchSvnRevision(File dir, String svnRevision) {
    String workingRev = getGitWorkingRev(dir);
    String targetRev = getGitRevForSvnRev(dir, svnRevision);
    if (!workingRev.equals(targetRev)) {
      return false;
    }
    String status = getGitStatus(dir);
    return status.contains("nothing to commit (working directory clean)");
  }

  /**
   * Returns the git commit number matching the specified svn revision.
   */
  static String getGitRevForSvnRev(File dir, String svnRevision) {
    String output = CommandRunner.getCommandOutput(dir, "git", "svn",
        "find-rev", "r" + svnRevision);
    output = output.trim();
    if (output.length() == 0) {
      throw new BuildException("git svn find-rev didn't give any answer");
    }
    return output;
  }

  /**
   * Runs "git status" and returns the result.
   */
  static String getGitStatus(File dir) {
    // git status returns 1 for a status code, so just don't check it.
    String output = CommandRunner.getCommandOutput(false, dir, "git", "status");
    if (output.length() == 0) {
      throw new BuildException("git status didn't give any answer");
    }
    return output;
  }

  /**
   * Runs "git svn info", returning the output as a string.
   */
  static String getGitSvnInfo(File dir) {
    String output = CommandRunner.getCommandOutput(dir, "git", "svn", "info");
    if (output.length() == 0) {
      throw new BuildException("git svn info didn't give any answer");
    }
    return output;
  }

  /**
   * Returns the current git commit number of the working copy.
   */
  static String getGitWorkingRev(File dir) {
    String output = CommandRunner.getCommandOutput(dir, "git", "rev-list",
        "--max-count=1", "HEAD");
    output = output.trim();
    if (output.length() == 0) {
      throw new BuildException("git rev-list didn't give any answer");
    }
    return output;
  }

  /**
   * Runs "svn info", returning the output as a string.
   */
  static String getSvnInfo(File dir) {
    String output = CommandRunner.getCommandOutput(dir, "svn", "info");
    if (output.length() == 0) {
      throw new BuildException("svn info didn't give any answer");
    }
    return output;
  }

  /**
   * Runs "svnversion", returning the output as a string.
   */
  static String getSvnVersion(File dir) {
    String output = CommandRunner.getCommandOutput(dir, "svnversion", ".");
    output = output.trim();
    if (output.length() == 0) {
      throw new BuildException("svnversion didn't give any answer");
    }
    return output;
  }

  static boolean looksLikeGit(File dir) {
    dir = dir.getAbsoluteFile();
    while (dir != null) {
      if (new File(dir, ".git").isDirectory()) {
        return true;
      }
      dir = dir.getParentFile();
    }
    return false;
  }

  /**
   * Returns <code>true</code> if the specified directory looks like an svn
   * working copy.
   */
  static boolean looksLikeSvn(File dir) {
    return new File(dir, ".svn").isDirectory();
  }

  /**
   * Parses the output of running "svn info".
   */
  static Info parseInfo(String svnInfo) {
    String rootUrl = null;
    String branchUrl = null;
    String revision = null;
    LineNumberReader lnr = new LineNumberReader(new StringReader(svnInfo));
    try {
      for (String line = lnr.readLine(); line != null; line = lnr.readLine()) {
        Matcher m;
        if ((m = ROOT_PATTERN.matcher(line)) != null && m.matches()) {
          rootUrl = m.group(1);
        } else if ((m = BRANCH_PATTERN.matcher(line)) != null && m.matches()) {
          branchUrl = m.group(1);
        } else if ((m = REVISION_PATTERN.matcher(line)) != null && m.matches()) {
          revision = m.group(1);
        }
      }
    } catch (IOException e) {
      throw new BuildException("Should never happen", e);
    }

    if (rootUrl == null) {
      throw new BuildException("svn info didn't get root URL: " + svnInfo);
    }
    if (branchUrl == null) {
      throw new BuildException("svn info didn't get branch URL: " + svnInfo);
    }
    if (revision == null) {
      throw new BuildException("svn info didn't get revision: " + svnInfo);
    }
    rootUrl = removeTrailingSlash(rootUrl);
    branchUrl = removeTrailingSlash(branchUrl);
    if (!branchUrl.startsWith(rootUrl)) {
      throw new BuildException("branch URL (" + branchUrl + ") and root URL ("
          + rootUrl + ") did not match");
    }

    String branch;
    if (branchUrl.length() == rootUrl.length()) {
      branch = "";
    } else {
      branch = branchUrl.substring(rootUrl.length() + 1);
    }
    return new Info(branch, revision);
  }

  static String removeTrailingSlash(String url) {
    if (url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }

  private String fileprop;

  private String outprop;

  private String workdir;

  public SvnInfo() {
    super();
  }

  @Override
  public void execute() throws BuildException {
    if (outprop == null) {
      throw new BuildException(
          "<svninfo> task requires an outputproperty attribute");
    }
    if (workdir == null) {
      workdir = getProject().getProperty("basedir");
    }
    File workDirFile = new File(workdir);
    if (!workDirFile.isDirectory()) {
      throw new BuildException(workdir + " is not a directory");
    }

    Info info;
    if (looksLikeSvn(workDirFile)) {
      info = parseInfo(getSvnInfo(workDirFile));

      // Use svnversion to get a more exact revision string.
      info.revision = getSvnVersion(workDirFile);
    } else if (looksLikeGit(workDirFile)) {
      info = parseInfo(getGitSvnInfo(workDirFile));

      // Add a 'M' tag if this working copy is not pristine.
      if (!doesGitWorkingCopyMatchSvnRevision(workDirFile, info.revision)) {
        info.revision += "M";
      }
    } else {
      info = new Info("unknown", "unknown");
    }

    getProject().setNewProperty(outprop, info.branch + "@" + info.revision);
    if (fileprop != null) {
      getProject().setNewProperty(fileprop,
          info.branch + "-" + info.revision.replaceAll(":", "-"));
    }
  }

  /**
   * Establishes the directory used as the SVN workspace to fetch version
   * information.
   * 
   * @param srcdir workspace directory name
   */
  public void setDirectory(String srcdir) {
    workdir = srcdir;
  }

  /**
   * Establishes the property containing the SVN output string, branch@rev.
   * 
   * @param propname Name of a property
   */
  public void setOutputFileProperty(String propname) {
    fileprop = propname;
  }

  /**
   * Establishes the property containing the SVN output string, branch@rev.
   * 
   * @param propname Name of a property
   */
  public void setOutputProperty(String propname) {
    outprop = propname;
  }
}
