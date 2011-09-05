package models;

import javax.persistence.Id;
import javax.persistence.Version;

public class City {
        @Id
        private Long            id;

        @Version
        private Long            version;

        private String  name;
        private Country country;

        public City() {
        }

        public City(Country iCountry, String iName) {
                country = iCountry;
                name = iName;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public Country getCountry() {
                return country;
        }

        public Object setCountry(Country iCountry) {
                return country = iCountry;
        }

        public Long getId() {
                return id;
        }

        public Long getVersion() {
                return version;
        }
}
