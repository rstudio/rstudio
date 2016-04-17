package elemental.json.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import junit.framework.TestCase;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonNull;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class JsonSerializationJreTest extends TestCase {

  public void testSerializeNull() throws Exception {
    JsonNull null1 = Json.createNull();
    JsonNull null2 = Json.createNull();
    JsonValue out = serializeDeserialize(null1);
    assertJsonEquals(null1, out);
    assertSame(null1, out);
    assertSame(null2, out);
    assertSame(JreJsonNull.NULL_INSTANCE, out);
  }

  public void testSerializeObject() throws Exception {
    JsonObject foo = Json.createObject();
    foo.put("true", true);
    foo.put("string", "string");
    foo.put("number", 1.25);

    JsonObject subObject = Json.createObject();
    subObject.put("false", false);
    subObject.put("string2", "string2");
    subObject.put("number", -151);

    JsonArray subArray = Json.createArray();
    subArray.set(0, true);
    subArray.set(1, 1);
    subArray.set(2, "2");

    foo.put("object", subObject);
    foo.put("array", subArray);
    foo.put("null", Json.createNull());

    assertJsonEqualsAfterSerialization(foo);
  }

  public void testSerializeArray() throws Exception {
    JsonObject subObject = Json.createObject();
    subObject.put("false", false);
    subObject.put("string2", "string2");
    subObject.put("number", -151);

    JsonArray subArray = Json.createArray();
    subArray.set(0, true);
    subArray.set(1, 1);
    subArray.set(2, "2");

    JsonArray array = Json.createArray();
    array.set(0, true);
    array.set(1, false);
    array.set(2, 2);
    array.set(3, "3");
    array.set(4, subObject);
    array.set(5, subArray);

    assertJsonEqualsAfterSerialization(array);
  }

  public void testSerializeBoolean() throws Exception {
    assertJsonEqualsAfterSerialization(Json.create(true));
    assertJsonEqualsAfterSerialization(Json.create(false));
  }

  public void testSerializeString() throws Exception {
    assertJsonEqualsAfterSerialization(Json.create("foo"));
    assertJsonEqualsAfterSerialization(Json.create(""));
  }

  public void testSerializeNumber() throws Exception {
    assertJsonEqualsAfterSerialization(Json.create(0));
    assertJsonEqualsAfterSerialization(Json.create(-1.213123123));
  }

  private <T extends Serializable & JsonValue> void assertJsonEqualsAfterSerialization(
      T in) throws Exception {
    T out = serializeDeserialize(in);
    assertNotSame(in, out);
    assertJsonEquals(in, out);
  }

  private void assertJsonEquals(JsonValue a, JsonValue b) {
    assertEquals(a.toJson(), b.toJson());
  }

  @SuppressWarnings("unchecked")
  public <T extends Serializable & JsonValue> T serializeDeserialize(
      T originalJsonValue) throws Exception {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(buffer);
    out.writeObject(originalJsonValue);
    out.close();

    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
        buffer.toByteArray()));
    T processedJsonValue = (T) in.readObject();
    in.close();
    return processedJsonValue;
  }

}
