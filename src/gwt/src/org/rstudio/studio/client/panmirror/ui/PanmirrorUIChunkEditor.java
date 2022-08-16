/*
 * PanmirrorUIChunkEditor.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.panmirror.ui;

import com.google.gwt.dom.client.Element;

import elemental2.core.JsObject;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUIChunkEditor
{
   public JsObject editor;
   public Element element;
   public SetMode setMode;
   public Destroy destroy;
   public ExecuteSelection executeSelection;
   public SetExpanded setExpanded;
   public GetExpanded getExpanded;
   
   @JsFunction
   public interface SetMode
   {
      void setMode(String mode);
   }
   
   @JsFunction
   public interface Destroy
   {
      void destroy();
   }

   @JsFunction
   public interface ExecuteSelection
   {
      void executeSelection();
   }

   @JsFunction
   public interface SetExpanded
   {
      void setExpanded(boolean expanded);
   }
   
   @JsFunction
   public interface GetExpanded
   {
      boolean getExpanded();
   }
}
