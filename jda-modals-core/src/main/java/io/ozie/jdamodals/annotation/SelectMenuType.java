package io.ozie.jdamodals.annotation;

/**
 * Defines the type of SelectMenu component.
 */
public enum SelectMenuType {
    /**
     * A select menu for choosing Discord users.
     * Field type should be {@link net.dv8tion.jda.api.entities.User},
     * {@link net.dv8tion.jda.api.entities.Member}, or a List of these.
     */
    USER,

    /**
     * A select menu for choosing Discord roles.
     * Field type should be {@link net.dv8tion.jda.api.entities.Role}
     * or {@link java.util.List} of Role.
     */
    ROLE,

    /**
     * A select menu for choosing Discord channels.
     * Field type should be a channel type (e.g., GuildChannel, TextChannel)
     * or {@link java.util.List} of channels.
     * Can be filtered by channel types using {@link SelectMenu#channelTypes()}.
     */
    CHANNEL,

    /**
     * A select menu for choosing users or roles (mentionables).
     * Field type should be {@link net.dv8tion.jda.api.entities.IMentionable}
     * or {@link java.util.List} of IMentionable.
     */
    MENTIONABLE,

    /**
     * A select menu with predefined string options.
     * Field type should be String or {@link java.util.List} of String.
     * Options are defined using {@link SelectMenu#options()}.
     */
    STRING
}
