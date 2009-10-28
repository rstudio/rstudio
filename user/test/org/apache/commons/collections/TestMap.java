/*
 * Copyright 1999-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Tests base {@link java.util.Map} methods and contracts.
 * <p>
 * The forces at work here are similar to those in {@link TestCollection}.
 * If your class implements the full Map interface, including optional
 * operations, simply extend this class, and implement the {@link
 * #makeEmptyMap()} method.
 * <p>
 * On the other hand, if your map implemenation is wierd, you may have to
 * override one or more of the other protected methods.  They're described
 * below.<P>
 *
 * <B>Entry Population Methods</B><P>
 *
 * Override these methods if your map requires special entries:
 *
 * <UL>
 * <LI>{@link #getSampleKeys()}
 * <LI>{@link #getSampleValues()}
 * <LI>{@link #getNewSampleValues()}
 * <LI>{@link #getOtherKeys()}
 * <LI>{@link #getOtherValues()}
 * </UL>
 *
 * <B>Supported Operation Methods</B><P>
 *
 * Override these methods if your map doesn't support certain operations:
 *
 * <UL>
 * <LI> {@link #useDuplicateValues()}
 * <LI> {@link #useNullKey()}
 * <LI> {@link #useNullValue()}
 * <LI> {@link #isAddRemoveModifiable()}
 * <LI> {@link #isChangeable()}
 * </UL>
 *
 * <B>Fixture Methods</B><P>
 *
 * For tests on modification operations (puts and removes), fixtures are used
 * to verify that that operation results in correct state for the map and its
 * collection views.  Basically, the modification is performed against your
 * map implementation, and an identical modification is performed against
 * a <I>confirmed</I> map implementation.  A confirmed map implementation is
 * something like <Code>java.util.HashMap</Code>, which is known to conform
 * exactly to the {@link Map} contract.  After the modification takes place
 * on both your map implementation and the confirmed map implementation, the
 * two maps are compared to see if their state is identical.  The comparison
 * also compares the collection views to make sure they're still the same.<P>
 *
 * The upshot of all that is that <I>any</I> test that modifies the map in
 * <I>any</I> way will verify that <I>all</I> of the map's state is still
 * correct, including the state of its collection views.  So for instance
 * if a key is removed by the map's key set's iterator, then the entry set 
 * is checked to make sure the key/value pair no longer appears.<P>
 *
 * The {@link #map} field holds an instance of your collection implementation.
 * The {@link #entrySet}, {@link #keySet} and {@link #collectionValues} fields hold
 * that map's collection views.  And the {@link #confirmed} field holds
 * an instance of the confirmed collection implementation.  The 
 * {@link #resetEmpty()} and {@link #resetFull()} methods set these fields to 
 * empty or full maps, so that tests can proceed from a known state.<P>
 *
 * After a modification operation to both {@link #map} and {@link #confirmed},
 * the {@link #verify()} method is invoked to compare the results.  The {@link
 * verify()} method calls separate methods to verify the map and its three
 * collection views ({@link verifyMap(), {@link verifyEntrySet()}, {@link
 * verifyKeySet()}, and {@link verifyValues()}).  You may want to override one
 * of the verification methodsto perform additional verifications.  For
 * instance, {@link TestDoubleOrderedMap} would want override its {@link
 * #verifyValues()} method to verify that the values are unique and in
 * ascending order.<P>
 *  
 * <B>Other Notes</B><P>
 *
 * If your {@link Map} fails one of these tests by design, you may still use
 * this base set of cases.  Simply override the test case (method) your {@link
 * Map} fails and/or the methods that define the assumptions used by the test
 * cases.  For example, if your map does not allow duplicate values, override
 * {@link #useDuplicateValues()} and have it return <code>false</code>
 *
 * @author Michael Smith
 * @author Rodney Waldhoff
 * @author Paul Jack
 * @version $Id: TestMap.java,v 1.20.2.1 2004/05/22 12:14:05 scolebourne Exp $
 */
public abstract class TestMap extends TestObject{

    // These instance variables are initialized with the reset method.
    // Tests for map methods that alter the map (put, putAll, remove) 
    // first call reset() to create the map and its views; then perform
    // the modification on the map; perform the same modification on the
    // confirmed; and then call verify() to ensure that the map is equal
    // to the confirmed, that the already-constructed collection views
    // are still equal to the confirmed's collection views.


