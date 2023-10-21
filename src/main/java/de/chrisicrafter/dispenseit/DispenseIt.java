package de.chrisicrafter.dispenseit;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.List;

@Mod(DispenseIt.MOD_ID)
public class DispenseIt {
    public static final String MOD_ID = "dispenseit";
    private static final Logger LOGGER = LogUtils.getLogger();

    public DispenseIt() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("COMMON SETUP");

        //Register block-place dispenser-behaviour
        for(Block block : ForgeRegistries.BLOCKS.getValues()) {
            if(block.defaultBlockState().getPistonPushReaction() == PushReaction.NORMAL || block.defaultBlockState().getPistonPushReaction() == PushReaction.PUSH_ONLY) {
                Item item = block.asItem();
                DispenserBlock.registerBehavior(item, new DefaultDispenseItemBehavior() {
                    @Override
                    protected ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
                        ServerLevel world = blockSource.level();
                        Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
                        BlockPos pos = blockSource.pos();
                        switch (direction) {
                            case DOWN -> pos = pos.below();
                            case UP -> pos = pos.above();
                            case EAST -> pos = pos.east();
                            case WEST -> pos = pos.west();
                            case NORTH -> pos = pos.north();
                            case SOUTH -> pos = pos.south();
                        }
                        if (world.getBlockState(pos).isAir()) {
                            world.setBlock(pos, block.defaultBlockState(), 3);
                            itemStack.split(1);
                            return itemStack;
                        }
                        return super.execute(blockSource, itemStack);
                    }
                });
            }
        }

        //Register block-mine dispenser-behaviour
        for(Item item : ForgeRegistries.ITEMS.getValues()) {
            if(item instanceof DiggerItem) {
                DispenserBlock.registerBehavior(item, new DefaultDispenseItemBehavior() {
                    @Override
                    protected ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
                        ServerLevel world = blockSource.level();
                        Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
                        BlockPos pos = blockSource.pos();
                        switch (direction) {
                            case DOWN -> pos = pos.below();
                            case UP -> pos = pos.above();
                            case EAST -> pos = pos.east();
                            case WEST -> pos = pos.west();
                            case NORTH -> pos = pos.north();
                            case SOUTH -> pos = pos.south();
                        }
                        BlockState blockState = world.getBlockState(pos);
                        if (itemStack.getItem() instanceof TieredItem tieredItem
                                && ((blockState.is(BlockTags.MINEABLE_WITH_AXE) && itemStack.getItem() instanceof AxeItem)
                                || (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE) && itemStack.getItem() instanceof PickaxeItem)
                                || (blockState.is(BlockTags.MINEABLE_WITH_HOE) && itemStack.getItem() instanceof HoeItem)
                                || (blockState.is(BlockTags.MINEABLE_WITH_SHOVEL) && itemStack.getItem() instanceof ShovelItem))
                                && ((blockState.is(BlockTags.NEEDS_STONE_TOOL) && tieredItem.getTier().getLevel() >= Tiers.STONE.getLevel())
                                || (blockState.is(BlockTags.NEEDS_IRON_TOOL) && tieredItem.getTier().getLevel() >= Tiers.IRON.getLevel())
                                || (blockState.is(BlockTags.NEEDS_DIAMOND_TOOL) && tieredItem.getTier().getLevel() >= Tiers.DIAMOND.getLevel())
                                || (!blockState.is(BlockTags.NEEDS_STONE_TOOL) && !blockState.is(BlockTags.NEEDS_IRON_TOOL) && !blockState.is(BlockTags.NEEDS_DIAMOND_TOOL)))
                                && !Block.getDrops(blockState, world, pos, null, null, itemStack).isEmpty()) {
                            List<ItemStack> drops = Block.getDrops(blockState, world, pos, null, null, itemStack);
                            world.destroyBlock(pos, false);
                            for (ItemStack stack : drops) {
                                if (stack != null && stack.getCount() > 0) {
                                    ItemEntity entityItem = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
                                    if (stack.hasTag()) {
                                        entityItem.getItem().setTag(stack.getTag().copy());
                                    }
                                    world.addFreshEntity(entityItem);
                                }
                            }
                            if (itemStack.isDamageableItem())
                                itemStack.setDamageValue(itemStack.getDamageValue() + 1);
                            return itemStack;
                        }
                        return super.execute(blockSource, itemStack);
                    }
                });
            }
        }

        //Register lava-cauldron-emptying dispenser-behaviour
        DispenserBlock.registerBehavior(Items.BUCKET, new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
                ServerLevel world = blockSource.level();
                Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
                BlockPos pos = blockSource.pos();
                switch (direction) {
                    case DOWN -> pos = pos.below();
                    case UP -> pos = pos.above();
                    case EAST -> pos = pos.east();
                    case WEST -> pos = pos.west();
                    case NORTH -> pos = pos.north();
                    case SOUTH -> pos = pos.south();
                }
                if(world.getBlockState(pos).getBlock() instanceof LavaCauldronBlock cauldron && cauldron.isFull(world.getBlockState(pos))) {
                    world.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
                    ItemStack stack = new ItemStack(Items.LAVA_BUCKET);
                    ItemEntity entityItem = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
                    if (stack.hasTag()) {
                        entityItem.getItem().setTag(stack.getTag().copy());
                    }
                    world.addFreshEntity(entityItem);
                    itemStack.split(1);
                    return itemStack;
                }
                return super.execute(blockSource, itemStack);
            }
        });
    }
}
