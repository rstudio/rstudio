/*
 * Copyright 2014 Google Inc.
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

package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.FinalFieldsTestService;

/**
 * The server side implementation of the RPC service.
 *
 */
public class FinalFieldsTestServiceImpl extends RemoteServiceServlet
  implements FinalFieldsTestService {

  public FinalFieldsNode transferObject(FinalFieldsNode node) {
    return new FinalFieldsNode(6, "B", 10);
  }

  @Override
  public int returnI(FinalFieldsNode node) {
    return node.i;
  }
}
