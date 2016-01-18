package kentonsmith.bluetoothascend;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by admin on 1/17/2016.
 */

public class ConnectThread extends Thread {
    private BluetoothDevice mmDevice;

    private BluetoothSocket mmSocket;

    private BluetoothAdapter mBluetoothAdapter;

    private BooleanWrapper foundOneSuccessfulConnection;

    private UUID uuid_that_connected;

    public ConnectThread(BluetoothDevice device, String uuid_string, UUID globalUUID, BluetoothSocket globalSocket, BluetoothAdapter globalAdapter, BooleanWrapper globalBoolean) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;

        this.mmSocket = globalSocket;
        this.mBluetoothAdapter = globalAdapter;
        this.uuid_that_connected = globalUUID;
        this.foundOneSuccessfulConnection = globalBoolean;

        Log.v("ConnectThread", "globalBoolean = " + globalBoolean );

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code

            //Tried connecting insecure and secure KQS_TO_DO
            UUID my_uuid = UUID.fromString(uuid_string);

            tmp = device.createRfcommSocketToServiceRecord(my_uuid);
            Log.v("ConnectThread", "createRfcommSocket successful in ConnectedThread constrcutor");
            this.uuid_that_connected = my_uuid;
            this.mmSocket = tmp;


           // uuid_that_connected = my_uuid;
        } catch (IOException e) {
            Log.v("ConnectThread","createRfcommSocket failed in ConnectedThread constrcutor");
        }
      //  mmSocket = tmp;
        this.mmSocket = tmp;
       // globalSocket = tmp;
       // globalBoolean = true;

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
            this.foundOneSuccessfulConnection = new BooleanWrapper(true);
            Log.v("ConnectThread", "foundOneSuccessfulConnection set = true inside class");

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

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}

