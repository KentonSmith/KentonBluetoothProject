package kentonsmith.bluetoothascend;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class AWS_Functionality_Test extends Activity {

    private KentonsAWSWrapper wrapper_dynamodb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aws__functionality__test);

        wrapper_dynamodb = new KentonsAWSWrapper(getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_aws__functionality__test, menu);
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

    public void makeRowOnClick(View v)
    {
        wrapper_dynamodb.makeRowTest();
        /*
        try
        {
            wrapper_dynamodb.makeRowTest();
        }
        catch(Exception e)
        {
            Log.v("onActivityResult", e.toString());
        }
        */
    }
}
