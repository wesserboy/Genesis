package genesis.world.biome;

import genesis.common.GenesisBlocks;
import genesis.metadata.EnumSilt;
import genesis.metadata.SiltBlocks;

public class BiomeGenRedBeach extends BiomeGenBeachGenesis
{
	public BiomeGenRedBeach(int id)
	{
		super(id);
		setBiomeName("Red Beach");
		setTemperatureRainfall(2.0F, 0.0F);
		setDisableRain();
		topBlock = GenesisBlocks.silt.getBlockState(SiltBlocks.SILT, EnumSilt.RED_SILT);
		fillerBlock = GenesisBlocks.silt.getBlockState(SiltBlocks.SILT, EnumSilt.RED_SILT);
	}
}