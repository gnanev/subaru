#include "comm.h"

void comm_RxCallback();
void comm_TxCallback();
void comm_SendNextByte();

u08					nNextByte = 0;
BOOL				bHaveMoreData = FALSE;
struct DataFrame	dfOutput;

void CommInit(u32 nBaudrate)
{
	uartSetRxCallback(comm_RxCallback);
	uartSetTxCallback(comm_TxCallback);
	uartInit();
	uartSetBaudRate(nBaudrate);
}

BOOL CommIsFree()
{
	return !bHaveMoreData;
}

BOOL CommSendData(u08 nAddressFrom, u08 nAddressTo, u08 nCmd, u08 nDataLen, u08* pData)
{
	if (bHaveMoreData)
		return FALSE;

	dfOutput.Header.nCmd = nCmd;
	dfOutput.Header.nAddressFrom = nAddressFrom;
	dfOutput.Header.nAddressTo = nAddressTo;
	dfOutput.Header.nDataLen = nDataLen;
	if (nDataLen > 0)
		memcpy(&dfOutput.Data, pData, nDataLen);
	data_SendFrame(&dfOutput);
	comm_SendNextByte();
	return TRUE;
}


BOOL CommGetData(struct DataFrame* pdf)
{
	return data_GetData(pdf);
}

void comm_SendNextByte()
{
	bHaveMoreData = data_SendNextByte(&nNextByte);
	uartSendByte(nNextByte);
}


void comm_RxCallback()
{
	data_RecieveByte(uartRecieveByte());
}

void comm_TxCallback()
{
	if (!bHaveMoreData)
		return;
	
	comm_SendNextByte();
}
