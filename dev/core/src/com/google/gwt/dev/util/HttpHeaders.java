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
package com.google.gwt.dev.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * HTTP header strings defined by RFCs.
 */
public final class HttpHeaders {

  public static final long MS_SEC = 1000;
  public static final long MS_MIN = MS_SEC * 60;
  public static final long MS_HR = MS_MIN * 60;
  public static final long MS_DAY = MS_HR * 24;

  public static final long SEC_MIN = 60;
  public static final long SEC_HR = SEC_MIN * 60;
  public static final long SEC_DAY = SEC_HR * 24;
  public static final long SEC_YR = SEC_DAY * 365;

  public static final String CACHE_CONTROL = "Cache-Control";
  public static final String CACHE_CONTROL_MAXAGE = "max-age=";
  public static final String CACHE_CONTROL_MUST_REVALIDATE = "must-revalidate";
  public static final String CACHE_CONTROL_NO_CACHE = "no-cache";
  public static final String CACHE_CONTROL_PRIVATE = "private";
  public static final String CACHE_CONTROL_PUBLIC = "public";

  public static final String CONTENT_ENCODING = "Content-Encoding";
  public static final String CONTENT_ENCODING_GZIP = "gzip";
  public static final String CONTENT_LENGTH = "Content-Length";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_TYPE_APPLICATION_XJAVASCRIPT_UTF8 = "application/x-javascript; charset=utf-8";
  public static final String CONTENT_TYPE_TEXT_CSS = "text/css";
  public static final String CONTENT_TYPE_TEXT_HTML = "text/html";
  public static final String CONTENT_TYPE_TEXT_HTML_UTF8 = "text/html; charset=utf-8";
  public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

  public static final String DATE = "Date";
  public static final String ETAG = "ETag";
  public static final String EXPIRES = "Expires";
  public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
  public static final String IF_NONE_MATCH = "If-None-Match";
  public static final String LAST_MODIFIED = "Last-Modified";

  /**
   * The Internet date format for HTTP.
   */
  private static DateFormat sHttpDateFormat = new SimpleDateFormat(
      "EEE, d MMM yyyy HH:mm:ss", Locale.US);

  /**
   * Converts an HTTP date string into a file-style date/time.
   */
  public static long fromInternetDateFormat(String timeStr) {
    Date dateGmt;
    try {
      synchronized (sHttpDateFormat) {
        dateGmt = sHttpDateFormat.parse(timeStr);
      }
    } catch (ParseException e) {
      return 0;
    }
    dateGmt = gmtToDate(dateGmt);
    return dateGmt.getTime();
  }

  /**
   * Converts a file-style date/time into a string form that is compatible with
   * HTTP.
   */
  public static String toInternetDateFormat(long time) {
    Date date = dateToGMT(new Date(time));
    String dateGmt;
    synchronized (sHttpDateFormat) {
      dateGmt = sHttpDateFormat.format(date) + " GMT";
    }
    return dateGmt;
  }

  /**
   * Converts a local date to GMT.
   *
   * @param date the date in the local time zone
   * @return the GMT version
   */
  private static Date dateToGMT(Date date) {
    Calendar cal = Calendar.getInstance();
    long tzMillis = cal.get(Calendar.ZONE_OFFSET)
        + cal.get(Calendar.DST_OFFSET);
    return new Date(date.getTime() - tzMillis);
  }

  /**
   * Converts a GMT into a local date.
   *
   * @param date the date in GMT
   * @return the local time zone version
   */
  private static Date gmtToDate(Date date) {
    Calendar cal = Calendar.getInstance();
    long tzMillis = cal.get(Calendar.ZONE_OFFSET)
        + cal.get(Calendar.DST_OFFSET);
    return new Date(date.getTime() + tzMillis);
  }
}
