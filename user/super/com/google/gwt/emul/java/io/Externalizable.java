/*
 * Copyright 2017 Google Inc.
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

package java.io;

/**
 * A version of java.io.Externalizable to be able to compile shared Java code that uses it.
 * <p>
 * Externalization is not supported in GWT. Shared classes that implement the interface can
 * mark the corresponding methods as {@code @GwtIncompatible} but the interface still needs to
 * exist for the code to compile.
 * <p>
 * The implementation has to mark all the overriding methods as {@code @GwtIncompatible}.
 */
public interface Externalizable extends Serializable {
  // public void readExternal(ObjectInput input);
  // public void writeExternal(ObjectOutput output);
}