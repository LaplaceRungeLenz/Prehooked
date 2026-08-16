package com.thecodewarrior.hooked.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import net.minecraft.init.Items;

import org.junit.Test;

public class ModItemsTest {

    @Test
    public void ropeUsesTwoStringsInAFreeformRecipe() {
        Object[] ingredients = ModItems.ropeIngredients();

        assertEquals(2, ingredients.length);
        assertSame(Items.string, ingredients[0]);
        assertSame(Items.string, ingredients[1]);
    }
}
