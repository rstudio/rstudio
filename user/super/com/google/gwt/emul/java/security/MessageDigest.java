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
package java.security;

import com.google.gwt.core.client.impl.Md5Digest;

/**
 * Message Digest algorithm - <a href=
 * "http://java.sun.com/j2se/1.4.2/docs/api/java/security/MessageDigest.html"
 * >[Sun's docs]</a>.
 */
public abstract class MessageDigest extends MessageDigestSpi {

  public static MessageDigest getInstance(String algorithm)
      throws NoSuchAlgorithmException {
    if ("MD5".equals(algorithm)) {
      return new Md5Digest();
    }
    throw new NoSuchAlgorithmException(algorithm + " not supported");
  }

  public static boolean isEqual(byte[] digestA, byte[] digestB) {
    int n = digestA.length;
    if (n != digestB.length) {
      return false;
    }
    for (int i = 0; i < n; ++i) {
      if (digestA[i] != digestB[i]) {
        return false;
      }
    }
    return true;
  }

  private final String algorithm;

  protected MessageDigest(String algorithm) {
    this.algorithm = algorithm;
  }

  public byte[] digest() {
    return engineDigest();
  }

  public byte[] digest(byte[] input) {
    update(input);
    return digest();
  }

  public int digest(byte[] buf, int offset, int len) throws DigestException {
    return engineDigest(buf, offset, len);
  }

  public final String getAlgorithm() {
    return algorithm;
  }

  public final int getDigestLength() {
    return engineGetDigestLength();
  }

  public void reset() {
    engineReset();
  }

  public void update(byte input) {
    engineUpdate(input);
  }

  public void update(byte[] input) {
    engineUpdate(input, 0, input.length);
  }

  public void update(byte[] input, int offset, int len) {
    engineUpdate(input, offset, len);
  }
}