    /** Map created by reset(). */
    protected Map map;

    /** Entry set of map created by reset(). */
    protected Set entrySet;

    /** Key set of map created by reset(). */
    protected Set keySet;

    /** Values collection of map created by reset(). */
    protected Collection collectionValues;

    /** HashMap created by reset(). */
    protected Map confirmed;

 


    /**
     *  Override if your map does not allow a <code>null</code> key.  The
     *  default implementation returns <code>true</code>
     **/
    protected boolean useNullKey() {
        return true;
    }

    /**
     *  Override if your map does not allow <code>null</code> values.  The
     *  default implementation returns <code>true</code>.
     **/
    protected boolean useNullValue() {
        return true;
    }

    /**
     *  Override if your map does not allow duplicate values.  The default
     *  implementation returns <code>true</code>.
     **/
    protected boolean useDuplicateValues() {
        return true;
    }

    /**
     *  Override if your map allows its mappings to be changed to new values.
     *  The default implementation returns <code>true</code>.
     **/
    protected boolean isChangeable() {
        return true;
    }

    /**
     *  Override if your map does not allow add/remove modifications.  The
     *  default implementation returns <code>true</code>.
     **/
    protected boolean isAddRemoveModifiable() {
        return true;
    }

    /**
     *  Returns the set of keys in the mappings used to test the map.  This
     *  method must return an array with the same length as {@link
     *  #getSampleValues()} and all array elements must be different. The
     *  default implementation constructs a set of String keys, and includes a
     *  single null key if {@link #useNullKey()} returns <code>true</code>.
     **/
    protected Object[] getSampleKeys() {
        Object[] result = new Object[] {
            "blah", "foo", "bar", "baz", "tmp", "gosh", "golly", "gee", 
            "hello", "goodbye", "we'll", "see", "you", "all", "again",
            "key",
            "key2",
            (useNullKey()) ? null : "nonnullkey"
        };
        return result;
    }


    protected Object[] getOtherKeys() {
        return TestCollection.getOtherNonNullStringElements();
    }

    protected Object[] getOtherValues() {
        return TestCollection.getOtherNonNullStringElements();
    }

    /**
     *  Returns the set of values in the mappings used to test the map.  This
     *  method must return an array with the same length as {@link
     *  #getSampleKeys()}.  The default implementation contructs a set of
     *  String values and includes a single null value if {@link
     *  #useNullValue()} returns <code>true</code>, and includes two values
     *  that are the same if {@link #useDuplicateValues()} returns
     *  <code>true</code>.
     **/
    protected Object[] getSampleValues() {
        Object[] result = new Object[] {
            "blahv", "foov", "barv", "bazv", "tmpv", "goshv", "gollyv", "geev",
            "hellov", "goodbyev", "we'llv", "seev", "youv", "allv", "againv",
            (useNullValue()) ? null : "nonnullvalue",
            "value",
            (useDuplicateValues()) ? "value" : "value2",
        };
        return result;
    }

    /**
     *  Returns a the set of values that can be used to replace the values
     *  returned from {@link #getSampleValues()}.  This method must return an
     *  array with the same length as {@link #getSampleValues()}.  The values
     *  returned from this method should not be the same as those returned from
     *  {@link #getSampleValues()}.  The default implementation constructs a
     *  set of String values and includes a single null value if {@link
     *  #useNullValue()} returns <code>true</code>, and includes two values
     *  that are the same if {@link #useDuplicateValues()} returns
     *  <code>true</code>.  
     **/
    protected Object[] getNewSampleValues() {
        Object[] result = new Object[] {
            (useNullValue()) ? null : "newnonnullvalue",
            "newvalue",
            (useDuplicateValues()) ? "newvalue" : "newvalue2",
            "newblahv", "newfoov", "newbarv", "newbazv", "newtmpv", "newgoshv", 
            "newgollyv", "newgeev", "newhellov", "newgoodbyev", "newwe'llv", 
            "newseev", "newyouv", "newallv", "newagainv",
        };
        return result;
    }

