import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
}
