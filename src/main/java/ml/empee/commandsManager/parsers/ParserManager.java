package ml.empee.commandsManager.parsers;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.SneakyThrows;
import ml.empee.commandsManager.parsers.types.EnumParser;
import ml.empee.commandsManager.parsers.types.annotations.EnumParam;

public final class ParserManager {

  private final HashMap<Integer, ParameterParser<?>> defaultParsers = new HashMap<>();
  private final HashMap<Integer, Class<? extends ParameterParser<?>>> registeredParsers = new HashMap<>();
  private final ArrayList<ParameterParser<?>> cachedParsers = new ArrayList<>();

  public void registerParser(Class<? extends Annotation> identifier, Class<? extends ParameterParser<?>> parser) {
    registeredParsers.put(identifier.hashCode(), parser);
  }

  public void setDefaultParserForType(Class<?> targetType, ParameterParser<?> parser) {
    defaultParsers.put(targetType.hashCode(), cacheParser(parser));
  }

  public boolean isParserRegistered(Class<? extends Annotation> identifier) {
    return registeredParsers.get(identifier.hashCode()) != null;
  }

  @SuppressWarnings("unchecked")
  public ParameterParser<Object> getParameterParser(Parameter parameter) {
    ParameterParser<?> parser = null;
    for (Annotation annotation : parameter.getAnnotations()) {
      parser = buildParameterParser(parameter, annotation);
      if (parser != null) {
        break;
      }
    }

    if (parser == null) {
      parser = defaultParsers.get(parameter.getType().hashCode());
    }


    if(parser == null && parameter.getType().isEnum()) {
      parser = new EnumParser<>("", "", (Class<Enum>) parameter.getType());
    }

    if(parser != null && (parser.getLabel() == null || parser.getLabel().isEmpty()) && parameter.isNamePresent()) {
      parser.setLabel(parameter.getName());
    }

    return (ParameterParser<Object>) cacheParser(parser);
  }

  @SneakyThrows
  private ArrayList<Object> extractParserConstructorArguments(Annotation annotation) {
    ArrayList<Object> params = new ArrayList<>();
    for (Method method : annotation.annotationType().getMethods()) {
      ParameterParser.Property property = method.getAnnotation(ParameterParser.Property.class);
      if (property != null) {

        int index = property.index();

        //Fill ArrayList to prevent OutOfBoundsException
        while (index >= params.size()) {
          params.add(null);
        }

        params.set(index, method.invoke(annotation));
      }
    }

    return params;
  }

  private ParameterParser<?> buildParameterParser(Parameter parameter, Annotation annotation) {
    if (!isParserRegistered(annotation.annotationType())) {
      return null;
    }

    Class<? extends Annotation> identifier = annotation.annotationType();
    ArrayList<Object> params = extractParserConstructorArguments(annotation);
    if(annotation.annotationType().equals(EnumParam.class)) {
        params.add(parameter.getType());
    }

    try {
      Class<?>[] paramsType = new Class<?>[params.size()];
      for (int i = 0; i < paramsType.length; i++) {
        paramsType[i] = params.get(i).getClass();
      }

      return getParserClass(identifier).getConstructor(paramsType).newInstance(params.toArray());
    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException("The parameter " + identifier.getName() + " is missing the default constructor", e);
    }

  }

  private Class<? extends ParameterParser<?>> getParserClass(Class<? extends Annotation> identifier) {

    Class<? extends ParameterParser<?>> parameterClazz = registeredParsers.get(identifier.hashCode());

    if (parameterClazz == null) {
      throw new IllegalArgumentException("The parser linked to " + identifier.getName() + " isn't registered");
    }

    return parameterClazz;

  }

  private ParameterParser<?> cacheParser(ParameterParser<?> parser) {
    if(parser == null) {
      return null;
    }

    for (ParameterParser<?> p : cachedParsers) {

      if (p.equals(parser)) {
        return p;
      }

    }

    cachedParsers.add(parser);
    return parser;
  }

}
