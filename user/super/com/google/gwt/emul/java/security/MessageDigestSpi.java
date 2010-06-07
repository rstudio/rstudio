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

/**
 * Message Digest Service Provider Interface - <a
 * href="http://java.sun.com/j2se/1.4.2/docs/api/java/security/MessageDigestSpi.html">[Sun's
 * docs]</a>.
 */
public abstract class MessageDigestSpi {

  protected abstract byte[] engineDigest();
  
  protected int engineDigest(byte[] buf, int offset, int len)
      throws DigestException {
    byte[] digest = engineDigest();
    if (buf.length < digest.length + offset) {
      throw new DigestException("Insufficient buffer space for digest");
    }
    if (len < digest.length) {
      throw new DigestException("Length not large enough to hold digest");
    }
    System.arraycopy(digest, 0, buf, offset, digest.length);
    return digest.length;
  }
  
  protected int engineGetDigestLength() {
    return 0;
  }
  
  protected abstract void engineReset();
  
  protected abstract void engineUpdate(byte input);
  
  protected abstract void engineUpdate(byte[] input, int offset, int len);
}
