/*
 * Copyright 2012 Google Inc.
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
package elemental.html;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div><strong>`DRAFT</strong> <div>This page is not complete.</div>
</div>
<p>The <code>DirectoryReaderSync</code> interface of the <a title="en/DOM/File_API/File_System_API" rel="internal" href="https://developer.mozilla.org/en/DOM/File_API/File_System_API">FileSystem API</a> lets a user list files and directories in a directory.</p>
  */
public interface DirectoryReaderSync {

  EntryArraySync readEntries();
}
