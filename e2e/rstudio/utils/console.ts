/**
 * Extract only the output lines from the console panel text.
 * Console output includes echoed commands (lines starting with ">")
 * and other UI chrome. This returns only the lines between the last
 * command echo and the next ">" prompt — i.e., the actual R output.
 */
export function getOutputLines(fullText: string): string {
  const lines = fullText.split('\n');
  const outputLines: string[] = [];
  let lastPromptIndex = -1;

  // Find the last line starting with ">" (the echoed command)
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].trim().startsWith('>') && lines[i].trim().length > 1) {
      lastPromptIndex = i;
    }
  }

  // Collect lines after the last command echo, stopping at the next ">" prompt
  if (lastPromptIndex >= 0) {
    for (let i = lastPromptIndex + 1; i < lines.length; i++) {
      const trimmed = lines[i].trim();
      if (trimmed.startsWith('>')) break;
      if (trimmed !== '') outputLines.push(lines[i]);
    }
  }
  return outputLines.join('\n');
}
