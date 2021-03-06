package genesis.world.biome.decorate;

import java.util.Random;

import genesis.combo.SiltBlocks;
import genesis.common.GenesisBlocks;
import genesis.util.WorldUtils;
import genesis.util.functional.WorldBlockMatcher;
import genesis.util.random.f.FloatRange;
import genesis.util.random.i.IntRange;
import genesis.util.random.i.RandomIntProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class WorldGenBoulders extends WorldGenDecorationBase
{
	private static final float DEFAULT_SHORE_RADIUS = 3;
	
	protected final IBlockState dry;
	protected final IBlockState wet;
	private final float landChance;
	private final float shoreChance;
	private final float waterChance;
	private final float shoreRadius;
	private boolean inGround = true;
	
	private FloatRange largeProvider;
	
	@SuppressWarnings("unused")
	private RandomIntProvider smallCountProvider = IntRange.create(1, 3);
	@SuppressWarnings("unused")
	private FloatRange smallProvider;
	
	private FloatRange horizStretch;
	private FloatRange vertStretch;
	
	public WorldGenBoulders(IBlockState dry, IBlockState wet, float landChance, float shoreChance, float waterChance, float shoreRadius)
	{
		super(WorldBlockMatcher.STANDARD_AIR_WATER,
				(s, w, p) -> s.getBlock() == Blocks.DIRT
						|| s.getBlock() == Blocks.GRASS
						|| s.getBlock() == GenesisBlocks.MOSS
						|| GenesisBlocks.SILT.isStateOf(s, SiltBlocks.SILT)
						|| GenesisBlocks.SILT.isStateOf(s, SiltBlocks.CRACKED_SILT));
		
		setRadius(1.25F);
		setStretch(1.25F);
		
		this.dry = dry;
		this.wet = wet;
		this.landChance = landChance;
		this.shoreChance = shoreChance;
		this.waterChance = waterChance;
		this.shoreRadius = shoreRadius;
	}
	
	public WorldGenBoulders(IBlockState dry, IBlockState wet, float landChance, float shoreChance, float waterChance)
	{
		this(dry, wet, landChance, shoreChance, waterChance, DEFAULT_SHORE_RADIUS);
	}
	
	public WorldGenBoulders(IBlockState dry, float landChance, float shoreChance, float waterChance, float waterRadius)
	{
		this(dry, null, landChance, shoreChance, waterChance, waterRadius);
	}
	
	public WorldGenBoulders(IBlockState dry, float landChance, float shoreChance, float waterChance)
	{
		this(dry, landChance, shoreChance, waterChance, DEFAULT_SHORE_RADIUS);
	}
	
	public WorldGenBoulders(float landChance, float shoreChance, float waterChance, float waterRadius)
	{
		this(GenesisBlocks.GRANITE.getDefaultState(), GenesisBlocks.MOSSY_GRANITE.getDefaultState(), landChance, shoreChance, waterChance, waterRadius);
	}
	
	public WorldGenBoulders(float landChance, float shoreChance, float waterChance)
	{
		this(landChance, shoreChance, waterChance, DEFAULT_SHORE_RADIUS);
	}
	
	@Override
	public boolean place(World world, Random rand, BlockPos pos)
	{
		float radius = largeProvider.get(rand);
		
		int checkRadius = (int) (radius * radius);
		float shore = radius + shoreRadius;
		int shoreSqr = (int) (shore * shore);
		
		float chance = landChance;
		
		for (BlockPos checkPos : WorldUtils.getArea(pos, MathHelper.ceiling_float_int(shore)))
		{
			int distSqr = WorldUtils.distSqr(pos, checkPos);
			
			if (WorldBlockMatcher.WATER.apply(world, checkPos))
			{
				if (distSqr <= checkRadius)
				{
					chance = waterChance;
					break;
				}
				else if (distSqr <= shoreSqr)
				{
					chance = shoreChance;
				}
			}
		}
		
		if (chance <= 0 || rand.nextFloat() > chance)
			return false;
		
		while (true)
		{
			boulder_loop:
			{
				if (pos.getY() > 3)
				{
					if (world.isAirBlock(pos.down()))
					{
						break boulder_loop;
					}
				}
				
				if (pos.getY() <= 3)
				{
					return false;
				}
				
				int spread = (int)radius;
				
				for (int i = 0; spread >= 0 && i < 3; ++i)
				{
					int px = spread + rand.nextInt(2);
					int py = spread + rand.nextInt(2);
					int pz = spread + rand.nextInt(2);
					float factor = (float)(px + py + pz) * 0.233F + 0.3F;
					
					for (BlockPos blockpos : BlockPos.getAllInBox(pos.add(-px, -py, -pz), pos.add(px, py, pz)))
					{
						if (blockpos.distanceSq(pos) <= (double)(factor * factor))
						{
							this.placeRockAndDryAround(world, blockpos, rand);
						}
					}
					
					pos = pos.add(-(spread + 1) + rand.nextInt(2 + spread * 2), 0 - rand.nextInt(2), -(spread + 1) + rand.nextInt(2 + spread * 2));
				}
				
				return true;
			}
		pos = pos.down();
		}
	}
	
	protected void placeRock(World world, BlockPos pos, Random rand)
	{
		int water = 0;
		int solid = 0;
		
		if (wet != null)
		{
			for (EnumFacing side : EnumFacing.HORIZONTALS)
			{
				BlockPos sidePos = pos.offset(side);
				IBlockState state = world.getBlockState(sidePos);
				
				if (state.getMaterial() == Material.WATER)
					water++;
				else if (state.getBlock().isSideSolid(state, world, sidePos, side.getOpposite()))
					solid++;
			}
		}
		
		IBlockState state = dry;
		
		BlockPos abovePos = pos.up();
		IBlockState above = world.getBlockState(abovePos);
		
		if (wet != null)
		{
			if (water > 0
					&& !WorldUtils.isWater(world, pos.up())
					&& above.getMaterial() != Material.WATER
					&& !above.getBlock().isSideSolid(above, world, abovePos, EnumFacing.DOWN))
				state = wet;
			else if (solid + water < 4 && rand.nextInt(4) <= 2)	// 75% chance for moss.
				state = wet;
		}
		
		if (inGround)
		{
			Material material = world.getBlockState(pos).getMaterial();
			
			if (material == Material.GRASS
					|| material == Material.GROUND
					|| material == Material.WATER)
				setBlock(world, pos, state);
			else
				setAirBlock(world, pos, state);
		}
		else
		{
			setAirBlock(world, pos, state);
		}
		
		//world.notifyBlockUpdate(pos, Blocks.AIR.getDefaultState(), dry, 3);	// For testing it with item right click.
	}
	
	protected void placeRockAndDryAround(World world, BlockPos pos, Random rand)
	{
		placeRock(world, pos, rand);
		
		if (wet != null)
		{
			for (EnumFacing side : EnumFacing.HORIZONTALS)
			{
				BlockPos sidePos = pos.offset(side);
				
				if (world.getBlockState(sidePos) == wet)
					placeRock(world, sidePos, rand);
			}
		}
	}
	
	protected void placeSphere(World world, Vec3d center, Random rand, float radius)
	{
		BlockPos pos = new BlockPos(center);
		int area = MathHelper.ceiling_float_int(radius);
		
		radius *= radius;
		
		float scaleX = horizStretch.get(rand);
		float scaleY = vertStretch.get(rand);
		float scaleZ = horizStretch.get(rand);
		
		for (BlockPos rockPos : BlockPos.getAllInBoxMutable(pos.add(-area, -area, -area), pos.add(area, area, area)))
		{
			double dX = (rockPos.getX() + 0.5 - center.xCoord) / scaleX;
			double dY = (rockPos.getY() + 0.5 - center.yCoord) / scaleY;
			double dZ = (rockPos.getZ() + 0.5 - center.zCoord) / scaleZ;
			
			if (dX * dX + dY * dY + dZ * dZ <= radius)
			{
				placeRockAndDryAround(world, rockPos, rand);
			}
		}
	}
	
	public WorldGenBoulders setInGround(boolean in)
	{
		inGround = in;
		return this;
	}
	
	public WorldGenBoulders setRadius(FloatRange large, FloatRange small)
	{
		largeProvider = large;
		smallProvider = small;
		return this;
	}
	
	public WorldGenBoulders setRadius(float large, float small)
	{
		return setRadius(FloatRange.create(1, large), FloatRange.create(0.5F, small));
	}
	
	public WorldGenBoulders setRadius(float radius)
	{
		return setRadius(radius, radius - 0.5F);
	}
	
	public WorldGenBoulders setStretch(FloatRange horizontal, FloatRange vertical)
	{
		horizStretch = horizontal;
		vertStretch = vertical;
		return this;
	}
	
	public WorldGenBoulders setStretch(float horizontal, float vertical)
	{
		return setStretch(FloatRange.create(horizontal - 0.5F, horizontal), FloatRange.create(vertical - 0.5F, vertical));
	}
	
	public WorldGenBoulders setStretch(float stretch)
	{
		return setStretch(stretch, stretch);
	}
}
