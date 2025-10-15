package com.wintercogs.appliedpneumatics.util;

public class APMath
{
    public static int ClampToInt(long value)
    {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(value, Integer.MIN_VALUE));
    }
}
