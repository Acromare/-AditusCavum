package com.heng.aditus.config;

import com.heng.aditus.AditusOperations;
import com.heng.aditus.annotation.AditusClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Modifier;
import java.util.List;

final class AditusClientScanner implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, ResourceLoaderAware {
    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) { this.environment = environment; }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) { this.resourceLoader = resourceLoader; }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String configured = environment.getProperty("aditus-cavum.client.base-packages");
        List<String> packages = StringUtils.hasText(configured)
                ? List.of(StringUtils.commaDelimitedListToStringArray(configured))
                : registry instanceof ConfigurableListableBeanFactory factory && AutoConfigurationPackages.has(factory)
                    ? AutoConfigurationPackages.get(factory) : List.of();
        var scanner = new ClassPathScanningCandidateComponentProvider(false, environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                return definition.getMetadata().isIndependent(); // include interfaces for validation
            }
        };
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(AditusClient.class));
        for (String basePackage : packages) {
            if (!StringUtils.hasText(basePackage)) continue;
            for (var candidate : scanner.findCandidateComponents(basePackage.trim())) {
                Class<?> type = ClassUtils.resolveClassName(candidate.getBeanClassName(), resourceLoader.getClassLoader());
                validate(type);
                String beanName = type.getName();
                if (registry.containsBeanDefinition(beanName)) {
                    var existing = registry.getBeanDefinition(beanName);
                    if (AditusClientFactoryBean.class.getName().equals(existing.getBeanClassName())
                            && type.equals(existing.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE))) continue;
                    throw new IllegalStateException("Aditus client bean name already exists: " + beanName);
                }
                var definition = new RootBeanDefinition(AditusClientFactoryBean.class);
                definition.getConstructorArgumentValues().addIndexedArgumentValue(0, type);
                definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, type);
                registry.registerBeanDefinition(beanName, definition);
            }
        }
    }

    private static void validate(Class<?> type) {
        if (!type.isInterface() || !Modifier.isPublic(type.getModifiers()) || type.isSealed()
                || !AditusOperations.class.isAssignableFrom(type)) {
            throw new IllegalStateException("@AditusClient requires a public, non-sealed interface extending AditusOperations: " + type.getName());
        }
        for (var method : type.getMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) continue;
            try {
                var operation = AditusOperations.class.getMethod(method.getName(), method.getParameterTypes());
                if (!operation.getGenericReturnType().equals(method.getGenericReturnType())) throw new NoSuchMethodException();
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Unsupported Aditus client method: " + method
                        + ". Use AditusOperations signatures or implement a Java default method.");
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) { }
}
