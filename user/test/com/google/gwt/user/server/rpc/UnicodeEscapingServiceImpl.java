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

import com.google.gwt.user.client.rpc.UnicodeEscapingService;
import com.google.gwt.user.client.rpc.UnicodeEscapingTest;

/**
 * Implementation of the {@link UnicodeEscapingService} interface.
 */
public class UnicodeEscapingServiceImpl extends RemoteServiceServlet implements
    UnicodeEscapingService {

  /**
   * @see UnicodeEscapingService#echo(String)
   */
  @Override
  public String echo(String str) {
    return str;
  }

  /**
   * @see UnicodeEscapingService#getStringContainingCharacterRange(int, int)
   */
  @Override
  public String getStringContainingCharacterRange(int start, int end) {
    return UnicodeEscapingTest.getStringContainingCharacterRange(start, end);
  }

  /**
   * @see UnicodeEscapingService#verifyStringContainingCharacterRange(int, int,
   *      String)
   */
  @Override
  public boolean verifyStringContainingCharacterRange(int start, int end,
      String str) throws InvalidCharacterException {
    UnicodeEscapingTest.verifyStringContainingCharacterRange(start, end, str);
    return true;
  }
}
