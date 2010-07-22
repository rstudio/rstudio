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
package com.google.gwt.dev.util.log;

import com.google.gwt.dev.json.JsonArray;
import com.google.gwt.dev.json.JsonException;
import com.google.gwt.dev.json.JsonObject;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.EventType;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Tests the SpeedTracerLogger class.
 */
public class SpeedTracerLoggerTest extends TestCase {
  private static class DummyEventType implements EventType {
    private final String color;
    private final String name;

    public DummyEventType(String name, String color) {
      this.name = name;
      this.color = color;
    }

    public String getColor() {
      return color;
    }

    public String getName() {
      return name;
    }
  }
  private class TestLoggerThreadedThread extends Thread {

    private final EventType event;

    public TestLoggerThreadedThread(EventType event) {
      super();
      this.event = event;
    }

    @Override
    public void run() {
      for (int i = 0; i < MAX_EVENT_LOGS; i++) {
        SpeedTracerLogger.get().start(event);
        SpeedTracerLogger.get().end(event);
      }
    }
  }

  private static final EventType dummyOne = new DummyEventType("Larry", "Red");

  private static final EventType dummyThree = new DummyEventType("Moe", "Blue");

  private static final EventType dummyTwo = new DummyEventType("Curly", "Green");

  private static final int MAX_EVENT_LOGS = 10000;

  public void testSpeedTracerLogger() throws IOException, JsonException {
    Writer writer = new StringWriter();
    SpeedTracerLogger.init(writer);
    SpeedTracerLogger.get().start(dummyOne);
    SpeedTracerLogger.get().start(dummyTwo);
    SpeedTracerLogger.get().end(dummyTwo);
    SpeedTracerLogger.get().start(dummyThree);
    SpeedTracerLogger.get().end(dummyThree);
    SpeedTracerLogger.get().end(dummyOne);
    SpeedTracerLogger.get().flush();

    Reader jsonReader = extractJsonFromWriter(writer);
    JsonObject parsedObject = JsonObject.parse(jsonReader);
    assertTrue("dummyOne", compareJsonToEvent(parsedObject, dummyOne));

    JsonObject dataObject = parsedObject.get("data").asObject();
    assertNotNull(dataObject);

    JsonArray childArray = parsedObject.get("children").asArray();
    assertNotNull(childArray);
    assertEquals(2, childArray.getLength());

    JsonObject child = childArray.get(0).asObject();
    assertTrue("dummyTwo", compareJsonToEvent(child, dummyTwo));

    child = childArray.get(1).asObject();
    assertTrue("dummyThree", compareJsonToEvent(child, dummyThree));
  }

  public void testSpeedTracerLoggerExtraData() throws IOException,
      JsonException {
    Writer writer = new StringWriter();
    SpeedTracerLogger.init(writer);
    SpeedTracerLogger.get().start(dummyOne, "extraStart", "valueStart");
    SpeedTracerLogger.get().end(dummyOne, "extraEnd", "valueEnd");
    SpeedTracerLogger.get().flush();

    Reader jsonReader = extractJsonFromWriter(writer);
    JsonObject parsedObject = JsonObject.parse(jsonReader);
    assertEquals("valueStart", parsedObject.get("data").asObject().get("extraStart").asString().getString());
    assertEquals("valueEnd", parsedObject.get("data").asObject().get("extraEnd").asString().getString());
  }

  public void testSpeedTracerLoggerMultiple() throws IOException, JsonException {
    Writer writer = new StringWriter();
    SpeedTracerLogger.init(writer);
    SpeedTracerLogger.get().start(dummyOne);
    SpeedTracerLogger.get().end(dummyOne);
    SpeedTracerLogger.get().start(dummyTwo);
    SpeedTracerLogger.get().end(dummyTwo);
    SpeedTracerLogger.get().start(dummyThree);
    SpeedTracerLogger.get().end(dummyThree);
    SpeedTracerLogger.get().flush();

    Reader jsonReader = extractJsonFromWriter(writer);
    JsonObject dummyOneObject = JsonObject.parse(jsonReader).asObject();
    JsonObject dummyTwoObject = JsonObject.parse(jsonReader).asObject();
    JsonObject dummyThreeObject = JsonObject.parse(jsonReader).asObject();

    assertTrue(compareJsonToEvent(dummyOneObject, dummyOne));
    assertTrue(compareJsonToEvent(dummyTwoObject, dummyTwo));
    assertTrue(compareJsonToEvent(dummyThreeObject, dummyThree));
  }

  public void testSpeedTracerLoggerThreaded() throws InterruptedException,
      IOException {
    final int NUM_THREADS = 3;
    Writer writer = new StringWriter();
    SpeedTracerLogger.init(writer);
    Thread threads[] = new Thread[NUM_THREADS];
    threads[0] = new TestLoggerThreadedThread(dummyOne);
    threads[1] = new TestLoggerThreadedThread(dummyTwo);
    threads[2] = new TestLoggerThreadedThread(dummyThree);
    for (int i = 0; i < NUM_THREADS; i++) {
      threads[i].start();
    }
    for (int i = 0; i < NUM_THREADS; i++) {
      threads[i].join();
    }
    SpeedTracerLogger.get().flush();
    BufferedReader jsonReader = extractJsonFromWriter(writer);

    int tally[] = new int[NUM_THREADS];
    for (int i = 0; i < MAX_EVENT_LOGS * NUM_THREADS; i++) {
      JsonObject result = null;
      try {
        result = JsonObject.parse(jsonReader).asObject();
      } catch (JsonException ex) {
        fail("Failed to parse json after " + i + " iterations.  "
            + ex.toString());
      }
      if (compareJsonToEvent(result, dummyOne)) {
        tally[0]++;
      } else if (compareJsonToEvent(result, dummyTwo)) {
        tally[1]++;
      } else if (compareJsonToEvent(result, dummyThree)) {
        tally[2]++;
      } else {
        fail("Node with typeName "
            + result.get("typeName").asString().getString()
            + " doesn't match expected");
      }
    }
    for (int i = 0; i < NUM_THREADS; i++) {
      assertEquals("dummy" + i + " has the wrong number of logs.", tally[i],
          MAX_EVENT_LOGS);
    }
  }

  private boolean compareJsonToEvent(JsonObject jsonObject, EventType eventType) {
    String typeName = jsonObject.get("typeName").asString().getString();
    String color = jsonObject.get("color").asString().getString();
    return typeName.equals(eventType.getName())
        && color.equals(eventType.getColor());
  }

  private BufferedReader extractJsonFromWriter(Writer writer)
      throws IOException {
    String jsonString = writer.toString();
    BufferedReader jsonReader = new BufferedReader(new StringReader(jsonString));
    // Skip ahead to start of JSON
    while (true) {
      jsonReader.mark(16 * 1024);
      String line = jsonReader.readLine();
      if (line == null) {
        fail("Didn't find start of JSON string");
      }
      if (line.startsWith("{")) {
        jsonReader.reset();
        break;
      }
    }
    return jsonReader;
  }
}
