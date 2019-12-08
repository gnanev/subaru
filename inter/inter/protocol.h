#ifndef _PROTOCOL_H_
#define _PROTOCOL_H_

#include "protocol_data.h"
#include "common.h"

void 			data_RecieveByte(unsigned char nData);
BOOL		 	data_SendNextByte(unsigned char* pData);
void 			data_SendFrame(struct DataFrame* pdf);
BOOL 			data_GetData(struct DataFrame* pdf);

#endif
