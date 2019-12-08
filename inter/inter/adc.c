
#include "adc.h"

volatile uint32_t data_sum = 0;
volatile uint16_t data_temp = 0;
volatile uint16_t data = 0;
volatile uint16_t cnt = 0;
volatile uint8_t have_sample = 0;

voidFunc_Byte adcCallback = NULL;

ISR(ADC_vect) {
	data_temp = ADCL;
	data_temp |= ADCH << 8;
	
	data_sum += data_temp;
	if (++cnt == 1024) {
		data = data_sum >> 12;
		data_sum = 0;
		cnt = 0;
		have_sample = 1;
		if (adcCallback != NULL) {
			adcCallback(data);
		}
	}
}

void setAdcAsyncCallback(voidFunc_Byte callback) {
	adcCallback = callback;
}

uint16_t adc_data_sync() {
   uint16_t retVal;
   
   while(!have_sample);
	
	cli();
	retVal = data;
	sei();
	
	return retVal;
}

void adc_start(BOOL left) {//Passing a 0 will not left align results
	
  ADCSRB = 0;
  
  ADCSRA |= (1 << ADPS2)
         | (1 << ADPS1)
         | (1 << ADPS0); // Set ADC prescaler to 128 - 125KHz sample rate @ 16MHz
  ADMUX |= (1 << REFS0); // Set ADC reference to AVCC
  if (left) {
    ADMUX |= (1 << ADLAR); // Left adjust ADC result to allow easy 8 bit reading
  }
  
  ADCSRA |= (1 << ADATE);
  ADCSRA |= (1 << ADEN);  // Enable ADC
  ADCSRA |= (1 << ADIE);  // Enable ADC Interrupt

  ADMUX &= ~(1 << MUX0)
        &  ~(1 << MUX1)
        &  ~(1 << MUX2)
        &  ~(1 << MUX3);

  ADCSRA |= (1 << ADSC);  // Start A2D Conversions

  return;
}

void adc_stop(void) {
  //stop the ADC
  ADCSRA &= ~(1 << ADSC);
  return;
}

void adc_change(u08 chan) {
  //stop the ADC
  ADCSRA &= ~(1 << ADSC);

  switch (chan) {
    case 0://binary 0 (reading downwards)
      ADMUX &= ~(1 << MUX0)
            &  ~(1 << MUX1)
            &  ~(1 << MUX2)
            &  ~(1 << MUX3);
      break;
    case 1://binary 1
      ADMUX |=  (1 << MUX0);
      ADMUX &= ~(1 << MUX1)
            &  ~(1 << MUX2)
            &  ~(1 << MUX3);
      break;
    case 2://binary 2
      ADMUX &= ~(1 << MUX0);
      ADMUX |=  (1 << MUX1);
      ADMUX &= ~(1 << MUX2)
            &  ~(1 << MUX3);
      break;
    case 3: //should have the picture by now
      ADMUX |=  (1 << MUX0)
            |   (1 << MUX1);
      ADMUX &= ~(1 << MUX2)
            &  ~(1 << MUX3);
      break;
    case 4:
      ADMUX &= ~(1 << MUX0)
            &  ~(1 << MUX1);
      ADMUX |=  (1 << MUX2);
      ADMUX &= ~(1 << MUX3);
      break;
    case 5:
      ADMUX |=  (1 << MUX0);
      ADMUX &= ~(1 << MUX1);
      ADMUX |=  (1 << MUX2);
      ADMUX &= ~(1 << MUX3);
      break;
    case 6:
      ADMUX &= ~(1 << MUX0);
      ADMUX |=  (1 << MUX1)
            |   (1 << MUX2);
      ADMUX &= ~(1 << MUX3);
      break;
    case 7:
      ADMUX |=  (1 << MUX0)
            |   (1 << MUX1)
            |   (1 << MUX2);
      ADMUX &= ~(1 << MUX3);
      break;
    case 8:
      ADMUX &= ~(1 << MUX0)
            &  ~(1 << MUX1)
            &  ~(1 << MUX2);
      ADMUX |=  (1 << MUX3);
      break;
  }
  //re-enable ADC conversions now that the channel is selected
  ADCSRA |= (1 << ADSC);
  return;
}
