package play.modules.orientdb;

import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

// XXX hacky
public class OSQLSynchPaginatedQuery<T> extends OSQLSynchQuery<T> {
    private static final long serialVersionUID = -2752384175069620762L;

    private int currentRecord;

    private int offset;

    public OSQLSynchPaginatedQuery(String iText, int offset, int iLimit) {
        super(iText, offset + iLimit);
        this.offset = offset;
    }

    @Override
    public boolean result(Object iRecord) {
        if (offset <= currentRecord) {
            super.result(iRecord);
        } else {
            currentRecord++;
        }
        return true;
    }

}
