/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.shell;

import com.google.gwt.dev.shell.BrowserChannel.RemoteObjectRef;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of references to remote objects.  When the objects are no longer
 * needed, their ids are returned in {@link #getRefIdsForCleanup()}.
 * 
 * @param <T> subtype of RemoteObjectRef contained in this table
 */
public class RemoteObjectTable<T extends RemoteObjectRef> {

  /**
   * This maps References to RemoteObjectRefs back to the original refId.
   * Because we need the refId of the RemoteObjectRef after it's been
   * garbage-collected, this state must be stored externally.
   */
  private final Map<Reference<T>, Integer> idFromRemoteObject;
  
  /**
   * This accumulates remote objects that are no longer referenced on this side
   * of the channel.
   */
  private final ReferenceQueue<T> refQueue;

  /**
   * This map associates a remote object ID with a Reference to the
   * RemoteObjectRef that currently represents that id.
   */
  private final Map<Integer, Reference<T>> remoteObjectFromId;

  /**
   * Create a new RemoteObjectTable.
   */
  public RemoteObjectTable() {
    refQueue = new ReferenceQueue<T>();
    remoteObjectFromId = new HashMap<Integer, Reference<T>>();
    idFromRemoteObject = new IdentityHashMap<Reference<T>, Integer>();
  }

  /**
   * @return the set of remote object reference IDs that should be freed.
   */
  public synchronized Set<Integer> getRefIdsForCleanup() {
    // Access to these objects is inherently synchronous
    Map<Integer, Reference<T>> objectMap = remoteObjectFromId;
    Map<Reference<T>, Integer> refIdMap = idFromRemoteObject;
    Set<Integer> toReturn = new HashSet<Integer>();

    // Find all refIds associated with previous garbage collection cycles
    Reference<? extends RemoteObjectRef> ref;
    while ((ref = refQueue.poll()) != null) {
      Integer i = refIdMap.remove(ref);
      assert i != null;
      toReturn.add(i);
    }

    /*
     * Check for liveness. This is necessary because the last reference to a
     * RemoteObjectRef could have been cleared and a new reference to that refId
     * created before this method has been called.
     */
    for (Iterator<Integer> i = toReturn.iterator(); i.hasNext();) {
      Integer refId = i.next();
      if (objectMap.containsKey(refId)) {
        if (objectMap.get(refId).get() != null) {
          i.remove();
        } else {
          objectMap.remove(refId);
        }
      }
    }

    return toReturn;
  }

  /**
   * Obtain the RemoteObjectRef that is currently in use to act as a proxy for
   * the given remote object ID.
   * 
   * @return the RemoteObjectRef or null if the ID is not currently in use
   */
  public synchronized T getRemoteObjectRef(int refId) {
    if (remoteObjectFromId.containsKey(refId)) {
      Reference<T> ref = remoteObjectFromId.get(refId);
      T toReturn = ref.get();
      if (toReturn != null) {
        return toReturn;
      }
    }
    return null;
  }

  /**
   * Check to see if this ID does not already exist.
   * 
   * @param refId reference ID to check
   * @return true if this ID is not currently in use
   */
  public synchronized boolean isNewObjectId(int refId) {
    return !remoteObjectFromId.containsKey(refId)
    || (remoteObjectFromId.get(refId).get() == null);
  }

  /**
   * Store a remote object reference in the table.
   * 
   * @param refId
   * @param remoteObjectRef
   */
  public synchronized void putRemoteObjectRef(int refId,
      T remoteObjectRef) {
    Reference<T> ref = new WeakReference<T>(remoteObjectRef, refQueue);
    remoteObjectFromId.put(refId, ref);
    idFromRemoteObject.put(ref, refId);
  }
}
