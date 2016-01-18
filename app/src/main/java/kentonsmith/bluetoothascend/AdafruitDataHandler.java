package kentonsmith.bluetoothascend;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

/**
 * Created by admin on 1/17/2016.
 */
public class AdafruitDataHandler extends Handler {

        Activity activity;

        AdafruitDataHandler(Activity mainActivity)
        {
            this.activity = mainActivity;
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();

            Long current_time = b.getLong("Milliseconds");
            TextView milliSeconds = (TextView) this.activity.findViewById(R.id.millisecondsText);
            milliSeconds.setText(current_time.toString());

            Double euler_x = b.getDouble("Euler_X");
            TextView eulerX = (TextView) this.activity.findViewById(R.id.eulerXText);
            eulerX.setText(euler_x.toString());

            Double euler_y = b.getDouble("Euler_Y");
            TextView eulerY = (TextView) this.activity.findViewById(R.id.eulerYText);
            eulerY.setText(euler_y.toString());

            Double euler_z = b.getDouble("Euler_Z");
            TextView eulerZ = (TextView) this.activity.findViewById(R.id.eulerZText);
            eulerZ.setText(euler_z.toString());

            Double gyro_x = b.getDouble("Gyro_X");
            TextView gyroX = (TextView) this.activity.findViewById(R.id.gyroXText);
            gyroX.setText(gyro_x.toString());

            Double gyro_y = b.getDouble("Gyro_Y");
            TextView gyroY = (TextView) this.activity.findViewById(R.id.gyroYText);
            gyroY.setText(gyro_y.toString());

            Double gyro_z = b.getDouble("Gyro_Z");
            TextView gyroZ = (TextView) this.activity.findViewById(R.id.gyroZText);
            gyroZ.setText(gyro_z.toString());

            Double lin_acc_x = b.getDouble("Lin_Acc_X");
            TextView linAccX = (TextView) this.activity.findViewById(R.id.linAccXText);
            linAccX.setText(lin_acc_x.toString());

            Double lin_acc_y = b.getDouble("Lin_Acc_Y");
            TextView linAccY = (TextView) this.activity.findViewById(R.id.linAccYText);
            linAccY.setText(lin_acc_y.toString());

            Double lin_acc_z = b.getDouble("Lin_Acc_Z");
            TextView linAccZ = (TextView) this.activity.findViewById(R.id.linAccZText);
            linAccZ.setText(lin_acc_z.toString());

            //Log.v("mHandler", "In handleMessage function right now " + current_time);

        }
}


  /*  USED TO BE IN MainActivity
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Bundle b = msg.getData();

                Long current_time = b.getLong("Milliseconds");
                TextView milliSeconds = (TextView) findViewById(R.id.millisecondsText);
                milliSeconds.setText(current_time.toString());

                Double euler_x = b.getDouble("Euler_X");
                TextView eulerX = (TextView) findViewById(R.id.eulerXText);
                eulerX.setText(euler_x.toString());

                Double euler_y = b.getDouble("Euler_Y");
                TextView eulerY = (TextView) findViewById(R.id.eulerYText);
                eulerY.setText(euler_y.toString());

                Double euler_z = b.getDouble("Euler_Z");
                TextView eulerZ = (TextView) findViewById(R.id.eulerZText);
                eulerZ.setText(euler_z.toString());

                Double gyro_x = b.getDouble("Gyro_X");
                TextView gyroX = (TextView) findViewById(R.id.gyroXText);
                gyroX.setText(gyro_x.toString());

                Double gyro_y = b.getDouble("Gyro_Y");
                TextView gyroY = (TextView) findViewById(R.id.gyroYText);
                gyroY.setText(gyro_y.toString());

                Double gyro_z = b.getDouble("Gyro_Z");
                TextView gyroZ = (TextView) findViewById(R.id.gyroZText);
                gyroZ.setText(gyro_z.toString());

                Double lin_acc_x = b.getDouble("Lin_Acc_X");
                TextView linAccX = (TextView) findViewById(R.id.linAccXText);
                linAccX.setText(lin_acc_x.toString());

                Double lin_acc_y = b.getDouble("Lin_Acc_Y");
                TextView linAccY = (TextView) findViewById(R.id.linAccYText);
                linAccY.setText(lin_acc_y.toString());

                Double lin_acc_z = b.getDouble("Lin_Acc_Z");
                TextView linAccZ = (TextView) findViewById(R.id.linAccZText);
                linAccZ.setText(lin_acc_z.toString());

                //Log.v("mHandler", "In handleMessage function right now " + current_time);


            }
        };
*/

