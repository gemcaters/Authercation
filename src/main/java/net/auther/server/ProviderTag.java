package net.auther.server;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** The visible marker appended to display names of fallback-provider players. */
public final class ProviderTag {

    private ProviderTag() {}

    public static MutableComponent suffix() {
        return Component.literal(" [Ely.by]").withStyle(ChatFormatting.GREEN);
    }
}
