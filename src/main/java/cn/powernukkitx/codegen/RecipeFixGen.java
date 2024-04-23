package cn.powernukkitx.codegen;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RecipeFixGen {
    static Set<String> fixForSlab = Set.of("minecraft:stone_block_slab", "minecraft:stone_block_slab2", "minecraft:stone_block_slab3", "minecraft:stone_block_slab4");

    public static int slabAux(String s) {
        return switch (s) {
            case "minecraft:smooth_stone:0", "minecraft:red_sandstone:0", "minecraft:end_stone:0",
                 "minecraft:end_bricks:0", "minecraft:stonebrick:1", "minecraft:red_sandstone:1" -> 0;
            case "minecraft:sandstone:0", "minecraft:sandstone:1", "minecraft:purpur_block:0",
                 "minecraft:red_sandstone:3", "minecraft:quartz_block:3" -> 1;
            case "minecraft:prismarine:0", "minecraft:polished_andesite:0", "minecraft:stone:0" -> 2;
            case "minecraft:cobblestone:0", "minecraft:prismarine:1", "minecraft:andesite:0", "minecraft:sandstone:2" ->
                    3;
            case "minecraft:brick_block:0", "minecraft:prismarine:2", "minecraft:diorite:0",
                 "minecraft:red_sandstone:2" -> 4;
            case "minecraft:stonebrick:0", "minecraft:mossy_cobblestone:0", "minecraft:polished_diorite:0" -> 5;
            case "minecraft:quartz_block:0", "minecraft:sandstone:3", "minecraft:granite:0" -> 6;
            case "minecraft:nether_brick:0", "minecraft:red_nether_brick:0", "minecraft:polished_granite:0" -> 7;
            default -> throw new IllegalStateException("Unexpected value: " + s);
        };
    }

    public static void main(String[] args) {
        try (var recipe = RecipeFixGen.class.getClassLoader().getResourceAsStream("recipes.json")) {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().setNumberToNumberStrategy(JsonReader::nextInt).setObjectToNumberStrategy(JsonReader::nextInt).create();
            Map map = gson.fromJson(new InputStreamReader(recipe), Map.class);
            JsonObject jsonTree = gson.toJsonTree(map).getAsJsonObject();
            JsonArray recipes = jsonTree.getAsJsonArray("recipes");

            //fix for stone_block_slab output
            for (var member : recipes.asList()) {
                JsonObject o = member.getAsJsonObject();
                int type = o.get("type").getAsInt();

                if (type == 0 || type == 1) {//fix slab craft for stonecutter or crafting_table
                    List<JsonElement> output = o.getAsJsonArray("output").asList();
                    JsonObject block = output.get(0).getAsJsonObject();
                    if (fixForSlab.contains(block.get("id").getAsString()) && !block.has("auxValue")) {
                        JsonObject input;
                        if (type == 0) {
                            input = o.getAsJsonArray("input").asList().get(0).getAsJsonObject();
                        } else {
                            input = o.getAsJsonObject("input").get("A").getAsJsonObject();
                        }
                        block.addProperty("auxValue", slabAux(input.get("itemId").getAsString() + ":" + input.get("auxValue").getAsInt()));
                        output.set(0, block);
                    }
                }

                if (type == 1) {//fix craft for axe
                    List<JsonElement> output = o.getAsJsonArray("output").asList();
                    JsonObject block = output.get(0).getAsJsonObject();
                    JsonObject in = o.getAsJsonObject("input");
                    if (block.get("id").getAsString().endsWith("_axe") && in.has("A") && in.has("B")) {
                        in.addProperty("mirror", true);
                    }
                }

                fixSandStoneRecipe(o);
                fixQuartzBlockRecipe(o);
            }
            Path path = Path.of("build/recipes.json");
            Files.deleteIfExists(path);
            Files.writeString(path, gson.toJson(jsonTree), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //fix recipe about sandstone
    public static void fixSandStoneRecipe(JsonObject o) {
        String id = Optional.ofNullable(o.get("id")).map(JsonElement::getAsString).orElse("");
        JsonElement output = o.get("output");
        if (output instanceof JsonArray jsonArray) {
            List<JsonElement> outputList = jsonArray.asList();
            JsonObject block = outputList.get(0).getAsJsonObject();
            if (id.equals("minecraft:stonecutter_sandstone_heiroglyphs") ||
                    id.equals("heiroglyphs_sandstone_recipeId") ||
                    id.equals("minecraft:stonecutter_red_sandstone_heiroglyphs") ||
                    id.equals("heiroglyphs_redsandstone_recipeId")
            ) {
                block.addProperty("auxValue", 1);
                outputList.set(0, block);
            } else if (id.equals("minecraft:stonecutter_sandstone_cut") || id.equals("minecraft:stonecutter_red_sandstone_cut")) {
                block.addProperty("auxValue", 2);
                outputList.set(0, block);
            } else if (id.equals("minecraft:smooth_sandstone") || id.equals("minecraft:smooth_red_sandstone")) {
                block.addProperty("auxValue", 3);
                outputList.set(0, block);
            }
        }
        if (Optional.ofNullable(o.get("block")).map(JsonElement::getAsString).orElse("").equals("furnace")) {
            JsonObject outputF = o.getAsJsonObject("output");
            String str = o.getAsJsonObject("output").get("id").getAsString();
            if (str.equals("minecraft:sandstone")
                    || str.equals("minecraft:red_sandstone")) {
                outputF.addProperty("auxValue", 3);
            }
        }
    }

    public static void fixQuartzBlockRecipe(JsonObject o) {
        String id = Optional.ofNullable(o.get("id")).map(JsonElement::getAsString).orElse("");
        JsonElement output = o.get("output");
        if (output instanceof JsonArray jsonArray) {
            List<JsonElement> outputList = jsonArray.asList();
            JsonObject block = outputList.get(0).getAsJsonObject();
            if (id.equals("minecraft:stonecutter_quartz_chiseled") || id.equals("chiseled_quartz_recipeId")
            ) {
                block.addProperty("auxValue", 1);
                outputList.set(0, block);
            } else if (id.equals("minecraft:stonecutter_quartz_lines") || id.equals("minecraft:pillar_quartz_block")) {
                block.addProperty("auxValue", 2);
                outputList.set(0, block);
            }
        }
        if (Optional.ofNullable(o.get("block")).map(JsonElement::getAsString).orElse("").equals("furnace")) {
            JsonObject outputF = o.getAsJsonObject("output");
            String str = o.getAsJsonObject("output").get("id").getAsString();
            if (str.equals("minecraft:quartz_block")) {
                outputF.addProperty("auxValue", 3);
            }
        }
    }
}
