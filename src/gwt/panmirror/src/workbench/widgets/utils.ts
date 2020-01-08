export function focusInput(input: HTMLInputElement | HTMLSelectElement | null) {
  if (input) {
    if (input.type === 'text') {
      (input as HTMLInputElement).setSelectionRange(0, 0);
    }
    input.focus();
  }
}
