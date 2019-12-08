#ifndef _COMM_H_
#define _COMM_H_

#include "common.h"
#include "uart.h"
#include "protocol.h"

void		CommInit(u32 nBaudrate);
BOOL		CommIsFree();
BOOL 		CommSendData(u08 nAddressFrom, u08 nAddressTo, u08 nCmd, u08 nDataLen, u08* pData);
BOOL		CommGetData(struct DataFrame* pdf);

#endif
