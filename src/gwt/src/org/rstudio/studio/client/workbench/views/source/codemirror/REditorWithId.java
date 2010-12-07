/*
 * REditorWithId.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.codemirror;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.reditor.REditor;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;

public class REditorWithId extends REditor
                                  implements TextEditingTarget.DocDisplay
{
   @Inject
   public REditorWithId(ConsoleServerOperations server)
   {
      super(server);
   }

   public String getId()
   {
      return id_;
   }

   public void setId(String id)
   {
      id_ = id;
   }

   public TextFileType getFileType()
   {
      return fileType_;
   }

   public void setFileType(TextFileType fileType)
   {
      fileType_ = fileType;
      setLanguage(fileType.getEditorLanguage());
   }

   public Widget toWidget()
   {
      return this;
   }

   private String id_;
   private TextFileType fileType_;
}
