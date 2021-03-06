package genesis.world.gen.feature;

import java.util.Random;

import com.google.common.base.Predicate;

import genesis.block.BlockGenesisLeaves;
import genesis.block.BlockResin;
import genesis.combo.TreeBlocksAndItems;
import genesis.combo.variant.EnumTree;
import genesis.common.GenesisBlocks;
import genesis.util.random.i.RandomIntProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockLog.EnumAxis;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.common.IPlantable;

public abstract class WorldGenTreeBase extends WorldGenAbstractTree
{
	public final IBlockState sapling;
	public final IBlockState wood;
	public final IBlockState leaves;
	public final IBlockState hangingFruit;
	
	protected final boolean notify;
	protected int rarity = 1;
	protected int vineRarity = 6;
	protected int vineMaxLength = 8;
	
	protected RandomIntProvider heightProvider;
	
	private boolean canGrowInWater = false;
	private boolean generateVine = false;
	private Predicate<IBlockState> soilPredicate = (s) -> true;
	
	protected RandomIntProvider saplingCountProvider = null;
	
	protected TreeTypes treeType = TreeTypes.TYPE_1;
	
	public boolean plantedWithSapling = false;
	
	private BlockVine vine = GenesisBlocks.ANKYROPTERIS;
	
	public enum TreeTypes
	{
		TYPE_1, TYPE_2, TYPE_3
	}
	
	public WorldGenTreeBase(IBlockState sapling, IBlockState wood, IBlockState leaves, IBlockState hangingFruit,
			RandomIntProvider heightProvider, boolean notify)
	{
		super(notify);
		
		this.sapling = sapling;
		this.wood = wood;
		this.leaves = leaves;
		this.hangingFruit = hangingFruit;
		
		this.heightProvider = heightProvider;
		
		this.notify = notify;
	}
	
	public WorldGenTreeBase(EnumTree variant, RandomIntProvider heightProvider, boolean notify)
	{
		this(GenesisBlocks.TREES.getBlockState(TreeBlocksAndItems.SAPLING, variant),
				GenesisBlocks.TREES.getBlockState(TreeBlocksAndItems.LOG, variant).withProperty(BlockLog.LOG_AXIS, EnumAxis.Y),
				GenesisBlocks.TREES.getBlockState(TreeBlocksAndItems.LEAVES, variant),
				GenesisBlocks.TREES.containsVariant(TreeBlocksAndItems.HANGING_FRUIT, variant)
						? GenesisBlocks.TREES.getBlockState(TreeBlocksAndItems.HANGING_FRUIT, variant)
						: null,
				heightProvider,
				notify);
	}
	
	public WorldGenTreeBase generateVine(int vineRarity)
	{
		this.generateVine = true;
		this.vineRarity = vineRarity;
		return this;
	}
	
	public WorldGenTreeBase generateVine(int vineRarity, BlockVine vine)
	{
		this.generateVine = true;
		this.vineRarity = vineRarity;
		this.vine = vine;
		return this;
	}
	
	public WorldGenTreeBase generateVine(int vineRarity, int vineMaxLength, BlockVine vine)
	{
		this.generateVine = true;
		this.vineRarity = vineRarity;
		this.vineMaxLength = vineMaxLength;
		if (this.vineMaxLength == 0)
			this.vineMaxLength = 1;
		this.vine = vine;
		return this;
	}
	
	public WorldGenTreeBase setType(TreeTypes type)
	{
		this.treeType = type;
		return this;
	}
	
	public WorldGenTreeBase setRarity(int r)
	{
		rarity = r;
		return this;
	}
	
	public WorldGenTreeBase setCanGrowInWater(boolean canGrow)
	{
		canGrowInWater = canGrow;
		return this;
	}
	
	public void setSoilPredicate(Predicate<IBlockState> predicate)
	{
		soilPredicate = predicate;
	}
	
	public boolean getCanGrowInWater()
	{
		return canGrowInWater;
	}
	
	protected abstract boolean doGenerate(World world, Random rand, BlockPos pos);
	
