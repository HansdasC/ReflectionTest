public class Address {
    public String street;
    public String postCode;

    public Address(String street, String postCode) {
        this.street = street;
        this.postCode = postCode;
    }

    public String getStreet() {
        return street;
    }

    public String getPostCode() {
        return postCode;
    }

    @Printable
    public void printStreet() {
        System.out.println("Address street: " + street);
    }
    @Printable
    public void printPostCode() {
        System.out.println("Address postcode: " + postCode);
    }
}
