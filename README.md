# JDA Modals

Declarative modal framework for [JDA](https://github.com/discord-jda/JDA) (Java Discord API).

Define Discord modals using annotations instead of builder boilerplate.

## Features

- **Declarative syntax** — define modals as annotated POJOs
- **Type-safe mapping** — automatic conversion to String, Integer, Long, Double, User, Role, Channel
- **All component types** — TextInput, Attachment, SelectMenu (String/User/Role/Channel/Mentionable)
- **Multi-step wizards** — automatic session management for forms with >5 fields
- **Nested composition** — reuse field groups across modals
- **Spring Boot starter** — zero-config integration

## Requirements

- Java 21+
- JDA 6.2.0+

## Installation

### Maven

```xml
<dependency>
    <groupId>io.ozie.jda-modals</groupId>
    <artifactId>jda-modals-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Optional: Spring Boot integration -->
<dependency>
    <groupId>io.ozie.jda-modals</groupId>
    <artifactId>jda-modals-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### 1. Define a Modal

```java
@Modal(id = "feedback", title = "Feedback Form")
public class FeedbackModal extends ModalModel {

    @TextInput(label = "Your Name", placeholder = "Enter your name")
    private String name;

    @TextInput(label = "Message", style = TextInputStyle.PARAGRAPH, maxLength = 1000)
    private String message;

    @SelectMenu(
        type = SelectMenuType.STRING,
        label = "Rating",
        options = {
            @SelectOption(value = "5", label = "Excellent"),
            @SelectOption(value = "3", label = "Average"),
            @SelectOption(value = "1", label = "Poor")
        }
    )
    private String rating;

    // Getters
    public String getName() { return name; }
    public String getMessage() { return message; }
    public String getRating() { return rating; }
}
```

### 2. Create a Handler

```java
public class FeedbackHandler implements ModalHandler<FeedbackModal> {

    @Override
    public Class<FeedbackModal> getModalClass() {
        return FeedbackModal.class;
    }

    @Override
    public void handle(ModalInteractionEvent event, FeedbackModal modal) {
        event.reply("Thanks, " + modal.getName() + "! Rating: " + modal.getRating())
            .setEphemeral(true)
            .queue();
    }
}
```

### 3. Register and Show

```java
// Setup
ModalMapper mapper = new ModalMapper();
ModalDispatcher dispatcher = new ModalDispatcher(mapper, List.of(new FeedbackHandler()));
jda.addEventListener(dispatcher);

// Show modal (e.g., from slash command)
Modal modal = mapper.build(FeedbackModal.class);
event.replyModal(modal).queue();
```

## Annotations

### @Modal

Class-level annotation for modal definition.

```java
@Modal(id = "unique-id", title = "Modal Title")
public class MyModal extends ModalModel { }
```

### @TextInput

Text input field.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `label` | String | required | Display label |
| `id` | String | field name | Component ID |
| `placeholder` | String | "" | Placeholder text |
| `style` | TextInputStyle | SHORT | SHORT or PARAGRAPH |
| `required` | boolean | true | Is required |
| `minLength` | int | -1 | Minimum length |
| `maxLength` | int | -1 | Maximum length |
| `defaultValue` | String | "" | Pre-filled value |
| `order` | int | 0 | Display order |
| `step` | int | 1 | Wizard step number |

**Supported field types:** `String`, `Integer`, `int`, `Long`, `long`, `Double`, `double`

### @Attachment

File upload field.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `label` | String | required | Display label |
| `id` | String | field name | Component ID |
| `required` | boolean | true | Is required |
| `order` | int | 0 | Display order |
| `step` | int | 1 | Wizard step number |

**Supported field types:** `Message.Attachment`, `List<Message.Attachment>`

### @SelectMenu

Selection menu for strings or Discord entities.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `type` | SelectMenuType | required | Menu type |
| `label` | String | required | Display label |
| `id` | String | field name | Component ID |
| `placeholder` | String | "" | Placeholder text |
| `required` | boolean | true | Is required |
| `minValues` | int | 1 | Minimum selections |
| `maxValues` | int | 1 | Maximum selections |
| `options` | SelectOption[] | {} | Options for STRING type |
| `channelTypes` | ChannelType[] | {} | Filter for CHANNEL type |
| `order` | int | 0 | Display order |
| `step` | int | 1 | Wizard step number |

**SelectMenuType and supported field types:**

| Type | Field Types |
|------|-------------|
| `STRING` | `String`, `List<String>` |
| `USER` | `User`, `Member`, `List<User>`, `List<Member>` |
| `ROLE` | `Role`, `List<Role>` |
| `CHANNEL` | `GuildChannel`, `TextChannel`, `VoiceChannel`, `List<...>` |
| `MENTIONABLE` | `IMentionable`, `List<IMentionable>` |

### @SelectOption

Options for STRING select menus.

```java
@SelectMenu(
    type = SelectMenuType.STRING,
    label = "Color",
    options = {
        @SelectOption(value = "red", label = "Red", description = "Warm color", emoji = "🔴"),
        @SelectOption(value = "blue", label = "Blue", isDefault = true)
    }
)
private String color;
```

### @NestedModal

Embed reusable field groups.

```java
public class AddressFields extends ModalModel {
    @TextInput(label = "Street")
    private String street;

    @TextInput(label = "City")
    private String city;
}

@Modal(id = "order", title = "Order")
public class OrderModal extends ModalModel {

    @NestedModal(prefix = "shipping", titleSuffix = " - Shipping")
    private AddressFields shippingAddress;

    @NestedModal(prefix = "billing", titleSuffix = " - Billing")
    private AddressFields billingAddress;
}
```

## Multi-Step Wizards

Discord limits modals to 5 components. Use `step` parameter for longer forms:

```java
@Modal(id = "registration", title = "Registration")
public class RegistrationModal extends ModalModel {

    @TextInput(label = "Username", step = 1)
    private String username;

    @TextInput(label = "Email", step = 1)
    private String email;

    @TextInput(label = "Password", step = 2)
    private String password;

    @TextInput(label = "Bio", step = 3, style = TextInputStyle.PARAGRAPH)
    private String bio;
}
```

The framework automatically:
- Generates step IDs: `registration_step_1`, `registration_step_2`, `registration_step_3`
- Shows step indicator in title: "Registration (1/3)"
- Stores intermediate data in `ModalSessionStorage`
- Reconstructs the full model after completion

## Spring Boot Integration

Add the starter dependency and annotate handlers with `@Component`:

```java
@Component
public class MyHandler implements ModalHandler<MyModal> {
    // Automatically registered
}
```

### Configuration

```yaml
jda:
  modals:
    session-ttl: 15m      # Session expiration (default: 15 minutes)
    cleanup-interval: 5m  # Cleanup task interval (default: 5 minutes)
```

### Internationalization

When Spring's `MessageSource` is available, the framework uses it for i18n. Otherwise, falls back to `jda-modals-messages.properties`.

## API Reference

### ModalMapper

```java
ModalMapper mapper = new ModalMapper();

// Build modal for display
Modal modal = mapper.build(MyModal.class);
Modal step2 = mapper.buildForStep(WizardModal.class, 2);

// Map event to model (single-step)
MyModal model = mapper.map(event, MyModal.class);

// Wizard utilities
int totalSteps = mapper.getTotalSteps(WizardModal.class);
boolean isWizard = mapper.isMultiStep(WizardModal.class);
String baseId = mapper.getBaseModalId("wizard_step_2"); // "wizard"
int step = mapper.getStepFromModalId("wizard_step_2");  // 2
```

### ModalDispatcher

```java
ModalDispatcher dispatcher = new ModalDispatcher(
    mapper,
    handlers,
    sessionStorage,  // optional, created automatically
    modalMessages    // optional, for i18n
);

// Register as JDA listener
jda.addEventListener(dispatcher);
```