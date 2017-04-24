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
import org.rstudio.studio.client.workbench.views.source.editors.explorer.ObjectExplorerEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.ProfilerEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.urlcontent.UrlContentEditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

public interface EditingTargetSource
{
   EditingTarget getEditingTarget(FileType fileType);
   EditingTarget getEditingTarget(SourceDocument document,
                                  RemoteFileSystemContext fileContext,
                                  Provider<String> defaultNameProvider);
   String getDefaultNamePrefix(SourceDocument doc);

   public static class Impl implements EditingTargetSource
   {
      @Inject
      public Impl(FileTypeRegistry registry,
                  Provider<TextEditingTarget> pTextEditingTarget,
                  Provider<DataEditingTarget> pDataEditingTarget,
                  Provider<UrlContentEditingTarget> pUrlContentEditingTarget,
                  Provider<CodeBrowserEditingTarget> pCodeBrowserEditingTarget,
                  Provider<ProfilerEditingTarget> pProfilerEditingTarget,
                  Provider<ObjectExplorerEditingTarget> pObjectExplorerEditingTarget)
      {
         registry_ = registry;
         pTextEditingTarget_ = pTextEditingTarget;
         pDataEditingTarget_ = pDataEditingTarget;
         pUrlContentEditingTarget_ = pUrlContentEditingTarget;
         pCodeBrowserEditingTarget_ = pCodeBrowserEditingTarget;
         pProfilerEditingTarget_ = pProfilerEditingTarget;
         pObjectExplorerEditingTarget_ = pObjectExplorerEditingTarget;
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
         else if (type instanceof ProfilerType)
            return pProfilerEditingTarget_.get();
         else if (type instanceof ObjectExplorerFileType)
            return pObjectExplorerEditingTarget_.get();
         else
            return null;
      }

      public EditingTarget getEditingTarget(final SourceDocument document,
                                   final RemoteFileSystemContext fileContext,
                                   final Provider<String> defaultNameProvider)
      {
         FileType type = getTypeFromDocument(document);
         
         final FileType finalType = type;
         EditingTarget target = getEditingTarget(type);
         target.initialize(document,
                           fileContext,
                           finalType,
                           defaultNameProvider);
         return target;
      }
      
      public String getDefaultNamePrefix(SourceDocument document)
      {
         FileType type = getTypeFromDocument(document);
         EditingTarget target = getEditingTarget(type);
         
         return target.getDefaultNamePrefix();
      }
      
      private FileType getTypeFromDocument(SourceDocument document)
      {
         FileType type = registry_.getTypeByTypeName(document.getType());
         if (type == null)
         {
            Debug.log("Unknown document type: " + document.getType());
            type = FileTypeRegistry.TEXT;
         }
         
         return type;
      }

      private final FileTypeRegistry registry_;
      private final Provider<TextEditingTarget> pTextEditingTarget_;
      private final Provider<DataEditingTarget> pDataEditingTarget_;
      private final Provider<UrlContentEditingTarget> pUrlContentEditingTarget_;
      private final Provider<CodeBrowserEditingTarget> pCodeBrowserEditingTarget_;
      private final Provider<ProfilerEditingTarget> pProfilerEditingTarget_;
      private final Provider<ObjectExplorerEditingTarget> pObjectExplorerEditingTarget_;
   }
}
