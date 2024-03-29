public class Customer {
    private String name;
    private String email;
    public  Customer(String name, String email){
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Printable
    public void printName(){
        System.out.println("Customer email: "+ name);
    }
    @Printable
    public void printEmail(){
        System.out.println("Customer email: "+ email);
    }
}