	@Override
	public final boolean generate(World world, Random rand, BlockPos pos)
	{
		pos = getTreePos(world, pos, -1);
		
		if (pos == null)
			return false;
		
		return rand.nextInt(rarity) == 0 && doGenerate(world, rand, pos);
	}
	
	@Override
	public void generateSaplings(World world, Random rand, BlockPos pos)
	{
		int saplingCount = saplingCountProvider != null ? saplingCountProvider.get(rand) : 0;
		
		for (int i = 0; i < saplingCount; i++)
		{
			BlockPos plantPos = pos.add(rand.nextInt(9) - 4, 1, rand.nextInt(9) - 4);
			plantPos = getTreePos(world, plantPos, 3);
			
			if (plantPos == null)
				continue;
			
			BlockPos soilPos = plantPos.down();
			IBlockState soilState = world.getBlockState(soilPos);
			
			if (!soilState.getBlock().canSustainPlant(soilState, world, soilPos, EnumFacing.UP, (IPlantable) sapling.getBlock()))
				continue;
			
			setBlockInWorld(world, plantPos, sapling);
		}
	}
	
	/**
	 * @return The position the sapling would be at above the soil, or null if the tree cannot grow there.
	 */
	public BlockPos getTreePos(World world, BlockPos pos, int distance)
	{
		BlockPos soilPos = pos;
		IBlockState checkState;
		
		do
		{
			if (distance != -1 && pos.getY() - soilPos.getY() > distance)
				return null;
			
			checkState = world.getBlockState(soilPos);
			
			if (!checkState.getBlock().isAir(checkState, world, soilPos)
					&& !checkState.getBlock().isLeaves(checkState, world, soilPos)
					&& checkState.getMaterial() != Material.WATER)
				break;
			
			soilPos = soilPos.down();
		}
		while (soilPos.getY() > 0);
		
		// Begin checking whether tree can grow here.
		BlockPos saplingPos = soilPos.up();
		
		if (!soilPredicate.apply(checkState)
				|| !((BlockBush) sapling.getBlock()).canBlockStay(world, saplingPos, sapling))
			return null;
		
		IBlockState replacing = world.getBlockState(saplingPos);
		
		if (!replacing.getBlock().isReplaceable(world, saplingPos)
				|| (!canGrowInWater && replacing.getMaterial() == Material.WATER))
			return null;
		
		return saplingPos;
	}
	
	public BlockPos getTreePos(World world, BlockPos pos)
	{
		return getTreePos(world, pos, -1);
	}
	
	protected void generateBranchSideup(World world, BlockPos pos, Random rand, int dirX, int dirZ, int bBaseHeight, int bGrowSize, int leavesLength)
	{
		BlockPos branchPos = pos;
		int height = bBaseHeight + rand.nextInt(bGrowSize);
		
		branchPos = branchPos.add(dirX, 0, dirZ);
		setBlockInWorld(world, branchPos, wood);
		
		branchPos = branchPos.add(dirX, 1, dirZ);
		
		for (int i = 0; i < height; ++i)
		{
			setBlockInWorld(world, branchPos, wood);
			branchPos = branchPos.up();
		}
		
		doPineTopLeaves(world, pos, branchPos.down(), height, branchPos.getY() - height + 1, rand, false, leavesLength, false, false);
	}
	
	protected void generateBranchSide(World world, BlockPos pos, Random rand, int dirX, int dirZ, int maxLength, int branchRarity)
	{
		BlockPos branchPos = pos;
		EnumAxis axis;
		int branchLength = 1 + rand.nextInt(maxLength);
		
		branchPos = branchPos.add(dirX, 0, dirZ);
		
		if (dirX != 0)
			axis = EnumAxis.X;
		else if (dirZ != 0)
			axis = EnumAxis.Z;
		else
			axis = EnumAxis.Y;
		
		setBlockInWorld(world, branchPos, wood.withProperty(BlockLog.LOG_AXIS, axis));
		generateHorizontalBranchLeaveS(world, branchPos, dirX, dirZ);
		
		for (int i = 1; i <= branchLength - 1; ++i)
		{
			branchPos = branchPos.add(dirX, (rand.nextInt(3) - 1) * rand.nextInt(2), dirZ);
			setBlockInWorld(world, branchPos, wood.withProperty(BlockLog.LOG_AXIS, axis));
			generateHorizontalBranchLeaveS(world, branchPos, dirX, dirZ);
			
			if (rand.nextInt(branchRarity) == 0)
			{
				int dSwitch = (rand.nextInt(2) == 0)? 1:-1;
				int childLength = 1 + rand.nextInt(branchLength);
				generateBranchSide(world, branchPos, rand, ((dirX == 0)? dSwitch : 0), ((dirZ == 0)? dSwitch : 0), childLength, branchRarity + 1); //((int)(maxLength / 2) < 1)? 1 : ((int)(maxLength / 2))
			}
		}
		
		branchPos = branchPos.add(dirX, 0, dirZ);
		setBlockInWorld(world, branchPos, leaves);
	}
	