    /**
     *  Helper method to add all the mappings described by {@link
     *  #getSampleKeys()} and {@link #getSampleValues()}.
     **/
    protected void addSampleMappings(Map m) {

        Object[] keys = getSampleKeys();
        Object[] values = getSampleValues();
        
        for(int i = 0; i < keys.length; i++) {
            try {
                m.put(keys[i], values[i]);
            } catch (NullPointerException exception) {
                assertTrue("NullPointerException only allowed to be thrown " +
                           "if either the key or value is null.", 
                           keys[i] == null || values[i] == null);
                
                if (keys[i] == null) {
                  if (useNullKey()) {
                    throw new Error("NullPointerException on null key, but " +
                        "useNullKey is not overridden to return false.", exception);
                  }
                } else if (values[i] == null) {
                  if (useNullValue()) {
                    throw new Error("NullPointerException on null value, but " +
                        "useNullValue is not overridden to return false.", exception);
                  }
                } else {
                  // Unknown reason for NullPointer.
                  throw exception;
                }
            }
        }
        assertEquals("size must reflect number of mappings added.",
                     keys.length, m.size());
    }

    /**
     * Return a new, empty {@link Map} to be used for testing. 
     */
    protected abstract Map makeEmptyMap();

    protected Map makeConfirmedMap() {
      return new HashMap();
    }

    /**
     *  Return a new, populated map.  The mappings in the map should match the
     *  keys and values returned from {@link #getSampleKeys()} and {@link
     *  #getSampleValues()}.  The default implementation uses makeEmptyMap()
     *  and calls {@link #addSampleMappings()} to add all the mappings to the
     *  map.
     **/
    protected Map makeFullMap() {
        Map m = makeEmptyMap();
        addSampleMappings(m);
        return m;
    }

    public Object makeObject() {
        return makeEmptyMap();
    }

    /**
     *  Test to ensure the test setup is working properly.  This method checks
     *  to ensure that the getSampleKeys and getSampleValues methods are
     *  returning results that look appropriate.  That is, they both return a
     *  non-null array of equal length.  The keys array must not have any
     *  duplicate values, and may only contain a (single) null key if
     *  useNullKey() returns true.  The values array must only have a null
     *  value if useNullValue() is true and may only have duplicate values if
     *  useDuplicateValues() returns true.  
     **/
    public void testSampleMappings() {
      Object[] keys = getSampleKeys();
      Object[] values = getSampleValues();
      Object[] newValues = getNewSampleValues();

      assertTrue("failure in test: Must have keys returned from " +
                 "getSampleKeys.", keys != null);

      assertTrue("failure in test: Must have values returned from " +
                 "getSampleValues.", values != null);

      // verify keys and values have equivalent lengths (in case getSampleX are
      // overridden)
      assertEquals("failure in test: not the same number of sample " +
                   "keys and values.",  keys.length, values.length);
      
      assertEquals("failure in test: not the same number of values and new values.",
                   values.length, newValues.length);

      // verify there aren't duplicate keys, and check values
      for(int i = 0; i < keys.length - 1; i++) {
          for(int j = i + 1; j < keys.length; j++) {
              assertTrue("failure in test: duplicate null keys.",
                         (keys[i] != null || keys[j] != null));
              assertTrue("failure in test: duplicate non-null key.",
                         (keys[i] == null || keys[j] == null || 
                          (!keys[i].equals(keys[j]) && 
                           !keys[j].equals(keys[i]))));
          }
          assertTrue("failure in test: found null key, but useNullKey " +
                     "is false.", keys[i] != null || useNullKey());
          assertTrue("failure in test: found null value, but useNullValue " +
                     "is false.", values[i] != null || useNullValue());
          assertTrue("failure in test: found null new value, but useNullValue " +
                     "is false.", newValues[i] != null || useNullValue());
          assertTrue("failure in test: values should not be the same as new value",
                     values[i] != newValues[i] && 
                     (values[i] == null || !values[i].equals(newValues[i])));
      }
    }
    
    // tests begin here.  Each test adds a little bit of tested functionality.
    // Many methods assume previous methods passed.  That is, they do not
    // exhaustively recheck things that have already been checked in a previous
    // test methods.  

