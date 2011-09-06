package listeners;

import play.Logger;

import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseListener;

public class LoggingDatabaseListener implements ODatabaseListener {

    @Override
    public void onAfterTxCommit(ODatabase arg0) {
        // info("afterCommit");
    }

    @Override
    public void onAfterTxRollback(ODatabase arg0) {
        // info("afterRollback");
    }

    @Override
    public void onBeforeTxBegin(ODatabase arg0) {
        // info("beforeBegin");
    }

    @Override
    public void onBeforeTxCommit(ODatabase arg0) {
        // info("beforeCommit");
    }

    @Override
    public void onBeforeTxRollback(ODatabase arg0) {
        // info("beforeRollback");
    }

    @Override
    public void onClose(ODatabase arg0) {
        // info("close");
    }

    @Override
    public void onCreate(ODatabase arg0) {
        // info("create");
    }

    @Override
    public void onDelete(ODatabase arg0) {
        // info("delete");
    }

    @Override
    public void onOpen(ODatabase arg0) {
        // info("open");
    }

    private void info(String msg) {
        Logger.info("[LOG LISTENER] %s", msg);
    }

}
