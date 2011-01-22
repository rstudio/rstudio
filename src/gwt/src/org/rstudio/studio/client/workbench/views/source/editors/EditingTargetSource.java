/*
 * EditingTargetSource.java
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
package org.rstudio.studio.client.workbench.views.source.editors;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.common.filetypes.*;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.urlcontent.UrlContentEditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

public interface EditingTargetSource
{
   void getEditingTarget(FileType fileType,
                         CommandWithArg<EditingTarget> callback);
   void getEditingTarget(SourceDocument document,
                         RemoteFileSystemContext fileContext,
                         Provider<String> defaultNameProvider,
                         CommandWithArg<EditingTarget> callback);

   public static class Impl implements EditingTargetSource
   {
      @Inject
      public Impl(FileTypeRegistry registry,
                  Provider<TextEditingTarget> pTextEditingTarget,
                  Provider<DataEditingTarget> pDataEditingTarget,
                  Provider<UrlContentEditingTarget> pUrlContentEditingTarget)
      {
         registry_ = registry;
         pTextEditingTarget_ = pTextEditingTarget;
         pDataEditingTarget_ = pDataEditingTarget;
         pUrlContentEditingTarget_ = pUrlContentEditingTarget;
      }

      public void getEditingTarget(FileType type,
                                   final CommandWithArg<EditingTarget> callback)
      {
         if (type instanceof TextFileType)
         {
            AceEditor.create(new CommandWithArg<AceEditor>()
            {
               public void execute(AceEditor arg)
               {
                  TextEditingTarget tet = pTextEditingTarget_.get();
                  tet.setEditor(arg);
                  callback.execute(tet);
               }
            });
         }
         else if (type instanceof DataFrameType)
            callback.execute(pDataEditingTarget_.get());
         else if (type instanceof UrlContentType)
            callback.execute(pUrlContentEditingTarget_.get());
      }

      public void getEditingTarget(final SourceDocument document,
                                   final RemoteFileSystemContext fileContext,
                                   final Provider<String> defaultNameProvider,
                                   final CommandWithArg<EditingTarget> callback)
      {
         FileType type = registry_.getTypeByTypeName(document.getType());
         if (type == null)
         {
            Debug.log("Unknown document type: " + document.getType());
            type = FileTypeRegistry.TEXT;
         }
         final FileType finalType = type;
         getEditingTarget(type, new CommandWithArg<EditingTarget>()
         {
            public void execute(EditingTarget target)
            {
               target.initialize(document,
                                 fileContext,
                                 finalType,
                                 defaultNameProvider);
               callback.execute(target);
            }
         });
      }

      private final FileTypeRegistry registry_;
      private final Provider<TextEditingTarget> pTextEditingTarget_;
      private final Provider<DataEditingTarget> pDataEditingTarget_;
      private final Provider<UrlContentEditingTarget> pUrlContentEditingTarget_;
   }
}
