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

import com.google.gwt.ant.taskdefs.SvnInfo.Info;

import junit.framework.TestCase;

import java.io.File;

/**
 * Tests for {@link SvnInfo}.
 */
public class SvnInfoTest extends TestCase {

  /**
   * The current directory.
   */
  private static final File dir = new File(".");

  /**
   * A cached copy of 'git svn info' so we don't have to keep running it (makes
   * the tests run faster).
   */
  private static String gitSvnInfo = null;

  /**
   * Check that this is a valid git rev.
   */
  private static void assertIsValidGitRev(String rev) {
    assertNotNull(rev);
    assertEquals(rev, 40, rev.length());
    for (char ch : rev.toCharArray()) {
      assertTrue(isHexDigit(ch));
    }
  }

  private static String getGitSvnInfo() {
    if (gitSvnInfo == null) {
      gitSvnInfo = SvnInfo.getGitSvnInfo(dir);
    }
    return gitSvnInfo;
  }

  private static boolean isHexDigit(char ch) {
    return (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F')
        || (ch >= 'a' && ch <= 'f');
  }

  /**
   * Test that doesGitWorkingCopyMatchSvnRevision finishes.
   */
  public void testDoesGitWorkingCopyMatchSvnRevision() {
    if (SvnInfo.looksLikeGit(dir)) {
      Info info = SvnInfo.parseInfo(getGitSvnInfo());
      SvnInfo.doesGitWorkingCopyMatchSvnRevision(dir, info.revision);
    }
  }

  /**
   * Test that getGitRevForSvnRev returns a 40-character git hash.
   */
  public void testGetGitRevForSvnRev() {
    if (SvnInfo.looksLikeGit(dir)) {
      Info info = SvnInfo.parseInfo(getGitSvnInfo());
      String rev = SvnInfo.getGitRevForSvnRev(dir, info.revision);
      assertIsValidGitRev(rev);
    }
  }

  public void testGetGitStatus() {
    if (SvnInfo.looksLikeGit(dir)) {
      String status = SvnInfo.getGitStatus(dir);
      assertNotNull(status);
      assertTrue(!"".equals(status));
    }
  }

  public void testGetGitSvnInfo() {
    if (SvnInfo.looksLikeGit(dir)) {
      String info = getGitSvnInfo();
      assertNotNull(info);
      assertTrue(!"".equals(info));
    }
  }

  public void testGetGitSvnWorkingRev() {
    if (SvnInfo.looksLikeGit(dir)) {
      String rev = SvnInfo.getGitWorkingRev(dir);
      assertIsValidGitRev(rev);
    }
  }

  /**
   * If this is an svn working copy, just verify that "svn info" succeeds and
   * returns something.
   */
  public void testGetSvnInfo() {
    if (SvnInfo.looksLikeSvn(dir)) {
      String info = SvnInfo.getSvnInfo(dir);
      assertNotNull(info);
      assertTrue(!"".equals(info));
    }
  }

  /**
   * If this is an svn working copy, just verify that "svnversion" succeeds and
   * returns something.
   */
  public void testGetSvnVersion() {
    if (SvnInfo.looksLikeSvn(dir)) {
      String version = SvnInfo.getSvnVersion(dir);
      assertNotNull(version);
      assertTrue(!"".equals(version));
    }
  }

  /**
   * Test that the correct info is parsed out of a canned result.
   */
  public void testParseInfo() {
    String svnInfo = "Path: .\n" + "URL: http://example.com/svn/tags/w00t\n"
        + "Repository Root: http://example.com/svn\n"
        + "Repository UUID: 00000000-0000-0000-0000-000000000000\n"
        + "Revision: 9999\n" + "Node Kind: directory\n" + "Schedule: normal\n"
        + "Last Changed Author: foo@example.com\n" + "Last Changed Rev: 8888\n"
        + "Last Changed Date: 2009-01-01 00:00:00 +0000 (Thu, 01 Jan 2009)\n";
    Info info = SvnInfo.parseInfo(svnInfo);
    assertEquals("tags/w00t", info.branch);
    assertEquals("9999", info.revision);
  }

  /**
   * Test that trailing slashes are removed correctly.
   */
  public void testRemoveTrailingSlash() {
    assertEquals("http://example.com/svn",
        SvnInfo.removeTrailingSlash("http://example.com/svn"));
    assertEquals("http://example.com/svn",
        SvnInfo.removeTrailingSlash("http://example.com/svn/"));
  }
}
