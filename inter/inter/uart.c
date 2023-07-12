#include "uart.h"

volatile static voidFunc UartRxCallback = DummyUartCallback;
volatile static voidFunc UartTxCallback = DummyUartCallback;

void uartInit(void)
{
	outb(UCSR0B, BV(RXCIE0)|BV(TXCIE0)|BV(RXEN0)|BV(TXEN0));
	uartSetBaudRate(UART_DEFAULT_BAUD_RATE); 
	sei();
}

void uartSetBaudRate(u32 nBaudrate)
{
	// calculate division factor for requested baud rate, and set it
	u16 bauddiv = ((F_CPU+(nBaudrate*8L))/(nBaudrate*16L)-1);
	outb(UBRR0L, bauddiv);
	#ifdef UBRR0H
	outb(UBRR0H, bauddiv>>8);
	#endif
}

void uartSendByte(u08 txData)
{
	while(!(UCSR0A & (1<<UDRE0)));
	outb(UDR0, txData);
}

u08 uartRecieveByte()
{
	return inb(UDR0);
}

void uartSetRxCallback(voidFunc pNewCallback)
{
	if (pNewCallback != NULL)
		UartRxCallback = pNewCallback;
}

void uartSetTxCallback(voidFunc pNewCallback)
{
	if (pNewCallback != NULL)
		UartTxCallback = pNewCallback;
}


void DummyUartCallback() {}

ISR(USART_RX_vect)
{
	UartRxCallback();
}

ISR(USART_TX_vect)
{
	UartTxCallback();
}
