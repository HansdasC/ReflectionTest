[TOC]

# Reference

https://www.bilibili.com/video/BV1K4421w7zP/?share_source=copy_web&vd_source=e40b707ba9b46ace5a15c44fb5fa3388

# 0. 需求

通过一个订单Order对象获取用户User信息和地址Address信息

# 1. 常规方法 

```java
Address address = new Address("99 Shangda Road", "114514");
Customer customer = new Customer("Hansdas", "Hansdas@xx.com");
Order order = new Order(customer, address);
order.getCustomer().printName();
order.getAddress().printStreet();
```

【缺陷】在面对动态场景时存在限制，一旦代码完成，就无法动态创建对象或调用方法

#  2. 反射方法实现

## 2.1 定义Config类与Container类

- 先定义一个Config类：用于定义Customer服务与Address服务，通过对应方法进行实例化并返回需要的对象

  ```java
  public class Config {
      public Customer customer(){
          // 定义Customer服务与Address服务，通过对应方法进行实例化并返回需要的对象
          return new Customer("Hansdas", "hansdas@xx.com");
      }
  
      public Address address(){
          return new Address("99 Shandda Road", "114514");
      }
  
      public Message message(){
          return new Message("Hi there!");
      }
  }
  ```

- 再定义一个Container类：注册和获得实例

  ```java
  public class Container {
      // 创建一个Map类型来存放Config里的所有方法
      // K: 方法返回的Class，V：对应的方法
      // 注意：这里存储的时=是方法本身，而不是方法执行后获得的实例（具体实例在执行这些方法后获取，这样可以节省资源并提高性能）
      private Map<Class<?>, Method> methods;
      private Object config; // 用于存放config实例
  
      // 初始化注册加载所有的方法和实例化Config对象
      public void init() throws ClassNotFoundException {
          Class<?> clazz = Class.forName("Config");
          Method[] methods = clazz.getDeclaredMethods();
          for (Method method: methods){
              System.out.println(method.getName());
          }
      }
  }
  ```

## 2.2 使用注解过滤得到目标方法

比如上述Config中，我们只需要`customer()`与`address()`，不需要`message()`，可以在`customer()`与`address()`加自定义注解，通过`Method.getDeclaredAnnotation()`过滤。

- 自定义一个Bean

  ```java
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Bean {
      
  }
  ```

- 在Config中为需要的方法加上注解

  ```java
  public class Config {
      @Bean
      public Customer customer(){
          // 定义Customer服务与Address服务，通过对应方法进行实例化并返回需要的对象
          return new Customer("Hansdas", "hansdas@xx.com");
      }
      @Bean
      public Address address(){
          return new Address("99 Shandda Road", "114514");
      }
  
      public Message message(){
          return new Message("Hi there!");
      }
  }
  ```

- 修改Container ` init()` 中注册服务的过程

  ```java
  public void init() throws ClassNotFoundException {
      Class<?> clazz = Class.forName("Config");
      Method[] methods = clazz.getDeclaredMethods();
      for (Method method: methods){
          if (method.getDeclaredAnnotation(Bean.class) != null){
              // 如果有Bean注解的话就来处理这个方法
              System.out.println(method.getName());
          }
      }
  }
  ```

##  2.3 使用Map在Container中进行管理

###  2.3.1 通过Map管理目标实例方法

前面Container中有个Map类型的methods属性

```
// 创建一个Map类型来存放Config里的所有方法
// K: 方法返回的Class，V：对应的方法
// 注意：这里存储的时=是方法本身，而不是方法执行后获得的实例（具体实例在执行这些方法后获取，这样可以节省资源并提高性能）
private Map<Class<?>, Method> methods;
```



- 在Container的`init()`中，将需要的Method加入methods的map中，并实例化config用于之后调用方法服务与对象

  ```java
  public void init() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
          this.methods = new HashMap<>();
          Class<?> clazz = Class.forName("Config");
          Method[] methods = clazz.getDeclaredMethods();
          for (Method method: methods){
              if (method.getDeclaredAnnotation(Bean.class) != null){
                  // 如果有Bean注解的话就来处理这个方法
                  this.methods.put(method.getReturnType(), method);
              }
          }
          this.config = clazz.getConstructor().newInstance();
   }
  ```

- 在Container中写一个`getServiceInstanceByClass`方法，通过类的class对象获取相应的服务实例

  ```java
  public Object getServiceInstanceByClass(Class<?> clazz) throws InvocationTargetException, IllegalAccessException {
  	   // 使用Class对象作为Key，在事先初始化的map中查找对应的方法
  	   // 找到了相应的方法就执行该方法，返回对应的实例化服务对象
  	  if (this.methods.containsKey(clazz)){
  	      Method method = this.methods.get(clazz);
  	      Object obj = method.invoke(this.config);
  	      return obj;
  	  }
  	  return null;
  }
  ```

