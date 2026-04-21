package io.ozie.jdamodals.yaml;

import io.ozie.jdamodals.ModalModel;
import io.ozie.jdamodals.annotation.Attachment;
import io.ozie.jdamodals.annotation.ConditionalStep;
import io.ozie.jdamodals.annotation.Modal;
import io.ozie.jdamodals.annotation.SelectMenu;
import io.ozie.jdamodals.annotation.SelectMenuType;
import io.ozie.jdamodals.annotation.SelectOption;
import io.ozie.jdamodals.annotation.TextInput;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.matcher.ElementMatchers;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Loads a {@link ModalModel} subclass from a YAML description at runtime.
 *
 * <h2>Two usage modes</h2>
 *
 * <h3>Untyped ({@code load})</h3>
 * The YAML fully describes the modal (id, title, field types). Handlers work with a
 * {@code Map<String, Object>} of values via {@link YamlModalHandler}.
 *
 * <h3>Typed ({@code bind})</h3>
 * The user supplies a Java interface whose methods declare field names and types;
 * YAML carries only the UI metadata (labels, placeholders, options, step, when).
 * The generated class implements the interface, so handlers get type-safe getters
 * via {@link TypedYamlModalHandler}.
 *
 * <pre>{@code
 * public interface RegistrationForm {
 *     String fullName();
 *     Integer age();
 *     Message.Attachment avatar();
 * }
 *
 * Class<? extends ModalModel> clazz = loader.bind(yaml, RegistrationForm.class);
 * // a generated instance is BOTH a ModalModel and a RegistrationForm
 * }</pre>
 *
 * <h2>YAML schema</h2>
 * <pre>{@code
 * id: survey
 * title: Survey
 * fields:
 *   - name: hasExperience
 *     type: select              # optional in typed mode (inferred)
 *     menuType: STRING          # optional in typed mode (inferred)
 *     label: Do you have experience?
 *     step: 1
 *     options:
 *       - { value: "yes", label: "Yes" }
 *       - { value: "no",  label: "No"  }
 *
 *   - name: experienceDetails
 *     type: text
 *     label: Describe your experience
 *     style: PARAGRAPH
 *     step: 2
 *     when:                     # generates @ConditionalStep
 *       dependsOn: hasExperience
 *       havingValue: "yes"      # or a list: ["yes", "maybe"]
 *       negate: false
 * }</pre>
 */
public class YamlModalLoader {

    private static final String GENERATED_PACKAGE = "io.ozie.jdamodals.generated";
    private static final AtomicLong COUNTER = new AtomicLong();

    private final ClassLoader parentClassLoader;

    public YamlModalLoader() {
        this(YamlModalLoader.class.getClassLoader());
    }

