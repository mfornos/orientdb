package models;

import javax.persistence.Id;

import play.modules.orientdb.Model;

public class Address extends Model {
    @SuppressWarnings("unused")
    @Id
    private Object id;

    private String type;
    private String street;
    private City city;

    public Address() {
    }

    public Address(String iType, City iCity, String iStreet) {
        type = iType;
        city = iCity;
        street = iStreet;
    }

    public City getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public String getType() {
        return type;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setType(String type) {
        this.type = type;
    }
}
