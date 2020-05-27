/*
 * AceResources.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;

import org.rstudio.core.client.resources.StaticDataResource;

public interface AceResources extends ClientBundle
{
   public static final AceResources INSTANCE = GWT.create(AceResources.class);
   
   @Source("ace.js")
   StaticDataResource acejs();
   
   @Source("ace-uncompressed.js")
   StaticDataResource acejsUncompressed();

   @Source("acesupport.js")
   StaticDataResource acesupportjs();
   
   // emacs, vim keybindings
   @Source("keybinding-emacs.js")
   StaticDataResource keybindingEmacsJs();
   
   @Source("keybinding-emacs-uncompressed.js")
   StaticDataResource keybindingEmacsUncompressedJs();
   
   @Source("keybinding-vim.js")
   StaticDataResource keybindingVimJs();
   
   @Source("keybinding-vim-uncompressed.js")
   StaticDataResource keybindingVimUncompressedJs();
   
   @Source("ext-language_tools.js")
   StaticDataResource extLanguageTools();
   
   @Source("ext-language_tools-uncompressed.js")
   StaticDataResource extLanguageToolsUncompressed();
   
}
