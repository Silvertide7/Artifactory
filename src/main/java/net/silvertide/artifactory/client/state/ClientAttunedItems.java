package net.silvertide.artifactory.client.state;

import net.silvertide.artifactory.modifications.AttunementModification;
import net.silvertide.artifactory.modifications.ModificationFactory;
import net.silvertide.artifactory.storage.AttunedItem;

import java.util.*;

public class ClientAttunedItems {
    private ClientAttunedItems(){};
    private static Map<UUID, AttunedItem> myAttunedItems = new HashMap<>();
    private static Map<String, String> attunedItemModifications = new HashMap<>();

    public static void setAttunedItem(AttunedItem attunedItem) {
        myAttunedItems.put(attunedItem.getItemUUID(), attunedItem);
    }

    public static void setModification(String resourceLocation, String description) {
        attunedItemModifications.put(resourceLocation, description);
    }

    public static List<String> getModifications(String resourceLocation) {
        String modifications = attunedItemModifications.get(resourceLocation);
        return modifications != null ? getModificationDescriptions(modifications) : new ArrayList<>();
    }

    private static List<String> getModificationDescriptions(String modificationSerialization) {
        // These serializations take the form 1#soulbound,invulnerable~2#unbreakable,attribute/...
        // We need to break this apart into usable information by each level.

        ArrayList<String> results = new ArrayList<>();
        // "1#soulbound,invulnerable~2#unbreakable"

        // Break the encoding up by level
        String[] modificationLevelCodes = modificationSerialization.split("~");
        for (String modLevelCode : modificationLevelCodes) {

            // Break it up by the level number and the modifications themselves
            String[] modLevelInformation = modLevelCode.split("#");

            // The first index should be the level (1 or 2 etc).
            // The second index should be the modification codes separated by commas.
            // As such there should only be 2 indices
            if(modLevelInformation.length == 2) {
                StringBuilder levelDescription = new StringBuilder("Level ").append(modLevelInformation[0]).append(": ");

                // If this level doesn't have any modifications associated with it.
                if("NONE".equals(modLevelInformation[1])) {
                    levelDescription.append("None");
                } else {
                    // Split it apart into individual modification codes
                    String[] modificationCodes = modLevelInformation[1].split(",");
                    for(int i = 0; i < modificationCodes.length; i++) {
                        String modificationCode = modificationCodes[i];

                        // This code should successfully create a modification. We will then use that modifications toString
                        // to get the relevant information.
                        Optional<AttunementModification> modification = ModificationFactory.createAttunementModification(modificationCode);
                        if(modification.isPresent()) {
                            levelDescription.append(modification.get());
                            if(i != modificationCodes.length - 1) levelDescription.append(", ");
                        }
                    }
                }

                results.add(levelDescription.toString());
            }
        }
        return results;
    }

    public static void clearAllAttunedItems() {
        myAttunedItems = new HashMap<>();
        attunedItemModifications = new HashMap<>();
    }

    public static void removeAttunedItem(UUID itemUUIDToRemove) {
        myAttunedItems.remove(itemUUIDToRemove);
    }

    public static List<AttunedItem> getAttunedItemsAsList() {
        return myAttunedItems.isEmpty() ? new ArrayList<>() : new ArrayList<>(myAttunedItems.values());
    }
}
