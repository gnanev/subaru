#ifndef _UART_H_
#define _UART_H_

#include "common.h"

#define UART_DEFAULT_BAUD_RATE		9600

void uartInit(void);
void uartSetBaudRate(u32 nBaudrate);
void uartSendByte(u08 txData);
u08 uartRecieveByte();
void uartSetRxCallback(voidFunc pNewCallback);
void uartSetTxCallback(voidFunc pNewCallback);
void DummyUartCallback();

#endif
