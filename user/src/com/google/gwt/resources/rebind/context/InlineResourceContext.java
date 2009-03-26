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
package com.google.gwt.resources.rebind.context;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;

class InlineResourceContext extends StaticResourceContext {
  /**
   * The largest file size that will be inlined. Note that this value is taken
   * before any encodings are applied.
   */
  // The JLS specifies a maximum size for any string to be 2^16 characters, so
  // we'll leave some padding. Assuming a Base64 encoding, it is true that
  // (2 ^ 15) * 4/3 < 2 ^ 16, so we can safely inline files up to 32k.
  private static final int MAX_INLINE_SIZE = 2 << 15;

  InlineResourceContext(TreeLogger logger, GeneratorContext context,
      JClassType resourceBundleType) {
    super(logger, context, resourceBundleType);
  }

  @Override
  public String deploy(String suggestedFileName, String mimeType, byte[] data,
      boolean xhrCompatible) throws UnableToCompleteException {
    TreeLogger logger = getLogger();

    // data: URLs are not compatible with XHRs on FF and Safari browsers
    if ((!xhrCompatible) && (data.length < MAX_INLINE_SIZE)) {
      logger.log(TreeLogger.DEBUG, "Inlining", null);

      // This is bad, but I am lazy and don't want to write _another_ encoder
      sun.misc.BASE64Encoder enc = new sun.misc.BASE64Encoder();
      String base64Contents = enc.encode(data).replaceAll("\\s+", "");

      return "\"data:" + mimeType + ";base64," + base64Contents + "\"";
    } else {
      return super.deploy(suggestedFileName, mimeType, data, true);
    }
  }

  @Override
  public boolean supportsDataUrls() {
    return true;
  }
}