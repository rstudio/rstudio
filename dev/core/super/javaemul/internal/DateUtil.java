/*
 * Copyright 2015 Google Inc.
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
package javaemul.internal;

/**
 * Simple Helper class to return Date.now.
 */
public class DateUtil {
  /**
   * Returns the numeric value corresponding to the current time -
   * the number of milliseconds elapsed since 1 January 1970 00:00:00 UTC.
   */
  public static native double now() /*-{
      // IE8 does not have Date.now
      // when removing IE8 support we change this to Date.now()
      if (Date.now) {
          // Date.now vs Date.getTime() performance comparison:
          // http://jsperf.com/date-now-vs-new-date/8
          return Date.now();
      }
      return (new Date()).getTime();
  }-*/;
}