package proxy;

import net.sf.cglib.proxy.LazyLoader;

public class NameLazyLoader implements LazyLoader {
    @Override
    public Object loadObject() {
        return new Name("Proxy 객체");
    }
}
