/*
 * insert_symbol-emoji-skintone.ts
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

import React from 'react';

import { TextButton } from '../../api/widgets/button';

import { SkinTone, emojiFromAlias, emojiWithSkinTonePreference, emojiForAllSkinTones, Emoji } from '../../api/emoji';
import { SymbolPreferencesProps } from './insert_symbol-dataprovider';

import './insert_symbol-emoji-skintone.css';

export const SymbolEmojiPreferencesPanel = React.forwardRef<any, SymbolPreferencesProps>((props, ref) => {
  // The currently selected skin tone
  const [selectedSkinTone, setSelectedSkinTone] = React.useState<SkinTone>(props.prefs.emojiSkinTone());

  // Shows and hides the selection UI
  const [selectSkinTone, setSelectSkinTone] = React.useState<boolean>(false);

  const previewEmoji = React.useMemo(() => {
    const emojiRaw = emojiFromAlias('hand');
    return emojiWithSkinTonePreference(emojiRaw!, selectedSkinTone);
  }, [selectedSkinTone]);

  const previewEmojiWithSkinTones = React.useMemo(() => {
    return emojiForAllSkinTones(previewEmoji);
  }, [previewEmoji]);

  // If the user navigates or changes the symbol, close the picker
  React.useEffect(() => {
    setSelectSkinTone(false);
  }, [props.selectedSymbolIndex]);

  // Support navigation within the panel
  const handleKeyDown = (event: React.KeyboardEvent) => {
    switch (event.key) {
      case 'ArrowRight':
        if (selectSkinTone) {
          setSelectedSkinTone(nextSkinTone());
        }
        break;

      case 'ArrowLeft':
        if (selectSkinTone) {
          setSelectedSkinTone(previousSkinTone());
        }
        break;

      case 'Enter':
      case ' ':
        commitSkinTonePreference(selectedSkinTone);
        setSelectSkinTone(!selectSkinTone);
        event.preventDefault();
        break;
    }
  };

  const handleClick = (skinTone: SkinTone) => {
    return (event: React.MouseEvent<HTMLElement>) => {
      commitSkinTonePreference(skinTone);
      setSelectSkinTone(false);
    };
  };

  const handleButtonClick = () => {
    setSelectSkinTone(true);
  };

  function nextSkinTone(): SkinTone {
    const index = getCurrentSkinToneIndex();
    if (index + 1 < previewEmojiWithSkinTones.length) {
      return previewEmojiWithSkinTones[index + 1].skinTone;
    }
    return selectedSkinTone;
  }

  function previousSkinTone(): SkinTone {
    const index = getCurrentSkinToneIndex();
    if (index - 1 >= 0) {
      return previewEmojiWithSkinTones[index - 1].skinTone;
    }
    return selectedSkinTone;
  }

  function getCurrentSkinToneIndex(): number {
    const index = previewEmojiWithSkinTones.findIndex(emoji => emoji.skinTone === selectedSkinTone);
    return Math.max(Math.min(index, previewEmojiWithSkinTones.length), 0);
  }

  function skinToneSelectedClass(emoji: Emoji): string {
    if (
      selectedSkinTone === emoji.skinTone ||
      (selectedSkinTone === SkinTone.None && emoji.skinTone === SkinTone.Default)
    ) {
      return 'pm-emoji-skintone-picker-item-selected pm-input-button';
    }
    return '';
  }

  function commitSkinTonePreference(skinTone: SkinTone) {
    setSelectedSkinTone(skinTone);
    props.prefs.setEmojiSkinTone(skinTone);
    props.onPreferencesChanged();
  }

  const skinTonePicker = () => {
    return (
      <div>
        <div className="pm-emoji-skintone-picker pm-input-button">
          {previewEmojiWithSkinTones.map(emoji => {
            return (
              <div
                key={emoji.emoji}
                className={`pm-emoji-skintone-picker-item ${skinToneSelectedClass(emoji)}`}
                onClick={handleClick(emoji.skinTone)}
              >
                {emoji.emoji}
              </div>
            );
          })}
        </div>
        <div className="pm-emoji-skintone-picker-label pm-light-text-color">
          {props.context.translateText('Preferred skin tone')}
        </div>
      </div>
    );
  };

  const currentSkinToneButton = () => {
    return (
      <TextButton
        classes={['pm-emoji-skintone-picker-button', 'pm-light-text-color']}
        title={
          selectedSkinTone === SkinTone.None
            ? props.context.translateText(`${previewEmoji.emoji} Skin tone`)
            : previewEmoji.emoji
        }
        onClick={handleButtonClick}
        tabIndex={-1}
      />
    );
  };

  return (
    <div className="pm-emoji-skintone-picker-container" onKeyDown={handleKeyDown} tabIndex={0} ref={ref}>
      {selectSkinTone ? skinTonePicker() : currentSkinToneButton()}
    </div>
  );
});
