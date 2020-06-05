/*
 * editor-images.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { EditorUIImages } from '../api/ui-images';

import copyImage from './images/copy.png';
import propertiesImage from './images/properties.png';
import removelinkImage from './images/removelink.png';
import runchunkImage from './images/runchunk.png';
import runprevchunksImage from './images/runprevchunks.png';
import searchImage from './images/search.png';
import heading1Image from './images/omni_insert/heading1.png';
import heading1DarkImage from './images/omni_insert/heading1_dark.png';
import heading2Image from './images/omni_insert/heading2.png';
import heading2DarkImage from './images/omni_insert/heading2_dark.png';
import heading3Image from './images/omni_insert/heading3.png';
import heading3DarkImage from './images/omni_insert/heading3_dark.png';
import bulletListImage from './images/omni_insert/bullet_list.png';
import bulletListDarkImage from './images/omni_insert/bullet_list_dark.png';
import orderedListImage from './images/omni_insert/ordered_list.png';
import orderedListDarkImage from './images/omni_insert/ordered_list_dark.png';
import blockquoteImage from './images/omni_insert/blockquote.png';
import blockquoteDarkImage from './images/omni_insert/blockquote_dark.png';

export function defaultEditorUIImages(): EditorUIImages {
  return {
    copy: copyImage,
    properties: propertiesImage,
    removelink: removelinkImage,
    runchunk: runchunkImage,
    runprevchunks: runprevchunksImage,
    search: searchImage,
    omni_insert: {
      heading1: heading1Image,
      heading1_dark: heading1DarkImage,
      heading2: heading2Image,
      heading2_dark: heading2DarkImage,
      heading3: heading3Image,
      heading3_dark: heading3DarkImage,
      bullet_list: bulletListImage,
      bullet_list_dark: bulletListDarkImage,
      ordered_list: orderedListImage,
      ordered_list_dark: orderedListDarkImage,
      blockquote: blockquoteImage,
      blockquote_dark: blockquoteDarkImage 
    }
  };
}
