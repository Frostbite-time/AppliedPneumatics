package com.wintercogs.appliedpneumatics.common.me.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import me.desht.pneumaticcraft.api.crafting.recipe.AmadronRecipe;
import me.desht.pneumaticcraft.common.amadron.AmadronOfferManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class AmadronPatternDetails implements IPatternDetails
{
    private static final String PATTERN_INFO_TAG_NAME = "amadron_pattern_info";

    private final AEItemKey definition;

    private final Input[] inputs;
    private final GenericStack[] outputs;

    private final ResourceLocation offerId;

    public AmadronPatternDetails(AEItemKey definition)
    {
        this.definition = definition;

        if(!definition.hasTag() || !definition.getTag().contains(PATTERN_INFO_TAG_NAME))
            throw new IllegalArgumentException("Given item does not encode a processing pattern: " + definition);

        EncodedAmadronPattern pattern = EncodedAmadronPattern.fromNBT(definition.getTag().getCompound(PATTERN_INFO_TAG_NAME));

        AmadronRecipe offer = AmadronOfferManager.getInstance().getOffer(pattern.offerId());
        if(offer == null)
            throw new IllegalArgumentException("Given item does not have an offer: " + definition);

        this.offerId = offer.getId();
        ItemStack mayInputStackItem = offer.getInput().getItem();
        FluidStack mayInputStackFluid = offer.getInput().getFluid();
        GenericStack input = mayInputStackItem.isEmpty() ? GenericStack.fromFluidStack(mayInputStackFluid) : GenericStack.fromItemStack(mayInputStackItem);
        inputs = new Input[] { new Input(input) };

        ItemStack mayOutputStackItem = offer.getOutput().getItem();
        FluidStack mayOutputStackFluid = offer.getOutput().getFluid();
        GenericStack output = mayOutputStackItem.isEmpty() ? GenericStack.fromFluidStack(mayOutputStackFluid) : GenericStack.fromItemStack(mayOutputStackItem);
        outputs = new GenericStack[] {output};
    }

    public ResourceLocation getOfferId()
    {
        return offerId;
    }

    @Override
    public AEItemKey getDefinition()
    {
        return this.definition;
    }

    @Override
    public IInput[] getInputs()
    {
        return inputs;
    }

    @Override
    public GenericStack[] getOutputs()
    {
        return outputs;
    }

    public static void encode(ItemStack stack, ResourceLocation offerId)
    {
        if(AmadronOfferManager.getInstance().getOffer(offerId) != null)
        {
            stack.getOrCreateTag().put(PATTERN_INFO_TAG_NAME, new EncodedAmadronPattern(offerId).toNBT());
        }
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == this) return true;
        if(obj instanceof AmadronPatternDetails patternDetails)
        {
            return definition.equals(patternDetails.definition);
        }
        return false;
    }

    private static class Input implements IInput
    {
        private final GenericStack[] template;
        private final long multiplier;

        private Input(GenericStack stack) {
            this.template = new GenericStack[] { new GenericStack(stack.what(), 1) };
            this.multiplier = stack.amount();
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return template;
        }

        @Override
        public long getMultiplier() {
            return multiplier;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return input.matches(template[0]);
        }

        @Nullable
        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
