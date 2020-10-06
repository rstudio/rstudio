/*
 * ClassIds.java
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
package org.rstudio.core.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

/**
 * Element classes that are displayed by the Help / Diagnostics / Show DOM Elements command.
 *
 * Should remain stable as they may be used via external automation/scripting.
 *
 * These are intended for use on User Interface elements which can occur multiple times in
 * the UI. For example, the same command button might be seen at same time in multiple
 * toolbars, or exist multiple times due to being part of the editor UI and having several
 * open documents.
 *
 * For elements guaranteed to only exist once, it is more suitable to use elementIds as in ElementIds.java.
 */
public class ClassIds
{
   public static void assignClassId(Element ele, String classId)
   {
      ele.addClassName(getClassId(classId));
   }

   public static void assignClassId(Widget widget, String classId)
   {
      assignClassId(widget.getElement(), classId);
   }

   public static String getClassId(String classId)
   {
      return CLASS_PREFIX + classId;
   }

   public static String idSafeString(String text)
   {
      return ElementIds.idSafeString(text);
   }

   public static void removeClassId(Element ele, String classId)
   {
      ele.removeClassName(getClassId(classId));
   }

   public static void removeClassId(Widget widget, String classId)
   {
      removeClassId(widget.getElement(), classId);
   }

   public final static String CLASS_PREFIX = "rstudio_";

   // Source Panel
   public final static String SOURCE_PANEL = "source_panel";
   public final static String DOC_OUTLINE_CONTAINER = "doc_outline_container";

   // WindowFrameButton (combined with unique suffix for each quadrant)
   public final static String PANEL_MIN_BTN = "panel_min_btn";
   public final static String PANEL_MAX_BTN = "panel_max_btn";

   // Chunk Context (combined with unique suffix for each quadrant)
   public final static String CHUNK = "chunk";
   public final static String CHUNK_OUTPUT = "chunk_output";
   public final static String MODIFY_CHUNK = "modify_chunk";
   public final static String RUN_CHUNK = "run_chunk";
   public final static String PREVIEW_CHUNK = "preview_chunk";

   // ToolbarButton
   public final static String TOOLBAR_BTN = "tlbr_btn";
}
