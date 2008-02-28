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
package com.google.gwt.benchmarks.viewer.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

/**
 * A data object for Benchmark results.
 */
public class Result implements IsSerializable {

  private String agent;

  private String exception;

  private String host;

  private List<Trial> trials;

  public Result() {
  }

  public String getAgent() {
    return agent;
  }

  public String getException() {
    return exception;
  }

  public String getHost() {
    return host;
  }

  public List<Trial> getTrials() {
    return trials;
  }

  public void setAgent(String agent) {
    this.agent = agent;
  }

  public void setException(String exception) {
    this.exception = exception;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setTrials(List<Trial> trials) {
    this.trials = trials;
  }
}
