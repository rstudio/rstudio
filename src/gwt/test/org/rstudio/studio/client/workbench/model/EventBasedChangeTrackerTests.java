/*
 * EventBasedChangeTrackerTests.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.junit.client.GWTTestCase;

public class EventBasedChangeTrackerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Stands in for the editor: a HandlerManager-backed event source, like AceEditor.
   private static class Source implements HasValueChangeHandlers<Void>
   {
      @Override
      public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Void> handler)
      {
         return handlers_.addHandler(ValueChangeEvent.getType(), handler);
      }

      @Override
      public void fireEvent(GwtEvent<?> event)
      {
         handlers_.fireEvent(event);
      }

      public void change()
      {
         ValueChangeEvent.fire(this, null);
      }

      public int getHandlerCount()
      {
         return handlers_.getHandlerCount(ValueChangeEvent.getType());
      }

      private final HandlerManager handlers_ = new HandlerManager(this);
   }

   public void testVersionCountsEveryChange()
   {
      Source source = new Source();
      ChangeTracker tracker = new EventBasedChangeTracker<>(source);
      assertFalse(tracker.hasChanged());
      assertEquals(0, tracker.getVersion());

      source.change();
      source.change();
      assertTrue(tracker.hasChanged());
      assertEquals(2, tracker.getVersion());
   }

   public void testResetClearsChangedButKeepsVersion()
   {
      Source source = new Source();
      ChangeTracker tracker = new EventBasedChangeTracker<>(source);
      source.change();

      tracker.reset();
      assertFalse(tracker.hasChanged());
      assertEquals(1, tracker.getVersion());

      source.change();
      assertTrue(tracker.hasChanged());
      assertEquals(2, tracker.getVersion());
   }

   // A save compares the version it saw when it took the editor's contents with
   // the version when the save completes; an edit in between must show up.
   public void testVersionDetectsEditDuringSave()
   {
      Source source = new Source();
      ChangeTracker tracker = new EventBasedChangeTracker<>(source);
      source.change();

      int savedVersion = tracker.getVersion();
      assertEquals(savedVersion, tracker.getVersion());

      source.change();
      assertTrue(tracker.getVersion() != savedVersion);
   }

   // The tracker's whole footprint on its source is one handler, added once.
   public void testTrackerAddsOneHandlerToSource()
   {
      Source source = new Source();
      assertEquals(0, source.getHandlerCount());

      ChangeTracker tracker = new EventBasedChangeTracker<>(source);
      assertEquals(1, source.getHandlerCount());

      source.change();
      tracker.reset();
      tracker.getVersion();
      assertEquals(1, source.getHandlerCount());
   }
}
