package kentonsmith.bluetoothascend;

/**
 * Created by admin on 1/17/2016.
 */

public class BooleanWrapper
{
    private boolean val;

    BooleanWrapper(boolean val)
    {
        this.val  = val;
    }

    public boolean getVal()
    {
        return this.val;
    }

    public void setVal(boolean val)
    {
        this.val = val;
    }
}