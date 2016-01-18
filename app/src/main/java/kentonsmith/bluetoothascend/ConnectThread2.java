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


public class ConnectThread2 extends Thread {
    private BluetoothDevice mmDevice;

    private BooleanWrapper wrapper;

    private BluetoothAdapter innerAdapter;

    private BluetoothSocketWrapper innerSocketWrapper;

    private UUID_Wrapper successfulUUID;

    public ConnectThread2(BluetoothDevice device, String uuid_string, UUID_Wrapper globalUUIDWrapper, BluetoothAdapter globalAdapter, BluetoothSocketWrapper globalSocketWrapper, BooleanWrapper globalBooleanWrapper) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;
        this.wrapper = globalBooleanWrapper;
        this.innerAdapter = globalAdapter;
        this.innerSocketWrapper = globalSocketWrapper;
        this.successfulUUID = globalUUIDWrapper;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code

            //Tried connecting insecure and secure KQS_TO_DO
            UUID my_uuid = UUID.fromString(uuid_string);

            tmp = device.createRfcommSocketToServiceRecord(my_uuid);
            Log.v("ConnectThread2", "createRfcommSocket successful in ConnectedThread constrcutor");
            //uuid_that_connected = my_uuid;  PASS IN uuid_that_connected

            globalUUIDWrapper.setUuid(my_uuid);

            globalSocketWrapper.setSocket(tmp);

        } catch (IOException e) {
            Log.v("ConnectThread2","createRfcommSocket failed in ConnectedThread constrcutor");
        }
        //mmSocket = tmp;
        // globalSocket = tmp;  //PASS IN mmSocket to function

    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        //mBluetoothAdapter.cancelDiscovery();
        this.innerAdapter.cancelDiscovery();

        if(this.innerSocketWrapper == null)
        {
            return;
        }

        Log.v("ConnectThread2", "mmSocket.isConnected? " + this.innerSocketWrapper.getSocket().isConnected());
        // Log.v("ConnectThread", "mmSocket.isConnected? " + mmSocket.isConnected());

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            this.innerSocketWrapper.getSocket().connect();
            Log.v("ConnectThread2", "mmSocket.isConnected? " + this.innerSocketWrapper.getSocket().isConnected());
            Log.v("ConnectThread2", "mmSocket.connect() successful run() method inside thread");

            //don't look for any more
           // foundOneSuccessfulConnection = true;

            this.wrapper.setVal(true);

            Log.v("ConnectThread2", "mmSocket is now connected to " + this.innerSocketWrapper.getSocket().getRemoteDevice().getName());
            Log.v("ConnectThread2", "uuid for mmsocket connection is " + this.successfulUUID.getUuid().toString());

            //Now begin manage connection thread

        } catch (IOException connectException) {

            Log.v("ConnectThread2", "mmSocket.connect() failed in run() of thread");
            Log.v("ConnectThread2", "connectException.toString() = " + connectException.toString());
            // Unable to connect; close the socket and get out
            try {
                this.innerSocketWrapper.getSocket().close();
                // mmSocket.close();
            } catch (IOException closeException) {
                Log.v("ConnectThread2", "closeException.toString() = " + closeException.toString());
            }

            //AGGRESSIVE APPROACH KQS_TO_DO
            //makeConnection();
            return;
        }

    }

    //Will cancel an in-progress connection, and close the socket
    public void cancel() {
        try {
            this.innerSocketWrapper.getSocket().close();
            // mmSocket.close();
        } catch (IOException e) { }
    }
}


/*  Original working Iteration #1
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

*/
