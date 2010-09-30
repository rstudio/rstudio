/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.user.client.rpc.core.java.util.logging;

import com.google.gwt.core.client.impl.SerializableThrowable;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Custom serializer for LogRecord.
 */
public class LogRecord_CustomFieldSerializer {
  public static void deserialize(SerializationStreamReader reader,
      LogRecord instance) throws SerializationException {
    String loggerName = reader.readString();
    Long millis = reader.readLong();
    Object throwable = reader.readObject();

    instance.setLoggerName(loggerName);
    instance.setMillis(millis);
    if (throwable != null && throwable instanceof SerializableThrowable) {
      instance.setThrown(((SerializableThrowable) throwable).getThrowable());
    }
  }

  public static LogRecord instantiate(SerializationStreamReader reader)
  throws SerializationException {
    // Note: Fields should be read in the same order that they were written.
    String levelString = reader.readString();
    String msg = reader.readString();
    
    Level level = Level.parse(levelString);
    LogRecord toReturn = new LogRecord(level, msg);
    return toReturn;
  }

  public static void serialize(SerializationStreamWriter writer,
      LogRecord lr) throws SerializationException {
    // Although Level is serializable, the Level in LogRecord is actually a
    // LevelWithExposedConstructor, which serialization does not like, so we
    // manually just serialize the name.
    writer.writeString(lr.getLevel().getName());
    writer.writeString(lr.getMessage());
    writer.writeString(lr.getLoggerName());
    writer.writeLong(lr.getMillis());
    if (lr.getThrown() != null) {
      writer.writeObject(new SerializableThrowable(lr.getThrown()));
    } else {
      writer.writeObject(null);
    }
  }
}
