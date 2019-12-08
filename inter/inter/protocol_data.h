#ifndef __PROTOCOL_DATA_H__
#define __PROTOCOL_DATA_H__

#define		DATA_MAX_LEN	64
#define		START_BYTE		0xAA

#define ADDR_HEAD_UNIT	1
#define ADDR_CTRL		2

#define CMD_GET_ADC_DATA	11
#define CMD_ADC_DATA		12
#define CMD_GET_CONFIG		13
#define CMD_CONFIG			14
#define CMD_SET_CONFIG		15
#define CMD_GET_LOCK		16
#define CMD_LOCK			17
#define CMD_SET_LOCK		18
#define CMD_SET_DRL			19

#define INPUT_SYSTEM_VOLTAGE 0
#define INPUT_DOOR			 1
#define INPUT_ACC			 2

struct DataFrameHeader
{
	unsigned char	nStart;
	unsigned char	nAddressFrom;
	unsigned char	nAddressTo;
	unsigned char	nCmd;
	unsigned char	nDataLen;
	unsigned char	nCheckSum;
};

struct DataFrame
{
	struct DataFrameHeader	Header;
	unsigned char			Data[DATA_MAX_LEN];
};

#endif