    /**
     *  Test to ensure that makeEmptyMap and makeFull returns a new non-null
     *  map with each invocation.  
     **/
    public void testMakeMap() {
        Map em = makeEmptyMap();
        assertTrue("failure in test: makeEmptyMap must return a non-null map.",
                   em != null);
        
        Map em2 = makeEmptyMap();
        assertTrue("failure in test: makeEmptyMap must return a non-null map.",
                   em != null);

        assertTrue("failure in test: makeEmptyMap must return a new map " +
                   "with each invocation.", em != em2);

        Map fm = makeFullMap();
        assertTrue("failure in test: makeFullMap must return a non-null map.",
                   fm != null);
        
        Map fm2 = makeFullMap();
        assertTrue("failure in test: makeFullMap must return a non-null map.",
                   fm != null);

        assertTrue("failure in test: makeFullMap must return a new map " +
                   "with each invocation.", fm != fm2);
    }

    /**
     *  Tests Map.isEmpty()
     **/
    public void testMapIsEmpty() {
        
        resetEmpty();
        assertEquals("Map.isEmpty() should return true with an empty map", 
                     true, map.isEmpty());
         verify();

        resetFull();
        assertEquals("Map.isEmpty() should return false with a non-empty map",
                     false, map.isEmpty());
         verify();
          }

    /**
     *  Tests Map.size()
     **/
    public void testMapSize() {
        resetEmpty();
        assertEquals("Map.size() should be 0 with an empty map",
                     0, map.size());
        verify();

        resetFull();
        assertEquals("Map.size() should equal the number of entries " +
                     "in the map", getSampleKeys().length, map.size());
        verify();
    }

    /**
     *  Tests {@link Map#clear()}.  If the map {@link #isAddRemoveModifiable()
     *  can add and remove elements}, then {@link Map#size()} and {@link
     *  Map#isEmpty()} are used to ensure that map has no elements after a call
     *  to clear.  If the map does not support adding and removing elements,
     *  this method checks to ensure clear throws an
     *  UnsupportedOperationException.
     **/
    public void testMapClear() {
        if (!isAddRemoveModifiable()) return;

        resetEmpty();
        map.clear();
        confirmed.clear();
        verify();
        
        resetFull();
        map.clear();
        confirmed.clear();
        verify();
    }


    /**
     *  Tests Map.containsKey(Object) by verifying it returns false for all
     *  sample keys on a map created using an empty map and returns true for
     *  all sample keys returned on a full map. 
     **/
    public void testMapContainsKey() {
        Object[] keys = getSampleKeys();

        resetEmpty();
        for(int i = 0; i < keys.length; i++) {
            assertTrue("Map must not contain key when map is empty", 
                       !map.containsKey(keys[i]));
        }
        verify();

        resetFull();
        for(int i = 0; i < keys.length; i++) {
            assertTrue("Map must contain key for a mapping in the map. " +
		       "Missing: " + keys[i], map.containsKey(keys[i]));
        }
        verify();
    }

    /**
     *  Tests Map.containsValue(Object) by verifying it returns false for all
     *  sample values on an empty map and returns true for all sample values on
     *  a full map.
     **/
    public void testMapContainsValue() {
        Object[] values = getSampleValues();

        resetEmpty();
        for(int i = 0; i < values.length; i++) {
            assertTrue("Empty map must not contain value", 
                       !map.containsValue(values[i]));
        }
        verify();
        
        resetFull();
        for(int i = 0; i < values.length; i++) {
            assertTrue("Map must contain value for a mapping in the map.", 
                       map.containsValue(values[i]));
        }
        verify();
    }


    /**
     *  Tests Map.equals(Object)
     **/
    public void testMapEquals() {
        resetEmpty();
        assertTrue("Empty maps unequal.", map.equals(confirmed));
        verify();

        resetFull();
        assertTrue("Full maps unequal.", map.equals(confirmed));
        verify();

        resetFull();
	// modify the HashMap created from the full map and make sure this
	// change results in map.equals() to return false.
        Iterator iter = confirmed.keySet().iterator();
        iter.next();
        iter.remove();
        assertTrue("Different maps equal.", !map.equals(confirmed));
        
        resetFull();
        assertTrue("equals(null) returned true.", !map.equals(null));
        assertTrue("equals(new Object()) returned true.", 
		   !map.equals(new Object()));
        verify();
    }


