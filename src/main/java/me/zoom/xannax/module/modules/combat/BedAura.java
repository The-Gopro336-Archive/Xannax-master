package me.zoom.xannax.module.modules.combat;

import java.util.Comparator;
import java.util.List;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.zoom.xannax.util.friend.Friends;
import me.zoom.xannax.module.Module;
import me.zoom.xannax.setting.Setting;
import me.zoom.xannax.command.Command;
import me.zoom.xannax.util.RenderUtil;
import me.zoom.xannax.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemBed;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

//Probably the module what most people are going to be reading so ill annotate it pretty heavily

//This is the combined work of over a month of bedaura shit from two different clients
//so.. this is pretty spaghetti

//i could do a nice (much cleaner) rewrite but tbh if it works it works and i cba
//as a result theres probably going to be a lot of useless variables and checks that i forgot about
//or didnt bother to remove

//so.. sorry if this is hard to follow

public class BedAura extends Module {
    public BedAura() {
        super("BedAura", "CA but for beds", Category.Combat);
    }

    Setting.Integer range;
    Setting.Boolean announceUsage;
    Setting.Integer placedelay;

    @Override
    public void setup() {
        range = registerInteger("Range", "Range", 7, 0, 9);
        placedelay = registerInteger("Place Delay", "PlaceDelay", 15, 8, 20);
        announceUsage = registerBoolean("ToggleMSG", "ToggleMSG", true);
    }

    private int playerHotbarSlot = -1;
    private int lastHotbarSlot = -1;
    private EntityPlayer closestTarget;
    private String lastTickTargetName;
    private int bedSlot = -1;
    private BlockPos placeTarget;
    private float rotVar;
    private int blocksPlaced;
    private double diffXZ;
    private boolean firstRun;

    private boolean nowTop = false;


    @Override
    public void onEnable() {

        if (mc.player == null) {
            this.toggle();
            return;
        }

        MinecraftForge.EVENT_BUS.register(this);
        //this is gay ^
        //this should already happen on the onEnable() in module.class (since its overriding it)
        //but it doesnt


        firstRun = true;


        blocksPlaced = 0;


        playerHotbarSlot = mc.player.inventory.currentItem;
        lastHotbarSlot = -1;


    }

    @Override
    public void onDisable() {

        if (mc.player == null) {
            return;
        }

        MinecraftForge.EVENT_BUS.unregister(this);

        if (lastHotbarSlot != playerHotbarSlot && playerHotbarSlot != -1) {
            mc.player.inventory.currentItem = playerHotbarSlot;
        }

        playerHotbarSlot = -1;
        lastHotbarSlot = -1;

        if (announceUsage.getValue()) {
            Command.sendClientMessage(ChatFormatting.RED + "BedAura has been toggled off!");
        }

        blocksPlaced = 0;


    }

