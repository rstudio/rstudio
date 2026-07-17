/*
 * VimrcLoaderTests.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

import com.google.gwt.junit.client.GWTTestCase;

// Tests for VimrcLoader.prepareLine(), which decides which vimrc lines are
// forwarded to the Vim emulation. The allowlist is a security boundary: it
// keeps a vimrc from triggering RStudio's own ex commands (:qall, :Rscript,
// :edit, ...) as a side effect of being loaded.
public class VimrcLoaderTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testMappingCommandsPassThrough()
   {
      assertEquals("imap jk <Esc>", VimrcLoader.prepareLine("imap jk <Esc>"));
      assertEquals("nnoremap Y y$", VimrcLoader.prepareLine("nnoremap Y y$"));
      assertEquals("unmap Y", VimrcLoader.prepareLine("unmap Y"));
      assertEquals("set ignorecase", VimrcLoader.prepareLine("set ignorecase"));
   }

   public void testShortCommandNamesPassThrough()
   {
      assertEquals("nn j gj", VimrcLoader.prepareLine("nn j gj"));
      assertEquals("se ic", VimrcLoader.prepareLine("se ic"));
   }

   public void testUnsupportedCommandsRejected()
   {
      assertNull(VimrcLoader.prepareLine("call plug#begin()"));
      assertNull(VimrcLoader.prepareLine("let mapleader = \",\""));
      assertNull(VimrcLoader.prepareLine("syntax on"));

      // ex commands registered by RStudio itself must not run at load time
      assertNull(VimrcLoader.prepareLine("qall"));
      assertNull(VimrcLoader.prepareLine("edit foo.R"));
      assertNull(VimrcLoader.prepareLine("Rscript"));
   }

   public void testCommentsAndBlankLinesRejected()
   {
      assertNull(VimrcLoader.prepareLine("\" a comment"));
      assertNull(VimrcLoader.prepareLine(""));
      assertNull(VimrcLoader.prepareLine("   "));
   }

   public void testSkippableArgumentsStripped()
   {
      assertEquals("nnoremap Y y$", VimrcLoader.prepareLine("nnoremap <silent> Y y$"));
      assertEquals("nnoremap Y y$", VimrcLoader.prepareLine("nnoremap <silent> <unique> Y y$"));

      // special arguments are case-insensitive
      assertEquals("nnoremap Y y$", VimrcLoader.prepareLine("nnoremap <Silent> Y y$"));
   }

   public void testUnsupportedSemanticsRejected()
   {
      assertNull(VimrcLoader.prepareLine("nnoremap <expr> j Foo()"));
      assertNull(VimrcLoader.prepareLine("nnoremap <buffer> j gj"));
      assertNull(VimrcLoader.prepareLine("nnoremap <silent> <buffer> j gj"));
   }

   public void testKeyNotationLhsPreserved()
   {
      assertEquals("nnoremap <C-a> ggVG", VimrcLoader.prepareLine("nnoremap <C-a> ggVG"));
      assertEquals("nmap <Leader>w :w<CR>", VimrcLoader.prepareLine("nmap <Leader>w :w<CR>"));
   }

   public void testArgumentSpacingPreserved()
   {
      // spacing within the arguments is significant and must survive
      assertEquals("imap ,, a  b", VimrcLoader.prepareLine("imap ,, a  b"));
   }

   public void testEdgeCases()
   {
      // bare command
      assertEquals("map", VimrcLoader.prepareLine("map"));

      // surrounding whitespace is trimmed
      assertEquals("imap jk <Esc>", VimrcLoader.prepareLine("  imap jk <Esc>  "));

      // unterminated key notation passes through untouched
      assertEquals("nnoremap <C-a", VimrcLoader.prepareLine("nnoremap <C-a"));
   }
}
