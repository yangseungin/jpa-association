package proxy;

import net.sf.cglib.proxy.Enhancer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HelloTargetTest {
    @DisplayName("MethodInterceptor 이용하여 대문자로 인삿말을 출력한다.")
    @Test
    void BigLetterInterceptorTest() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(HelloTarget.class);
        enhancer.setCallback(new BigLetterInterceptor());
        Object obj = enhancer.create();
        HelloTarget helloTarget = (HelloTarget) obj;
        String actualSayHello = helloTarget.sayHello("다희");
        String actualHi = helloTarget.sayHi("다희");
        String actualThankYou = helloTarget.sayThankYou("다희");

        Assertions.assertThat(actualSayHello).isEqualTo("HELLO 다희");
        Assertions.assertThat(actualHi).isEqualTo("HI 다희");
        Assertions.assertThat(actualThankYou).isEqualTo("THANK YOU 다희");
    }

    @DisplayName("LazyLoader 이용하여, 항상 같은 인삿말을 출력한다.")
    @Test
    void helloTargetProxyTest() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Name.class);
        enhancer.setCallback(new NameLazyLoader());
        Object obj = enhancer.create();
        Name name = (Name) obj;
        String actualSayHello = name.sayName();

        Assertions.assertThat(actualSayHello).isEqualTo("I'm Proxy 객체");
    }
}
