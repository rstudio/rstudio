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

package com.google.gwt.resources.client.gss;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;

/**
 * ClientBundle used in order to test the {@code @import}
 * and {@code @ImportedWithPrefix} annotations.
 */
public interface ImportResource extends ClientBundle {

  /**
   * CssResource that will be imported without any prefix.
   */
  public interface ImportCss extends CssResource {
    String className();

    String className2();
  }

  /**
   * CssResource that will be imported with a prefix.
   */
  @ImportedWithPrefix("testPrefix")
  public interface ImportWithPrefixCss extends CssResource {
    String className();
  }

  ImportCss importCss();

  ImportWithPrefixCss importWithPrefixCss();
}
