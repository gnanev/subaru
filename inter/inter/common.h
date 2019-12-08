#ifndef _COMMON_H_
#define _COMMON_H_

#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/eeprom.h>
#include <string.h>
#include "avrlibtypes.h"
#include "avrlibdefs.h"

typedef void (*voidFunc)();
typedef void (*voidFunc_Byte)(u08);

#define BOOL u08
#define ON 1
#define OFF 0

#endif
