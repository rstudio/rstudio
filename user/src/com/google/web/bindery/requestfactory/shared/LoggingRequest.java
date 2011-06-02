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

package com.google.web.bindery.requestfactory.shared;

import com.google.web.bindery.requestfactory.server.Logging;

/**
 * "API Generated" request selector interface implemented by objects that give
 * client access to the methods of {@link Logging}.
 */
@Service(Logging.class)
public interface LoggingRequest extends RequestContext {

  // TODO(unnurg): Pass a SerializableLogRecord here rather than it's
  // serialized string.
  /**
   * Log a message on the server.
   * 
   * @param serializedLogRecordString a json serialized LogRecord, as provided
   *          by
   *          {@link com.google.gwt.logging.client.JsonLogRecordClientUtil#logRecordAsJsonObject(LogRecord)}
   * @return a Void {@link Request}
   */
  Request<Void> logMessage(String serializedLogRecordString);
}
