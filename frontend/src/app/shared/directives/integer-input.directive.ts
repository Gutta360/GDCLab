import {
  Directive,
  ElementRef,
  HostListener
} from '@angular/core';

@Directive({
  selector: 'input[gdcIntegerInput]',
  standalone: true
})
export class IntegerInputDirective {

  constructor(
    private elementRef: ElementRef<HTMLInputElement>
  ) {}

  @HostListener('beforeinput', ['$event'])
  onBeforeInput(event: InputEvent): void {
    if (
      event.inputType.startsWith('delete') ||
      event.inputType === 'historyUndo' ||
      event.inputType === 'historyRedo' ||
      event.isComposing
    ) {
      return;
    }

    if (!/^\d*$/.test(this.nextValue(event.data ?? ''))) {
      event.preventDefault();
    }
  }

  @HostListener('paste', ['$event'])
  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    this.insertText((event.clipboardData?.getData('text') ?? '').replace(/\D/g, ''));
  }

  @HostListener('drop', ['$event'])
  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.insertText((event.dataTransfer?.getData('text') ?? '').replace(/\D/g, ''));
  }

  private nextValue(text: string): string {
    const input = this.elementRef.nativeElement;
    const start = input.selectionStart ?? input.value.length;
    const end = input.selectionEnd ?? start;

    return `${input.value.slice(0, start)}${text}${input.value.slice(end)}`;
  }

  private insertText(text: string): void {
    if (!text) {
      return;
    }

    const input = this.elementRef.nativeElement;
    const start = input.selectionStart ?? input.value.length;
    const end = input.selectionEnd ?? start;

    input.value = `${input.value.slice(0, start)}${text}${input.value.slice(end)}`.replace(/\D/g, '');
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }
}