    /**
     *  Tests Map.get(Object)
     **/
    public void testMapGet() {
      resetEmpty();

      Object[] keys = getSampleKeys();
      Object[] values = getSampleValues();

      for (int i = 0; i < keys.length; i++) {
        assertTrue("Empty map.get() should return null.", 
            map.get(keys[i]) == null);
      }
      verify();

      resetFull();
      for (int i = 0; i < keys.length; i++) {
        assertEquals("Full map.get() should return value from mapping.", 
            values[i], map.get(keys[i]));
      }
    }

    /**
     *  Tests Map.hashCode()
     **/
    public void testMapHashCode() {
      resetEmpty();
      assertTrue("Empty maps have different hashCodes.", 
          map.hashCode() == confirmed.hashCode());

      resetFull();
      assertTrue("Equal maps have different hashCodes.", 
          map.hashCode() == confirmed.hashCode());
    }

    /**
     *  Tests Map.toString().  Since the format of the string returned by the
     *  toString() method is not defined in the Map interface, there is no
     *  common way to test the results of the toString() method.  Thereforce,
     *  it is encouraged that Map implementations override this test with one
     *  that checks the format matches any format defined in its API.  This
     *  default implementation just verifies that the toString() method does
     *  not return null.
     **/
    public void testMapToString() {
      resetEmpty();
      assertTrue("Empty map toString() should not return null", 
          map.toString() != null);
      verify();

      resetFull();
      assertTrue("Empty map toString() should not return null", 
          map.toString() != null);
      verify();
    }





    /**
     *  Tests Map.put(Object, Object)
     **/
    public void testMapPut() {
      if (!isAddRemoveModifiable()) return;

      resetEmpty();

      Object[] keys = getSampleKeys();
      Object[] values = getSampleValues();
      Object[] newValues = getNewSampleValues();

      for(int i = 0; i < keys.length; i++) {
        Object o = map.put(keys[i], values[i]);
        confirmed.put(keys[i], values[i]);
        verify();
        assertTrue("First map.put should return null", o == null);
        assertTrue("Map should contain key after put", 
            map.containsKey(keys[i]));
        assertTrue("Map should contain value after put", 
            map.containsValue(values[i]));
      }

      for(int i = 0; i < keys.length; i++) {
        Object o = map.put(keys[i], newValues[i]);
        confirmed.put(keys[i], newValues[i]);
        verify();
        assertEquals("Second map.put should return previous value",
            values[i], o);
        assertTrue("Map should still contain key after put",
            map.containsKey(keys[i]));
        assertTrue("Map should contain new value after put",
            map.containsValue(newValues[i]));

        // if duplicates are allowed, we're not guarunteed that the value
        // no longer exists, so don't try checking that.
        if(!useDuplicateValues()) {
          assertTrue("Map should not contain old value after second put",
              !map.containsValue(values[i]));
        }
      }
    }

    /**
     *  Tests Map.putAll(Collection)
     **/
    public void testMapPutAll() {
      if (!isAddRemoveModifiable()) return;

      resetEmpty();

      Map m2 = makeFullMap();

      map.putAll(m2);
      confirmed.putAll(m2);
      verify();

      resetEmpty();

      m2 = new HashMap();
      Object[] keys = getSampleKeys();
      Object[] values = getSampleValues();
      for(int i = 0; i < keys.length; i++) {
        m2.put(keys[i], values[i]);
      }

      map.putAll(m2);
      confirmed.putAll(m2);
      verify();
    }

    /**
     *  Tests Map.remove(Object)
     **/
    public void testMapRemove() {
      if (!isAddRemoveModifiable()) return;

      resetEmpty();

      Object[] keys = getSampleKeys();
      Object[] values = getSampleValues();
      for(int i = 0; i < keys.length; i++) {
        Object o = map.remove(keys[i]);
        assertTrue("First map.remove should return null", o == null);
      }
      verify();

      resetFull();

      for(int i = 0; i < keys.length; i++) {
        Object o = map.remove(keys[i]);
        confirmed.remove(keys[i]);
        verify();

        assertEquals("map.remove with valid key should return value",
            values[i], o);
      }

      Object[] other = getOtherKeys();

      resetFull();
      int size = map.size();
      for (int i = 0; i < other.length; i++) {
        Object o = map.remove(other[i]);
        assertEquals("map.remove for nonexistent key should return null",
            o, null);
        assertEquals("map.remove for nonexistent key should not " +
            "shrink map", size, map.size());
      }
      verify();
    }


