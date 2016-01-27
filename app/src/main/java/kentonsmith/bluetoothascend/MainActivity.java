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
import android.content.IntentSender;
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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.*;
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.ChangeListener;
//import gms.drive.*;


//Got implements from http://stackoverflow.com/questions/23751905/error-implementing-googleapiclient-builder-for-android-development
public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private DriveId ascendDataFolder;

    private DriveFolder mDriveFolder;
    private DriveFile someFile;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int RESOLVE_CONNECTION_REQUEST_CODE = 500;
    private boolean fromBluetooth = false;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter mArrayAdapter;
    private static AdafruitDataHandler mHandler;
    private BooleanWrapper inputStreamIsOpen;
    private ListView lv;

    private static final int REQUEST_CODE_RESOLUTION = 3;

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
    private StringBuffer totalString;
    private ArrayList<String> ArduinoDataList;
    private ArrayList<String> globalLines;

    private DriveId mFolderDriveId;

    //RESOURCE ID
    private final static String EXISTING_FOLDER_ID = "0B5a_d0CBZLskMjliVnoycmQwLUU";

    //NEED TO CHANGE FOR PC, POSSIBLE LOOP THROUGH ALL POSSIBLE UUIDS
    //Randomly off internet; 1d8df488-9d58-11e5-8994-feff819cdc9f

    //well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB
    private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice mainDevice;
    private ArrayList<BluetoothDevice> myDevices;
    private ArrayList<String> uuid_list;

    private GoogleApiClient mGoogleApiClient;

    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.v("createFile","Error while trying to create new file contents");
                        return;
                    }
                    final DriveContents driveContents = result.getDriveContents();

                    Log.v("createFile","creating thread");


                    // Perform I/O off the UI thread.
                    new Thread() {
                        @Override
                        public void run() {
                            // write content to DriveContents
                            OutputStream outputStream = driveContents.getOutputStream();
                            Writer writer = new OutputStreamWriter(outputStream);
                            try {
                                for(int i = 0; i < globalLines.size(); i++)
                                {

                                  writer.write(globalLines.get(i) + "\n");
                                }
                                //writer.write("Hello World!");
                                writer.close();
                            } catch (IOException e) {
                                Log.e("CreateFile", e.getMessage());
                            }

                            Date myDate = new Date();



                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle("AscendData_"+myDate.toString()).setMimeType("text/csv").setStarred(true).build();


                            // MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                             //       .setTitle("Ascend_"+myDate.toString()).setMimeType("text/plain").setStarred(true).build();

                                   // .setMimeType("text/plain")


                            DriveFolder folder = mFolderDriveId.asDriveFolder();
                            folder.createFile(mGoogleApiClient, changeSet, driveContents)
                                    .setResultCallback(fileCallback);

                            // create a file on root folder
                            /*
                            Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                    .createFile(mGoogleApiClient, changeSet, driveContents)
                                    .setResultCallback(fileCallback);
                                    */

                            //reset strings for next run
                            globalLines.clear();
                            totalString.setLength(0);
                            sb.setLength(0);

                            //globalLines = new ArrayList<String>();
                            //totalString = new StringBuffer("");
                            //sb = new StringBuffer("");
                        }
                    }.start();
                }
            };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        //showMessage("Error while trying to create the file");
                        return;
                    }
                   // showMessage("Created a file with content: " + result.getDriveFile().getDriveId());
                }
            };

    public void createFile()
    {
        Log.v("createFile", "mGoogleApiClient.isConnected() ?" + mGoogleApiClient.isConnected());
//        Log.v("createFile", "mGoogleApiClient.isConnected() ?" + mGoogleApiClient.isConnected());


      //  Drive.DriveApi.fetchDriveId()

        Drive.DriveApi.fetchDriveId(mGoogleApiClient, EXISTING_FOLDER_ID)
                .setResultCallback(idCallback);
        //DriveId:CAESABi-KiCarZCPplIoAQ==
       // Drive.DriveApi.fetchDriveId(mGoogleApiClient, "CAESABi-KiCarZCPplIoAQ==")
         //       .setResultCallback(idCallback);

        //WORKS (direct route without getting folder)
       // Drive.DriveApi.newDriveContents(mGoogleApiClient)
        //        .setResultCallback(driveContentsCallback);
    }

    final private ResultCallback<DriveApi.DriveIdResult> idCallback = new ResultCallback<DriveApi.DriveIdResult>() {
        @Override
        public void onResult(DriveApi.DriveIdResult result) {
            if (!result.getStatus().isSuccess()) {
                Log.v("createFile","Cannot find DriveId. Are you authorized to view this file?");
                return;
            }
            Log.v("createFile", result.getDriveId().toString());
            mFolderDriveId = result.getDriveId();
            Drive.DriveApi.newDriveContents(mGoogleApiClient)
                  .setResultCallback(driveContentsCallback);
            //Drive.DriveApi.newDriveContents(getGoogleApiClient())
              //      .setResultCallback(driveContentsCallback);
        }
    };

    public void createFolderToGetID()
    {
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("Ascend_Data").build();


        Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                mGoogleApiClient, changeSet).setResultCallback(folderCreatedCallback);


    }

    ResultCallback<DriveFolder.DriveFolderResult> folderCreatedCallback = new
            ResultCallback<DriveFolder.DriveFolderResult>() {
                @Override
                public void onResult(DriveFolder.DriveFolderResult result) {
                    if (!result.getStatus().isSuccess()) {
                        //("Error while trying to create the folder");
                        return;
                    }

              //  result.getDriveFolder().addChangeSubscription(changeListener);
                    //ascendDataFolder = result.getDriveFolder().getDriveId();

                    mDriveFolder = result.getDriveFolder();
                    mDriveFolder.addChangeListener(mGoogleApiClient, changeListener);

                   // result.getDriveFolder().addChangeListener(mGoogleApiClient, changeListener);
                     //someFile = ascendDataFolder.asDriveFile();
//                    someFile.addChangeSubscription((GoogleApiClient) changeListener);
                   // Log.v("createFolder", "encode to string = " + ascendDataFolder.encodeToString());
                    //Log.v("createFolder", "to string = " + ascendDataFolder.toString());
                   // Log.v("createFolder", "encode to string = " + ascendDataFolder.);
                    //android.os.SystemClock.sleep(5000);

                    //Log.v("createFolder", "resource ID = " + result.getDriveFolder().getDriveId().getResourceId());
                    //String s = result.getDriveFolder().getDriveId().getResourceId();
                }
            };

    final private ChangeListener changeListener = new ChangeListener() {
        @Override
        public void onChange(ChangeEvent event) {
            //event.
            Log.v("createFolder", "Resource ID = " + mDriveFolder.getDriveId().getResourceId());
            Log.v("createFolder", "Resource ID = " + event.getDriveId().getResourceId());
        }
    };

    public void whatWasReadInButtonOnClick(View v)
    {
        Log.v("ReadInButtonOnClick", "printing arduinio data in logcat");

        ArrayList<String> lines = new ArrayList<String>();

        lines.add( "Time elapsed(milliseconds), EulerX (angle), EulerY (angle), EulerZ (angle), GyroX (rad/s), GyroY (rad/s), GyroZ (rad/s), Lin_AccX (m/s^2), Lin_AccY (m/s^2), Lin_AccZ (m/s^2)");

        String[] split_on_ampersand = totalString.toString().split("&");

        Log.v("ReadInButtonOnClick", "Time elapsed(milliseconds), EulerX (angle), EulerY (angle), EulerZ (angle), GyroX (rad/s), GyroY (rad/s), GyroZ (rad/s), Lin_AccX (m/s^2), Lin_AccY (m/s^2), Lin_AccZ (m/s^2)");
        for(int i = 0; i < split_on_ampersand.length; i++)
        {
            split_on_ampersand[i] = split_on_ampersand[i].replace("$",",");
            lines.add(split_on_ampersand[i]);
            Log.v("ReadInButtonOnClick", split_on_ampersand[i]);
        }
        //Log.v("ReadInButtonOnClick", Arrays.toString(split_on_ampersand));

        this.globalLines = lines;

       // createFolderToGetID();
       createFile();
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

                inputStreamIsOpen.setVal(true);  //ensure that we are reading in stuff

                byte[] read_signal = new byte[1];
                read_signal[0] = 82;  //capital 'R'   tell arduino it is ok to start sendign data again

                mCT.writeByteArray(read_signal);

                return; // exit out of function, we are already reading
            }

            AlertDialog.Builder mDialog = new AlertDialog.Builder(MainActivity.this);
            mDialog.setTitle("Is There Incoming Data?");
            mDialog.setMessage("Confirm That Arduino Is Sending Data.\nIf not, app will crash");
            mDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();

                    //MCT.RUN GOES HERE
                    Toast toast5 = Toast.makeText(getApplicationContext(), "Read Operation Confirmed\n mCT.run() stub here", Toast.LENGTH_SHORT);
                    toast5.show();

                    inputStreamIsOpen.setVal(true);

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
        } else {

            Log.v("stopReadOnClick", "Current thread state = " + mCT.getState().toString());


            byte[] read_signal = new byte[1];
            read_signal[0] = 83;  //capital 'S'   Tell arduino to stop writing data
            mCT.writeByteArray(read_signal);
            Log.v("stopReadOnClick", "Writing to bluetooth to stop = " + read_signal.toString());


          //  Log.v("stopReadOnClick", "Attempting to interrupt mCT");

            //     mCT.interrupt();

            Log.v("stopReadOnClick", "Attempting to cancel mCT");

            inputStreamIsOpen.setVal(false);   //instead of cancelling thread, just switch flag to keep data halted, but not shut off
           // mCT.cancel(); //set inputsteamreadin to false

            Log.v("stopReadOnClick", "Current thread state = " + mCT.getState().toString());
//            mCT = null;
        }
    }

    //private ConnectThread connectedThread;
    private ConnectThread2 connectThread2;

    /*
    public void awsIntentOnClick(View v)
    {
        Intent intent = new Intent(this, AWS_Functionality_Test.class);
        startActivity(intent);

    }
    */

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient.isConnected() == false)
        {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v("onConnectionSuspended", "mGoogleApiClient connection is suspended");

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("onConnected", "mGoogleApiClient is connected");
        Toast toast3 = Toast.makeText(getApplicationContext(), "mGoogleApiClient is connected", Toast.LENGTH_SHORT);
        toast3.show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v("onConnectionFailed", "In onConnectionFailed after attempting mGoogleApiClient.connect()");
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Log.v("onCreate", "mGoogleApiClient ==  null? " + (mGoogleApiClient == null));

       // mGoogleApiClient.connect();
        globalLines = new ArrayList<String>();

        mHandler = new AdafruitDataHandler(this);

        inputStreamIsOpen = new BooleanWrapper(false);

        sb = new StringBuffer("");
        totalString = new StringBuffer("");
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


        /*
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("New file")
                .setMimeType("text/plain").build();

        mDriveFolder.createFolder(mGoogleApiClient, changeSet);
*/

       // Log.v("createFolder", "Resource ID = " + ascendDataFolder.getResourceId());
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

       BooleanWrapper booleanWrapper = new BooleanWrapper(false);

        BluetoothSocketWrapper bSocketWrapper = new BluetoothSocketWrapper(mmSocket);

        UUID_Wrapper uuidWrapper = new UUID_Wrapper(uuid_that_connected);

        if(uuid_list.size() > 0)
        {

            for(int i = 0; i < uuid_list.size(); i++)
            {
                Log.v("makeConnection", "Trying UUID: " + uuid_list.get(i) + " in thread");

                if(foundOneSuccessfulConnection.booleanValue() !=  true)
                {
                    // booleanWrapper = new BooleanWrapper(false);
                    //connectedThread = new ConnectThread(mainDevice, uuid_list.get(i), uuid_that_connected, mmSocket, mBluetoothAdapter, booleanWrapper);

                    connectThread2 = new ConnectThread2(mainDevice, uuid_list.get(i), uuidWrapper, mBluetoothAdapter, bSocketWrapper, booleanWrapper);
                    connectThread2.run();
                   //WORKING
                    //connectedThread = new ConnectThread(mainDevice, uuid_list.get(i)); //KQSNewComment
                    //connectedThread.run();
                }
            }

            foundOneSuccessfulConnection = booleanWrapper.getVal();
            mmSocket = bSocketWrapper.getSocket();
            uuid_that_connected = uuidWrapper.getUuid();

            //Print into about mmSocket and UUID about device we connected to
            if(foundOneSuccessfulConnection == true)
            {
                Log.v("makeConnection", "Remote device info (address and uuid");
                if(mmSocket == null)
                {
                    Log.v("makeConnection", "mmSocket is null KQS");
                }
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
 //    public ManageConnectionThread(BluetoothSocket globalSocket, StringBuffer globalStringBuffer, AdafruitDataHandler globalHandler, BooleanWrapper globalBoolean) {

          mCT = new ManageConnectionThread(mmSocket, sb, mHandler, inputStreamIsOpen, totalString);
          //  mCT = new ManageConnectionThread(mmSocket);  OLD ONE THAT WORKED
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
        if(requestCode == RESOLVE_CONNECTION_REQUEST_CODE)
        {
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            }
            return;
        }



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
