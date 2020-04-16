import { uuidv4 } from "../../src/api/util";
import assert from "assert";

describe("uuidv4", () => {
  it("output matches basic uuid specifications", () => {
    const value = uuidv4();
    assert(value.length === 36, "uuid length is 36");
    assert(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value),
      "dashes and hex digits in proper places");
  });
  
  it("returns random results, probably", () => {
    const seen = new Map<string, boolean>();
    for (let i = 0; i < 50; i++) {
      const first4 = uuidv4().substr(0, 4);
      assert(!seen.has(first4), "First 4 digits are unique");
      seen.set(first4, true);
    }
  });
});
