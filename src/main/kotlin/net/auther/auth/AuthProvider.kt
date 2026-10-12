package net.auther.auth

/**
 * Describes a Yggdrasil-compatible session server that can answer
 * `GET <checkUrl>?username=..&serverId=..[&ip=..]` ("has this player joined?").
 *
 * Mojang/Microsoft is **not** a provider here: the vanilla login handshake
 * already checks it, and `HasJoinedServerMixin` only consults the providers
 * below when Mojang rejects the session.
 */
interface AuthProvider {
    /** Short id used in the config file, e.g. `"elyby"`. */
    val id: String

    /** Human-readable name for logs. */
    val displayName: String

    /** The provider's `hasJoined` endpoint. */
    val checkUrl: String

    /**
     * Optional URL template for fetching signed profile properties (textures).
     * `{0}` is replaced with the player's username. `null` = use the properties
     * from the `hasJoined` response.
     */
    val propertyUrl: String?
}
