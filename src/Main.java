import java.lang.reflect.*;
import java.util.Arrays;

public class Main {
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
}