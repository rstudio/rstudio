// Tries to parse a unicode codepoint string that a user might enter
// For example:
// U+2A1F
// 2A1F
// 10783
const kMinValidCodepoint = 0
const kMaxValidCodepoint = 1114112

const kHexCodepointPrefix = 'U+'

export function parseCodepoint(codepointText: string): number | undefined {

  // Try parsing it as a base 10 int first
  const base10Value = parseInt(codepointText, 10);
  if (isValidCodepoint(base10Value)) {
    return base10Value;
  }

  // It might have a user prefix for unicode character, remove
  var hexOnlyText = codepointText;
  if (codepointText.startsWith(kHexCodepointPrefix)) {
    hexOnlyText = codepointText.substr(kHexCodepointPrefix.length, codepointText.length - kHexCodepointPrefix.length);
  }

  // try parsing it as a hex string
  const hexValue = parseInt(hexOnlyText, 16);
  if (isValidCodepoint(hexValue)) {
    return hexValue;
  }

  return undefined;  
}

function isValidCodepoint(codepoint: number) {
  return codepoint > kMinValidCodepoint && codepoint < kMaxValidCodepoint;
}
