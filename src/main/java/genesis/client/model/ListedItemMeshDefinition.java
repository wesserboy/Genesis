package genesis.client.model;

import java.util.Collection;

import net.minecraft.client.renderer.ItemMeshDefinition;

public interface ListedItemMeshDefinition extends ItemMeshDefinition
{
	public Collection<String> getVariants();
}