    public YamlModalLoader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }

    public Class<? extends ModalModel> load(String yaml) {
        return fromMap(new Yaml().load(yaml), null);
    }

    public Class<? extends ModalModel> load(InputStream in) {
        return fromMap(new Yaml().load(in), null);
    }

    public Class<? extends ModalModel> load(Reader reader) {
        return fromMap(new Yaml().load(reader), null);
    }

    public Class<? extends ModalModel> bind(String yaml, Class<?> view) {
        requireInterface(view);
        return fromMap(new Yaml().load(yaml), view);
    }

    public Class<? extends ModalModel> bind(InputStream in, Class<?> view) {
        requireInterface(view);
        return fromMap(new Yaml().load(in), view);
    }

    public Class<? extends ModalModel> bind(Reader reader, Class<?> view) {
        requireInterface(view);
        return fromMap(new Yaml().load(reader), view);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends ModalModel> fromMap(Object raw, Class<?> view) {
        if (!(raw instanceof Map)) {
            throw new IllegalArgumentException("YAML root must be a mapping with 'id', 'title', 'fields'");
        }
        Map<String, Object> spec = (Map<String, Object>) raw;

        String id = requireString(spec, "id");
        String title = requireString(spec, "title");
        List<Map<String, Object>> fields = castList(spec.get("fields"));

        AnnotationDescription modalAnn = AnnotationDescription.Builder.ofType(Modal.class)
                .define("id", id)
                .define("title", title)
                .build();

        String className = GENERATED_PACKAGE + ".Modal_" + sanitize(id) + "_" + COUNTER.incrementAndGet();

        DynamicType.Builder<ModalModel> builder = new ByteBuddy()
                .subclass(ModalModel.class)
                .name(className)
                .annotateType(modalAnn);

        if (view != null) {
            builder = builder.implement(view);
        }

        for (Map<String, Object> field : fields) {
            builder = defineField(builder, field, view);
        }

        if (view != null) {
            for (Method m : accessorMethods(view)) {
                builder = builder.method(ElementMatchers.named(m.getName()))
                        .intercept(FieldAccessor.ofField(m.getName()));
            }
        }

        return builder.make()
                .load(parentClassLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
    }

    private DynamicType.Builder<ModalModel> defineField(DynamicType.Builder<ModalModel> b,
                                                        Map<String, Object> f,
                                                        Class<?> view) {
        Method viewMethod = view == null ? null : findMethod(view, requireString(f, "name"));
        if (viewMethod != null && !f.containsKey("type")) {
            f.put("type", inferTopType(viewMethod, f.containsKey("options")));
        }
        String type = requireString(f, "type").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "text" -> textField(b, f, viewMethod);
            case "attachment" -> attachmentField(b, f, viewMethod);
            case "select" -> selectField(b, f, viewMethod);
            default -> throw new IllegalArgumentException("Unknown field type: " + type);
        };
    }

    private DynamicType.Builder<ModalModel> textField(DynamicType.Builder<ModalModel> b,
                                                      Map<String, Object> f,
                                                      Method viewMethod) {
        String name = requireString(f, "name");
        String label = requireString(f, "label");

        Class<?> fieldType = (viewMethod != null)
                ? boxed(viewMethod.getReturnType())
                : switch (string(f, "javaType", "string").toLowerCase(Locale.ROOT)) {
                    case "int", "integer" -> Integer.class;
                    case "long" -> Long.class;
                    case "double" -> Double.class;
                    default -> String.class;
                };

        AnnotationDescription textInput = AnnotationDescription.Builder.ofType(TextInput.class)
                .define("id", string(f, "id", ""))
                .define("label", label)
                .define("placeholder", string(f, "placeholder", ""))
                .define("style", parseStyle(string(f, "style", "SHORT")))
                .define("required", bool(f, "required", true))
                .define("minLength", intVal(f, "minLength", -1))
                .define("maxLength", intVal(f, "maxLength", -1))
                .define("defaultValue", string(f, "defaultValue", ""))
                .define("order", intVal(f, "order", 0))
                .define("step", intVal(f, "step", 1))
                .build();

        return b.defineField(name, fieldType, Visibility.PRIVATE)
                .annotateField(fieldAnnotations(textInput, f));
    }

    private DynamicType.Builder<ModalModel> attachmentField(DynamicType.Builder<ModalModel> b,
                                                             Map<String, Object> f,
                                                             Method viewMethod) {
        String name = requireString(f, "name");
        String label = requireString(f, "label");
        boolean multi = (viewMethod != null)
                ? isListReturn(viewMethod)
                : bool(f, "multi", false);

        AnnotationDescription attachment = AnnotationDescription.Builder.ofType(Attachment.class)
                .define("id", string(f, "id", ""))
                .define("label", label)
                .define("required", bool(f, "required", true))
                .define("order", intVal(f, "order", 0))
                .define("step", intVal(f, "step", 1))
                .build();

        AnnotationDescription[] anns = fieldAnnotations(attachment, f);
        TypeDefinition fieldType = multi
                ? TypeDescription.Generic.Builder.parameterizedType(List.class, Message.Attachment.class).build()
                : TypeDescription.ForLoadedType.of(Message.Attachment.class);

        return b.defineField(name, fieldType, Visibility.PRIVATE).annotateField(anns);
    }

    private DynamicType.Builder<ModalModel> selectField(DynamicType.Builder<ModalModel> b,
                                                        Map<String, Object> f,
                                                        Method viewMethod) {
        String name = requireString(f, "name");
        String label = requireString(f, "label");
        boolean multi = (viewMethod != null)
                ? isListReturn(viewMethod)
                : bool(f, "multi", false);
        Class<?> elementType = (viewMethod != null)
                ? returnElementType(viewMethod)
                : null;
        SelectMenuType menuType = resolveMenuType(f, elementType);

        AnnotationDescription.Builder ab = AnnotationDescription.Builder.ofType(SelectMenu.class)
                .define("id", string(f, "id", ""))
                .define("label", label)
                .define("type", menuType)
                .define("placeholder", string(f, "placeholder", ""))
                .define("required", bool(f, "required", true))
                .define("minValues", intVal(f, "minValues", 1))
                .define("maxValues", intVal(f, "maxValues", multi ? 25 : 1))
                .define("order", intVal(f, "order", 0))
                .define("step", intVal(f, "step", 1));

        if (menuType == SelectMenuType.STRING) {
            List<Map<String, Object>> opts = castList(f.get("options"));
            AnnotationDescription[] optionAnns = new AnnotationDescription[opts.size()];
            for (int i = 0; i < opts.size(); i++) {
                optionAnns[i] = buildOption(opts.get(i));
            }
            ab = ab.defineAnnotationArray(
                    "options",
                    TypeDescription.ForLoadedType.of(SelectOption.class),
                    optionAnns);
        }

        AnnotationDescription[] anns = fieldAnnotations(ab.build(), f);
        Class<?> fieldElement = elementType != null ? elementType : elementTypeFor(menuType);

        TypeDefinition fieldType = multi
                ? TypeDescription.Generic.Builder.parameterizedType(List.class, fieldElement).build()
                : TypeDescription.ForLoadedType.of(fieldElement);

        return b.defineField(name, fieldType, Visibility.PRIVATE).annotateField(anns);
    }

    private SelectMenuType resolveMenuType(Map<String, Object> f, Class<?> inferredElement) {
        if (f.containsKey("menuType")) {
            return SelectMenuType.valueOf(requireString(f, "menuType").toUpperCase(Locale.ROOT));
        }
        if (inferredElement != null) {
            if (inferredElement == User.class) return SelectMenuType.USER;
            if (inferredElement == Role.class) return SelectMenuType.ROLE;
            if (GuildChannel.class.isAssignableFrom(inferredElement)) return SelectMenuType.CHANNEL;
            if (inferredElement == IMentionable.class) return SelectMenuType.MENTIONABLE;
            if (inferredElement == String.class && f.containsKey("options")) return SelectMenuType.STRING;
        }
        throw new IllegalArgumentException("Cannot resolve 'menuType' for select field: " + f.get("name"));
    }

    private String inferTopType(Method m, boolean hasOptions) {
        if (hasOptions) return "select";
        Class<?> element = returnElementType(m);
        if (element == Message.Attachment.class) return "attachment";
        if (element == User.class || element == Role.class
                || GuildChannel.class.isAssignableFrom(element)
                || element == IMentionable.class) {
            return "select";
        }
        return "text";
    }

    private static Method findMethod(Class<?> view, String name) {
        for (Method m : view.getMethods()) {
            if (!m.isDefault() && !Modifier.isStatic(m.getModifiers())
                    && m.getParameterCount() == 0 && m.getName().equals(name)) {
                return m;
            }
        }
        throw new IllegalArgumentException(
                "YAML field '" + name + "' has no matching accessor method in " + view.getName());
    }

    private static List<Method> accessorMethods(Class<?> view) {
        List<Method> methods = new ArrayList<>();
        for (Method m : view.getMethods()) {
            if (m.isDefault() || Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            methods.add(m);
        }
        return methods;
    }

    private static boolean isListReturn(Method m) {
        return m.getReturnType() == List.class;
    }

    private static Class<?> returnElementType(Method m) {
        if (m.getReturnType() != List.class) {
            return m.getReturnType();
        }
        Type generic = m.getGenericReturnType();
        if (generic instanceof ParameterizedType pt && pt.getActualTypeArguments().length == 1) {
            Type arg = pt.getActualTypeArguments()[0];
            if (arg instanceof Class<?> c) return c;
        }
        throw new IllegalArgumentException(
                "List return type on " + m.getName() + " must have a concrete element type");
    }

    private static Class<?> boxed(Class<?> c) {
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == double.class) return Double.class;
        if (c == boolean.class) return Boolean.class;
        return c;
    }

    private static void requireInterface(Class<?> view) {
        if (view == null || !view.isInterface()) {
            throw new IllegalArgumentException("bind() requires an interface, got: " + view);
        }
    }

    @SuppressWarnings("unchecked")
    private AnnotationDescription[] fieldAnnotations(AnnotationDescription primary, Map<String, Object> f) {
        Object whenRaw = f.get("when");
        if (whenRaw == null) {
            return new AnnotationDescription[]{primary};
        }
        if (!(whenRaw instanceof Map)) {
            throw new IllegalArgumentException("'when' must be a mapping with dependsOn/havingValue");
        }
        return new AnnotationDescription[]{primary, buildConditional((Map<String, Object>) whenRaw)};
    }

    private AnnotationDescription buildConditional(Map<String, Object> when) {
        String dependsOn = requireString(when, "dependsOn");
        String[] havingValue = stringList(when.get("havingValue"));
        if (havingValue.length == 0) {
            throw new IllegalArgumentException("'when.havingValue' must have at least one value");
        }
        return AnnotationDescription.Builder.ofType(ConditionalStep.class)
                .define("dependsOn", dependsOn)
                .defineArray("havingValue", havingValue)
                .define("negate", bool(when, "negate", false))
                .build();
    }

    private AnnotationDescription buildOption(Map<String, Object> opt) {
        return AnnotationDescription.Builder.ofType(SelectOption.class)
                .define("value", requireString(opt, "value"))
                .define("label", requireString(opt, "label"))
                .define("description", string(opt, "description", ""))
                .define("emoji", string(opt, "emoji", ""))
                .define("isDefault", bool(opt, "isDefault", false))
                .build();
    }

    private static Class<?> elementTypeFor(SelectMenuType type) {
        return switch (type) {
            case STRING -> String.class;
            case USER -> User.class;
            case ROLE -> Role.class;
            case CHANNEL -> GuildChannel.class;
            case MENTIONABLE -> IMentionable.class;
        };
    }

    private static TextInputStyle parseStyle(String s) {
        return TextInputStyle.valueOf(s.toUpperCase(Locale.ROOT));
    }

    private static String requireString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Missing required key: " + key);
        }
        return v.toString();
    }

    private static String string(Map<String, Object> m, String key, String defaultValue) {
        Object v = m.get(key);
        return v == null ? defaultValue : v.toString();
    }

    private static boolean bool(Map<String, Object> m, String key, boolean defaultValue) {
        Object v = m.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }

    private static int intVal(Map<String, Object> m, String key, int defaultValue) {
        Object v = m.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }

    private static String[] stringList(Object raw) {
        if (raw == null) return new String[0];
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).toArray(String[]::new);
        }
        return new String[]{raw.toString()};
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object raw) {
        if (raw == null) return List.of();
        if (!(raw instanceof List)) {
            throw new IllegalArgumentException("Expected a list, got: " + raw.getClass().getSimpleName());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) {
                throw new IllegalArgumentException("Expected a mapping in list, got: " + item);
            }
            result.add((Map<String, Object>) item);
        }
        return result;
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
