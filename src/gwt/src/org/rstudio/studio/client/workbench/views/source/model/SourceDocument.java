/*
 * SourceDocument.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.core.client.js.JsObject;

public class SourceDocument extends JavaScriptObject
{
   protected SourceDocument()
   {
   }

   /**
    * A unique ID that identifies this document. This can't simply be the
    * path, because multiple documents that have never been saved can be
    * opened at the same time. (And in theory you could also have the same
    * file opened as multiple documents with independent state.)
    */
   public native final String getId() /*-{
      return this.id;
   }-*/;

   /**
    * Gets the path where this file was last saved (or opened from). This may
    * change over the lifetime of this document if Save As is used. It may
    * also be null if the document has never been saved.
    */
   public native final String getPath() /*-{
      return this.path;
   }-*/;

   public native final String getRawPath() /*-{
      return this.raw_path ? this.raw_path : this.path;
   }-*/;

   public native final void setPath(String path) /*-{
      this.path = path;
   }-*/;

   public native final String getType() /*-{
      return this.type;
   }-*/;

   public native final void setType(String type) /*-{
      this.type = type;
   }-*/;

   /**
    * Gets the contents of the file.
    */
   public native final String getContents() /*-{
      return this.contents;
   }-*/;

   public native final void setContents(String contents) /*-{
      this.contents = contents;
   }-*/;

   /**
    * True if changes have been saved to the ID that have not been persisted
    * to the file.
    */
   public native final boolean isDirty() /*-{
      return this.dirty;
   }-*/;

   public native final void setDirty(boolean dirty) /*-{
      this.dirty = dirty;
   }-*/;

   public native final String getHash() /*-{
      return this.hash;
   }-*/;

   public native final void setHash(String hash) /*-{
      this.hash = hash;
   }-*/;

   public native final boolean sourceOnSave() /*-{
      return this.source_on_save;
   }-*/;

   public native final void setSourceOnSave(boolean sourceOnSave) /*-{
      this.source_on_save = sourceOnSave;
   }-*/;

   public native final String getEncoding() /*-{
      return this.encoding || "";
   }-*/;

   public native final void setEncoding(String encoding) /*-{
      this.encoding = encoding;
   }-*/;

   public native final JsObject getProperties() /*-{
      if (!this.properties)
         this.properties = {};
      return this.properties;
   }-*/;

   public native final String getFoldSpec() /*-{
      return this.folds || "";
   }-*/;

   public native final void setFoldSpec(String foldSpec) /*-{
      this.folds = foldSpec;
   }-*/;
}
