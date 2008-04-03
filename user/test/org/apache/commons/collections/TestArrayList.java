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
// MODIFIED BY GOOGLE
package org.apache.commons.collections;
 import java.util.List;

/**
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @version $Id: TestArrayList.java,v 1.5.2.1 2004/05/22 12:14:05 scolebourne Exp $
 */
public abstract class TestArrayList extends TestList
{ 
  
 
    // GOOGLE
    protected List list = makeEmptyList();


    public void testNewArrayList()
    {
         assertTrue("New list is empty", list.isEmpty());
        assertEquals("New list has size zero", list.size(), 0);

      
        
    }

    public void testSearch()
    {
        list.add("First Item");
        list.add("Last Item");
        assertEquals("First item is 'First Item'", list.get(0), "First Item");
        assertEquals("Last Item is 'Last Item'", list.get(1), "Last Item");
    }

}
