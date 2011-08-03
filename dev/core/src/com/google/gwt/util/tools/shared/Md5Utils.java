/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.util.tools.shared;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class to generate MD5 hashes using per-thread MD5
 * {@link MessageDigest} instance.
 */
public class Md5Utils {

  /**
   * Per thread MD5 instance.
   */
  private static final ThreadLocal<MessageDigest> perThreadMd5  =
    new ThreadLocal<MessageDigest>() {
      @Override
      protected MessageDigest initialValue() {
        try {
          return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
          throw new RuntimeException("MD5 implementation not found", e);
        }
      };
  };

  /**
   * Generate MD5 digest.
   *
   * @param input input data to be hashed.
   * @return MD5 digest.
   */
  public static byte[] getMd5Digest(byte[] input) {
    MessageDigest md5 = perThreadMd5.get();
    md5.reset();
    md5.update(input);
    return md5.digest();
  }
}
