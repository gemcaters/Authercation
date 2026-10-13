package net.auther.server;

import net.auther.auth.ProviderUuids;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Puts fallback-provider players (recognised by {@link ProviderUuids#isForeign})
 * into a scoreboard team whose suffix is {@code [Ely.by]}.
 *
 * Because it is a real team suffix, vanilla shows it in the tab list, chat,
 * join/death messages and above the player's head. The profile name itself is
 * never changed, so commands, whitelist and ops keep working.
 *
 * A player can only be in one team: if an admin already put the player in
 * another team, that team is left alone and no suffix is added.
 */
public final class ProviderTeams {

    private static final Logger LOGGER = LoggerFactory.getLogger("Authercation/Teams");

    /** Name of the scoreboard team used for the suffix. */
    public static final String TEAM_NAME = "authercation_elyby";

    private ProviderTeams() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> apply(handler.player, server));
    }

    private static void apply(ServerPlayer player, MinecraftServer server) {
        if (!ProviderUuids.isForeign(player.getUUID())) {
            return;
        }

        Scoreboard scoreboard = server.getScoreboard();
        String holder = player.getScoreboardName();

        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        PlayerTeam current = scoreboard.getPlayersTeam(holder);

        if (current != null && current != team) {
            LOGGER.debug("{} is already in team '{}', not adding the provider suffix", holder, current.getName());
            return;
        }

        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
        }
        team.setPlayerSuffix(ProviderTag.suffix());

        if (current == null) {
            scoreboard.addPlayerToTeam(holder, team);
        }
    }
}
