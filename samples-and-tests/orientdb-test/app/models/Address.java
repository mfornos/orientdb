package models;

import javax.persistence.Id;
import javax.persistence.Version;

import play.modules.orientdb.Model;

public class Address extends Model {
    @SuppressWarnings("unused")
    @Id
    private String id;

    @Version
    private Integer version;

    private String type;
    private String street;
    private City city;

    public Address() {
    }

    public Address(String iStreet) {
      street = iStreet;
    }

    public Address(String iType, City iCity, String iStreet) {
      type = iType;
      city = iCity;
      street = iStreet;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getStreet() {
      return street;
    }

    public void setStreet(String street) {
      this.street = street;
    }

    public City getCity() {
      return city;
    }

    public void setCity(City city) {
      this.city = city;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public Integer getVersion() {
      return version;
    }

    public void setVersion(Integer version) {
      this.version = version;
    }
}
