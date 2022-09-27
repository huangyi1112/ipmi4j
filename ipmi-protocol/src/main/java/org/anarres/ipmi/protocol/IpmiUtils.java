package org.anarres.ipmi.protocol;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import org.anarres.ipmi.protocol.client.session.IpmiPacketContext;

public class IpmiUtils {
    public static final SecureRandom RANDOM = new SecureRandom();

    public static <T> String getClassName() {
        return new ClassWrapper<T>() {}.getClassName();
    }

    public static abstract class ClassWrapper<E> {
        @SuppressWarnings ("unchecked")
        public Class<? super E> getTypeParameterClass()
        {
            final TypeToken<E> type = new TypeToken<E>(getClass()) {};
            return type.getRawType();
            /*
            Type type = getClass().getGenericSuperclass();
            ParameterizedType paramType = (ParameterizedType) type;
            return (Class<E>) paramType.getActualTypeArguments()[0];
            */
        }

        String getClassName() {
            // return ((Class<?>)((ParameterizedType)ClassWrapper.class.getGenericSuperclass()).getActualTypeArguments()[0]).getSimpleName();
            return getTypeParameterClass().getTypeName();
        }
    }

    public static <T> String typeName() {
        return new ClassWrapper<T>() {}.getClassName();
    }

    static abstract class StrawManParameterizedClass<T> {
        final TypeToken<T> type = new TypeToken<T>(getClass()) {};
    }

    public static void main(String[] args) throws Exception {
        final StrawManParameterizedClass<String> smpc = new StrawManParameterizedClass<String>() {};
        final String string = (String) smpc.type.getRawType().newInstance();
        System.out.format("string = \"%s\"\n",string);
        System.out.println(smpc.type.getRawType().getSimpleName());

        System.out.println(new ClassWrapper<String>() {}.getClassName());
        System.out.println(IpmiUtils.<Map>getClassName());

        IpmiPacketContext context = new IpmiPacketContext();
        context.set("test", new Boolean(true));
    }
}
