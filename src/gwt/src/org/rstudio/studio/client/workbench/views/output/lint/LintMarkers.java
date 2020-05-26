/*
 * LintMarkers.java
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
package org.rstudio.studio.client.workbench.views.output.lint;

import java.util.ArrayList;

import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Document;

import com.google.gwt.core.client.JsArray;

public class LintMarkers
{
   class AnchoredAceAnnotation
   {
      public AnchoredAceAnnotation(AceAnnotation annotation)
      {
         annotation_ = annotation;
         anchor_ = Anchor.createAnchor(
               document_,
               annotation.row(),
               annotation.column());
      }
      
      public AceAnnotation asAceAnnotation()
      {
         return AceAnnotation.create(
               anchor_.getRow(),
               anchor_.getColumn(),
               annotation_.text(),
               annotation_.type());
      }
      
      private final AceAnnotation annotation_;
      private final Anchor anchor_;
   }
   
   public LintMarkers(Document document,
                      JsArray<AceAnnotation> annotations)
   {
      document_ = document;
      annotations_ = new ArrayList<AnchoredAceAnnotation>();
      annotations_.ensureCapacity(annotations.length());
      for (int i = 0; i < annotations.length(); i++)
         annotations_.set(i, new AnchoredAceAnnotation(annotations.get(i)));
   }
   
   public JsArray<AceAnnotation> asAceAnnotations()
   {
      JsArray<AceAnnotation> annotations =
            JsArray.createArray().cast();
      annotations.setLength(annotations_.size());
      for (int i = 0; i < annotations_.size(); i++)
         annotations.set(i, annotations_.get(i).asAceAnnotation());
      return annotations;
   }
   
   private final Document document_;
   private final ArrayList<AnchoredAceAnnotation> annotations_;
}