	protected void generateHorizontalBranchLeaveS(World world, BlockPos pos, int dirX, int dirZ)
	{
		setBlockInWorld(world, pos.add(0, 1, 0), leaves);
		setBlockInWorld(world, pos.add(0, -1, 0), leaves);
		setBlockInWorld(world, pos.add(((dirX == 0) ? 1 : 0), 0, ((dirZ == 0) ? 1 : 0)), leaves);
		setBlockInWorld(world, pos.add(((dirX == 0) ? -1 : 0), 0, ((dirZ == 0) ? -1 : 0)), leaves);
	}
	
	protected void generateLeafLayerCircle(World world, Random random, double radius, BlockPos pos)
	{
		for (int x = (int) Math.ceil(pos.getX() - radius); x <= (int) Math.ceil(pos.getX() + radius); x++)
		{
			for (int z = (int) Math.ceil(pos.getZ() - radius); z <= (int) Math.ceil(pos.getZ() + radius); z++)
			{
				double xfr = z - pos.getZ();
				double zfr = x - pos.getX();
				
				if (xfr * xfr + zfr * zfr <= radius * radius || x * x + z * z <= radius * radius)
				{
					setBlockInWorld(world, new BlockPos(x, pos.getY(), z), leaves);
				}
			}
		}
	}
	
	protected void generateResin(World world, BlockPos pos, int height)
	{
		generateResin(world, pos, height, 0);
	}
	
	protected void generateResin(World world, BlockPos pos, int height, int baseHeight)
	{
		for (int i = baseHeight; i < height; i++)
		{
			for (EnumFacing facing : EnumFacing.HORIZONTALS)
			{
				if (world.rand.nextInt(!plantedWithSapling ? 200 + world.rand.nextInt(10) : 600) == 0)
				{
					setBlockInWorld(world,
							pos.up(i).offset(facing),
							GenesisBlocks.RESIN.getDefaultState()
									.withProperty(BlockHorizontal.FACING, facing)
									.withProperty(BlockResin.LAYERS, world.rand.nextInt(4)));
				}
			}
		}
	}
	
	protected void setBlockInWorld(World world, BlockPos pos, IBlockState state)
	{
		setBlockInWorld(world, pos, state, false);
	}
	
