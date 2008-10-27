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
package java.lang;

import java.io.IOException;

/**
 * See <a
 * href="http://java.sun.com/javase/6/docs/api/java/lang/Appendable.html">the
 * official Java API doc</a> for details.
 */
public interface Appendable {

  Appendable append(char c) throws IOException;

  Appendable append(CharSequence charSquence) throws IOException;

  Appendable append(CharSequence charSquence, int start, int end)
      throws IOException;
}
