package carpet.utils.portalcalculator;

public enum EnumTargetDirection {
    /**
     * find the portal pattern toward which the target goes
     */
    FROM,
    /**
     * find a range in which portals toward the target doesn't go wrong,
     * or find the closest correct portal position in the range
     */
    TO
}
