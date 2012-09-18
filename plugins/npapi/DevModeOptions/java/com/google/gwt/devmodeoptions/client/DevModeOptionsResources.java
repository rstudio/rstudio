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
package com.google.gwt.devmodeoptions.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface DevModeOptionsResources extends ClientBundle {
  
  public interface Css extends CssResource {  
    String errorMessage();
  
    String exclude();
    
    String explanation();
    
    String header();
    
    String important();
    
    String include();
    
    String logo();
    
    String mainPanel();
    
    String savedHosts();
    
    String savedHostsHeading();
    
    String textBox();
    
    String textCol();
  }
  
  @Source("com/google/gwt/devmodeoptions/client/resources/DevModeOptions.css")
  Css css();

  @Source("com/google/gwt/devmodeoptions/client/resources/gwt128.png")
  ImageResource gwt128();

  @Source("com/google/gwt/devmodeoptions/client/resources/gwt16.png")
  ImageResource gwt16();

  @Source("com/google/gwt/devmodeoptions/client/resources/gwt32.png")
  ImageResource gwt32();

  @Source("com/google/gwt/devmodeoptions/client/resources/gwt48.png")
  ImageResource gwt48();

  @Source("com/google/gwt/devmodeoptions/client/resources/gwt64.png")
  ImageResource gwt64();
  
  @Source("com/google/gwt/devmodeoptions/client/resources/warning.png")
  ImageResource warning();
}
