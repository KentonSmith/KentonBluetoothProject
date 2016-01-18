package kentonsmith.bluetoothascend;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;

/**
 * Created by admin on 1/17/2016.
 */
public class BluetoothSocketWrapper {

    public BluetoothSocket getSocket() {
        return socket;
    }

    public void setSocket(BluetoothSocket socket) {
        this.socket = socket;
    }

    private BluetoothSocket socket;

    BluetoothSocketWrapper(BluetoothSocket socket)
    {
        this.socket = socket;
    }



}
