/*
 * AceEditorGutterStyles.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

public class AceEditorGutterStyles
{
   public static final String ACTIVE_DEBUG_LINE            = "ace_active_debug_line";
   public static final String EXECUTING_LINE               = "ace_executing-line";
   public static final String INACTIVE_BREAKPOINT          = "ace_inactive-breakpoint";
   public static final String PENDING_BREAKPOINT           = "ace_pending-breakpoint";

   // NES (Next Edit Suggestion) gutter styles - base class + color modifier
   public static final String NES_GUTTER_BASE              = "ace_nes-gutter";
   public static final String NEXT_EDIT_SUGGESTION         = "ace_nes-gutter ace_nes-gutter-highlight";
   public static final String NEXT_EDIT_SUGGESTION_DELETION = "ace_nes-gutter ace_nes-gutter-deletion";
   public static final String NEXT_EDIT_SUGGESTION_INSERTION = "ace_nes-gutter ace_nes-gutter-insertion";
   public static final String NEXT_EDIT_SUGGESTION_REPLACEMENT = "ace_nes-gutter ace_nes-gutter-replacement";
}
