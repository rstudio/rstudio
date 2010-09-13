/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.logexample.server;


import com.google.gwt.sample.logexample.shared.MyService;
import com.google.gwt.sample.logexample.shared.SharedClass;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.logging.Logger;

/**
 * A simple servlet that responds to requests to log from the server, either
 * directly, or by calling the shared logging library code.
 */
public class MyServiceImpl extends RemoteServiceServlet implements MyService {

  private static Logger logger =
    Logger.getLogger(MyServiceImpl.class.getName());

  public void doSomething() {
    // Pretend that an error occured
    logger.severe("MyServiceImpl.doSomething() has encountered a (pretend) error");
  }

  public void doSomethingUsingSharedLibrary() {
    SharedClass.doSomething("server");
  }
}
