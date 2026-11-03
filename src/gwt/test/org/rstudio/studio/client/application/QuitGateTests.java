/*
 * QuitGateTests.java
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
package org.rstudio.studio.client.application;

import com.google.gwt.junit.client.GWTTestCase;

public class QuitGateTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testOneQuitAtATime()
   {
      QuitGate gate = new QuitGate();
      assertFalse(gate.isClosed());
      assertTrue(gate.start());
      assertTrue(gate.isClosed());
      assertFalse(gate.start()); // a second quit while the first runs
      assertNull(gate.claim());

      // the first quit failed or was aborted
      gate.reopen();
      assertTrue(gate.start());
   }

   public void testClaimReservesTheQuit()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Claim claim = gate.claim();
      assertNotNull(claim);
      assertTrue(gate.isClosed());

      // nobody else quits while the claim is held
      assertFalse(gate.start());
      assertNull(gate.claim());

      // the holder starts its quit once, and the gate stays closed for it
      assertTrue(claim.start());
      assertTrue(gate.isClosed());
      assertFalse(claim.start()); // a consumed claim is not reusable
      assertFalse(gate.start());
   }

   public void testReleaseReopensTheGate()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Claim claim = gate.claim();
      claim.release();
      assertFalse(gate.isClosed());
      assertFalse(claim.start()); // released: the quit must be claimed again

      QuitGate.Claim next = gate.claim();
      assertNotNull(next);
      claim.release(); // an old claim cannot release a newer one
      assertTrue(gate.isClosed());
      assertTrue(next.start());
   }

   public void testReopenDropsAHeldClaim()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Claim claim = gate.claim();
      gate.reopen();
      assertFalse(claim.start());
      assertTrue(gate.start());
   }
}
