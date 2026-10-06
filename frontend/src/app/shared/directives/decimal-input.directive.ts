import {
  Directive,
  ElementRef,
  HostListener,
  Input
} from '@angular/core';

@Directive({
  selector: 'input[gdcDecimalInput]',
  standalone: true
})
export class DecimalInputDirective {

  @Input() gdcDecimalPlaces = 2;

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

    const nextValue = this.nextValue(event.data ?? '');

    if (!this.isValid(nextValue)) {
      event.preventDefault();
    }
  }

  @HostListener('paste', ['$event'])
  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    this.insertText(this.sanitize(event.clipboardData?.getData('text') ?? ''));
  }

  @HostListener('drop', ['$event'])
  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.insertText(this.sanitize(event.dataTransfer?.getData('text') ?? ''));
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
    const nextValue = `${input.value.slice(0, start)}${text}${input.value.slice(end)}`;

    input.value = this.sanitize(nextValue);
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }

  private isValid(value: string): boolean {
    return new RegExp(`^\\d*(\\.\\d{0,${this.gdcDecimalPlaces}})?$`).test(value);
  }

  private sanitize(value: string): string {
    const digitsAndDots = value.replace(/[^\d.]/g, '');
    const [whole = '', ...decimalParts] = digitsAndDots.split('.');
    const decimal = decimalParts.join('').slice(0, this.gdcDecimalPlaces);

    return decimalParts.length
      ? `${whole}.${decimal}`
      : whole;
  }
}
