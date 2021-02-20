package javapower.storagetech.item;

import com.raoulvdberge.refinedstorage.RSItems;
import com.raoulvdberge.refinedstorage.api.storage.StorageType;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDiskProvider;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDiskSyncData;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import java.util.List;
import java.util.UUID;
import javapower.storagetech.core.StorageTech;
import javapower.storagetech.item.ItemMemory;
import javapower.storagetech.util.IItemRegister;
import javapower.storagetech.util.IRenderItemRegister;
import javapower.storagetech.util.ItemRenderCast;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class ItemFluidDiskCustom
extends Item
implements IItemRegister,
IStorageDiskProvider,
IRenderItemRegister {
    public ItemFluidDiskCustom() {
        this.setRegistryName("fluidcustomdisk");
        this.setTranslationKey("fluidcustomdisk");
        this.setCreativeTab(StorageTech.creativeTab);
        this.setMaxStackSize(1);
    }

    @Override
    public Item getItem() {
        return this;
    }
	
	@Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.onUpdate(stack, world, entity, slot, selected);
        if (!world.isRemote) {
			/*
            if (!this.isValid(stack)) {
                API.instance().getOneSixMigrationHelper().migrateDisk(world, stack);
            }
			*/
            if (!stack.hasTagCompound()) {
                UUID id = UUID.randomUUID();
                API.instance().getStorageDiskManager(world).set(id, API.instance().createDefaultFluidDisk(world, this.getCapacity(stack)));
                API.instance().getStorageDiskManager(world).markForSaving();
                this.setId(stack, id);
            }
        }
    }
	
	@Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        if (this.isValid(stack)) {
            UUID id = this.getId(stack);
            API.instance().getStorageDiskSync().sendRequest(id);
            IStorageDiskSyncData data = API.instance().getStorageDiskSync().getData(id);
            if (data != null) {
                tooltip.add(I18n.format((String)"misc.refinedstorage:storage.stored_capacity", (Object[])new Object[]{API.instance().getQuantityFormatter().format(data.getStored()), API.instance().getQuantityFormatter().format(data.getCapacity())}));
            }
            if (flag.isAdvanced()) {
                tooltip.add(id.toString());
            }
        }
    }
	
	@Override
    public int getEntityLifespan(ItemStack stack, World world) {
        return Integer.MAX_VALUE;
    }

    @Override
    public ItemRenderCast[] getItemsRender() {
        return new ItemRenderCast[]{new ItemRenderCast(0, "fluidcustomdisk")};
    }
	
	@Override
    public int getCapacity(ItemStack disk) {
        int cappacity = 1;
        if (disk != null && disk.getTagCompound() != null && disk.getTagCompound().hasKey("st_cap")) {
            cappacity = disk.getTagCompound().getInteger("st_cap");
        }
        return cappacity;
    }
	
	@Override
    public UUID getId(ItemStack disk) {
        return disk.getTagCompound().getUniqueId("Id");
    }
	
	@Override
    public StorageType getType() {
        return StorageType.FLUID;
    }
	
	@Override
    public boolean isValid(ItemStack disk) {
        return disk.hasTagCompound() && disk.getTagCompound().hasUniqueId("Id") && disk.getTagCompound().hasKey("st_cap");
    }
	
	@Override
    public void setId(ItemStack disk, UUID id) {
        disk.setTagCompound(new NBTTagCompound());
        disk.getTagCompound().setUniqueId("Id", id);
    }
	
	@Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack itemStack;
        if (!worldIn.isRemote && playerIn.isSneaking() && (itemStack = playerIn.getHeldItem(handIn)) != null && itemStack.getItem() instanceof ItemFluidDiskCustom) {
            UUID id = this.getId(itemStack);
            API.instance().getStorageDiskSync().sendRequest(id);
            IStorageDiskSyncData storageData = API.instance().getStorageDiskSync().getData(id);
            if (storageData == null || storageData.getStored() <= 0) {
                int cap = itemStack.getTagCompound().getInteger("st_cap");
                ItemStack memory_item = ItemMemory.createItem(cap, true);
                playerIn.setHeldItem(handIn, new ItemStack((Item)RSItems.STORAGE_HOUSING, 1));
                if (cap > 0) {
                    playerIn.dropItem(memory_item, true);
                }
            }
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
    }
}
