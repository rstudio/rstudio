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
package com.google.gwt.resources.rebind.context;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Encodes resources into Multipart HTML files. In order to avoid mixed-content
 * warnings using the mhtml: protocol, this context will include a fallback to
 * static files when the module has been loaded from an https source.
 * 
 * @see "RFC 2557"
 */
public class MhtmlResourceContext extends StaticResourceContext {

  /**
   * The MIME multipart boundary token. This is chosen so that it does not
   * overlap with any possible base64 sequences.
   */
  private static final String BOUNDARY = "_GWT";
  private static final String ID_PREFIX = "r";

  private String bundleBaseIdent;
  private int id = 0;
  private String isHttpsIdent;
  private final Map<String, String> strongNameToExpressions = new HashMap<String, String>();

  /**
   * Output is lazily initialized in the case that all deployed resources are
   * large.
   */
  private ByteArrayOutputStream out;
  private String partialPath;
  private PrintWriter pw;

  MhtmlResourceContext(TreeLogger logger, GeneratorContext context,
      JClassType resourceBundleType, ClientBundleContext clientBundleCtx) {
    super(logger, context, resourceBundleType, clientBundleCtx);
  }

  @Override
  public String deploy(String suggestedFileName, String mimeType, byte[] data,
      boolean forceExternal) throws UnableToCompleteException {

    String strongName = Util.computeStrongName(data);
    String toReturn = strongNameToExpressions.get(strongName);
    if (toReturn != null) {
      return toReturn;
    }

    assert isHttpsIdent != null : "isHttpsIdent";
    assert bundleBaseIdent != null : "bundleBaseIdent";

    /*
     * mhtml URLs don't work in HTTPS, so we'll always need the static versions
     * as a fallback.
     */
    String staticLocation = super.deploy(suggestedFileName, mimeType, data,
        forceExternal);

    /*
     * ie6 doesn't treat XHRs to mhtml as cross-site, but ie8 does, so we'll
     * play it safe here.
     */
    if (forceExternal || data.length > MAX_INLINE_SIZE) {
      return staticLocation;
    }

    if (out == null) {
      out = new ByteArrayOutputStream();
      pw = new PrintWriter(out);
      pw.println("Content-Type: multipart/related; boundary=\"" + BOUNDARY
          + "\"");
      pw.println();
    }

    String location = ID_PREFIX + id++;
    String base64 = toBase64(data);

    pw.println("--" + BOUNDARY);
    pw.println("Content-Id:<" + location + ">");
    // CHECKSTYLE_OFF
    pw.println("Content-Type:" + mimeType);
    // CHECKSTYLE_ON
    pw.println("Content-Transfer-Encoding:base64");
    pw.println();
    pw.println(base64);
    pw.println();

    /*
     * Return a Java expression:
     * 
     * isHttps ? (staticLocation) : (bundleBaseIdent + "location")
     */
    toReturn = isHttpsIdent + " ? (" + staticLocation + ") : ("
        + bundleBaseIdent + " + \"" + location + "\")";
    strongNameToExpressions.put(strongName, toReturn);
    return toReturn;
  }

  public void finish() throws UnableToCompleteException {
    if (out != null) {
      TreeLogger logger = getLogger().branch(TreeLogger.DEBUG,
          "Writing container to disk");
      pw.close();

      assert partialPath != null : "partialPath";

      OutputStream realOut = getContext().tryCreateResource(logger, partialPath);
      try {
        realOut.write(out.toByteArray());
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to write container file", e);
      }
      getContext().commitResource(logger, realOut);
    }
  }

  void setBundleBaseIdent(String ident) {
    bundleBaseIdent = ident;
  }

  void setIsHttpsIdent(String ident) {
    isHttpsIdent = ident;
  }

  void setPartialPath(String partialPath) {
    this.partialPath = partialPath;
  }
}