    /**
     *  Utility methods to create an array of Map.Entry objects
     *  out of the given key and value arrays.<P>
     *
     *  @param keys    the array of keys
     *  @param values  the array of values
     *  @return an array of Map.Entry of those keys to those values
     */
    private Map.Entry[] makeEntryArray(Object[] keys, Object[] values) {
        Map.Entry[] result = new Map.Entry[keys.length];
        for (int i = 0; i < keys.length; i++) {
            result[i] = new DefaultMapEntry(keys[i], values[i]);
        }
        return result;
    }

 
    class TestMapEntrySet extends TestSet {
        public TestMapEntrySet() {
            super("");
        }

        // Have to implement manually; entrySet doesn't support addAll
        protected Object[] getFullElements() {
            Object[] k = getSampleKeys();
            Object[] v = getSampleValues();
            return makeEntryArray(k, v);
        }
        
        // Have to implement manually; entrySet doesn't support addAll
        protected Object[] getOtherElements() {
            Object[] k = getOtherKeys();
            Object[] v = getOtherValues();
            return makeEntryArray(k, v);
        }
        
        protected Set makeEmptySet() {
            return makeEmptyMap().entrySet();
        }
        
        protected Set makeFullSet() {
            return makeFullMap().entrySet();
        }
        
        protected boolean isAddSupported() {
            // Collection views don't support add operations.
            return false;
        }
        
        protected boolean isRemoveSupported() {
            // Entry set should only support remove if map does
            return isAddRemoveModifiable();
        }
        
        protected void resetFull() {
            TestMap.this.resetFull();
            collection = map.entrySet();
            TestMapEntrySet.this.confirmed = TestMap.this.confirmed.entrySet();
        }
        
        protected void resetEmpty() {
            TestMap.this.resetEmpty();
            collection = map.entrySet();
            TestMapEntrySet.this.confirmed = TestMap.this.confirmed.entrySet();
        }
        
        protected void verify() {
            super.verify();
            TestMap.this.verify();
        }
    }

 

    class TestMapKeySet extends TestSet {
        public TestMapKeySet() {
            super("");
        }
        protected Object[] getFullElements() {
            return getSampleKeys();
        }
        
        protected Object[] getOtherElements() {
            return getOtherKeys();
        }
        
        protected Set makeEmptySet() {
            return makeEmptyMap().keySet();
        }
        
        protected Set makeFullSet() {
            return makeFullMap().keySet();
        }
        
        protected boolean isAddSupported() {
            return false;
        }
        
        protected boolean isRemoveSupported() {
            return isAddRemoveModifiable();
        }
        
        protected void resetEmpty() {
            TestMap.this.resetEmpty();
            collection = map.keySet();
            TestMapKeySet.this.confirmed = TestMap.this.confirmed.keySet();
        }
        
        protected void resetFull() {
            TestMap.this.resetFull();
            collection = map.keySet();
            TestMapKeySet.this.confirmed = TestMap.this.confirmed.keySet();
        }
        
        protected void verify() {
            super.verify();
            TestMap.this.verify();
        }
    }


     
    class TestMapValues extends TestCollection {
        public TestMapValues() {
            
        }

        protected Object[] getFullElements() {
            return getSampleValues();
        }
        
        protected Object[] getOtherElements() {
            return getOtherValues();
        }
        
        protected Collection makeCollection() {
            return makeEmptyMap().values();
        }
        
        protected Collection makeFullCollection() {
            return makeFullMap().values();
        }
        
        protected boolean isAddSupported() {
            return false;
        }
        
        protected boolean isRemoveSupported() {
            return isAddRemoveModifiable();
        }

        protected boolean areEqualElementsDistinguishable() {
            // equal values are associated with different keys, so they are
            // distinguishable.  
            return true;
        }

        protected Collection makeConfirmedCollection() {
            // never gets called, reset methods are overridden
            return null;
        }
        
        protected Collection makeConfirmedFullCollection() {
            // never gets called, reset methods are overridden
            return null;
        }
        
        protected void resetFull() {
            TestMap.this.resetFull();
            collection = map.values();
            TestMapValues.this.confirmed = TestMap.this.confirmed.values();
        }
        
