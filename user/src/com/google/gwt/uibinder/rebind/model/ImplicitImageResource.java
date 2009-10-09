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

import com.google.gwt.resources.client.ImageResource.RepeatStyle;

/**
 * Models a method returning an ImageResource on a generated ClientBundle.
 */
public class ImplicitImageResource {
  private final String name;
  private final String source;
  private final Boolean flipRtl;
  private final RepeatStyle repeatStyle;

  public ImplicitImageResource(
      String name, String source, Boolean flipRtl, RepeatStyle repeatStyle) {
    this.name = name;
    this.source = source;
    this.flipRtl = flipRtl;
    this.repeatStyle = repeatStyle;
  }

  public Boolean getFlipRtl() {
    return flipRtl;
  }

  public String getName() {
    return name;
  }

  public RepeatStyle getRepeatStyle() {
    return repeatStyle;
  }

  public String getSource() {
    return source;
  }
}
