/*
 * completion.tsx
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


import React from "react";

import { WidgetProps } from "./react";

import './completion.css';

export interface CompletionItemViewProps extends WidgetProps {
  width: number;
  image?: string;
  idView: JSX.Element;
  title: string;
  htmlTitle?: boolean;
}

export const CompletionItemView: React.FC<CompletionItemViewProps> = props => {

  const className = ['pm-completion-item'].concat(props.classes || []).join(' ');
  const style: React.CSSProperties = {
    width: props.width + 'px',
    ...props.style,
  };

  return (
    <div className={className} style={style}>
      <div className={'pm-completion-item-type'}>
        <img className={'pm-block-border-color'} src={props.image} />
      </div>
      <div className={'pm-completion-item-summary'} style={{ width: props.width - 40 - 36 + 'px' }}>
        <div className={'pm-completion-item-id'}>
          {props.idView}
        </div>
        {props.htmlTitle ?
          <div className={'pm-completion-item-title'}
            dangerouslySetInnerHTML={{ __html: props.title || '' }}
          />
          :
          <div className={'pm-completion-item-title'}>
            {props.title}
          </div>
        }
      </div>
    </div>
  );

};


