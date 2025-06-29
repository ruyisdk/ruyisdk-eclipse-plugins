SECTIONS
{
    . = 0x80000000;
    .text : {
        *(.init)
        *(.text*)
    }
    .data : {
        *(.data*)
    }
    .bss : {
        *(.bss*)
        *(COMMON)
    }
    . = ALIGN(8);
    _stack_top = .;
}
