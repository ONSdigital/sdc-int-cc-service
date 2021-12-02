package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OrikaTest {

  private MapperFacade mapper;
  private MapperFactory factory;

  @BeforeEach
  public void setUp() {
    factory = new DefaultMapperFactory.Builder().mapNulls(false).build();
    factory.registerClassMap(factory.classMap(One.class, Two.class).byDefault().toClassMap());
    factory.registerClassMap(
        factory.classMap(Class3Impl1.class, Class3Impl1.class).byDefault().toClassMap());
    factory.registerClassMap(
        factory.classMap(Class3Impl2.class, Class3Impl2.class).byDefault().toClassMap());
    mapper = factory.getMapperFacade();
  }

  public static class One {
    public Class3 class3;
  }

  public static class Two {
    public Class3 class3;
  }

  public abstract static class Class3 {
    public String x = "x";
  }

  public static class Class3Impl1 extends Class3 {
    public String y = "y";
  }

  public static class Class3Impl2 extends Class3 {
    public String y = "y";
  }

  @Test
  public void mapping1Test() {
    One one = new One();
    one.class3 = new Class3Impl1();

    Two two = mapper.map(one, Two.class);

    assertEquals(one.class3.x, two.class3.x);
    assertTrue(Class3Impl1.class.isInstance(two.class3));
    assertEquals(((Class3Impl1) one.class3).y, ((Class3Impl1) two.class3).y);
  }

  @Test
  public void mapping2Test() {
    One one = new One();
    one.class3 = new Class3Impl2();

    Two two = mapper.map(one, Two.class);

    assertEquals(one.class3.x, two.class3.x);
    assertTrue(Class3Impl2.class.isInstance(two.class3));
    assertEquals(((Class3Impl2) one.class3).y, ((Class3Impl2) two.class3).y);
  }
}
