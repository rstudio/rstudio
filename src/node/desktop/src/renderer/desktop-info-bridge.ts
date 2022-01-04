/*
 * desktop-info-bridge.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function getDesktopInfoBridge() {
  if (process.platform === 'win32') {
    return {
      platform: '',
      version: '',
      scrollingCompensationType: '',
      fixedWidthFontList: `BIZ UDGothic
BIZ UDMincho Medium
Cascadia Code
Cascadia Code ExtraLight
Cascadia Code Light
Cascadia Code SemiBold
Cascadia Code SemiLight
Cascadia Mono
Cascadia Mono ExtraLight
Cascadia Mono Light
Cascadia Mono SemiBold
Cascadia Mono SemiLight
Consolas
Courier
Courier New
Lucida Console
Lucida Sans Typewriter
MingLiU-ExtB
MingLiU_HKSCS-ExtB
MS Gothic
MS Mincho
NSimSun
OCR A Extended
SimSun
SimSun-ExtB
UD Digi Kyokasho N-B
UD Digi Kyokasho N-R`,
      fixedWidthFont: 'Lucida Console',
      proportionalFont: 'Segoe UI',
      desktopSynctexViewer: '',
      zoomLevel: 1.0,
      chromiumDevtoolsPort: 0,
    };
  } else {
    return {
      platform: '',
      version: '',
      scrollingCompensationType: '',
      fixedWidthFontList: `AndaleMono
AppleBraille-Outline6Dot
AppleBraille-Outline8Dot
AppleBraille-Pinpoint6Dot
AppleBraille-Pinpoint8Dot
AppleBraille
AppleColorEmoji
Courier
Courier-Oblique
Courier-Bold
Courier-BoldOblique
CourierNewPSMT
CourierNewPS-ItalicMT
CourierNewPS-BoldMT
CourierNewPS-BoldItalicMT
Menlo-Regular
Menlo-Italic
Menlo-Bold
Menlo-BoldItalic
Monaco
PTMono-Regular
PTMono-Bold`,
      fixedWidthFont: 'Monaco',
      proportionalFont: 'Lucida Grande',
      desktopSynctexViewer: '',
      zoomLevel: 1.0,
      chromiumDevtoolsPort: 0,
    };
  }
}
