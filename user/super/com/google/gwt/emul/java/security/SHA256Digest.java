/*
 * Copyright 2016 Google Inc.
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

import java.util.Arrays;

/**
 * SHA256 implementation for GWT.
 */
public class SHA256Digest extends MessageDigest {

  private static final int WORD_LENGTH = 32;
  private static final int[] K = {
      0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4,
      0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe,
      0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f,
      0x4a7484aa, 0x5cb0a9dc, 0x76f988da, 0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
      0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc,
      0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b,
      0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070, 0x19a4c116,
      0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
      0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7,
      0xc67178f2
  };

  private static final int[] H = {
      0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
      0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
  };

  private int[] hash;
  private byte[] wordBuffer;
  private int wordOff;
  private int[] blockBuffer;
  private int blockOff;
  private long byteCounter;

  SHA256Digest() {
    super("SHA-256");
    hash = new int[8];
    wordBuffer = new byte[4];
    blockBuffer = new int[64];
    engineReset();
  }

  @Override
  protected void engineReset() {
    System.arraycopy(H, 0, hash, 0, 8);
    Arrays.fill(wordBuffer, (byte) 0);
    Arrays.fill(blockBuffer, 0);
    wordOff = 0;
    blockOff = 0;
    byteCounter = 0;
  }

  @Override
  protected void engineUpdate(byte input) {
    wordBuffer[wordOff] = input;
    wordOff++;
    byteCounter++;
    if (wordOff == 4) {
      updateWord();
      wordOff = 0;
    }
  }

  @Override
  protected void engineUpdate(byte[] input, int inOff, int len) {
    for (int i = 0; i < len; i++) {
      engineUpdate(input[inOff + i]);
    }
  }

  @Override
  protected byte[] engineDigest() {
    doPadding();
    byte[] result = new byte[32];
    for (int i = 0; i < 8; i++) {
      int2bytes(hash[i], result, i * 4);
    }
    engineReset();
    return result;
  }

  private void int2bytes(int input, byte[] output, int outOff) {
    output[outOff] = (byte) ((input >>> 24) & 0xff);
    output[outOff + 1] = (byte) ((input >>> 16) & 0xff);
    output[outOff + 2] = (byte) ((input >>> 8) & 0xff);
    output[outOff + 3] = (byte) (input & 0xff);
  }

  private void doPadding() {
    long length = byteCounter << 3;
    engineUpdate((byte) 128);
    while (wordOff != 0) {
      engineUpdate((byte) 0);
    }
    Arrays.fill(blockBuffer, blockOff, 64, 0);
    if (blockOff > 14) {
      computeBlock();
      Arrays.fill(blockBuffer, 0);
    }
    blockBuffer[14] = (int) (length >>> 32);
    blockBuffer[15] = enforceOverflow((int) length);
    computeBlock();
  }

  private void updateWord() {
    blockBuffer[blockOff] = ((wordBuffer[0] & 0xff) << 24) | ((wordBuffer[1] & 0xff) << 16)
        | ((wordBuffer[2] & 0xff) << 8) | (wordBuffer[3] & 0xff);
    blockOff++;
    if (blockOff == 16) {
      computeBlock();
    }
  }

  private void computeBlock() {
    // Prepare message schedule.
    for (int t = 16; t < blockBuffer.length; t++) {
      blockBuffer[t] = enforceOverflow(sigma1(blockBuffer[t - 2]) + blockBuffer[t - 7]
          + sigma0(blockBuffer[t - 15]) + blockBuffer[t - 16]);
    }
    // Init working variables with hash value from previously round.
    int a = hash[0];
    int b = hash[1];
    int c = hash[2];
    int d = hash[3];
    int e = hash[4];
    int f = hash[5];
    int g = hash[6];
    int h = hash[7];

    for (int t = 0; t < 64; t++) {
      int t1 = h + sum1(e) + ch(e, f, g) + K[t] + blockBuffer[t];
      int t2 = sum0(a) + maj(a, b, c);
      h = g;
      g = f;
      f = e;
      e = enforceOverflow(d + t1);
      d = c;
      c = b;
      b = a;
      a = enforceOverflow(t1 + t2);
    }

    // Compute intermediate hash.
    hash[0] = enforceOverflow(a + hash[0]);
    hash[1] = enforceOverflow(b + hash[1]);
    hash[2] = enforceOverflow(c + hash[2]);
    hash[3] = enforceOverflow(d + hash[3]);
    hash[4] = enforceOverflow(e + hash[4]);
    hash[5] = enforceOverflow(f + hash[5]);
    hash[6] = enforceOverflow(g + hash[6]);
    hash[7] = enforceOverflow(h + hash[7]);

    blockOff = 0;
  }

  private int sum0(int x) {
    return rightRotate(x, 2) ^ rightRotate(x, 13) ^ rightRotate(x, 22);
  }

  private int sum1(int x) {
    return rightRotate(x, 6) ^ rightRotate(x, 11) ^ rightRotate(x, 25);
  }

  private int sigma0(int x) {
    return rightRotate(x, 7) ^ rightRotate(x, 18) ^ (x >>> 3);
  }

  private int sigma1(int x) {
    return rightRotate(x, 17) ^ rightRotate(x, 19) ^ (x >>> 10);
  }

  private int rightRotate(int x, int n) {
    return (x >>> n) | enforceOverflow(x << (WORD_LENGTH - n));
  }

  private int ch(int x, int y, int z) {
    return enforceOverflow(((x & y) ^ ((~x) & z)));
  }

  private int maj(int x, int y, int z) {
    return (x & y) ^ (x & z) ^ (y & z);
  }

  // GWT emulates integer with double and doesn't overflow. Enfore it by a integer mask.
  private int enforceOverflow(int input) {
    return input & 0xffffffff;
  }
}
