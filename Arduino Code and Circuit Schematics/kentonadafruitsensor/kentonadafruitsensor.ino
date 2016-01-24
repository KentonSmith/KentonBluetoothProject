#include <string.h>
#include <Servo.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BNO055.h>
#include <utility/imumaths.h>
#include <SoftwareSerial.h>
#include <LiquidCrystal.h>


/* This driver uses the Adafruit unified sensor library (Adafruit_Sensor),
   which provides a common 'type' for sensor data and some helper functions.

   To use this driver you will also need to download the Adafruit_Sensor
   library and include it in your libraries folder.

   You should also assign a unique ID to this sensor for use with
   the Adafruit Sensor API so that you can identify this particular
   sensor in any data logs, etc.  To assign a unique ID, simply
   provide an appropriate value in the constructor below (12345
   is used by default in this example).

   Connections
   ===========
   Connect SCL to analog 5
   Connect SDA to analog 4
   Connect VDD to 3-5V DC
   Connect GROUND to common ground

   History
   =======
   2015/MAR/03  - First release (KTOWN)
   2015/AUG/27  - Added calibration and system status helpers
*/

/* Set the delay between fresh samples */
#define BNO055_SAMPLERATE_DELAY_MS (10) //can be as low as 100

///#define BLUETOOTH_SPEED 9600
#define BLUETOOTH_SPEED 115200
//SoftwareSerial mySerial(10, 11); // RX, TX               //used to be (10, 11)
 SoftwareSerial mySerial(3, 5);  //changed to digital 3 and 5
 unsigned long time;
unsigned long start_time;


//LiquidCrystal lcd(7, 8, 4, 5, 6, 12);

int num_iterations = 500;
int count = 0;

char state = 'S';  //'S' is stop, do not send any bluetooth data.  'R' is android wants to read, so write data from bluetooth


Adafruit_BNO055 bno = Adafruit_BNO055(55);

/**************************************************************************/
/*
    Displays some basic information on this sensor from the unified
    sensor API sensor_t type (see Adafruit_Sensor for more information)
*/
/**************************************************************************/
void displaySensorDetails(void)
{
  sensor_t sensor;
  bno.getSensor(&sensor);
  Serial.println("------------------------------------");
  Serial.print  ("Sensor:       "); Serial.println(sensor.name);
  Serial.print  ("Driver Ver:   "); Serial.println(sensor.version);
  Serial.print  ("Unique ID:    "); Serial.println(sensor.sensor_id);
  Serial.print  ("Max Value:    "); Serial.print(sensor.max_value); Serial.println(" xxx");
  Serial.print  ("Min Value:    "); Serial.print(sensor.min_value); Serial.println(" xxx");
  Serial.print  ("Resolution:   "); Serial.print(sensor.resolution); Serial.println(" xxx");
  Serial.println("------------------------------------");
  Serial.println("");
  delay(500); 
 Serial.println("current user: Hard coded value -- change later");

  
}






/**************************************************************************/
/*
    Display some basic info about the sensor status
*/
/**************************************************************************/
void displaySensorStatus(void)
{
  /* Get the system status values (mostly for debugging purposes) */
  uint8_t system_status, self_test_results, system_error;
  system_status = self_test_results = system_error = 0;
  bno.getSystemStatus(&system_status, &self_test_results, &system_error);

  /* Display the results in the Serial Monitor */
  Serial.println("");
  Serial.print("System Status: 0x");
  Serial.println(system_status, HEX);
  Serial.print("Self Test:     0x");
  Serial.println(self_test_results, HEX);
  Serial.print("System Error:  0x");
  Serial.println(system_error, HEX);
  Serial.println("");
  delay(500);
}

/**************************************************************************/
/*
    Display sensor calibration status
*/
/**************************************************************************/
void displayCalStatus(void)
{
  /* Get the four calibration values (0..3) */
  /* Any sensor data reporting 0 should be ignored, */
  /* 3 means 'fully calibrated" */
  uint8_t system, gyro, accel, mag;
  system = gyro = accel = mag = 0;
  bno.getCalibration(&system, &gyro, &accel, &mag);

  /* The data should be ignored until the system calibration is > 0 */
  Serial.print("\t");
  if (!system)
  {
    Serial.print("! ");
  }

  /* Display the individual values */
  Serial.print("Sys:");
  Serial.print(system, DEC);
  Serial.print(" G:");
  Serial.print(gyro, DEC);
  Serial.print(" A:");
  Serial.print(accel, DEC);
  Serial.print(" M:");
  Serial.print(mag, DEC);
}




