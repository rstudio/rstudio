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

package com.google.gwt.user.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;

/**
 * Helper class, which, during startup, asserts that the browser's current
 * rendering mode is one of the values allowed by the
 * {@value #PROPERTY_DOCUMENT_COMPATMODE}.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Quirks_mode">Quirks Mode</a>
 */
public class DocumentModeAsserter implements EntryPoint {

  /**
   * Interface to provide {@value #PROPERTY_DOCUMENT_COMPATMODE} configuration
   * property value.
   */
  public interface DocumentModeProperty {
    String[] getAllowedDocumentModes();

    Severity getDocumentModeSeverity();
  }

  /**
   * Determine the severity of the runtime {@literal $doc.compatMode} check:
   */
  public static enum Severity {
    /**
     * Receive an error message at runtime.
     */
    ERROR,

    /**
     * No runtime check.
     */
    IGNORE,

    /**
     * Receive a warning in Development Mode.
     */
    WARN;
  }

  /**
   * GWT module configuration property, which enumerates one or more valid
   * browser rendering modes, to be compared with value of
   * {@literal $doc.compatMode} at runtime.
   */
  public static final String PROPERTY_DOCUMENT_COMPATMODE = "document.compatMode";

  /**
   * GWT module configuration property, which determines the severity of the
   * runtime {@literal $doc.compatMode} check. Valid values are specified by
   * {@link Severity}.
   */
  public static final String PROPERTY_DOCUMENT_COMPATMODE_SEVERITY = "document.compatMode.severity";

  /**
   * Value of {@literal $doc.compatMode} in Quirks Mode, {@value}.
   */
  private static final String QUIRKS_MODE_BACK_COMPAT = "BackCompat";

  /**
   * Value of {@literal $doc.compatMode} in Standards Mode, {@value}.
   */
  private static final String STANDARDS_MODE_CSS1_COMPAT = "CSS1Compat";

  @Override
  public void onModuleLoad() {
    DocumentModeProperty impl = GWT.create(DocumentModeProperty.class);
    Severity severity = impl.getDocumentModeSeverity();
    if (severity == Severity.IGNORE) {
      return;
    }

    String currentMode = Document.get().getCompatMode();
    String[] allowedModes = impl.getAllowedDocumentModes();
    for (int i = 0; i < allowedModes.length; i++) {
      if (allowedModes[i].equals(currentMode)) {
        return;
      }
    }

    String message;
    if (allowedModes.length == 1 && STANDARDS_MODE_CSS1_COMPAT.equals(allowedModes[0])
        && QUIRKS_MODE_BACK_COMPAT.equals(currentMode)) {
      /*
       * GWT no longer supports Quirks Mode.
       */
      message = "GWT no longer supports Quirks Mode (document.compatMode=' "
          + QUIRKS_MODE_BACK_COMPAT
          + "').<br>Make sure your application's host HTML page has a Standards Mode "
          + "(document.compatMode=' "
          + STANDARDS_MODE_CSS1_COMPAT
          + "') doctype,<br>e.g. by using &lt;!doctype html&gt; at the start of your application's HTML "
          + "page.<br><br>To continue using this unsupported rendering mode and risk layout problems, "
          + "suppress this message by adding<br>the following line to your*.gwt.xml module file:<br>"
          + "&nbsp;&nbsp;&lt;extend-configuration-property name=\"document.compatMode\" value=\""
          + currentMode + "\"/&gt;";
    } else {
      /*
       * Developer is doing something custom and have modified the default
       * document.compatMode configuration property settings from
       * DocumentMode.gwt.xml, so they're mostly on their own.
       */
      message = "Your *.gwt.xml module configuration prohibits the use of the current doucment "
          + "rendering mode (document.compatMode=' " + currentMode
          + "').<br>Modify your application's host HTML page doctype, or update your custom "
          + "'document.compatMode' configuration property settings.";
    }

    if (severity == Severity.ERROR) {
      throw new RuntimeException(message);
    }

    // Warning compiled out in Production Mode
    GWT.log(message);
  }
}
