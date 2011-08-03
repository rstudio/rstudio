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
package com.google.gwt.i18n.rebind.keygen;

import com.google.gwt.util.tools.shared.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Key generator using the MD5 hash of the text and meaning.
 * @deprecated Use {@link com.google.gwt.i18n.server.keygen.MD5KeyGenerator}
 * instead.
 */
@Deprecated
public class MD5KeyGenerator implements KeyGenerator {

  public String generateKey(String className, String methodName, String text,
      String meaning) {
    /*
     * This does not use Util.computeStrongName because we would have
     * to concatenate the text and meaning into a temporary buffer.
     */

    if (text == null) {
      // Cannot compute a key if no default text is supplied.
      return null;
    }
    MessageDigest md5;
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error initializing MD5", e);
    }

    try {
      md5.update(text.getBytes("UTF-8"));
      if (meaning != null) {
        md5.update(meaning.getBytes("UTF-8"));
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 unsupported", e);
    }
    return StringUtils.toHexString(md5.digest());
  }
}