- main中测试一下能否根据class拿到相应对象

  ```java
  public static void main(String[] args) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException {
      Container container = new Container();
      container.init();
      Class<?> clazz = Class.forName("Customer");
      Object obj = container.getServiceInstanceByClass(clazz);
      System.out.println(obj);
  
  }
  ```

  输出如下，成功拿到

  ```
  Customer@21b8d17c
  ```

这里成功通过该方法获得了一个对象实例，但此时每次调用这个方法获取一个实例时，都会创建一个新对象，有的服务是全局的，希望只创建一次，之后就复用这个实例，因此可以增加一个Map来维护这个对实例的唯一性

###  2.3.2 通过Map实现目标的唯一性

创建一个Map来存储已经创建的实例，在`init()`中初始化，并修改 `getServiceInstanceByClass` 返回实例的流程，修改后的Container如下：

```java
public class Container {
    // 创建一个Map类型来存放Config里的所有方法
    // K: 方法返回的Class，V：对应的方法
    // 注意：这里存储的时=是方法本身，而不是方法执行后获得的实例（具体实例在执行这些方法后获取，这样可以节省资源并提高性能）
    private Map<Class<?>, Method> methods;
    private Object config; // 用于存放config实例
    private Map<Class<?>, Object> services; // 存放已经创建的对象实例

    // 初始化注册加载所有的方法和实例化Config对象
    public void init() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.methods = new HashMap<>();
        this.services = new HashMap<>();
        Class<?> clazz = Class.forName("Config");
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method: methods){
            if (method.getDeclaredAnnotation(Bean.class) != null){
                // 如果有Bean注解的话就来处理这个方法
                this.methods.put(method.getReturnType(), method);
            }
        }
        this.config = clazz.getConstructor().newInstance();
    }

    public Object getServiceInstanceByClass(Class<?> clazz) throws InvocationTargetException, IllegalAccessException {
        if (this.services.containsKey(clazz)){
            // 如果已经创建过这个class的实例，直接返回
            return this.services.get(clazz);
        }else{
            // 如果没有创建过这个class的实例：
            // 使用Class对象作为Key，在事先初始化的map中查找对应的方法，找到了相应的方法就执行该方法，返回对应的实例化服务对象
            if (this.methods.containsKey(clazz)){
                Method method = this.methods.get(clazz);
                Object obj = method.invoke(this.config);
                this.services.put(clazz, obj);
                return obj;
            }
            return null;
        }

    }
}
```

## 2.4 实现服务自动注入

在Container中定义一个`createInstance`方法，用于通过Class对象创建普通实例，并且将服务自动注入到对象中

### 2.4.1 获取目标对象的构造器

- 增加`createInstance`方法，通过`Class.getDeclaredConstructors()` 获取目标的构造器

  ```java
  public Object createInstance(Class<?> clazz) throws NoSuchMethodException {
      Constructor<?>[] constructors =  clazz.getDeclaredConstructors();
      for (Constructor<?> constructor : constructors){
          System.out.println(constructor);
      }
      return null;
  }
  ```

- 在main中测试一下

  ```java
  public class Main {
      public static void main(String[] args) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException {
          Container container = new Container();
          container.init();
          Class<?> clazz = Class.forName("Order");
          container.createInstance(clazz);
      }
  }
  ```

  输出如下，成功拿到Order的两个构造器

  ```
  public Order(Customer, Address)
  public Order()
  ```

### 2.4.2 通过注解获得指定构造器

上面已经成功拿到了目标对象Order的连个给构造器，与前面2.2相同，具体获取哪一个可以通过注解选择

- 定义一个`Autowired`注解

  ```java
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.CONSTRUCTOR)
  public @interface Autowired {
  }
  ```

- 在Order中需要的构造函数上加注解

  ```java
  public class Order {
      private Customer customer;
      private Address address;
  
      @Autowired
      public Order(Customer customer, Address address) {
          this.customer = customer;
          this.address = address;
      }
  	...
  }
  ```

- 修改`createInstance`，根据注解进行过滤

  ```java
  public Object createInstance(Class<?> clazz) throws NoSuchMethodException {
      Constructor<?>[] constructors =  clazz.getDeclaredConstructors();
      for (Constructor<?> constructor : constructors){
          if (constructor.getDeclaredAnnotation(Autowired.class) != null){
              System.out.println(constructor);
          }
      }
      return null;
  }
  ```

- 在main里测试一下，只有加了`Autowired`注解的有参构造了

  ```
  public Order(Customer, Address)
  ```

###  2.4.3 通过构造器实例化对象

