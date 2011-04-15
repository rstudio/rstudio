/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.media;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.user.client.MediaEventsSinkTest;
import com.google.gwt.media.client.AudioTest;
import com.google.gwt.media.client.VideoTest;

import junit.framework.Test;

/**
 * Tests of Media.
 */
public class MediaSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test suite for Media GWTTestCases");
    
    suite.addTestSuite(MediaEventsSinkTest.class);
    suite.addTestSuite(AudioTest.class);
    suite.addTestSuite(VideoTest.class);

    return suite;
  }

  private MediaSuite() {
  }
}
