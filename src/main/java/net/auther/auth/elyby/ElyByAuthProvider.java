package net.auther.auth.elyby;

import net.auther.auth.AuthProvider;

/**
 * Ely.by session server.
 *
 * Docs: https://docs.ely.by/en/minecraft-auth.html (`GET /session/hasJoined` on
 * `https://authserver.ely.by`). Textures are signed through Ely.by's skin system.
 *
 * When Ely.by authenticates a session, the session response properties may not
 * be Mojang-signed. Under `enforce-secure-profile=true`, that can produce the
 * error "Invalid signature for profile public key" on join.
 *
 * Mitigations: install No Chat Reports on the server, or set
 * `enforce-secure-profile=false` in `server.properties`.
 */
public final class ElyByAuthProvider implements AuthProvider {
    @Override
    public String getId() {
        return "elyby";
    }

    @Override
    public String getDisplayName() {
        return "Ely.by";
    }

    @Override
    public String getCheckUrl() {
        return "https://authserver.ely.by/session/hasJoined";
    }

    @Override
    public String getPropertyUrl() {
        return "http://skinsystem.ely.by/textures/signed/{0}";
    }
}
