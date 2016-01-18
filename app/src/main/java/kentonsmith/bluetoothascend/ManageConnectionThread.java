package kentonsmith.bluetoothascend;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by admin on 1/17/2016.
 */
public class ManageConnectionThread extends Thread {
    //private final BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private BluetoothSocket socket;

    private StringBuffer sb;

    private AdafruitDataHandler localHandler;

    // private final InputStream mmInStream;
    // private final OutputStream mmOutStream;
    private BooleanWrapper localBooleanWrapper;

    public ManageConnectionThread(BluetoothSocket globalSocket, StringBuffer globalStringBuffer, AdafruitDataHandler globalHandler, BooleanWrapper globalBoolean) {
        //  mmSocket = socket;
        socket = globalSocket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.sb = globalStringBuffer;
        this.localHandler = globalHandler;
        this.localBooleanWrapper = globalBoolean;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    //http://stackoverflow.com/questions/12294705/error-in-reading-data-from-inputstream-in-bluetooth-on-android
    public void run() {
        while (true) {
            try {
                byte[] buffer = new byte[128];
                String readMessage;
                int bytes;

                if(mmInStream == null)
                {
                    Log.v("ManageConnectionThread", "mmInStream is null after being stopped now throwing exception to try and break out of thread");
                    throw new IOException("Stop button was pressed");
                }

                //KQSNEWComment
                if (this.localBooleanWrapper.getVal() == true && mmInStream.available() > 0) {
                    try {
                        // Read from the InputStream
                        bytes = mmInStream.read(buffer);
                        readMessage = new String(buffer, 0, bytes);
                        Log.v("ManageConnectionThread", readMessage);

                        //lock
                        this.sb.append(readMessage);  //shared data with main UI thread
                        //unlock
                        String tempString = this.sb.toString();

                        int firstAmpersandIndex = tempString.indexOf('&');

                        String parseableChunk = "";
                        try
                        {
                            parseableChunk = tempString.substring(0, firstAmpersandIndex);
                        }
                        catch(StringIndexOutOfBoundsException e)  //case where there is no ampersand in remaining string
                        {
                            parseableChunk = "";
                        }

                        //Log.v("ManageConnectionThread", "Current parseableChunk = " + parseableChunk);

                        //DO THIS TO HAVE tenComponents keep up with bluetooth data streaming in
                        while(meetsCriteriaTenComponents(parseableChunk) != null)  //tenComponents is passed in as reference and will be assigned to results of parsing
                        {
                            //send to main chunk handler in bundle, remove first chunk from string, reassign stringbuilder

                            //  Log.v("ManageConnectionThread", "sb before removing chunk = " + sb.toString());

                            ArrayList<String> tenComponents = meetsCriteriaTenComponents(parseableChunk);
                            Log.v("ManageConnectionThread", "Ten components = " + tenComponents);

                            Message m = new Message();
                            Bundle b = new Bundle();

                            b.putLong("Milliseconds", Long.valueOf(tenComponents.get(0)));

                            b.putDouble("Euler_X", Double.valueOf(tenComponents.get(1)));
                            b.putDouble("Euler_Y", Double.valueOf(tenComponents.get(2)));
                            b.putDouble("Euler_Z", Double.valueOf(tenComponents.get(3)));

                            b.putDouble("Gyro_X", Double.valueOf(tenComponents.get(4)));
                            b.putDouble("Gyro_Y", Double.valueOf(tenComponents.get(5)));
                            b.putDouble("Gyro_Z", Double.valueOf(tenComponents.get(6)));

                            b.putDouble("Lin_Acc_X", Double.valueOf(tenComponents.get(7)));
                            b.putDouble("Lin_Acc_Y", Double.valueOf(tenComponents.get(8)));
                            b.putDouble("Lin_Acc_Z", Double.valueOf(tenComponents.get(9)));

                            m.setData(b);

                            this.sb = new StringBuffer(tempString.substring(firstAmpersandIndex + 1, tempString.length()));
                            //  Log.v("ManageConnectionThread", "sb after removing chunk = " + sb.toString());

                            this.localHandler.sendMessage(m);  //neeed to add code to update UI

                            tempString = this.sb.toString();

                            firstAmpersandIndex = tempString.indexOf('&');

                            try
                            {
                                parseableChunk = tempString.substring(0, firstAmpersandIndex);
                            }
                            catch(StringIndexOutOfBoundsException e)  //case where there is no ampersand in remaining string
                            {
                                parseableChunk = "";
                            }
                        }

                        //ArduinoDataList.add(readMessage);             //KQS 12/30/2015 NEED MUTEX SINCE TWO THREADS USE THIS

                    } catch (IOException e) {
                        Log.v("ManageConnectionThread", "disconnected");
                        break;
                    }
                    // Send the obtained bytes to the UI Activity

                } else {
                    SystemClock.sleep(100);
                }
            } catch (IOException e) {

                e.printStackTrace();
                Log.v("ManageConnectionThread", "Breaking out of while loop for reading");

                break;
            }
        }
    }

    // Call this from the main activity to send data to the remote device

    public void writeInt(int i)
    {
        try {
            Log.v("ManageConnectionThread", "try block for write method is called inside manage connection thread");
            mmOutStream.write(i);
            Log.v("ManageConnectionThread", "mmOutStream.writes(" + i + ") was successful");
            // mmOutStream.close();
        } catch (IOException e) {
            Log.v("ManageConnectionThread", "mmOutStream.writes(" + i + ") FAILED.  Now printing error");
            Log.v("ManageConnectionThread", e.toString());
        }
    }

    public void writeByteArray(byte[] bytes) {
        try {
            Log.v("ManageConnectionThread", "try block for write method is called inside manage connection thread");

            mmOutStream.flush();  //clear out any garbage from last time
            mmOutStream.flush();  //clear out any garbage from last time

            mmOutStream.write(bytes);
            Log.v("ManageConnectionThread", "mmOutStream.writes(" + Arrays.toString(bytes) + ") was successful");

            mmOutStream.flush();  //clear out any garbage from last time
            mmOutStream.flush();  //clear out any garbage from last time

            // mmOutStream.close();
        } catch (IOException e) {
            Log.v("ManageConnectionThread", "mmOutStream.writes(" + Arrays.toString(bytes) + ") FAILED.  Now printing error");
            Log.v("ManageConnectionThread", e.toString());
        }
    }

    // Call this from the main activity to shutdown the connection
    public void cancel() {
        try {
            //Recently added these.  Commented out mmOutStream.close()in the Write function of this thread
            //mmOutStream.close();
            //mmInStream.close();

            //inputStreamIsOpen = false;
            this.localBooleanWrapper.setVal(false);
            Log.v("ManageConnectionThread", "inputStreamIsOpen flag is set to false since stop button was pressed");

            throw new IOException("blah");
            // mmInStream = null;
            //mmOutStream = null;

            //mmSocket.close();  //Use to be uncommented 1/11/2016
        } catch (IOException e) { }
    }

    private ArrayList<String> meetsCriteriaTenComponents(String parseChunk)
    {
        ArrayList<String> results = new ArrayList<String>();

        Log.v("meetsCriteria", "raw string = " + parseChunk);

        String onlyCommasDelimiting = parseChunk.replace("$",",");

        Log.v("meetsCriteria", "with commas instead = " + onlyCommasDelimiting);

        String[] components = onlyCommasDelimiting.split(",");

        Log.v("meetsCriteria", Arrays.toString(components));

        if(components.length != 10)
        {
            return null;
        }

        for(int i = 0; i < components.length; i++)
        {
            results.add(components[i]);
        }

        return results;
    }
}

//Firsts iteration that containted global variables


/*
    private class ManageConnectionThread extends Thread {
        //private final BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private BluetoothSocket socket;

       // private final InputStream mmInStream;
       // private final OutputStream mmOutStream;

        public ManageConnectionThread(BluetoothSocket globalSocket) {
          //  mmSocket = socket;
            socket = globalSocket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //http://stackoverflow.com/questions/12294705/error-in-reading-data-from-inputstream-in-bluetooth-on-android
        public void run() {
            while (true) {
                try {
                    byte[] buffer = new byte[128];
                    String readMessage;
                    int bytes;

                    if(mmInStream == null)
                    {
                        Log.v("ManageConnectionThread", "mmInStream is null after being stopped now throwing exception to try and break out of thread");
                        throw new IOException("Stop button was pressed");
                    }

                    //KQSNEWComment
                    if (inputStreamIsOpen.booleanValue() == true && mmInStream.available() > 0) {
                        try {
                            // Read from the InputStream
                            bytes = mmInStream.read(buffer);
                            readMessage = new String(buffer, 0, bytes);
                            Log.v("ManageConnectionThread", readMessage);

                            //lock
                            sb.append(readMessage);  //shared data with main UI thread
                            //unlock
                           String tempString = sb.toString();

                            int firstAmpersandIndex = tempString.indexOf('&');

                            String parseableChunk = "";
                            try
                            {
                                parseableChunk = tempString.substring(0, firstAmpersandIndex);
                            }
                            catch(StringIndexOutOfBoundsException e)  //case where there is no ampersand in remaining string
                            {
                                parseableChunk = "";
                            }

                            //Log.v("ManageConnectionThread", "Current parseableChunk = " + parseableChunk);

                            //DO THIS TO HAVE tenComponents keep up with bluetooth data streaming in
                            while(meetsCriteriaTenComponents(parseableChunk) != null)  //tenComponents is passed in as reference and will be assigned to results of parsing
                            {
                                //send to main chunk handler in bundle, remove first chunk from string, reassign stringbuilder

                              //  Log.v("ManageConnectionThread", "sb before removing chunk = " + sb.toString());

                                ArrayList<String> tenComponents = meetsCriteriaTenComponents(parseableChunk);
                                Log.v("ManageConnectionThread", "Ten components = " + tenComponents);

                                Message m = new Message();
                                Bundle b = new Bundle();

                                b.putLong("Milliseconds", Long.valueOf(tenComponents.get(0)));

                                b.putDouble("Euler_X", Double.valueOf(tenComponents.get(1)));
                                b.putDouble("Euler_Y", Double.valueOf(tenComponents.get(2)));
                                b.putDouble("Euler_Z", Double.valueOf(tenComponents.get(3)));

                                b.putDouble("Gyro_X", Double.valueOf(tenComponents.get(4)));
                                b.putDouble("Gyro_Y", Double.valueOf(tenComponents.get(5)));
                                b.putDouble("Gyro_Z", Double.valueOf(tenComponents.get(6)));

                                b.putDouble("Lin_Acc_X", Double.valueOf(tenComponents.get(7)));
                                b.putDouble("Lin_Acc_Y", Double.valueOf(tenComponents.get(8)));
                                b.putDouble("Lin_Acc_Z", Double.valueOf(tenComponents.get(9)));

                                m.setData(b);

                                sb = new StringBuffer(tempString.substring(firstAmpersandIndex + 1, tempString.length()));
                              //  Log.v("ManageConnectionThread", "sb after removing chunk = " + sb.toString());

                                mHandler.sendMessage(m);  //neeed to add code to update UI

                                tempString = sb.toString();

                                firstAmpersandIndex = tempString.indexOf('&');

                                try
                                {
                                    parseableChunk = tempString.substring(0, firstAmpersandIndex);
                                }
                                catch(StringIndexOutOfBoundsException e)  //case where there is no ampersand in remaining string
                                {
                                    parseableChunk = "";
                                }
                            }

                            //ArduinoDataList.add(readMessage);             //KQS 12/30/2015 NEED MUTEX SINCE TWO THREADS USE THIS

                        } catch (IOException e) {
                            Log.v("ManageConnectionThread", "disconnected");
                            break;
                        }
                        // Send the obtained bytes to the UI Activity

                    } else {
                        SystemClock.sleep(100);
                    }
                } catch (IOException e) {

                    e.printStackTrace();
                    Log.v("ManageConnectionThread", "Breaking out of while loop for reading");

                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device

        public void writeInt(int i)
        {
            try {
                Log.v("ManageConnectionThread", "try block for write method is called inside manage connection thread");
                mmOutStream.write(i);
                Log.v("ManageConnectionThread", "mmOutStream.writes(" + i + ") was successful");
                // mmOutStream.close();
            } catch (IOException e) {
                Log.v("ManageConnectionThread", "mmOutStream.writes(" + i + ") FAILED.  Now printing error");
                Log.v("ManageConnectionThread", e.toString());
            }
        }

        public void writeByteArray(byte[] bytes) {
            try {
                Log.v("ManageConnectionThread", "try block for write method is called inside manage connection thread");

                mmOutStream.flush();  //clear out any garbage from last time
                mmOutStream.flush();  //clear out any garbage from last time

                mmOutStream.write(bytes);
                Log.v("ManageConnectionThread", "mmOutStream.writes(" + Arrays.toString(bytes) + ") was successful");

                mmOutStream.flush();  //clear out any garbage from last time
                mmOutStream.flush();  //clear out any garbage from last time

                // mmOutStream.close();
            } catch (IOException e) {
                Log.v("ManageConnectionThread", "mmOutStream.writes(" + Arrays.toString(bytes) + ") FAILED.  Now printing error");
                Log.v("ManageConnectionThread", e.toString());
            }
        }

        // Call this from the main activity to shutdown the connection
        public void cancel() {
            try {
                //Recently added these.  Commented out mmOutStream.close()in the Write function of this thread
                //mmOutStream.close();
                //mmInStream.close();
                inputStreamIsOpen = false;
                Log.v("ManageConnectionThread", "inputStreamIsOpen flag is set to false since stop button was pressed");

                throw new IOException("blah");
               // mmInStream = null;
                //mmOutStream = null;

                //mmSocket.close();  //Use to be uncommented 1/11/2016
            } catch (IOException e) { }
        }
    }
*/