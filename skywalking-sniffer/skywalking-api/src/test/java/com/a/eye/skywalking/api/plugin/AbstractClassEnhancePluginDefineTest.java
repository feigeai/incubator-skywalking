package com.a.eye.skywalking.api.plugin;

import com.a.eye.skywalking.api.plugin.utility.ClassFileExtraction;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.matcher.ElementMatchers;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.instrument.ClassFileTransformer;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(PowerMockRunner.class)
public class AbstractClassEnhancePluginDefineTest {
    static final String WEAVE_CLASS = "com.a.eye.skywalking.api.plugin.MockTargetObject";
    static final String INTERCEPTOR_CLASS = "com.a.eye.skywalking.api.plugin.MockPluginInterceptor";
    static final String WEAVE_INSTANCE_METHOD_NAME = "targetInstanceMethod";
    static final String WEAVE_INSTANCE_WITH_EXCEPTION_METHOD_NAME = "targetInstanceMethodWithException";
    static final String WEAVE_STATIC_METHOD_NAME = "targetStaticMethod";
    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(MockTargetObject.class),
                null,
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
    }

    @Test
    public void weaveInstanceMethod() throws Exception {
        ByteBuddyAgent.install();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(AgentBuilder.PoolStrategy.Default.FAST)
                .ignore(none())
                .type(ElementMatchers.is(MockTargetObject.class), ElementMatchers.is(classLoader)).transform(new MockTargetObjectTransformer())
                .installOnByteBuddyAgent();

        try {
            Class<?> type = classLoader.loadClass(MockTargetObject.class.getName());
            assertThat(type.getDeclaredMethod(WEAVE_INSTANCE_METHOD_NAME).invoke(type.getDeclaredConstructor(String.class).newInstance("a"))
                    , CoreMatchers.<Object>is(WEAVE_INSTANCE_METHOD_NAME + "a"));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }


    @Test(expected = RuntimeException.class)
    public void weaveInstanceMethodWITEXCEPTION() throws Exception {
        ByteBuddyAgent.install();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(AgentBuilder.PoolStrategy.Default.FAST)
                .ignore(none())
                .type(ElementMatchers.is(MockTargetObject.class), ElementMatchers.is(classLoader)).transform(new MockTargetObjectTransformer())
                .installOnByteBuddyAgent();

        try {
            Class<?> type = classLoader.loadClass(MockTargetObject.class.getName());
            type.getDeclaredMethod(WEAVE_INSTANCE_WITH_EXCEPTION_METHOD_NAME).invoke(type.getDeclaredConstructor(String.class).newInstance("a"));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    public void weaveStaticMethod() throws Exception {
        ByteBuddyAgent.install();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(AgentBuilder.PoolStrategy.Default.FAST)
                .ignore(none())
                .type(ElementMatchers.is(MockTargetObject.class), ElementMatchers.is(classLoader)).transform(new MockTargetObjectTransformer())
                .installOnByteBuddyAgent();

        try {
            Class<?> type = classLoader.loadClass(MockTargetObject.class.getName());
            assertThat(type.getDeclaredMethod(WEAVE_STATIC_METHOD_NAME).invoke(type), CoreMatchers.<Object>is(WEAVE_STATIC_METHOD_NAME + "_STATIC"));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    public static class MockTargetObjectTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
            try {
                DynamicType.Builder newBuilder = transformInstanceMethod(builder);
                return transformStaticMethod(newBuilder);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }

        private DynamicType.Builder<?> transformStaticMethod(DynamicType.Builder newBuilder) {
            MockPluginStaticMethodInstrumentation staticMethodInstrumentation = new MockPluginStaticMethodInstrumentation();
            return staticMethodInstrumentation.define(WEAVE_CLASS, newBuilder);
        }

        private DynamicType.Builder transformInstanceMethod(DynamicType.Builder<?> builder) {
            MockPluginInstanceMethodInstrumentation instrumentation = new MockPluginInstanceMethodInstrumentation();
            return instrumentation.define(WEAVE_CLASS, builder);
        }
    }

}