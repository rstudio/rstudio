/*
 * AceCommandManagerTests.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import org.rstudio.core.client.ExternalJavaScriptLoader;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.junit.client.GWTTestCase;

public class AceCommandManagerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   /**
    * Off macOS (Windows and Linux alike), does Modify Keyboard Shortcuts see
    * the "win" bindings that Ace editors use there?
    */
   public void testUsesWinBindingsOffMac()
   {
      withAcePlatform(false, () ->
      {
         AceCommandManager manager = AceCommandManager.create();
         assertEquals("Ctrl-Shift-Z|Ctrl-Y", redoBindings(manager));
      });
   }

   /**
    * On macOS, does Modify Keyboard Shortcuts see the "mac" bindings?
    */
   public void testUsesMacBindingsOnMac()
   {
      withAcePlatform(true, () ->
      {
         AceCommandManager manager = AceCommandManager.create();
         assertEquals("Command-Shift-Z|Command-Y", redoBindings(manager));
      });
   }

   private static String redoBindings(AceCommandManager manager)
   {
      AceCommand redo = manager.getCommands().getObject("redo");
      JsArrayString bindings = redo.getBindingsForCurrentPlatform();
      return bindings.join("|");
   }

   // Ace editors choose their binding set from Ace's own platform test, so
   // pin that test's answer, letting either set be checked on any host.
   private void withAcePlatform(boolean isMac, Runnable check)
   {
      delayTestFinish(30000);
      aceLoader().addCallback(() ->
      {
         boolean wasMac = setAceIsMac(isMac);
         try
         {
            check.run();
         }
         finally
         {
            setAceIsMac(wasMac);
         }
         finishTest();
      });
   }

   private static native boolean setAceIsMac(boolean isMac)
   /*-{
      var useragent = $wnd.require("ace/lib/useragent");
      var previous = useragent.isMac;
      useragent.isMac = isMac;
      return previous;
   }-*/;

   // Created on first use, not in a static initializer, which JUnit would run
   // outside GWT. Shared so the suite loads Ace once.
   private static ExternalJavaScriptLoader aceLoader()
   {
      if (aceLoader_ == null)
      {
         String url = AceResources.INSTANCE.acejs().getSafeUri().asString();
         aceLoader_ = new ExternalJavaScriptLoader(url);
      }
      return aceLoader_;
   }

   private static ExternalJavaScriptLoader aceLoader_;
}
