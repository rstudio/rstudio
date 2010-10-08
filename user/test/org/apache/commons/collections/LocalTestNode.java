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

/**
* Class LocalTestNode
*
* a helper class for TestDoubleOrderedMap
*
* @author Marc Johnson (marcj at users dot sourceforge dot net)
*/
class LocalTestNode implements Comparable {

    private Comparable key;
    private Comparable value;

    /**
     * construct a LocalTestNode
     *
     * @param key value used to create the key and value
     */
    LocalTestNode(final int key) {
        this.key   = new Integer(key);
        this.value = String.valueOf(key);
    }

    /**
     * @param key the unique key associated with the current node.
     */
    void setKey(Comparable key) {
        this.key = key;
    }

    /**
     * Returns the unique key associated with the current node.
     */
    Comparable getKey() {
        return key;
    }

    /**
     * @param value the unique value associated with the current node.
     */
    void setValue(Comparable value) {
        this.value = value;
    }

    /**
     * Returns the unique value associated with the current node.
     */
    Comparable getValue() {
        return value;
    }

    /**
     * Method compareTo
     *
     * @param o
     *
     * @return int
     */
    public int compareTo(Object o) {

        LocalTestNode other = (LocalTestNode) o;
        int           rval  = getKey().compareTo(other.getKey());

        if (rval == 0) {
            rval = getValue().compareTo(other.getValue());
        }

        return rval;
    }

    /**
     * Method equals
     *
     * @param o
     *
     * @return true if equal
     */
    public boolean equals(Object o) {

        if (o == null) {
            return false;
        }


        if(!(getClass().getName().equals(o.getClass().getName()))){
            return false;
        }
        LocalTestNode node = (LocalTestNode) o;

        return (getKey().equals(node.getKey())
                && getValue().equals(node.getValue()));
    }

    /**
     * @return hash code
     */
    public int hashCode() {
        return getKey().hashCode() ^ getValue().hashCode();
    }
}
