package com.simibubi.create.item;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.IOUtils;

import com.simibubi.create.AllItems;
import com.simibubi.create.gui.BlueprintEditScreen;
import com.simibubi.create.gui.GuiOpener;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;

public class ItemBlueprint extends Item {

	public ItemBlueprint(Properties properties) {
		super(properties.maxStackSize(1));
	}

	public static ItemStack create(String schematic, String owner) {
		ItemStack blueprint = new ItemStack(AllItems.BLUEPRINT.item);

		CompoundNBT tag = new CompoundNBT();
		tag.putBoolean("Deployed", false);
		tag.putString("Owner", owner);
		tag.putString("File", schematic);
		tag.put("Anchor", NBTUtil.writeBlockPos(BlockPos.ZERO));
		tag.putString("Rotation", Rotation.NONE.name());
		tag.putString("Mirror", Mirror.NONE.name());
		blueprint.setTag(tag);

		writeSize(blueprint);
		blueprint.setDisplayName(new StringTextComponent(TextFormatting.RESET + "" + TextFormatting.WHITE
				+ "Blueprint (" + TextFormatting.GOLD + schematic + TextFormatting.WHITE + ")"));

		return blueprint;
	}

	public static void writeSize(ItemStack blueprint) {
		CompoundNBT tag = blueprint.getTag();
		Template t = getSchematic(blueprint);
		tag.put("Bounds", NBTUtil.writeBlockPos(t.getSize()));
		blueprint.setTag(tag);
	}

	public static PlacementSettings getSettings(ItemStack blueprint) {
		CompoundNBT tag = blueprint.getTag();

		PlacementSettings settings = new PlacementSettings();
		settings.setRotation(Rotation.valueOf(tag.getString("Rotation")));
		settings.setMirror(Mirror.valueOf(tag.getString("Mirror")));

		return settings;
	}

	public static Template getSchematic(ItemStack blueprint) {
		Template t = new Template();
		String owner = blueprint.getTag().getString("Owner");
		String schematic = blueprint.getTag().getString("File");

		String filepath = "";

		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
			filepath = "schematics/uploaded/" + owner + "/" + schematic;
		else
			filepath = "schematics/" + schematic;

		InputStream stream = null;
		try {
			stream = Files.newInputStream(Paths.get(filepath), StandardOpenOption.READ);
			CompoundNBT nbt = CompressedStreamTools.readCompressed(stream);
			t.read(nbt);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (stream != null)
				IOUtils.closeQuietly(stream);
		}

		return t;
	}

	@Override
	public ActionResultType onItemUse(ItemUseContext context) {
		if (context.isPlacerSneaking() && context.getHand() == Hand.MAIN_HAND) {
			DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
				GuiOpener.open(new BlueprintEditScreen());
			});
			return ActionResultType.SUCCESS;
		}

		return super.onItemUse(context);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
		if (playerIn.isSneaking() && handIn == Hand.MAIN_HAND) {
			DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
				GuiOpener.open(new BlueprintEditScreen());
			});
			return new ActionResult<ItemStack>(ActionResultType.SUCCESS, playerIn.getHeldItem(handIn));
		}

		return super.onItemRightClick(worldIn, playerIn, handIn);
	}

}