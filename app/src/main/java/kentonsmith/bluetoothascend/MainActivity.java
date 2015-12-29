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

    private ListView lv;

    //KQS_TO_DO_11_26_PM
    private boolean foundOneSuccessfulConnection = false;

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


    private ArrayList<Integer> ArduinoDataList;

    //NEED TO CHANGE FOR PC, POSSIBLE LOOP THROUGH ALL POSSIBLE UUIDS
    //Randomly off internet; 1d8df488-9d58-11e5-8994-feff819cdc9f

    //well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB
    private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice mainDevice;

    private ArrayList<BluetoothDevice> myDevices;

    private ArrayList<String> uuid_list;


    private class ManageConnectionThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

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
                    if (mmInStream.available() > 0) {
                        try {
                            // Read from the InputStream
                            bytes = mmInStream.read(buffer);
                            readMessage = new String(buffer, 0, bytes);
                            Log.v("ManageConnectionThread", readMessage);


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



                mmOutStream.write(bytes);
                Log.v("ManageConnectionThread", "mmOutStream.writes(" + Arrays.toString(bytes) + ") was successful");

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
                mmOutStream.close();
                mmInStream.close();

                mmSocket.close();
            } catch (IOException e) { }
        }
    }

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

            // Do work to manage the connection (in a separate thread)

            //KQS_TO_DO
            //manageConnectedSocket(mmSocket);
        }




        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void readTestButtonOnClick(View v)
    {
        Log.v("readTestButtonOnClick", "At beginning readTestButtonOnClick");
        Toast toast3 = Toast.makeText(getApplicationContext(), "Read Test Button Pressed", Toast.LENGTH_SHORT);
        toast3.show();

        if(mCT != null)
        {
            //DIALOG STUFF

            AlertDialog.Builder mDialog = new AlertDialog.Builder(MainActivity.this);
            mDialog.setTitle("Is There Incoming Data?");
            mDialog.setMessage("Confirm That Arduino Is Sending Data.\nIf not, app will crash");
            mDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();

                    //MCT.RUN GOES HERE
                    Toast toast5 = Toast.makeText(getApplicationContext(), "Read Operation Confirmed\n mCT.run() stub here", Toast.LENGTH_SHORT);
                    toast5.show();

                    mCT.run();



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
        else
        {
            Toast toast4 = Toast.makeText(getApplicationContext(), "mCT is null.  Not reading anything", Toast.LENGTH_SHORT);
            toast4.show();
            Log.v("readTestButtonOnClick", "mCT is null.  Not reading anything");
        }


    }


    public void writeTestButtonOnClick(View v)
    {
        Log.v("writeTestButtonOnClick", "At beginning writeTestButtonOnClick");
        Toast toast3 = Toast.makeText(getApplicationContext(), "Write Test Button Pressed", Toast.LENGTH_SHORT);
        toast3.show();

        //BigInteger test = new BigInteger("3");

       // byte[] myByteTest = {1,2,3,4,5,6,7,8}; //'a', 'b', 'c', 'd'

        //int send_to_arduino = 7;

        if(mCT != null)
        {
           // byte[] temp = new byte[1];
           // temp[0] = 56;
           // mCT.writeByteArray(temp);

            byte[] temp2 = new byte[2];
            temp2[0] = 97;
            temp2[1] = 98;
            mCT.writeByteArray(temp2);

            Toast toast4 = Toast.makeText(getApplicationContext(), "writeTestButtonOnClick ", Toast.LENGTH_SHORT);
            toast4.show();
            Log.v("writeTestButtonOnClick", "Writing in  writeTestButtonOnClick");


            //size 2



        } else
        {
            Toast toast4 = Toast.makeText(getApplicationContext(), "mCT is null.  Not writing anything", Toast.LENGTH_SHORT);
            toast4.show();
            Log.v("writeTestButtonOnClick", "mCT is null.  Not writing anything");
        }

    }

    private MainActivity.ConnectThread connectedThread;


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


        ArduinoDataList = new ArrayList<Integer>();

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


    /*
    public void bluetoothstartOnClick(View view) {
        Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Button Pressed", Toast.LENGTH_SHORT);
        toast.show();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        Log.v("bluetoothstartOnClick", "Before Bluetooth Adapter");
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.v("bluetoothstartOnClick", "If Statement Bluetooth Button");
            Toast toast2 = Toast.makeText(getApplicationContext(), "Your device does not support bluetooth", Toast.LENGTH_LONG);
            toast2.show();
        }
        else
        {
            //If you have bluetooth but it isn't enabled.  This will start an intent to try to enable it.
            Log.v("bluetoothstartOnClick", "Beginning Else Statement Bluetooth Adapter");
            Log.v("bluetoothstartOnClick", "mBluetoothAdapter == null? " + (mBluetoothAdapter == null));
            Log.v("bluetoothstartOnClick", "mBluetoothAdapter is enabled?? " + mBluetoothAdapter.isEnabled());
            if (!mBluetoothAdapter.isEnabled()) {
                try
                {
                    Log.v("bluetoothstartOnClick", "Before startActivity in try block");
                    fromBluetooth = true;
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    Log.v("bluetoothstartOnClick", "After startActivity in try block");
                }
                catch(Exception e)
                {
                    Log.v("bluetoothstartOnClick", e.toString());
                }

            }
            else  //already connected
            {
                Log.v("bluetoothstartOnClick", "ScanBluetooth being called in case where bluetooth already is enabled");
               // scanBluetooth();
            }
        }
    }
    */

    public void makeConnection()
    {

        /*
        Toast toast3 = Toast.makeText(getApplicationContext(), "In Main Connection Function", Toast.LENGTH_SHORT);
        toast3.show();
        */

        Log.v("makeConnection", "Beginning makeConnection");

        mBluetoothAdapter.cancelDiscovery();

        Log.v("makeConnection", "uuid_list.size() = " + uuid_list.size());


        if(uuid_list.size() == 0)
        {
            String uuid_temp = "00001101-0000-1000-8000-00805f9b34fb"; //Standard SerialPortService ID
            uuid_list.add(uuid_temp);
        }

        Log.v("makeConnection", uuid_list.toString());

        if(uuid_list.size() > 0)
        {

            for(int i = 0; i < uuid_list.size(); i++)
            {
                Log.v("makeConnection", "Trying UUID: " + uuid_list.get(i) + " in thread");

                if(foundOneSuccessfulConnection != true)
                {
                    connectedThread = new ConnectThread(mainDevice, uuid_list.get(i));
                    connectedThread.run();
                }

            }

            //Print into about mmSocket and UUID about device we connected to
            if(foundOneSuccessfulConnection == true)
            {
                Log.v("makeConnection", "Remote device info (address and uuid");
                Log.v("makeConnection" , String.valueOf(mmSocket.getRemoteDevice().getAddress()));
                Log.v("makeConnection", uuid_that_connected.toString());
                Toast toast3 = Toast.makeText(getApplicationContext(), "Successfully connected to Device", Toast.LENGTH_SHORT);
                toast3.show();


                Log.v("makeConnection", "Now starting manageConnectionThreadFunction in makeConnection function");
                manageConnectionThreadFunction(mmSocket);
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




        /*

*/


    }

    public void manageConnectionThreadFunction(BluetoothSocket socket)
    {
        //this code used to be in makeConnection Function

        if(socket != null)
        {

            Log.v("manageConnectionThread", "Starting manage connection thread");
            mCT = new ManageConnectionThread(mmSocket);
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



    public void getAllBluetoothSocketInfo(BluetoothSocket somesocket)
    {
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
}
