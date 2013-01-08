/*
 * EditingTargetSource.java
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
package org.rstudio.studio.client.workbench.views.source.editors;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.common.filetypes.*;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.source.editors.codebrowser.CodeBrowserEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.urlcontent.UrlContentEditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

public interface EditingTargetSource
{
   EditingTarget getEditingTarget(FileType fileType);
   EditingTarget getEditingTarget(SourceDocument document,
                                  RemoteFileSystemContext fileContext,
                                  Provider<String> defaultNameProvider);

   public static class Impl implements EditingTargetSource
   {
      @Inject
      public Impl(FileTypeRegistry registry,
                  Provider<TextEditingTarget> pTextEditingTarget,
                  Provider<DataEditingTarget> pDataEditingTarget,
                  Provider<UrlContentEditingTarget> pUrlContentEditingTarget,
                  Provider<CodeBrowserEditingTarget> pCodeBrowserEditingTarget)
      {
         registry_ = registry;
         pTextEditingTarget_ = pTextEditingTarget;
         pDataEditingTarget_ = pDataEditingTarget;
         pUrlContentEditingTarget_ = pUrlContentEditingTarget;
         pCodeBrowserEditingTarget_ = pCodeBrowserEditingTarget;
      }

      public EditingTarget getEditingTarget(FileType type)
      {
         if (type instanceof TextFileType)
            return pTextEditingTarget_.get();
         else if (type instanceof DataFrameType)
            return pDataEditingTarget_.get();
         else if (type instanceof UrlContentType)
            return pUrlContentEditingTarget_.get();
         else if (type instanceof CodeBrowserType)
            return pCodeBrowserEditingTarget_.get();
         else
            return null;
      }

      public EditingTarget getEditingTarget(final SourceDocument document,
                                   final RemoteFileSystemContext fileContext,
                                   final Provider<String> defaultNameProvider)
      {
         FileType type = registry_.getTypeByTypeName(document.getType());
         if (type == null)
         {
            Debug.log("Unknown document type: " + document.getType());
            type = FileTypeRegistry.TEXT;
         }
         final FileType finalType = type;
         EditingTarget target = getEditingTarget(type);
         target.initialize(document,
                           fileContext,
                           finalType,
                           defaultNameProvider);
         return target;
      }

      private final FileTypeRegistry registry_;
      private final Provider<TextEditingTarget> pTextEditingTarget_;
      private final Provider<DataEditingTarget> pDataEditingTarget_;
      private final Provider<UrlContentEditingTarget> pUrlContentEditingTarget_;
      private final Provider<CodeBrowserEditingTarget> pCodeBrowserEditingTarget_;
   }
}
