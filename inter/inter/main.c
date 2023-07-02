
#include <avr/io.h>
#include <avr/interrupt.h>
#include <string.h>

#include "comm.h"
#include "adc.h"

#define adcChannelsMonitored 2

#define ADC_SUNLIGHT 0
#define ADC_BATT     1

#define DRL_PIN			1
#define HEADLIGHT_PIN	2

#define LIGHTS_STATE_OFF	0
#define LIGHTS_STATE_ON		1

#define SUN_SAMPLES_MAX_COUNT 800000
#define SUN_SAMPLES_START_COUNT 200000

void init();
void mainLoop();
void processData();
void processLights();
void processLightsOn();
void processLightsOff();
void processLightsNone();
void adcCallbackProc(u08 data);
void toggleOutput(u08 output, BOOL state);
void readDeviceConfig();
void writeDeviceConfig();
void setDefaultConfig();
BOOL getSunSample(u08* val);

struct DataFrame df;

struct st_deviceConfig
{
	u08		minVoltage;
	u08		lightsOnThreshold;
	u08		lightsOffThreshold;
};

struct st_deviceConfig deviceConfig;

u08 adcBuff[adcChannelsMonitored] = {0, 0};
u08 currentAdcChannel = 0;
u08 lightsState = LIGHTS_STATE_OFF;
u08 prevLightsState = LIGHTS_STATE_OFF;
BOOL drlForced = FALSE;
u32 sunSamplesCount = 0;
u32 sunSamplesSum = 0;
u32 sunSamplesMax = SUN_SAMPLES_START_COUNT;
BOOL isStartup = TRUE;
BOOL firstRun = TRUE;

int main(void)
{
	init();
	mainLoop();
}

void init() {
	//setDefaultConfig();
	
	readDeviceConfig();
	
	setAdcAsyncCallback(adcCallbackProc);
	adc_start(0);
	
	CommInit(19200);

	DDRA = 0xFF;
	DDRC = 0xFF;
	DDRD = 0xFF;

	toggleOutput(DRL_PIN, 0);
	toggleOutput(HEADLIGHT_PIN, 0);
	
	sei();
}

void mainLoop() {
	while (1)
	{
		if(data_GetData(&df)) {
			processData();
		}
		
		processLights();
	}
}

void processData() {
	switch(df.Header.nCmd) {
		case CMD_GET_ADC_DATA:
			if (CommIsFree())
				CommSendData(ADDR_CTRL, ADDR_HEAD_UNIT, CMD_ADC_DATA, adcChannelsMonitored, adcBuff);
			break;
			
		case CMD_GET_CONFIG:
			if (CommIsFree())
				CommSendData(ADDR_CTRL, ADDR_HEAD_UNIT, CMD_CONFIG, sizeof(deviceConfig), (u08*)&deviceConfig);
			break;
			
		case CMD_SET_CONFIG:
			if (sizeof(deviceConfig) == df.Header.nDataLen) {
				memcpy((u08*)&deviceConfig, df.Data, sizeof(deviceConfig));
				writeDeviceConfig();
			}
			break;
			
		case CMD_SET_DRL:
			drlForced = df.Data[0];
			if (drlForced) {
				toggleOutput(DRL_PIN, TRUE);
				prevLightsState = lightsState;
			}
			else {
				if (prevLightsState == LIGHTS_STATE_ON)
					toggleOutput(DRL_PIN, FALSE);
				lightsState = prevLightsState;
			}
				
			break;
	}
}

void adcCallbackProc(u08 data) {
	cli();
	
	adcBuff[currentAdcChannel++] = data;
	if (currentAdcChannel == adcChannelsMonitored)
		currentAdcChannel = 0;
	
	adc_change(currentAdcChannel);
	
	sei();
}

void toggleOutput(u08 output, BOOL state) {
	if (state) {
		switch (output) {
			case 0:
				PORTD |= (1 << PD7);
				break;
			case 1:
				PORTC |= (1 << PC1);
				break;
			case 2:
				PORTC |= (1 << PC3);
				break;
			case 3:
				PORTC |= (1 << PC5);
				break;
		}
	}
	else {
		switch (output) {
			case 0:
				PORTD &= ~(1 << PD7);
				break;
			case 1:
				PORTC &= ~(1 << PC1);
				break;
			case 2:
				PORTC &= ~(1 << PC3);
				break;
			case 3:
				PORTC &= ~(1 << PC5);
				break;
			}
	}
}

void processLights() {
	if (adcBuff[ADC_BATT] < deviceConfig.minVoltage)
		return;
		
	switch(lightsState) {	
			case LIGHTS_STATE_ON:
			processLightsOn();
			break;
			
			case LIGHTS_STATE_OFF:
			processLightsOff();
			break;
	}
}

void processLightsOn() {
	u08 sunlight;
	
	if (!getSunSample(&sunlight))
		return;
		
	if (sunlight >= deviceConfig.lightsOffThreshold)	{
		lightsState = LIGHTS_STATE_OFF;
		firstRun = FALSE;
		toggleOutput(DRL_PIN, TRUE);
		toggleOutput(HEADLIGHT_PIN, FALSE);
	}
}

void processLightsOff() {
	u08 sunlight;
	
	if (!getSunSample(&sunlight))
		return;
	
	if (sunlight <= deviceConfig.lightsOnThreshold) {
		lightsState = LIGHTS_STATE_ON;
		firstRun = FALSE;
		if (!drlForced)
			toggleOutput(DRL_PIN, FALSE);
		toggleOutput(HEADLIGHT_PIN, TRUE);
	}
	
	if (firstRun && (lightsState == LIGHTS_STATE_OFF)) {
		toggleOutput(DRL_PIN, TRUE);
		firstRun = FALSE;
	}
}

BOOL getSunSample(u08* val) {
	sunSamplesSum += adcBuff[ADC_SUNLIGHT];
	if (++sunSamplesCount == sunSamplesMax) {
		u32 v = sunSamplesSum / sunSamplesMax;
		*val = (u08)v;
		sunSamplesSum = 0;
		sunSamplesCount = 0;
		if (isStartup) {
			isStartup = FALSE;
			sunSamplesMax = SUN_SAMPLES_MAX_COUNT;
		}
		return TRUE;
	}
	
	return FALSE;
}

void readDeviceConfig()
{
	eeprom_read_block(&deviceConfig, (void *)1, sizeof(deviceConfig));
}

void writeDeviceConfig()
{
	eeprom_write_block(&deviceConfig, (void *)1, sizeof(deviceConfig));
}
	
void setDefaultConfig() {
	deviceConfig.minVoltage = 145;
	deviceConfig.lightsOnThreshold = 100;
	deviceConfig.lightsOffThreshold = 200;
	writeDeviceConfig();
}
