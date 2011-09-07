package models;

import play.modules.orientdb.Model;

import com.orientechnologies.orient.core.annotation.OId;
import com.orientechnologies.orient.core.annotation.OVersion;

public class Country extends Model {
    @OId
    private Object id;

    @OVersion
    private Object version;

    private String name;

    public Country() {
    }

    public Country(String iName) {
        name = iName;
    }

    public Object getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Object getVersion() {
        return version;
    }

    public void setName(String name) {
        this.name = name;
    }
}
