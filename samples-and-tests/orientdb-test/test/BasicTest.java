import org.junit.*;

import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.object.ODatabaseObject;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import java.util.*;

import play.Logger;
import play.modules.orientdb.ODB;
import play.modules.orientdb.Transactional;
import play.test.*;
import models.*;

public class BasicTest extends UnitTest {
    protected static final int TOT_RECORDS = 10;
    private City redmond = new City(new Country("Washington"), "Redmond");

    @Test
    public void basic() {
        ODatabaseObjectTx db = ODB.openObjectDB();
        Item item = new Item();
        item.description = "Description";
        item.name = "Item578";
        db.save(item);
        assertEquals(1, db.countClass(Item.class));
        for (Item it : db.browseClass(Item.class)) {
            assertNotNull(db.getIdentity(it).toString());
            assertEquals("Item578", it.name);
            db.delete(it);
        }
    }

    @Test
    public void crudInheritance() {
        ODatabaseObjectTx database = ODB.openObjectDB();

        long startRecordNumber = database.countClusterElements("Company");

        Company company;

        for (long i = startRecordNumber; i < startRecordNumber + TOT_RECORDS; ++i) {
            company = new Company((int) i, "Microsoft" + i);
            company.setEmployees((int) (100000 + i));
            company.getAddresses().add(new Address("Headquarter", redmond, "WA 98073-9717"));
            database.save(company);
        }
        assertEquals(database.countClusterElements("Company") - startRecordNumber, TOT_RECORDS);
        final List<Account> result = database.query(new OSQLSynchQuery<Account>(
                "select from Company where name.length() > 0"));

        assertTrue(result.size() > 0);
        assertEquals(result.size(), TOT_RECORDS);

        int companyRecords = 0;
        Account account;
        for (int i = 0; i < result.size(); ++i) {
            account = result.get(i);

            if (account instanceof Company)
                companyRecords++;

            assertNotSame(account.getName().length(), 0);
        }

        assertEquals(companyRecords, TOT_RECORDS);

        final List<Company> result2 = database.query(new OSQLSynchQuery<ODocument>(
                "select * from Company where name.length() > 0"));

        assertTrue(result2.size() == TOT_RECORDS);

        Company account2;
        for (int i = 0; i < result.size(); ++i) {
            account2 = result2.get(i);
            assertNotSame(account2.getName().length(), 0);
        }

        startRecordNumber = database.countClusterElements("Company");

        // DELETE ALL THE RECORD IN THE CLUSTER
        for (Object obj : database.browseCluster("Company")) {
            database.delete(obj);
            break;
        }

        assertEquals(database.countClusterElements("Company"), startRecordNumber - 1);
    }

    @Test
    public void graphDB() {
        OGraphDatabase database = ODB.openGraphDB();
        try {

            OClass vehicleClass = database.createVertexType("GraphVehicle");
            database.createVertexType("GraphCar", vehicleClass);
            database.createVertexType("GraphMotocycle", "GraphVehicle");

            ODocument carNode = (ODocument) database.createVertex("GraphCar").field("brand", "Hyundai")
                    .field("model", "Coupe").field("year", 2003).save();
            ODocument motoNode = (ODocument) database.createVertex("GraphMotocycle").field("brand", "Yamaha")
                    .field("model", "X-City 250").field("year", 2009).save();

            database.createEdge(carNode, motoNode).save();

            List<ODocument> result = database.query(new OSQLSynchQuery<ODocument>("select from GraphVehicle"));
            Assert.assertEquals(result.size(), 2);
            for (ODocument v : result) {
                Assert.assertTrue(v.getSchemaClass().isSubClassOf(vehicleClass));
            }

        } finally {
            database.close();
        }
    }

}
