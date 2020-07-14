/*
 * SourceDocument.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import com.google.gwt.core.client.JsArrayString;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.rmarkdown.model.NotebookDoc;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;

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

   public native final void setPath(String path) /*-{
      this.path = path;
   }-*/;

   public native final String getType() /*-{
      return this.type;
   }-*/;

   public native final void setType(String type) /*-{
      this.type = type;
   }-*/;

   public native final String getExtendedType() /*-{
      return this.extended_type;
   }-*/;

   public native final void setExtendedType(String extendedType) /*-{
      this.extended_type = extendedType;
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
   
   public native final NotebookDoc getNotebookDoc() /*-{
      return this.notebook || {};
   }-*/;
   
   public native final ProjectConfig getProjectConfig() /*-{
      return this.project_config || null;
   }-*/;
   
   public final String getSourceWindowId() 
   {
      if (getProperties().hasKey(SourceWindowManager.SOURCE_WINDOW_ID))
      {
         String windowId = getProperties().getAsString(
               SourceWindowManager.SOURCE_WINDOW_ID);
         if (windowId == null)
            return "";
         return windowId;
      }
      return "";
   }
   
   public final String getSourceDisplayName()
   {
      if (getProperties().hasKey(SourceColumnManager.COLUMN_PREFIX))
      {
         String displayName = getProperties().getAsString(SourceColumnManager.COLUMN_PREFIX);
         if (displayName == null)
            return "";
         return displayName;
      }
      return "";
   }

   public final void assignSourceWindowId(String windowId)
   {
      getProperties().setString(SourceWindowManager.SOURCE_WINDOW_ID, windowId);
   }
   
   public final void assignSourceDisplayName(String name)
   {
      getProperties().setString(SourceColumnManager.COLUMN_PREFIX, name);
   }

   // get the collaborative editing session associated with this document 
   // (local-only property; not persisted)
   public native final CollabEditStartParams getCollabParams() /*-{
     if (typeof this.collab_params === "undefined")
        return null;
     return this.collab_params;
   }-*/;
   
   public native final void setCollabParams(CollabEditStartParams params) /*-{
     this.collab_params = params;
   }-*/;
   
   public native final boolean isReadOnly() /*-{
      return !!this.read_only;
   }-*/;
   
   public native final JsArrayString getReadOnlyAlternatives() /*-{
      return this.read_only_alternatives;
   }-*/;
   
   public static boolean isPlumberFile(String extendedType)
   {
      return extendedType != null && extendedType == SourceDocument.XT_PLUMBER_API;
   }

   public static boolean hasCustomSource(String extendedType)
   {
      return extendedType != null && extendedType == SourceDocument.XT_R_CUSTOM_SOURCE;
   }
   
   public final static String XT_RMARKDOWN_PREFIX = "rmarkdown-";
   public final static String XT_RMARKDOWN_DOCUMENT = "rmarkdown-document";
   public final static String XT_RMARKDOWN_NOTEBOOK = "rmarkdown-notebook";
   public final static String XT_SHINY_PREFIX = "shiny-";
   public final static String XT_SHINY_DIR = "shiny-dir";
   public final static String XT_SHINY_SINGLE_FILE = "shiny-single-file";
   public final static String XT_SHINY_SINGLE_EXE = "shiny-single-executable";
   public final static String XT_SHINY_DOCUMENT = "shiny-document";
   public final static String XT_TEST_PREFIX = "test-";
   public final static String XT_TEST_TESTTHAT = "test-testthat";
   public final static String XT_TEST_SHINYTEST = "test-shinytest";
   public final static String XT_JS_PREVIEWABLE = "js-previewable";
   public final static String XT_SQL_PREVIEWABLE = "sql-previewable";
   public final static String XT_PLUMBER_API = "plumber-api";
   public final static String XT_R_CUSTOM_SOURCE = "r-custom-source";
}
