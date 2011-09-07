package models;


public class Company extends Account {
    private int employees;

    public Company() {
    }

    public Company(int iId, String iName) {
        super(iId, iName, null);
    }

    public int getEmployees() {
        return employees;
    }

    public void setEmployees(int employees) {
        this.employees = employees;
    }
}
