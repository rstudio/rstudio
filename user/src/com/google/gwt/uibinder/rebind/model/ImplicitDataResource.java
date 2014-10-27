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
package com.google.gwt.uibinder.rebind.model;

/**
 * Models a method returning a DataResource on a generated ClientBundle.
 */
public class ImplicitDataResource {
  private final String name;
  private final String mimeType;
  private final String source;
  private final Boolean doNotEmbed;

  ImplicitDataResource(String name, String source, String mimeType, Boolean doNotEmbed) {
    this.name = name;
    this.source = source;
    this.mimeType = mimeType;
    this.doNotEmbed = doNotEmbed;
  }

  public String getName() {
    return name;
  }

  public String getSource() {
    return source;
  }

  public String getMimeType() {
    return mimeType;
  }

  public Boolean getDoNotEmbed() {
    return doNotEmbed;
  }
}
