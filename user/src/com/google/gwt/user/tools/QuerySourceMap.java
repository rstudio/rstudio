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
package com.google.gwt.user.tools;

import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapConsumerFactory;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapping;
import com.google.gwt.dev.util.Util;

import java.io.File;

/**
 * Command-line utility for querying source maps.
 */
public class QuerySourceMap {

  public static void main(String[] args) throws Exception {
    String filename = args[0];
    int line = Integer.valueOf(args[1]);
    int col = Integer.valueOf(args[2]);

    SourceMapping consumer = SourceMapConsumerFactory.parse(Util.readFileAsString(new File(filename)));
    System.out.println(consumer.getMappingForLine(line, col));
  }
}
