/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cleverbus.core.common.route;

import java.beans.Introspector;
import java.util.Set;

import org.cleverbus.api.route.CamelConfiguration;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;


/**
 * {@link BeanNameGenerator} implementation for bean classes annotated with the
 * {@link CamelConfiguration @CamelConfiguration} annotation.
 *
 * <p/>
 * Derives a default bean name from the given bean definition.
 * The default implementation simply builds a decapitalized version of the short class name
 * plus add constant for input/output module and finally adds suffix "{@value #BEAN_SUFFIX}":
 * e.g. <pre class="code">"com.cleverlance.tutan.modules.in.account.CreateCustomerAccountRoute" -> "createCustomerAccountRouteInBean".</pre>
 *
 * <p/>
 * If final bean name (defined or generated) is not unique then exception is thrown.
 * See {@code context:component-scan} how to use it.
 *
 * @author <a href="mailto:petr.juza@cleverlance.com">Petr Juza</a>
 * @since 1.1
 */
public class RouteBeanNameGenerator extends AnnotationBeanNameGenerator {

    private static final String CAMEL_CONF_CLASSNAME = "org.cleverbus.api.route.CamelConfiguration";

    public static final String BEAN_SUFFIX = "Bean";

    public static final String MODULES_IN = "In";

    public static final String MODULES_PACKAGE_IN = "modules.in";

    public static final String MODULES_OUT = "Out";

    public static final String MODULES_PACKAGE_OUT = "modules.out";

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        String beanName = null;

        if (definition instanceof AnnotatedBeanDefinition) {
            // bean name is from annotation
            beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);

            if (StringUtils.isEmpty(beanName) && isRouteAnnotation((AnnotatedBeanDefinition) definition)) {
                // generate bean name for routes
                beanName = buildRouteBeanName(definition);
            }
        }

        if (StringUtils.isEmpty(beanName)) {
            // generate default name
            beanName = buildDefaultBeanName(definition);
        }

        // check uniqueness
        if (registry.containsBeanDefinition(beanName)) {
            throw new IllegalStateException("Bean name '" + beanName + "' already exists, please change "
                    + "class name or explicitly define bean name");
        }

        return beanName;
    }

    protected boolean isRouteAnnotation(AnnotatedBeanDefinition annotatedDef) {
        AnnotationMetadata amd = annotatedDef.getMetadata();
        Set<String> types = amd.getAnnotationTypes();
        for (String type : types) {
            if (type.equals(CAMEL_CONF_CLASSNAME)) {
                return true;
            }
        }

        return false;
    }

    protected String buildRouteBeanName(BeanDefinition definition) {
        String shortClassName = ClassUtils.getShortName(definition.getBeanClassName());
        String beanName = Introspector.decapitalize(shortClassName);

        if (StringUtils.contains(definition.getBeanClassName(), MODULES_PACKAGE_IN)) {
            beanName += MODULES_IN;
        } else if (StringUtils.contains(definition.getBeanClassName(), MODULES_PACKAGE_OUT)) {
            beanName += MODULES_OUT;
        }

        beanName += BEAN_SUFFIX;

        return beanName;
    }
}
