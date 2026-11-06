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
      assertNotNull(gate.start());
      assertTrue(gate.isClosed());
      assertNull(gate.start()); // a second quit while the first runs
      assertNull(gate.claim());

      // the first quit failed or was aborted
      gate.reopen();
      assertNotNull(gate.start());
   }

   public void testClaimReservesTheQuit()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Claim claim = gate.claim();
      assertNotNull(claim);
      assertTrue(gate.isClosed());

      // nobody else quits while the claim is held
      assertNull(gate.start());
      assertNull(gate.claim());

      // the holder starts its quit once, and the gate stays closed for it
      QuitGate.Attempt attempt = claim.start();
      assertNotNull(attempt);
      assertTrue(attempt.isCurrent());
      assertTrue(gate.isClosed());
      assertNull(claim.start()); // a consumed claim is not reusable
      assertNull(gate.start());
   }

   public void testReleaseReopensTheGate()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Claim claim = gate.claim();
      claim.release();
      assertFalse(gate.isClosed());
      assertNull(claim.start()); // released: the quit must be claimed again

      QuitGate.Claim next = gate.claim();
      assertNotNull(next);
      claim.release(); // an old claim cannot release a newer one
      assertTrue(gate.isClosed());
      assertNotNull(next.start());
   }

   public void testReopenDropsAHeldClaim()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Claim claim = gate.claim();
      gate.reopen();
      assertNull(claim.start());
      assertNotNull(gate.start());
   }

   public void testAStalledQuitNoLongerRefusesTheNext()
   {
      QuitGate gate = new QuitGate();
      assertNotNull(gate.start(1000));
      assertNull(gate.start(1000 + QuitGate.kStaleQuitMs - 1));
      assertNull(gate.claim(1000 + QuitGate.kStaleQuitMs - 1));

      // the next quit closes the gate afresh
      assertNotNull(gate.start(1000 + QuitGate.kStaleQuitMs));
      assertTrue(gate.isClosed());
      assertNull(gate.start(1000 + QuitGate.kStaleQuitMs + 1));
   }

   public void testAStaleClaimIsDroppedByTheNextQuit()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Claim claim = gate.claim(0);
      QuitGate.Claim next = gate.claim(QuitGate.kStaleQuitMs);
      assertNotNull(next);
      assertNull(claim.start()); // superseded
      assertNotNull(next.start());
   }

   public void testOnlyTheLatestAttemptsFailureReopensTheGate()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Attempt stalled = gate.start(0);
      QuitGate.Attempt latest = gate.start(QuitGate.kStaleQuitMs);
      assertFalse(stalled.isCurrent());
      assertTrue(latest.isCurrent());

      // the stalled quit fails late: the one that replaced it goes on
      assertFalse(stalled.fail());
      assertTrue(gate.isClosed());
      assertTrue(latest.isCurrent());

      assertTrue(latest.fail());
      assertFalse(gate.isClosed());
      assertFalse(latest.isCurrent());
      assertFalse(latest.fail()); // undone once
   }

   public void testAClaimReplacesAStalledAttempt()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Attempt stalled = gate.start(0);
      QuitGate.Claim claim = gate.claim(QuitGate.kStaleQuitMs);
      assertFalse(stalled.isCurrent());

      // its late failure must not reopen the gate under the claim, or
      // undo what the claimer is arming
      assertFalse(stalled.fail());
      assertTrue(gate.isClosed());
      assertTrue(claim.start().isCurrent());
   }

   public void testNoFailureUndoesAQuitTheSessionAccepted()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Attempt stalled = gate.start(0);
      QuitGate.Attempt latest = gate.start(QuitGate.kStaleQuitMs);

      // the session read the stalled quit's request first and is exiting;
      // the latest request fails with it
      stalled.acknowledged();
      assertFalse(latest.fail());
      assertTrue(gate.isClosed());

      // until the session reports that quit aborted
      gate.reopen();
      QuitGate.Attempt next = gate.start(QuitGate.kStaleQuitMs + 1);
      assertTrue(next.fail());
   }

   public void testAnUnansweredOlderRequestMayStillBeAccepted()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Attempt stalled = gate.start(0);
      stalled.sent();
      QuitGate.Attempt latest = gate.start(QuitGate.kStaleQuitMs);

      // the replacement fails first (its desktop call, say): the session
      // may still quit for the older request, so nothing is undone
      assertFalse(latest.fail());
      assertTrue(gate.isClosed());

      // and it does; no later failure undoes that either
      stalled.acknowledged();
      QuitGate.Attempt again = gate.start(2 * QuitGate.kStaleQuitMs);
      assertFalse(again.fail());
      assertTrue(gate.isClosed());
   }

   public void testTheLastUnansweredRequestToFailUndoesTheQuit()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Attempt stalled = gate.start(0);
      stalled.sent();
      QuitGate.Attempt latest = gate.start(QuitGate.kStaleQuitMs);
      latest.sent();

      assertFalse(latest.fail());
      assertTrue(gate.isClosed());
      assertTrue(stalled.fail());
      assertFalse(gate.isClosed());
   }

   public void testAReleasedClaimKeepsAnAcceptedQuit()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Attempt accepted = gate.start(0);
      accepted.sent();
      accepted.acknowledged();

      // .Last() outlasted the stale limit, and an R switch claimed the next
      // quit, then failed to prepare its R
      QuitGate.Claim claim = gate.claim(QuitGate.kStaleQuitMs);
      assertNotNull(claim);
      assertFalse(claim.release());
      assertTrue(gate.isClosed()); // so no restart can replace the quit

      // until the session reports that quit aborted
      gate.reopen();
      assertFalse(gate.isClosed());
   }

   public void testAReleasedClaimWaitsForAnUnansweredRequest()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Attempt stalled = gate.start(0);
      stalled.sent();
      QuitGate.Claim claim = gate.claim(QuitGate.kStaleQuitMs);

      assertFalse(claim.release());
      assertTrue(gate.isClosed());
      assertTrue(stalled.fail());
      assertFalse(gate.isClosed());
   }

   public void testAnAcceptanceAfterTheAbortIsIgnored()
   {
      QuitGate gate = new QuitGate();
      QuitGate.Attempt aborted = gate.start(0);
      gate.reopen(); // the abort event overtook the quit's own answer
      aborted.acknowledged();

      QuitGate.Attempt next = gate.start(1);
      assertTrue(next.fail());
      assertFalse(gate.isClosed());
   }
}
