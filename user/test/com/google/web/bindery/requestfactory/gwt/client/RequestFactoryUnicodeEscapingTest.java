/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.web.bindery.requestfactory.gwt.client;

import com.google.gwt.user.client.rpc.UnicodeEscapingService.InvalidCharacterException;
import com.google.gwt.user.client.rpc.UnicodeEscapingTest;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs through a portion of the Basic Multilingual Plane.
 */
public class RequestFactoryUnicodeEscapingTest extends RequestFactoryTestBase {
  private static final int TEST_FINISH_DELAY_MS = 5000;
  private final UnicodeEscapingTest test = new UnicodeEscapingTest() {

    @Override
    protected void clientToServerVerifyRange(int start, final int end,
        final int size, final int step) throws InvalidCharacterException {
      current = start;
      int blockEnd = Math.min(end, current + size);
      req.unicodeTestRequest().verifyStringContainingCharacterRange(current,
          blockEnd, getStringContainingCharacterRange(start, blockEnd)).fire(
          new Receiver<Void>() {
            List<ServerFailure> fails = new ArrayList<ServerFailure>();

            @Override
            public void onFailure(ServerFailure error) {
              fails.add(error);
              onSuccess(null);
            }

            @Override
            public void onSuccess(Void response) {
              current += step;
              if (current < end) {
                delayTestFinish(TEST_FINISH_DELAY_MS);
                int blockEnd = Math.min(end, current + size);
                req.unicodeTestRequest().verifyStringContainingCharacterRange(
                    current, blockEnd,
                    getStringContainingCharacterRange(current, blockEnd)).fire(
                    this);
              } else if (!fails.isEmpty()) {
                StringBuilder msg = new StringBuilder();
                for (ServerFailure error : fails) {
                  msg.append(error.getMessage()).append("\n");
                }
                throw new RuntimeException(msg.toString());
              } else {
                finishTest();
              }
            }
          });
    }

    @Override
    protected void serverToClientVerify(int start, final int end,
        final int size, final int step) {
      current = start;
      req.unicodeTestRequest().getStringContainingCharacterRange(start,
          Math.min(end, current + size)).fire(new Receiver<String>() {
        List<ServerFailure> fails = new ArrayList<ServerFailure>();

        @Override
        public void onFailure(ServerFailure error) {
          fails.add(error);
          nextBatch();
        }

        @Override
        public void onSuccess(String response) {
          try {
            verifyStringContainingCharacterRange(current,
                Math.min(end, current + size), response);
          } catch (InvalidCharacterException e) {
            fails.add(new ServerFailure(e.getMessage()));
          }
          nextBatch();
        }

        private void nextBatch() {
          current += step;
          if (current < end) {
            delayTestFinish(TEST_FINISH_DELAY_MS);
            req.unicodeTestRequest().getStringContainingCharacterRange(current,
                Math.min(end, current + size)).fire(this);
          } else if (!fails.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            for (ServerFailure t : fails) {
              msg.append(t.getMessage()).append("\n");
            }
            throw new RuntimeException(msg.toString());
          } else {
            finishTest();
          }
        }
      });
    }
  };

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  public void testClientToServerBMPHigh() throws InvalidCharacterException {
    test.testClientToServerBMPHigh();
  }

  public void testClientToServerBMPLow() throws InvalidCharacterException {
    test.testClientToServerBMPLow();
  }

  public void testClientToServerNonBMP() throws InvalidCharacterException {
    test.testClientToServerNonBMP();
  }

  public void testServerToClientBMP() {
    test.testServerToClientBMP();
  }

  public void testServerToClientNonBMP() {
    test.testServerToClientNonBMP();
  }

}
