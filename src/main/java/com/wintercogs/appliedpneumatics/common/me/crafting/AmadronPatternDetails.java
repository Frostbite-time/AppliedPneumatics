package com.wintercogs.appliedpneumatics.common.me.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import me.desht.pneumaticcraft.api.crafting.AmadronTradeResource;
import me.desht.pneumaticcraft.common.amadron.AmadronOfferManager;
import me.desht.pneumaticcraft.common.recipes.amadron.AmadronOffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AmadronPatternDetails implements IPatternDetails
{
    private final AEItemKey definition;

    private final Input[] inputs;
    private final List<GenericStack> outputs;

    private final ResourceLocation offerId;

    public AmadronPatternDetails(AEItemKey definition)
    {
        this.definition = definition;

        EncodedAmadronPattern pattern = definition.get(APDataComponents.AMADRON_PATTERN.get());

        if(pattern == null)
            throw new IllegalArgumentException("Given item does not encode a processing pattern: " + definition);

        AmadronOffer offer = AmadronOfferManager.getInstance().getOffer(pattern.offerId());
        if(offer == null)
            throw new IllegalArgumentException("Given item does not have an offer: " + definition);

        this.offerId = offer.getOfferId();
        ItemStack mayInputStackItem = offer.getInput().getItem();
        FluidStack mayInputStackFluid = offer.getInput().getFluid();
        GenericStack input = mayInputStackItem.isEmpty() ? GenericStack.fromFluidStack(mayInputStackFluid) : GenericStack.fromItemStack(mayInputStackItem);
        inputs = new Input[] { new Input(input) };

        ItemStack mayOutputStackItem = offer.getOutput().getItem();
        FluidStack mayOutputStackFluid = offer.getOutput().getFluid();
        GenericStack output = mayOutputStackItem.isEmpty() ? GenericStack.fromFluidStack(mayOutputStackFluid) : GenericStack.fromItemStack(mayOutputStackItem);
        outputs = new ArrayList<>(Collections.singleton(output));
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
    public List<GenericStack> getOutputs()
    {
        return outputs;
    }

    public static void encode(ItemStack stack, ResourceLocation offerId)
    {
        if(AmadronOfferManager.getInstance().getOffer(offerId) != null)
        {
            stack.set(APDataComponents.AMADRON_PATTERN, new EncodedAmadronPattern(offerId));
        }
    }


    public static PatternDetailsTooltip getInvalidPatternTooltip(ItemStack stack, Level level,
                                                                 @Nullable Exception cause, TooltipFlag flags)
    {
        PatternDetailsTooltip tooltip = new PatternDetailsTooltip(PatternDetailsTooltip.OUTPUT_TEXT_PRODUCES);

        var encodedPattern = stack.get(APDataComponents.AMADRON_PATTERN);
        if (encodedPattern != null && AmadronOfferManager.getInstance().getOffer(encodedPattern.offerId()) != null)
        {
            addTooltipFromAmadronResource(tooltip, AmadronOfferManager.getInstance().getOffer(encodedPattern.offerId()).getInput(), true);
            addTooltipFromAmadronResource(tooltip, AmadronOfferManager.getInstance().getOffer(encodedPattern.offerId()).getOutput(), false);
        }
        return tooltip;
    }

    private static void addTooltipFromAmadronResource(PatternDetailsTooltip tooltip, AmadronTradeResource resource, boolean toInput)
    {
        if(!resource.getItem().isEmpty())
        {
            if(toInput)
                tooltip.addInput(AEItemKey.of(resource.getItem()), resource.getAmount());
            else
                tooltip.addOutput(AEItemKey.of(resource.getItem()), resource.getAmount());
        }
        else if(!resource.getFluid().isEmpty())
        {
            if(toInput)
                tooltip.addInput(AEFluidKey.of(resource.getFluid()), resource.getAmount());
            else
                tooltip.addOutput(AEFluidKey.of(resource.getFluid()), resource.getAmount());
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
