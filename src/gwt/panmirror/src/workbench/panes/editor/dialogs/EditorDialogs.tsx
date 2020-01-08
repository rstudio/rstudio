import React from 'react';

import { t } from 'i18n';

import { AlertDialogProps, AlertDialog } from 'workbench/widgets/dialog/AlertDialog';

import { EditorDialogEditLink, EditorDialogEditLinkProps, defaultEditLinkProps } from './EditorDialogEditLink';
import { EditorDialogEditImage, EditorDialogEditImageProps, defaultEditImageProps } from './EditorDialogEditImage';
import { EditorDialogEditAttr, EditorDialogEditAttrProps, defaultEditAttrProps } from './EditorDialogEditAttr';
import {
  EditorDialogEditOrderedList,
  EditorDialogEditOrderedListProps,
  defaultEditOrderedListProps,
} from './EditorDialogEditOrderedList';
import {
  EditorDialogInsertTable,
  EditorDialogInsertTableProps,
  defaultInsertTableProps,
} from './EditorDialogInsertTable';
import {
  EditorDialogInsertCitationProps,
  defaultInsertCitationProps,
  EditorDialogInsertCitation,
} from './EditorDialogInsertCitation';
import { EditorDialogEditRawProps, defaultEditRawProps, EditorDialogEditRaw } from './EditorDialogEditRaw';

import {
  LinkProps,
  LinkEditResult,
  ImageProps,
  ImageEditResult,
  OrderedListProps,
  OrderedListEditResult,
  AttrProps,
  AttrEditResult,
  AlertType,
  InsertTableResult,
  InsertCitationResult,
  RawFormatResult as EditRawResult,
  RawFormatProps as EditRawProps,
} from 'editor/src/api/ui';

import { pandocAttrAvailable } from 'editor/src/api/pandoc_attr';
import { ListCapabilities } from 'editor/src/api/list';
import { LinkTargets, LinkCapabilities } from 'editor/src/api/link';

export interface EditorDialogsState {
  alert: AlertDialogProps;
  editLink: EditorDialogEditLinkProps;
  editImage: EditorDialogEditImageProps;
  editAttr: EditorDialogEditAttrProps;
  editOrderedList: EditorDialogEditOrderedListProps;
  editSpan: EditorDialogEditAttrProps;
  editDiv: EditorDialogEditAttrProps;
  editRaw: EditorDialogEditRawProps;
  insertTable: EditorDialogInsertTableProps;
  insertCitation: EditorDialogInsertCitationProps;
}

export default class EditorDialogs extends React.Component<Readonly<{}>, EditorDialogsState> {
  constructor(props: Readonly<{}>) {
    super(props);
    this.state = {
      alert: defaultAlertProps(),
      editLink: defaultEditLinkProps(),
      editAttr: defaultEditAttrProps(),
      editImage: defaultEditImageProps(),
      editOrderedList: defaultEditOrderedListProps(),
      editSpan: defaultEditAttrProps(),
      editDiv: defaultEditAttrProps(),
      editRaw: defaultEditRawProps(),
      insertTable: defaultInsertTableProps(),
      insertCitation: defaultInsertCitationProps(),
    };
  }

  public render() {
    return (
      <div>
        <AlertDialog {...this.state.alert} />
        <EditorDialogEditLink {...this.state.editLink} />
        <EditorDialogEditAttr {...this.state.editAttr} />
        <EditorDialogEditImage {...this.state.editImage} />
        <EditorDialogEditOrderedList {...this.state.editOrderedList} />
        <EditorDialogEditAttr {...this.state.editSpan} />
        <EditorDialogEditAttr {...this.state.editDiv} />
        <EditorDialogEditRaw {...this.state.editRaw} />
        <EditorDialogInsertTable {...this.state.insertTable} />
        <EditorDialogInsertCitation {...this.state.insertCitation} />
      </div>
    );
  }

  public alert(message: string, title?: string, type = AlertType.Info): Promise<void> {
    return new Promise(resolve => {
      this.setState({
        alert: {
          isOpen: true,
          message,
          title: title || '',
          type,
          onClosed: () => {
            this.setState({ alert: { ...this.state.alert, isOpen: false } });
            resolve();
          },
        },
      });
    });
  }

