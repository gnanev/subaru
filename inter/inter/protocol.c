#include "protocol.h"
#include <string.h>

struct DataFrame 			df[2];
struct DataFrame* 			pRecievedFrame = NULL;
unsigned char 				nCurrentFrame = 0;
unsigned char 	 			nRecieveStep = 0;
unsigned char 				nDataRecieved = 0;

unsigned char				OutputBuff[sizeof(struct DataFrame)];
unsigned char				nCurrentByte = 0;
unsigned char				nOutputBuffSize = 0;

void data_StartFrame();
void data_FinishFrame();
void data_DropFrame();
void data_SwapFrame();
void data_RecieveByte(unsigned char nData);
unsigned char data_CalcDataCheckSum(unsigned char* pData, unsigned char nDataLen, unsigned char nCmd, 
									unsigned char nAddressFrom, unsigned char nAddressTo);


void data_StartFrame()
{
	data_SwapFrame();
	nDataRecieved = 0;
	memset(&df[nCurrentFrame], 0, sizeof(struct DataFrame));
}

void data_FinishFrame()
{
	unsigned char nCheckSum = data_CalcDataCheckSum(df[nCurrentFrame].Data, 
										   df[nCurrentFrame].Header.nDataLen,
										   df[nCurrentFrame].Header.nCmd,
										   df[nCurrentFrame].Header.nAddressFrom,
										   df[nCurrentFrame].Header.nAddressTo);

	nRecieveStep = 0;
	nDataRecieved = 0;

	if (df[nCurrentFrame].Header.nCheckSum == nCheckSum) 
		pRecievedFrame = &df[nCurrentFrame];
	else
		data_DropFrame();
}

void data_DropFrame()
{
	nRecieveStep = 0;
	nDataRecieved = 0;
	pRecievedFrame = NULL;
	data_SwapFrame();
}

void data_SwapFrame()
{
	if (nCurrentFrame == 0)
		nCurrentFrame = 1;
	else
		nCurrentFrame = 0;
}

void data_RecieveByte(unsigned char nData)
{
	switch(nRecieveStep)
	{
		case 0:
			if (nData != START_BYTE)
				return;
			data_StartFrame();
			df[nCurrentFrame].Header.nStart = nData;			
			nRecieveStep++;
			break;

		case 1:
			df[nCurrentFrame].Header.nAddressFrom = nData;
			nRecieveStep++;
			break;

		case 2:
			df[nCurrentFrame].Header.nAddressTo = nData;
			nRecieveStep++;
			break;

		case 3:
			df[nCurrentFrame].Header.nCmd = nData;
			nRecieveStep++;
			break;

		case 4:
			if (nData > DATA_MAX_LEN)
			{
				data_DropFrame();
				return;
			}

			df[nCurrentFrame].Header.nDataLen = nData;
			nRecieveStep++;
			break;

		case 5:
			df[nCurrentFrame].Header.nCheckSum = nData;
			if (df[nCurrentFrame].Header.nDataLen == 0)
			{
				goto end_revieve;
			}
			nRecieveStep++;
			break;

		case 6:		
			df[nCurrentFrame].Data[nDataRecieved++] = nData;
			
			if (nDataRecieved > DATA_MAX_LEN)
			{
				data_DropFrame();
				return;
			}
			
			if (nDataRecieved == df[nCurrentFrame].Header.nDataLen)
			{
end_revieve:
				data_FinishFrame();
			}

			break;
	}
}

BOOL data_SendNextByte(unsigned char* pData)
{
	*pData = OutputBuff[nCurrentByte++];
	return nCurrentByte < nOutputBuffSize;
}

void data_SendFrame(struct DataFrame* pdf)
{
	nCurrentByte = 0;
	nOutputBuffSize = sizeof(struct DataFrameHeader) + pdf->Header.nDataLen;
	pdf->Header.nStart = START_BYTE;
	pdf->Header.nCheckSum = data_CalcDataCheckSum(pdf->Data, pdf->Header.nDataLen, pdf->Header.nCmd, 
												  pdf->Header.nAddressFrom, pdf->Header.nAddressTo);
	memcpy(OutputBuff, pdf, nOutputBuffSize);
}

BOOL data_GetData(struct DataFrame* pdf)
{
	if (pRecievedFrame != NULL)
	{
		cli();
		*pdf = *pRecievedFrame;
		pRecievedFrame = NULL;	
		sei();
		return TRUE;
	}

	return FALSE;	
}

unsigned char data_CalcDataCheckSum(unsigned char* pData, unsigned char nDataLen, unsigned char nCmd, 
									unsigned char nAddressFrom, unsigned char nAddressTo)
{
	unsigned short sum;
	unsigned char  i;

	sum = nDataLen + nCmd + nAddressFrom + nAddressTo;

	if (nDataLen > DATA_MAX_LEN) return 0;

	for(i=0; i<nDataLen; i++)
	{
		sum += pData[i];
	}

	return sum % 256;
}
