package myau.util;

import java.util.HashMap;
import myau.data.EnchantmentData;

public final class EnchantmentMap extends HashMap<Integer, EnchantmentData> {
    public EnchantmentMap() {
        this.put(0, new EnchantmentData("Pr", 4));
        this.put(1, new EnchantmentData("Fp", 4));
        this.put(2, new EnchantmentData("Ff", 4));
        this.put(3, new EnchantmentData("Bp", 4));
        this.put(4, new EnchantmentData("Pp", 4));
        this.put(5, new EnchantmentData("Re", 3));
        this.put(6, new EnchantmentData("Aq", 1));
        this.put(7, new EnchantmentData("Th", 3));
        this.put(8, new EnchantmentData("Ds", 3));
        this.put(16, new EnchantmentData("Sh", 5));
        this.put(17, new EnchantmentData("Sm", 5));
        this.put(18, new EnchantmentData("BoA", 5));
        this.put(19, new EnchantmentData("Kb", 2));
        this.put(20, new EnchantmentData("Fa", 2));
        this.put(21, new EnchantmentData("Lo", 3));
        this.put(32, new EnchantmentData("Ef", 5));
        this.put(33, new EnchantmentData("St", 1));
        this.put(34, new EnchantmentData("Ub", 3));
        this.put(35, new EnchantmentData("Fo", 3));
        this.put(48, new EnchantmentData("Po", 5));
        this.put(49, new EnchantmentData("Pu", 2));
        this.put(50, new EnchantmentData("Fl", 1));
        this.put(51, new EnchantmentData("Inf", 1));
        this.put(61, new EnchantmentData("LoS", 3));
        this.put(62, new EnchantmentData("Lu", 3));
    }
}
