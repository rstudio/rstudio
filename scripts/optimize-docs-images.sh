#!/usr/bin/env bash
#
# optimize-docs-images.sh
#
# Shrinks PNG screenshots in the user guide so they don't bloat the repository.
# Each image is converted to sRGB, stripped of metadata, capped at MAX_WIDTH
# pixels wide, and reduced to a 256-colour palette. Images that lose too much
# fidelity as a palette (photos, gradients) stay truecolor, and an image is
# only rewritten when the result is at least 10% smaller. Images that are
# already optimized are skipped, so the script is safe to re-run.
#
# Usage:
#
#    scripts/optimize-docs-images.sh [file.png ...]
#
# With no arguments, all tracked PNGs under docs/user/rstudio are processed.
# Requires ImageMagick 7 (magick) built with lcms.
#
# Environment:
#
#    MAX_WIDTH     Maximum width, in pixels (default: 2000)
#    MIN_PSNR      Minimum PSNR, in dB, for the palette version (default: 40)
#    SRGB_PROFILE  Path to an sRGB ICC profile

set -euo pipefail

MAX_WIDTH="${MAX_WIDTH:-2000}"
MIN_PSNR="${MIN_PSNR:-40}"

if ! command -v magick > /dev/null 2>&1; then
   echo "error: ImageMagick 7 (magick) is required" >&2
   exit 1
fi

# Screenshots usually embed the profile of the display they were taken on.
# Dropping it without converting would shift colours in the browser.
if [ -z "${SRGB_PROFILE:-}" ]; then
   for candidate in \
      "/System/Library/ColorSync/Profiles/sRGB Profile.icc" \
      "/usr/share/color/icc/colord/sRGB.icc" \
      "/usr/share/color/icc/sRGB.icc"
   do
      if [ -f "$candidate" ]; then
         SRGB_PROFILE="$candidate"
         break
      fi
   done
fi

if [ ! -f "${SRGB_PROFILE:-}" ]; then
   echo "error: no sRGB ICC profile found; set SRGB_PROFILE" >&2
   exit 1
fi

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

if [ "$#" -gt 0 ]; then
   printf '%s\n' "$@" > "$workdir/files"
else
   git -C "$root" ls-files 'docs/user/rstudio/**/*.png' | sed "s|^|$root/|" > "$workdir/files"
fi

# Leave out the timestamp chunks ImageMagick writes by default, so the same
# input always produces the same bytes.
png_options=(-define png:compression-level=9 -define png:exclude-chunks=date,time)

changed=0
before=0
after=0

while IFS= read -r file; do

   truecolor="$workdir/truecolor.png"
   palette="$workdir/palette.png"

   profile="$(magick identify -format '%[profile:icc]' "$file" 2> /dev/null)"
   width="$(magick identify -format '%w' "$file" 2> /dev/null)"

   # Already optimized. Quantizing again is not a no-op: ImageMagick merges
   # more colours on each pass, so skip rather than degrade the image.
   # (Counting colours is slow, so only do it when the cheap checks pass.)
   if [ -z "$profile" ] && [ "$width" -le "$MAX_WIDTH" ]; then
      colors="$(magick identify -format '%k' "$file" 2> /dev/null)"
      if [ "$colors" -le 256 ]; then
         size="$(wc -c < "$file" | tr -d ' ')"
         before=$((before + size))
         after=$((after + size))
         continue
      fi
   fi

   if [ -n "$profile" ]; then
      magick "$file" -profile "$SRGB_PROFILE" -strip -resize "${MAX_WIDTH}x>" -depth 8 "${png_options[@]}" "$truecolor"
   else
      magick "$file" -strip -resize "${MAX_WIDTH}x>" -depth 8 "${png_options[@]}" "$truecolor"
   fi

   # No dithering: it adds noise to the flat regions that dominate screenshots.
   magick "$truecolor" -dither None -colors 256 "${png_options[@]}" "png8:$palette"

   # compare exits non-zero when the images differ at all
   psnr="$(magick compare -metric PSNR "$truecolor" "$palette" null: 2>&1 | awk '{ print $1 }' || true)"

   result="$truecolor"
   if awk -v psnr="$psnr" -v min="$MIN_PSNR" 'BEGIN { exit !(psnr == "inf" || psnr + 0 >= min) }'; then
      result="$palette"
   fi

   old_size="$(wc -c < "$file" | tr -d ' ')"
   new_size="$(wc -c < "$result" | tr -d ' ')"
   before=$((before + old_size))

   if [ $((new_size * 10)) -lt $((old_size * 9)) ]; then
      cp "$result" "$file"
      changed=$((changed + 1))
      after=$((after + new_size))
      echo "$((old_size / 1024)) KB -> $((new_size / 1024)) KB  ${file#"$root"/}"
   else
      after=$((after + old_size))
   fi

done < "$workdir/files"

echo "Rewrote $changed image(s): $((before / 1024)) KB -> $((after / 1024)) KB"