        protected void resetEmpty() {
            TestMap.this.resetEmpty();
            collection = map.values();
            TestMapValues.this.confirmed = TestMap.this.confirmed.values();
        }

        protected void verify() {
            super.verify();
            TestMap.this.verify();
        }

        // TODO: should test that a remove on the values collection view
        // removes the proper mapping and not just any mapping that may have
        // the value equal to the value returned from the values iterator.
    }


    /**
     *  Resets the {@link #map}, {@link #entrySet}, {@link #keySet},
     *  {@link #collectionValues} and {@link #confirmed} fields to empty.
     */
    protected void resetEmpty() {
        this.map = makeEmptyMap();
        views();
        this.confirmed = makeConfirmedMap();
    }


    /**
     *  Resets the {@link #map}, {@link #entrySet}, {@link #keySet},
     *  {@link #collectionValues} and {@link #confirmed} fields to full.
     */
    protected void resetFull() {
        this.map = makeFullMap();
        views();
        this.confirmed = makeConfirmedMap();
        Object[] k = getSampleKeys();
        Object[] v = getSampleValues();
        for (int i = 0; i < k.length; i++) {
            confirmed.put(k[i], v[i]);
        }
    }


    /**
     *  Resets the collection view fields.
     */
    private void views() {
        this.keySet = map.keySet();
        this.collectionValues = map.values();
        this.entrySet = map.entrySet();
    }


    /**
     *  Verifies that {@link #map} is still equal to {@link #confirmed}.
     *  This method checks that the map is equal to the HashMap, 
     *  <I>and</I> that the map's collection views are still equal to
     *  the HashMap's collection views.  An <Code>equals</Code> test
     *  is done on the maps and their collection views; their size and
     *  <Code>isEmpty</Code> results are compared; their hashCodes are
     *  compared; and <Code>containsAll</Code> tests are run on the 
     *  collection views.
     */
    protected void verify() {
        verifyMap();
        verifyEntrySet();
        verifyKeySet();
     
    }

    protected void verifyMap() {
        int size = confirmed.size();
        boolean empty = confirmed.isEmpty();
        assertEquals("Map should be same size as HashMap", 
                     size, map.size());
        assertEquals("Map should be empty if HashMap is", 
                     empty, map.isEmpty());
        assertEquals("hashCodes should be the same",
                     confirmed.hashCode(), map.hashCode());
        // this fails for LRUMap because confirmed.equals() somehow modifies
        // map, causing concurrent modification exceptions.
        //assertEquals("Map should still equal HashMap", confirmed, map);
        // this works though and performs the same verification:
        assertTrue("Map should still equal HashMap", map.equals(confirmed));
        // TODO: this should really be rexamined to figure out why LRU map
        // behaves like it does (the equals shouldn't modify since all accesses
        // by the confirmed collection should be through an iterator, thus not
        // causing LRUMap to change).
    }

    protected void verifyEntrySet() {
        int size = confirmed.size();
        boolean empty = confirmed.isEmpty();
        assertEquals("entrySet should be same size as HashMap's",
                     size, entrySet.size());
        assertEquals("entrySet should be empty if HashMap is", 
                     empty, entrySet.isEmpty());
        assertTrue("entrySet should contain all HashMap's elements",
                   entrySet.containsAll(confirmed.entrySet()));
        assertEquals("entrySet hashCodes should be the same", 
                     confirmed.entrySet().hashCode(), entrySet.hashCode());
        assertEquals("Map's entry set should still equal HashMap's", 
                     confirmed.entrySet(), entrySet);
    }

    protected void verifyKeySet() { 
        int size = confirmed.size();
        boolean empty = confirmed.isEmpty();
        assertEquals("keySet should be same size as HashMap's",
                     size, keySet.size());
        assertEquals("keySet should be empty if HashMap is", 
                     empty, keySet.isEmpty());
        assertTrue("keySet should contain all HashMap's elements",
                   keySet.containsAll(confirmed.keySet()));
        assertEquals("keySet hashCodes should be the same", 
                     confirmed.keySet().hashCode(), keySet.hashCode());
        assertEquals("Map's key set should still equal HashMap's",
                     confirmed.keySet(), keySet);
    }

}
