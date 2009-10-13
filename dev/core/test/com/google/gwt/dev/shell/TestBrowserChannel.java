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
package com.google.gwt.dev.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

/**
 * A test channel that mocks out Java/JS object references.
 */
class TestBrowserChannel extends BrowserChannel {
  public TestBrowserChannel(InputStream inputStream,
      OutputStream outputStream) throws IOException {
    super(inputStream, outputStream, new ObjectRefFactory() {
      public JavaObjectRef getJavaObjectRef(int refId) {
        return new JavaObjectRef(refId);
      }

      public JsObjectRef getJsObjectRef(int refId) {
        return new JsObjectRef(refId);
      }

      public Set<Integer> getRefIdsForCleanup() {
        return Collections.emptySet();
      }
    });
  }
  
  public MessageType readMessageType() throws IOException,
      BrowserChannelException {
    getStreamToOtherSide().flush();
    return Message.readMessageType(getStreamFromOtherSide());
  }
}