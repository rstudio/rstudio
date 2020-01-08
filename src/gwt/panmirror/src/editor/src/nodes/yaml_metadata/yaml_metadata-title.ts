import { Node as ProsemirrorNode } from 'prosemirror-model';
import { ReplaceStep } from 'prosemirror-transform';
import { Plugin, PluginKey, Transaction, EditorState } from 'prosemirror-state';

import { findTopLevelBodyNodes } from '../../api/node';
import { RangeStep } from '../../api/transaction';

const plugin = new PluginKey<string>('yaml-metadata-title');

export function yamlMetadataTitlePlugin() {
  return new Plugin<string>({
    key: plugin,
    state: {
      init(_config, state: EditorState) {
        return titleFromState(state);
      },

      apply(tr: Transaction, title: string, oldState: EditorState, newState: EditorState) {
        if (!tr.docChanged) {
          return title;
        }

        // if the transaction included a replace within yaml_metadata, then recompute
        const isYamlEdit = tr.steps.some(step => {
          if (step instanceof ReplaceStep) {
            const rangeStep = step as RangeStep;
            let yamlEdit = false;
            oldState.doc.nodesBetween(rangeStep.from, rangeStep.to, node => {
              if (isYamlMetadataNode(node)) {
                yamlEdit = true;
                return false; // terminate iteration since we found a yaml node
              }
            });
            return yamlEdit;
          } else {
            return false; // not a replace step
          }
        });

        if (isYamlEdit) {
          return titleFromState(newState);
        } else {
          return title;
        }
      },
    },
  });
}

export function getTitle(state: EditorState) {
  return plugin.getState(state);
}

export function setTitle(state: EditorState, title: string) {
  // alias schema
  const schema = state.schema;

  // no-op if yaml_metadata isn't available
  if (!schema.nodes.yaml_metadata) {
    return;
  }

  // create transaction
  const tr = state.tr;

  // escape quotes in title then build the title line
  const escapedTitle = title.replace(/"/g, `\\"`);
  const titleLine = `\ntitle: "${escapedTitle}"\n`;

  // attempt to update existing title
  const yamlNodes = yamlMetadataNodes(tr.doc);
  let foundTitle = false;
  for (const yaml of yamlNodes) {
    const titleMatch = yaml.node.textContent.match(kTitleRegex);
    if (titleMatch) {
      const updatedMetadata = yaml.node.textContent.replace(kTitleRegex, titleLine);
      const updatedNode = schema.nodes.yaml_metadata.createAndFill({}, schema.text(updatedMetadata));
      tr.replaceRangeWith(yaml.pos, yaml.pos + yaml.node.nodeSize, updatedNode);
      foundTitle = true;
      break;
    }
  }

  // if we didn't find a title then inject one at the top
  if (!foundTitle) {
    const yamlText = schema.text(`---${titleLine}---`);
    const yamlNode = schema.nodes.yaml_metadata.create({}, yamlText);
    tr.insert(1, yamlNode);
  }

  // return transaction
  return tr;
}

const kTitleRegex = /\ntitle:(.*)\n/;

function titleFromState(state: EditorState) {
  const yamlNodes = yamlMetadataNodes(state.doc);
  for (const yaml of yamlNodes) {
    const titleMatch = yaml.node.textContent.match(kTitleRegex);
    if (titleMatch) {
      let title = titleMatch[1].trim();
      title = title.replace(/^["']|["']$/g, '');
      title = title.replace(/\\"/g, '"');
      title = title.replace(/''/g, "'");
      return title;
    }
  }
  return '';
}

function yamlMetadataNodes(doc: ProsemirrorNode) {
  return findTopLevelBodyNodes(doc, isYamlMetadataNode);
}

function isYamlMetadataNode(node: ProsemirrorNode) {
  return node.type === node.type.schema.nodes.yaml_metadata;
}
