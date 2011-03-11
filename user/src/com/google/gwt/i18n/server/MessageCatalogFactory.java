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
package com.google.gwt.i18n.server;

import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Interface for writing various message catalog formats.
 *
 * <p><hr><b>WARNING:</b> this API is expected to change as we develop additional
 * message catalog formats.  In particular, this interface will be extended
 * to support reading message catalogs and further changes may be required.
 * <hr></p>
 *
 * <p>Implementations of this interface are executed at compile time and
 * therefore must not contain any JSNI code.
 * </p>
 */
public interface MessageCatalogFactory {

  /**
   * Context for message catalogs, for things like logging errors.
   */
  interface Context {

    OutputStream createBinaryFile(String catalogName);

    PrintWriter createTextFile(String catalogName, String charSet);

    void error(String msg);

    void error(String msg, Throwable cause);

    GwtLocaleFactory getLocaleFactory();

    void warning(String msg);

    void warning(String msg, Throwable cause);
  }

  /**
   * Writes translatable messages to a message catalog.
   */
  public interface Writer extends Closeable {

    /**
     * Called when no more classes are to be written to this catalog.
     * 
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * Returns a visitor for visiting classes that should be written to this
     * catalog. 
     */
    MessageInterfaceVisitor visitClass();
  }

  /**
   * Returns the extension to use for this file type, including the dot.
   */
  String getExtension();

  /**
   * Return a {@link Writer} that can be used to write the source
   * of the messages to be translated for this class.
   * 
   * @param context
   * @param fileName relative path for output file, though implementations may
   *     ignore this value and use something else, such as an annotation giving
   *     a database key, for example
   * 
   * @return MessageCatalogWriter instance or null if the output file could not
   *     be created
   * @throws MessageProcessingException
   */
  Writer getWriter(Context context,
      String fileName) throws MessageProcessingException;
}