	protected void setBlockInWorld(World world, BlockPos pos, IBlockState state, boolean force)
	{
		if (!world.isBlockLoaded(pos))
			return;
		
		IBlockState currentState = world.getBlockState(pos);
		
		if (state == wood)
		{
			if (!(currentState.getBlock().isAir(currentState, world, pos)
							|| currentState.getBlock().isReplaceable(world, pos)
							|| currentState.getBlock().isLeaves(currentState, world, pos)
							|| (currentState.getMaterial() == Material.WATER && canGrowInWater))
					&& !force
					&& !(currentState.getBlock() == vine))
			return;
		}
		else if (state == leaves)
		{
			if (!currentState.getBlock().isAir(currentState, world, pos)
					&& !force
					&& !(currentState == hangingFruit)
					&& !(currentState.getBlock() == vine))
				return;
			
			if (GenesisBlocks.TREES.getVariant(leaves).getFruitType() == EnumTree.FruitType.LEAVES
					&& world.rand.nextInt(6) == 0)
				state = GenesisBlocks.TREES.getBlockState(TreeBlocksAndItems.LEAVES_FRUIT, GenesisBlocks.TREES.getVariant(leaves));
		}
		else if (
				!force
				&& !currentState.getBlock().isAir(currentState, world, pos)
				&& !canGrowInWater
				&& !currentState.getBlock().isReplaceable(world, pos))
		{
			return;
		}
		
		BlockPos below = pos.down();
		IBlockState stateBelow = world.getBlockState(below);
		
		if (stateBelow == hangingFruit)
			world.setBlockState(below, Blocks.AIR.getDefaultState());
		
		if (notify)
			world.setBlockState(pos, state, 3);
		else
			world.setBlockState(pos, state, 2);
		
		if (generateVine && world.rand.nextInt(vineRarity) == 0 && !(vine == GenesisBlocks.FRULLANIA && state.getBlock() instanceof BlockGenesisLeaves))
		{
			BlockPos north = pos.north();
			BlockPos south = pos.south();
			BlockPos east = pos.east();
			BlockPos west = pos.west();
			
			IBlockState stateNorth = world.getBlockState(north);
			IBlockState stateSouth = world.getBlockState(south);
			IBlockState stateEast = world.getBlockState(east);
			IBlockState stateWest = world.getBlockState(west);
			
			vineGenerate(world, stateNorth, north, BlockVine.SOUTH);
			vineGenerate(world, stateSouth, south, BlockVine.NORTH);
			vineGenerate(world, stateEast, east, BlockVine.WEST);
			vineGenerate(world, stateWest, west, BlockVine.EAST);
		}
		
		if (hangingFruit != null && state == leaves
				&& world.rand.nextInt(10) == 0
				&& stateBelow.getBlock().isAir(stateBelow, world, below))
			world.setBlockState(below, hangingFruit);
	}
	
	private void vineGenerate(World world, IBlockState state, BlockPos pos, PropertyBool oppositeVineFacing)
	{
		if (
				world.rand.nextInt(this.vineRarity) == 0
				&& state.getBlock().isAir(state, world, pos))
		{
			int vineLength = world.rand.nextInt(this.vineMaxLength) + 1;
			for (int vi = 0; vi < vineLength; ++vi)
				if (world.isAirBlock(pos.add(0, -vi, 0)))
					world.setBlockState(pos.add(0, -vi, 0), vine.getDefaultState().withProperty(oppositeVineFacing, true));
		}
	}
	
	public void doBranchLeaves(World world, BlockPos pos, Random random, boolean cap, int length)
	{
		doBranchLeaves(world, pos, random, cap, length, false);
	}
	
	public void doBranchLeaves(World world, BlockPos pos, Random random, boolean cap, int length, boolean irregular)
	{
		for (int i = 1; i <= length - ((irregular && random.nextInt(3) == 0)? (random.nextInt(length + 1)) : 0); ++i)
		{
			setBlockInWorld(world, pos.north(i), leaves);
			if (!irregular || !(random.nextInt(6) == 0))
				setBlockInWorld(world, pos.north(i - 1).east(), leaves);
			if (!irregular || !(random.nextInt(6) == 0))
				setBlockInWorld(world, pos.north(i - 1).west(), leaves);
		}
		
		for (int i = 1; i <= length - ((irregular && random.nextInt(3) == 0)? (random.nextInt(length + 1)) : 0); ++i)
		{
			setBlockInWorld(world, pos.south(i), leaves);
			if (!irregular || !(random.nextInt(6) == 0))
				setBlockInWorld(world, pos.south(i - 1).east(), leaves);
			if (!irregular || !(random.nextInt(6) == 0))
				setBlockInWorld(world, pos.south(i - 1).west(), leaves);
		}
		
		for (int i = 1; i <= length - ((irregular && random.nextInt(3) == 0)? (random.nextInt(length + 1)) : 0); ++i)
		{
			setBlockInWorld(world, pos.east(i), leaves);
			if (!irregular || !(random.nextInt(6) == 0))
				setBlockInWorld(world, pos.east(i - 1).north(), leaves);
			if (!irregular || !(random.nextInt(6) == 0))
				setBlockInWorld(world, pos.east(i - 1).south(), leaves);
		}
		
		for (int i = 1; i <= length - ((irregular && random.nextInt(3) == 0)? (random.nextInt(length + 1)) : 0); ++i)
		{
			setBlockInWorld(world, pos.west(i), leaves);
			if (!irregular || !(random.nextInt(6) == 0))
				setBlockInWorld(world, pos.west(i - 1).north(), leaves);
			if (!irregular || !(random.nextInt(6) == 0))
				setBlockInWorld(world, pos.west(i - 1).south(), leaves);
		}
		
		if (cap)
		{
			setBlockInWorld(world, pos.up(1), leaves);
			setBlockInWorld(world, pos.up(1).north(), leaves);
			setBlockInWorld(world, pos.up(1).south(), leaves);
			setBlockInWorld(world, pos.up(1).east(), leaves);
			setBlockInWorld(world, pos.up(1).west(), leaves);
			setBlockInWorld(world, pos.up(2), leaves);
		}
	}
	
