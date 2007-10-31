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
  
import com.google.gwt.emultest.java.util.EmulTestBase;

/**
 * Tests base {@link java.lang.Object} methods and contracts.
 * <p>
 * To use, simply extend this class, and implement
 * the {@link #makeObject()} method.
 * <p>
 * If your {@link Object} fails one of these tests by design,
 * you may still use this base set of cases.  Simply override the
 * test case (method) your {@link Object} fails.
 *
 * @author Rodney Waldhoff
 * @version $Id: TestObject.java,v 1.13.2.1 2004/05/22 12:14:05 scolebourne Exp $
 */
public abstract class TestObject extends EmulTestBase {
   

    // current major release for Collections
    public static final int COLLECTIONS_MAJOR_VERSION = 2;

    /**
     * This constant makes it possible for TestMap (and other subclasses,
     * if necessary) to automatically check CVS for a versionX copy of a
     * Serialized object, so we can make sure that compatibility is maintained.
     * See, for example, TestMap.getCanonicalFullMapName(Map map).
     * Subclasses can override this variable, indicating compatibility
     * with earlier Collections versions.
     * Defaults to 1, the earliest Collections version.  (Note: some
     * collections did not even exist in this version).
     * 
     * @return 1
     */
    public int getCompatibilityVersion() {
        return 1;
    }

    /**
     * Return a new, empty {@link Object} to used for testing.
     */
    public abstract Object makeObject();

    public void testObjectEqualsSelf() {
        Object obj = makeObject();
        assertEquals("A Object should equal itself",obj,obj);
    }

    public void testObjectHashCodeEqualsSelfHashCode() {
        Object obj = makeObject();
        assertEquals("hashCode should be repeatable",obj.hashCode(),obj.hashCode());
    }

    public void testObjectHashCodeEqualsContract() {
        Object obj1 = makeObject();
        if(obj1.equals(obj1)) {
            assertEquals("[1] When two objects are equal, their hashCodes should be also.",obj1.hashCode(),obj1.hashCode());
        }
        Object obj2 = makeObject();
        if(obj1.equals(obj2)) {
            assertEquals("[2] When two objects are equal, their hashCodes should be also.",obj1.hashCode(),obj2.hashCode());
            assertTrue("When obj1.equals(obj2) is true, then obj2.equals(obj1) should also be true", obj2.equals(obj1));
        }
    }

    

    public String getCanonicalEmptyCollectionName(Object object) {
        StringBuffer retval = new StringBuffer();
        retval.append("data/test/");
        String colName = object.getClass().getName();
        
        colName = colName.substring(colName.lastIndexOf(".")+1,colName.length());
        retval.append(colName);
        retval.append(".emptyCollection.version");
        retval.append(getCompatibilityVersion());
        retval.append(".obj");
        return retval.toString();
    }

    public String getCanonicalFullCollectionName(Object object) {
        StringBuffer retval = new StringBuffer();
        retval.append("data/test/");
        String colName = object.getClass().getName();
        colName = colName.substring(colName.lastIndexOf(".")+1,colName.length());
        retval.append(colName);
        retval.append(".fullCollection.version");
        retval.append(getCompatibilityVersion());
        retval.append(".obj");
        return retval.toString();
    }

    /**
     * Override this method if a subclass is testing a 
     * Collections that cannot serialize an "empty" Collection
     * (e.g. Comparators have no contents)
     * 
     * @return true
     */
    public boolean supportsEmptyCollections() {
        return true;
    }

    /**
     * Override this method if a subclass is testing a 
     * Collections that cannot serialize a "full" Collection
     * (e.g. Comparators have no contents)
     * 
     * @return true
     */
    public boolean supportsFullCollections() {
        return true;
    }


  
}
