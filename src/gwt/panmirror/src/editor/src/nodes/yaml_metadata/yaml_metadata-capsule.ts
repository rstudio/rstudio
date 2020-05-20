/*
 * yaml_metadata-capsule.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

import { Schema } from 'prosemirror-model';

import { kYamlBlocksRegex } from '../../api/yaml';
import { ProsemirrorWriter } from '../../api/pandoc';
import { uuidv4 } from '../../api/util';
import {
  encodedBlockCapsuleRegex,
  blockCapsuleSourceWithoutPrefix,
  PandocBlockCapsule,
  blockCapsuleParagraphTokenHandler,
  blockCapsuleTextHandler,
} from '../../api/pandoc_capsule';

export function yamlMetadataBlockCapsuleFilter() {
  const kYamlMetadataCapsuleType = 'E1819605-0ACD-4FAE-8B99-9C1B7BD7C0F1'.toLowerCase();

  return {
    type: kYamlMetadataCapsuleType,

    match: kYamlBlocksRegex,

    // add a newline to ensure that if the metadata block has text right
    // below it we still end up in our own pandoc paragarph block
    enclose: (capsuleText: string) => capsuleText + '\n',
    // globally replace any instances of our block capsule found in text
    handleText: blockCapsuleTextHandler(
      kYamlMetadataCapsuleType,
      encodedBlockCapsuleRegex(undefined, '\\n', 'gm'),
      (text: string) => text.substring(0, text.length - 1), // trim off newline
    ),

    // we are looking for a paragraph token consisting entirely of a
    // block capsule of our type. if find that then return the block
    // capsule text
    handleToken: blockCapsuleParagraphTokenHandler(kYamlMetadataCapsuleType),

    // write as yaml_metadata
    writeNode: (schema: Schema, writer: ProsemirrorWriter, capsule: PandocBlockCapsule) => {
      writer.openNode(schema.nodes.yaml_metadata, { navigation_id: uuidv4() });
      // write the lines w/o the source-level prefix
      writer.writeText(blockCapsuleSourceWithoutPrefix(capsule.source, capsule.prefix));
      writer.closeNode();
    },
  };
}
