package com.heng.aditus.config;

import com.heng.aditus.AditusAssistant;
import com.heng.aditus.AditusOperations;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/** Creates the client without resolving the assistant during Spring bean discovery. */
final class AditusClientFactoryBean implements FactoryBean<Object>, BeanFactoryAware {
    private final Class<?> clientType;
    private final Object client;
    private BeanFactory beanFactory;

    AditusClientFactoryBean(Class<?> clientType) {
        this.clientType = clientType;
        client = Proxy.newProxyInstance(clientType.getClassLoader(), new Class<?>[]{clientType}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "AditusClient[" + clientType.getName() + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                };
            }
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            try {
                return AditusOperations.class.getMethod(method.getName(), method.getParameterTypes())
                        .invoke(beanFactory.getBean(AditusAssistant.class), args);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        });
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) { this.beanFactory = beanFactory; }

    @Override
    public Object getObject() { return client; }

    @Override
    public Class<?> getObjectType() { return clientType; }

    @Override
    public boolean isSingleton() { return true; }
}
