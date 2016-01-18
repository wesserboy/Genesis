package genesis.world.biome;

public class BiomeGenSavannaM extends BiomeGenSavanna
{
	public BiomeGenSavannaM(int id)
	{
		super(id);
		setBiomeName("Savanna M");
		setHeight(0.4F, 0.7F);
	}
	
	@Override
	public float getFogDensity()
	{
		return 0.75F;
	}
	
	@Override
	public float getNightFogModifier()
	{
		return 0.65F;
	}
}