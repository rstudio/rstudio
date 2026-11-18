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
set(RSTUDIO_RIG_VERSION "0.11.1")

# SHA-256 digests published with https://github.com/r-lib/rig/releases/tag/v0.11.1
set(RIG_SHA256_macos_arm64   "ee9b1eceaa99382e2b88a69e14ba4c178d14dcb5c64241bc58ef01d1750afe4b")
set(RIG_SHA256_macos_x86_64  "8434e9c11205c6606aa0a4fd619a2bb0dafb25c951ad2f3d9dea5f4fc48a579c")
set(RIG_SHA256_linux_x86_64  "757b5b7886b09a8a97f35ab6e71546b68044d323ffbcaa68adcbe818409af156")
set(RIG_SHA256_linux_aarch64 "2b796885bc0398ae7700f8c95847a855e29c359769f31e1993569274316fb508")
set(RIG_SHA256_windows_x86_64 "a0ce65f2afb2f016488523818d14c4596080c61f32d0dab4c123c7f5e1db8910")
set(RIG_SHA256_windows_arm64  "fd052a46c0cca85a5d125a437d576b6e578d985387a699d7271c7ac35c982af0")
