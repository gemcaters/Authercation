package net.auther.auth;

/**
 * Describes a Yggdrasil-compatible session server that can answer
 * `GET <checkUrl>?username=..&serverId=..[&ip=..]` ("has this player joined?").
 *
 * Mojang/Microsoft is **not** a provider here: the vanilla login handshake
 * already checks it, and `HasJoinedServerMixin` only consults the providers
 * below when Mojang rejects the session.
 */
public interface AuthProvider {
    /** Short id used in the config file, e.g. `"elyby"`. */
    String getId();

    /** Human-readable name for logs. */
    String getDisplayName();

    /** The provider's `hasJoined` endpoint. */
    String getCheckUrl();

    /**
     * Optional URL template for fetching signed profile properties (textures).
     * `{0}` is replaced with the player's username. `null` = use the properties
     * from the `hasJoined` response.
     */
    String getPropertyUrl();
}
