// Tries to parse a unicode codepoint string that a user might enter
// For example:
// U+2054
// 2054
// 
// Or in base 10 notation a number between
// 0 and 1114111
export function parseCodepoint(codepointText: string): number | undefined {
  var hexOnlyText = codepointText;
  if (codepointText.startsWith('U+')) {
    hexOnlyText = codepointText.substr(2, codepointText.length - 2);
  }

  // Hex representations of codepoints are always 4 characters long
  if (hexOnlyText.length < 4) {
    const hexValue = parseInt(hexOnlyText, 16);
    if (isValidCodepoint(hexValue)) {
      return hexValue;
    }
  } 

  // base 10 representations can be up to 7 characters long
  if (hexOnlyText.length <= 7) {
    const base10Value = parseInt(hexOnlyText, 10);
    if (isValidCodepoint(base10Value)) {
      return base10Value;
    }
  }

  return undefined;  
}

function isValidCodepoint(codepoint: number) {
  return codepoint < 1114111 && codepoint > 0;
}
