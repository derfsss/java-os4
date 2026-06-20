package java8.java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ReflectionTest {

    static final String CLS = "ReflectionTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} System.out.flush(); }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    // ---- Test fixtures ----

    interface Greeter {
        String greet(String who);
    }

    static class Animal {
        public int legs = 4;
    }

    static class Dog extends Animal implements Greeter {
        private String name;
        public static int count;

        public Dog() { this.name = "Rex"; }
        public Dog(String name) { this.name = name; }

        public String greet(String who) { return "Woof " + who; }
        public int add(int a, int b) { return a + b; }
        private String secret() { return name; }
    }

    enum Color { RED, GREEN, BLUE }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Tag {
        String value();
        int level() default 1;
    }

    @Tag(value = "tagged", level = 7)
    static class Annotated { }

    // Generic-superclass fixture: a concrete subtype of a parameterized base.
    static class StringList extends ArrayList<String> { }

    public static void main(String[] args) {
        try {
            // Class basic identity
            ck("getName", Dog.class.getName(), "java8.java.lang.reflect.ReflectionTest$Dog");
            ck("getSimpleName", Dog.class.getSimpleName(), "Dog");
            ck("getSuperclass", Dog.class.getSuperclass(), Animal.class);

            Class<?>[] ifaces = Dog.class.getInterfaces();
            ck("interfaces length", ifaces.length, 1);
            ck("interface is Greeter", ifaces[0], Greeter.class);

            // isInstance / isAssignableFrom
            Dog d = new Dog("Fido");
            ck("isInstance Dog", Animal.class.isInstance(d), true);
            ck("isInstance wrong", Greeter.class.isInstance("x"), false);
            ck("isAssignableFrom", Animal.class.isAssignableFrom(Dog.class), true);
            ck("isAssignableFrom reverse", Dog.class.isAssignableFrom(Animal.class), false);

            // getDeclaredFields includes private + static
            Field[] dfs = Dog.class.getDeclaredFields();
            boolean hasName = false, hasCount = false;
            for (Field f : dfs) {
                if (f.getName().equals("name")) hasName = true;
                if (f.getName().equals("count")) hasCount = true;
            }
            ck("declared field name", hasName, true);
            ck("declared field count", hasCount, true);

            // getMethods (public, includes inherited interface impls)
            boolean hasGreet = false, hasAdd = false;
            for (Method m : Dog.class.getMethods()) {
                if (m.getName().equals("greet")) hasGreet = true;
                if (m.getName().equals("add")) hasAdd = true;
            }
            ck("public method greet", hasGreet, true);
            ck("public method add", hasAdd, true);

            // getDeclaredConstructor().newInstance()
            Constructor<Dog> ctor = Dog.class.getDeclaredConstructor();
            Dog made = ctor.newInstance();
            ck("newInstance default ctor", made.greet("you"), "Woof you");

            // isArray + getComponentType
            int[] arr = new int[3];
            ck("isArray", arr.getClass().isArray(), true);
            ck("componentType", arr.getClass().getComponentType(), int.class);

            // isEnum / isPrimitive
            ck("isEnum", Color.class.isEnum(), true);
            ck("not enum", Dog.class.isEnum(), false);
            ck("isPrimitive int", int.class.isPrimitive(), true);
            ck("not primitive", Integer.class.isPrimitive(), false);

            // Method.invoke + getReturnType + getParameterTypes
            Method add = Dog.class.getMethod("add", int.class, int.class);
            ck("invoke add", add.invoke(d, 2, 5), 7);
            ck("add returnType", add.getReturnType(), int.class);
            Class<?>[] pts = add.getParameterTypes();
            ck("add param count", pts.length, 2);
            ck("add param0 type", pts[0], int.class);

            // Field.get/set + setAccessible on private field
            Field nameF = Dog.class.getDeclaredField("name");
            nameF.setAccessible(true);
            ck("field get private", nameF.get(d), "Fido");
            nameF.set(d, "Buddy");
            ck("field set private", nameF.get(d), "Buddy");

            // Modifier
            int addMods = add.getModifiers();
            ck("Modifier.isPublic", Modifier.isPublic(addMods), true);
            ck("Modifier.isStatic add", Modifier.isStatic(addMods), false);
            Field countF = Dog.class.getDeclaredField("count");
            ck("Modifier.isStatic count", Modifier.isStatic(countF.getModifiers()), true);
            Method secret = Dog.class.getDeclaredMethod("secret");
            ck("Modifier.isPrivate", Modifier.isPrivate(secret.getModifiers()), true);

            // java.lang.reflect.Array
            Object iarr = Array.newInstance(int.class, 4);
            Array.set(iarr, 0, 11);
            Array.set(iarr, 1, 22);
            ck("Array.getLength", Array.getLength(iarr), 4);
            ck("Array.get", Array.get(iarr, 1), 22);
            ck("Array.get default", Array.get(iarr, 3), 0);

            // Annotation via getAnnotation (RUNTIME retention)
            Tag tag = Annotated.class.getAnnotation(Tag.class);
            ck("annotation present", tag != null, true);
            ck("annotation value", tag.value(), "tagged");
            ck("annotation level", tag.level(), 7);
            ck("isAnnotationPresent", Annotated.class.isAnnotationPresent(Tag.class), true);

            // Generic superclass -> ParameterizedType
            Type gsuper = StringList.class.getGenericSuperclass();
            ck("genericSuperclass is parameterized", gsuper instanceof ParameterizedType, true);
            ParameterizedType pt = (ParameterizedType) gsuper;
            ck("parameterized raw type", pt.getRawType(), ArrayList.class);
            Type[] targs = pt.getActualTypeArguments();
            ck("parameterized arg count", targs.length, 1);
            ck("parameterized type arg", targs[0], String.class);

            // Sanity: List vs raw to keep import used meaningfully
            List<String> lst = new StringList();
            lst.add("ok");
            ck("StringList usable", lst.get(0), "ok");

        } catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
