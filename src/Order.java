public class Order {
    private Customer customer;
    private Address address;

    @Autowired
    public Order(Customer customer, Address address) {
        this.customer = customer;
        this.address = address;
    }

    public Order() {
    }

    public Customer getCustomer() {
        return customer;
    }

    public Address getAddress() {
        return address;
    }
}