/**************************************************************************/
/*
    Arduino setup function (automatically called at startup)
*/
/**************************************************************************/
void setup(void)
{
  time = millis();
  start_time = time;

    //lcd.begin(16, 2);
  // Print a message to the LCD.
  //lcd.print("Initializing");

 Serial.begin(BLUETOOTH_SPEED);
    while (!Serial) {
    ;  //wait for serial port to connect. Needed for Leonardo only
  }
  //mySerial.print("AT+BAUD1");

Serial.println("Bluetooth Serial is ready");
delay(1000);
  Serial.println(state);
  
   mySerial.begin(115200);
  delay(1000);
  
  //Serial.begin(9600);
 // Serial.println("Orientation Sensor Test"); Serial.println("");

  /* Initialise the sensor */
  if(!bno.begin())
  {
    /* There was a problem detecting the BNO055 ... check your connections */
    Serial.print("Ooops, no BNO055 detected ... Check your wiring or I2C ADDR!");
    while(1);
  }

  delay(1000);

          

  /* Display some basic information on this sensor */
  //displaySensorDetails();

// delay(500);
  /* Optional: Display current status */
// displaySensorStatus();
// delay(500);
 // displayCalStatus();
 //delay(500);
  bno.setExtCrystalUse(true);

  mySerial.flush();
  
  //lcd.clear();
}

/**************************************************************************/
/*
    Arduino loop function, called once 'setup' is complete (your own code
    should go here)
*/
/**************************************************************************/
void loop(void)
{
  // set the cursor to column 0, line 1
  // (note: line 1 is the second row, since counting begins with 0):
  //lcd.setCursor(0,0);
  //lcd.print("Status");
  //lcd.setCursor(0, 1);
  // print the number of seconds since reset:
 // lcd.print(millis() / 1000);

  
    if(mySerial.available() > 0)
    {
        
        delay(1200);
        state =  mySerial.read();
        delay(1200);
        Serial.println(state);

        mySerial.flush();  //once we get state flush this
        
        delay(1200);
    }

    if(state == 'R')
    {
      
       /* Get a new sensor event */
        //lcd.print("Read");
       
        sensors_event_t euler_event;
       // bno.getEvent(&euler_event);
      
        imu::Vector<3> euler = bno.getVector(Adafruit_BNO055::VECTOR_EULER);
         imu::Vector<3> gyro = bno.getVector(Adafruit_BNO055::VECTOR_GYROSCOPE);
         imu::Vector<3> lineacc = bno.getVector(Adafruit_BNO055::VECTOR_LINEARACCEL);

        //time = millis();
       // unsigned long time_elapsed = millis() - start_time;
        
        
       // Serial.print(time_elapsed);        
       mySerial.print(millis() - start_time);
       // Serial.print("$");                 
       mySerial.print("$");
       // Serial.print(euler.x(), 4);        
       mySerial.print(euler.x(), 1);
       // Serial.print(",");                 
       mySerial.print(",");
       // Serial.print(euler.y(), 4);        
       mySerial.print(euler.y(), 1);
       // Serial.print(",");                 
       mySerial.print(",");
       // Serial.print(euler.z(), 4);        
       mySerial.print(euler.z(), 1);
       // Serial.print("$");                 
       mySerial.print("$");
       // Serial.print(gyro.x(),4);          
       mySerial.print(gyro.x(),3);
       // Serial.print(",");                 
       mySerial.print(",");
       // Serial.print(gyro.y(),);          
       mySerial.print(gyro.y(),3);
       // Serial.print(",");                 
       mySerial.print(",");
       // Serial.print(gyro.z(),3);          
       mySerial.print(gyro.z(),3);
       // Serial.print("$");                 
       mySerial.print("$");
       // Serial.print(lineacc.x(),4);       
       mySerial.print(lineacc.x(),3);  
       // Serial.print(",");                 
       mySerial.print(",");
       // Serial.print(lineacc.y(),4);       
       mySerial.print(lineacc.y(),3);  
       // Serial.print(",");                 
       mySerial.print(",");
       // Serial.print(lineacc.z(),4);       
       mySerial.print(lineacc.z(),3);  
    //    Serial.println("&");               
    mySerial.print("&");    //end of sequence 
      
    //  Serial.println("");
      /* Wait the specified delay before requesting nex data */
      
      
      //delay(BNO055_SAMPLERATE_DELAY_MS);  //just got rid of KQS 1/12/2016
      
    }
    else
    {
     // lcd.print("Stop");
      delay(10);  //this is so not tight loop when not reading
      start_time = millis();
    }

     
    //lcd.clear();

/*
    if(count < num_iterations)
    {
      count++;
    }
    else
    {
          if(count == num_iterations)
          {
            count++;
            mySerial.print('#');  //the pound sign indicates end of input
            Serial.println('#');
          }
    }
  */ 
        
}