  public editLink(
    link: LinkProps,
    targets: LinkTargets,
    capabilities: LinkCapabilities,
  ): Promise<LinkEditResult | null> {
    return new Promise(resolve => {
      this.setState({
        editLink: {
          isOpen: true,
          capabilities,
          link,
          targets,
          onClosed: (result: LinkEditResult | null) => {
            this.setState({ editLink: { ...this.state.editLink, isOpen: false } });
            resolve(result);
          },
        },
      });
    });
  }

  public editImage(image: ImageProps, editAttributes: boolean): Promise<ImageEditResult | null> {
    return new Promise(resolve => {
      this.setState({
        editImage: {
          isOpen: true,
          image,
          editAttributes,
          onClosed: (result: ImageEditResult | null) => {
            this.setState({ editImage: { ...this.state.editImage, isOpen: false } });
            resolve(result);
          },
        },
      });
    });
  }

  public editOrderedList(
    list: OrderedListProps,
    capabilities: ListCapabilities,
  ): Promise<OrderedListEditResult | null> {
    return new Promise(resolve => {
      this.setState({
        editOrderedList: {
          isOpen: true,
          list,
          capabilities,
          onClosed: (result: OrderedListProps | null) => {
            this.setState({ editOrderedList: { ...this.state.editOrderedList, isOpen: false } });
            resolve(result);
          },
        },
      });
    });
  }

  public editAttr(attr: AttrProps): Promise<AttrEditResult | null> {
    return new Promise(resolve => {
      this.setState({
        editAttr: {
          isOpen: true,
          attr,
          onClosed: (result: AttrEditResult | null) => {
            this.setState({ editAttr: { ...this.state.editAttr, isOpen: false } });
            resolve(result);
          },
        },
      });
    });
  }

  public editSpan(attr: AttrProps): Promise<AttrEditResult | null> {
    return new Promise(resolve => {
      this.setState({
        editSpan: {
          isOpen: true,
          attr,
          removeEnabled: true,
          caption: t('edit_span_dialog_caption'),
          onClosed: (result: AttrEditResult | null) => {
            this.setState({ editSpan: { ...this.state.editSpan, isOpen: false } });
            resolve(result);
          },
        },
      });
    });
  }

  public editDiv(attr: AttrProps): Promise<AttrEditResult | null> {
    return new Promise(resolve => {
      this.setState({
        editDiv: {
          isOpen: true,
          attr,
          removeEnabled: pandocAttrAvailable(attr),
          caption: t('edit_div_dialog_caption'),
          onClosed: (result: AttrEditResult | null) => {
            this.setState({ editDiv: { ...this.state.editDiv, isOpen: false } });
            resolve(result);
          },
        },
      });
    });
  }

  public editRawInline(raw: EditRawProps): Promise<EditRawResult | null> {
    return new Promise(resolve => {
      this.setState({
        editRaw: {
          isOpen: true,
          raw,
          onClosed: (result: EditRawResult | null) => {
            this.setState({ editRaw: { ...this.state.editRaw, isOpen: false } });
            resolve(result);
          },
        },
      });
    });
  }

  public editRawBlock(raw: EditRawProps): Promise<EditRawResult | null> {
    return new Promise(resolve => {
      this.setState({
        editRaw: {
          isOpen: true,
          minRows: 10,
          raw,
          onClosed: (result: EditRawResult | null) => {
            this.setState({ editRaw: { ...this.state.editRaw, isOpen: false } });
            resolve(result);
          },
        },
      });
    });
  }

  public insertTable(): Promise<InsertTableResult | null> {
    return new Promise(resolve => {
      this.setState({
        insertTable: {
          isOpen: true,
          onClosed: (result: InsertTableResult | null) => {
            this.setState({ insertTable: { ...this.state.insertTable, isOpen: false } });
            resolve(result);
          },
        },
      });
    });
  }

  public insertCitation(): Promise<InsertCitationResult | null> {
    return new Promise(resolve => {
      this.setState({
        insertCitation: {
          isOpen: true,
          onClosed: (result: InsertCitationResult | null) => {
            this.setState({ insertCitation: { ...this.state.insertCitation, isOpen: false } });
            resolve(result);
          },
        },
      });
    });
  }
}

function defaultAlertProps(): AlertDialogProps {
  return {
    title: '',
    message: '',
    type: AlertType.Info,
    isOpen: false,
    onClosed: () => {
      /* */
    },
  };
}
