#
# rig.cmake
#
# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# The bundled rig release. Keep SessionRig.R's pinned version in sync.
set(RSTUDIO_RIG_VERSION "0.11.0")

# SHA-256 digests published with https://github.com/r-lib/rig/releases/tag/v0.11.0
set(RIG_SHA256_macos_arm64   "abf557e9ed51a4f64b6584fee3451fcad2b4d2d091a1ae46dc19cbe10ac6d8b0")
set(RIG_SHA256_macos_x86_64  "e6683110d04eb88ecf57a0c8862a6806b9ec367e23fff57b16e55e383ad57822")
set(RIG_SHA256_linux_x86_64  "27e063df84b6dadb325fa96d13951676b44b69103bb4237b061518fbf564fcf1")
set(RIG_SHA256_linux_aarch64 "bb0e74fd5a8c0b7138b417fd71b0ed166bf144f9b713888ef3bdc08b2f1396f6")
set(RIG_SHA256_windows_x86_64 "81218ebc88d24f100686106542ac487fa9b2755f905539b450e0eb87a5364f9d")
set(RIG_SHA256_windows_arm64  "39dcf76cb8b6d4973ef6cec4dde63fea2e33c1b9c48762a315da9a4091691baa")
