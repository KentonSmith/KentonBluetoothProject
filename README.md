# KentonBluetoothProject

###Hardware Used (BOM)
* Arduino UNO
* JY-MCU Bluetooth Module https://core-electronics.com.au/attachments/guides/Product-User-Guide-JY-MCU-Bluetooth-UART-R1-0.pdf
* Adafruit BNO055 9 DOF Sensor https://www.adafruit.com/products/2472
* Android Phone
* 22 Gauge Wire

###High Level Description
This sensor apparatus reads in data from the Adafruit sensor, encodes it in a parseable format, and transmits it via Bluetooth to an 
Android phone.  The Android phone receives the incoming data in a thread dedicated to I/O and updates the UI an the main thread
in real-time at 80 Hz.

###Youtube Demonstration Video!!
https://www.youtube.com/watch?v=Ts81m9KBcSc


