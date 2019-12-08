//-------------------------------------------
/*
ADC.h
2013 - Josh Ashby
joshuaashby@joshashby.com
http://joshashby.com
http://github.com/JoshAshby
freenode/#linuxandsci - JoshAshby
*/
//-------------------------------------------
#ifndef ADC_H
#define ADC_H
#include <avr/io.h>
#include <stdio.h>
#include <avr/interrupt.h>
#include <compat/twi.h>
#include <inttypes.h>
#include <stdlib.h>
#include <avr/sleep.h>
#include <util/delay.h>

#include "common.h"

//-------------------------------------------
//Prototypes
//-------------------------------------------
void adc_start(BOOL left);
void adc_stop(void);
void adc_change(u08 chan);
uint16_t adc_data_sync();
void setAdcAsyncCallback(voidFunc_Byte callback);

#endif
