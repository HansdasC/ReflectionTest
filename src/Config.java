public class Config {
    @Bean // 声明是一个服务
    public Customer customer(){
        // 定义Customer服务与Address服务，通过对应方法进行实例化并返回需要的对象
        return new Customer("Hansdas", "hansdas@xx.com");
    }
    @Bean
    public Address address(){
        return new Address("99 Shandda Road", "114514");
    }

    @Bean
    public Message message(){
        return new Message("Hi there!");
    }
}
