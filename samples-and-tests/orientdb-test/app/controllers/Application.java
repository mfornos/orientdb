package controllers;

import java.util.List;

import javax.inject.Inject;

import models.Account;
import models.Item;
import play.Logger;
import play.exceptions.UnexpectedException;
import play.modules.orientdb.ODB.DBTYPE;
import play.modules.orientdb.Transactional;
import play.mvc.Controller;

import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.object.iterator.OObjectIteratorClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Application extends Controller {
    @Inject
    static OObjectDatabaseTx db;

    @Inject
    static ODatabaseDocumentTx docdb;

    public static void index() {
        List<Item> result = Item.find("select * from Item where name like ?", "my%");
        OObjectIteratorClass<Item> items = Item.all();
        OObjectIteratorClass<Account> accounts = db.browseClass(Account.class);
        render(result, items, accounts);
    }

    public static void detail(ORecordId id) {
        Item item = Item.findById(id);
        notFoundIfNull(item);
        render(item);
    }

    @Transactional
    public static void save(String name, String description) {
        Item item = new Item();
        item.name = name;
        item.description = description;
        item.save();
        index();
    }

    @Transactional(db = DBTYPE.DOCUMENT)
    public static void good() {
        ODocument doc = new ODocument("Account");
        doc.field("name", "good !!");
        doc.save();
        index();
    }

    @Transactional(db = DBTYPE.DOCUMENT)
    public static void bad() {
        ODocument doc = new ODocument("Account");
        doc.field("name", "bad :(");
        doc.save();
        throw new RuntimeException("Hello from bad transaction, will be rolled back!");
    }

}