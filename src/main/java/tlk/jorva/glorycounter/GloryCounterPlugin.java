package tlk.jorva.glorycounter;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@PluginDescriptor(name = "Glory Counter",
        description = "Count how many glories you charge in the wilderness",
        tags = {"eternal", "glory", "count", "counter"})
public class GloryCounterPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private LocalStorageHandler storageHandler;

    private static final List<Integer> GLORY_IDS = ImmutableList.of(
            ItemID.AMULET_OF_GLORY,
            ItemID.AMULET_OF_GLORY1,
            ItemID.AMULET_OF_GLORY2,
            ItemID.AMULET_OF_GLORY3,
            ItemID.AMULET_OF_GLORY4,
            ItemID.AMULET_OF_GLORY5,
            ItemID.AMULET_OF_GLORY_T,
            ItemID.AMULET_OF_GLORY_T1,
            ItemID.AMULET_OF_GLORY_T2,
            ItemID.AMULET_OF_GLORY_T3,
            ItemID.AMULET_OF_GLORY_T4,
            ItemID.AMULET_OF_GLORY_T5
    );

    private static final List<InventoryID> CONTAINER_IDS = ImmutableList.of(
            InventoryID.INVENTORY,
            InventoryID.EQUIPMENT
    );

//    private static final int FOUNTAIN_ID = 26782;

    private long userHash;
    private int chargeableGloryCount = 0;
    private int gloriesCharged = 0;

    @Override
    protected void startUp() throws Exception {

    }

    @Override
    protected void shutDown() throws Exception {
        save();
    }

    private void countGlories() {
        int glories = 0;

        for (InventoryID inventoryID : CONTAINER_IDS) {
            ItemContainer container = client.getItemContainer(inventoryID);
            if (container != null) {
                for (int id : GLORY_IDS) {
                    glories += container.count(id);
                }
            }
        }

        chargeableGloryCount = glories;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOADING) {
            userHash = client.getAccountHash();
            gloriesCharged = storageHandler.readFromFile(userHash);
            if (gloriesCharged == -1) {
                storageHandler.writeToFile(userHash, 0);
                gloriesCharged = 0;
            }
        }
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted event) {
        if (event.getCommand().equalsIgnoreCase("glory")) {
            informPlayer();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() == ChatMessageType.SPAM) {
            if (event.getMessage().equalsIgnoreCase("You hold your jewellery against the fountain...")) {
                gloriesCharged += chargeableGloryCount;
                save();
                informPlayer();
                countGlories();
            }
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        countGlories();
    }

    private void _SendMessage(String msg) {
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
    }

    private void informPlayer() {
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", String.format("You've charged %s glor%s.", gloriesCharged, gloriesCharged == 1 ? "y" : "ies"), null);
    }

    private void save() {
        storageHandler.writeToFile(userHash, gloriesCharged);
    }
}
