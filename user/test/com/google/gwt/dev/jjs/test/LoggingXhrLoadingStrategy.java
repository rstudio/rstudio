/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.impl.LoadingStrategyBase;
import com.google.gwt.core.client.impl.XhrLoadingStrategy.XhrDownloadStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An Xhr based fragment loading strategy that logs fragment index + fragment text pairs for later
 * introspection.
 */
public class LoggingXhrLoadingStrategy extends LoadingStrategyBase {
  private static Map<Integer, String> sourceByFragmentIndex = new HashMap<Integer, String>();

  protected static String getLeftOverFragmentText() {
    String leftOverFragmentText = "";
    int highestFragmentIndex = -1;
    for (Entry<Integer, String> entry :
        LoggingXhrLoadingStrategy.sourceByFragmentIndex.entrySet()) {
      if (entry.getKey() > highestFragmentIndex) {
        highestFragmentIndex = entry.getKey();
        leftOverFragmentText = entry.getValue();
      }
    }
    return leftOverFragmentText;
  }

  public LoggingXhrLoadingStrategy() {
    super(new XhrDownloadStrategy() {
      @Override
      public void tryDownload(final RequestData request) {
        super.tryDownload(new RequestData(
            request.getUrl(), request.getErrorHandler(), request.getFragment(), this,
            request.getRetryCount()) {

          @Override
          public void tryInstall(String code) {
            super.tryInstall(code);
            sourceByFragmentIndex.put(request.getFragment(), code);
          }
        });
      }
    });
  }
}