    @Override
    public void onUpdate() {

        if (mc.player == null) {
            return;
        }
        if(mc.player.dimension == 0) {
            Command.sendClientMessage("You are in the overworld!");
            this.toggle();
        }
        try {
            findClosestTarget();
        } catch(NullPointerException npe) {}
        //yeah if someone knows a more appropriate way to deal with npe crashes
        //please message me on discord
        //im gonna be averaging like 3 try/catches per module at this rate
        //as might as well literally just surround the entire fucking module in a try catch

        if(closestTarget == null && mc.player.dimension != 0) {
            if(firstRun) {
                firstRun = false;
                if(announceUsage.getValue()) {
                    Command.sendClientMessage(ChatFormatting.GREEN + "BedAura has been toggled on! Waiting for target...");
                }
            }
        }

        if (firstRun && closestTarget != null && mc.player.dimension != 0) {
            firstRun = false;
            lastTickTargetName = closestTarget.getName();
            if (announceUsage.getValue()) {
                Command.sendClientMessage(ChatFormatting.GREEN + "BedAura has been toggled on! Target: " + lastTickTargetName);
            }
        }

        if(closestTarget != null && lastTickTargetName != null) {
            if (!lastTickTargetName.equals(closestTarget.getName())) {
                lastTickTargetName = closestTarget.getName();
                if (announceUsage.getValue()) {
                    Command.sendClientMessage(TextFormatting.GREEN + "BedAura new target:" + lastTickTargetName);
                }
            }
        }

        //Stolen from some autotrap ^
        //still a pretty nice way to find the target
        //but i still havent implemented an antisuicide so if the target is in your hole... good luck

        try {
            diffXZ = mc.player.getPositionVector().distanceTo(closestTarget.getPositionVector());
        } catch(NullPointerException npe) {

        }

        try {

            //messiness below
            //TODO: rewrite

            //i figured damage calc is pretty useless since the only way its really gonna do significant damage
            //is if its right next to the target (or ideally - inside it)
            if(closestTarget != null) {
                placeTarget = new BlockPos(closestTarget.getPositionVector().add(1, 1, 0));
                nowTop = false;
                rotVar = 90;

                BlockPos block1 = placeTarget;
                if(!canPlaceBed(block1)) {
                    placeTarget = new BlockPos(closestTarget.getPositionVector().add(-1, 1, 0));
                    rotVar = -90;
                    nowTop = false;
                }
                BlockPos block2 = placeTarget;
                if(!canPlaceBed(block2)) {
                    placeTarget = new BlockPos(closestTarget.getPositionVector().add(0, 1, 1));
                    rotVar = 180;
                    nowTop = false;
                }
                BlockPos block3 = placeTarget;
                if(!canPlaceBed(block3)) {
                    placeTarget = new BlockPos(closestTarget.getPositionVector().add(0, 1, -1));
                    rotVar = 0;
                    nowTop = false;
                }
                BlockPos block4 = placeTarget;
                if(!canPlaceBed(block4)) {
                    placeTarget = new BlockPos(closestTarget.getPositionVector().add(0, 2, -1));
                    rotVar = 0;
                    nowTop = true;
                }
                BlockPos blockt1 = placeTarget;
                if(nowTop && !canPlaceBed(blockt1)) {
                    placeTarget = new BlockPos(closestTarget.getPositionVector().add(-1, 2, 0));
                    rotVar = -90;
                }
                BlockPos blockt2 = placeTarget;
                if(nowTop && !canPlaceBed(blockt2)) {
                    placeTarget = new BlockPos(closestTarget.getPositionVector().add(0, 2, 1));
                    rotVar = 180;
                }
                BlockPos blockt3 = placeTarget;
                if(nowTop && !canPlaceBed(blockt3)) {
                    placeTarget = new BlockPos(closestTarget.getPositionVector().add(1, 2, 0));
                    rotVar = 90;
                }
            }

            mc.world.loadedTileEntityList.stream()
                    .filter(e -> e instanceof TileEntityBed)
                    .filter(e -> mc.player.getDistance(e.getPos().getX(), e.getPos().getY(), e.getPos().getZ()) <= range.getValue())
                    .sorted(Comparator.comparing(e -> mc.player.getDistance(e.getPos().getX(), e.getPos().getY(), e.getPos().getZ())))
                    .forEach(bed -> {
                        if(mc.player.dimension != 0) {
                            mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(bed.getPos(), EnumFacing.UP, EnumHand.OFF_HAND, 0, 0, 0));
                        }
                        //why offhand?
                        //if youre in creative and the bed places without leaving your inventory, it sends a right click packet with the bed still in your hand
                        //this places another bed and the cycle repeats and fucking spams beds
                        //if you send the packet with the offhand, theres no beds to place in your offhand, so it only breaks it
                        //MUCH better than adding a break delay slider since you want the bed damage to be as accurate as possible
                    });

            if((mc.player.ticksExisted % placedelay.getValue() == 0)  && closestTarget != null) {
                //this is a really dope way to do this delay
                //i tried making an int called tickDelay and incrementing it every tick
                //but my onUpdate() literally runs 10 times as fast as it should be, meaning by 20 ticks the variable already had a value of 200

                //blu3 helped me with that so credit to him
                //and axua helped him with that so... credit to him

                this.findBeds();

                mc.player.ticksExisted++;
                // ^ have to add to it otherwise it does it a fuckton
                this.doDaMagic();
            }




        } catch(NullPointerException npe) {
            npe.printStackTrace();

            /*
             * the trick to writing a good program is surrounding the entire thing
             * in try catch brackets so you never get a crash
             */
        }
    }

    //this is all pretty self explanatory
    private void doDaMagic() {
        if(diffXZ <= range.getValue()) {
            for (int i = 0; i < 9; i++) {
                if (bedSlot != -1) {
                    break;
                }
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (stack.getItem() instanceof ItemBed) {
                    bedSlot = i;
                    if(i != -1) {
                        mc.player.inventory.currentItem = bedSlot;
                    }
                    break;
                }
            }
            bedSlot = -1;
            if(blocksPlaced == 0 && mc.player.inventory.getStackInSlot(mc.player.inventory.currentItem).getItem() instanceof ItemBed) {
                mc.player.connection.sendPacket(new CPacketEntityAction((Entity)mc.player, CPacketEntityAction.Action.START_SNEAKING));
                mc.player.connection.sendPacket(new CPacketPlayer.Rotation(rotVar, 0, mc.player.onGround));
                placeBlock(new BlockPos(this.placeTarget), EnumFacing.DOWN);
                mc.player.connection.sendPacket(new CPacketEntityAction((Entity)mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
                blocksPlaced = 1;
                nowTop = false;
            }

            blocksPlaced = 0;
            //blocksPlaced might be useless but i thought it would be a fix to the spamming of beds in creative mode
            //spoiler : it wasnt

        }
    }

    private void findBeds() {
        if (mc.currentScreen == null || !(mc.currentScreen instanceof GuiContainer)) {
            if (mc.player.inventory.getStackInSlot(0).getItem() != Items.BED) {
                for(int i = 9; i < 36; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == Items.BED) {
                        mc.playerController.windowClick(mc.player.inventoryContainer.windowId, i, 0, ClickType.SWAP, mc.player);
                        //i chose slot 0 by default just because it works nice
                        break;
                    }

                }

            }
        }
    }

    private boolean canPlaceBed(BlockPos pos) {
        return (mc.world.getBlockState(pos).getBlock() == Blocks.AIR || mc.world.getBlockState(pos).getBlock() == Blocks.BED) &&
                mc.world.getEntitiesWithinAABB((Class) Entity.class, new AxisAlignedBB(pos)).isEmpty();

    }

    private void findClosestTarget() {
        List<EntityPlayer> playerList = mc.world.playerEntities;
        closestTarget = null;
        for (EntityPlayer target : playerList) {
            //this is also a remnant from the skid
            //but it works well so who cares
            if (target == mc.player) {
                continue;
            }
            if (Friends.isFriend(target.getName())) {
                continue;
            }
            if (!isLiving(target)) {
                continue;
            }
            if ((target).getHealth() <= 0) {
                continue;
            }
            if (closestTarget == null) {
                closestTarget = target;
                continue;
            }
            if (mc.player.getDistance(target) < mc.player.getDistance(closestTarget)) {
                closestTarget = target;
            }
        }
    }

    private void placeBlock(BlockPos pos, EnumFacing side) {
        //this method might not even be worth it but again if it works it works
        BlockPos neighbour = pos.offset(side);
        EnumFacing opposite = side.getOpposite();
        Vec3d hitVec = new Vec3d(neighbour).add(0.5, 0.5, 0.5).add(new Vec3d(opposite.getDirectionVec()).scale(0.5));
        mc.playerController.processRightClickBlock(mc.player, mc.world, neighbour, opposite, hitVec, EnumHand.MAIN_HAND);
        mc.player.swingArm(EnumHand.MAIN_HAND);
    }

    public static boolean isLiving(Entity e) {
        return e instanceof EntityLivingBase;
    }

    //render type shit
    //renders where the bed was calculated to place, not neccessarily where it DID place.
    //i used to render just the bed using a bedesp type shit, but i wanted this for debug purposes
    //since the placetarget updates every tick the rendering of the box might fling around a lil

    //credit to TrvsF for this the method i was using before was shitty
    //and this looks nicer

}