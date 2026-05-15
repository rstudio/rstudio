/*
 * SessionOpenerTests.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import com.google.inject.Provider;

import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.DummyApplicationServerOperations;
import org.rstudio.studio.client.common.DummyGlobalDisplay;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;

public class SessionOpenerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   /**
    * Verify that onCompleted is called when sendPing exhausts all retries
    * without a successful ping response (regression test for #17143).
    */
   public void testOnCompletedCalledOnRetryExhaustion()
   {
      delayTestFinish(5000);

      // Server mock that never responds to ping (simulates unreachable session)
      ApplicationServerOperations server = new DummyApplicationServerOperations()
      {
         @Override
         public void ping(ServerRequestCallback<VoidResponse> requestCallback)
         {
            // Never call back — ping stays in-flight forever
         }
      };

      SessionOpener opener = new SessionOpener(
         nullProvider(),
         providerOf(new DummyGlobalDisplay()),
         providerOf(server),
         nullProvider()
      );

      // maxRetries=0: first execute() increments retries_ to 1 > 0, hitting exhaustion
      opener.sendPing(10, 0, new Command()
      {
         @Override
         public void execute()
         {
            finishTest();
         }
      });
   }

   @SuppressWarnings("unchecked")
   private static <T> Provider<T> nullProvider()
   {
      return new Provider<T>()
      {
         @Override
         public T get()
         {
            return null;
         }
      };
   }

   private static <T> Provider<T> providerOf(final T value)
   {
      return new Provider<T>()
      {
         @Override
         public T get()
         {
            return value;
         }
      };
   }
}
