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
package com.google.gwt.layout.client;

/**
 * User-Agent utility methods used by {@link LayoutImplIE6}.
 * 
 * TODO: Generalize this, move it into a common place, and make it available for
 * use by other classes.
 */
class UserAgent {

  // Stolen and modified from UserAgent.gwt.xml.
  public static native boolean isIE6() /*-{
     function makeVersion(result) {
       return (parseInt(result[1]) * 1000) + parseInt(result[2]);
     }

     var ua = navigator.userAgent.toLowerCase();
     if (ua.indexOf("msie") != -1) {
       var result = /msie ([0-9]+)\.([0-9]+)/.exec(ua);
       if (result && result.length == 3) {
         var v = makeVersion(result);
         if (v < 7000) {
           return true;
         }
       }
     }

     return false;
   }-*/;
}