- 修改`createInstance`

- ```
  public Object createInstance(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
      Constructor<?>[] constructors =  clazz.getDeclaredConstructors();
      for (Constructor<?> constructor : constructors){
          if (constructor.getDeclaredAnnotation(Autowired.class) != null){
              // 获取所有的参数类型
              Class<?>[] parameterTypes = constructor.getParameterTypes();
              // 存储参数的对象实例
              Object[] arguements = new Object[parameterTypes.length];
              for (int i = 0; i < parameterTypes.length; i++) {
                  arguements[i] = getServiceInstanceByClass(parameterTypes[i]);
              }
              return constructor.newInstance(arguements);
          }
      }
      // 无Autowired注解直接通过无参构造返回实例
      return clazz.getDeclaredConstructor().newInstance();
  }
  ```

- 注入测试

  ```java
  public static void main(String[] args) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException {
      Container container = new Container();
      container.init();
      Class<?> clazz = Class.forName("Order");
      Object obj = container.createInstance(clazz);
      Field field = clazz.getDeclaredField("customer");
      field.setAccessible(true);
      Object fieldValue = field.get(obj);
      System.out.println(fieldValue);
  }
  ```

  输出如下，获取到Customer服务对象，注入成功

  ```
  Customer@41cf53f9
  ```

###  2.4.4 调用服务对象的方法

- 在main中通过`Class.getDeclaredMethods`获得服务对象的所有方法，print一下

  ```java
  Method[] methods = fieldValue.getClass().getDeclaredMethods();
  for (Method method: methods){
      System.out.println(method.getName());
  }
  ```

  输出如下，成功拿到Customer的方法

  ```
  getName
  printEmail
  printName
  getEmail
  ```

  这里获得了Customer的所有方法，如果想要获取带print的方法，也只需要加上注解即可

- 定义Printable注解

  ```java
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Printable {
  }
  ```

- 在print相关方法上增加注解

  ```java
  public class Customer {
  		...
  
      @Printable
      public void printName(){
          System.out.println("Customer email: "+ name);
      }
      @Printable
      public void printEmail(){
          System.out.println("Customer email: "+ email);
      }
  }
  ```

- 测试增加约束，过滤掉不含Printable注解的方法

  ```java
  Method[] methods = fieldValue.getClass().getDeclaredMethods();
  for (Method method: methods){
      if (method.getDeclaredAnnotation(Printable.class) != null){
          System.out.println(method.getName());
      }
  }
  ```

  输出如下，成功

  ```
  printName
  printEmail
  ```

- Main中使用`Method.invoke`执行方法

  ```
  Method[] methods = fieldValue.getClass().getDeclaredMethods();
  for (Method method: methods){
      if (method.getDeclaredAnnotation(Printable.class) != null){
          method.invoke(fieldValue);
      }
  }
  ```

  输出

  ```
  Customer email: Hansdas
  Customer email: hansdas@xx.com
  ```

- 也可以把类名与变量名放到变量里，方便配置，最终的Main如下

  ```java
  public static void main(String[] args) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException {
          Container container = new Container();
          container.init();
          String className = "Order";
          String fieldName = "customer";
          Class<?> clazz = Class.forName(className);
          Object obj = container.createInstance(clazz);
          Field field = clazz.getDeclaredField(fieldName);
          field.setAccessible(true);
          Object fieldValue = field.get(obj);
          Method[] methods = fieldValue.getClass().getDeclaredMethods();
          for (Method method: methods){
              if (method.getDeclaredAnnotation(Printable.class) != null){
                  method.invoke(fieldValue);
              }
          }
   }
  ```

##  2.5 其他测试

如果要换一下class跟field，记得加上相应注解：

1. `User`中将相应构造函数加`@Autowired`
2. `Message`中print方法加`@Printable`
3. `Config` 中将message()加`@Bean`

```java
public static void main(String[] args) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException {
    Container container = new Container();
    container.init();
    String className = "User";
    String fieldName = "message";
    Class<?> clazz = Class.forName(className);
    Object obj = container.createInstance(clazz);
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    Object fieldValue = field.get(obj);
    Method[] methods = fieldValue.getClass().getDeclaredMethods();
    for (Method method: methods){
        if (method.getDeclaredAnnotation(Printable.class) != null){
            method.invoke(fieldValue);
        }
    }
}
```

#  反射的优缺点

## 反射的优点

可以看出，此时对象的实例化和方法的调用不再依赖于硬编码的类名和方法名，而是基于字符串和注解Annotation提供的配置信息

使得代码非常的灵活且通用，适合编写需要高度解耦的框架与程序

##  反射的缺点

会带来性能开销、安全性问题和维护的复杂性，需要在灵活性与这些问题中平衡

