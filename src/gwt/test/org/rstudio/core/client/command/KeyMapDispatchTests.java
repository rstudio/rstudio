/*
 * KeyMapDispatchTests.java
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
package org.rstudio.core.client.command;

import java.util.Arrays;
import java.util.List;

import org.rstudio.core.client.command.KeyMap.CommandBinding;

import com.google.gwt.junit.client.GWTTestCase;

public class KeyMapDispatchTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtSetUp()
   {
      // Ordered as in ShortcutManager: addins dispatch before app commands
      addins_ = new KeyMap();
      app_ = new KeyMap();

      prefix_ = new KeyCombination(
            "h", 72, KeyboardShortcut.CTRL | KeyboardShortcut.ALT | KeyboardShortcut.SHIFT);
      ctrl1_ = new KeyCombination("1", 49, KeyboardShortcut.CTRL);
      ctrl2_ = new KeyCombination("2", 50, KeyboardShortcut.CTRL);
      plainX_ = new KeyCombination("x", 88, KeyboardShortcut.NONE);
   }

   public void testCompletionThatRunsCounts()
   {
      app_.addBinding(keys(prefix_, ctrl2_), binding(AppCommand.Context.Workbench, true));
      assertTrue(canDispatchAfterPrefix(false));
   }

   public void testCompletionThatDoesNotRunIsRejected()
   {
      app_.addBinding(keys(prefix_, ctrl2_), binding(AppCommand.Context.Editor, true));
      assertFalse(canDispatchAfterPrefix(false));
   }

   public void testShorterBindingHidesLongerOneInSameMap()
   {
      app_.addBinding(keys(prefix_, ctrl1_), binding(AppCommand.Context.Editor, true));
      app_.addBinding(keys(prefix_, ctrl1_, ctrl2_), binding(AppCommand.Context.Workbench, true));
      assertFalse(canDispatchAfterPrefix(false));
   }

   public void testEarlierMapWinsForSameSequence()
   {
      addins_.addBinding(keys(prefix_, ctrl2_), binding(AppCommand.Context.Editor, true));
      app_.addBinding(keys(prefix_, ctrl2_), binding(AppCommand.Context.Workbench, true));
      assertFalse(canDispatchAfterPrefix(false));
   }

   public void testShorterBindingInEarlierMapHidesLongerOne()
   {
      addins_.addBinding(keys(prefix_, ctrl1_), binding(AppCommand.Context.Editor, true));
      app_.addBinding(keys(prefix_, ctrl1_, ctrl2_), binding(AppCommand.Context.Workbench, true));
      assertFalse(canDispatchAfterPrefix(false));
   }

   public void testShorterBindingInLaterMapHidesLongerOne()
   {
      addins_.addBinding(keys(prefix_, ctrl1_, ctrl2_), binding(AppCommand.Context.Workbench, true));
      app_.addBinding(keys(prefix_, ctrl1_), binding(AppCommand.Context.Editor, true));
      assertFalse(canDispatchAfterPrefix(false));
   }

   public void testOtherBranchInEarlierMapDoesNotHide()
   {
      addins_.addBinding(keys(prefix_, ctrl1_), binding(AppCommand.Context.Editor, true));
      app_.addBinding(keys(prefix_, ctrl2_), binding(AppCommand.Context.Workbench, true));
      assertTrue(canDispatchAfterPrefix(false));
   }

   public void testStrokeWithoutModifierBeforeLastClearsKeys()
   {
      app_.addBinding(keys(prefix_, plainX_, ctrl2_), binding(AppCommand.Context.Workbench, true));
      assertFalse(canDispatchAfterPrefix(false));
   }

   public void testLastStrokeWithoutModifierStillDispatches()
   {
      app_.addBinding(keys(prefix_, ctrl2_, plainX_), binding(AppCommand.Context.Workbench, true));
      assertTrue(canDispatchAfterPrefix(false));
   }

   public void testDisabledBindingHidesOnlyWhenIncluded()
   {
      addins_.addBinding(keys(prefix_, ctrl2_), binding(AppCommand.Context.Editor, false));
      app_.addBinding(keys(prefix_, ctrl2_), binding(AppCommand.Context.Workbench, true));
      assertTrue(canDispatchAfterPrefix(false));
      assertFalse(canDispatchAfterPrefix(true));
   }

   private boolean canDispatchAfterPrefix(boolean includeDisabled)
   {
      List<KeyMap> maps = Arrays.asList(addins_, app_);
      return KeyMap.canDispatchAfter(maps, keys(prefix_), includeDisabled,
            binding -> binding.getContext() == AppCommand.Context.Workbench);
   }

   private KeySequence keys(KeyCombination... strokes)
   {
      return new KeySequence(Arrays.asList(strokes));
   }

   private static CommandBinding binding(AppCommand.Context context, boolean enabled)
   {
      return new CommandBinding()
      {
         @Override
         public String getId() { return context + "-" + enabled; }

         @Override
         public void execute() {}

         @Override
         public boolean isEnabled() { return enabled; }

         @Override
         public boolean isUserDefinedBinding() { return false; }

         @Override
         public AppCommand.Context getContext() { return context; }

         @Override
         public boolean isRoutedOutOfCurrentWindow() { return false; }
      };
   }

   // KeyCombination needs GWT client code, so these can't be static
   private KeyCombination prefix_;
   private KeyCombination ctrl1_;
   private KeyCombination ctrl2_;
   private KeyCombination plainX_;

   private KeyMap addins_;
   private KeyMap app_;
}