	protected void doLeavesCircleLayer(World world, BlockPos pos, int width)
	{
		int i = (width * width);
		
		for (int j = -width; j <= width + 1; ++j)
		{
			for (int k = -width; k <= width + 1; ++k)
			{
				int l = j - 1;
				int i1 = k - 1;
				
				if (j * j + k * k <= i || l * l + i1 * i1 <= i || j * j + i1 * i1 <= i || l * l + k * k <= i)
				{
					BlockPos blockpos = pos.add(j, 0, k);
					setBlockInWorld(world, blockpos, leaves);
				}
			}
		}
	}
	
	public void doPineTopLeaves(World world, BlockPos genPos, BlockPos branchPos, int treeHeight, int leavesBase, Random rand, boolean alternate, boolean irregular, boolean inverted)
	{
		doPineTopLeaves(world, genPos, branchPos, treeHeight, leavesBase, rand, alternate, 4, irregular, inverted);
	}
	
	public void doPineTopLeaves(World world, BlockPos genPos, BlockPos branchPos, int treeHeight, int leavesBase, Random rand, boolean alternate, boolean irregular)
	{
		doPineTopLeaves(world, genPos, branchPos, treeHeight, leavesBase, rand, alternate, 4, irregular, false);
	}
	
	public void doPineTopLeaves(World world, BlockPos genPos, BlockPos branchPos, int treeHeight, int leavesBase, Random rand, boolean alternate)
	{
		doPineTopLeaves(world, genPos, branchPos, treeHeight, leavesBase, rand, alternate, 4, false, false);
	}
	
	public void doPineTopLeaves(World world, BlockPos genPos, BlockPos branchPos, int treeHeight, int leavesBase, Random rand, boolean alternate, int maxLeaveLength, boolean irregular, boolean inverted)
	{
		boolean alt = false;
		float percent;
		int leaves;
		
		if (leavesBase > branchPos.getY())
			return;
		
		doBranchLeaves(world, branchPos, rand, true, 1);
		
		while (branchPos.getY() > leavesBase)
		{
			branchPos = branchPos.add(0, -1, 0);
			
			percent = ((branchPos.getY() - leavesBase) / (float) (genPos.getY() + treeHeight - leavesBase));
			
			if (!inverted)
				percent = 1 - percent;
			
			leaves = MathHelper.ceiling_float_int(maxLeaveLength * percent);
			
			if (leaves > maxLeaveLength)
				leaves = maxLeaveLength;
			
			if (alt || !alternate || (irregular && rand.nextInt(5) == 0))
				doBranchLeaves(world, branchPos, rand, false, leaves, irregular);
			
			alt = !alt;
		}
	}
	
	@Override
	protected boolean canGrowInto(Block block)
	{
		return block == GenesisBlocks.MOSS || super.canGrowInto(block);
	}
	
	protected Predicate<BlockPos> isEmptySpace(World world)
	{
		return pos ->
		{
			IBlockState state = world.getBlockState(pos);
			return (state.getBlock().isReplaceable(world, pos)
						   || state.getBlock().isLeaves(state, world, pos))
						   && (!canGrowInWater || state.getMaterial() != Material.WATER);
		};
	}
}
