package models;

import javax.persistence.Id;
import javax.persistence.Version;

import play.modules.orientdb.Model;

public class City extends Model {
    @Id
    private String id;

    @Version
    private Long version;

    private String name;
    private Country country;

    public City() {
    }

    public City(Country iCountry, String iName) {
        country = iCountry;
        name = iName;
    }

    public Country getCountry() {
        return country;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getVersion() {
        return version;
    }

    public Object setCountry(Country iCountry) {
        return country = iCountry;
    }

    public void setName(String name) {
        this.name = name;
    }
}
