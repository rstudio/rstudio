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
package com.google.gwt.user.client.rpc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associates a {@link RemoteService} with a relative path. This annotation will
 * cause the client-side proxy to automatically invoke the
 * {@link ServiceDefTarget#setServiceEntryPoint(String)} method with
 * <code>{@link com.google.gwt.core.client.GWT#getModuleBaseURL() GWT.getModuleBaseURL()} + {@link RemoteServiceRelativePath#value()}</code>
 * as its argument. Subsequent calls to
 * {@link ServiceDefTarget#setServiceEntryPoint(String)} will override this
 * default path.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RemoteServiceRelativePath {
  /**
   * The relative path for the {@link RemoteService} implementation.
   * 
   * @return relative path for the {@link RemoteService} implementation
   */
  String value();
}
