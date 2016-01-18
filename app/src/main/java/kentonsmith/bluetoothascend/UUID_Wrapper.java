package kentonsmith.bluetoothascend;

import java.util.UUID;

/**
 * Created by admin on 1/17/2016.
 */
public class UUID_Wrapper {

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    private UUID uuid;

    UUID_Wrapper(UUID uuid)
    {
        this.uuid = uuid;
    }
}
