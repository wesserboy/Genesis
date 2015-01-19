package genesis.block;

import genesis.common.GenesisBlocks;
import genesis.common.GenesisCreativeTabs;
import genesis.util.Constants;
import genesis.util.Metadata;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

import java.util.List;

public class BlockPlant extends BlockBush {
    public BlockPlant() {
        setDefaultState(getBlockState().getBaseState().withProperty(Constants.PLANT_VARIANT, EnumPlant.COOKSONIA));
        setCreativeTab(GenesisCreativeTabs.DECORATIONS);
        setBlockBounds(0.5F - 0.4F, 0.0F, 0.5F - 0.4F, 0.5F + 0.4F, 0.4F * 2, 0.5F + 0.4F);
    }

    @Override
    protected boolean canPlaceBlockOn(Block ground) {
        return ground == GenesisBlocks.moss || super.canPlaceBlockOn(ground);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return Metadata.getMetadata(state, Constants.PLANT_VARIANT);
    }

    @Override
    public void getSubBlocks(Item itemIn, CreativeTabs tab, List list) {
        Metadata.getSubItems(EnumPlant.class, list);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return Metadata.getState(this, Constants.PLANT_VARIANT, EnumPlant.class, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return Metadata.getMetadata(state, Constants.PLANT_VARIANT);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, Constants.PLANT_VARIANT);
    }

    @Override
    public Block.EnumOffsetType getOffsetType() {
        return Block.EnumOffsetType.XYZ;
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 60;
    }
}
