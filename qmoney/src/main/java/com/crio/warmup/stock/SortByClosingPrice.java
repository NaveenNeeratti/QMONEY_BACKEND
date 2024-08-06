package com.crio.warmup.stock;

import java.util.Comparator;
import com.crio.warmup.stock.dto.TotalReturnsDto;

public class SortByClosingPrice implements Comparator<TotalReturnsDto> {

    @Override
    public int compare(TotalReturnsDto arg0, TotalReturnsDto arg1) {
        int diff = (int)(arg0.getClosingPrice() - arg1.getClosingPrice());
        return diff;
    }
}
