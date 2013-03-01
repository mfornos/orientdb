package hooks;

import play.Logger;

import com.orientechnologies.orient.core.hook.ORecordHookAbstract;
import com.orientechnologies.orient.core.record.ORecord;

public class LoggingRecordHook extends ORecordHookAbstract {

    @Override
    public RESULT onTrigger(TYPE iType, ORecord<?> iRecord) {
        // Logger.info("[LOG HOOK] %s %s", iType, iRecord.toString());
        return super.onTrigger(iType, iRecord);
    }

}
