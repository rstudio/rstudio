/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.user.client.rpc.EnumsTestService;

/**
 * Simple remote service used to echo enumerated types.
 */
public class EnumsTestServiceImpl extends RemoteServiceServlet implements
    EnumsTestService {

  @Override
  public Basic echo(Basic value) {
    return value;
  }

  @Override
  public Complex echo(Complex value) throws EnumStateModificationException {
    if ("client".equals(value.value)) {
      throw new EnumStateModificationException(
          "Client-side enumeration state made it to the server");
    }

    return value;
  }

  @Override
  public Subclassing echo(Subclassing value) {
    return value;
  }
  
  @Override
  public FieldEnumWrapper echo(FieldEnumWrapper value) {
    return value;
  }
}
