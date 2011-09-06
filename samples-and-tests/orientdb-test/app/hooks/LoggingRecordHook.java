package hooks;

import play.Logger;

import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.record.ORecord;

public class LoggingRecordHook implements ORecordHook {

    @Override
    public boolean onTrigger(TYPE type, ORecord<?> record) {
        // Logger.info("[LOG HOOK] %s %s", type, record.toString());
        return false;
    }

}
