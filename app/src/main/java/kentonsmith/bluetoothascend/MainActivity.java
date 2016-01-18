package kentonsmith.bluetoothascend;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {


    private final static int REQUEST_ENABLE_BT = 1;
    private boolean fromBluetooth = false;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter mArrayAdapter;
    private static Handler mHandler;
    private Boolean inputStreamIsOpen;
    private ListView lv;

    //KQS_TO_DO_11_26_PM
    private Boolean foundOneSuccessfulConnection = false;

    //Sony Vaio Laptop mac address "C4:85:08:53:E7:5A"
    //Kenton PC mac address: "00:26:83:32:DA:F8"
    //MPOW: "00:11:22:33:A1:D5"
    private String MAC_ADDRESS_WE_WANT = "C4:85:08:53:E7:5A";

    private BroadcastReceiver mReceiver;
    private BroadcastReceiver uuidReceiver;
    private BluetoothSocket clientSocket;
    private UUID uuid_that_connected = null;
    private BluetoothSocket mmSocket;
    private ManageConnectionThread mCT;
    private StringBuffer sb;
    private ArrayList<String> ArduinoDataList;

    //NEED TO CHANGE FOR PC, POSSIBLE LOOP THROUGH ALL POSSIBLE UUIDS
    //Randomly off internet; 1d8df488-9d58-11e5-8994-feff819cdc9f

    //well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB
    private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice mainDevice;
    private ArrayList<BluetoothDevice> myDevices;
    private ArrayList<String> uuid_list;


    private class ManageConnectionThread extends Thread {
        //private final BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

       // private final InputStream mmInStream;
       // private final OutputStream mmOutStream;

        public ManageConnectionThread(BluetoothSocket socket) {
            mmSocket = socket;
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

        /* Call this from the main activity to send data to the remote device */

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

        /* Call this from the main activity to shutdown the connection */
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


    // KQSNewcomment

    private class ConnectThread extends Thread {
        private BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device, String uuid_string) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code

                //Tried connecting insecure and secure KQS_TO_DO
                 UUID my_uuid = UUID.fromString(uuid_string);

                tmp = device.createRfcommSocketToServiceRecord(my_uuid);
                Log.v("ConnectThread","createRfcommSocket successful in ConnectedThread constrcutor");
                uuid_that_connected = my_uuid;
            } catch (IOException e) {
                Log.v("ConnectThread","createRfcommSocket failed in ConnectedThread constrcutor");
            }
            mmSocket = tmp;

        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();
            Log.v("ConnectThread", "mmSocket.isConnected? " + mmSocket.isConnected());

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.v("ConnectThread", "mmSocket.isConnected? " + mmSocket.isConnected());
                Log.v("ConnectThread", "mmSocket.connect() successful run() method inside thread");

                //don't look for any more
                foundOneSuccessfulConnection = true;

                Log.v("ConnectThread", "mmSocket is now connected to " + mmSocket.getRemoteDevice().getName());
                Log.v("ConnectThread", "uuid for mmsocket connection is " + uuid_that_connected.toString());

                //Now begin manage connection thread

            } catch (IOException connectException) {

                Log.v("ConnectThread", "mmSocket.connect() failed in run() of thread");
                Log.v("ConnectThread", "connectException.toString() = " + connectException.toString());
                Log.e("Spam2", connectException.toString());
                // Unable to connect; close the socket and get out
                try {

                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.v("ConnectThread", "closeException.toString() = " + closeException.toString());
                    Log.e("Spam2",closeException.toString());
                }

                //AGGRESSIVE APPROACH KQS_TO_DO
                //makeConnection();
                return;
            }

        }

        //Will cancel an in-progress connection, and close the socket
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }



    public void whatWasReadInButtonOnClick(View v)
    {
        Log.v("ReadInButtonOnClick", "printing arduinio data in logcat");

        String[] split_on_ampersand = sb.toString().split("&");

        Log.v("ReadInButtonOnClick", "Time elapsed(milliseconds), EulerX (angle), EulerY (angle), EulerZ (angle), GyroX (rad/s), GyroY (rad/s), GyroZ (rad/s), Lin_AccX (m/s^2), Lin_AccY (m/s^2), Lin_AccZ (m/s^2)");
        for(int i = 0; i < split_on_ampersand.length; i++)
        {
            split_on_ampersand[i] = split_on_ampersand[i].replace("$",",");

            Log.v("ReadInButtonOnClick", split_on_ampersand[i]);
        }


        //Log.v("ReadInButtonOnClick", Arrays.toString(split_on_ampersand));
    }


    //used to be called readTestButtonOnClick
    public void startReadOnClick(View v)
    {
        Log.v("startReadOnClick", "At beginning startReadOnClick");
        Toast toast3 = Toast.makeText(getApplicationContext(), "startReadOnClick Button Pressed", Toast.LENGTH_SHORT);
        toast3.show();


        if(mCT == null)
        {
            manageConnectionThreadFunction(mmSocket);  //start reading WHEN we want instead of immediately after connecting
        }

        if(mCT == null)  //if it is still equal to null after manage connection thread than that means we could not establish thread from socket
        {
            Toast toast4 = Toast.makeText(getApplicationContext(), "mCT is null.  Not reading anything", Toast.LENGTH_SHORT);
            toast4.show();
            Log.v("startReadOnClick", "mCT is null.  Not reading anything");
            return;  //unsuccessful

        }
        else  //mCT != null
        {

            Log.v("startReadOnClick", "mCT.getState().toString() = " + mCT.getState().toString());

            //case where thread is not null and already running, don't want to start another one
            if(!mCT.getState().equals(Thread.State.NEW))  //nn\ot a new thread, already running
            {
                Log.v("startReadOnClick", "Current thread state = " + mCT.getState().toString());
                Toast toast7 = Toast.makeText(getApplicationContext(), "Thread is currently running. Cannot perform a read while reading!", Toast.LENGTH_SHORT);
                toast7.show();


                Log.v("startReadOnClick", "inputStreamIsOpen is being set to true - should be continuing to read");

                inputStreamIsOpen = true;  //ensure that we are reading in stuff

                byte[] read_signal = new byte[1];
                read_signal[0] = 82;  //capital 'R'   tell arduino it is ok to start sendign data again

                mCT.writeByteArray(read_signal);

                return; // exit out of function, we are already reading
            }


            //DIALOG STUFF

            /*
            if(mmSocket == null)
            {
                connectedThread.shortCutConnect();
            }
*/

            AlertDialog.Builder mDialog = new AlertDialog.Builder(MainActivity.this);
            mDialog.setTitle("Is There Incoming Data?");
            mDialog.setMessage("Confirm That Arduino Is Sending Data.\nIf not, app will crash");
            mDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();

                    //MCT.RUN GOES HERE
                    Toast toast5 = Toast.makeText(getApplicationContext(), "Read Operation Confirmed\n mCT.run() stub here", Toast.LENGTH_SHORT);
                    toast5.show();

                    inputStreamIsOpen = true;

                    Log.v("startReadOnClick", "inputStreamIsOpen flag is set to true since read button pressed");


                    //used to by mCT.run()  KENTON SMITH 12/30/2015
                    mCT.start();

                    byte[] read_signal = new byte[1];
                    read_signal[0] = 82;  //capital 'R'

                    mCT.writeByteArray(read_signal);

                    Log.v("startReadOnClick", "Main UI stuff after mCT.start()");

                }
            });
            mDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();

                    //MCT.RUN DOES HERE
                    Toast toast5 = Toast.makeText(getApplicationContext(), "Read Operation Cancelled", Toast.LENGTH_SHORT);
                    toast5.show();

                }
            });

            AlertDialog alert = mDialog.create();
            alert.show();

        }



    }

    public void stopReadAndResetArduinoTimeOnClick(View v)
    {
        if(mCT == null)
        {
            Toast toast4 = Toast.makeText(getApplicationContext(), "mCT is null.  No reading process to stop", Toast.LENGTH_SHORT);
            toast4.show();
            Log.v("stopReadOnClick", "mCT is null. No reading process to stop");
        }
        else {

            Log.v("stopReadOnClick", "Current thread state = " + mCT.getState().toString());


            byte[] read_signal = new byte[1];
            read_signal[0] = 83;  //capital 'S'   Tell arduino to stop writing data
            mCT.writeByteArray(read_signal);
            Log.v("stopReadOnClick", "Writing to bluetooth to stop = " + read_signal.toString());


          //  Log.v("stopReadOnClick", "Attempting to interrupt mCT");

            //     mCT.interrupt();

            Log.v("stopReadOnClick", "Attempting to cancel mCT");

            inputStreamIsOpen = false;   //instead of cancelling thread, just switch flag to keep data halted, but not shut off
           // mCT.cancel(); //set inputsteamreadin to false

            Log.v("stopReadOnClick", "Current thread state = " + mCT.getState().toString());


//            mCT = null;



        }

    }


    private ConnectThread connectedThread;


    public void awsIntentOnClick(View v)
    {
        Intent intent = new Intent(this, AWS_Functionality_Test.class);
        startActivity(intent);

    }

    public void makeDiscoverableOnClick(View v)
    {

        Toast toast3 = Toast.makeText(getApplicationContext(), "Make Discoverable Button Clicked)", Toast.LENGTH_SHORT);
        toast3.show();
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

    }

    /*
    public void sendDataOnClick(View v)
    {
        EditText text = (EditText) findViewById(R.id.sendDataEditText);
        Toast toast3 = Toast.makeText(getApplicationContext(), "Planning on sending bytes " + text.getText().toString(), Toast.LENGTH_SHORT);
        toast3.show();
        Log.v("onActivityResult", "sending bytes = " + text.getText().toString());
    }
    */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new AdafruitDataHandler(this);

        inputStreamIsOpen = false;

        sb = new StringBuffer("");

        ArduinoDataList = new ArrayList<String>();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //uuid_list is where uuid's for the device we choose to connect to are stored
        uuid_list = new ArrayList<String>();

        //global variable (private member variable) for BluetoothDevice object so we have  access to it in all functions
        mainDevice = null;

        //list of devices returned in nearby scan.  Will be used eventually to populate array adapter for click handler
        //this is so that user can click on the nearby devices they want to connect to.
        myDevices = new ArrayList<BluetoothDevice>();

        //Array adapter UI object (current blank until after scan has been done
        mArrayAdapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, android.R.id.text1);


        //Broadcast receiver for SCAN
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.v("mReceiver", "In mReceiver broadcast receiver after discovery begin in scanBluetooth()");

                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    Log.v("mReceiver", device.getAddress());
                    myDevices.add(device);

                    /*
                    if(device.getAddress().equals(MAC_ADDRESS_WE_WANT))
                    {
                        mainDevice = device;  //device we want to connect to
                        //  -- find out as much as we can about the device TO MAKE CONNECTION
                        getAllBluetoothDeviceInfo(mainDevice);
                    }
                    */
                    // Add the name and address to an array adapter to show in a ListView
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress() + "\n Source: Devices Discovered In Scan");
                }
            }
        };

        uuidReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                String action = intent.getAction();
                Log.v("uuidReceiver", "action = " + action);

                //BluetoothDevice deviceExtra = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (BluetoothDevice.ACTION_UUID.equals(action)){
                    Log.v("uuidReceiver", "Received Intent created by fetchUuidsWithSdp()");
                    Parcelable[] uuidExtra =intent.getParcelableArrayExtra("android.bluetooth.device.extra.UUID");

                    boolean found = false;
                    Log.v("uuidReceiver", "uuid array is null? " + (uuidExtra == null));

                    if(uuidExtra != null)
                    {
                        Log.v("uuidReceiver", "Now printing UUIDS supported by server device");

                        for(int i=0;i<uuidExtra.length;i++){
                            Log.v("uuidReceiver", "UUID:   " + uuidExtra[i]);

                            uuid_list.add(uuidExtra[i].toString());

                            if((uuidExtra[i].toString()).equals(MY_UUID.toString())){
                                found = true;
                                Log.v("uuidReceiver","Match found in loop");

                            }
                        }

                        Log.v("uuidReceiver", "Calling makeConnection from uuidReceiveer.  In future will loop through all UUIDS");
                       // makeConnection();  SAVE THIS FOR LATER
                    }
                }
                // }
            }
        };

        /*
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        IntentFilter filter2 = new IntentFilter((BluetoothDevice.ACTION_UUID));
        registerReceiver(uuidReceiver, filter2);

        */
    }

    //Only do this AFTER we know Bluetooth is enabled on the device
    public void initializeReceivers()
    {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        IntentFilter filter2 = new IntentFilter((BluetoothDevice.ACTION_UUID));
        registerReceiver(uuidReceiver, filter2);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unregisterReceiver(uuidReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void clearButtonOnClick(View view) {
        Toast toast = Toast.makeText(getApplicationContext(), "Clear Button Pressed", Toast.LENGTH_SHORT);
        toast.show();
        mArrayAdapter.clear();
        mArrayAdapter.notifyDataSetChanged();
    }


    public void enableBluetoothOnClick(View view)
    {
        Log.v("enableBluetoothOnClick", "Inside enableBluetoothOnClick");

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.v("enableBluetoothOnClick", "mBluetoothAdapter is currently null.  Device does not support bluetooth");
            Toast toast2 = Toast.makeText(getApplicationContext(), "Your device does not support bluetooth", Toast.LENGTH_LONG);
            toast2.show();
        }
        else
        {
            //If you have bluetooth but it may or may not be enabled.  This will start an intent to try to enable it.
            Log.v("enableBluetoothOnClick", "Beginning Else Statement Bluetooth Adapter");
            Log.v("enableBluetoothOnClick", "mBluetoothAdapter == null? " + (mBluetoothAdapter == null));  //SHOULD BE FALSE
            Log.v("enableBluetoothOnClick", "mBluetoothAdapter is enabled?? " + mBluetoothAdapter.isEnabled());
            if (!mBluetoothAdapter.isEnabled()) {
                try
                {
                    Log.v("enableBluetoothOnClick", "Before startActivityForResult intent in try block");
                    fromBluetooth = true;
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    Log.v("enableBluetoothOnClick", "After startActivity in try block");
                }
                catch(Exception e)
                {
                    Log.v("enableBluetoothOnClick", e.toString());
                }

            }
            else  //already connected
            {
                Toast toast3 = Toast.makeText(getApplicationContext(), "Bluetooth Already Enabled", Toast.LENGTH_LONG);
                toast3.show();
                Log.v("enableBluetoothOnClick", "ScanBluetooth being called in case where bluetooth already is enabled");
            }
        }

    }

    public void makeConnection()
    {
        Log.v("makeConnection", "Beginning makeConnection");

        mBluetoothAdapter.cancelDiscovery();

        Log.v("makeConnection", "uuid_list.size() = " + uuid_list.size());

        if(uuid_list.size() == 0)
        {
            String uuid_temp = "00001101-0000-1000-8000-00805f9b34fb"; //Standard SerialPortService ID
            uuid_list.add(uuid_temp);
        }

        Log.v("makeConnection", uuid_list.toString());

       // BooleanWrapper booleanWrapper = new BooleanWrapper(false);

        if(uuid_list.size() > 0)
        {

            for(int i = 0; i < uuid_list.size(); i++)
            {
                Log.v("makeConnection", "Trying UUID: " + uuid_list.get(i) + " in thread");

                if(foundOneSuccessfulConnection.booleanValue() !=  true)
                {
                    // booleanWrapper = new BooleanWrapper(false);
                    //connectedThread = new ConnectThread(mainDevice, uuid_list.get(i), uuid_that_connected, mmSocket, mBluetoothAdapter, booleanWrapper);
                    connectedThread = new ConnectThread(mainDevice, uuid_list.get(i)); //KQSNewComment
                    connectedThread.run();
                }
            }

           // foundOneSuccessfulConnection = booleanWrapper.val;

            //Print into about mmSocket and UUID about device we connected to
            if(foundOneSuccessfulConnection == true)
            {
                Log.v("makeConnection", "Remote device info (address and uuid");
                Log.v("makeConnection", String.valueOf(mmSocket.getRemoteDevice().getAddress()));
                Log.v("makeConnection", uuid_that_connected.toString());
                Toast toast3 = Toast.makeText(getApplicationContext(), "Successfully connected to Device", Toast.LENGTH_SHORT);
                toast3.show();


                Log.v("makeConnection", "Now starting manageConnectionThreadFunction in makeConnection function");

                //manageConnectionThreadFunction(mmSocket);  put in read button by KQS 1/11/2016

                //mmSocket is now one we want
                //connectedThread is now one we want

                //try to start manage thread here inside of inside of ConnectedThread?


                //CALL manageConnectionThreadFunction Here
            }
            else
            {
                Toast toast3 = Toast.makeText(getApplicationContext(), "Failed to Connect to Device", Toast.LENGTH_SHORT);
                toast3.show();
            }
        }
    }

    public void manageConnectionThreadFunction(BluetoothSocket socket)
    {
        //this code used to be in makeConnection Function

        if(socket != null)
        {

            Log.v("manageConnectionThread", "Starting manage connection thread");
            mCT = new ManageConnectionThread(mmSocket);
            Log.v("manageConnectionThread", "mCT set equal to new ManageConnectionThread(mmSocket)");
           // mCT.run();
           // byte[] test = {0,1,2};
           // mCT.write(test);
        }
    }

    public BluetoothDevice findDeviceUserClickedOn(ArrayList<BluetoothDevice> btlist, String address)
    {
        for(int i = 0; i < btlist.size(); i++)
        {
            if(btlist.get(i).getAddress().equals(address))
            {
                return btlist.get(i);
            }
        }
        return null;
    }


    public void scanButtonOnClick(View view)
    {
        Toast toast2 = Toast.makeText(getApplicationContext(), "Scan bluetooth being called", Toast.LENGTH_SHORT);
        toast2.show();

        lv = (ListView) findViewById(R.id.pairedDevicesList);

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress() + "\n Source: Paired Devices Already Saved");
            }
        }

        boolean passed_criteria_to_scan = false;

        if(mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
        {
            passed_criteria_to_scan = true;
        }

        if(passed_criteria_to_scan)
        {
            Log.v("scanButtonOnClick", "Passed criteria.  Now initializing receivers.  Then will begin scan");
            initializeReceivers();
            mBluetoothAdapter.startDiscovery();  //discover stuff and add to array adapter before displaying adapter
            lv.setAdapter(mArrayAdapter);

            //http://stackoverflow.com/questions/8615417/how-can-i-set-onclicklistener-on-arrayadapter
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Log.v("scanButtonOnClick", "in onItemClick for array adapter element");
                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                    Log.v("scanButtonOnClick", "Attempting to print text in current listview element \n" + textView.getText().toString());

                    final String[] Info_Split_Into_Array = textView.getText().toString().split("\n");
                    Log.v("scanButtonOnClick", Arrays.toString(Info_Split_Into_Array));

                    final String mac_address_will_attempt_to_connect_to = Info_Split_Into_Array[1];
                    Log.v("scanButtonOnClick", mac_address_will_attempt_to_connect_to);

                    //DIALOG STUFF

                    AlertDialog.Builder mDialog = new AlertDialog.Builder(MainActivity.this);
                    mDialog.setTitle("Connection Prompt");
                    mDialog.setMessage("Would you like to connect to...\n" + "Name: " + Info_Split_Into_Array[0] + "\n"
                    + "MAC Address: " + Info_Split_Into_Array[1] + "?");
                    mDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            MAC_ADDRESS_WE_WANT = mac_address_will_attempt_to_connect_to;

                            mainDevice = findDeviceUserClickedOn(myDevices, Info_Split_Into_Array[1]);
                            Log.v("scanButtonOnClick", "mainDevice == null? " + (mainDevice == null));
                            if(mainDevice != null)
                            {
                                Log.v("scanButtonOnClick", "mainDevice name = " + mainDevice.getAddress());

                                //this also gets all UUIDS connected to this particular device
                                getAllBluetoothDeviceInfo(mainDevice);

                                //establishes non-null mmSocket for input/output stream
                                makeConnection();

                                //makeConnectionHere
                            }
                            else
                            {
                                Toast toast6 = Toast.makeText(getApplicationContext(), "Device you are trying to connect to is null \nNOT IN RANGE", Toast.LENGTH_LONG);
                                toast6.show();
                                //TOAST HERE
                            }

                        }
                    });
                    mDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Log.v("scanButtonOnClick", "Cancelled connecting to device");
                            dialog.cancel();
                        }
                    });
                    AlertDialog alert = mDialog.create();
                    alert.show();

                }
            });
        }
        else
        {
            Log.v("scanButtonOnClick", "Criteria not met to run scan.  Please enable bluetooth");
            Toast toast5 = Toast.makeText(getApplicationContext(), "Criteria not met to run scan.  Please enable bluetooth", Toast.LENGTH_LONG);
            toast5.show();
        }
    }

    //Inputs: This device takes in a BluetoothDevice object
    //Ouputs: This function does nothing but sideeffects.  It prints out all related .toString() values of
    //all of its member variables such as the device's MAC address.  Log tag to filter by is getAllBTDeviceInfo
    public void getAllBluetoothDeviceInfo(BluetoothDevice device)
    {
        Log.v("getAllBTDeviceInfo", "Begin getAllBluetoothDeviceInfo ");
        boolean has_uuids_with_sdp = device.fetchUuidsWithSdp();
        Log.v("getAllBTDeviceInfo", "has_uuids_with_sdp = " + has_uuids_with_sdp);

        ParcelUuid[] uuids = device.getUuids();
        Log.v("getAllBTDeviceInfo", "uuids is null? " + (uuids == null));

        if(uuids != null)
        {
            for(int i = 0; i < uuids.length; i++)
            {
                Log.v("getAllBTDeviceInfo", "uuid number " + i + ", " + uuids[i].toString());
            }
        }

        String address = device.getAddress();
        Log.v("getAllBTDeviceInfo", "address  = " + address);

        int bond_state = device.getBondState();
        Log.v("getAllBTDeviceInfo", "bond_state = " + bond_state);

        String name = device.getName();
        Log.v("getAllBTDeviceInfo", "name = " + name);

        int type = device.getType();
        Log.v("getAllBTDeviceInfo", "type = " + type);

        String toStringOfDeviceObject = device.toString();
        Log.v("getAllBTDeviceInfo", "toStringOfDeviceObject = " + toStringOfDeviceObject);

        BluetoothClass btClass = device.getBluetoothClass();
        Log.v("getAllBTDeviceInfo", "btClass.toString() =  " + btClass.toString());
        Log.v("getAllBTDeviceInfo", "btClass.getDeviceClass() =  " + btClass.getDeviceClass());
        Log.v("getAllBTDeviceInfo", "btClass.getMajorDeviceClass() =  " + btClass.getMajorDeviceClass());
        Log.v("getAllBTDeviceInfo", "End getAllBluetoothDeviceInfo ");
    }


    //Inputs: An intent received from a different function (most notably, enableBluetoothOnClick())
    //Ouputs: User enabling bluetooth or not
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        Log.v("onActivityResult", getIntent().getAction());
        Log.v("onActivityResult", "resultCode = " + resultCode);
        Toast toast5 = Toast.makeText(getApplicationContext(), "Currently In Activity Result", Toast.LENGTH_LONG);
        toast5.show();
        if (fromBluetooth == true && resultCode == RESULT_OK) {
            Toast toast2 = Toast.makeText(getApplicationContext(), "RESULT_OK for enabling bluetooth", Toast.LENGTH_LONG);
            toast2.show();
            Log.v("onActivityResult", "User has now enabled bluetooth successfully");
            fromBluetooth = false;  //reset back to false.  This is just a flag telling us what intent stuff comes from
            //scanBluetooth();
        } else if(fromBluetooth == true && resultCode == RESULT_CANCELED){
            Log.v("onActivityResult", "User has canceled process of enabling bluetooth");
            Toast toast2 = Toast.makeText(getApplicationContext(), "RESULT_CANCELLED for cancelling bluetooth enable", Toast.LENGTH_LONG);
            toast2.show();
            fromBluetooth = false;
        }
        else
        {
            Toast toast2 = Toast.makeText(getApplicationContext(), "Other Result", Toast.LENGTH_LONG);
            toast2.show();
            fromBluetooth = false;
        }
    }

    public ArrayList<String> meetsCriteriaTenComponents(String parseChunk)
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
